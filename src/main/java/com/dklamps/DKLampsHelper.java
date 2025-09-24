package com.dklamps;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.runelite.api.coords.WorldPoint;

public class DKLampsHelper
{
    private static final int WORLD_MAP_LINE = 5312;
    private static final int DK_WEST_VALUE = 2688;
    private static final int DK_EAST_VALUE = 2751;
    private static final int DK_NORTH_VALUE = 5375;
    private static final int DK_SOUTH_VALUE = 5248;

	private static final Map<Area, Set<Lamp>> LAMPS_BY_AREA;
    private static final Map<Integer, Set<Lamp>> LAMPS_BY_BIT_POSITION;

    	static
	{
		ImmutableMap.Builder<Area, Set<Lamp>> areaBuilder = new ImmutableMap.Builder<>();
		for (Area area : Area.values())
		{
			Set<Lamp> lampsInArea = Sets.immutableEnumSet(
				java.util.Arrays.stream(Lamp.values())
					.filter(lamp -> lamp.getArea() == area)
					.collect(Collectors.toSet())
			);
			areaBuilder.put(area, lampsInArea);
		}
		LAMPS_BY_AREA = areaBuilder.build();

		ImmutableMap.Builder<Integer, Set<Lamp>> bitBuilder = new ImmutableMap.Builder<>();
		for (int i = 0; i < 32; i++)
		{
			final int bit = i;
			Set<Lamp> lampsForBit = Sets.immutableEnumSet(
				java.util.Arrays.stream(Lamp.values())
					.filter(lamp -> lamp.getBitPosition() == bit)
					.collect(Collectors.toSet())
			);
			if (!lampsForBit.isEmpty())
			{
				bitBuilder.put(i, lampsForBit);
			}
		}
		LAMPS_BY_BIT_POSITION = bitBuilder.build();
	}

	public static Set<Lamp> getLampsByArea(Area area)
	{
		return LAMPS_BY_AREA.getOrDefault(area, Collections.emptySet());
	}
    
    public static Area getArea(WorldPoint worldPoint)
    {
        int plane = worldPoint.getPlane();
        int y = worldPoint.getY();
        int x = worldPoint.getX();

        if (x < DK_WEST_VALUE || x > DK_EAST_VALUE || y > DK_NORTH_VALUE || y < DK_SOUTH_VALUE)
        {
            return null;
        }

        if (plane == 0)
        {
            return y >= WORLD_MAP_LINE ? Area.P0_N : Area.P0_S;
        }
        else if (plane == 1)
        {
            return y >= WORLD_MAP_LINE ? Area.P1_N : Area.P1_S;
        }
        else if (plane == 2)
        {
            return y >= WORLD_MAP_LINE ? Area.P2_N : Area.P2_S;
        }
        return null;
    }

    public static Set<Lamp> getBrokenLamps(int varbitValue)
	{
		Set<Lamp> brokenLamps = Sets.newEnumSet(Collections.emptySet(), Lamp.class);
		for (int i = 0; i < 32; i++)
		{
			if ((varbitValue & (1 << i)) != 0)
			{
				Set<Lamp> lamps = LAMPS_BY_BIT_POSITION.get(i);
				if (lamps != null)
				{
					brokenLamps.addAll(lamps);
				}
			}
		}
		return brokenLamps;
	}

	public static Lamp getLamp(WorldPoint worldPoint)
	{
		for (Lamp lamp : Lamp.values())
		{
			if (lamp.getWorldPoint().equals(worldPoint))
			{
				return lamp;
			}
		}
		return null;
	}
}
