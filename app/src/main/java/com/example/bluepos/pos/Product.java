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
    public int quantity;
    public int quantityLimit;
    public String category;
    public int userId;

    public Product(String name, double cost, double price, int quantity, int quantityLimit, String category, int userId) {
        this.name = name;
        this.cost = cost;
        this.price = price;
        this.quantity = quantity;
        this.quantityLimit = quantityLimit;
        this.category = category;
        this.userId = userId;
    }
}
