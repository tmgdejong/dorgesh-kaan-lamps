package com.dklamps;

import com.dklamps.enums.Area;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.HintArrowType;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;

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
    private Instant wireRespawnTime;
    
    private Set<Lamp> previouslyBrokenLamps = new HashSet<>();
    private Area lastArea = null;
    private WorldPoint previousHintArrowPoint = null;
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
        previousHintArrowPoint = null;
        gameTickCounter = 0;
        isLampFixed = false;
    }

    public void onGameTick() {
        if (client.getLocalPlayer() == null) {
            return;
        }

        gameTickCounter++;
        boolean isHeavyOperationTick = (gameTickCounter % HEAVY_OPERATIONS_INTERVAL) == 0;

        Area currentArea = DKLampsHelper.getArea(client.getLocalPlayer().getWorldLocation());
        if (currentArea == null) {
            lastArea = null;
            return;
        }

        if (!currentArea.equals(lastArea)) {
            lastArea = currentArea;
            previouslyBrokenLamps.clear();
            isLampFixed = true;
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

        Map<Lamp,LampStatus> newStatuses = DKLampsHelper.updateLampStatuses(
            lampStatuses, 
            brokenLamps, 
            isLampFixed, 
            currentArea);

        lampStatuses.clear();
        lampStatuses.putAll(newStatuses);
        isLampFixed = false;

        if (isHeavyOperationTick) {
            detectInformativeStairs();
        }

        detectRuneLiteHintArrow();
        
        previouslyBrokenLamps.clear();
        previouslyBrokenLamps.addAll(brokenLamps);
        lastArea = currentArea;
    }

    // --- Event Handlers ---

    public void onGameObjectSpawned(GameObject gameObject) {
        if (DKLampsHelper.isLamp(gameObject.getId())) {
            spawnedLamps.put(gameObject.getWorldLocation(), gameObject);
        } else if (DKLampsConstants.STAIR_IDS.contains(gameObject.getId())) {
            stairs.add(gameObject);
        } else if (DKLampsConstants.WIRE_MACHINE_IDS.contains(gameObject.getId())) {
            wireMachine = gameObject;
            if (gameObject.getId() == DKLampsConstants.WIRE_MACHINE_INACTIVE) {
                wireRespawnTime = Instant.now().plusMillis((DKLampsConstants.WIRE_RESPAWN_TICKS) * 600);
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
            
            // Clear transient game object state
            spawnedLamps.clear();
            doors.clear();
            stairs.clear();
            wireMachine = null;
            previousHintArrowPoint = null;

            if (gameState != GameState.LOADING) {
                // Clear persistent state on full logout/hop
                brokenLamps.clear();
                resetLampStatuses();
            }
        }
    }
    
    // --- Internal Logic ---

    private void resetLampStatuses() {
        lampStatuses.clear();
        for (Lamp lamp : Lamp.values()) {
            lampStatuses.put(lamp, LampStatus.UNKNOWN);
        }
    }
    
    private void detectInformativeStairs() {
        informativeStairs.clear();
        for (GameObject stair : stairs) {
            WorldPoint stairLocation = stair.getWorldLocation();
            for (int plane = 0; plane <= 2; plane++) {
                if (plane == stairLocation.getPlane()) {
                    continue;
                }
                WorldPoint targetPlane = new WorldPoint(stairLocation.getX(), stairLocation.getY(), plane);
                Area targetArea = DKLampsHelper.getArea(targetPlane);
                
                if (targetArea != null) {
                    Set<Lamp> lampsInArea = DKLampsHelper.getLampsByArea(targetArea);
                    boolean hasUnknownLamps = false;
                    for (Lamp lamp : lampsInArea) {
                        LampStatus status = lampStatuses.getOrDefault(lamp, LampStatus.UNKNOWN);
                        if (status == LampStatus.UNKNOWN) {
                            hasUnknownLamps = true;
                            break;
                        }
                    }
                    if (hasUnknownLamps) {
                        informativeStairs.add(stairLocation);
                        break;
                    }
                }
            }
        }
    }

    private void detectRuneLiteHintArrow() {
        boolean currentlyHasHintArrow = client.hasHintArrow();
        WorldPoint currentHintArrowPoint = null;
        
        if (currentlyHasHintArrow && client.getHintArrowType() == HintArrowType.COORDINATE) {
            currentHintArrowPoint = client.getHintArrowPoint();
        }
        
        if (currentlyHasHintArrow && currentHintArrowPoint != null && 
            !currentHintArrowPoint.equals(previousHintArrowPoint)) {
            
            for (Lamp lamp : Lamp.values()) {
                if (lamp.getWorldPoint().equals(currentHintArrowPoint)) {
                    log.info("RuneLite hint arrow detected pointing to lamp: {} at {}", 
                            lamp.name(), currentHintArrowPoint);
                    
                    lampStatuses.put(lamp, LampStatus.BROKEN);
                    brokenLamps.add(lamp);
                    break;
                }
            }
        }
        
        previousHintArrowPoint = currentHintArrowPoint;
    }
}