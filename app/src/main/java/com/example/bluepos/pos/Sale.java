package com.example.bluepos.pos;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sales")
public class Sale {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public double totalAmount;
    public double amountPaid;
    public double change;
    public long timestamp;
    public String itemsSummary; // A simple string representing items sold, e.g., "Item A x2, Item B x1"
    public int userId;
    public String userName;
    public int dataOwnerId;

    public Sale(double totalAmount, double amountPaid, double change, long timestamp, String itemsSummary, int userId, String userName, int dataOwnerId) {
        this.totalAmount = totalAmount;
        this.amountPaid = amountPaid;
        this.change = change;
        this.timestamp = timestamp;
        this.itemsSummary = itemsSummary;
        this.userId = userId;
        this.userName = userName;
        this.dataOwnerId = dataOwnerId;
    }
}
