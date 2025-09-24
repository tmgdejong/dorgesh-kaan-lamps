package com.dklamps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.api.Perspective;

public class DKLampsOverlay extends Overlay
{
	private final Client client;
	private final DKLampsPlugin plugin;
	private final DKLampsConfig config;

	@Inject
	private DKLampsOverlay(Client client, DKLampsPlugin plugin, DKLampsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
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

		for (Lamp lamp : Lamp.values())
		{
			WorldPoint lampWorldPoint = lamp.getWorldPoint();
			if (lampWorldPoint.getPlane() != client.getLocalPlayer().getWorldLocation().getPlane())
			{
				continue;
			}

			LocalPoint localPoint = LocalPoint.fromWorld(client, lampWorldPoint);
			if (localPoint == null)
			{
				continue;
			}

			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly == null)
			{
				continue;
			}

			Color color = plugin.getBrokenLamps().contains(lamp) ? config.getBrokenLampColor() : config.getWorkingLampColor();
			OverlayUtil.renderPolygon(graphics, poly, color);
		}
		return null;
	}
}