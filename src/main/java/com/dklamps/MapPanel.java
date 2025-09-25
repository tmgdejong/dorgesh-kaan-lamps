package com.dklamps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class MapPanel extends JPanel
{
	private static final WorldPoint MAP_NORTH_WEST_CORNER = new WorldPoint(2688, 5375, 0);
	private static final WorldPoint MAP_SOUTH_EAST_CORNER = new WorldPoint(2751, 5248, 0);

	private final DKLampsPlugin plugin;
	private final int plane;
	@Getter
	private final String title;
	private final List<Lamp> lampsOnThisFloor;
	private final BufferedImage scaledMapImage;

	MapPanel(DKLampsPlugin plugin, int plane, String title)
	{
		this.plugin = plugin;
		this.plane = plane;
		this.title = title;

		setLayout(new BorderLayout());

		BufferedImage mapImage = ImageUtil.loadImageResource(getClass(), "/map_p" + plane + ".png");

		if (mapImage != null)
		{
			int panelWidth = PluginPanel.PANEL_WIDTH;
			double scale = (double) panelWidth / mapImage.getWidth();
			int newHeight = (int) (mapImage.getHeight() * scale);
			scaledMapImage = new BufferedImage(panelWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = scaledMapImage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(mapImage, 0, 0, panelWidth, newHeight, null);
			g.dispose();
		}
		else
		{
			scaledMapImage = null;
		}

		JPanel mapDisplay = new MapDisplay();
		if (scaledMapImage != null)
		{
			mapDisplay.setPreferredSize(new Dimension(scaledMapImage.getWidth(), scaledMapImage.getHeight()));
		}
		add(mapDisplay, BorderLayout.CENTER);

		this.lampsOnThisFloor = Arrays.stream(Lamp.values())
			.filter(lamp -> lamp.getWorldPoint().getPlane() == plane)
			.collect(Collectors.toList());
	}

	public void update()
	{
		this.repaint();
	}

	private class MapDisplay extends JPanel
	{
		MapDisplay()
		{
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					for (Lamp lamp : lampsOnThisFloor)
					{
						Ellipse2D.Double circle = getLampCircle(lamp);
						if (circle != null && circle.contains(e.getPoint()))
						{
							plugin.setHintArrow(lamp);
							break;
						}
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			if (scaledMapImage == null) return;
			g.drawImage(scaledMapImage, 0, 0, this.getWidth(), this.getHeight(), null);


			if (plugin.getClient().getLocalPlayer() != null &&
				plugin.getClient().getLocalPlayer().getWorldLocation().getPlane() == plane)
			{
				g.setColor(Color.CYAN);
				g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
			}

			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			for (Lamp lamp : lampsOnThisFloor)
			{
				LampStatus status = plugin.getLampStatuses().getOrDefault(lamp, LampStatus.UNKNOWN);

				Color color;
                if (status == LampStatus.BROKEN)
                {
                    color = plugin.getConfig().getBrokenLampColor();
                }
                else if (status == LampStatus.FIXED && plugin.getConfig().showKnownWorkingLamps())
                {
                    color = plugin.getConfig().getWorkingLampColor();
                }
                else
                {
                    color = plugin.getConfig().getDefaultLampColor();
                }

				g2d.setColor(color);
				Ellipse2D.Double circle = getLampCircle(lamp);
				if (circle != null)
				{
					g2d.fill(circle);
					g2d.setColor(Color.BLACK);
					g2d.draw(circle);
				}
			}
		}

		private Ellipse2D.Double getLampCircle(Lamp lamp)
		{
			WorldPoint point = lamp.getWorldPoint();

			if (point.getX() < MAP_NORTH_WEST_CORNER.getX() || point.getX() > MAP_SOUTH_EAST_CORNER.getX() ||
				point.getY() > MAP_NORTH_WEST_CORNER.getY() || point.getY() < MAP_SOUTH_EAST_CORNER.getY())
			{
				log.warn("Lamp {} at {} is outside the defined map bounds!", lamp.name(), point);
				return null;
			}

			double mapWidthInTiles = (MAP_SOUTH_EAST_CORNER.getX() - MAP_NORTH_WEST_CORNER.getX()) + 1;
			double mapHeightInTiles = (MAP_NORTH_WEST_CORNER.getY() - MAP_SOUTH_EAST_CORNER.getY()) + 1;

			double lampRelativeX = (point.getX() - MAP_NORTH_WEST_CORNER.getX() + 0.5) / mapWidthInTiles;
			double lampRelativeY = (MAP_NORTH_WEST_CORNER.getY() - point.getY() + 0.5) / mapHeightInTiles;

			int panelX = (int) (lampRelativeX * getWidth());
			int panelY = (int) (lampRelativeY * getHeight());
			int radius = 4;

			return new Ellipse2D.Double(panelX - radius, panelY - radius, radius * 2, radius * 2);
		}
	}
}