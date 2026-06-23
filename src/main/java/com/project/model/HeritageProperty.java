package com.project.model;

public class HeritageProperty {
    private int propertyId;
    private String propertyName;
    private double dailyRate;
    private String currentStatus;
    private boolean isDeleted;
    private String imagePath; // filename stored in resources/images/, e.g. "homestay1.jpg"

    public HeritageProperty(int propertyId, String propertyName, double dailyRate, String currentStatus, boolean isDeleted) {
        this.propertyId = propertyId;
        this.propertyName = propertyName;
        this.dailyRate = dailyRate;
        this.currentStatus = currentStatus;
        this.isDeleted = isDeleted;
        this.imagePath = null;
    }

    public HeritageProperty(int propertyId, String propertyName, double dailyRate, String currentStatus, boolean isDeleted, String imagePath) {
        this.propertyId = propertyId;
        this.propertyName = propertyName;
        this.dailyRate = dailyRate;
        this.currentStatus = currentStatus;
        this.isDeleted = isDeleted;
        this.imagePath = imagePath;
    }

    public int getPropertyId() { return propertyId; }
    public String getPropertyName() { return propertyName; }
    public double getDailyRate() { return dailyRate; }
    public String getCurrentStatus() { return currentStatus; }
    public boolean isDeleted() { return isDeleted; }
    public String getImagePath() { return imagePath; }
}