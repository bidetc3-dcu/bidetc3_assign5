package com.example.android.liondesk;

/**
 * Created by Caroline Bidet on 22/03/2018.
 *
 * Adapted from Album.java class from
 * https://www.androidhive.info/2016/05/android-working-with-card-view-and-recycler-view/
 */

public class HotDesk {
    private String m_ID;
    private int m_floor;
    private String m_status;
    private int m_thumbnail;

    // Constructor
    public HotDesk () {
    }

    public HotDesk(String id, int floor, String status, int thumbnail) {
        this.m_ID = id;
        this.m_floor = floor;
        this.m_status = status;
        this.m_thumbnail = thumbnail;
    }

    public String getID() {
        return m_ID;
    }

    public void setID(String id) {
        this.m_ID = id;
    }

    public int getFloorNumber() {
        return m_floor;
    }

    public void setFloorNumber(int floor) {
        this.m_floor = floor;
    }

    public String getStatus() {
        return m_status;
    }

    public void setStatus(String status) {
        this.m_status = status;
    }

    public int getThumbnail() {
        return m_thumbnail;
    }

    public void setThumbnail(int thumbnail) {
        this.m_thumbnail = thumbnail;
    }
}