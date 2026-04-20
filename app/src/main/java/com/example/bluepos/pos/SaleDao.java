package com.example.bluepos.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SaleDao {
    @Insert
    void insert(Sale sale);

    @Query("SELECT * FROM sales WHERE userId = :userId ORDER BY timestamp DESC")
    List<Sale> getAllSales(int userId);

    @Query("SELECT * FROM sales WHERE userId = :userId AND id = :saleId")
    Sale getSaleById(int userId, int saleId);
}
