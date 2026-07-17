package com.example.bluepos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bluepos.pos.AppDatabase;
import com.example.bluepos.pos.User;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private AppDatabase db;
    private List<User> adminList = new ArrayList<>();
    private List<String> adminNames = new ArrayList<>();
    private ArrayAdapter<String> adminAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = AppDatabase.getDatabase(this);

        TextInputEditText etName = findViewById(R.id.etName);
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        RadioGroup rgRole = findViewById(R.id.rgRole);
        RadioButton rbStaff = findViewById(R.id.rbStaff);
        Spinner spinnerAdmin = findViewById(R.id.spinnerAdmin);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        adminAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, adminNames);
        adminAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAdmin.setAdapter(adminAdapter);

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStaff) {
                spinnerAdmin.setVisibility(View.VISIBLE);
                loadAdmins();
            } else {
                spinnerAdmin.setVisibility(View.GONE);
            }
        });

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            String role = rbStaff.isChecked() ? "Staff" : "Admin";
            Integer adminId = null;

            if (role.equals("Staff")) {
                int selectedAdminPos = spinnerAdmin.getSelectedItemPosition();
                if (selectedAdminPos >= 0 && selectedAdminPos < adminList.size()) {
                    adminId = adminList.get(selectedAdminPos).id;
                } else {
                    Toast.makeText(this, "Please select an Admin", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                final Integer finalAdminId = adminId;
                new Thread(() -> {
                    if (db.userDao().getUserByEmail(email) != null) {
                        runOnUiThread(() -> Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    User newUser = new User(name, email, password, role, finalAdminId);
                    db.userDao().insert(newUser);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }).start();
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        tvLogin.setOnClickListener(v -> {
            finish(); // Go back to LoginActivity
        });
    }

    private void loadAdmins() {
        new Thread(() -> {
            adminList = db.userDao().getAllAdmins();
            adminNames.clear();
            for (User admin : adminList) {
                adminNames.add(admin.name);
            }
            runOnUiThread(() -> adminAdapter.notifyDataSetChanged());
        }).start();
    }
}
