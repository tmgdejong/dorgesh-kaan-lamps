import json

CENTRE_Y = 5312
DK_WEST = 2688
DK_EAST = 2751
DK_NORTH = 5375
DK_SOUTH = 5248

with open('feature_navigation.json', 'r') as f:
    features = json.load(f)

filtered_features = {}

for feature,values in features.items():
    for v in values:
        if (DK_WEST < v["origin"]["x"] < DK_EAST and
                DK_SOUTH < v["origin"]["y"] <DK_NORTH):
            filtered_features[feature] = values
            print(f"{feature} has value in range")
            break

with open('feature_nav_filtered.json', 'w') as f:
    json.dump(filtered_features, f, indent=4)
    

            
          
