package com.fpt.attendix.data.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class AttendanceRecord {
    private String userId;
    private String studentId;
    private String slot;
    private String wifiSsid;
    private String wifiBssid;
    private String imageUrl;
    @ServerTimestamp
    private Date timestamp;

    public AttendanceRecord() {
    }

    public AttendanceRecord(String userId, String studentId, String slot, String wifiSsid, String wifiBssid, String imageUrl) {
        this.userId = userId;
        this.studentId = studentId;
        this.slot = slot;
        this.wifiSsid = wifiSsid;
        this.wifiBssid = wifiBssid;
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getWifiSsid() {
        return wifiSsid;
    }

    public void setWifiSsid(String wifiSsid) {
        this.wifiSsid = wifiSsid;
    }

    public String getWifiBssid() {
        return wifiBssid;
    }

    public void setWifiBssid(String wifiBssid) {
        this.wifiBssid = wifiBssid;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}