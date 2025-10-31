# Dorgesh-Kaan Lamps
A plugin to help you identify which lamps need to be fixed in Dorgesh-Kaan.

This plugin provides a comprehensive set of tools to make the Dorgesh-Kaan lamp-fixing minigame as efficient as possible. It guides you to the nearest broken lamp, helps you restock on supplies, and tracks your stats.

## Features
- **Smart Pathfinding:** Draws a tile-by-tile path to the nearest broken lamp.
- **Utility Pathing:** Automatically switches to pathing to the bank or the wire machine (in a separate color) when you run out of lightbulbs.
- **Lamp Highlighting:** Highlights broken lamps in the game world. You can also configure it to highlight working lamps or all lamps with custom colors.
- **Object Highlighting:**
  - Highlights the wire machine and displays its respawn timer when used.
  - Highlights "informative" stairs/ladders that lead to floors with unknown lamp statuses.
  - Highlights doors and stairs that are part of your currently calculated path.
  - Optionally highlights all closed doors in the city.
- **Side Panel Map:** Adds a convenient side panel with a full map of all three floors of Dorgesh-Kaan, showing the real-time status of every lamp and your player's location.
- **Stats Overlay:** Displays an overlay with your current target (Lamp, Bank, Wiring machine), the distance to it, lamps fixed this session, total lamps fixed, and your lamps-per-hour rate.
- **Teleport Hint:** Highlights your Dorgesh-Kaan sphere in your inventory if the closest broken lamp is very far away, letting you know it might be faster to teleport and reset the minigame.
---
## Contributions
Special thanks to the following plugins, of which a lot of inspiration and logic came:

### [Dorgesh-Kaan Lights Plugin](https://github.com/andmcadams/dk-lights)
Initial idea, gave great insight to the working of the varbit and the different areas
### [Shortest Path](https://github.com/Skretzo/shortest-path)
The entire pathfinding logic is based on this plugin
