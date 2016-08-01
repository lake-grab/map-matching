package com.grab.speed;

import com.graphhopper.matching.*;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.StopWatch;

import java.util.ArrayList;
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

        GrabGraphHopper hopper = new GrabGraphHopper();
        hopper.init(mockCmdArgs);
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        FlagEncoder firstEncoder = hopper.getEncodingManager().fetchEdgeEncoders().get(0);
        GraphHopperStorage graph = hopper.getGraphHopperStorage();

        int gpsAccuracy = mockCmdArgs.getInt("gpx_accuracy", 15);

        LocationIndexMatch locationIndex = new LocationIndexMatch(graph,
                (LocationIndexTree) hopper.getLocationIndex(), gpsAccuracy);

        MapMatching mapMatching = new MapMatching(graph, locationIndex, firstEncoder);
        mapMatching.setMeasurementErrorSigma(gpsAccuracy);

        StopWatch matchSW = new StopWatch();
        List<GPXEntry> list = new ArrayList<>();
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
            MatchResult mr = mapMatching.doWork(list);

            // return GraphHopper edges with all associated GPX entries
            List<EdgeMatch> matches = mr.getEdgeMatches();
            // now do something with the edges like storing the edgeIds or doing fetchWayGeometry etc
            for (int i=0;i<matches.size();i++){
                EdgeIteratorState edgeState = matches.get(i).getEdgeState();
                System.out.println("----------match edge index:" + edgeState.getEdge() + "----------");
                System.out.println(edgeState.getName());
                System.out.println(edgeState.getBaseNode());
                System.out.println(edgeState.getAdjNode());
                for (GPXExtension extension: matches.get(i).getGpxExtensions()) {
                    System.out.println("origin index:" + extension.getGpxListIndex());
                    System.out.println("query point:" + extension.getQueryResult().getQueryPoint());
                    System.out.println("snapped point:" + extension.getQueryResult().getSnappedPoint());
                    System.out.println("closest edge:" + extension.getQueryResult().getClosestEdge().getEdge());
                    System.out.println("closest internal way:" + hopper.getInternalWayId(extension.getQueryResult().getClosestEdge().getEdge()));
                    System.out.println("closest osm way:" + hopper.getOsmWayId(hopper.getInternalWayId(extension.getQueryResult().getClosestEdge().getEdge())));
                    System.out.println("closest node:" + extension.getQueryResult().getClosestNode());
                    System.out.println("closest osrm id:" + hopper.getOsmNodeId(-(extension.getQueryResult().getOsrmTrafficNode()+3)));
                    System.out.println("closest way nodes:" + hopper.getOsmNodeIdsByEdge(extension.getQueryResult().getClosestEdge().getEdge()));

//                    System.out.println(graph.getNodeAccess().getLat(extension.getQueryResult().getOsrmTrafficNode()) + "," + graph.getNodeAccess().getLon(extension.getQueryResult().getOsrmTrafficNode()));
                    System.out.println("");
                    System.out.println("");
                }
            }
//            for (GrabMapMatchResult grabMapMatchResult: mr.getGrabResults()) {
//                System.out.println("origin index:" + grabMapMatchResult.getOriginIndex());
//                System.out.println("edge:" + grabMapMatchResult.getEdgeId());
//                System.out.println("snap node:" + hopper.getOSMNodeId(-(grabMapMatchResult.getSnappedNodeId()+3)));
//                System.out.println("");
//            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            matchSW.stop();
        }
        System.out.println("match took: " + matchSW.getSeconds() + " s");
    }
}