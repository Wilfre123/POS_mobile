package com.example.bluepos.pos;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "debts")
public class Debt {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String customerName;
    public String productName;
    public int quantity;
    public double amount;
    public long timestamp;
    public String status; // "Unpaid", "Paid"
    public String note;
    public int userId;
    public int associatedSaleId; // Links to the Sale ID when paid

    public Debt(String customerName, String productName, int quantity, double amount, long timestamp, String status, String note, int userId) {
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
        this.note = note;
        this.userId = userId;
        this.associatedSaleId = -1; // Default -1 for unpaid debts
    }
}
