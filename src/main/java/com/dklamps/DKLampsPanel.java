package com.dklamps;

import javax.swing.BoxLayout;
import net.runelite.client.ui.PluginPanel;

public class DKLampsPanel extends PluginPanel
{
    private final DKLampsPlugin plugin;
	private final CollapsibleMapPanel floor0;
	private final CollapsibleMapPanel floor1;
	private final CollapsibleMapPanel floor2;

	DKLampsPanel(DKLampsPlugin plugin)
	{
		super(false);
        this.plugin = plugin;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		floor0 = new CollapsibleMapPanel(new MapPanel(plugin, 0, "Ground Floor"));
		floor1 = new CollapsibleMapPanel(new MapPanel(plugin, 1, "First Floor"));
		floor2 = new CollapsibleMapPanel(new MapPanel(plugin, 2, "Second Floor"));

		add(floor0);
		add(floor1);
		add(floor2);
	}

	public void update()
	{
		int currentPlane = -1;
		if (plugin.getClient().getLocalPlayer() != null)
		{
			currentPlane = plugin.getClient().getLocalPlayer().getWorldLocation().getPlane();
		}

		if (currentPlane == 0) floor0.expand(); else floor0.collapse();
		if (currentPlane == 1) floor1.expand(); else floor1.collapse();
		if (currentPlane == 2) floor2.expand(); else floor2.collapse();

		repaint();
	}
}