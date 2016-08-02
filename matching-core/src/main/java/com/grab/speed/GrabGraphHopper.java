package com.grab.speed;

import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.EdgeIteratorState;
import gnu.trove.list.TLongList;

import java.util.Collection;

/**
 * Created by hubo on 16/7/27.
 */
public class GrabGraphHopper extends GraphHopperOSM {
    // mapping of internal edge ID to OSM way ID
    private DataAccess pillarNodeMapping;
    private DataAccess towerNodeMapping;
    // mapping of internal edge ID to internal way ID
    private DataAccess edgeMapping;
    // mapping of internal way ID + index to OSM node ID
    private DataAccess nodeIndexMapping;
    //debug mapping of internal way ID to OSM way ID
    private DataAccess wayMapping;
    private BitUtil bitUtil;

    private final static int SIZEOF_WAY_NODES = 50;

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

        if (loaded) {
            pillarNodeMapping.loadExisting();
            towerNodeMapping.loadExisting();
            edgeMapping.loadExisting();
            nodeIndexMapping.loadExisting();
            wayMapping.loadExisting();
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

            }

            // this method is only in >0.6 protected, before it was private
            @Override
            protected void storeOsmWayID(int edgeId, long osmWayId) {
                long pointer = 4L * edgeId;
                edgeMapping.ensureCapacity(pointer + 4L);

                Integer internalWayId = getOsmWayIdToInternalIdMap().get(osmWayId);
                edgeMapping.setInt(pointer, internalWayId.intValue());

                super.storeOsmWayID(edgeId, osmWayId);
            }

            @Override
            protected Collection<EdgeIteratorState> addOSMWay( TLongList osmNodeIds, long wayFlags, long wayId )
            {
                for (int i = 0; i < SIZEOF_WAY_NODES; i++){
                    long nodeId;
                    if (i < osmNodeIds.size()) {
                        nodeId = osmNodeIds.get(i);
                    }else {
                        nodeId = 0l;  // osm id equals 0 means no osm id
                     }
                    if ( super.getOsmWayIdToInternalIdMap().get(wayId) != null ) {
                        long pointer = 400L * super.getOsmWayIdToInternalIdMap().get(wayId);
                        nodeIndexMapping.ensureCapacity(pointer + 400);
                        nodeIndexMapping.setInt(pointer + 8*i, bitUtil.getIntLow(nodeId));
                        nodeIndexMapping.setInt(pointer + 8*i + 4, bitUtil.getIntHigh(nodeId));
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

                towerNodeMapping.flush();
                pillarNodeMapping.flush();
                edgeMapping.flush();
                nodeIndexMapping.flush();
                wayMapping.flush();

                super.finishedReading();
            }
        };

        return initDataReader(reader);
    }

    private void saveMapping(DataAccess mapping,long osmId, int internalId) {
        long pointer = 8L * internalId;
        mapping.ensureCapacity(pointer + 8L);
        mapping.setInt(pointer, bitUtil.getIntLow(osmId));
        mapping.setInt(pointer + 4, bitUtil.getIntHigh(osmId));
    }

    public long getOsmNodeId(int internalNodeId) {
        if (internalNodeId > 0) {
            long pointer = 8L * internalNodeId;
            return bitUtil.combineIntsToLong(pillarNodeMapping.getInt(pointer), pillarNodeMapping.getInt(pointer + 4L));
        }else {
            long pointer = 8L * Math.abs(internalNodeId);
            return bitUtil.combineIntsToLong(towerNodeMapping.getInt(pointer), towerNodeMapping.getInt(pointer + 4L));
        }
    }

    public int getInternalWayId(int edgeId) {
        long pointer = 4L * edgeId;
        return edgeMapping.getInt(pointer);
    }

    public String getOsmNodeIdsByEdge(int edgeId) {
        StringBuilder sb = new StringBuilder();
        sb.append("nodes:");
        long pointer = 400L * edgeMapping.getInt(4L * edgeId);
        for (long i = 0; i < SIZEOF_WAY_NODES ; i++){
            long nodeId = bitUtil.combineIntsToLong(nodeIndexMapping.getInt(pointer + 8*i), nodeIndexMapping.getInt(pointer + 8*i + 4));
            if (nodeId != 0l) {
                sb.append(nodeId + ",");
            }
        }
        return sb.toString();
    }

    public long getOsmWayId(int internalWayId) {
            long pointer = 8L * internalWayId;
            return bitUtil.combineIntsToLong(wayMapping.getInt(pointer), wayMapping.getInt(pointer + 4L));
    }

    public String getAdjacentNodeList(int internalWayId, long startOsmId, long endOsmId) {
        StringBuilder sb = new StringBuilder();
        long pointer = 400L * internalWayId;
        boolean needAddFlag = false;
        for (long i = 0; i < SIZEOF_WAY_NODES ; i++){
            long nodeId = bitUtil.combineIntsToLong(nodeIndexMapping.getInt(pointer + 8*i), nodeIndexMapping.getInt(pointer + 8*i + 4));
            if (nodeId == 0l) {
                break;
            }
            if (startOsmId == nodeId || endOsmId == nodeId) {
                if (!("").equals(sb.toString())) {
                    sb.append(nodeId);
                    break;
                }
                needAddFlag = true;
            }
            if (needAddFlag) {
                sb.append(nodeId).append(",");
            }
        }
        return sb.toString();
    }

}
