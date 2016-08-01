package com.grab.speed;

/**
 * Created by hubo on 16/7/26.
 */
public class GrabMapMatchResult {
    private int snappedNodeId;
    private int originIndex;
    private int edgeId;

    public int getSnappedNodeId() {
        return snappedNodeId;
    }

    public void setSnappedNodeId(int snappedNodeId) {
        this.snappedNodeId = snappedNodeId;
    }

    public int getOriginIndex() {
        return originIndex;
    }

    public void setOriginIndex(int originIndex) {
        this.originIndex = originIndex;
    }

    public int getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(int edgeId) {
        this.edgeId = edgeId;
    }
}
