package com.dklamps.pathfinder;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import net.runelite.api.coords.WorldPoint;

public class TransportLoader {
    public static List<Transport> loadTransports() throws IOException {
        List<Transport> transports = new ArrayList<>();
        InputStream transportsStream = TransportLoader.class.getResourceAsStream("/transports.tsv");

        if (transportsStream == null) {
            throw new IOException("transports.tsv not found in resources");
        }

        try (Scanner scanner = new Scanner(transportsStream, StandardCharsets.UTF_8)) {
            // Skip the header line
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\t");

                // Ensure the line has enough parts to avoid errors
                if (parts.length < 10) { 
                    continue;
                }

                String[] originCoords = parts[0].split(" ");
                String[] destCoords = parts[1].split(" ");

                WorldPoint origin = new WorldPoint(
                    Integer.parseInt(originCoords[0]),
                    Integer.parseInt(originCoords[1]),
                    Integer.parseInt(originCoords[2])
                );

                WorldPoint destination = new WorldPoint(
                    Integer.parseInt(destCoords[0]),
                    Integer.parseInt(destCoords[1]),
                    Integer.parseInt(destCoords[2])
                );

                String menuOption = parts[2];
                String menuTarget = parts[3];
                int objectId = Integer.parseInt(parts[4]);
                int duration = Integer.parseInt(parts[9].trim());

                transports.add(new Transport(origin, destination, menuOption, menuTarget, objectId, duration));
            }
        }

        return transports;
    }
}