package com.dklamps;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

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
        showOnBank();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {
        // Use the correct Item ID for your sphere here
        if (itemId != ObjectIDs.TELEPORT_SPHERE_ID)
        {
            return;
        }

        List<WorldPoint> path = plugin.getShortestPath();

        // Only highlight if we have a path AND it's longer than the config allows
        if (path != null && (path.size() > config.maxPathDistance() || plugin.getBrokenLamps().isEmpty()))
        {
            Rectangle bounds = itemWidget.getCanvasBounds();
            java.awt.Stroke oldStroke = graphics.getStroke();
            Color oldColor = graphics.getColor();

            graphics.setColor(config.pathColor());
            graphics.setStroke(new java.awt.BasicStroke(3.0f));
            graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

            graphics.setStroke(oldStroke);
            graphics.setColor(oldColor);
        }
    }
}