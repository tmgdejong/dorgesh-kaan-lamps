import json
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np

def load_feature_map(filepath):
    """Loads a feature map from a JSON file."""
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: The file '{filepath}' was not found.")
        return None
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from the file '{filepath}'.")
        return None

def decompress_feature_data(feature_data):
    """Decompresses the run-length encoded 'd' array from a feature map."""
    if 'd' not in feature_data or 's' not in feature_data:
        print("Error: Feature data is missing 'd' (data) or 's' (size) key.")
        return None

    compressed_vec = feature_data['d']
    size = feature_data['s']
    
    # The first two elements of 'd' are width/height, which we get from 's'
    # The actual run-length data starts from the 3rd element.
    run_length_data = compressed_vec[2:]
    
    width = size['x']
    height = size['y']

    decompressed_list = []
    for i, length in enumerate(run_length_data):
        # Even indices are walkable (True), odd indices are blocked (False)
        is_walkable = (i % 2 == 0)
        decompressed_list.extend([is_walkable] * length)
    
    # Convert the flat list into a 2D numpy array (grid)
    # Note: OSRS map data is often stored column-major, so we reshape and transpose.
    try:
        grid = np.array(decompressed_list).reshape((height, width))
        return grid
    except ValueError as e:
        print(f"Error reshaping data: {e}")
        print(f"Expected {width*height} tiles, but got {len(decompressed_list)}.")
        return None


def visualize_decompressed_map(grid, filename=""):
    """Creates a visual grid representation of the decompressed feature data."""
    if grid is None:
        return

    rows, cols = grid.shape

    fig, ax = plt.subplots(figsize=(12, 12))
    ax.set_aspect('equal', adjustable='box')

    for y in range(rows):
        for x in range(cols):
            # True is walkable (white), False is blocked (black)
            color = 'white' if grid[y, x] else 'black'
            
            rect = patches.Rectangle((x, y), 1, 1, linewidth=1, edgecolor='gainsboro', facecolor=color)
            ax.add_patch(rect)

    ax.set_xlim(0, cols)
    ax.set_ylim(0, rows)
    ax.invert_yaxis()
    plt.title(f"Decompressed Feature Map: {filename}", fontsize=16)
    plt.axis('off')
    plt.tight_layout()
    plt.show()

if __name__ == '__main__':
    # --- IMPORTANT ---
    # Change this path to the actual location of your feature map file.
    # For example: 'src/main/resources/collision_maps/features/features_json/feature_0_14584.json'
    FILE_PATH = 'tmgdejong/dorgesh-kaan-lamps/dorgesh-kaan-lamps-1b1a0526ae13a325a092dc2457cc5d717af3ed37/src/main/resources/collision_maps/features/features_json/feature_0_14584.json'
    
    feature_map_data = load_feature_map(FILE_PATH)
    if feature_map_data:
        decompressed_grid = decompress_feature_data(feature_map_data)
        if decompressed_grid is not None:
            visualize_decompressed_map(decompressed_grid, filename=FILE_PATH.split('/')[-1])
