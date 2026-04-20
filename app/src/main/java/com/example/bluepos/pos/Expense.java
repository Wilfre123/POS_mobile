package com.example.bluepos.pos;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public double amount;
    public long timestamp;
    public int userId;

    public Expense(String title, double amount, long timestamp, int userId) {
        this.title = title;
        this.amount = amount;
        this.timestamp = timestamp;
        this.userId = userId;
    }
}
