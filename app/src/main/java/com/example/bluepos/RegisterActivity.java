package com.example.bluepos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bluepos.pos.AppDatabase;
import com.example.bluepos.pos.User;

public class RegisterActivity extends AppCompatActivity {

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = AppDatabase.getDatabase(this);

        EditText etName = findViewById(R.id.etName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                if (db.userDao().getUserByEmail(email) != null) {
                    Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
                    return;
                }
                User newUser = new User(name, email, password);
                db.userDao().insert(newUser);
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                // After registration, go to login
                finish();
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        tvLogin.setOnClickListener(v -> {
            finish(); // Go back to LoginActivity
        });
    }
}
