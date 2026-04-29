package com.example.bluepos.pos;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public double cost;
    public double price;
    public int stock;
    public int minStock;
    public String category;
    public int userId;
    public boolean hasExpiration;
    public Long expirationDate;

    public Product(String name, double cost, double price, int stock, int minStock, String category, int userId, boolean hasExpiration, Long expirationDate) {
        this.name = name;
        this.cost = cost;
        this.price = price;
        this.stock = stock;
        this.minStock = minStock;
        this.category = category;
        this.userId = userId;
        this.hasExpiration = hasExpiration;
        this.expirationDate = expirationDate;
    }
}
