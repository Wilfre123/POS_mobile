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

    @Query("SELECT * FROM sales WHERE dataOwnerId = :dataOwnerId ORDER BY timestamp DESC")
    List<Sale> getAllSales(int dataOwnerId);

    @Query("SELECT * FROM sales WHERE dataOwnerId = :dataOwnerId ORDER BY timestamp DESC")
    List<Sale> getAllSalesSync(int dataOwnerId);

    @Query("SELECT * FROM sales WHERE userId = :userId ORDER BY timestamp DESC")
    List<Sale> getStaffSalesSync(int userId);

    @Query("SELECT * FROM sales WHERE dataOwnerId = :dataOwnerId AND id = :saleId")
    Sale getSaleById(int dataOwnerId, int saleId);

    @Query("UPDATE sales SET userId = :userId")
    void updateUserIdForAll(int userId);

    @Query("DELETE FROM sales WHERE userId = :userId")
    void deleteAllByUserId(int userId);

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE dataOwnerId = :dataOwnerId AND timestamp >= :since")
    double getRevenueSince(long since, int dataOwnerId);

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE dataOwnerId = :dataOwnerId")
    double getTotalRevenue(int dataOwnerId);

    @Query("SELECT COUNT(*) FROM sales WHERE dataOwnerId = :dataOwnerId")
    int getSalesCount(int dataOwnerId);
}
