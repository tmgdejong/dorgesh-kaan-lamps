package com.dklamps;

import com.dklamps.enums.Area;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class StatsOverlay extends OverlayPanel
{
    private final DKLampsPlugin plugin;
    private final DKLampsConfig config;
    private final Client client;

    @Inject
    private StatsOverlay(DKLampsPlugin plugin, DKLampsConfig config, Client client)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showStatsOverlay())
        {
            return null;
        }
        
        // Only show overlay when in Dorgesh-Kaan region
        if (client.getLocalPlayer() == null)
        {
            return null;
        }
        
        Area currentArea = DKLampsHelper.getArea(client.getLocalPlayer().getWorldLocation());
        if (currentArea == null)
        {
            return null;
        }

        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Dorgesh-Kaan Lamps")
            .color(Color.CYAN)
            .build());
            
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Target:")
            .right(plugin.getCurrentTargetType())
            .rightColor(config.pathColor())
            .build());

        if (config.showClosestDistance())
        {
            String distText = (plugin.getClosestDistance() == Integer.MAX_VALUE) ? "-" : String.valueOf(plugin.getClosestDistance());
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Distance:")
                .right(distText + " tiles")
                .build());
        }

        // Follow woodcutting plugin pattern for session stats
        int lampsFixed = plugin.getLampsFixed();
        if (lampsFixed > 0 && config.showSessionFixed())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Lamps fixed:")
                .right(Integer.toString(lampsFixed))
                .build());

            int lampsPerHr = plugin.getLampsPerHr();
            if (lampsPerHr > 0 && config.showLampsPerHour())
            {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Lamps/hr:")
                    .right(Integer.toString(lampsPerHr))
                    .build());
            }
        }

        if (config.showTotalFixed())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Total Fixed:")
                .right(String.valueOf(plugin.getTotalLampsFixed()))
                .build());
        }

        return super.render(graphics);
    }
}