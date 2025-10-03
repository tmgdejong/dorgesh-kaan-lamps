import json
import matplotlib.pyplot as plt
import matplotlib.patches as patches

def load_collision_map(filepath):
    """Loads a 64x64 collision map from a JSON file."""
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: The file '{filepath}' was not found.")
        return None
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from the file '{filepath}'.")
        return None

def visualize_map(collision_data, filename=""):
    """Creates a visual grid representation of the collision map data."""
    if not collision_data:
        return

    rows = len(collision_data)
    cols = len(collision_data[0])

    fig, ax = plt.subplots(figsize=(12, 12))
    ax.set_aspect('equal', adjustable='box')

    for y, row_data in enumerate(collision_data):
        for x, tile_data in enumerate(row_data):
            color = 'black' # Default to blocked/wall
            
            if 'f' in tile_data:
                color = 'cornflowerblue' # Feature tile
            elif tile_data.get('c') == False:
                color = 'white' # Walkable tile

            # Draw the tile background
            rect = patches.Rectangle((x, y), 1, 1, linewidth=1, edgecolor='gainsboro', facecolor=color)
            ax.add_patch(rect)

            # Draw directional blockers as red lines
            if tile_data.get('n') == False:
                ax.plot([x, x + 1], [y + 1, y + 1], color='red', linewidth=2)
            if tile_data.get('s') == False:
                ax.plot([x, x + 1], [y, y], color='red', linewidth=2)
            if tile_data.get('e') == False:
                ax.plot([x + 1, x + 1], [y, y + 1], color='red', linewidth=2)
            if tile_data.get('w') == False:
                ax.plot([x, x], [y, y + 1], color='red', linewidth=2)

    # Configure plot appearance
    ax.set_xlim(0, cols)
    ax.set_ylim(0, rows)
    ax.invert_yaxis() # Puts (0,0) in the top-left corner
    plt.title(f"Collision Map: {filename}", fontsize=16)
    plt.axis('off')
    plt.tight_layout()
    plt.show()

if __name__ == '__main__':
    # --- IMPORTANT ---
    # Change this path to the actual location of your collision map file.
    # For example: 'src/main/resources/collision_maps/0_42_82.json'
    FILE_PATH = 'tmgdejong/dorgesh-kaan-lamps/dorgesh-kaan-lamps-1b1a0526ae13a325a092dc2457cc5d717af3ed37/src/main/resources/collision_maps/0_42_82.json'
    
    collision_map_data = load_collision_map(FILE_PATH)
    if collision_map_data:
        visualize_map(collision_map_data, filename=FILE_PATH.split('/')[-1])
