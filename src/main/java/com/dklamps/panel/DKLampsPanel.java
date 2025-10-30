package com.dklamps.panel;

import java.awt.GridLayout;
import javax.swing.border.EmptyBorder;

import com.dklamps.DKLampsPlugin;

import net.runelite.client.ui.PluginPanel;

public class DKLampsPanel extends PluginPanel {
    private final MapPanel floor0;
    private final MapPanel floor1;
    private final MapPanel floor2;

    public DKLampsPanel(DKLampsPlugin plugin) {
        super(false);
        setBorder(new EmptyBorder(5, 10, 5, 10));
        setLayout(new GridLayout(0, 1, 0, 5));

        floor2 = new MapPanel(plugin, 2, "Second Floor");
        floor1 = new MapPanel(plugin, 1, "First Floor");
        floor0 = new MapPanel(plugin, 0, "Ground Floor");

        add(floor2);
        add(floor1);
        add(floor0);
    }

    public void update() {
        floor2.update();
        floor1.update();
        floor0.update();
    }
}