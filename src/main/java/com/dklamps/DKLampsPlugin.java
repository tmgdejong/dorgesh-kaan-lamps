package com.dklamps;

import com.dklamps.enums.Area;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import com.dklamps.pathfinder.Pathfinder;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
import static com.dklamps.ObjectIDs.DOOR_IDS;
import static com.dklamps.ObjectIDs.STAIR_IDS;
import static com.dklamps.ObjectIDs.WIRE_MACHINE_IDS;

@Slf4j
@PluginDescriptor(
	name = "Dorgesh-Kaan Lamps"
)
public class DKLampsPlugin extends Plugin
{
	private static final int DORGESHKAAN_LAMPS_VARBIT = 4038;
	private static final int WIRE_RESPAWN_TICKS = 8;

	@Inject
	private Client client;

	@Inject
	private DKLampsConfig config;

    @Inject
	private OverlayManager overlayManager;

	@Inject
	private DKLampsOverlay overlay;

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
	private GameObject wireMachine;

    @Getter
	private Set<Lamp> brokenLamps = new HashSet<>();
	private Set<Lamp> previouslyBrokenLamps = new HashSet<>();
	private Area lastArea = null;
	@Getter
	private Instant wireRespawnTime;

    @Getter
    private final Map<Lamp, LampStatus> lampStatuses = new EnumMap<>(Lamp.class);
    
    private Lamp lastLoggedClosestLamp = null;
    private CompletableFuture<Void> currentClosestLampTask;
    private long lastClosestLampCalculation = 0;
    private static final long CLOSEST_LAMP_COOLDOWN_MS = 600; // Update every tick (600ms)

    @Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

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
	protected void shutDown() throws Exception
	{
		log.info("Dorgesh-Kaan Lamps stopped!");
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
        
        if (currentClosestLampTask != null) {
            currentClosestLampTask.cancel(true);
        }
        if (pathfindingExecutor != null) {
            pathfindingExecutor.shutdown();
        }
        
		brokenLamps.clear();
		client.clearHintArrow();
        doors.clear();
		stairs.clear();
		wireMachine = null;
	}

    private void resetLampStatuses()
	{
		lampStatuses.clear();
		for (Lamp lamp : Lamp.values())
		{
			lampStatuses.put(lamp, LampStatus.UNKNOWN);
		}
	}

    @Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject gameObject = event.getGameObject();

		if (DKLampsHelper.isLamp(gameObject.getId()))
		{
			spawnedLamps.put(gameObject.getWorldLocation(), gameObject);
		}
		else if (STAIR_IDS.contains(gameObject.getId()))
		{
			stairs.add(gameObject);
		}
		else if (WIRE_MACHINE_IDS.contains(gameObject.getId()))
		{
			wireMachine = gameObject;
			if (gameObject.getId() == 22731)
			{
				wireRespawnTime = Instant.now().plusMillis((WIRE_RESPAWN_TICKS) * 600);
			}
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject gameObject = event.getGameObject();
		if (DKLampsHelper.isLamp(gameObject.getId()))
		{
			spawnedLamps.remove(gameObject.getWorldLocation());
		}
		else if (STAIR_IDS.contains(gameObject.getId()))
		{
			stairs.remove(gameObject);
		}
		else if (WIRE_MACHINE_IDS.contains(gameObject.getId()))
		{
			wireMachine = null;
		}
	}

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event)
    {
        WallObject wallObject = event.getWallObject();
        if (DOOR_IDS.contains(wallObject.getId()))
        {
            doors.add(wallObject);
        }
    }

    @Subscribe
    public void onWallObjectDespawned(WallObjectDespawned event)
    {
        WallObject wallObject = event.getWallObject();
        if (DOOR_IDS.contains(wallObject.getId()))
        {
            doors.remove(wallObject);
        }
    }

    @Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOADING ||
			gameStateChanged.getGameState() == GameState.LOGIN_SCREEN ||
			gameStateChanged.getGameState() == GameState.HOPPING)
		{
			spawnedLamps.clear();
            doors.clear();
			stairs.clear();
			wireMachine = null;

			if (gameStateChanged.getGameState() != GameState.LOADING)
			{
				brokenLamps.clear();
                lastLoggedClosestLamp = null;
				resetLampStatuses();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		Area currentArea = DKLampsHelper.getArea(client.getLocalPlayer().getWorldLocation());
		if (currentArea == null)
		{
			lastArea = null;
            client.clearHintArrow();
			return;
		}

		if (!currentArea.equals(lastArea))
		{
			lastArea = currentArea;
            previouslyBrokenLamps = new HashSet<>();
			return;
		}

		int lampVarbit = client.getVarbitValue(DORGESHKAAN_LAMPS_VARBIT);
		brokenLamps = DKLampsHelper.getBrokenLamps(lampVarbit, currentArea);

		Set<Lamp> fixedLamps = new HashSet<>(previouslyBrokenLamps);
		fixedLamps.removeAll(brokenLamps);

		if (!fixedLamps.isEmpty())
		{
			Area oppositeArea = currentArea.getOpposite();
			for (Map.Entry<Lamp, LampStatus> entry : lampStatuses.entrySet())
			{
				Area lampArea = entry.getKey().getArea();
				if (lampArea != currentArea && lampArea != oppositeArea && entry.getValue() == LampStatus.WORKING)
				{
					lampStatuses.put(entry.getKey(), LampStatus.UNKNOWN);
				}
			}
		}

		Set<Lamp> lampsInCurrentArea = DKLampsHelper.getLampsByArea(currentArea);
		for (Lamp lamp : lampsInCurrentArea)
		{
			lampStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.WORKING);
		}

		Area oppositeArea = currentArea.getOpposite();
		if (oppositeArea != null)
		{
			Set<Lamp> validOppositeLamps = DKLampsHelper.getValidOppositeLamps(currentArea);
			for (Lamp lamp : validOppositeLamps)
			{
				lampStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.WORKING);
			}
		}

		if (panel.isVisible())
		{
			panel.update();
		}

		if (config.showHintArrow())
		{
			updateHintArrow();
		}
		else
		{
			client.clearHintArrow();
		}

        logClosestLamp();

		previouslyBrokenLamps = new HashSet<>(brokenLamps);
		lastArea = currentArea;
	}

    private void logClosestLamp()
    {
        // Get all broken lamps from lampStatuses instead of just current area
        Set<Lamp> allBrokenLamps = new HashSet<>();
        for (Map.Entry<Lamp, LampStatus> entry : lampStatuses.entrySet())
        {
            if (entry.getValue() == LampStatus.BROKEN)
            {
                allBrokenLamps.add(entry.getKey());
            }
        }
        
        if (allBrokenLamps.isEmpty())
        {
            if (lastLoggedClosestLamp != null)
            {
                log.info("No broken lamps found");
                lastLoggedClosestLamp = null;
            }
            shortestPath.clear();
            return;
        }

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (playerLocation == null || pathfinder == null || pathfindingExecutor == null)
        {
            return;
        }

        // Throttle closest lamp calculation
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClosestLampCalculation < CLOSEST_LAMP_COOLDOWN_MS)
        {
            return;
        }
        lastClosestLampCalculation = currentTime;

        // Cancel any existing closest lamp calculation
        if (currentClosestLampTask != null && !currentClosestLampTask.isDone())
        {
            currentClosestLampTask.cancel(true);
        }

        // Create a snapshot for the background thread
        final WorldPoint playerPos = playerLocation;
        final Set<Lamp> lampsToCheck = new HashSet<>(allBrokenLamps);

        currentClosestLampTask = CompletableFuture.runAsync(() -> {
            try {
                Lamp closestLamp = null;
                List<WorldPoint> bestPath = null;
                int shortestPathLength = Integer.MAX_VALUE;

                for (Lamp lamp : lampsToCheck)
                {
                    // Check if task was cancelled
                    if (Thread.currentThread().isInterrupted())
                    {
                        return;
                    }

                    try {
                        List<WorldPoint> path = pathfinder.findPath(playerPos, lamp.getWorldPoint());

                        if (path != null && !path.isEmpty() && path.size() < shortestPathLength)
                        {
                            closestLamp = lamp;
                            bestPath = new ArrayList<>(path);
                            shortestPathLength = path.size();
                        }
                    } catch (Exception e) {
                        log.debug("Failed to find path to lamp {}: {}", lamp.name(), e.getMessage());
                        // Fallback to distance calculation for this lamp
                        int fallbackDistance = lamp.getWorldPoint().distanceTo(playerPos);
                        if (lamp.getWorldPoint().getPlane() != playerPos.getPlane()) {
                            fallbackDistance += 32;
                        }
                        if (fallbackDistance < shortestPathLength) {
                            closestLamp = lamp;
                            // Create a simple direct path for fallback
                            bestPath = new ArrayList<>();
                            bestPath.add(playerPos);
                            bestPath.add(lamp.getWorldPoint());
                            shortestPathLength = fallbackDistance;
                        }
                    }
                }

                // Update the result on the main thread
                final Lamp finalClosestLamp = closestLamp;
                final List<WorldPoint> finalPath = bestPath;
                final int finalDistance = shortestPathLength;
                
                if (!Thread.currentThread().isInterrupted() && finalClosestLamp != null)
                {
                    // Update the path for overlay rendering
                    shortestPath = finalPath != null ? finalPath : new ArrayList<>();
                    // Only log if the closest lamp changed
                    if (!finalClosestLamp.equals(lastLoggedClosestLamp))
                    {
                        WorldPoint lampLocation = finalClosestLamp.getWorldPoint();
                        String planeInfo = lampLocation.getPlane() == playerPos.getPlane() ? 
                            "same floor" : 
                            String.format("floor %d (you're on floor %d)", lampLocation.getPlane(), playerPos.getPlane());
                        
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

    public Lamp getClosestLamp()
    {
        return lastLoggedClosestLamp;
    }

	public void setHintArrow(Lamp lamp)
	{
		client.setHintArrow(lamp.getWorldPoint());
	}

	private void updateHintArrow()
	{
		if (brokenLamps.isEmpty())
		{
			client.clearHintArrow();
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		Lamp closestLamp = brokenLamps.stream()
			.filter(lamp -> lamp.getWorldPoint().getPlane() == playerLocation.getPlane())
			.min(Comparator.comparingInt(lamp -> lamp.getWorldPoint().distanceTo(playerLocation)))
			.orElse(null);

		if (closestLamp != null)
		{
			client.setHintArrow(closestLamp.getWorldPoint());
		}
		else
		{
			client.clearHintArrow();
		}
	}

	@Provides
	DKLampsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DKLampsConfig.class);
	}

    public DKLampsConfig getConfig()
	{
		return config;
	}

    public Client getClient()
    {
        return client;
    }

}
