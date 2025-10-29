package com.dklamps;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class TeleportOverlay extends WidgetItemOverlay
{
    private final DKLampsPlugin plugin;
    private final DKLampsConfig config;
    private final ItemManager itemManager;

    @Inject
    private TeleportOverlay(DKLampsPlugin plugin, DKLampsConfig config, ItemManager itemManager)
    {
        this.plugin = plugin;
        this.config = config;
        this.itemManager = itemManager;
        showOnInventory();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {
        if (itemId != DKLampsConstants.TELEPORT_SPHERE_ID)
        {
            return;
        }

        List<WorldPoint> path = plugin.getNavigationManager().getShortestPath();

        // Only highlight if we have a path AND it's longer than the config allows
        if (path != null && (path.size() > config.maxPathDistance() || plugin.getStateManager().getBrokenLamps().isEmpty()))
        {
            Rectangle bounds = itemWidget.getCanvasBounds();
            Color oldColor = graphics.getColor();

            Color base = config.pathColor();
            graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 128));
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            graphics.setColor(oldColor);
        }
    }
}