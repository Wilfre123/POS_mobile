package com.example.bluepos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import com.example.bluepos.pos.POSActivity;
import com.example.bluepos.pos.AppDatabase;
import com.example.bluepos.pos.Product;
import com.google.android.material.navigation.NavigationView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private AppDatabase db;
    private ExpiringProductAdapter expiringAdapter;
    private int userId;

    private TextView tvNotificationBadge;
    private android.view.View btnNotification;

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
        CardView cardSettings = findViewById(R.id.cardSettings);

        cardPOS.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, POSActivity.class)));

        cardInventory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProductsActivity.class)));

        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, POSActivity.class);
            intent.putExtra("TARGET_VIEW", "REPORTS");
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, POSActivity.class);
            intent.putExtra("TARGET_VIEW", "SETTINGS");
            startActivity(intent);
        });

        setupExpiringProducts();

        btnNotification = findViewById(R.id.btnNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> showLowStockDialog());
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();
            
            if (id == R.id.nav_pos) {
                startActivity(new Intent(MainActivity.this, POSActivity.class));
            } else if (id == R.id.nav_dashboard) {
                Intent intent = new Intent(MainActivity.this, POSActivity.class);
                intent.putExtra("TARGET_VIEW", "DASHBOARD");
                startActivity(intent);
            } else if (id == R.id.nav_statistics) {
                Intent intent = new Intent(MainActivity.this, POSActivity.class);
                intent.putExtra("TARGET_VIEW", "STATISTICS");
                startActivity(intent);
            } else if (id == R.id.nav_reports) {
                Intent intent = new Intent(MainActivity.this, POSActivity.class);
                intent.putExtra("TARGET_VIEW", "REPORTS");
                startActivity(intent);
            } else if (id == R.id.nav_sales_history) {
                Intent intent = new Intent(MainActivity.this, POSActivity.class);
                intent.putExtra("TARGET_VIEW", "SALES_HISTORY");
                startActivity(intent);
            } else if (id == R.id.nav_reservations) {
                Intent intent = new Intent(MainActivity.this, POSActivity.class);
                intent.putExtra("TARGET_VIEW", "RESERVATIONS");
                startActivity(intent);
            } else if (id == R.id.nav_debts) {
                Intent intent = new Intent(MainActivity.this, POSActivity.class);
                intent.putExtra("TARGET_VIEW", "DEBTS");
                startActivity(intent);
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(MainActivity.this, ProductsActivity.class));
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, POSActivity.class);
                intent.putExtra("TARGET_VIEW", "SETTINGS");
                startActivity(intent);
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

    private void setupExpiringProducts() {
        db = AppDatabase.getDatabase(this);
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        RecyclerView rvExpiring = findViewById(R.id.rvExpiringProducts);
        rvExpiring.setLayoutManager(new LinearLayoutManager(this));
        expiringAdapter = new ExpiringProductAdapter(new ArrayList<>());
        rvExpiring.setAdapter(expiringAdapter);

        refreshExpiringProducts();
    }

    private void refreshExpiringProducts() {
        new Thread(() -> {
            List<Product> allProducts = db.productDao().getAll(userId);
            List<Product> expiringOnly = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.hasExpiration && p.expirationDate != null) {
                    expiringOnly.add(p);
                }
            }
            runOnUiThread(() -> expiringAdapter.updateList(expiringOnly));
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (expiringAdapter != null) {
            refreshExpiringProducts();
        }
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        new Thread(() -> {
            List<Product> products = db.productDao().getAll(userId);
            int lowStockCount = 0;
            for (Product p : products) {
                if (p.stock <= p.minStock) lowStockCount++;
            }

            final int count = lowStockCount;
            runOnUiThread(() -> {
                if (tvNotificationBadge != null) {
                    if (count > 0) {
                        tvNotificationBadge.setVisibility(android.view.View.VISIBLE);
                        tvNotificationBadge.setText(String.valueOf(count));
                    } else {
                        tvNotificationBadge.setVisibility(android.view.View.GONE);
                    }
                }
            });
        }).start();
    }

    private void showLowStockDialog() {
        new Thread(() -> {
            List<Product> products = db.productDao().getAll(userId);
            List<Product> lowStockItems = new ArrayList<>();
            for (Product p : products) {
                if (p.stock <= p.minStock) lowStockItems.add(p);
            }

            runOnUiThread(() -> {
                if (lowStockItems.isEmpty()) {
                    Toast.makeText(this, "No low stock products", Toast.LENGTH_SHORT).show();
                    return;
                }

                android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_low_stock, null);
                android.widget.EditText etSearchLowStock = dialogView.findViewById(R.id.etSearchLowStock);
                RecyclerView rvLowStock = dialogView.findViewById(R.id.rvLowStock);
                android.widget.Button btnManageInventory = dialogView.findViewById(R.id.btnManageInventory);

                rvLowStock.setLayoutManager(new LinearLayoutManager(this));

                com.example.bluepos.pos.ProductAdapter lowStockAdapter = new com.example.bluepos.pos.ProductAdapter(new ArrayList<>(lowStockItems), product -> {});
                lowStockAdapter.setLowStockMode(true);
                rvLowStock.setAdapter(lowStockAdapter);

                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Low Stock Products")
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .create();

                btnManageInventory.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, ProductsActivity.class));
                    dialog.dismiss();
                });

                etSearchLowStock.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        List<Product> filtered = new ArrayList<>();
                        for (Product p : lowStockItems) {
                            if (p.name.toLowerCase().contains(s.toString().toLowerCase())) {
                                filtered.add(p);
                            }
                        }
                        lowStockAdapter.updateList(filtered);
                    }
                    @Override public void afterTextChanged(android.text.Editable s) {}
                });

                dialog.show();
            });
        }).start();
    }
}
