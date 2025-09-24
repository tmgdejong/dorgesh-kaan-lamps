package com.dklamps;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Dorgesh-Kaan Lamps"
)
public class DKLampsPlugin extends Plugin
{
	private static final int DORGESHKAAN_LAMPS_VARBIT = 4038;

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
	private Set<Lamp> brokenLamps = new HashSet<>();

    @Getter
	private final Map<Lamp, LampStatus> lampStatuses = new EnumMap<>(Lamp.class);

    @Override
	protected void startUp() throws Exception
	{
		log.info("Dorgesh-Kaan Lamps started!");
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
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN ||
			gameStateChanged.getGameState() == GameState.HOPPING)
		{
			brokenLamps.clear();
            resetLampStatuses();
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
			if (!brokenLamps.isEmpty())
			{
				brokenLamps = Collections.emptySet();
                client.clearHintArrow();
			}
			return;
		}

		int lampVarbit = client.getVarbitValue(DORGESHKAAN_LAMPS_VARBIT);
		brokenLamps = DKLampsHelper.getBrokenLamps(lampVarbit);
        
        // Update lamp statuses
		for (Lamp lamp : DKLampsHelper.getLampsByArea(currentArea))
		{
			if (brokenLamps.contains(lamp))
			{
				lampStatuses.put(lamp, LampStatus.BROKEN);
			}
			else
			{
				lampStatuses.put(lamp, LampStatus.FIXED);
			}
		}

		panel.updateLamps();

        if (config.showHintArrow())
        {
            updateHintArrow();
        }
        else
        {
            client.clearHintArrow();
        }
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
}
