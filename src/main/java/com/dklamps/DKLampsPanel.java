package com.dklamps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

public class DKLampsPanel extends PluginPanel
{
	private final DKLampsPlugin plugin;
	private final Map<Lamp, JLabel> lampLabels = new EnumMap<>(Lamp.class);

	DKLampsPanel(DKLampsPlugin plugin)
	{
		super();
		this.plugin = plugin;

		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());

		JPanel titlePanel = new JPanel();
		titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
		titlePanel.setLayout(new BorderLayout());

		JLabel title = new JLabel("Dorgesh-Kaan Lamps");
		title.setForeground(Color.WHITE);
		titlePanel.add(title, BorderLayout.NORTH);
		add(titlePanel, BorderLayout.NORTH);

		JPanel lampGridPanel = new JPanel(new GridLayout(0, 5, 5, 5));
		for (Lamp lamp : Lamp.values())
		{
			JLabel lampLabel = new JLabel("?");
			lampLabel.setOpaque(true);
			lampLabel.setHorizontalAlignment(JLabel.CENTER);
			lampLabel.setToolTipText(lamp.getDescription());
			lampLabels.put(lamp, lampLabel);
			lampGridPanel.add(lampLabel);
		}
		add(lampGridPanel, BorderLayout.CENTER);
	}

	public void updateLamps()
	{
		for (Map.Entry<Lamp, JLabel> entry : lampLabels.entrySet())
		{
			Lamp lamp = entry.getKey();
			JLabel label = entry.getValue();
			LampStatus status = plugin.getLampStatuses().getOrDefault(lamp, LampStatus.UNKNOWN);
			label.setBackground(status.getColor());
		}
	}
}