package com.example.bluepos.pos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT * FROM products WHERE userId = :userId")
    List<Product> getAll(int userId);

    @Query("SELECT * FROM products WHERE userId = :userId AND name LIKE '%' || :query || '%'")
    List<Product> searchProducts(int userId, String query);

    @Insert
    void insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);
}
