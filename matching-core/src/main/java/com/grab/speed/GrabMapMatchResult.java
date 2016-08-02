package com.grab.speed;

/**
 * Created by hubo on 16/7/26.
 */
public class GrabMapMatchResult {
    private int originIndex;
    private long time;
    private int snappedTowerNodeId;
    private int snappedEdgeId;
    private String originCoordinate;
    private String snappedCoordinate;

    public int getSnappedTowerNodeId() {
        return snappedTowerNodeId;
    }

    public void setSnappedTowerNodeId(int snappedTowerNodeId) {
        this.snappedTowerNodeId = snappedTowerNodeId;
    }

    public int getOriginIndex() {
        return originIndex;
    }

    public void setOriginIndex(int originIndex) {
        this.originIndex = originIndex;
    }

    public int getSnappedEdgeId() {
        return snappedEdgeId;
    }

    public void setSnappedEdgeId(int snappedEdgeId) {
        this.snappedEdgeId = snappedEdgeId;
    }

    public String getOriginCoordinate() {
        return originCoordinate;
    }

    public void setOriginCoordinate(String originCoordinate) {
        this.originCoordinate = originCoordinate;
    }

    public String getSnappedCoordinate() {
        return snappedCoordinate;
    }

    public void setSnappedCoordinate(String snappedCoordinate) {
        this.snappedCoordinate = snappedCoordinate;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
