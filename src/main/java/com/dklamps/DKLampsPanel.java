package com.dklamps;

import java.awt.GridLayout;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

public class DKLampsPanel extends PluginPanel
{
	private final MapPanel floor0;
	private final MapPanel floor1;
	private final MapPanel floor2;

	DKLampsPanel(DKLampsPlugin plugin)
	{
		super(false); // The panel itself doesn't scroll
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new GridLayout(0, 1, 0, 10)); // 0 rows, 1 col = vertical stack with 0px gap

		floor2 = new MapPanel(plugin, 2, "Second Floor");
		floor1 = new MapPanel(plugin, 1, "First Floor");
		floor0 = new MapPanel(plugin, 0, "Ground Floor");

		add(floor2);
		add(floor1);
		add(floor0);
	}

	public void update()
	{
		floor2.update();
		floor1.update();
		floor0.update();
	}
}