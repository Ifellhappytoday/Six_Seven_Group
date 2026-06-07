package com.project.model;
import java.sql.Date;

public class Booking {
    private int bookingId, propertyId, guestId;
    private Date startDate, endDate;
    private double totalPrice;
    private boolean isDeleted;

    public Booking(int bookingId, int propertyId, int guestId, Date startDate, Date endDate, double totalPrice, boolean isDeleted) {
        this.bookingId = bookingId;
        this.propertyId = propertyId;
        this.guestId = guestId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.isDeleted = isDeleted;
    }
    
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    
    public int getPropertyId() { return propertyId; }
    public void setPropertyId(int propertyId) { this.propertyId = propertyId; }
    
    public int getGuestId() { return guestId; }
    public void setGuestId(int guestId) { this.guestId = guestId; }
    
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}