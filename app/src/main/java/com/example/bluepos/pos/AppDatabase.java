package com.example.bluepos.pos;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Product.class, User.class, Sale.class, Expense.class}, version = 6)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract ProductDao productDao();
    public abstract UserDao userDao();
    public abstract SaleDao saleDao();
    public abstract ExpenseDao expenseDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "pos_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Only for simplicity in this example
                    .build();
        }
        return instance;
    }
}
