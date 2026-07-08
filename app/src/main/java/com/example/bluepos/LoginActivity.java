package com.example.bluepos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bluepos.pos.AppDatabase;
import com.example.bluepos.pos.POSActivity;
import com.example.bluepos.pos.User;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = AppDatabase.getDatabase(this);

        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (!email.isEmpty() && !password.isEmpty()) {
                User user = db.userDao().login(email, password);
                if (user != null) {
                    // Save user info to SharedPreferences
                    android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    prefs.edit()
                        .putInt("userId", user.id)
                        .putString("username", user.name)
                        .putString("userEmail", user.email)
                        .putString("userRole", user.role)
                        .putInt("adminId", user.adminId != null ? user.adminId : -1)
                        .apply();

                    Intent intent = new Intent(LoginActivity.this, POSActivity.class);
                    intent.putExtra("TARGET_VIEW", "DASHBOARD");
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Incorrect email or password", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show();
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}
