package com.dklamps.pathfinder;

import com.dklamps.enums.Transport;
import com.dklamps.enums.Direction;
import com.dklamps.enums.Lamp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;

public class Pathfinder {
    private static final int MAX_ITERATIONS = 10000; // Reasonable limit to prevent hangs
    private static final int MAX_PATH_LENGTH = 512; // Reasonable path length limit

    private final CollisionMap collisionMap;
    private final Map<WorldPoint, List<Transport>> transports;

    public Pathfinder() throws IOException {
        SplitFlagMap map = SplitFlagMap.loadFromResources();
        this.collisionMap = new CollisionMap(map);
        this.transports = new HashMap<>();

        for (Transport transport : Transport.values()) {
            transports.computeIfAbsent(transport.getOrigin(), k -> new ArrayList<>()).add(transport);
        }
    }

    public List<WorldPoint> findPath(WorldPoint start, WorldPoint end) {
        return findPathInternal(start, end, MAX_ITERATIONS);
    }
    
    private List<WorldPoint> findPathInternal(WorldPoint start, WorldPoint end, int maxIterations) {
        // Basic validation
        if (start == null || end == null) {
            return new ArrayList<>();
        }

        // If already at destination
        if (start.equals(end)) {
            List<WorldPoint> path = new ArrayList<>();
            path.add(start);
            return path;
        }

        // Check if destinations are too far apart (rough distance check)
        int roughDistance = Math.abs(start.getX() - end.getX()) + Math.abs(start.getY() - end.getY());
        if (roughDistance > MAX_PATH_LENGTH) {
            System.out.println("DEBUG: Pathfinding rejected - too far apart: " + roughDistance + " > " + MAX_PATH_LENGTH);
            return new ArrayList<>(); // Destination too far
        }
                
        Direction[] cardinalDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        
        // Find the lamp at this location to get unreachable directions
        Lamp targetLamp = null;
        for (Lamp lamp : Lamp.values()) {
            if (lamp.getWorldPoint().equals(end)) {
                targetLamp = lamp;
                break;
            }
        }
        
        WorldPoint bestTarget = null;
        int closestDistance = Integer.MAX_VALUE;
        
        for (Direction direction : cardinalDirections) {
            // Skip this direction if it's marked as unreachable for this lamp
            if (targetLamp != null && targetLamp.getUnreachableDirections().contains(direction)) {
                continue;
            }
            
            WorldPoint nearby = new WorldPoint(end.getX() + direction.getX(), end.getY() + direction.getY(), end.getPlane());
            List<Node> nearbyNeighbors = getNeighbors(new Node(nearby));
            if (!nearbyNeighbors.isEmpty()) {
                // Calculate distance from player to this adjacent tile
                int distance = Math.abs(start.getX() - nearby.getX()) + Math.abs(start.getY() - nearby.getY());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    bestTarget = nearby;
                }
            }
        }
        
        if (bestTarget != null) {
            end = bestTarget; // Use closest walkable tile as target
        } else {
            System.out.println("DEBUG: No walkable directions found for lamp at " + end);
            return new ArrayList<>();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<WorldPoint> closedSet = new HashSet<>();
        int iterations = 0;

        Node startNode = new Node(start);
        startNode.setGCost(0);
        startNode.setHCost(calculateHeuristic(start, end));
        startNode.setFCost(startNode.getHCost());
        openSet.add(startNode);

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;

            Node currentNode = openSet.poll();

            if (currentNode.getWorldPoint().equals(end)) {
                List<WorldPoint> path = currentNode.getPath();
                // Additional safety check on path length
                if (path.size() > MAX_PATH_LENGTH) {
                    return new ArrayList<>();
                }
                return path;
            }
            
            // Early termination for very short paths - they're likely optimal
            if (currentNode.getGCost() <= 5) {
                int directDistance = Math.abs(currentNode.getWorldPoint().getX() - end.getX()) + 
                                   Math.abs(currentNode.getWorldPoint().getY() - end.getY());
                if (directDistance <= 2) {
                    // We're very close, this path is likely good enough
                    List<WorldPoint> path = currentNode.getPath();
                    path.add(end); // Add the destination
                    return path;
                }
            }

            closedSet.add(currentNode.getWorldPoint());

            for (Node neighbor : getNeighbors(currentNode)) {
                if (closedSet.contains(neighbor.getWorldPoint())) {
                    continue;
                }

                int transportCost = 1;
                if (transports.containsKey(currentNode.getWorldPoint())) {
                    for (Transport transport : transports.get(currentNode.getWorldPoint())) {
                        transportCost = transport.getDuration();
                        break;
                    }
                }
                int tentativeGCost = currentNode.getGCost() + transportCost;

                // Safety check: abandon paths that are getting too long
                if (tentativeGCost > MAX_PATH_LENGTH) {
                    continue;
                }

                if (tentativeGCost < neighbor.getGCost() || !openSet.contains(neighbor)) {
                    neighbor.setGCost(tentativeGCost);
                    neighbor.setHCost(calculateHeuristic(neighbor.getWorldPoint(), end));
                    neighbor.setFCost(neighbor.getGCost() + neighbor.getHCost());
                    neighbor.setParent(currentNode);

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // Debug: Log why pathfinding failed
        if (iterations >= maxIterations) {
            System.out.println("DEBUG: Pathfinding hit MAX_ITERATIONS (" + maxIterations + ")");
        } else {
            System.out.println("DEBUG: Pathfinding failed - openSet empty after " + iterations + " iterations");
        }
        return new ArrayList<>(); // Return empty list if no path is found
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = collisionMap.getValidNeighbors(node, transports);
        return neighbors;
    }

    private int calculateHeuristic(WorldPoint from, WorldPoint to) {
        // Simple Manhattan distance - more reliable than WorldPointUtil
        int dx = Math.abs(from.getX() - to.getX());
        int dy = Math.abs(from.getY() - to.getY());
        int distance = dx + dy;
        
        // Add penalty for plane changes
        if (from.getPlane() != to.getPlane()) {
            // Higher penalty that scales with horizontal distance
            // If lamp is directly above/below (dx+dy small), penalty is significant
            // If lamp is far horizontally, penalty becomes less significant relative to distance
            int planeChangePenalty = Math.max(10, (dx + dy) / 4);
            distance += planeChangePenalty;
        }
        return distance;
    }
}