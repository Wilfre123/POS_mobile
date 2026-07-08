package com.example.bluepos.pos;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String email;
    public String password;
    public String role; // "Admin" or "Staff"
    public Integer adminId; // If Staff, this is the ID of their Admin

    public User(String name, String email, String password, String role, Integer adminId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.adminId = adminId;
    }
}
