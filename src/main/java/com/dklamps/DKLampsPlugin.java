package com.dklamps;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
				wireRespawnTime = Instant.now().plusSeconds((long) ((WIRE_RESPAWN_TICKS - 1) * 0.6));
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

		// If the player has just entered a new area, do nothing this tick.
		// Update lastArea and wait for the next tick to ensure varbits are synced.
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
			// A lamp was fixed. Reset other Fixed lamps to Unknown.
			Area oppositeArea = currentArea.getOpposite();
			for (Map.Entry<Lamp, LampStatus> entry : lampStatuses.entrySet())
			{
				Area lampArea = entry.getKey().getArea();
				if (lampArea != currentArea && lampArea != oppositeArea && entry.getValue() == LampStatus.FIXED)
				{
					lampStatuses.put(entry.getKey(), LampStatus.UNKNOWN);
				}
			}
		}

		// Update statuses for the current area with fresh data
		Set<Lamp> lampsInCurrentArea = DKLampsHelper.getLampsByArea(currentArea);
		for (Lamp lamp : lampsInCurrentArea)
		{
			lampStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.FIXED);
		}

		// Also update statuses for the opposite area
		Area oppositeArea = currentArea.getOpposite();
		if (oppositeArea != null)
		{
			Set<Lamp> validOppositeLamps = DKLampsHelper.getValidOppositeLamps(currentArea);
			for (Lamp lamp : validOppositeLamps)
			{
				lampStatuses.put(lamp, brokenLamps.contains(lamp) ? LampStatus.BROKEN : LampStatus.FIXED);
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

		// Update state for the next tick
		previouslyBrokenLamps = new HashSet<>(brokenLamps);
		lastArea = currentArea;
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
