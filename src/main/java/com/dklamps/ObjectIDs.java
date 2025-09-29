package com.dklamps;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public final class ObjectIDs
{
	private ObjectIDs()
	{
		// Private constructor to prevent instantiation
	}

	public static final int WIRE_MACHINE_ID = 22730;

	public static final Set<Integer> DOOR_IDS = ImmutableSet.of(
        22913,
        22914,
        22920,
        22921

	);

	public static final Set<Integer> STAIR_IDS = ImmutableSet.of(
        22931,
        22932,
        22933,
        22934,
        22935,
        22936,
        22937,
        22938,
        22939,
        22940,
        22941,
        22942
	);
}