from collections import defaultdict
import os
import json
from PIL import Image, ImageDraw, ImageFont

# --- Configuration ---
# Adjust these paths and settings as needed
FEATURES_DIR = './src/main/resources/pathfinder_data/features'
OUTPUT_IMAGE_PREFIX = 'dorgesh_kaan_floor'
TILE_SIZE = 70  # Pixels per game tile
COORDINATE_FONT_SIZE = 9
FEATURE_ID_FONT_SIZE = 24

# --- Colors ---
BACKGROUND_COLOR = (240, 240, 240)
WALKABLE_COLOR = (200, 255, 200)  # Light green for walkable tiles
NON_WALKABLE_COLOR = (255, 200, 200) # Light red for non-walkable tiles
TILE_OUTLINE_COLOR = (220, 220, 220)
BORDER_COLOR = (50, 50, 50)
TEXT_COLOR = (10, 10, 10)


def load_features_by_floor(directory):
    """Loads all feature JSON files from a directory and groups them by floor."""
    features_by_floor = defaultdict(list)
    print(f"Loading features from: {directory}")
    for filename in os.listdir(directory):
        if filename.startswith('feature_') and filename.endswith('.json'):
            try:
                # Extract floor number from filename (e.g., feature_0_14545.json -> 0)
                floor_str = filename.split('_')[1]
                floor = int(floor_str)
                
                filepath = os.path.join(directory, filename)
                with open(filepath, 'r') as f:
                    feature_data = json.load(f)
                    features_by_floor[floor].append(feature_data)
            except (json.JSONDecodeError, IOError, IndexError, ValueError) as e:
                print(f"Warning: Could not read, parse, or get floor from {filename}: {e}")
    
    for floor, features in features_by_floor.items():
         print(f"Found {len(features)} features on floor {floor}.")
    return features_by_floor

def get_map_bounds(features):
    """Calculates the min/max coordinates to determine canvas size."""
    if not features:
        return 0, 0, 0, 0

    min_x = float('inf')
    min_y = float('inf')
    max_x = float('-inf')
    max_y = float('-inf')

    for feature in features:
        origin = feature['o']
        size = feature['s']
        min_x = min(min_x, origin['x'])
        min_y = min(min_y, origin['y'])
        max_x = max(max_x, origin['x'] + size['x'])
        max_y = max(max_y, origin['y'] + size['y'])
        
    return min_x, min_y, max_x, max_y

def parse_walkability(feature_data):
    """Parses the 'd' string from feature data into a 2D game tile walkability grid,
    accounting for the 4x4 sub-tile resolution."""
    d = feature_data['d']
    size = feature_data['s']
    game_tile_width = size['x']
    game_tile_height = size['y']
    
    # The 'd' string describes a grid of sub-tiles, 4x4 for each game tile.
    sub_tile_width = game_tile_width * 4
    sub_tile_height = game_tile_height * 4
    
    # 1. Create a flat list of sub-tile walkability states from the run-length encoding.
    sub_tile_flat = []
    is_walkable_flag = True
    for run_length in d[2:]:
        sub_tile_flat.extend([is_walkable_flag] * run_length)
        is_walkable_flag = not is_walkable_flag
        
    # 2. Reshape the flat list into a 2D sub-tile grid.
    sub_tile_grid = [sub_tile_flat[i * sub_tile_height:(i + 1) * sub_tile_height] for i in range(game_tile_width * 4)]

    # 3. Create the final game tile grid. A game tile is walkable only if all
    #    of its 16 (4x4) sub-tiles are walkable.
    game_tile_grid = [[False for _ in range(game_tile_height)] for _ in range(game_tile_width)]

    for x in range(game_tile_width):
        for y in range(game_tile_height):
            is_game_tile_walkable = True
            # Check all 16 sub-tiles for the current game tile
            for sub_y_offset in range(4):
                for sub_x_offset in range(4):
                    if not sub_tile_grid[x * 4 + sub_x_offset][y * 4 + sub_y_offset]:
                        is_game_tile_walkable = False
                        break
                if not is_game_tile_walkable:
                    break
            game_tile_grid[x][y] = is_game_tile_walkable
            
    return game_tile_grid

def create_map_for_floor(floor_level, features_on_floor):
    """Generates and saves a map image for a single floor."""
    if not features_on_floor:
        print(f"No features to draw for floor {floor_level}.")
        return

    min_x, min_y, max_x, max_y = get_map_bounds(features_on_floor)

    canvas_width = (max_x - min_x) * TILE_SIZE
    canvas_height = (max_y - min_y) * TILE_SIZE

    print(f"Floor {floor_level} Map Bounds: X({min_x}, {max_x}), Y({min_y}, {max_y})")
    print(f"Floor {floor_level} Canvas Dimensions: {canvas_width}x{canvas_height} pixels")

    img = Image.new('RGB', (canvas_width, canvas_height), BACKGROUND_COLOR)
    draw = ImageDraw.Draw(img)

    try:
        coord_font = ImageFont.truetype("arial.ttf", COORDINATE_FONT_SIZE)
        feature_font = ImageFont.truetype("arialbd.ttf", FEATURE_ID_FONT_SIZE)
    except IOError:
        print("Warning: Arial font not found. Using default font.")
        coord_font = ImageFont.load_default()
        feature_font = ImageFont.load_default()

    for feature in features_on_floor:
        origin = feature['o']
        size = feature['s']
        feature_id = feature['f']
        
        walkability_grid = parse_walkability(feature)

        for x_offset in range(size['x']):
            for y_offset in range(size['y']):
                abs_x = origin['x'] + x_offset
                abs_y = origin['y'] + y_offset
                
                px = (abs_x - min_x) * TILE_SIZE
                # Invert Y for drawing (image origin is top-left)
                py = (max_y - abs_y - 1) * TILE_SIZE
                
                # Determine tile color based on walkability
                is_walkable = walkability_grid[x_offset][y_offset]
                tile_fill = WALKABLE_COLOR if is_walkable else NON_WALKABLE_COLOR
                
                # Draw the individual tile with its color
                draw.rectangle(
                    [px, py, px + TILE_SIZE, py + TILE_SIZE],
                    fill=tile_fill,
                    outline=TILE_OUTLINE_COLOR
                )
                
                # Draw coordinate string on top
                coord_text = f"{abs_x},{abs_y}"
                draw.text((px + 4, py + 4), coord_text, font=coord_font, fill=TEXT_COLOR)
                
        # Draw a thicker border around the entire feature
        fx1 = (origin['x'] - min_x) * TILE_SIZE
        fy1 = (max_y - (origin['y'] + size['y'])) * TILE_SIZE
        fx2 = fx1 + size['x'] * TILE_SIZE
        fy2 = fy1 + size['y'] * TILE_SIZE
        draw.rectangle([fx1, fy1, fx2, fy2], outline=BORDER_COLOR, width=2)

        # Draw the large feature ID in the center
        center_x_px = fx1 + (size['x'] * TILE_SIZE) / 2
        center_y_px = fy1 + (size['y'] * TILE_SIZE) / 2
        feature_id_text = str(feature_id)
        draw.text((center_x_px, center_y_px), feature_id_text, font=feature_font, fill='red', anchor='mm')

    output_path = f"{OUTPUT_IMAGE_PREFIX}_{floor_level}.png"
    img.save(output_path)
    print(f"Successfully created map for floor {floor_level} at: {output_path}")

def main():
    """Main function to generate the feature map images for each floor."""
    features_by_floor = load_features_by_floor(FEATURES_DIR)
    if not features_by_floor:
        print("No features found. Exiting.")
        return

    for floor_level, features_on_floor in sorted(features_by_floor.items()):
        print("-" * 30)
        create_map_for_floor(floor_level, features_on_floor)

if __name__ == '__main__':
    main()


