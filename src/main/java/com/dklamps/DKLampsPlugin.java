package com.dklamps;

import com.dklamps.enums.InventoryState;
import com.dklamps.overlay.DKLampsOverlay;
import com.dklamps.overlay.StatsOverlay;
import com.dklamps.overlay.TeleportOverlay;
import com.dklamps.panel.DKLampsPanel;
import com.dklamps.pathfinder.Pathfinder;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
    @Getter
    private Client client;

    @Inject
    @Getter
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
    @Getter
    private DKLampsNavigationManager navigationManager;
    @Getter
    private DKLampsStatsTracker statsTracker;
    @Getter
    private DKLampsStateManager stateManager;

    private ExecutorService pathfindingExecutor;

    @Getter
    private Pathfinder pathfinder;

    @Getter
    private Instant lastTickInstant = Instant.now();

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        overlayManager.add(teleportOverlay);
        overlayManager.add(statsOverlay);

        statsTracker = new DKLampsStatsTracker();
        stateManager = new DKLampsStateManager(client, statsTracker);

        pathfindingExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DKLamps-Pathfinder");
            t.setDaemon(true);
            return t;
        });

        try {
            pathfinder = new Pathfinder();
        } catch (IOException e) {
            log.error("Failed to load pathfinder collision data", e);
            return;
        }

        navigationManager = new DKLampsNavigationManager(client, pathfinder, pathfindingExecutor);

        panel = new DKLampsPanel(this);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/light_orb_32x32.png");
        navButton = NavigationButton.builder()
                .tooltip("Dorgesh-Kaan Lamps")
                .icon(icon)
                .priority(7)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Dorgesh-Kaan Lamps stopped!");
        overlayManager.remove(overlay);
        overlayManager.remove(teleportOverlay);
        overlayManager.remove(statsOverlay);
        clientToolbar.removeNavigation(navButton);

        navigationManager.shutDown();

        if (stateManager != null) {
            stateManager.shutDown();
        }
        if (navigationManager != null) {
            navigationManager.shutDown();
        }
        if (pathfindingExecutor != null) {
            pathfindingExecutor.shutdown();
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        stateManager.onGameObjectSpawned(event.getGameObject());
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        stateManager.onGameObjectDespawned(event.getGameObject());
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event) {
        stateManager.onWallObjectSpawned(event.getWallObject());
    }

    @Subscribe
    public void onWallObjectDespawned(WallObjectDespawned event) {
        stateManager.onWallObjectDespawned(event.getWallObject());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        stateManager.onGameStateChanged(gameStateChanged.getGameState());
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        stateManager.onChatMessage(chatMessage);
        statsTracker.onChatMessage(chatMessage);
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        lastTickInstant = Instant.now();
        
        if (client.getLocalPlayer() == null) {
            return;
        }

        stateManager.onGameTick();

        InventoryState inventoryState = InventoryState.NO_LIGHT_BULBS.getInventoryState(client);
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        navigationManager.update(
                stateManager.getLampStatuses(),
                inventoryState,
                playerLocation,
                stateManager.getWireMachine());

        if (panel.isVisible()) {
            panel.update();
        }

        client.clearHintArrow();
    }

    @Provides
    DKLampsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DKLampsConfig.class);
    }
}
