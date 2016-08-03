package com.grab.speed;

/**
 * Created by hubo on 16/7/26.
 */
public class GrabMapMatchResult {
    private int originIndex;
    private long time;
    private int snappedTowerNodeId;
    private int snappedEdgeId;
    private double originLat;
    private double originLon;
    private double snappedLat;
    private double snappedLon;

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
        return this.originLat+","+this.originLon;
    }

    public String getSnappedCoordinate() {
        return this.snappedLat+","+this.snappedLon;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setOriginLat(double originLat) {
        this.originLat = originLat;
    }

    public void setOriginLon(double originLon) {
        this.originLon = originLon;
    }

    public void setSnappedLat(double snappedLat) {
        this.snappedLat = snappedLat;
    }

    public void setSnappedLon(double snappedLon) {
        this.snappedLon = snappedLon;
    }

    public double getOriginLat() {
        return originLat;
    }

    public double getOriginLon() {
        return originLon;
    }

    public double getSnappedLat() {
        return snappedLat;
    }

    public double getSnappedLon() {
        return snappedLon;
    }
}
