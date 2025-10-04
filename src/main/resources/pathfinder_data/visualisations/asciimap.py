import json

def load_collision_map(filepath):
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: The file '{filepath}' was not found.")
        return None
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from the file '{filepath}'.")
        return None

def generate_ascii_map(collision_data):
    if not collision_data:
        return

    map_string = ""
    for row_data in collision_data:
        row_string = ""
        for tile_data in row_data:
            char = '█'  # Blocked tile
            if 'f' in tile_data:
                char = 'F'  # Feature tile
            elif tile_data.get('c') == False:
                char = ' '  # Walkable tile
            row_string += char * 2
        map_string += row_string + "\n"
    
    print(map_string)
    print("\nLegend: ' ' = Walkable, '█' = Blocked, 'F' = Feature")


if __name__ == '__main__':
    # Change this path to the collision map you want to inspect.
    FILE_PATH = '0_42_82.json'
    
    collision_map_data = load_collision_map(FILE_PATH)
    if collision_map_data:
        print(f"--- ASCII Map for {FILE_PATH.split('/')[-1]} ---")
        generate_ascii_map(collision_map_data)


