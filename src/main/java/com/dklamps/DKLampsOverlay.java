package com.dklamps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
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

		for (GameObject lampObject : plugin.getSpawnedLamps().values())
		{
			if (lampObject.getPlane() != client.getPlane())
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

			HighlightStyle style = config.highlightStyle();
			switch (style)
			{
				case HIGHLIGHT_BORDER:
					modelOutlineRenderer.drawOutline(lampObject, config.borderThickness(), color, config.borderFeather());
					break;
				case HIGHLIGHT_CLICKBOX:
					Shape clickbox = lampObject.getClickbox();
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
		return null;
	}
}