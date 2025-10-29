package com.dklamps;

import com.dklamps.enums.InventoryState;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import com.dklamps.enums.TargetType;
import com.dklamps.pathfinder.Pathfinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class DKLampsNavigationManager {

    private final Client client;
    private final Pathfinder pathfinder;
    private final ExecutorService pathfindingExecutor;

    @Getter
    private List<WorldPoint> shortestPath = new ArrayList<>();
    @Getter
    private int closestDistance = 0;
    @Getter
    private TargetType currentTargetType = TargetType.NONE;
    
    private Lamp lastLoggedClosestLamp = null;
    private CompletableFuture<Void> currentClosestLampTask;
    private long lastClosestLampCalculation = 0;
    private static final long CLOSEST_LAMP_COOLDOWN_MS = 600;
    
    private final Set<Lamp> brokenLamps = new HashSet<>();

    public DKLampsNavigationManager(Client client, Pathfinder pathfinder, ExecutorService pathfindingExecutor) {
        this.client = client;
        this.pathfinder = pathfinder;
        this.pathfindingExecutor = pathfindingExecutor;
    }

    public void update(Map<Lamp, LampStatus> lampStatuses, 
                         InventoryState inventoryState, 
                         WorldPoint playerLocation, 
                         GameObject wireMachine) {
        
        if (playerLocation == null || pathfinder == null || pathfindingExecutor == null) {
            return;
        }

        WorldPoint targetLocation = null;

        switch (inventoryState) {
            case NO_LIGHT_BULBS:
                targetLocation = DKLampsConstants.BANK_LOCATION;
                currentTargetType = TargetType.BANK;
                break;

            case ONLY_EMPTY_BULBS:
                targetLocation = DKLampsConstants.WIRE_MACHINE_LOCATION;
                currentTargetType = TargetType.WIRING_MACHINE;
                break;

            case HAS_WORKING_BULBS:
                currentTargetType = TargetType.LAMP;
                findClosestBrokenLamp(lampStatuses, playerLocation);
                return;
            default:
                currentTargetType = TargetType.NONE;
                shortestPath.clear();
                return;
        }

        if (targetLocation != null) {
            calculatePathToTarget(targetLocation, currentTargetType, playerLocation);
        }
    }

    public void shutDown() {
        if (currentClosestLampTask != null) {
            currentClosestLampTask.cancel(true);
        }
    }

    private void findClosestBrokenLamp(Map<Lamp, LampStatus> lampStatuses, WorldPoint playerLocation) {
        brokenLamps.clear();
        for (Map.Entry<Lamp, LampStatus> entry : lampStatuses.entrySet()) {
            if (entry.getValue() == LampStatus.BROKEN) {
                brokenLamps.add(entry.getKey());
            }
        }
        final Set<Lamp> allBrokenLamps = brokenLamps;

        if (allBrokenLamps.isEmpty()) {
            if (lastLoggedClosestLamp != null) {
                log.info("No broken lamps found");
                lastLoggedClosestLamp = null;
            }
            shortestPath.clear();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClosestLampCalculation < CLOSEST_LAMP_COOLDOWN_MS) {
            return;
        }
        lastClosestLampCalculation = currentTime;

        if (currentClosestLampTask != null && !currentClosestLampTask.isDone()) {
            currentClosestLampTask.cancel(true);
        }

        final WorldPoint playerPos = playerLocation;
        final Set<Lamp> lampsToCheck = allBrokenLamps;

        currentClosestLampTask = CompletableFuture.runAsync(() -> {
            try {
                Lamp closestLamp = null;
                List<WorldPoint> bestPath = null;
                int shortestPathLength = Integer.MAX_VALUE;

                for (Lamp lamp : lampsToCheck) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    try {
                        List<WorldPoint> path = pathfinder.findPath(playerPos, lamp.getWorldPoint());

                        if (path != null && !path.isEmpty() && path.size() < shortestPathLength) {
                            closestLamp = lamp;
                            bestPath = new ArrayList<>(path);
                            shortestPathLength = path.size();
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

                            if (!finalFallbackLamp.equals(lastLoggedClosestLamp)) {
                                lastLoggedClosestLamp = finalFallbackLamp;
                            }
                        }
                        return;
                    }
                }

                if (!Thread.currentThread().isInterrupted() && finalClosestLamp != null) {
                    shortestPath = finalPath != null ? finalPath : new ArrayList<>();
                    if (!finalClosestLamp.equals(lastLoggedClosestLamp)) {
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

    private void calculatePathToTarget(WorldPoint targetLocation, TargetType targetType, WorldPoint playerLocation) {
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

        if (DKLampsHelper.isInBankArea(playerLocation) && targetType == TargetType.BANK) {
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
                    } else {
                        shortestPath = new ArrayList<>();
                        shortestPath.add(playerPos);
                        shortestPath.add(target);
                        closestDistance = target.distanceTo(playerPos);
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
