package com.example.aravindh.demo;

/**
 * Created by aravindh on 7/3/18.
 */

public class AccessPoint
{
    private String mac;
    private float xCoordinate;
    private float yCoordinate;
    private float rssi;
    private int cnt;
    private float distance;

    public AccessPoint(String mac, float xCoordinate, float yCoordinate) {
        this.mac = mac;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public float getxCoordinate() {
        return xCoordinate;
    }

    public void setxCoordinate(float xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public float getyCoordinate() {
        return yCoordinate;
    }

    public void setyCoordinate(float yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public float getRssi() {
        return rssi;
    }

    public void setRssi(float rssi) {
        this.rssi = rssi;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }



}

