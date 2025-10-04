import json
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import os
from pathlib import Path

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

def visualize_map(collision_data, filename="", output_path=None):
    """Creates a visual grid representation of the collision map data and saves it."""
    if not collision_data:
        return

    # collision_data is structured as [x][y], so first dimension is width (X)
    width = len(collision_data)
    height = len(collision_data[0])

    # First pass: collect all unique feature values
    feature_values = set()
    for x in range(width):
        for y in range(height):
            tile_data = collision_data[x][y]
            if 'f' in tile_data:
                feature_values.add(tile_data['f'])
    
    # Generate a color map for features
    feature_values = sorted(feature_values)
    color_palette = plt.cm.tab20.colors if len(feature_values) <= 20 else plt.cm.viridis(
        [i / len(feature_values) for i in range(len(feature_values))]
    )
    feature_colors = {val: color_palette[i % len(color_palette)] for i, val in enumerate(feature_values)}

    fig, ax = plt.subplots(figsize=(12, 12))
    ax.set_aspect('equal', adjustable='box')

    # Iterate through the data with proper X and Y mapping
    for x in range(width):
        for y in range(height):
            tile_data = collision_data[x][y]
            color = 'black' # Default to blocked/wall
            
            if 'f' in tile_data:
                color = feature_colors[tile_data['f']] # Feature tile with unique color
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
    ax.set_xlim(0, width)
    ax.set_ylim(0, height)
    # Don't invert Y-axis - OSRS uses standard coordinates with Y increasing northward
    plt.title(f"Collision Map: {filename}", fontsize=16)
    plt.axis('off')
    
    # Add legend for feature values if any exist
    if feature_values:
        legend_elements = [patches.Patch(facecolor=feature_colors[val], edgecolor='black', 
                                        label=f'Feature {val}') 
                          for val in feature_values]
        legend_elements.insert(0, patches.Patch(facecolor='white', edgecolor='black', label='Walkable'))
        legend_elements.insert(0, patches.Patch(facecolor='black', edgecolor='black', label='Blocked'))
        ax.legend(handles=legend_elements, loc='upper left', bbox_to_anchor=(1, 1), 
                 fontsize=10, framealpha=0.9)
    
    plt.tight_layout()
    
    if output_path:
        plt.savefig(output_path, dpi=150, bbox_inches='tight')
        plt.close()
    else:
        plt.show()

if __name__ == '__main__':
    # Directory containing collision map JSON files
    script_dir = Path(__file__).parent
    input_dir = script_dir
    output_dir = script_dir / 'collision_map_images'
    
    # Create output directory
    output_dir.mkdir(exist_ok=True)
    
    # Process all JSON files in the collision_maps directory
    json_files = list(input_dir.glob('*.json'))
    
    if not json_files:
        print(f"No JSON files found in {input_dir}")
    else:
        print(f"Found {len(json_files)} JSON files to process...")
        
        for json_file in json_files:
            print(f"Processing {json_file.name}...")
            collision_map_data = load_collision_map(str(json_file))
            
            if collision_map_data:
                output_filename = json_file.stem + '.png'
                output_path = output_dir / output_filename
                visualize_map(collision_map_data, filename=json_file.name, output_path=str(output_path))
                print(f"  Saved to {output_path}")
        
        print(f"\nDone! All images saved to {output_dir}")
