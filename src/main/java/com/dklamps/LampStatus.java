package com.dklamps;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LampStatus
{
	BROKEN(Color.RED),
	FIXED(Color.GREEN),
	UNKNOWN(Color.GRAY);

	private final Color color;
}