package com.dklamps;

import com.dklamps.enums.Area;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.runelite.api.coords.WorldPoint;

public class DKLampsHelper {
    private static final Map<Area, Set<Lamp>> LAMPS_BY_AREA;
    private static final Map<Integer, Set<Lamp>> LAMPS_BY_BIT_POSITION;
    private static final Map<Integer, Lamp> LAMPS_BY_OBJECT_ID;
    private static final Map<WorldPoint, Lamp> LAMPS_BY_WORLD_POINT;

    private static final Map<Area, Set<Integer>> BIT_POSITIONS_BY_AREA;
    private static final Map<Area, Integer> MAX_BIT_BY_AREA;

    static {
        ImmutableMap.Builder<Area, Set<Lamp>> areaBuilder = new ImmutableMap.Builder<>();
        for (Area area : Area.values()) {
            Set<Lamp> lampsInArea = Sets.immutableEnumSet(
                    java.util.Arrays.stream(Lamp.values())
                            .filter(lamp -> lamp.getArea() == area)
                            .collect(Collectors.toSet()));
            areaBuilder.put(area, lampsInArea);
        }
        LAMPS_BY_AREA = areaBuilder.build();

        ImmutableMap.Builder<Integer, Set<Lamp>> bitBuilder = new ImmutableMap.Builder<>();
        for (int i = 0; i < 32; i++) {
            final int bit = i;
            Set<Lamp> lampsForBit = Sets.immutableEnumSet(
                    java.util.Arrays.stream(Lamp.values())
                            .filter(lamp -> lamp.getBitPosition() == bit)
                            .collect(Collectors.toSet()));
            if (!lampsForBit.isEmpty()) {
                bitBuilder.put(i, lampsForBit);
            }
        }
        LAMPS_BY_BIT_POSITION = bitBuilder.build();

        ImmutableMap.Builder<Integer, Lamp> idBuilder = new ImmutableMap.Builder<>();
        ImmutableMap.Builder<WorldPoint, Lamp> wpBuilder = new ImmutableMap.Builder<>();
        for (Lamp lamp : Lamp.values()) {
            idBuilder.put(lamp.getObjectId(), lamp);
            wpBuilder.put(lamp.getWorldPoint(), lamp);
        }
        LAMPS_BY_OBJECT_ID = idBuilder.build();
        LAMPS_BY_WORLD_POINT = wpBuilder.build();

        ImmutableMap.Builder<Area, Set<Integer>> bitPosBuilder = new ImmutableMap.Builder<>();
        ImmutableMap.Builder<Area, Integer> maxBitBuilder = new ImmutableMap.Builder<>();
        for (Area area : Area.values()) {
            Set<Lamp> lamps = LAMPS_BY_AREA.get(area);
            if (lamps == null || lamps.isEmpty()) {
                bitPosBuilder.put(area, Collections.emptySet());
                maxBitBuilder.put(area, 0);
                continue;
            }
            
            Set<Integer> bits = lamps.stream()
                    .map(Lamp::getBitPosition)
                    .collect(Collectors.toSet());
            bitPosBuilder.put(area, bits);

            int maxBit = lamps.stream()
                    .mapToInt(Lamp::getBitPosition)
                    .max()
                    .orElse(0);
            maxBitBuilder.put(area, maxBit);
        }
        BIT_POSITIONS_BY_AREA = bitPosBuilder.build();
        MAX_BIT_BY_AREA = maxBitBuilder.build();
    }

    public static Set<Lamp> getLampsByArea(Area area) {
        return LAMPS_BY_AREA.getOrDefault(area, Collections.emptySet());
    }

    public static Area getArea(WorldPoint worldPoint) {
        int plane = worldPoint.getPlane();
        int y = worldPoint.getY();
        int x = worldPoint.getX();

        if (x < DKLampsConstants.DK_WEST_VALUE || x > DKLampsConstants.DK_EAST_VALUE
                || y > DKLampsConstants.DK_NORTH_VALUE || y < DKLampsConstants.DK_SOUTH_VALUE) {
            return null;
        }

        if (plane == 0) {
            return y >= DKLampsConstants.WORLD_MAP_LINE ? Area.P0_N : Area.P0_S;
        } else if (plane == 1) {
            return y >= DKLampsConstants.WORLD_MAP_LINE ? Area.P1_N : Area.P1_S;
        } else if (plane == 2) {
            return y >= DKLampsConstants.WORLD_MAP_LINE ? Area.P2_N : Area.P2_S;
        }
        return null;
    }

    public static Lamp getLamp(int objectId) {
        return LAMPS_BY_OBJECT_ID.get(objectId);
    }

    public static Lamp getLamp(WorldPoint worldPoint) {
        return LAMPS_BY_WORLD_POINT.get(worldPoint);
    }

    public static boolean isLamp(int objectId) {
        return LAMPS_BY_OBJECT_ID.containsKey(objectId);
    }

    public static Set<Lamp> getBrokenLamps(int varbitValue, Area currentArea) {
        Set<Lamp> brokenLamps = Sets.newEnumSet(Collections.emptySet(), Lamp.class);
        if (currentArea == null) {
            return brokenLamps;
        }

        Area oppositeArea = currentArea.getOpposite();
        Set<Lamp> lampsInCurrentArea = getLampsByArea(currentArea);
        Set<Lamp> lampsInOppositeArea = getLampsByArea(oppositeArea);

        int maxBitPosition = MAX_BIT_BY_AREA.getOrDefault(currentArea, 0);

        for (int i = 0; i <= maxBitPosition; i++) {
            if ((varbitValue & (1 << i)) != 0) {
                Set<Lamp> lampsForBit = LAMPS_BY_BIT_POSITION.getOrDefault(i, Collections.emptySet());

                Lamp lampToAdd = null;
                for (Lamp lamp : lampsForBit) {
                    if (lampsInCurrentArea.contains(lamp)) {
                        lampToAdd = lamp;
                        break;
                    }

                    if (lampsInOppositeArea.contains(lamp)) {
                        lampToAdd = lamp;
                    }
                }

                if (lampToAdd != null) {
                    brokenLamps.add(lampToAdd);
                }
            }
        }

        return brokenLamps;
    }

    public static Set<Lamp> getValidOppositeLamps(Area currentArea) {
        Area oppositeArea = currentArea.getOpposite();
        if (oppositeArea == null) {
            return Collections.emptySet();
        }

        Set<Lamp> lampsInOppositeArea = getLampsByArea(oppositeArea);

        int maxBitPosition = MAX_BIT_BY_AREA.getOrDefault(currentArea, 0);
        Set<Integer> currentAreaBitPositions = BIT_POSITIONS_BY_AREA.getOrDefault(currentArea, Collections.emptySet());

        return lampsInOppositeArea.stream()
                .filter(lamp -> lamp.getBitPosition() <= maxBitPosition
                            && !currentAreaBitPositions.contains(lamp.getBitPosition()))
                .collect(Collectors.toSet());
    }

    public static Map<Lamp, LampStatus> updateLampStatuses(
            Map<Lamp, LampStatus> lampStatuses,
            Set<Lamp> brokenLamps,
            boolean isLampsFixed,
            Area currentArea) {

        Map<Lamp, LampStatus> updatedStatuses = new EnumMap<>(lampStatuses);

        Set<Lamp> lampsInCurrentArea = getLampsByArea(currentArea);
        for (Lamp lamp : lampsInCurrentArea) {
            updatedStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.WORKING);
        }

        Area oppositeArea = currentArea.getOpposite();
        if (oppositeArea != null) {
            Set<Lamp> validOppositeLamps = getValidOppositeLamps(currentArea);
            for (Lamp lamp : validOppositeLamps) {
                updatedStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.WORKING);
            }
            
            if (isLampsFixed) {
                for (Lamp lamp : Lamp.values()) {
                    Area lampArea = lamp.getArea();
                    if (lampArea != currentArea && lampArea != oppositeArea) {
                        LampStatus status = updatedStatuses.get(lamp);
                        if (status == LampStatus.WORKING) {
                            updatedStatuses.put(lamp, LampStatus.UNKNOWN);
                        }
                    }
                }
            }
        }
        autoFixAllLampsIfNeeded(updatedStatuses);
        
        return updatedStatuses;
    }

    public static int countBrokenLamps(Map<Lamp, LampStatus> lampStatuses) {
        int totalBroken = 0;
        for (LampStatus status : lampStatuses.values()) {
            if (status == LampStatus.BROKEN) {
                totalBroken++;
            }
        }
        return totalBroken;
    }

    public static void autoFixAllLampsIfNeeded(Map<Lamp, LampStatus> lampStatuses) {
        int totalBroken = countBrokenLamps(lampStatuses);

        if (totalBroken == 10) {
            for (Lamp lamp : Lamp.values()) {
                if (lampStatuses.get(lamp) != LampStatus.BROKEN) {
                    lampStatuses.put(lamp, LampStatus.WORKING);
                }
            }
        }
    }

    public static int countUnknownLampsInArea(Map<Lamp, LampStatus> lampStatuses, Area area) {
        if (area == null) {
            return 0;
        }
        
        int unknownCount = 0;
        Set<Lamp> lampsInArea = getLampsByArea(area);
        
        for (Lamp lamp : lampsInArea) {
            if (lampStatuses.getOrDefault(lamp, LampStatus.UNKNOWN) == LampStatus.UNKNOWN) {
                unknownCount++;
            }
        }
        return unknownCount;
    }

    public static boolean isInBankArea(WorldPoint playerLocation) {
        return playerLocation.distanceTo(DKLampsConstants.BANK_LOCATION) <= 5;
    }
}
