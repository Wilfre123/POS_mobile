package com.example.bluepos.pos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insert(Expense expense);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY timestamp DESC")
    List<Expense> getAllExpenses(int userId);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY timestamp DESC")
    List<Expense> getAllExpensesSync(int userId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId")
    double getTotalExpenses(int userId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND timestamp >= :startOfDay")
    double getTodayExpenses(int userId, long startOfDay);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND timestamp >= :since")
    double getExpensesSince(long since, int userId);

    @Query("UPDATE expenses SET userId = :userId")
    void updateUserIdForAll(int userId);

    @Query("DELETE FROM expenses WHERE userId = :userId")
    void deleteAllByUserId(int userId);
}
