package com.example.bluepos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import com.example.bluepos.pos.POSActivity;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        
        // Update Nav Header with username
        android.view.View headerView = navigationView.getHeaderView(0);
        TextView tvUsername = headerView.findViewById(R.id.nav_header_username);
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Guest User");
        if (tvUsername != null) tvUsername.setText(username);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        CardView cardPOS = findViewById(R.id.cardPOS);
        CardView cardInventory = findViewById(R.id.cardInventory);
        CardView cardReports = findViewById(R.id.cardReports);

        cardPOS.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, POSActivity.class));
        });

        cardInventory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProductsActivity.class));
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();
            
            if (id == R.id.nav_pos) {
                startActivity(new Intent(MainActivity.this, POSActivity.class));
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(MainActivity.this, ProductsActivity.class));
            } else if (id == R.id.nav_logout) {
                showLogoutDialog();
            }
            return true;
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Do you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
