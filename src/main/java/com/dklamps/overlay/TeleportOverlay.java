package com.dklamps.overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;

import com.dklamps.DKLampsConfig;
import com.dklamps.DKLampsConstants;
import com.dklamps.DKLampsPlugin;
import com.dklamps.enums.TargetType;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class TeleportOverlay extends WidgetItemOverlay
{
    private final DKLampsPlugin plugin;
    private final DKLampsConfig config;
    @Inject
    private TeleportOverlay(DKLampsPlugin plugin, DKLampsConfig config, ItemManager itemManager)
    {
        this.plugin = plugin;
        this.config = config;
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

        if (path != null && (path.size() > config.maxPathDistance() || plugin.getStateManager().getBrokenLamps().isEmpty())
                && plugin.getNavigationManager().getCurrentTargetType() == TargetType.LAMP)
        {
            Rectangle bounds = itemWidget.getCanvasBounds();
            Color oldColor = graphics.getColor();

            Color base = config.pathColor();

            long phase = (System.currentTimeMillis() / 600) % 2;
            int alpha = (phase == 0) ? 192 : 64;

            graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            graphics.setColor(oldColor);
        }
    }
}