package com.example.bluepos.pos;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reservations")
public class Reservation {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String customerName;
    public String contactInfo;
    public String itemsSummary;
    public double totalAmount;
    public long timestamp;
    public String status; // Pending, Completed, Cancelled
    public int userId;
    public Integer associatedSaleId; // Link to the sale record if completed

    public Reservation(String customerName, String contactInfo, String itemsSummary, double totalAmount, long timestamp, String status, int userId) {
        this.customerName = customerName;
        this.contactInfo = contactInfo;
        this.itemsSummary = itemsSummary;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
        this.status = status;
        this.userId = userId;
    }
}
