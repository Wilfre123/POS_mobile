package com.example.bluepos.pos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DebtDao {
    @Insert
    void insert(Debt debt);

    @Update
    void update(Debt debt);

    @Delete
    void delete(Debt debt);

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY timestamp DESC")
    List<Debt> getAllDebts(int userId);

    @Query("SELECT * FROM debts WHERE userId = :userId AND status = :status ORDER BY timestamp DESC")
    List<Debt> getDebtsByStatus(int userId, String status);

    @Query("UPDATE debts SET userId = :userId")
    void updateUserIdForAll(int userId);

    @Query("DELETE FROM debts WHERE userId = :userId")
    void deleteAllByUserId(int userId);
}
