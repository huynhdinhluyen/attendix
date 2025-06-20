package com.fpt.attendix.data.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class AttendanceRecord {
    private String userId;
    private String studentId;
    private String slot;
    private String ssid;
    private String bssid;
    private String imageUrl;

    @ServerTimestamp
    private Date timestamp;

    // Default constructor for Firestore
    public AttendanceRecord() {}

    public AttendanceRecord(String userId, String studentId, String slot, String ssid, String bssid, String imageUrl) {
        this.userId = userId;
        this.studentId = studentId;
        this.slot = slot;
        this.ssid = ssid;
        this.bssid = bssid;
        this.imageUrl = imageUrl;
        // timestamp will be set by @ServerTimestamp
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public String getBssid() { return bssid; }
    public void setBssid(String bssid) { this.bssid = bssid; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}