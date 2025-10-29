package com.dklamps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.time.Instant;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import com.dklamps.enums.HighlightType;
import com.dklamps.enums.Lamp;
import com.dklamps.enums.LampStatus;
import com.dklamps.enums.TargetType;
import com.dklamps.enums.TimerType;
import com.dklamps.enums.Transport;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Slf4j
public class DKLampsOverlay extends Overlay {

    private final Client client;
    private final DKLampsPlugin plugin;
    private final DKLampsConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    private DKLampsOverlay(Client client, DKLampsPlugin plugin, DKLampsConfig config,
            ModelOutlineRenderer modelOutlineRenderer) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getLocalPlayer() == null) {
            return null;
        }

        Set<WorldPoint> pathPoints = new HashSet<>(plugin.getNavigationManager().getShortestPath());

        renderLamps(graphics);

        if (config.highlightClosedDoors()) {
            for (WallObject door : plugin.getStateManager().getDoors()) {
                if (door.getPlane() != client.getTopLevelWorldView().getPlane()) {
                    continue;
                }

                if (!pathPoints.contains(door.getWorldLocation())) {
                    renderTileObject(door, config.doorHighlightColor(), graphics);
                }
            }
        }
        
        if (config.highlightInformativeStairs()) {
            for (GameObject stair : plugin.getStateManager().getStairs()) {
                if (stair.getPlane() != client.getTopLevelWorldView().getPlane()) {
                    continue;
                }
                if (plugin.getStateManager().getInformativeStairs().contains(stair.getWorldLocation())
                        && !pathPoints.contains(stair.getWorldLocation())) {
                    renderTileObject(stair, config.informativeStairColor(), graphics);
                }
            }
        }

        if (config.highlightWireMachine() && plugin.getStateManager().getWireMachine() != null) {
            GameObject wireMachine = plugin.getStateManager().getWireMachine();
            if (wireMachine.getId() == DKLampsConstants.WIRE_MACHINE_ACTIVE) {
                renderTileObject(wireMachine, config.wireMachineHighlightColor(), graphics);
            } else {
                renderWireTimer(graphics);
            }
        }

        drawPathToClosestLamp(graphics, pathPoints);

        return null;
    }

    private void renderLamps(Graphics2D graphics) {
        for (GameObject lampObject : plugin.getStateManager().getSpawnedLamps().values()) {
            if (lampObject.getPlane() != client.getTopLevelWorldView().getPlane()
                    && !config.highlightOtherPlanesLamps()) {
                continue;
            }

            Lamp lamp = DKLampsHelper.getLamp(lampObject.getId());
            if (lamp == null) {
                continue;
            }

            LampStatus status = plugin.getStateManager().getLampStatuses().getOrDefault(lamp, LampStatus.UNKNOWN);

            Color color;
            if (status == LampStatus.BROKEN && config.highlightBrokenLamps()) {
                color = config.getBrokenLampColor();
            } else if (status == LampStatus.WORKING && config.highlightWorkingLamps()) {
                color = config.getWorkingLampColor();
            } else if (config.highlightAllLamps()) {
                color = config.getDefaultLampColor();
            } else {
                continue;
            }

            if (lampObject.getPlane() != client.getTopLevelWorldView().getPlane() && config.darkenOtherPlanesLamps()) {
                color = color.darker();
            }

            renderTileObject(lampObject, color, graphics);
        }
    }

    private void renderTileObject(TileObject tileObject, Color color, Graphics2D graphics) {
        HighlightType style = config.highlightStyle();
        switch (style) {
            case HIGHLIGHT_BORDER:
                modelOutlineRenderer.drawOutline(tileObject, config.borderThickness(), color, config.borderFeather());
                break;
            case HIGHLIGHT_CLICKBOX:
                Shape clickbox = tileObject.getClickbox();
                if (clickbox != null) {
                    Point mousePosition = client.getMouseCanvasPosition();
                    if (clickbox.contains(mousePosition.getX(), mousePosition.getY())) {
                        graphics.setColor(color.darker());
                    } else {
                        graphics.setColor(color);
                    }
                    graphics.draw(clickbox);
                    graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
                    graphics.fill(clickbox);
                }
                break;
        }
    }

    private void drawPathToClosestLamp(Graphics2D graphics, Set<WorldPoint> pathPoints) {
        if (!config.showPathToClosestLamp()) {
            return;
        }

        List<WorldPoint> path = plugin.getNavigationManager().getShortestPath();
        if (path == null || path.isEmpty()) {
            return;
        }

        TargetType targetType = plugin.getNavigationManager().getCurrentTargetType();
        boolean isUtilityTarget = targetType == TargetType.BANK || targetType == TargetType.WIRING_MACHINE;

        if (!isUtilityTarget && config.maxPathDistance() > 0 && path.size() > config.maxPathDistance()) {
            return;
        }

        if (targetType == TargetType.BANK && DKLampsConstants.BANK_TILES.contains(client.getLocalPlayer().getWorldLocation())) {
            return;
        }

        Set<Transport> activeTransports = new HashSet<>();

        for (WorldPoint point : path) {
            if (point.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
            if (localPoint == null) {
                continue;
            }

            Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
            if (poly != null) {
                OverlayUtil.renderPolygon(graphics, poly, config.pathColor());
            }

            List<Transport> transports = plugin.getPathfinder().getTransportsAt(point);
            for (Transport transport : transports) {
                if (pathPoints.contains(transport.getOrigin()) && pathPoints.contains(transport.getDestination())) {
                    activeTransports.add(transport);
                }
            }
        }

        highlightTransportsOnPath(graphics, activeTransports);
        highlightDoorsOnPath(graphics, pathPoints);

    }
    
    private void highlightTransportsOnPath(Graphics2D graphics, Set<Transport> activeTransports) {
        
        if (activeTransports.isEmpty()) {
            return;
        }
                
        for (GameObject stair : plugin.getStateManager().getStairs()) {
            if (stair.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }
            
            WorldPoint stairLocation = stair.getWorldLocation();
            if (isStairBetweenTransportPoints(stairLocation, activeTransports)) {
                renderTileObject(stair, config.pathColor(), graphics);
            }
        }
        
    }
    
    private void highlightDoorsOnPath(Graphics2D graphics, Set<WorldPoint> pathPoints) {
        for (WallObject door : plugin.getStateManager().getDoors()) {
            if (door.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }
            
            WorldPoint doorLocation = door.getWorldLocation();
            if (pathPoints.contains(doorLocation)) {
                renderTileObject(door, config.pathColor(), graphics);
            }
        }
    }
    
    private boolean isStairBetweenTransportPoints(WorldPoint objectLocation, Set<Transport> activeTransports) {
        for (Transport transport : activeTransports) {
            WorldPoint origin = transport.getOrigin();
            WorldPoint destination = transport.getDestination();
            
            if (objectLocation.getPlane() != origin.getPlane() && objectLocation.getPlane() != destination.getPlane()) {
                continue;
            }
            
            int objX = objectLocation.getX();
            int objY = objectLocation.getY();
            
            int minX = Math.min(origin.getX(), destination.getX());
            int maxX = Math.max(origin.getX(), destination.getX());
            int minY = Math.min(origin.getY(), destination.getY());
            int maxY = Math.max(origin.getY(), destination.getY());
            
            boolean xBetween = objX >= minX && objX <= maxX;
            boolean yBetween = objY >= minY && objY <= maxY;

            if (xBetween && yBetween) {
                return true;
            }

            if ((Math.abs(objX - origin.getX()) <= 1 && Math.abs(objY - origin.getY()) <= 1) &&
                (Math.abs(objX - destination.getX()) <= 1 && Math.abs(objY - destination.getY()) <= 1)) {
                return true;
            }
        }
        return false;
    }

    private void renderWireTimer(Graphics2D graphics) {
        if (plugin.getStateManager().getWireRespawnTime() == null || plugin.getStateManager().getWireMachine() == null) {
            return;
        }

        Instant now = Instant.now();
        if (now.isAfter(plugin.getStateManager().getWireRespawnTime())) {
            return;
        }

        Duration duration = Duration.between(now, plugin.getStateManager().getWireRespawnTime());
        double seconds = (double) Math.ceil(duration.toMillis() / 100.0) / 10.0;
        double progress = (double) duration.toMillis() / ((DKLampsConstants.WIRE_RESPAWN_TICKS) * 600.0);

        LocalPoint lp = plugin.getStateManager().getWireMachine().getLocalLocation();
        Point point = net.runelite.api.Perspective.getCanvasTextLocation(client, graphics, lp, " ", 0);

        if (point == null) {
            return;
        }

        if (config.timerType() == TimerType.PIE) {
            ProgressPieComponent pie = new ProgressPieComponent();
            pie.setPosition(point);
            pie.setBorderColor(config.wireMachineHighlightColor());
            pie.setDiameter(20);
            pie.setFill(config.wireMachineHighlightColor());
            pie.setProgress(progress);
            pie.render(graphics);
        } else if (config.timerType() == TimerType.TICKS) {
            long ticks = (long) Math.ceil(duration.toMillis() / 600.0);
            String text = String.valueOf(ticks);
            OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
        } else if (config.timerType() == TimerType.SECONDS) {
            String text = String.format("%.1f", seconds);
            OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
        }
    }
}
