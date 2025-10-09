package com.dklamps;

import java.awt.Color;

import com.dklamps.enums.DisplayFloorTypes;
import com.dklamps.enums.HighlightTypes;
import com.dklamps.enums.TimerTypes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("dorgeshkaanlamps")
public interface DKLampsConfig extends Config{
    @ConfigItem(
        keyName = "showHintArrow",
        name = "Show Hint Arrow",
        description = "Show a hint arrow pointing to the nearest broken lamp.",
        position = 1
    )
	default boolean showHintArrow() {
		return true;
	}

	@ConfigItem(
		keyName = "showPathToClosestLamp",
		name = "Show Path to Closest Lamp",
		description = "Draw a path on the ground to the closest broken lamp",
		position = 3
	)
	default boolean showPathToClosestLamp() {
		return true;
	}

	@ConfigItem(
		keyName = "pathColor",
		name = "Path Color",
		description = "The color of the path to the closest lamp",
		position = 4
	)
	default Color pathColor() {
		return new Color(255,102,102);
	}

	@ConfigSection(
        name = "Highlight Lamps",
        description = "Settings for highlighting Dorgesh-Kaan lamps.",
        position = 10
	)
	String lampsSection = "lampsSection";

    @ConfigItem(
		keyName = "highlightBrokenLamps",
		name = "Broken Lamps",
		description = "Highlight known broken lamps.",
        section = lampsSection,
        position = 1
	)
	default boolean highlightBrokenLamps() {
		return true;
	}

    @ConfigItem(
		keyName = "highlightWorkingLamps",
		name = "Working Lamps",
		description = "Highlight known working lamps.",
        section = lampsSection,
        position = 2
	)
	default boolean highlightWorkingLamps() {
		return false;
	}

    @ConfigItem(
		keyName = "highlightAllLamps",
		name = "All Lamps",
		description = "Highlight all lamps.",
        section = lampsSection,
        position = 3
	)
	default boolean highlightAllLamps() {
		return false;
	}

    @ConfigItem(
		keyName = "brokenLampColor",
		name = "Broken Lamp Color",
		description = "The color of the overlay for broken lamps.",
        section = lampsSection,
        position = 4
	)
	default Color getBrokenLampColor() {
		return Color.RED;
	}

    @ConfigItem(
		keyName = "workingLampColor",
		name = "Working Lamp Color",
		description = "The color of the overlay for working lamps.",
        section = lampsSection,
        position = 5
	)
	default Color getWorkingLampColor() {
		return Color.GREEN;
	}

    @ConfigItem(
		keyName = "defaultLampColor",
		name = "Default Lamp Color",
		description = "The default color of the overlay for lamps.",
        section = lampsSection,
        position = 6
	)
	default Color getDefaultLampColor() {
		return Color.GRAY;
	}

    @ConfigItem(
		keyName = "highlightAllPlanesLamps",
		name = "Other Planes",
		description = "Highlight lamps on other planes.",
        section = lampsSection,
        position = 7
	)
	default boolean highlightOtherPlanesLamps() {
		return false;
	}

    @ConfigItem(
		keyName = "darkenOtherPlanesLamps",
		name = "Darken Other Planes",
		description = "Darken lamps on other planes.",
        section = lampsSection,
        position = 8
	)
	default boolean darkenOtherPlanesLamps() {
		return true;
	}

    @ConfigSection(
        name = "Side Panel",
        description = "Settings for the side panel map.",
        position = 50
	)
	String sidePanelSection = "sidePanel";

    @ConfigItem(
        keyName = "enableSidePanel",
        name = "Enable Side Panel",
        description = "Enable the side panel map.",
        section = sidePanelSection,
        position = 1
    )
    default boolean enableSidePanel() {
        return true;
    }


    @ConfigItem(
        keyName = "displayWorkingLampsInPanel",
        name = "Display Working Lamps",
        description = "Display working lamps in the side panel map.",
        section = sidePanelSection,
        position = 2
    )
    default DisplayFloorTypes displayWorkingLampsInPanel() {
        return DisplayFloorTypes.ALL_FLOORS;
    }

    @ConfigItem(
        keyName = "displayPlayerInPanel",
        name = "Display Player",
        description = "Display the player's location in the side panel map.",
        section = sidePanelSection,
        position = 3
    )
    default DisplayFloorTypes displayPlayerInPanel() {
        return DisplayFloorTypes.ALL_FLOORS;
    }

    @Range(min = 1, max = 100)
    @ConfigItem(
        keyName = "playerOpacityOtherFloors",
        name = "Player Opacity Other Floors",
        description = "The opacity of the player icon on other floors.",
        section = sidePanelSection,
        position = 4
    )
    default int playerOpacityOtherFloors() {
        return 75;
    }

    @ConfigSection(
        name = "Hints",
        description = "Settings for additional hints.",
        position = 98
	)
	String hintsSection = "hintsSection";

    @ConfigItem(
		keyName = "highlightWireMachine",
		name = "Highlight Wire Machine",
		description = "Highlight the wire machine and show a respawn timer.",
        section = hintsSection,
		position = 1
	)
	default boolean highlightWireMachine() {
		return true;
	}

    @ConfigItem(
        keyName = "timerType",
        name = "Timer Display Type",
        description = "The UI style for displaying the respawn timer on the wire machine.",
        section = hintsSection,
		position = 2
    )
    default TimerTypes timerType() {
        return TimerTypes.PIE;
    }

	@ConfigItem(
		keyName = "highlightClosedDoors",
		name = "Closed Doors",
		description = "Highlight closed doors in Dorgesh-Kaan.",
		section = hintsSection,
        position = 10
	)
	default boolean highlightClosedDoors() {
		return false;
	}

    @ConfigItem(
		keyName = "highlightStairs",
		name = "Highlight Stairs/Ladders",
		description = "Highlight stairs and ladders in Dorgesh-Kaan.",
		section = hintsSection,
        position = 20
	)
	default boolean highlightStairs() {
		return false;
	}

    @ConfigItem(
		keyName = "wireMachineHighlightColor",
		name = "Wire Machine Color",
		description = "The color to highlight the wire machine.",
        section = hintsSection,
        position = 100
	)
	default Color wireMachineHighlightColor() {
		return Color.ORANGE;
	}

	@ConfigItem(
		keyName = "doorHighlightColor",
		name = "Door Highlight Color",
		description = "The color to highlight closed doors.",
		section = hintsSection,
        position = 110
	)
	default Color doorHighlightColor() {
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "stairHighlightColor",
		name = "Stair/Ladder Highlight Color",
		description = "The color to highlight stairs and ladders.",
		section = hintsSection,
        position = 120
	)
	default Color stairHighlightColor() {
		return Color.CYAN;
	}


    @ConfigSection(
        name = "Highlight Style",
        description = "Settings for highlight style.",
        position = 99
    )
    String highlightStyleSection = "highlightStyleSection";

    @ConfigItem(
        keyName = "highlightStyle",
        name = "Highlight Style",
        description = "Choose how to highlight lamps.",
        section = highlightStyleSection,
        position = 1
    )
    default HighlightTypes highlightStyle() {
        return HighlightTypes.HIGHLIGHT_CLICKBOX;
    }

    @Range(min = 0, max = 4)
	@ConfigItem(
		keyName = "borderFeather",
		name = "Border Feather",
		description = "The feathering amount for the border highlight.",
        section = highlightStyleSection,
        position = 2
	)
	default int borderFeather() {
		return 0;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "borderThickness",
		name = "Border Thickness",
		description = "The thickness of the border highlight.",
        section = highlightStyleSection,
        position = 3
	)
	default int borderThickness() {
		return 2;
	}
}
