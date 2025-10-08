// src/main/java/com/dklamps/Pathfinder.java
package com.dklamps.pathfinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

public class Pathfinder {
    private final SplitFlagMap map;

    public Pathfinder(InputStream collisionMapStream) throws IOException {
        this.map = SplitFlagMap.loadFromStream(collisionMapStream);
    }

    /**
     * Finds the shortest path between two points using the A* algorithm.
     *
     * @param start The starting WorldPoint.
     * @param end   The ending WorldPoint.
     * @return A list of WorldPoints representing the path, or an empty list if no path is found.
     */
    public List<WorldPoint> findPath(WorldPoint start, WorldPoint end) {
        // A* algorithm implementation will go here
        // ...
        return List.of(); // Placeholder
    }

    /**
     * Checks if a tile is walkable.
     *
     * @param point The WorldPoint to check.
     * @return True if the tile is walkable, false otherwise.
     */
    public boolean isWalkable(WorldPoint point) {
        return map.isWalkable(point.getX(), point.getY(), point.getPlane());
    }
}