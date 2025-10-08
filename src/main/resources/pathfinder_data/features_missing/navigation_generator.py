import json
import os
import argparse
from datetime import datetime

def load_json_data(filepath, default_type=dict):
    """Loads a JSON file. Returns a default type if not found."""
    try:
        with open(filepath, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Warning: File '{filepath}' not found. A new one will be created.")
        return default_type()
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from '{filepath}'. Please check its format.")
        return None

def save_json_data(filepath, data):
    """Saves the updated data to a JSON file, creating a backup of the old file."""
    if os.path.exists(filepath):
        backup_path = filepath + '.' + datetime.now().strftime("%Y%m%d%H%M%S") + '.bak'
        print(f"Backing up existing file to '{backup_path}'...")
        os.rename(filepath, backup_path)
    
    with open(filepath, 'w') as f:
        # Sort keys for consistent file structure
        sorted_data = dict(sorted(data.items(), key=lambda item: [int(x) for x in item[0].split('_')]))
        # Also sort the lists within the feature table for consistency
        if isinstance(next(iter(sorted_data.values()), None), list):
             for key in sorted_data:
                sorted_data[key].sort(key=lambda item: [int(x) for x in item.split('_')])
        
        json.dump(sorted_data, f, indent=4)
    print(f"✓ Successfully saved updated data to '{filepath}'")

def get_next_row_number(nav_data):
    """Finds the highest existing rowNumber and returns the next integer."""
    max_row = 0
    for feature_key in nav_data:
        for link in nav_data[feature_key]:
            if "rowNumber" in link and link["rowNumber"] > max_row:
                max_row = link["rowNumber"]
    return max_row + 1

def interactive_add_door(nav_data, table_data):
    """Runs an interactive loop to add new door connections."""
    while True:
        print("\n--- Adding a New Door Connection ---")
        
        try:
            plane = int(input("Enter plane/floor (0, 1, or 2): "))
            
            origin_feature_id = int(input("Enter the Origin Feature ID (the room you are 'in'): "))
            origin_x = int(input(f"Enter the X coordinate of the tile INSIDE feature {origin_feature_id}: "))
            origin_y = int(input(f"Enter the Y coordinate of the tile INSIDE feature {origin_feature_id}: "))
            
            dest_feature_id = int(input("Enter the Destination Feature ID (the room you are going 'to'): "))
            
            direction = ""
            while direction not in ['N', 'E', 'S', 'W']:
                direction = input("Direction from origin to destination (N/E/S/W): ").upper()

        except ValueError:
            print("\nInvalid input. Please enter numbers for coordinates and IDs.")
            continue

        # Calculate destination coordinates
        dest_x, dest_y = origin_x, origin_y
        if direction == 'N':
            dest_y += 1
        elif direction == 'S':
            dest_y -= 1
        elif direction == 'E':
            dest_x += 1
        elif direction == 'W':
            dest_x -= 1

        origin_key = f"{plane}_{origin_feature_id}"
        dest_key = f"{plane}_{dest_feature_id}"

        # Get a unique ID for this pair of links
        next_id = get_next_row_number(nav_data)

        # Create the two-way links
        link_forward = {
            "rowNumber": next_id,
            "groupName": "Door (actions = [\"Open\"])",
            "no-repeat-key": str(next_id),
            "destination": {"plane": plane, "x": dest_x, "y": dest_y, "f": dest_key},
            "origin": {"plane": plane, "x": origin_x, "y": origin_y, "f": origin_key}
        }
        
        link_backward = {
            "rowNumber": next_id + 1,
            "groupName": "Door (actions = [\"Open\"])",
            "no-repeat-key": str(next_id + 1),
            "destination": {"plane": plane, "x": origin_x, "y": origin_y, "f": origin_key},
            "origin": {"plane": plane, "x": dest_x, "y": dest_y, "f": dest_key}
        }
        
        print("\n--- Generated Links ---")
        print("Forward Link (Origin -> Destination):")
        print(json.dumps(link_forward, indent=2))
        print("\nBackward Link (Destination -> Origin):")
        print(json.dumps(link_backward, indent=2))
        
        confirm = input("\nAdd these two links to both navigation and feature table files? (y/n): ").lower()
        
        if confirm == 'y':
            # Update navigation data
            if origin_key not in nav_data:
                nav_data[origin_key] = []
            nav_data[origin_key].append(link_forward)
            
            if dest_key not in nav_data:
                nav_data[dest_key] = []
            nav_data[dest_key].append(link_backward)
            print("Navigation links prepared.")

            # Update feature table data
            if origin_key not in table_data:
                table_data[origin_key] = []
            if dest_key not in table_data[origin_key]:
                table_data[origin_key].append(dest_key)

            if dest_key not in table_data:
                table_data[dest_key] = []
            if origin_key not in table_data[dest_key]:
                table_data[dest_key].append(origin_key)
            print("Feature table links prepared.")

        else:
            print("Operation cancelled.")
            
        if input("Add another door? (y/n): ").lower() != 'y':
            break
            
    return nav_data, table_data

def main():
    parser = argparse.ArgumentParser(description="Interactively add door connections to feature navigation and feature table JSON files.")
    parser.add_argument("nav_filepath", type=str, help="Path to the dk_feature_navigation.json file.")
    parser.add_argument("table_filepath", type=str, help="Path to the dk_features_table.json file.")
    args = parser.parse_args()

    nav_data = load_json_data(args.nav_filepath)
    table_data = load_json_data(args.table_filepath)

    if nav_data is not None and table_data is not None:
        updated_nav_data, updated_table_data = interactive_add_door(nav_data, table_data)
        
        save_json_data(args.nav_filepath, updated_nav_data)
        save_json_data(args.table_filepath, updated_table_data)

if __name__ == '__main__':
    main()

