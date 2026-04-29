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

    @Query("SELECT * FROM products WHERE id = :productId")
    Product getById(int productId);

    @Query("SELECT DISTINCT category FROM products WHERE userId = :userId")
    List<String> getUniqueCategories(int userId);

    @Query("UPDATE products SET userId = :userId")
    void updateUserIdForAll(int userId);

    @Query("DELETE FROM products WHERE userId = :userId")
    void deleteAllByUserId(int userId);

    @Query("SELECT * FROM products WHERE name = :name AND userId = :userId LIMIT 1")
    Product getProductByName(String name, int userId);
}
