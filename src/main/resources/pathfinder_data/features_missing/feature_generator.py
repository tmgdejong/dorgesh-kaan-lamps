import json
import argparse
import os
import sys

def generate_feature_json(feature_map_string, origin_x, origin_y, feature_id, plane, save_file=False, output_dir=None):
    """
    Generates a Dorgesh-Kaan feature JSON object from a simple text map.
    ' ' = Walkable tile
    'X' = Non-walkable tile
    
    Args:
        feature_map_string: String representation of the room layout
        origin_x: X coordinate of the south-west corner
        origin_y: Y coordinate of the south-west corner
        feature_id: Unique feature identifier
        plane: Floor level (0, 1, 2, etc.)
        save_file: If True, save the JSON to a file
        output_dir: Directory to save the file (default: current directory)
    
    Returns:
        dict: The generated feature data
    """
    lines = feature_map_string.strip().split('\n')
    
    # Validate that all lines have the same length
    if lines:
        expected_length = len(lines[0])
        for i, line in enumerate(lines):
            if len(line) != expected_length:
                raise ValueError(f"Line {i+1} has length {len(line)}, expected {expected_length}. All lines must have the same length!")
    
    size_y = len(lines)
    size_x = len(lines[0]) if lines else 0

    # Build a flat list of characters in the correct column-major, bottom-to-top order.
    flat_char_list = []
    for x in range(size_x):
        for y_line_idx in range(size_y - 1, -1, -1):
            flat_char_list.append(lines[y_line_idx][x])

    # The 'd' string is run-length encoded. It must start with a walkable count.
    d_values = [size_x, size_y]
    
    # If the very first tile (south-west corner) is blocked, the initial walkable run is 0.
    if not flat_char_list or flat_char_list[0] == 'X':
        d_values.append(0)

    # Perform run-length encoding on the correctly ordered flat list
    if flat_char_list:
        current_char = flat_char_list[0]
        count = 0
        for char in flat_char_list:
            if char == current_char:
                count += 1
            else:
                d_values.append(count * 16) # A game tile has 4x4 sub-tiles
                current_char = char
                count = 1
        
        # Add the final run
        d_values.append(count * 16)

    # Assemble the final JSON object
    feature_data = {
        "f": feature_id,
        "s": {"x": size_x, "y": size_y},
        "o": {"x": origin_x, "y": origin_y},
        "d": d_values
    }

    # Generate the filename
    filename = f"feature_{plane}_{feature_id}.json"
    
    # Print the results
    print("-" * 50)
    print(f"Generated data for: {filename}")
    print("-" * 50)
    print(json.dumps(feature_data, indent=4))
    print("-" * 50)
    
    # Save to file if requested
    if save_file:
        if output_dir is None:
            output_dir = os.path.dirname(os.path.abspath(__file__))
        else:
            os.makedirs(output_dir, exist_ok=True)
            output_dir = os.path.abspath(output_dir)
        
        filepath = os.path.join(output_dir, filename)
        
        # Check if file already exists
        if os.path.exists(filepath):
            response = input(f"File '{filename}' already exists. Overwrite? (y/n): ")
            if response.lower() != 'y':
                print("File not saved.")
                return feature_data
        
        with open(filepath, 'w') as f:
            json.dump(feature_data, f, indent=4)
        
        print(f"✓ File saved to: {filepath}")
    else:
        print("\nCopy the JSON object above and save it as a new file named:")
        print(f"'{filename}' in your features directory.")
    
    print("-" * 50)
    
    return feature_data

def load_map_from_file(filepath):
    """Load a room layout from a text file."""
    try:
        with open(filepath, 'r') as f:
            return f.read()
    except FileNotFoundError:
        print(f"Error: The file '{filepath}' was not found.")
        sys.exit(1)

def interactive_mode():
    """Run the generator in interactive mode, prompting for all parameters."""
    print("=" * 50)
    print("Feature Generator - Interactive Mode")
    print("=" * 50)
    
    # Get feature ID
    while True:
        try:
            feature_id = int(input("Enter feature ID (e.g., 50001): "))
            break
        except ValueError:
            print("Please enter a valid integer.")
    
    # Get plane
    while True:
        try:
            plane = int(input("Enter plane/floor level (0, 1, 2, etc.): "))
            break
        except ValueError:
            print("Please enter a valid integer.")
    
    # Get origin coordinates
    while True:
        try:
            origin_x = int(input("Enter origin X coordinate (south-west corner): "))
            origin_y = int(input("Enter origin Y coordinate (south-west corner): "))
            break
        except ValueError:
            print("Please enter valid integers.")
    
    # Get room layout
    print("\nEnter room layout (use ' ' for walkable, 'X' for walls).")
    print("Enter a blank line when finished:")
    lines = []
    while True:
        line = input()
        if line == "":
            break
        lines.append(line)
    
    room_layout = "\n".join(lines)
    
    # Ask if user wants to save
    save = input("\nSave to file? (y/n): ").lower() == 'y'
    output_dir = None
    
    if save:
        output_dir = input("Enter output directory (leave blank for current directory): ").strip()
        if not output_dir:
            output_dir = None
    
    # Generate
    try:
        generate_feature_json(room_layout, origin_x, origin_y, feature_id, plane, save, output_dir)
    except ValueError as e:
        print(f"\nError: {e}")

def main():
    parser = argparse.ArgumentParser(
        description='Generate Dorgesh-Kaan feature JSON files from text-based room layouts.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python create_feature.py -i
  
  # Generate from command line arguments (use quotes for the map string)
  python create_feature.py -f 50001 -p 1 -x 2700 -y 5350 -m "XXX\\nX X\\nXXX"
  
  # Load layout from file and save output to a 'features' subfolder
  python create_feature.py -f 50001 -p 1 -x 2700 -y 5350 --map-file layout.txt -s -o features
  
  # Use the hardcoded example
  python create_feature.py --example
        """
    )
    
    parser.add_argument('-i', '--interactive', action='store_true',
                        help='Run in interactive mode')
    parser.add_argument('-f', '--feature-id', type=int,
                        help='Feature ID')
    parser.add_argument('-p', '--plane', type=int,
                        help='Plane/floor level')
    parser.add_argument('-x', '--origin-x', type=int,
                        help='Origin X coordinate (south-west corner)')
    parser.add_argument('-y', '--origin-y', type=int,
                        help='Origin Y coordinate (south-west corner)')
    parser.add_argument('-m', '--map', type=str,
                        help='Room layout as a string (use \\n for newlines in your shell)')
    parser.add_argument('--map-file', type=str,
                        help='Load room layout from a file')
    parser.add_argument('-s', '--save', action='store_true',
                        help='Save output to a JSON file')
    parser.add_argument('-o', '--output-dir', type=str,
                        help='Output directory for saved file')
    parser.add_argument('--example', action='store_true',
                        help='Run with hardcoded example values')
    
    args = parser.parse_args()
    
    # Interactive mode
    if args.interactive:
        interactive_mode()
        return
    
    # Example mode
    if args.example:
        room_layout = """
XXXXXXXXX
X       X
X       X
X   X   X
X       X
X       X
XXXXXXXXX
"""
        generate_feature_json(room_layout, 2700, 5350, 50001, 1, args.save, args.output_dir)
        return
    
    # Default to help if no arguments are given
    if len(sys.argv) == 1:
        parser.print_help(sys.stderr)
        sys.exit(1)

    # Command-line mode
    if args.feature_id is None or args.plane is None or args.origin_x is None or args.origin_y is None:
        parser.error("Required arguments: -f/--feature-id, -p/--plane, -x/--origin-x, -y/--origin-y (or use -i)")
    
    # Get room layout
    if args.map_file:
        room_layout = load_map_from_file(args.map_file)
    elif args.map:
        room_layout = args.map.replace('\\n', '\n')
    else:
        parser.error("Either --map or --map-file must be provided")
    
    # Generate
    try:
        generate_feature_json(room_layout, args.origin_x, args.origin_y, args.feature_id, args.plane, args.save, args.output_dir)
    except ValueError as e:
        print(f"\nError: {e}")

if __name__ == '__main__':
    main()

