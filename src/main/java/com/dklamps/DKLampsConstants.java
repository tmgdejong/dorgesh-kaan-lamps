package com.dklamps;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.coords.WorldPoint;
import java.util.Set;
import java.util.regex.Pattern;

public final class DKLampsConstants {
    private DKLampsConstants() {}

    public static final int DORGESHKAAN_LAMPS_VARBIT = 4038;
    public static final int WIRE_RESPAWN_TICKS = 8;

    public static final Pattern TOTAL_LAMPS_PATTERN = Pattern.compile("Total lights fixed: (?:<col=[^>]*>)?([0-9,]+)");

    // Coordinates
    public static final int WORLD_MAP_LINE = 5312; // Y
    public static final int DK_WEST_VALUE = 2688; // X
    public static final int DK_EAST_VALUE = 2751; // X
    public static final int DK_NORTH_VALUE = 5375; // Y
    public static final int DK_SOUTH_VALUE = 5248; // Y

    // Object IDs
    public static final int TELEPORT_SPHERE_ID = 10972;
    public static final int LIGHT_BULB_EMPTY = 10980;
    public static final int LIGHT_BULB_WORKING = 10973;
    public static final int WIRE_MACHINE_ACTIVE = 22730;
    public static final int WIRE_MACHINE_INACTIVE = 22731;

    public static final Set<Integer> WIRE_MACHINE_IDS = ImmutableSet.of(
            WIRE_MACHINE_ACTIVE,
            WIRE_MACHINE_INACTIVE
    );

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
            22942
    );
    
    // Bank location in Dorgesh-Kaan
    public static final WorldPoint WIRE_MACHINE_LOCATION = new WorldPoint(2722, 5278, 0);
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