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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
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
    private static final Pattern HINT_PATTERN = Pattern.compile("There is another broken lamp nearby, (.+?) of here (.+?)\\.");

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
            findShortestPath();
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

		previouslyBrokenLamps = new HashSet<>(brokenLamps);
		lastArea = currentArea;
	}

    // @Subscribe
    // public void onChatMessage(ChatMessage event)
    // {
    //     if (event.getType() != ChatMessageType.GAMEMESSAGE)
    //     {
    //         return;
    //     }

    //     Matcher matcher = HINT_PATTERN.matcher(event.getMessage());

    //     if (matcher.find())
    //     {
    //         WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
    //         if (playerLocation == null)
    //         {
    //             return;
    //         }

    //         String directionHint = matcher.group(1).toLowerCase();
    //         String floorHint = matcher.group(2);

    //         int currentPlane = playerLocation.getPlane();
    //         if (floorHint != null)
    //         {
    //             if (floorHint.contains("upstairs"))
    //             {
    //                 if (currentPlane == 0)
    //                 {
    //                     findLampFromHint(directionHint, 1, playerLocation);
    //                     findLampFromHint(directionHint, 2, playerLocation);
    //                 }
    //                 else if (currentPlane == 1)
    //                 {
    //                     findLampFromHint(directionHint, 2, playerLocation);
    //                 }
    //             }
    //             else if (floorHint.contains("downstairs"))
    //             {
    //                 if (currentPlane == 2)
    //                 {
    //                     findLampFromHint(directionHint, 1, playerLocation);
    //                     findLampFromHint(directionHint, 0, playerLocation);
    //                 }
    //                 else if (currentPlane == 1)
    //                 {
    //                     findLampFromHint(directionHint, 0, playerLocation);
    //                 }
    //             }
    //             else if (floorHint.contains("on the same floor"))
    //             {
    //                 findLampFromHint(directionHint, currentPlane, playerLocation);
    //             }
    //             else
    //             {
    //                 log.warn("Unknown floor hint: {}", floorHint);
    //             }
    //         }
    //     }
    // }

    // private void findLampFromHint(String directionHint, int targetPlane, WorldPoint playerLocation)
    // {
    //     Set<Lamp> candidateLamps = new HashSet<>();
    //     for (Map.Entry<Lamp, LampStatus> entry : lampStatuses.entrySet())
    //     {
    //         if (entry.getKey().getWorldPoint().getPlane() == targetPlane && entry.getValue() == LampStatus.UNKNOWN)
    //         {
    //             candidateLamps.add(entry.getKey());
    //         }
    //     }

    //     if (candidateLamps.isEmpty())
    //     {
    //         return;
    //     }

    //     Set<Lamp> matchingDirectionLamps = new HashSet<>();
    //     for (Lamp lamp : candidateLamps)
    //     {
    //         int dx = lamp.getWorldPoint().getX() - playerLocation.getX();
    //         int dy = lamp.getWorldPoint().getY() - playerLocation.getY();
    //         boolean directionMatch = false;

    //         // TODO: combined directions like "north east"
    //         if (directionHint.contains("north") && dy > 0) directionMatch = true;
    //         if (directionHint.contains("south") && dy < 0) directionMatch = true;
    //         if (directionHint.contains("east") && dx > 0) directionMatch = true;
    //         if (directionHint.contains("west") && dx < 0) directionMatch = true;
    //         if (!directionHint.contains("north") && !directionHint.contains("south") && !directionHint.contains("east") && !directionHint.contains("west"))
    //         {
    //             directionMatch = true; // No direction hint (e.g., just "upstairs")
    //         }


    //         if (directionMatch)
    //         {
    //             matchingDirectionLamps.add(lamp);
    //         }
    //     }

    //     // If direction filtering is too strict, fall back to all unknown lamps on that plane
    //     if (matchingDirectionLamps.isEmpty())
    //     {
    //         matchingDirectionLamps.addAll(candidateLamps);
    //     }

    //     Lamp closestLamp = matchingDirectionLamps.stream()
    //             .min(Comparator.comparingInt(lamp -> lamp.getWorldPoint().distanceTo(playerLocation)))
    //             .orElse(null);

    //     if (closestLamp != null)
    //     {
    //         lampStatuses.put(closestLamp, LampStatus.BROKEN);
    //         if (panel.isVisible())
    //         {
    //             panel.update();
    //         }
    //     }
    // }

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

    private void findShortestPath()
    {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (playerLocation == null)
        {
            return;
        }

        if (brokenLamps.isEmpty())
        {
            shortestPath.clear();
            return;
        }

        List<WorldPoint> bestPath = null;

        for (Lamp lamp : brokenLamps)
        {
            List<WorldPoint> currentPath = pathfinder.findPath(playerLocation, lamp.getWorldPoint());

            if (currentPath == null || currentPath.isEmpty())
            {
                continue;
            }

            if (bestPath == null || currentPath.size() < bestPath.size())
            {
                bestPath = currentPath;
            }
        }
        
        shortestPath = (bestPath != null) ? bestPath : new ArrayList<>();
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
