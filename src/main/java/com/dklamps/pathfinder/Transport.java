package com.dklamps.pathfinder;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data
@AllArgsConstructor
public class Transport {
    private WorldPoint origin;
    private WorldPoint destination;
    private String menuOption;
    private String menuTarget;
    private int objectId;
    private int duration;
}