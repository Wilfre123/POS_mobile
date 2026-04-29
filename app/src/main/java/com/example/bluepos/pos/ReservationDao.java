package com.example.bluepos.pos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ReservationDao {
    @Insert
    void insert(Reservation reservation);

    @Update
    void update(Reservation reservation);

    @Delete
    void delete(Reservation reservation);

    @Query("SELECT * FROM reservations WHERE userId = :userId ORDER BY timestamp DESC")
    List<Reservation> getAllReservations(int userId);

    @Query("SELECT * FROM reservations WHERE userId = :userId AND status = :status ORDER BY timestamp ASC")
    List<Reservation> getReservationsByStatus(int userId, String status);

    @Query("UPDATE reservations SET userId = :userId")
    void updateUserIdForAll(int userId);

    @Query("DELETE FROM reservations WHERE userId = :userId")
    void deleteAllByUserId(int userId);
}
