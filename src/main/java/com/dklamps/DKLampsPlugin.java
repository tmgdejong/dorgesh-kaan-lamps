package com.dklamps;

import com.dklamps.enums.Area;
import com.dklamps.enums.InventoryState;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import com.dklamps.pathfinder.Pathfinder;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.HintArrowType;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;


@Slf4j
@PluginDescriptor(name = "Dorgesh-Kaan Lamps")
public class DKLampsPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private DKLampsConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DKLampsOverlay overlay;

    @Inject
    private TeleportOverlay teleportOverlay;

    @Inject
    private StatsOverlay statsOverlay;

    @Inject
    private ClientToolbar clientToolbar;

    private DKLampsPanel panel;
    private NavigationButton navButton;
    private Pathfinder pathfinder;
    private ExecutorService pathfindingExecutor;
    @Getter
    private List<WorldPoint> shortestPath = new ArrayList<>();

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
    private Set<Lamp> brokenLamps = new HashSet<>();
    private Set<Lamp> previouslyBrokenLamps = new HashSet<>();
    private Area lastArea = null;
    @Getter
    private Instant wireRespawnTime;

    @Getter
    private final Map<Lamp, LampStatus> lampStatuses = new EnumMap<>(Lamp.class);

    public Pathfinder getPathfinder() {
        return pathfinder;
    }

    @Getter
    private Lamp lastLoggedClosestLamp = null;
    private CompletableFuture<Void> currentClosestLampTask;
    private long lastClosestLampCalculation = 0;
    private static final long CLOSEST_LAMP_COOLDOWN_MS = 600;

    @Getter
    private int lampsFixed = 0;
    @Getter
    private int lampsPerHr = 0;
    @Getter
    private int totalLampsFixed = 0;
    @Getter
    private int closestDistance = 0;
    private Instant start;
    
    @Getter
    private String currentTargetType = "Lamp";
    
    private WorldPoint previousHintArrowPoint = null;
    private boolean previouslyHadHintArrow = false;
    
    private final Set<Lamp> reusableFixedLamps = new HashSet<>();
    private final Set<Lamp> reusableBrokenLamps = new HashSet<>();
    
    // Optimization flags
    private boolean needsLampStatusUpdate = false;
    private int gameTickCounter = 0;
    private static final int HEAVY_OPERATIONS_INTERVAL = 5; // Run heavy operations every 5 ticks

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        overlayManager.add(teleportOverlay);
        overlayManager.add(statsOverlay);

        panel = new DKLampsPanel(this);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/light_orb_32x32.png");
        navButton = NavigationButton.builder()
                .tooltip("Dorgesh-Kaan Lamps")
                .icon(icon)
                .priority(7)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        resetLampStatuses();

        pathfindingExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DKLamps-Pathfinder");
            t.setDaemon(true);
            return t;
        });

        try {
            pathfinder = new Pathfinder();
        } catch (IOException e) {
            log.error("Failed to load pathfinder collision data", e);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Dorgesh-Kaan Lamps stopped!");
        overlayManager.remove(overlay);
        overlayManager.remove(teleportOverlay);
        overlayManager.remove(statsOverlay);

        clientToolbar.removeNavigation(navButton);

        if (currentClosestLampTask != null) {
            currentClosestLampTask.cancel(true);
        }
        if (pathfindingExecutor != null) {
            pathfindingExecutor.shutdown();
        }

        brokenLamps.clear();
        doors.clear();
        stairs.clear();
        wireMachine = null;
        
        reusableFixedLamps.clear();
        reusableBrokenLamps.clear();
        previouslyBrokenLamps.clear();

        lampsFixed = 0;
        lampsPerHr = 0;
        closestDistance = 0;
        start = null;
        gameTickCounter = 0;
        needsLampStatusUpdate = false;
    }

    private void resetLampStatuses() {
        lampStatuses.clear();
        for (Lamp lamp : Lamp.values()) {
            lampStatuses.put(lamp, LampStatus.UNKNOWN);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();

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

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject gameObject = event.getGameObject();
        if (DKLampsHelper.isLamp(gameObject.getId())) {
            spawnedLamps.remove(gameObject.getWorldLocation());
        } else if (DKLampsConstants.STAIR_IDS.contains(gameObject.getId())) {
            stairs.remove(gameObject);
        } else if (DKLampsConstants.WIRE_MACHINE_IDS.contains(gameObject.getId())) {
            wireMachine = null;
        }
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event) {
        WallObject wallObject = event.getWallObject();
        if (DKLampsConstants.DOOR_IDS.contains(wallObject.getId())) {
            doors.add(wallObject);
        }
    }

    @Subscribe
    public void onWallObjectDespawned(WallObjectDespawned event) {
        WallObject wallObject = event.getWallObject();
        if (DKLampsConstants.DOOR_IDS.contains(wallObject.getId())) {
            doors.remove(wallObject);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOADING ||
                gameStateChanged.getGameState() == GameState.LOGIN_SCREEN ||
                gameStateChanged.getGameState() == GameState.HOPPING) {
            spawnedLamps.clear();
            doors.clear();
            stairs.clear();
            wireMachine = null;
            
            previousHintArrowPoint = null;
            previouslyHadHintArrow = false;

            if (gameStateChanged.getGameState() != GameState.LOADING) {
                brokenLamps.clear();
                lastLoggedClosestLamp = null;
                resetLampStatuses();
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        String message = chatMessage.getMessage();
        Matcher matcher = DKLampsConstants.TOTAL_LAMPS_PATTERN.matcher(message);

        if (message.contains("Total lights fixed:")) {
            if (matcher.find()) {
                try {
                    String numberStr = matcher.group(1).replace(",", "");
                    int number = Integer.parseInt(numberStr);

                    if (number > 0 && number > totalLampsFixed) {
                        totalLampsFixed = number;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse number from chat message: {}", matcher.group(1));
                }
            } else {
                log.debug("Could not match pattern in message: {}", message);
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (client.getLocalPlayer() == null) {
            return;
        }

        gameTickCounter++;
        boolean isHeavyOperationTick = (gameTickCounter % HEAVY_OPERATIONS_INTERVAL) == 0;

        Area currentArea = DKLampsHelper.getArea(client.getLocalPlayer().getWorldLocation());
        if (currentArea == null) {
            lastArea = null;
            client.clearHintArrow();
            return;
        }

        if (!currentArea.equals(lastArea)) {
            lastArea = currentArea;
            previouslyBrokenLamps.clear(); // Reuse existing set instead of creating new one
            needsLampStatusUpdate = true;
            return;
        }

        int lampVarbit = client.getVarbitValue(DKLampsConstants.DORGESHKAAN_LAMPS_VARBIT);
        
        // Reuse collection instead of creating new one
        reusableBrokenLamps.clear();
        reusableBrokenLamps.addAll(DKLampsHelper.getBrokenLamps(lampVarbit, currentArea));
        
        // Check if broken lamps actually changed to avoid unnecessary work
        if (!reusableBrokenLamps.equals(brokenLamps)) {
            // Reuse collection for fixed lamps calculation
            reusableFixedLamps.clear();
            reusableFixedLamps.addAll(previouslyBrokenLamps);
            reusableFixedLamps.removeAll(reusableBrokenLamps);

            if (!reusableFixedLamps.isEmpty()) {
                int lampsFixedThisTick = reusableFixedLamps.size();
                for (int i = 0; i < lampsFixedThisTick; i++) {
                    incrementLampsFixed();
                }
                needsLampStatusUpdate = true;
            }
            
            // Update broken lamps
            brokenLamps.clear();
            brokenLamps.addAll(reusableBrokenLamps);
            needsLampStatusUpdate = true;
        }

        // Only update lamp statuses when needed or on heavy operation ticks
        if (needsLampStatusUpdate || isHeavyOperationTick) {
            updateLampStatuses(currentArea);
            needsLampStatusUpdate = false;
        }

        // Only update UI when visible and on heavy operation ticks to reduce overhead
        if (panel.isVisible() && isHeavyOperationTick) {
            panel.update();
        }

        // Run expensive operations less frequently
        if (isHeavyOperationTick) {
            detectInformativeStairs();
        }

        detectRuneLiteHintArrow();
        client.clearHintArrow();

        logClosestLamp();

        // Reuse collection instead of creating new one
        previouslyBrokenLamps.clear();
        previouslyBrokenLamps.addAll(brokenLamps);
        lastArea = currentArea;
    }

    private void updateLampStatuses(Area currentArea) {
        // Update current area lamp statuses
        Set<Lamp> lampsInCurrentArea = DKLampsHelper.getLampsByArea(currentArea);
        for (Lamp lamp : lampsInCurrentArea) {
            lampStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.WORKING);
        }

        // Update opposite area lamp statuses
        Area oppositeArea = currentArea.getOpposite();
        if (oppositeArea != null) {
            Set<Lamp> validOppositeLamps = DKLampsHelper.getValidOppositeLamps(currentArea);
            for (Lamp lamp : validOppositeLamps) {
                lampStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.WORKING);
            }
            
            // Reset statuses for lamps outside current and opposite areas when lamps are fixed
            if (!reusableFixedLamps.isEmpty()) {
                for (Map.Entry<Lamp, LampStatus> entry : lampStatuses.entrySet()) {
                    Area lampArea = entry.getKey().getArea();
                    if (lampArea != currentArea && lampArea != oppositeArea && entry.getValue() == LampStatus.WORKING) {
                        lampStatuses.put(entry.getKey(), LampStatus.UNKNOWN);
                    }
                }
            }
        }

        // Optimize: Count broken lamps without stream for better performance
        int totalBroken = 0;
        for (LampStatus status : lampStatuses.values()) {
            if (status == LampStatus.BROKEN) {
                totalBroken++;
            }
        }

        // If we have exactly 10 broken lamps, mark all others as working
        if (totalBroken == 10) {
            for (Lamp lamp : Lamp.values()) {
                if (lampStatuses.get(lamp) != LampStatus.BROKEN) {
                    lampStatuses.put(lamp, LampStatus.WORKING);
                }
            }
        }
    }

    private void logClosestLamp() {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (playerLocation == null || pathfinder == null || pathfindingExecutor == null) {
            return;
        }

        InventoryState inventoryState = InventoryState.NO_LIGHT_BULBS.getInventoryState(client);

        WorldPoint targetLocation = null;
        String targetType = null;

        switch (inventoryState) {
            case NO_LIGHT_BULBS:
                targetLocation = DKLampsConstants.BANK_LOCATION;
                targetType = "Bank";
                break;

            case ONLY_EMPTY_BULBS:
                if (wireMachine != null) {
                    targetLocation = wireMachine.getWorldLocation();
                    targetType = "Wire Machine";
                } else {
                    shortestPath.clear();
                    currentTargetType = "Wire Machine";
                    return;
                }
                break;

            case HAS_WORKING_BULBS:
                currentTargetType = "Lamp";
                findClosestBrokenLamp();
                return;
        }

        currentTargetType = targetType;
        if (targetLocation != null) {
            calculatePathToTarget(targetLocation, targetType, playerLocation);
        }
    }

    private void findClosestBrokenLamp() {
        // Reuse collection instead of creating new one
        reusableBrokenLamps.clear();
        for (Map.Entry<Lamp, LampStatus> entry : lampStatuses.entrySet()) {
            if (entry.getValue() == LampStatus.BROKEN) {
                reusableBrokenLamps.add(entry.getKey());
            }
        }
        final Set<Lamp> allBrokenLamps = reusableBrokenLamps;

        if (allBrokenLamps.isEmpty()) {
            if (lastLoggedClosestLamp != null) {
                log.info("No broken lamps found");
                lastLoggedClosestLamp = null;
            }
            shortestPath.clear();
            return;
        }

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClosestLampCalculation < CLOSEST_LAMP_COOLDOWN_MS) {
            return;
        }
        lastClosestLampCalculation = currentTime;

        if (currentClosestLampTask != null && !currentClosestLampTask.isDone()) {
            currentClosestLampTask.cancel(true);
        }

        final WorldPoint playerPos = playerLocation;
        final Set<Lamp> lampsToCheck = allBrokenLamps; // Use the reused collection directly

        currentClosestLampTask = CompletableFuture.runAsync(() -> {
            try {
                Lamp closestLamp = null;
                List<WorldPoint> bestPath = null;
                int shortestPathLength = Integer.MAX_VALUE;

                for (Lamp lamp : lampsToCheck) {
                    // Check if task was cancelled
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    try {
                        List<WorldPoint> path = pathfinder.findPath(playerPos, lamp.getWorldPoint());
                        log.debug("Pathfinding to {}: found path with {} tiles", lamp.name(),
                                path != null ? path.size() : "null");

                        if (path != null && !path.isEmpty() && path.size() < shortestPathLength) {
                            closestLamp = lamp;
                            bestPath = new ArrayList<>(path);
                            shortestPathLength = path.size();
                            log.debug("New best path to {}: {} tiles", lamp.name(), path.size());
                        }
                    } catch (Exception e) {
                        log.debug("Failed to find path to lamp {}: {}", lamp.name(), e.getMessage());
                        int fallbackDistance = lamp.getWorldPoint().distanceTo(playerPos);
                        if (lamp.getWorldPoint().getPlane() != playerPos.getPlane()) {
                            fallbackDistance += 32;
                        }
                        if (fallbackDistance < shortestPathLength) {
                            closestLamp = lamp;
                            bestPath = new ArrayList<>();
                            bestPath.add(playerPos);
                            bestPath.add(lamp.getWorldPoint());
                            shortestPathLength = fallbackDistance;
                            log.info("Using fallback path to {} with distance {}", lamp.name(), fallbackDistance);
                        }
                    }
                }

                final Lamp finalClosestLamp = closestLamp;
                final List<WorldPoint> finalPath = bestPath;
                final int finalDistance = shortestPathLength;

                closestDistance = finalDistance;

                if (finalClosestLamp == null && !lampsToCheck.isEmpty()) {
                    Lamp fallbackLamp = lampsToCheck.stream()
                            .min((l1, l2) -> {
                                int dist1 = l1.getWorldPoint().distanceTo(playerPos);
                                int dist2 = l2.getWorldPoint().distanceTo(playerPos);
                                if (l1.getWorldPoint().getPlane() != playerPos.getPlane())
                                    dist1 += 32;
                                if (l2.getWorldPoint().getPlane() != playerPos.getPlane())
                                    dist2 += 32;
                                return Integer.compare(dist1, dist2);
                            })
                            .orElse(null);

                    if (fallbackLamp != null) {
                        List<WorldPoint> fallbackPath = new ArrayList<>();
                        fallbackPath.add(playerPos);
                        fallbackPath.add(fallbackLamp.getWorldPoint());
                        log.info("All pathfinding failed, using direct path fallback to {}", fallbackLamp.name());

                        final Lamp finalFallbackLamp = fallbackLamp;
                        final List<WorldPoint> finalFallbackPath = fallbackPath;

                        if (!Thread.currentThread().isInterrupted()) {
                            shortestPath = finalFallbackPath;
                            log.debug("Updated shortestPath with {} tiles for fallback lamp {}",
                                    shortestPath.size(), finalFallbackLamp.name());

                            if (!finalFallbackLamp.equals(lastLoggedClosestLamp)) {
                                WorldPoint lampLocation = finalFallbackLamp.getWorldPoint();
                                String planeInfo = lampLocation.getPlane() == playerPos.getPlane() ? "same floor"
                                        : String.format("floor %d (you're on floor %d)", lampLocation.getPlane(),
                                                playerPos.getPlane());

                                log.info(
                                        "Closest broken lamp (fallback): {} at ({}, {}) on {} - Direct distance: {} tiles",
                                        finalFallbackLamp.name(),
                                        lampLocation.getX(),
                                        lampLocation.getY(),
                                        planeInfo,
                                        lampLocation.distanceTo(playerPos));

                                lastLoggedClosestLamp = finalFallbackLamp;
                            }
                        }
                        return;
                    }
                }

                if (!Thread.currentThread().isInterrupted() && finalClosestLamp != null) {
                    shortestPath = finalPath != null ? finalPath : new ArrayList<>();
                    log.debug("Updated shortestPath with {} tiles for lamp {}",
                            shortestPath.size(), finalClosestLamp.name());
                    if (!finalClosestLamp.equals(lastLoggedClosestLamp)) {
                        WorldPoint lampLocation = finalClosestLamp.getWorldPoint();
                        String planeInfo = lampLocation.getPlane() == playerPos.getPlane() ? "same floor"
                                : String.format("floor %d (you're on floor %d)", lampLocation.getPlane(),
                                        playerPos.getPlane());

                        log.info("Closest broken lamp (pathfinding): {} at ({}, {}) on {} - Path length: {} tiles",
                                finalClosestLamp.name(),
                                lampLocation.getX(),
                                lampLocation.getY(),
                                planeInfo,
                                finalDistance);

                        lastLoggedClosestLamp = finalClosestLamp;
                    }
                }
            } catch (Exception e) {
                log.error("Error during closest lamp calculation", e);
            }
        }, pathfindingExecutor).exceptionally(throwable -> {
            if (!(throwable instanceof java.util.concurrent.CancellationException)) {
                log.error("Closest lamp calculation failed", throwable);
            }
            return null;
        });
    }

    private void detectInformativeStairs() {
        informativeStairs.clear(); // Clear previous results
        
        for (GameObject stair : stairs) {
            WorldPoint stairLocation = stair.getWorldLocation();
            
            for (int plane = 0; plane <= 2; plane++) {
                if (plane == stairLocation.getPlane()) {
                    continue; // Skip current plane
                }
                
                WorldPoint targetPlane = new WorldPoint(stairLocation.getX(), stairLocation.getY(), plane);
                com.dklamps.enums.Area targetArea = DKLampsHelper.getArea(targetPlane);
                
                if (targetArea != null) {
                    // Check if this area has any lamps with unknown status
                    Set<Lamp> lampsInArea = DKLampsHelper.getLampsByArea(targetArea);
                    boolean hasUnknownLamps = false;
                    for (Lamp lamp : lampsInArea) {
                        LampStatus status = lampStatuses.getOrDefault(lamp, LampStatus.UNKNOWN);
                        if (status == LampStatus.UNKNOWN) {
                            hasUnknownLamps = true;
                            break; // Early exit when we find unknown lamps
                        }
                    }
                    if (hasUnknownLamps) {
                        informativeStairs.add(stairLocation);
                        break; // No need to check other planes for this stair
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
        
        // Check if this is a new hint arrow pointing to a different location
        if (currentlyHasHintArrow && currentHintArrowPoint != null && 
            !currentHintArrowPoint.equals(previousHintArrowPoint)) {
            
            // Check if the hint arrow is pointing to a lamp location
            for (Lamp lamp : Lamp.values()) {
                if (lamp.getWorldPoint().equals(currentHintArrowPoint)) {
                    // RuneLite is pointing to a lamp - this lamp must be broken!
                    log.info("RuneLite hint arrow detected pointing to lamp: {} at {}", 
                            lamp.name(), currentHintArrowPoint);
                    
                    // Update our lamp status to broken
                    lampStatuses.put(lamp, LampStatus.BROKEN);
                    
                    // Add to broken lamps set if not already there
                    brokenLamps.add(lamp);
                    break;
                }
            }
        }
        
        // Update previous state
        previousHintArrowPoint = currentHintArrowPoint;
        previouslyHadHintArrow = currentlyHasHintArrow;
    }

    void incrementLampsFixed() {
        ++lampsFixed;

        if (start == null) {
            start = Instant.now();
        }

        Duration elapsed = Duration.between(start, Instant.now());
        long elapsedMs = elapsed.toMillis();
        if (lampsFixed >= 3 && elapsedMs > 0) {
            lampsPerHr = (int) ((double) lampsFixed * Duration.ofHours(1).toMillis() / elapsedMs);
        }
    }

    @Provides
    DKLampsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DKLampsConfig.class);
    }

    public DKLampsConfig getConfig() {
        return config;
    }

    public Client getClient() {
        return client;
    }

    private boolean isInBankArea(WorldPoint playerLocation) {
        return playerLocation.distanceTo(DKLampsConstants.BANK_LOCATION) <= 5;
    }

    private void calculatePathToTarget(WorldPoint targetLocation, String targetType, WorldPoint playerLocation) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClosestLampCalculation < CLOSEST_LAMP_COOLDOWN_MS) {
            return;
        }
        lastClosestLampCalculation = currentTime;

        if (currentClosestLampTask != null && !currentClosestLampTask.isDone()) {
            currentClosestLampTask.cancel(true);
        }

        final WorldPoint playerPos = playerLocation;
        final WorldPoint target = targetLocation;

        if (isInBankArea(playerLocation) && targetType.equals("Bank")) {
            shortestPath.clear();
            return;
        }

        currentClosestLampTask = CompletableFuture.runAsync(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                List<WorldPoint> path = pathfinder.findPath(playerPos, target);

                if (!Thread.currentThread().isInterrupted()) {
                    if (path != null && !path.isEmpty()) {
                        shortestPath = new ArrayList<>(path);
                        closestDistance = path.size();
                        log.info("Path to {} calculated: {} tiles", targetType, path.size());
                    } else {
                        shortestPath = new ArrayList<>();
                        shortestPath.add(playerPos);
                        shortestPath.add(target);
                        closestDistance = target.distanceTo(playerPos);
                        log.info("Direct path to {} (fallback): {} tiles", targetType, closestDistance);
                    }
                }
            } catch (Exception e) {
                log.error("Error calculating path to {}: {}", targetType, e.getMessage());
                if (!Thread.currentThread().isInterrupted()) {
                    shortestPath = new ArrayList<>();
                    shortestPath.add(playerPos);
                    shortestPath.add(target);
                    closestDistance = target.distanceTo(playerPos);
                }
            }
        }, pathfindingExecutor).exceptionally(throwable -> {
            if (!(throwable instanceof java.util.concurrent.CancellationException)) {
                log.error("Path calculation to {} failed", targetType, throwable);
            }
            return null;
        });
    }

}
