package com.project.model;

import java.sql.Date;

public class Maintenance {
    private int maintenanceId;
    private int propertyId;
    private String maintenanceType;  // "Structural", "Timber Restoration", "Roof Repair", "Painting", etc.
    private String description;
    private Date startDate;
    private Date endDate;
    private String status;           // "Scheduled", "In Progress", "Completed", "Cancelled"
    private String assignedTo;       // Staff name
    private double estimatedCost;
    private boolean isDeleted;

    public Maintenance(int maintenanceId, int propertyId, String maintenanceType,
                       String description, Date startDate, Date endDate,
                       String status, String assignedTo, double estimatedCost, boolean isDeleted) {
        this.maintenanceId = maintenanceId;
        this.propertyId = propertyId;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.assignedTo = assignedTo;
        this.estimatedCost = estimatedCost;
        this.isDeleted = isDeleted;
    }

    public int getMaintenanceId() { return maintenanceId; }
    public void setMaintenanceId(int maintenanceId) { this.maintenanceId = maintenanceId; }

    public int getPropertyId() { return propertyId; }
    public void setPropertyId(int propertyId) { this.propertyId = propertyId; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}