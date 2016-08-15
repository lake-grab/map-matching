package com.grab.speed;

import com.graphhopper.coll.GHLongIntBTree;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.*;
import gnu.trove.list.TLongList;

import java.util.*;

/**
 * Created by hubo on 16/7/27.
 */
public class GrabGraphHopper extends GraphHopperOSM {
    private final static int SIZEOF_WAY_NODES = 50;
    private static final int LAT = 0 * 4, LON = 1 * 4, LONG_LOW_INT = 0 * 4, LONG_HIGH_INT = 1 * 4;
    private final long LONG_SIZE_IN_BYTES = 2 * 4;

    // mapping of internal node ID to OSM node ID
    public DataAccess pillarNodeMapping;
    private DataAccess towerNodeMapping;
    // mapping of internal edge ID to internal way ID
    private DataAccess edgeMapping;
    // mapping of internal way ID + index to internal node ID
    private DataAccess nodeIndexMapping;
    // mapping of internal way ID to OSM way ID
    private DataAccess wayMapping;
    // osm node id can't get the lat,lon...
    private LongIntMap osmNodeIdToInternalMap = new GHLongIntBTree(200);
    // mapping of pillar node osm id to lat,lon
    private DataAccess pillarNodeInfoMapping;

    private BitUtil bitUtil;
    final static DistanceCalc distanceCalc = new DistancePlaneProjection();

    @Override
    public boolean load(String graphHopperFolder) {
        boolean loaded = super.load(graphHopperFolder);
        Directory dir = getGraphHopperStorage().getDirectory();
        bitUtil = BitUtil.get(dir.getByteOrder());
        pillarNodeMapping = dir.find("pillar_node_mapping");
        towerNodeMapping = dir.find("tower_node_mapping");
        edgeMapping = dir.find("edge_mapping");
        nodeIndexMapping = dir.find("node_way_index_mapping");
        wayMapping = dir.find("way_mapping");
        pillarNodeInfoMapping = dir.find("pillar_node_info_mapping");

        if (loaded) {
            pillarNodeMapping.loadExisting();
            towerNodeMapping.loadExisting();
            edgeMapping.loadExisting();
            nodeIndexMapping.loadExisting();
            wayMapping.loadExisting();
            pillarNodeInfoMapping.loadExisting();

            for (int i=3; i<Integer.MAX_VALUE; i++) {
                long osmNodeId = this.getOsmNodeId(i);
                if (osmNodeId >= 0) {
                    osmNodeIdToInternalMap.put(osmNodeId,i);
                }else{
                    break;
                }
            }

            for (int i=-3; i > Integer.MIN_VALUE; i--) {
                long osmNodeId = this.getOsmNodeId(i);
                if (osmNodeId >= 0) {
                    osmNodeIdToInternalMap.put(osmNodeId,i);
                }else{
                    break;
                }
            }
        }

        return loaded;
    }

    @Override
    protected DataReader createReader(GraphHopperStorage ghStorage) {
        OSMReader reader = new OSMReader(ghStorage) {
            {
                pillarNodeMapping.create(1000);
                towerNodeMapping.create(1000);
                edgeMapping.create(1000);
                nodeIndexMapping.create(1000);
                wayMapping.create(1000);
                pillarNodeInfoMapping.create(1000);
            }


            // this method is only in >0.6 protected, before it was private
            @Override
            protected void storeOsmWayID(int edgeId, long osmWayId) {
                long pointer = (LONG_SIZE_IN_BYTES/2) * edgeId;
                edgeMapping.ensureCapacity(pointer + (LONG_SIZE_IN_BYTES/2));

                Integer internalWayId = getOsmWayIdToInternalIdMap().get(osmWayId);
                edgeMapping.setInt(pointer, internalWayId.intValue());

                super.storeOsmWayID(edgeId, osmWayId);
            }

            @Override
            protected Collection<EdgeIteratorState> addOSMWay( TLongList osmNodeIds, long wayFlags, long wayId ) {
                if (super.getOsmWayIdToInternalIdMap().get(wayId) != null ) {
                    for (int i = 0; i < SIZEOF_WAY_NODES; i++) {
                        long nodeId;
                        if (i < osmNodeIds.size()) {
                            nodeId = osmNodeIds.get(i);
                        } else {
                            nodeId = 0l;  // osm id equals 0 means no osm id
                        }

                        long pointer = 400L * super.getOsmWayIdToInternalIdMap().get(wayId);
                        nodeIndexMapping.ensureCapacity(pointer + 400);
                        nodeIndexMapping.setInt(pointer + LONG_SIZE_IN_BYTES * i + LONG_LOW_INT, bitUtil.getIntLow(nodeId));
                        nodeIndexMapping.setInt(pointer + LONG_SIZE_IN_BYTES * i + LONG_HIGH_INT, bitUtil.getIntHigh(nodeId));
                    }
                }
                return super.addOSMWay(osmNodeIds,wayFlags,wayId);
            }

            @Override
            protected void finishedReading() {

                // store node mapping
                LongIntMap map = getNodeMap();
                for (long osmNodeId : getInternalNodeToOsrmNodeIdMap().values()) {
                    int internalNodeId = map.get(osmNodeId);
                    if (internalNodeId != EMPTY) {
                        if (internalNodeId > 0) {
                            saveMapping(pillarNodeMapping,osmNodeId,internalNodeId);
                        }else {
                            saveMapping(towerNodeMapping,osmNodeId,Math.abs(internalNodeId));
                        }
                    }

                }

                // store way mapping
                for (long osmWayId: super.getOsmWayIdToInternalIdMap().keySet()) {
                    Integer internalWayId = super.getOsmWayIdToInternalIdMap().get(osmWayId);
                    if (internalWayId != null) {
                        saveMapping(wayMapping,osmWayId, internalWayId);
                    }
                }

                // store node lat/lon mapping
                for (long nodeOsmId: super.getNodeInfoMap().keySet()) {
                    ReaderNode node = super.getNodeInfoMap().get(nodeOsmId);
                    Integer internalNodeId = getNodeMap().get(nodeOsmId);
                    if (internalNodeId != null && internalNodeId > 0) {
                        long pointer = internalNodeId.intValue() * LONG_SIZE_IN_BYTES;
                        pillarNodeInfoMapping.ensureCapacity(pointer + LONG_SIZE_IN_BYTES);
                        pillarNodeInfoMapping.setInt(pointer + LAT, Helper.degreeToInt(node.getLat()));
                        pillarNodeInfoMapping.setInt(pointer + LON, Helper.degreeToInt(node.getLon()));
                    }
                }

                towerNodeMapping.flush();
                pillarNodeMapping.flush();
                edgeMapping.flush();
                nodeIndexMapping.flush();
                wayMapping.flush();
                pillarNodeInfoMapping.flush();

                super.getNodeInfoMap().clear();
                super.finishedReading();
            }
        };

        return initDataReader(reader);
    }

    private void saveMapping(DataAccess mapping,long osmId, int internalId) {
        long pointer = LONG_SIZE_IN_BYTES * internalId;
        mapping.ensureCapacity(pointer + LONG_SIZE_IN_BYTES);
        mapping.setInt(pointer + LONG_LOW_INT, bitUtil.getIntLow(osmId));
        mapping.setInt(pointer + LONG_HIGH_INT, bitUtil.getIntHigh(osmId));
    }

    public long getOsmNodeId(int internalNodeId) {
        try {
            if (internalNodeId > 0) {
                long pointer = LONG_SIZE_IN_BYTES * internalNodeId;
                return bitUtil.combineIntsToLong(pillarNodeMapping.getInt(pointer + LONG_LOW_INT), pillarNodeMapping.getInt(pointer + LONG_HIGH_INT));
            }else {
                long pointer = LONG_SIZE_IN_BYTES * Math.abs(internalNodeId);
                return bitUtil.combineIntsToLong(towerNodeMapping.getInt(pointer + LONG_LOW_INT), towerNodeMapping.getInt(pointer + LONG_HIGH_INT));
            }
        }catch(Exception e) {
            return -1;
        }
    }

    public int getInternalWayId(int edgeId) {
        long pointer = (LONG_SIZE_IN_BYTES/2) * edgeId;
        return edgeMapping.getInt(pointer);
    }

    public String getOsmNodeIdsByEdge(int edgeId) {
        StringBuilder sb = new StringBuilder();
        sb.append("nodes:");
        long pointer = 400L * edgeMapping.getInt((LONG_SIZE_IN_BYTES/2) * edgeId);
        for (long i = 0; i < SIZEOF_WAY_NODES ; i++){
            long nodeId = bitUtil.combineIntsToLong(nodeIndexMapping.getInt(pointer + LONG_SIZE_IN_BYTES * i + LONG_LOW_INT), nodeIndexMapping.getInt(pointer + LONG_SIZE_IN_BYTES * i + LONG_HIGH_INT));
            if (nodeId != 0l) {
                sb.append(nodeId + ",");
            }
        }
        return sb.toString();
    }

    public long getOsmWayId(int internalWayId) {
            long pointer = LONG_SIZE_IN_BYTES * internalWayId;
            return bitUtil.combineIntsToLong(wayMapping.getInt(pointer + LONG_LOW_INT) , wayMapping.getInt(pointer + LONG_HIGH_INT));
    }

    public double getPillarNodeLat(int internalNodeId) {
        int intVal = pillarNodeInfoMapping.getInt(internalNodeId * LONG_SIZE_IN_BYTES + LAT);
        return Helper.intToDegree(intVal);
    }

    public double getPillarNodeLon(int internalNodeId) {
        int intVal = pillarNodeInfoMapping.getInt(internalNodeId * LONG_SIZE_IN_BYTES + LON);
        return Helper.intToDegree(intVal);
    }

    public List<Long> getAdjacentNodeList(int internalWayId, double startLat, double startLon, double endLat, double endLon) {
        List<Long> nodes = new ArrayList<>();
        long[] wayNodes = new long[SIZEOF_WAY_NODES];
        double closestStartDistance = Double.MAX_VALUE;
        double closestEndDistance = Double.MAX_VALUE;
        int closestStartIndex = -1;
        int closestEndIndex = -1;
        double toCalculateLat;
        double toCalculateLon;

        long pointer = 400L * internalWayId;

        for (int i = 0; i < SIZEOF_WAY_NODES ; i++){
            long nodeId = bitUtil.combineIntsToLong(nodeIndexMapping.getInt(pointer + LONG_SIZE_IN_BYTES * i + LONG_LOW_INT), nodeIndexMapping.getInt(pointer + LONG_SIZE_IN_BYTES * i + LONG_HIGH_INT));
            if (nodeId == 0l) {
                break;
            }
            wayNodes[i] = nodeId;
            Integer internalNodeId = osmNodeIdToInternalMap.get(nodeId);
            if (internalNodeId == null) {
                continue;
            }
            if (internalNodeId > 0) {
                toCalculateLat = this.getPillarNodeLat(internalNodeId);
                toCalculateLon = this.getPillarNodeLon(internalNodeId);
            }else {
                toCalculateLat = super.getGraphHopperStorage().getNodeAccess().getLat(-internalNodeId-3);
                toCalculateLon = super.getGraphHopperStorage().getNodeAccess().getLon(-internalNodeId - 3);
            }
            double startDistance = distanceCalc.calcDist(startLat,startLon,toCalculateLat, toCalculateLon);
            double endDistance = distanceCalc.calcDist(endLat,endLon,toCalculateLat, toCalculateLon);

            if (startDistance < closestStartDistance) {
                closestStartDistance = startDistance;
                closestStartIndex = i;
            }
            if (endDistance < closestEndDistance) {
                closestEndDistance = endDistance;
                closestEndIndex = i;
            }
        }
        if (closestStartIndex != closestEndIndex) {
            if (closestStartIndex < closestEndIndex) {
                for (int wayIndex=closestStartIndex; wayIndex <= closestEndIndex; wayIndex++) {
                    nodes.add(wayNodes[wayIndex]);
                }
            }else {
                for (int wayIndex=closestStartIndex; wayIndex >= closestEndIndex; wayIndex--) {
                    nodes.add(wayNodes[wayIndex]);
                }
            }
        }else {
            if (closestStartIndex > 0) {
                nodes.add(wayNodes[closestStartIndex--]);
                nodes.add(wayNodes[closestStartIndex]);
            } else {
                nodes.add(wayNodes[closestStartIndex]);
                nodes.add(wayNodes[closestStartIndex++]);
            }

        }
        return nodes;
    }

    public Long findCrossNode(int startInternalWayId, int endInternalWayId) {
        Set<Long> startWayNodeSet = new HashSet<>(SIZEOF_WAY_NODES);

        long startPointer = 400L * startInternalWayId;
        long endPointer = 400L * endInternalWayId;

        for (int i=0; i < SIZEOF_WAY_NODES; i++) {
            long startNodeId = bitUtil.combineIntsToLong(nodeIndexMapping.getInt(startPointer + LONG_SIZE_IN_BYTES * i + LONG_LOW_INT), nodeIndexMapping.getInt(startPointer + LONG_SIZE_IN_BYTES * i + LONG_HIGH_INT));
            if (startNodeId != 0) {
                startWayNodeSet.add(startNodeId);
            }else {
                break;
            }
        }

        for (int i=0; i < SIZEOF_WAY_NODES; i++) {
            long endNodeId = bitUtil.combineIntsToLong(nodeIndexMapping.getInt(endPointer + LONG_SIZE_IN_BYTES * i + LONG_LOW_INT), nodeIndexMapping.getInt(endPointer + LONG_SIZE_IN_BYTES * i + LONG_HIGH_INT));
            if (endNodeId != 0) {
                if (startWayNodeSet.contains(endNodeId)) {
                    return endNodeId;
                }
            }else {
                break;
            }
        }
        return null;
    }

    public LongIntMap getOsmNodeIdToInternalMap() {
        return osmNodeIdToInternalMap;
    }

}
