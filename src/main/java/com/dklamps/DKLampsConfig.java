package com.dklamps;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("dorgeshkaanlamps")
public interface DKLampsConfig extends Config
{

    @ConfigItem(
		keyName = "defaultLampColor",
		name = "Default Lamp Color",
		description = "The default color for lamps."
	)
	default Color getDefaultLampColor()
	{
		return Color.GRAY;
	}

	@ConfigItem(
		keyName = "brokenLampColor",
		name = "Broken Lamp Color",
		description = "The color of the overlay for broken lamps."
	)
	default Color getBrokenLampColor()
	{
		return Color.RED;
	}

    @ConfigItem(
		keyName = "workingLampColor",
		name = "Working Lamp Color",
		description = "The color of the overlay for working lamps."
	)
	default Color getWorkingLampColor()
	{
		return Color.GREEN;
	}

    @ConfigItem(
        keyName = "showHintArrow",
        name = "Show Hint Arrow",
        description = "Show a hint arrow pointing to the nearest broken lamp."
    )
    default boolean showHintArrow()
    {
        return true;
    }

    @ConfigItem(
		keyName = "showAllLamps",
		name = "Show All Lamps",
		description = "Show overlays for all lamps."
	)
	default boolean showAllLamps()
	{
		return false;
	}

    @ConfigItem(
		keyName = "showKnownWorkingLamps",
		name = "Show Known Working Lamps",
		description = "Also show overlays and map markers for known working lamps."
	)
	default boolean showKnownWorkingLamps()
	{
		return false;
	}

    @ConfigItem(
        keyName = "highlightStyle",
        name = "Highlight Style",
        description = "Choose how to highlight lamps."
    )
    default HighlightStyle highlightStyle()
    {
        return HighlightStyle.HIGHLIGHT_CLICKBOX;
    }

	@Range(min = 0, max = 4)
	@ConfigItem(
		keyName = "borderFeather",
		name = "Border Feather",
		description = "The feathering amount for the border highlight."
	)
	default int borderFeather()
	{
		return 0;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "borderThickness",
		name = "Border Thickness",
		description = "The thickness of the border highlight."
	)
	default int borderThickness()
	{
		return 2;
	}
}
