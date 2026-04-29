package com.example.bluepos.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SaleDao {
    @Insert
    long insert(Sale sale);

    @Query("DELETE FROM sales WHERE id = :saleId")
    void deleteById(int saleId);

    @Query("SELECT * FROM sales WHERE userId = :userId ORDER BY timestamp DESC")
    List<Sale> getAllSales(int userId);

    @Query("SELECT * FROM sales WHERE userId = :userId AND id = :saleId")
    Sale getSaleById(int userId, int saleId);

    @Query("UPDATE sales SET userId = :userId")
    void updateUserIdForAll(int userId);

    @Query("DELETE FROM sales WHERE userId = :userId")
    void deleteAllByUserId(int userId);
}
