package com.dklamps.pathfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data
public class Node implements Comparable<Node> {
    private final WorldPoint worldPoint;
    private Node parent;
    private int gCost; // Cost from start to current node
    private int hCost; // Heuristic cost from current node to end
    private int fCost; // gCost + hCost

    public Node(WorldPoint worldPoint) {
        this.worldPoint = worldPoint;
    }

    public List<WorldPoint> getPath() {
        List<WorldPoint> path = new ArrayList<>();
        Node currentNode = this;
        while (currentNode != null) {
            path.add(currentNode.getWorldPoint());
            currentNode = currentNode.getParent();
        }
        Collections.reverse(path);
        return path;
    }
    
    @Override
    public int compareTo(Node other) {
        return Integer.compare(this.fCost, other.fCost);
    }
}
