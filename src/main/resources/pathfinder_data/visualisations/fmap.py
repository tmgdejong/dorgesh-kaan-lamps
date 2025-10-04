import json
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np
import os
from pathlib import Path

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
    
    width = size['x'] * 4
    height = size['y'] * 4

    decompressed_list = []
    for i, length in enumerate(run_length_data):
        # Even indices are walkable (True), odd indices are blocked (False)
        is_walkable = (i % 2 == 0)
        decompressed_list.extend([is_walkable] * length)
    
    # Convert the flat list into a 2D numpy array (grid)
    # OSRS map data is stored with X as the first dimension, so reshape as (width, height) then transpose
    try:
        grid = np.array(decompressed_list).reshape((width, height)).T
        return grid
    except ValueError as e:
        print(f"Error reshaping data: {e}")
        print(f"Expected {width*height} tiles, but got {len(decompressed_list)}.")
        return None


def visualize_decompressed_map(grid, filename="", output_path=None):
    """Creates a visual grid representation of the decompressed feature data and saves it."""
    if grid is None:
        return

    rows, cols = grid.shape

    fig, ax = plt.subplots(figsize=(12, 12))
    ax.set_aspect('equal', adjustable='box')

    # Grid is now (rows, cols) which corresponds to (y, x) in map coordinates
    for y in range(rows):
        for x in range(cols):
            # True is walkable (white), False is blocked (black)
            color = 'white' if grid[y, x] else 'black'
            
            rect = patches.Rectangle((x, y), 1, 1, linewidth=1, edgecolor='gainsboro', facecolor=color)
            ax.add_patch(rect)

    ax.set_xlim(0, cols)
    ax.set_ylim(0, rows)
    # Don't invert Y-axis - OSRS uses standard coordinates with Y increasing northward
    plt.title(f"Decompressed Feature Map: {filename}", fontsize=16)
    plt.axis('off')
    plt.tight_layout()
    
    if output_path:
        plt.savefig(output_path, dpi=150, bbox_inches='tight')
        plt.close()
    else:
        plt.show()

if __name__ == '__main__':
    # Directory containing feature map JSON files
    script_dir = Path(__file__).parent
    input_dir = script_dir / 'features' / 'features_json'
    output_dir = script_dir / 'feature_map_images'
    
    # Create output directory
    output_dir.mkdir(exist_ok=True)
    
    # Process all JSON files in the features_json directory
    json_files = list(input_dir.glob('*.json'))
    
    if not json_files:
        print(f"No JSON files found in {input_dir}")
    else:
        print(f"Found {len(json_files)} JSON files to process...")
        
        for json_file in json_files:
            print(f"Processing {json_file.name}...")
            feature_map_data = load_feature_map(str(json_file))
            
            if feature_map_data:
                decompressed_grid = decompress_feature_data(feature_map_data)
                if decompressed_grid is not None:
                    output_filename = json_file.stem + '.png'
                    output_path = output_dir / output_filename
                    visualize_decompressed_map(decompressed_grid, filename=json_file.name, output_path=str(output_path))
                    print(f"  Saved to {output_path}")
        
        print(f"\nDone! All images saved to {output_dir}")
