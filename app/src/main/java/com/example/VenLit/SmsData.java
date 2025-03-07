package com.example.VenLit;

public class SmsData {
    private String sender;
    private long timestamp;
    private String message;

    public SmsData(String sender, long timestamp, String message) {
        this.sender = sender;
        this.timestamp = timestamp;
        this.message = message;
    }
}