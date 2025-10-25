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
    
    public boolean isTransportLocation(WorldPoint point) {
        return transports.containsKey(point);
    }
    
    public List<Transport> getTransportsAt(WorldPoint point) {
        return transports.getOrDefault(point, new ArrayList<>());
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
        
        // If the lamp is on a different plane, we need to find a path that includes transports
        boolean crossPlane = start.getPlane() != end.getPlane();
        if (crossPlane) {
            System.out.println("DEBUG: Cross-plane pathfinding from plane " + start.getPlane() + " to plane " + end.getPlane());
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
        
        // If we're on the same plane, find adjacent walkable tiles
        if (!crossPlane) {
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
        }
        // For cross-plane pathfinding, we keep the lamp location as the target
        // and let the A* algorithm find transports to reach it

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

            // Check if we've reached the exact destination
            if (currentNode.getWorldPoint().equals(end)) {
                List<WorldPoint> path = currentNode.getPath();
                // Additional safety check on path length
                if (path.size() > MAX_PATH_LENGTH) {
                    return new ArrayList<>();
                }
                return path;
            }
            
            // For cross-plane pathfinding, also check if we're adjacent to the lamp on the same plane
            if (crossPlane && currentNode.getWorldPoint().getPlane() == end.getPlane()) {
                int dx = Math.abs(currentNode.getWorldPoint().getX() - end.getX());
                int dy = Math.abs(currentNode.getWorldPoint().getY() - end.getY());
                if (dx <= 1 && dy <= 1) {
                    // We're adjacent to the lamp on the correct plane
                    List<WorldPoint> path = currentNode.getPath();
                    // Don't add the lamp itself, just get to adjacent tile
                    if (path.size() > MAX_PATH_LENGTH) {
                        return new ArrayList<>();
                    }
                    return path;
                }
            }
            
            // Early termination for very short paths on same plane - they're likely optimal
            // Only apply this optimization for same-plane pathfinding and when we're very close
            if (!crossPlane && currentNode.getGCost() <= 3) {
                int directDistance = Math.abs(currentNode.getWorldPoint().getX() - end.getX()) + 
                                   Math.abs(currentNode.getWorldPoint().getY() - end.getY());
                if (directDistance <= 1) {
                    // We're immediately adjacent, this path is optimal
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

                float transportCost = 1;
                if (transports.containsKey(currentNode.getWorldPoint())) {
                    for (Transport transport : transports.get(currentNode.getWorldPoint())) {
                        // Use the transport's duration but add a small base cost to make
                        // pathfinder prefer direct routes when possible
                        transportCost = Math.max(transport.getDuration(), 2);
                        break;
                    }
                } else {
                    WorldPoint current = currentNode.getWorldPoint();
                    WorldPoint next = neighbor.getWorldPoint();
                    int dx = Math.abs(next.getX() - current.getX());
                    int dy = Math.abs(next.getY() - current.getY());
                    
                    if (dx == 1 && dy == 1) {
                        transportCost += 0.01;
                    }
                }
                float tentativeGCost = currentNode.getGCost() + transportCost;

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
        int dx = Math.abs(from.getX() - to.getX());
        int dy = Math.abs(from.getY() - to.getY());
        
        // Use pure Chebyshev distance without plane penalties
        // Plane differences should not be penalized since transport usage is often optimal
        int chebyshevDistance = Math.max(dx, dy);

        return chebyshevDistance;
    }
}