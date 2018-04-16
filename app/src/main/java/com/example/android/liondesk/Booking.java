package com.example.android.liondesk;

// class modeled on the class FriendlyMessage from the Udacity app Friendly Chat

public class Booking {

    // instance variables
    private String id;
    private String date;
    private String username; // or email?
    private String hotDeskID; // or Hotdesk object?


    // empty constructor
    public Booking() {
    }

    // constructor with values provided
    public Booking(String date, String username, String hotDeskID) {
        this.date = date;
        this.username = username;
        this.hotDeskID = hotDeskID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHotDeskID() {
        return hotDeskID;
    }

    public void setHotDeskID(String hotDeskID) {
        this.hotDeskID = hotDeskID;
    }
}
