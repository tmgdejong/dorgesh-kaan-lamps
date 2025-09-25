package com.dklamps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

public class CollapsibleMapPanel extends JPanel
{
	private final MapPanel mapPanel;
	private boolean isCollapsed = false;

	CollapsibleMapPanel(MapPanel mapPanel)
	{
		this.mapPanel = mapPanel;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel titleLabel = new JLabel(mapPanel.getTitle(), JLabel.CENTER);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		titleLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

		titleLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggle();
			}
		});

		add(titleLabel, BorderLayout.NORTH);
		add(mapPanel, BorderLayout.CENTER);
	}

	public void expand()
	{
		if (isCollapsed)
		{
			toggle();
		}
	}

	public void collapse()
	{
		if (!isCollapsed)
		{
			toggle();
		}
	}

	private void toggle()
	{
		isCollapsed = !isCollapsed;
		mapPanel.setVisible(!isCollapsed);
		revalidate();
	}
}