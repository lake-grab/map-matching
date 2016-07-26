package com.grab.speed;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.LocationIndexMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.reader.osm.GraphHopperOSM;
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

        GraphHopper hopper = new GraphHopperOSM().init(mockCmdArgs);
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
        matchSW.start();
        List<GPXEntry> list = new ArrayList<>();
        GPXEntry entry0 = new GPXEntry(1.35086,103.984877,new Date().getTime());
        GPXEntry entry1 = new GPXEntry(1.351064,103.985285,new Date().getTime()+10000);
        GPXEntry entry2 = new GPXEntry(1.351858,103.985596,new Date().getTime()+20000);
        GPXEntry entry3 = new GPXEntry(1.352351,103.985671,new Date().getTime()+30000);
        GPXEntry entry4 = new GPXEntry(1.352888,103.986003,new Date().getTime()+40000);
        list.add(entry0);
        list.add(entry1);
        list.add(entry2);
        list.add(entry3);
        list.add(entry4);
        MatchResult mr = mapMatching.doWork(list);
        matchSW.stop();

        System.out.println("\tmatches:\t" + mr.getEdgeMatches().size() + "match took: " + matchSW.getSeconds() + " s");
        // return GraphHopper edges with all associated GPX entries
        List<EdgeMatch> matches = mr.getEdgeMatches();
        // now do something with the edges like storing the edgeIds or doing fetchWayGeometry etc
        for (int i=0;i<matches.size();i++){
            EdgeIteratorState edgeState = matches.get(i).getEdgeState();
            System.out.println(i);
            System.out.println(graph.getNodeAccess().getLatitude(edgeState.getBaseNode()) + "," + graph.getNodeAccess().getLongitude(edgeState.getBaseNode()));
            System.out.println(graph.getNodeAccess().getLat(edgeState.getBaseNode()) + "," + graph.getNodeAccess().getLon(edgeState.getBaseNode()));
            System.out.println(edgeState.getName());
            System.out.println(edgeState.getBaseNode());
            System.out.println(edgeState.getAdjNode());
        }
    }
}