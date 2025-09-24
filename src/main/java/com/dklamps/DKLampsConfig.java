package com.dklamps;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("dorgeshkaanlamps")
public interface DKLampsConfig extends Config
{
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
		keyName = "brokenLampColor",
		name = "Broken Lamp Color",
		description = "The color of the overlay for broken lamps."
	)
	default Color getBrokenLampColor()
	{
		return Color.RED;
	}

    @ConfigItem(
		keyName = "unknownLampColor",
		name = "Unknown Lamp Color",
		description = "The color for lamps with an unknown status."
	)
	default Color getUnknownLampColor()
	{
		return Color.GRAY;
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
        keyName = "highlightStyle",
        name = "Highlight Style",
        description = "Choose how to highlight lamps."
    )
    default HighlightStyle highlightStyle()
    {
        return HighlightStyle.HIGHLIGHT_CLICKBOX;
    }
}
