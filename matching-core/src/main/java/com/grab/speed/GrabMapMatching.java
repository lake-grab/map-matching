package com.grab.speed;

import com.graphhopper.matching.LocationIndexMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by hubo on 16/7/25.
 */
public class GrabMapMatching {

    final static DistanceCalc distanceCalc = new DistancePlaneProjection();

    public static void main(String[] args){
        // import OpenStreetMap data
        CmdArgs mockCmdArgs = new CmdArgs();
        mockCmdArgs.put("graph.flag_encoders","car");
        mockCmdArgs.put("datareader.file","./map/map.osm.pbf");
        // standard should be to remove disconnected islands
        mockCmdArgs.put("prepare.min_network_size", 200);
        mockCmdArgs.put("prepare.min_one_way_network_size", 200);

        final GrabGraphHopper hopper = new GrabGraphHopper();
        hopper.init(mockCmdArgs);
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        FlagEncoder firstEncoder = hopper.getEncodingManager().fetchEdgeEncoders().get(0);
        GraphHopperStorage graph = hopper.getGraphHopperStorage();

        int gpsAccuracy = mockCmdArgs.getInt("gpx_accuracy", 10);

        LocationIndexMatch locationIndex = new LocationIndexMatch(graph,
                (LocationIndexTree) hopper.getLocationIndex(), gpsAccuracy);

        final MapMatching mapMatching = new MapMatching(graph, locationIndex, firstEncoder);
        mapMatching.setMeasurementErrorSigma(gpsAccuracy);

        StopWatch matchSW = new StopWatch();
        final List<GPXEntry> list = new ArrayList<>();
//        GPXEntry entry0 = new GPXEntry(1.2866,103.817163,1459949745);
//        GPXEntry entry1 = new GPXEntry(1.287053,103.818278,1459949777);

//
//        GPXEntry entry2 = new GPXEntry(1.295028,103.854053,1464766658);


                String[] input = "1.335135,103.745076,1.335048,103.745133,1464767897000,1464767913000".split(",");
        GPXEntry entry3 = new GPXEntry(Double.parseDouble(input[0]),Double.parseDouble(input[1]),Long.parseLong(input[4]));
        GPXEntry entry4 = new GPXEntry(Double.parseDouble(input[2]),Double.parseDouble(input[3]),Long.parseLong(input[5]));
//        GPXEntry entry3 = new GPXEntry(1.342298,103.982109,1462032004);
//        GPXEntry entry4 = new GPXEntry(1.282792,103.859626,1462032005);
//        GPXEntry entry5 = new GPXEntry(1.328499,103.840244,1462032006);
//        GPXEntry entry6 = new GPXEntry(1.389726,103.745281,1462032009);
//        GPXEntry entry7 = new GPXEntry(1.300272,103.834707,1462032010);
//        GPXEntry entry8 = new GPXEntry(1.381037,103.754359,1462032010);
//        GPXEntry entry9 = new GPXEntry(1.290513,103.84712, 1462032011);


//        list.add(entry0);
//        list.add(entry1);
//        list.add(entry2);
        list.add(entry3);
        list.add(entry4);
//        list.add(entry3);
//        list.add(entry4);
//        list.add(entry5);
//        list.add(entry6);
//        list.add(entry7);
//        list.add(entry8);
//        list.add(entry9);

        matchSW.start();
        doMapMatchingDebug(hopper, mapMatching, list);
        matchSW.stop();

        System.out.println("match took: " + matchSW.getNanos()/1000l + "ms");
    }

    private static void doMapMatchingDebug(GrabGraphHopper hopper, MapMatching mapMatching, List<GPXEntry> list) {
        MatchResult mr = mapMatching.doGrabWork(list);
        if (!mr.getGrabResults().isEmpty() && mr.getGrabResults().size() >=2) {
            String nodeString = "";
            StringBuilder originCoordinates = new StringBuilder();
            originCoordinates.append("origin:").append("\n");
            StringBuilder snappedCoordinates = new StringBuilder();
            snappedCoordinates.append("snapped:").append("\n");
            List<GrabMapMatchResult> matchResults = mr.getGrabResults();

            for (int i = 0; i < mr.getGrabResults().size() - 1; i++) {
                originCoordinates.append(mr.getGrabResults().get(i).getOriginCoordinate()).append("\n");
                snappedCoordinates.append(mr.getGrabResults().get(i).getSnappedCoordinate()).append("\n");


                int startWay = hopper.getInternalWayId(matchResults.get(i).getSnappedEdgeId());
                int endWay = hopper.getInternalWayId(matchResults.get(i+1).getSnappedEdgeId());

                if (startWay == endWay) {
                    nodeString = getNodeSeriesString(hopper, startWay, matchResults.get(i).getSnappedLat(),matchResults.get(i).getSnappedLon(),matchResults.get(i+1).getSnappedLat(),matchResults.get(i+1).getSnappedLon());
                } else {
                    Long crossoverNodeID = hopper.findCrossNode(startWay, endWay);
                    if (crossoverNodeID != null && crossoverNodeID != 0) {
                        Double toCalculateLat,toCalculateLon;
                        Integer internalNodeId = hopper.getOsmNodeIdToInternalMap().get(crossoverNodeID);
                        if (internalNodeId == null) {
                            continue;
                        }
                        if (internalNodeId > 0) {
                            toCalculateLat = hopper.getPillarNodeLat(internalNodeId);
                            toCalculateLon = hopper.getPillarNodeLon(internalNodeId);
                        }else {
                            toCalculateLat = hopper.getGraphHopperStorage().getNodeAccess().getLat(-internalNodeId-3);
                            toCalculateLon = hopper.getGraphHopperStorage().getNodeAccess().getLon(-internalNodeId - 3);
                        }

                        List<Long> startNodes = hopper.getAdjacentNodeList(startWay, matchResults.get(i).getSnappedLat(),matchResults.get(i).getSnappedLon(),toCalculateLat,toCalculateLon);
                        List<Long> endNodes = hopper.getAdjacentNodeList(endWay, toCalculateLat,toCalculateLon, matchResults.get(i+1).getSnappedLat(),matchResults.get(i+1).getSnappedLon());
                        nodeString = getCrossNodeSeriesString(startNodes, endNodes);
                    }
                }
            }
            originCoordinates.append(mr.getGrabResults().get(mr.getGrabResults().size()-1).getOriginCoordinate()).append("\n");
            snappedCoordinates.append(mr.getGrabResults().get(mr.getGrabResults().size()-1).getSnappedCoordinate()).append("\n");
            System.out.println();
            System.out.println(originCoordinates.toString());
            System.out.println(snappedCoordinates.toString());
            System.out.println();
            System.out.println(nodeString);
        }
    }

    public static List<GrabMapMatchResult> doGrabMapMatching(MapMatching mapMatching, List<GPXEntry> list) {
        MatchResult mr = mapMatching.doGrabWork(list);
        return mr.getGrabResults();
    }

    private static String getNodeSeriesString(GrabGraphHopper hopper,int internalWay, double startLat, double startLon, double endLat, double endLon) {
        StringBuilder nodesBuilder = new StringBuilder();
        List<Long> nodes = hopper.getAdjacentNodeList(internalWay, startLat, startLon, endLat, endLon);

        if (!nodes.isEmpty()) {
            for (long nodeId : nodes) {
                nodesBuilder.append(nodeId).append(",");
            }
        }

        return nodesBuilder.toString().length() > 0 ? nodesBuilder.toString().substring(0, nodesBuilder.length() - 1) : "empty";
    }

    private static String getCrossNodeSeriesString(List<Long> start, List<Long> end) {
        StringBuilder nodesBuilder = new StringBuilder();
        Set<Long> startSet = new HashSet<>(start.size());
        if (!start.isEmpty()) {
            for (long nodeId : start) {
                nodesBuilder.append(nodeId).append(",");
                startSet.add(nodeId);
            }
        }
        if (!end.isEmpty()) {
            for (long nodeId : end) {
                if (!startSet.contains(nodeId)) {
                    nodesBuilder.append(nodeId).append(",");
                }
            }
        }
        return nodesBuilder.toString().length() > 0 ? nodesBuilder.toString().substring(0, nodesBuilder.length() - 1) : "empty";
    }
}