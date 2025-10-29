package com.dklamps;

import java.awt.Color;

import com.dklamps.enums.DisplayFloorType;
import com.dklamps.enums.HighlightType;
import com.dklamps.enums.TimerType;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("dklamps")
public interface DKLampsConfig extends Config{


	@ConfigItem(
		keyName = "showPathToLocation",
		name = "Show Path to Locations",
		description = "Draw a path on the ground to the closest broken lamp or utility locations",
		position = 3
	)
	default boolean showPathToLocation() {
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

    @ConfigItem(
		keyName = "utilityPathColor",
		name = "Utility Path Color",
		description = "The color of the path to the bank or wiring machine",
		position = 4
	)
	default Color utilityPathColor() {
		return new Color(102,255,102);
	}

    @ConfigItem(
        keyName = "maxPathDistance",
        name = "Max Path Distance",
        description = "Highlight teleport sphere if the path is longer than this distance. 0 to disable.",
        position = 5
    )
    default int maxPathDistance()
    {
        return 64;
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
    default DisplayFloorType displayWorkingLampsInPanel() {
        return DisplayFloorType.ALL_FLOORS;
    }

    @ConfigItem(
        keyName = "displayPlayerInPanel",
        name = "Display Player",
        description = "Display the player's location in the side panel map.",
        section = sidePanelSection,
        position = 3
    )
    default DisplayFloorType displayPlayerInPanel() {
        return DisplayFloorType.ALL_FLOORS;
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
        name = "Stats Overlay",
        description = "Settings for the statistics overlay",
        position = 80
    )
    String statsOverlaySection = "statsOverlaySection";

    @ConfigItem(
        keyName = "showStatsOverlay",
        name = "Show Stats Overlay",
        description = "Toggles the statistics overlay",
        position = 81,
        section = statsOverlaySection
    )
    default boolean showStatsOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showClosestDistance",
        name = "Show Distance",
        description = "Show distance to closest lamp",
        position = 82,
        section = statsOverlaySection
    )
    default boolean showClosestDistance()
    {
        return true;
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
    default TimerType timerType() {
        return TimerType.PIE;
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
		keyName = "highlightInformativeStairs",
		name = "Highlight Informative Stairs",
		description = "Highlight stairs/ladders that lead to areas with unknown lamp information.",
		section = hintsSection,
        position = 20
	)
	default boolean highlightInformativeStairs() {
		return true;
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
		keyName = "informativeStairColor",
		name = "Informative Stair Color",
		description = "The color to highlight informative stairs/ladders.",
		section = hintsSection,
        position = 120
	)
	default Color informativeStairColor() {
		return Color.MAGENTA;
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
    default HighlightType highlightStyle() {
        return HighlightType.HIGHLIGHT_CLICKBOX;
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
