package com.example.bluepos.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insert(Expense expense);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY timestamp DESC")
    List<Expense> getAllExpenses(int userId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId")
    double getTotalExpenses(int userId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND timestamp >= :startOfDay")
    double getTodayExpenses(int userId, long startOfDay);
}
