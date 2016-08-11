package com.grab.speed;

import com.graphhopper.matching.LocationIndexMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        int gpsAccuracy = mockCmdArgs.getInt("gpx_accuracy", 15);

        LocationIndexMatch locationIndex = new LocationIndexMatch(graph,
                (LocationIndexTree) hopper.getLocationIndex(), gpsAccuracy);

        final MapMatching mapMatching = new MapMatching(graph, locationIndex, firstEncoder);
        mapMatching.setMeasurementErrorSigma(gpsAccuracy);

        StopWatch matchSW = new StopWatch();
        final List<GPXEntry> list = new ArrayList<>();
        GPXEntry entry0 = new GPXEntry(1.314472,103.879013,1462032002);
        GPXEntry entry1 = new GPXEntry(1.357697,103.958711,1462032003);
        GPXEntry entry2 = new GPXEntry(1.275028,103.814815,1462032003);
//        GPXEntry entry3 = new GPXEntry(1.342298,103.982109,1462032004);
//        GPXEntry entry4 = new GPXEntry(1.282792,103.859626,1462032005);
//        GPXEntry entry5 = new GPXEntry(1.328499,103.840244,1462032006);
//        GPXEntry entry6 = new GPXEntry(1.389726,103.745281,1462032009);
//        GPXEntry entry7 = new GPXEntry(1.300272,103.834707,1462032010);
//        GPXEntry entry8 = new GPXEntry(1.381037,103.754359,1462032010);
//        GPXEntry entry9 = new GPXEntry(1.290513,103.84712, 1462032011);

        list.add(entry0);
        list.add(entry1);
        list.add(entry2);
//        list.add(entry3);
//        list.add(entry4);
//        list.add(entry5);
//        list.add(entry6);
//        list.add(entry7);
//        list.add(entry8);
//        list.add(entry9);

        matchSW.start();
        try {
        for (int i=0; i<1; i++) {
            final List<GPXEntry> mockList = new ArrayList<>(list);
            Collections.copy(mockList, list);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    doMapMatchingDebug(hopper, mapMatching, list);

                }
            }).start();
        }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            matchSW.stop();
        }
        System.out.println("match took: " + matchSW.getNanos()/1000l + "ms");
    }

    private static void doMapMatchingDebug(GrabGraphHopper hopper, MapMatching mapMatching, List<GPXEntry> list) {
        MatchResult mr = mapMatching.doWork(list);
        if (!mr.getGrabResults().isEmpty() && mr.getGrabResults().size() >=2) {
            StringBuilder originCoordinates = new StringBuilder();
            originCoordinates.append("origin:").append("\n");
            StringBuilder snappedCoordinates = new StringBuilder();
            snappedCoordinates.append("snapped:").append("\n");

            for (int i = 0; i < mr.getGrabResults().size() - 1; i++) {
                originCoordinates.append(mr.getGrabResults().get(i).getOriginCoordinate()).append("\n");
                snappedCoordinates.append(mr.getGrabResults().get(i).getSnappedCoordinate()).append("\n");

                int startWay = hopper.getInternalWayId(mr.getGrabResults().get(i).getSnappedEdgeId());
                int endWay = hopper.getInternalWayId(mr.getGrabResults().get(i + 1).getSnappedEdgeId());
                if (startWay == endWay) {
                    List<Long> nodes = hopper.getAdjacentNodeList(startWay, mr.getGrabResults().get(i).getSnappedLat(),mr.getGrabResults().get(i).getSnappedLon(),mr.getGrabResults().get(i+1).getSnappedLat(),mr.getGrabResults().get(i+1).getSnappedLon());
                    if (!nodes.isEmpty()) {
                        System.out.println();
                        System.out.print(nodes);
                        System.out.println();
                        System.out.println();
                        System.out.println("distance:" + distanceCalc.calcDist(mr.getGrabResults().get(i).getSnappedLat(),mr.getGrabResults().get(i).getSnappedLon(),mr.getGrabResults().get(i+1).getSnappedLat(),mr.getGrabResults().get(i+1).getSnappedLon()));
                    }
                }
            }
            originCoordinates.append(mr.getGrabResults().get(mr.getGrabResults().size()-1).getOriginCoordinate()).append("\n");
            snappedCoordinates.append(mr.getGrabResults().get(mr.getGrabResults().size()-1).getSnappedCoordinate()).append("\n");
            System.out.println();
            System.out.println(originCoordinates.toString());
            System.out.println(snappedCoordinates.toString());
        }
    }

    public static List<GrabMapMatchResult> doMapMatching(MapMatching mapMatching, List<GPXEntry> list) {
        MatchResult mr = mapMatching.doWork(list);
        return mr.getGrabResults();
    }
}