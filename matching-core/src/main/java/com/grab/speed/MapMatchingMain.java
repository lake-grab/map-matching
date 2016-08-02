package com.grab.speed;

import com.graphhopper.matching.LocationIndexMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.StopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by hubo on 16/7/25.
 */
public class MapMatchingMain {
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
        GPXEntry entry0 = new GPXEntry(1.35086,103.984877,new Date().getTime());
        GPXEntry entry1 = new GPXEntry(1.351064,103.985285,new Date().getTime()+10000);
        GPXEntry entry2 = new GPXEntry(1.351858,103.985596,new Date().getTime()+20000);
        GPXEntry entry3 = new GPXEntry(1.352351,103.985671,new Date().getTime()+30000);
        GPXEntry entry4 = new GPXEntry(1.352888,103.986003,new Date().getTime()+40000);
        GPXEntry entry5 = new GPXEntry(1.353038,103.986229,new Date().getTime()+50000);

        list.add(entry0);
        list.add(entry1);
        list.add(entry2);
        list.add(entry3);
        list.add(entry4);
        list.add(entry5);

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
            for (int i = 0; i < mr.getGrabResults().size() - 1; i++) {
                StringBuilder originCoordinates = new StringBuilder();
                originCoordinates.append("origin:").append("\n");
                StringBuilder snappedCoordinates = new StringBuilder();
                snappedCoordinates.append("snapped:").append("\n");


                int startWay = hopper.getInternalWayId(mr.getGrabResults().get(i).getSnappedEdgeId());
                int endWay = hopper.getInternalWayId(mr.getGrabResults().get(i + 1).getSnappedEdgeId());
                if (startWay == endWay) {

                    long startOsmNodeId = hopper.getOsmNodeId(-(mr.getGrabResults().get(i).getSnappedTowerNodeId() + 3));
                    long endOsmNodeId = hopper.getOsmNodeId(-(mr.getGrabResults().get(i+1).getSnappedTowerNodeId() + 3));
                    if (startOsmNodeId != endOsmNodeId) {
                        String nodes = hopper.getAdjacentNodeList(startWay, startOsmNodeId, endOsmNodeId);
                        if (!"".equals(nodes)) {

                            originCoordinates.append(mr.getGrabResults().get(i).getOriginCoordinate()).append("\n");
                            originCoordinates.append(mr.getGrabResults().get(i+1).getOriginCoordinate()).append("\n");
                            snappedCoordinates.append(mr.getGrabResults().get(i).getSnappedCoordinate()).append("\n");
                            snappedCoordinates.append(mr.getGrabResults().get(i+1).getSnappedCoordinate()).append("\n");
                            System.out.println();
                            System.out.print(nodes);
                            System.out.println();
                            System.out.println();
                            System.out.println(originCoordinates.toString());
                            System.out.println(snappedCoordinates.toString());
                            System.out.println("avg speed:" + (mr.getGrabResults().get(i+1).getTime() - mr.getGrabResults().get(i).getTime()));

                        }
                    }
                }
            }
        }
    }
}