package com.dklamps;

import com.dklamps.enums.Area;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.HintArrowType;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;

@Slf4j
public class DKLampsStateManager {

    private final Client client;
    private final DKLampsStatsTracker statsTracker;

    @Getter
    private final Map<Lamp, LampStatus> lampStatuses = new EnumMap<>(Lamp.class);
    @Getter
    private final Map<WorldPoint, GameObject> spawnedLamps = new HashMap<>();
    @Getter
    private final Set<WallObject> doors = new HashSet<>();
    @Getter
    private final Set<GameObject> stairs = new HashSet<>();
    @Getter
    private final Set<WorldPoint> informativeStairs = new HashSet<>();
    @Getter
    private GameObject wireMachine;
    @Getter
    private int wireRespawnTick = -1;

    private Set<Lamp> previouslyBrokenLamps = new HashSet<>();
    @Getter
    private Area currentArea = null;
    private Area lastArea = null;
    private String lastHintDirection = null;
    private String lastHintFloor = null;
    private final Set<Lamp> fixedLamps = new HashSet<>();
    @Getter
    private final Set<Lamp> brokenLamps = new HashSet<>();
    private boolean isLampFixed = false;
    private int gameTickCounter = 0;
    private static final int HEAVY_OPERATIONS_INTERVAL = 5;

    public DKLampsStateManager(Client client, DKLampsStatsTracker statsTracker) {
        this.client = client;
        this.statsTracker = statsTracker;
        resetLampStatuses();
    }

    public void shutDown() {
        spawnedLamps.clear();
        doors.clear();
        stairs.clear();
        informativeStairs.clear();
        previouslyBrokenLamps.clear();
        fixedLamps.clear();
        brokenLamps.clear();

        wireMachine = null;
        lastArea = null;
        gameTickCounter = 0;
        isLampFixed = false;
        wireRespawnTick = -1;
    }

    public void onGameTick() {
        if (client.getLocalPlayer() == null) {
            return;
        }

        gameTickCounter++;
        boolean isHeavyOperationTick = (gameTickCounter % HEAVY_OPERATIONS_INTERVAL) == 0;

        currentArea = DKLampsHelper.getArea(client.getLocalPlayer().getWorldLocation());
        if (currentArea == null) {
            lastArea = null;
            return;
        }

        if (!currentArea.equals(lastArea)) {
            lastArea = currentArea;
            previouslyBrokenLamps.clear();
            return;
        }

        int lampVarbit = client.getVarbitValue(DKLampsConstants.DORGESHKAAN_LAMPS_VARBIT);

        brokenLamps.clear();
        brokenLamps.addAll(DKLampsHelper.getBrokenLamps(lampVarbit, currentArea));

        if (!brokenLamps.equals(previouslyBrokenLamps)) {
            fixedLamps.clear();
            fixedLamps.addAll(previouslyBrokenLamps);
            fixedLamps.removeAll(brokenLamps);

            if (!fixedLamps.isEmpty()) {
                statsTracker.incrementLampsFixed();
                isLampFixed = true;
            }
        }

        Map<Lamp, LampStatus> newStatuses = DKLampsHelper.updateLampStatuses(
                lampStatuses,
                brokenLamps,
                isLampFixed,
                currentArea);

        if (isLampFixed) {
            detectRuneLiteHintArrow(newStatuses);
            isLampFixed = false;
        }

        lampStatuses.clear();
        lampStatuses.putAll(newStatuses);

        if (isHeavyOperationTick) {
            detectInformativeStairs();
        }

        previouslyBrokenLamps.clear();
        previouslyBrokenLamps.addAll(brokenLamps);
        lastArea = currentArea;
    }

    public void onGameObjectSpawned(GameObject gameObject) {
        if (DKLampsHelper.isLamp(gameObject.getId())) {
            spawnedLamps.put(gameObject.getWorldLocation(), gameObject);
        } else if (DKLampsConstants.STAIR_IDS.contains(gameObject.getId())) {
            stairs.add(gameObject);
        } else if (DKLampsConstants.WIRE_MACHINE_IDS.contains(gameObject.getId())) {
            wireMachine = gameObject;
            if (gameObject.getId() == DKLampsConstants.WIRE_MACHINE_INACTIVE) {
                wireRespawnTick = client.getTickCount() + DKLampsConstants.WIRE_RESPAWN_TICKS;
            } else {
                wireRespawnTick = -1;
            }
        }
    }

    public void onGameObjectDespawned(GameObject gameObject) {
        if (DKLampsHelper.isLamp(gameObject.getId())) {
            spawnedLamps.remove(gameObject.getWorldLocation());
        } else if (DKLampsConstants.STAIR_IDS.contains(gameObject.getId())) {
            stairs.remove(gameObject);
        } else if (DKLampsConstants.WIRE_MACHINE_IDS.contains(gameObject.getId())) {
            wireMachine = null;
            wireRespawnTick = -1;
        }
    }

    public void onWallObjectSpawned(WallObject wallObject) {
        if (DKLampsConstants.DOOR_IDS.contains(wallObject.getId())) {
            doors.add(wallObject);
        }
    }

    public void onWallObjectDespawned(WallObject wallObject) {
        if (DKLampsConstants.DOOR_IDS.contains(wallObject.getId())) {
            doors.remove(wallObject);
        }
    }

    public void onGameStateChanged(GameState gameState) {
        if (gameState == GameState.LOADING ||
                gameState == GameState.LOGIN_SCREEN ||
                gameState == GameState.HOPPING) {

            spawnedLamps.clear();
            doors.clear();
            stairs.clear();
            informativeStairs.clear();
            wireMachine = null;
            wireRespawnTick = -1;
            currentArea = null;

            if (gameState != GameState.LOADING) {
                resetLampStatuses();
            }
        }
    }

    public void onChatMessage(ChatMessage chatMessage) {
        ChatMessageType chatMessageType = chatMessage.getType();
        String message = chatMessage.getMessage();

        if (chatMessageType == ChatMessageType.GAMEMESSAGE && 
                message.contains(DKLampsConstants.NEARBY_LAMP_CHAT_MESSAGE)) {
            log.info("Nearby lamp chat message detected: {}", message);
            parseNearbyLampChatMessage(message);
        }
    }

    private void parseNearbyLampChatMessage(String message) {
        Matcher matcher = DKLampsConstants.NEARBY_LAMP_PATTERN.matcher(message);
        if (matcher.find()) {
            log.info("Parsed nearby lamp hint: direction={}, floor={}",
                    matcher.group(1), matcher.group(2));
            lastHintDirection = matcher.group(1);
            lastHintFloor = matcher.group(2);
        }
    }

    private void resetLampStatuses() {
        lampStatuses.clear();
        for (Lamp lamp : Lamp.values()) {
            lampStatuses.put(lamp, LampStatus.UNKNOWN);
        }
    }



    private void detectInformativeStairs() {
        informativeStairs.clear();

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (playerLocation == null || stairs.isEmpty()) {
            return;
        }

        Map<Area, WorldPoint> closestStairForArea = new EnumMap<>(Area.class);
        Map<Area, Integer> minDistanceForArea = new EnumMap<>(Area.class);

        for (GameObject stair : stairs) {
            WorldPoint stairLocation = stair.getWorldLocation();
            int distanceToPlayer = playerLocation.distanceTo(stairLocation);

            Set<WorldPoint> destinations = DKLampsHelper.getStairDestinations(stairLocation);
            
            for (WorldPoint destination : destinations) {
                Area targetArea = DKLampsHelper.getArea(destination);

                if (targetArea != null && DKLampsHelper.areaHasUnknownLamps(targetArea, lampStatuses)) {
                    int currentMinDistance = minDistanceForArea.getOrDefault(targetArea, Integer.MAX_VALUE);

                    if (distanceToPlayer < currentMinDistance) {
                        minDistanceForArea.put(targetArea, distanceToPlayer);
                        closestStairForArea.put(targetArea, stairLocation);
                    }
                }
            }
        }

        informativeStairs.addAll(closestStairForArea.values());
    }

    private void detectRuneLiteHintArrow(Map<Lamp, LampStatus> newStatuses) {
        boolean currentlyHasHintArrow = client.hasHintArrow();
        WorldPoint currentHintArrowPoint = null;

        if (currentlyHasHintArrow && client.getHintArrowType() == HintArrowType.COORDINATE) {
            currentHintArrowPoint = client.getHintArrowPoint();
            log.info("RuneLite hint arrow detected at {}", currentHintArrowPoint);
        }

        if (currentlyHasHintArrow && currentHintArrowPoint != null) {

            for (Lamp lamp : Lamp.values()) {
                WorldPoint lampLocation = lamp.getWorldPoint();
                if (lampLocation.getX() == currentHintArrowPoint.getX() &&
                    lampLocation.getY() == currentHintArrowPoint.getY()) {
                        
                    int playerPlane = client.getLocalPlayer().getWorldLocation().getPlane();
                    boolean planeMatches = false;

                    if (lastHintFloor.equals("upstairs") && lampLocation.getPlane() > playerPlane) {
                        planeMatches = true;
                    } else if (lastHintFloor.equals("downstairs") && lampLocation.getPlane() < playerPlane) {
                        planeMatches = true;
                    } else if (lastHintFloor.equals("on the same floor") && lampLocation.getPlane() == playerPlane) {
                        planeMatches = true;
                    }

                    if (planeMatches) {
                        newStatuses.put(lamp, LampStatus.BROKEN);
                        break;
                    }
                }
            }
        }
    }
}