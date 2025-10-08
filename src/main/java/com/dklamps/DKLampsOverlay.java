package com.dklamps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.time.Instant;
import java.util.List;
import java.time.Duration;
import javax.inject.Inject;

import com.dklamps.enums.HighlightTypes;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import com.dklamps.enums.TimerTypes;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Slf4j
public class DKLampsOverlay extends Overlay
{
	private static final int WIRE_RESPAWN_TICKS = 8;

	private final Client client;
	private final DKLampsPlugin plugin;
	private final DKLampsConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private DKLampsOverlay(Client client, DKLampsPlugin plugin, DKLampsConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		renderLamps(graphics);

        if (config.highlightClosedDoors())
        {
            for (WallObject door : plugin.getDoors())
            {
                if (door.getPlane() != client.getTopLevelWorldView().getPlane())
                {
                    continue;
                }
                renderTileObject(door, config.doorHighlightColor(), graphics);
            }
        }

        if (config.highlightStairs())
        {
            for (GameObject stair : plugin.getStairs())
            {
                if (stair.getPlane() != client.getTopLevelWorldView().getPlane())
                {
                    continue;
                }
                renderTileObject(stair, config.stairHighlightColor(), graphics);
            }
        }

		if (config.highlightWireMachine() && plugin.getWireMachine() != null)
		{
			GameObject wireMachine = plugin.getWireMachine();
			if (wireMachine.getId() == 22730)
			{
				renderTileObject(wireMachine, config.wireMachineHighlightColor(), graphics);
			}
			else
			{
				renderWireTimer(graphics);
			}
		}

        // drawPath(graphics);

		return null;
	}

	private void renderLamps(Graphics2D graphics)
	{        
        for (GameObject lampObject : plugin.getSpawnedLamps().values())
        {
            if (lampObject.getPlane() != client.getTopLevelWorldView().getPlane() && !config.highlightOtherPlanesLamps())
            {
                continue;
            }
            
            Lamp lamp = DKLampsHelper.getLamp(lampObject.getId());
            if (lamp == null)
            {
                continue;
            }

            LampStatus status = plugin.getLampStatuses().getOrDefault(lamp, LampStatus.UNKNOWN);

            Color color;
            if (status == LampStatus.BROKEN && config.highlightBrokenLamps())
            {
                color = config.getBrokenLampColor();
            }
            else if (status == LampStatus.WORKING && config.highlightWorkingLamps())
            {
                color = config.getWorkingLampColor();
            }
            else if (config.highlightAllLamps())
            {
                color = config.getDefaultLampColor();
            }
            else
            {
                continue;
            }

            if (lampObject.getPlane() != client.getTopLevelWorldView().getPlane() && config.darkenOtherPlanesLamps())
            {
                color = color.darker();
            }

            renderTileObject(lampObject, color, graphics);
        }
    }

    private void renderTileObject(TileObject tileObject, Color color, Graphics2D graphics)
	{
		HighlightTypes style = config.highlightStyle();
		switch (style)
		{
			case HIGHLIGHT_BORDER:
				modelOutlineRenderer.drawOutline(tileObject, config.borderThickness(), color, config.borderFeather());
				break;
			case HIGHLIGHT_CLICKBOX:
				Shape clickbox = tileObject.getClickbox();
				if (clickbox != null)
				{
					Point mousePosition = client.getMouseCanvasPosition();
					if (clickbox.contains(mousePosition.getX(), mousePosition.getY()))
					{
						graphics.setColor(color.darker());
					}
					else
					{
						graphics.setColor(color);
					}
					graphics.draw(clickbox);
					graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
					graphics.fill(clickbox);
				}
				break;
		}
	}

	private void renderWireTimer(Graphics2D graphics)
	{
		if (plugin.getWireRespawnTime() == null || plugin.getWireMachine() == null)
		{
			return;
		}

		Instant now = Instant.now();
		if (now.isAfter(plugin.getWireRespawnTime()))
		{
			return;
		}

		Duration duration = Duration.between(now, plugin.getWireRespawnTime());
		double seconds = (double) Math.ceil(duration.toMillis() / 100.0) / 10.0;
		double progress = (double) duration.toMillis() / ((WIRE_RESPAWN_TICKS) * 600.0);


		LocalPoint lp = plugin.getWireMachine().getLocalLocation();
		Point point = net.runelite.api.Perspective.getCanvasTextLocation(client, graphics, lp, " ", 0);

		if (point == null)
		{
			return;
		}

		if (config.timerType() == TimerTypes.PIE)
		{
			ProgressPieComponent pie = new ProgressPieComponent();
			pie.setPosition(point);
			pie.setBorderColor(config.wireMachineHighlightColor());
			pie.setDiameter(20);
			pie.setFill(config.wireMachineHighlightColor());
			pie.setProgress(progress);
			pie.render(graphics);
		}
		else if (config.timerType() == TimerTypes.TICKS)
		{
			long ticks = (long) Math.ceil(duration.toMillis() / 600.0);
			String text = String.valueOf(ticks);
			OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
		}
		else if (config.timerType() == TimerTypes.SECONDS)
		{
			String text = String.format("%.1f", seconds);
			OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
		}
	}

    private void drawPath(Graphics2D graphics)
    {
        List<WorldPoint> path = plugin.getShortestPath();
        if (path == null || path.isEmpty())
        {
            return;
        }

        for (WorldPoint point : path)
        {
            if (point.getPlane() != client.getTopLevelWorldView().getPlane())
            {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(client, point);
            if (localPoint == null)
            {
                continue;
            }

            Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
            if (poly != null)
            {
                OverlayUtil.renderPolygon(graphics, poly, Color.CYAN);
            }
        }
    }
}
