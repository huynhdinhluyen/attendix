package com.fpt.attendix.data.model;

public class User {
    private String uid;
    private String studentId;
    private String email;

    public User() {
    }

    public User(String uid, String studentId, String email) {
        this.uid = uid;
        this.studentId = studentId;
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
