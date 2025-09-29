package com.dklamps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.time.Instant;
import java.time.Duration;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class DKLampsOverlay extends Overlay
{
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

        if (config.highlightDoors())
        {
            for (GameObject door : plugin.getDoors())
            {
                if (door.getPlane() != client.getPlane())
                {
                    continue;
                }
                renderGameObject(door, config.doorHighlightColor(), graphics);
            }
        }

        if (config.highlightStairs())
        {
            for (GameObject stair : plugin.getStairs())
            {
                if (stair.getPlane() != client.getPlane())
                {
                    continue;
                }
                renderGameObject(stair, config.stairHighlightColor(), graphics);
            }
        }

		if (config.highlightWireMachine() && plugin.getWireMachine() != null)
		{
			renderGameObject(plugin.getWireMachine(), config.wireMachineHighlightColor(), graphics);
			renderWireTimer(graphics);
		}


		return null;
	}

	private void renderLamps(Graphics2D graphics)
	{
		for (GameObject lampObject : plugin.getSpawnedLamps().values())
		{
			// if (lampObject.getPlane() != client.getPlane())
			// {
			// 	continue;
			// }

			Lamp lamp = DKLampsHelper.getLamp(lampObject.getId());
			if (lamp == null)
			{
				continue;
			}

			LampStatus status = plugin.getLampStatuses().getOrDefault(lamp, LampStatus.UNKNOWN);

			Color color;
			if (status == LampStatus.BROKEN)
			{
				color = config.getBrokenLampColor();
			}
			else if (status == LampStatus.FIXED && config.showKnownWorkingLamps())
			{
				color = config.getWorkingLampColor();
			}
			else if (config.showAllLamps())
			{
				color = config.getDefaultLampColor();
			}
			else
			{
				continue;
			}

			renderGameObject(lampObject, color, graphics);
		}
	}

	private void renderGameObject(GameObject gameObject, Color color, Graphics2D graphics)
	{
		HighlightStyle style = config.highlightStyle();
		switch (style)
		{
			case HIGHLIGHT_BORDER:
				modelOutlineRenderer.drawOutline(gameObject, config.borderThickness(), color, config.borderFeather());
				break;
			case HIGHLIGHT_CLICKBOX:
				Shape clickbox = gameObject.getClickbox();
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
		long seconds = duration.getSeconds();

		String text = String.valueOf(seconds);
		LocalPoint lp = plugin.getWireMachine().getLocalLocation();
		Point point = net.runelite.api.Perspective.getCanvasTextLocation(client, graphics, lp, text, 0);

		if (point != null)
		{
			OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
		}
	}
}