package com.dklamps;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.coords.WorldPoint;
import java.util.Set;

public final class ObjectIDs {
    private ObjectIDs() {}

    public static final int TELEPORT_SPHERE_ID = 10972;
    public static final int LIGHT_BULB_EMPTY = 10980;
    public static final int LIGHT_BULB_WORKING = 10973;
    public static final int WIRE_MACHINE_ACTIVE = 22730;
    public static final int WIRE_MACHINE_INACTIVE = 22731;

    public static final Set<Integer> WIRE_MACHINE_IDS = ImmutableSet.of(
            WIRE_MACHINE_ACTIVE,
            WIRE_MACHINE_INACTIVE);

    public static final Set<Integer> DOOR_IDS = ImmutableSet.of(
            22914,
            22921

    );

    public static final Set<Integer> STAIR_IDS = ImmutableSet.of(
            22931,
            22932,
            22933,
            22934,
            22935,
            22936,
            22937,
            22938,
            22939,
            22940,
            22941,
            22942);
    
    // Bank location in Dorgesh-Kaan
    public static final WorldPoint BANK_LOCATION = new WorldPoint(2702, 5349, 0);
    public static final Set<WorldPoint> BANK_TILES = ImmutableSet.of(
            new WorldPoint(2701, 5347, 0),
            new WorldPoint(2701, 5348, 0),
            new WorldPoint(2701, 5349, 0),
            new WorldPoint(2701, 5350, 0),
            new WorldPoint(2701, 5351, 0),
            new WorldPoint(2701, 5352, 0),
            new WorldPoint(2702, 5347, 0),
            new WorldPoint(2702, 5348, 0),
            new WorldPoint(2702, 5349, 0),
            new WorldPoint(2702, 5350, 0),
            new WorldPoint(2702, 5351, 0),
            new WorldPoint(2702, 5352, 0)
    );
}