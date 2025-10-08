package com.dklamps.pathfinder;

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
    private final SplitFlagMap map;
    private final Map<WorldPoint, List<Transport>> transports;

    public Pathfinder() throws IOException {
        this.map = SplitFlagMap.loadFromResources();
        this.transports = new HashMap<>();
        
        for (Transport transport : TransportLoader.loadTransports()) {
            transports.computeIfAbsent(transport.getOrigin(), k -> new ArrayList<>()).add(transport);
        }
    }

    public List<WorldPoint> findPath(WorldPoint start, WorldPoint end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<WorldPoint> closedSet = new HashSet<>();

        Node startNode = new Node(start);
        startNode.setGCost(0);
        startNode.setHCost(calculateHeuristic(start, end));
        startNode.setFCost(startNode.getHCost());
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node currentNode = openSet.poll();

            if (currentNode.getWorldPoint().equals(end)) {
                return currentNode.getPath();
            }

            closedSet.add(currentNode.getWorldPoint());

            for (Node neighbor : getNeighbors(currentNode)) {
                if (closedSet.contains(neighbor.getWorldPoint())) {
                    continue;
                }

                // A transport's cost is the distance between the origin and destination, 
                // plus a penalty to make walking preferred over short distances
                int transportCost = neighbor.getParent() != null && transports.containsKey(neighbor.getParent().getWorldPoint()) ? 5 : 1;
                int tentativeGCost = currentNode.getGCost() + transportCost;

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

        return new ArrayList<>(); // Return empty list if no path is found
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        WorldPoint p = node.getWorldPoint();

        // Walking neighbors
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                int checkX = p.getX() + dx;
                int checkY = p.getY() + dy;

                if (isWalkable(checkX, checkY, p.getPlane())) {
                    neighbors.add(new Node(new WorldPoint(checkX, checkY, p.getPlane())));
                }
            }
        }
        
        // Transport neighbors
        if (transports.containsKey(p)) {
            for (Transport transport : transports.get(p)) {
                neighbors.add(new Node(transport.getDestination()));
            }
        }

        return neighbors;
    }

    private boolean isWalkable(int x, int y, int z) {
        return !map.get(x, y, z, 1);
    }
    
    private int calculateHeuristic(WorldPoint from, WorldPoint to) {
        // Manhattan distance, but also factor in plane distance
        int distance = Math.abs(from.getX() - to.getX()) + Math.abs(from.getY() - to.getY());
        if (from.getPlane() != to.getPlane()) {
            distance += 10; // Add a penalty for changing floors
        }
        return distance;
    }
}