import json

def load_feature_map(filepath):
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: The file '{filepath}' was not found.")
        return None
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from the file '{filepath}'.")
        return None

def decompress_and_generate_ascii(feature_data):
    if 'd' not in feature_data or 's' not in feature_data:
        print("Error: Feature data is missing 'd' (data) or 's' (size) key.")
        return
    
    run_length_data = feature_data['d'][2:]
    width = feature_data['d'][1]*4
    height = feature_data['d'][0]*4

    print(f"width: {width}")
    print(f"height: {height}")
    print(f"area: {width*height}")
    print(f"Length data: {sum(run_length_data)}")
    print(run_length_data)
    
    decompressed_list = []
    for i, l in enumerate(run_length_data):
        print(l)
        is_walkable = (i % 2 == 0)
        array = [is_walkable] * l
        decompressed_list.extend([is_walkable] * l)

    if len(decompressed_list) != width * height:
        print(f"Error: Decompressed data size mismatch. Expected {width*height}, got {len(decompressed_list)}.")
        return

    map_string = ""
    for i in range(height):
        row_string = ""
        for j in range(width):
            is_walkable = decompressed_list[i * width + j]
            char = ' ' if is_walkable else '█'
            row_string += char * 2
        map_string += row_string + "\n"
        
    print(map_string)
    print("\nLegend: ' ' = Walkable, '█' = Blocked")


if __name__ == '__main__':
    # Change this path to the feature map you want to inspect.
    FILE_PATH = './features/features_json/feature_0_14584.json'
    
    feature_map_data = load_feature_map(FILE_PATH)
    if feature_map_data:
        print(f"--- ASCII Map for {FILE_PATH.split('/')[-1]} ---")
        decompress_and_generate_ascii(feature_map_data)


