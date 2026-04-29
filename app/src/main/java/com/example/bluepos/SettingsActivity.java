package com.example.bluepos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.bluepos.pos.AppDatabase;
import com.example.bluepos.pos.User;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsActivity extends AppCompatActivity {

    private AppDatabase db;
    private int userId;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> {
                if (uri != null) {
                    performExport(uri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    performImport(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = AppDatabase.getDatabase(this);
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.cardEditProfile).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.cardExportData).setOnClickListener(v -> exportLauncher.launch("bluepos_backup.db"));
        findViewById(R.id.cardImportData).setOnClickListener(v -> importLauncher.launch(new String[]{"*/*"}));
        findViewById(R.id.cardResetData).setOnClickListener(v -> showResetDataDialog());

        // Handle incoming action
        String action = getIntent().getStringExtra("ACTION");
        if ("EDIT_PROFILE".equals(action)) {
            showEditProfileDialog();
        } else if ("EXPORT".equals(action)) {
            exportLauncher.launch("bluepos_backup.db");
        } else if ("IMPORT".equals(action)) {
            importLauncher.launch(new String[]{"*/*"});
        } else if ("RESET".equals(action)) {
            showResetDataDialog();
        }
    }

    private void showResetDataDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        view.findViewById(R.id.etName).setVisibility(android.view.View.GONE);
        view.findViewById(R.id.etEmail).setVisibility(android.view.View.GONE);
        view.findViewById(R.id.etPassword).setVisibility(android.view.View.GONE);
        ((com.google.android.material.textfield.TextInputLayout)view.findViewById(R.id.tilOldPassword)).setHint("Enter Password to Reset");
        TextInputEditText etConfirmPassword = view.findViewById(R.id.etOldPassword);

        new AlertDialog.Builder(this)
                .setTitle("Reset My Data")
                .setMessage("This will permanently delete ALL your products, sales, expenses, and reservations. Are you sure?")
                .setView(view)
                .setPositiveButton("Reset Now", (dialog, which) -> {
                    String password = etConfirmPassword.getText().toString();
                    new Thread(() -> {
                        User user = db.userDao().getUserByEmail(prefs.getString("userEmail", ""));
                        if (user != null && user.password.equals(password)) {
                            // Delete data
                            db.productDao().deleteAllByUserId(user.id);
                            db.saleDao().deleteAllByUserId(user.id);
                            db.expenseDao().deleteAllByUserId(user.id);
                            db.reservationDao().deleteAllByUserId(user.id);
                            db.debtDao().deleteAllByUserId(user.id);
                            
                            runOnUiThread(() -> Toast.makeText(this, "All data reset successfully", Toast.LENGTH_SHORT).show());
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditProfileDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etOldPassword = view.findViewById(R.id.etOldPassword);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);

        new Thread(() -> {
            User user = db.userDao().getUserByEmail(prefs.getString("userEmail", ""));
            if (user != null) {
                runOnUiThread(() -> {
                    etName.setText(user.name);
                    etEmail.setText(user.email);
                });
            }
        }).start();

        new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String email = etEmail.getText().toString();
                    String oldPassword = etOldPassword.getText().toString();
                    String newPassword = etPassword.getText().toString();

                    if (name.isEmpty() || email.isEmpty() || oldPassword.isEmpty()) {
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        User user = db.userDao().getUserByEmail(prefs.getString("userEmail", ""));
                        if (user != null) {
                            if (!user.password.equals(oldPassword)) {
                                runOnUiThread(() -> Toast.makeText(this, "Incorrect old password", Toast.LENGTH_SHORT).show());
                                return;
                            }

                            user.name = name;
                            user.email = email;
                            if (!newPassword.isEmpty()) {
                                user.password = newPassword;
                            }
                            db.userDao().update(user);

                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("userEmail", email);
                            editor.apply();

                            runOnUiThread(() -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performExport(Uri uri) {
        try {
            db.close();
            File dbFile = getDatabasePath("pos_database");
            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = getContentResolver().openOutputStream(uri)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performImport(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("Import Data")
                .setMessage("This will overwrite your current data. Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    try {
                        AppDatabase.destroyInstance();
                        File dbFile = getDatabasePath("pos_database");
                        try (InputStream in = getContentResolver().openInputStream(uri);
                             OutputStream out = new FileOutputStream(dbFile)) {
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        }
                        // Re-initialize database
                        db = AppDatabase.getDatabase(this);
                        
                        // Update current user context and reassign imported data
                        new Thread(() -> {
                            User newUser = db.userDao().getUserByEmail(prefs.getString("userEmail", ""));
                            if (newUser != null) {
                                // Reassign all imported data to the current user
                                db.productDao().updateUserIdForAll(newUser.id);
                                db.saleDao().updateUserIdForAll(newUser.id);
                                db.expenseDao().updateUserIdForAll(newUser.id);
                                db.reservationDao().updateUserIdForAll(newUser.id);
                                db.debtDao().updateUserIdForAll(newUser.id);

                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("userId", newUser.id);
                                editor.apply();
                                userId = newUser.id;
                                runOnUiThread(() -> Toast.makeText(this, "Data imported and merged to your account.", Toast.LENGTH_LONG).show());
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, "Data imported. Please log in again to sync.", Toast.LENGTH_LONG).show());
                            }
                        }).start();
                    } catch (Exception e) {
                        Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
