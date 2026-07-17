package com.example.bluepos.pos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluepos.R;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationView;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class POSActivity extends AppCompatActivity {
    AppDatabase db;
    ProductAdapter adapter;
    List<CartItem> cart = new ArrayList<>();
    double totalAmount = 0.0;
    Button btnCheckout, btnCheckout2;
    Toolbar toolbar;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle toggle;
    EditText etAmountPaid, etSearch, etSearchCart;
    TextView tvChange, tvCartTotal, tvCartTotal2;
    RecyclerView rvCart, rvSalesHistory, rvManageProducts, rvRecentExpensesManage;
    CartAdapter cartAdapter;
    SaleAdapter saleAdapter;
    ProductManageAdapter manageAdapter;
    ExpenseAdapter manageExpenseAdapter;
    View cashierContent, dashboardContent, reportsContent, salesHistoryContent, cartContent, statisticsContent, reservations_content, debts_content, settingsContent, productsContent;
    View manageProductsSection, manageExpensesSection, manageHistorySection;
    EditText etExpenseTitleManage, etExpenseAmountManage;
    TextView tvExpTodayManage, tvExpTotalManage, tvExpRevenueManage, tvExpNetManage;
    androidx.appcompat.widget.SearchView searchViewManage;
    AutoCompleteTextView autoCompleteCategoryManage;
    ImageButton btnViewCart;
    RecyclerView rvReservations, rvDebts;
    ReservationAdapter reservationAdapter;
    DebtAdapter debtAdapter;
    List<Reservation> reservations = new ArrayList<>();
    List<Debt> debts = new ArrayList<>();

    TextView tvNetIncome, tvTotalRevenue, tvTotalExpenses, tvTodaySales, tvTotalProducts, tvTotalSalesCount, tvItemsSold, tvLowStock;
    TextView tvStatsTotalRevenue, tvStatsTotalExpenses, tvStatsNetIncome, tvStatsVisitCount;
    LineChart revenueChart, expenseRevenueChart;
    HorizontalBarChart topProductsChart;
    PieChart salesCategoryChart, stockCategoryChart;
    TextView tvNotificationBadge;
    View btnNotification;
    EditText etSearchReservations, etSearchDebts;
    Button btnExportProducts;
    List<Sale> currentChartSales = new ArrayList<>();
    List<Sale> statsChartSales = new ArrayList<>();
    List<Expense> statsChartExpenses = new ArrayList<>();

    int userId;
    String userRole;
    int adminId;
    int dataOwnerId; // This will be userId if Admin, and adminId if Staff
    int statsVisitCount = 0;

    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            uri -> {
                if (uri != null) {
                    saveExcelToUri(uri);
                }
            }
    );

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

    private static final String CHANNEL_ID = "low_stock_channel";
    private static final int NOTIFICATION_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);
        createNotificationChannel();

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getInt("userId", -1);
            userRole = prefs.getString("userRole", "Admin");
            adminId = prefs.getInt("adminId", -1);
        } else {
            // If passed by intent, we might need to fetch role/adminId from DB or assume defaults
            // For robustness, better to read from prefs or DB. 
            // Assuming LoginActivity always sets Prefs.
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userRole = prefs.getString("userRole", "Admin");
            adminId = prefs.getInt("adminId", -1);
        }

        if (userId == -1) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dataOwnerId = "Staff".equals(userRole) ? adminId : userId;
        if (dataOwnerId == -1) dataOwnerId = userId; // Fallback

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (cashierContent.getVisibility() != View.VISIBLE) {
                    switchContent(cashierContent, "Green POS System");
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        db = AppDatabase.getDatabase(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Update Nav Header with username
        android.view.View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvUsername = headerView.findViewById(R.id.nav_header_username);
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = prefs.getString("username", "User");
            if (tvUsername != null) tvUsername.setText(username);
            
            TextView tvUserRole = headerView.findViewById(R.id.nav_header_role);
            if (tvUserRole != null) tvUserRole.setText(userRole);
        }

        if ("Staff".equals(userRole)) {
            navigationView.getMenu().findItem(R.id.nav_products).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_reports).setVisible(false);
        }

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        cashierContent = findViewById(R.id.cashier_content);
        dashboardContent = findViewById(R.id.dashboard_content);
        reportsContent = findViewById(R.id.reports_content);
        salesHistoryContent = findViewById(R.id.sales_history_content);
        cartContent = findViewById(R.id.cart_content);
        statisticsContent = findViewById(R.id.statistics_content);
        reservations_content = findViewById(R.id.reservations_content);
        debts_content = findViewById(R.id.debts_content);
        settingsContent = findViewById(R.id.settings_content);
        productsContent = findViewById(R.id.products_content);

        // Integrated Products views
        rvManageProducts = findViewById(R.id.rvManageProducts);
        searchViewManage = findViewById(R.id.searchViewManage);
        manageProductsSection = findViewById(R.id.manage_products_section);
        manageExpensesSection = findViewById(R.id.manage_expenses_section);
        manageHistorySection = findViewById(R.id.manage_history_section);
        etExpenseTitleManage = findViewById(R.id.etExpenseTitleManage);
        etExpenseAmountManage = findViewById(R.id.etExpenseAmountManage);
        tvExpTodayManage = findViewById(R.id.tvExpTodayManage);
        tvExpTotalManage = findViewById(R.id.tvExpTotalManage);
        tvExpRevenueManage = findViewById(R.id.tvExpRevenueManage);
        tvExpNetManage = findViewById(R.id.tvExpNetManage);
        rvRecentExpensesManage = findViewById(R.id.rvRecentExpensesManage);
        autoCompleteCategoryManage = findViewById(R.id.autoCompleteCategoryManage);

        rvManageProducts.setLayoutManager(new LinearLayoutManager(this));
        manageAdapter = new ProductManageAdapter(new ArrayList<>(), new ProductManageAdapter.OnProductActionListener() {
            @Override
            public void onEdit(Product product) {
                showProductDialog(product);
            }

            @Override
            public void onDelete(Product product) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Delete Product")
                        .setMessage("Are you sure you want to delete " + product.name + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            verifyPasswordAndExecute(() -> {
                                new Thread(() -> {
                                    db.productDao().delete(product);
                                    runOnUiThread(() -> {
                                        refreshManageProducts();
                                        refreshProductList();
                                        updateNotificationBadge();
                                    });
                                }).start();
                            });
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        rvManageProducts.setAdapter(manageAdapter);

        rvRecentExpensesManage.setLayoutManager(new LinearLayoutManager(this));
        manageExpenseAdapter = new ExpenseAdapter(new ArrayList<>(), new ExpenseAdapter.OnExpenseActionListener() {
            @Override
            public void onEdit(Expense expense) {
                onEditExpenseManage(expense);
            }

            @Override
            public void onDelete(Expense expense) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Delete Expense")
                        .setMessage("Are you sure you want to delete this expense?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            verifyPasswordAndExecute(() -> {
                                new Thread(() -> {
                                    db.expenseDao().delete(expense);
                                    runOnUiThread(POSActivity.this::updateExpenseUIManage);
                                }).start();
                            });
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        rvRecentExpensesManage.setAdapter(manageExpenseAdapter);

        findViewById(R.id.fabAddProductManage).setOnClickListener(v -> showProductDialog(null));
        findViewById(R.id.btnAddExpenseManage).setOnClickListener(v -> saveExpenseManage());

        com.google.android.material.tabs.TabLayout tabLayoutProducts = findViewById(R.id.tabLayoutProducts);
        tabLayoutProducts.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    manageProductsSection.setVisibility(View.VISIBLE);
                    manageExpensesSection.setVisibility(View.GONE);
                    manageHistorySection.setVisibility(View.GONE);
                    refreshManageProducts();
                } else if (tab.getPosition() == 1) {
                    manageProductsSection.setVisibility(View.GONE);
                    manageExpensesSection.setVisibility(View.VISIBLE);
                    manageHistorySection.setVisibility(View.GONE);
                    updateExpenseUIManage();
                } else {
                    manageProductsSection.setVisibility(View.GONE);
                    manageExpensesSection.setVisibility(View.GONE);
                    manageHistorySection.setVisibility(View.VISIBLE);
                    updateExpenseUIManage();
                }
            }
            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        setupSearchManage();
        setupCategoryFilterManage();

        RecyclerView rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new ProductAdapter(new ArrayList<>(), this::onAddToCart);
        rvProducts.setAdapter(adapter);

        etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupCartAdapter();
        btnCheckout = findViewById(R.id.btnCheckout);
        btnCheckout2 = findViewById(R.id.btnCheckout2);
        etAmountPaid = findViewById(R.id.etAmountPaid);
        tvChange = findViewById(R.id.tvChange);

        View.OnClickListener checkoutListener = v -> {
            if (cart.isEmpty()) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            double paid = getPaidAmount();
            if (paid < totalAmount) {
                Toast.makeText(this, "Insufficient payment", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder summary = new StringBuilder();
            boolean lowStockDetected = false;
            String firstLowStockProduct = "";
            int firstLowStockValue = 0;

            for (CartItem item : cart) {
                summary.append(item.product.name).append(" x").append(item.quantity).append(", ");
                item.product.stock -= item.quantity;
                db.productDao().update(item.product);
                
                if (item.product.stock <= 10) {
                    lowStockDetected = true;
                    firstLowStockProduct = item.product.name;
                    firstLowStockValue = item.product.stock;
                }
            }

            if (lowStockDetected) {
                triggerLowStockNotification(firstLowStockProduct, firstLowStockValue);
            }

            Sale sale = new Sale(totalAmount, paid, paid - totalAmount, System.currentTimeMillis(), summary.toString(), userId, getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "Unknown"), dataOwnerId);
            db.saleDao().insert(sale);

            cart.clear();
            cartAdapter.notifyDataSetChanged();
            updateCartQuantitiesInAdapter();
            updateTotals();
            refreshProductList();
            updateNotificationBadge();
            Toast.makeText(this, "Checkout successful", Toast.LENGTH_SHORT).show();
            switchContent(cashierContent, "Green POS System");
        };

        btnCheckout.setOnClickListener(checkoutListener);
        btnCheckout2.setOnClickListener(checkoutListener);

        etAmountPaid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateChange();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnViewCart = findViewById(R.id.btnViewCart);
        btnViewCart.setOnClickListener(v -> switchContent(cartContent, "Cart"));

        rvSalesHistory = findViewById(R.id.rvSales);
        rvSalesHistory.setLayoutManager(new LinearLayoutManager(this));
        saleAdapter = new SaleAdapter(new ArrayList<>());
        rvSalesHistory.setAdapter(saleAdapter);

        rvReservations = findViewById(R.id.rvReservations);
        rvReservations.setLayoutManager(new LinearLayoutManager(this));
        setupReservationAdapter();

        etSearchReservations = findViewById(R.id.etSearchReservations);
        etSearchReservations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReservations(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearchDebts = findViewById(R.id.etSearchDebts);
        etSearchDebts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDebts(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        rvDebts = findViewById(R.id.rvDebts);
        rvDebts.setLayoutManager(new LinearLayoutManager(this));
        setupDebtAdapter();

        findViewById(R.id.btnAddReservation).setOnClickListener(v -> showAddReservationDialog());
        findViewById(R.id.btnAddDebt).setOnClickListener(v -> showAddDebtDialog());

        tvStatsVisitCount = findViewById(R.id.tvStatsVisitCount);
        statsVisitCount = getSharedPreferences("StatsPrefs", MODE_PRIVATE).getInt("visit_count", 0);

        initDashboardViews();
        setupChart();
        setupStatisticsCharts();
        updateNotificationBadge();

        btnNotification = findViewById(R.id.btnNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnNotification.setOnClickListener(v -> showLowStockDialog());

        btnExportProducts = findViewById(R.id.btnExportProducts);
        if (btnExportProducts != null) {
            btnExportProducts.setOnClickListener(v -> exportProductsToExcel());
        }

        findViewById(R.id.cardEditProfile).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.cardExportData).setOnClickListener(v -> exportLauncher.launch("greenpos_backup.db"));
        findViewById(R.id.cardImportData).setOnClickListener(v -> importLauncher.launch(new String[]{"*/*"}));
        findViewById(R.id.cardResetData).setOnClickListener(v -> showResetDataDialog());

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pos) {
                switchContent(cashierContent, "Green POS System");
            } else if (id == R.id.nav_dashboard) {
                showDashboard();
            } else if (id == R.id.nav_reports) {
                switchContent(reportsContent, "Reports");
            } else if (id == R.id.nav_sales_history) {
                showSalesHistoryPage();
            } else if (id == R.id.nav_products) {
                showManageProductsPage();
            } else if (id == R.id.nav_statistics) {
                showStatistics();
            } else if (id == R.id.nav_reservations) {
                showReservationsPage();
            } else if (id == R.id.nav_debts) {
                showDebtsPage();
            } else if (id == R.id.nav_settings) {
                switchContent(settingsContent, "Settings");
            } else if (id == R.id.nav_logout) {
                showLogoutDialog();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Set default view
        switchContent(cashierContent, "Green POS System");
        refreshProductList();

        // Handle target view from Intent
        String targetView = getIntent().getStringExtra("TARGET_VIEW");
        if ("REPORTS".equals(targetView)) {
            switchContent(reportsContent, "Reports");
        } else if ("SETTINGS".equals(targetView)) {
            switchContent(settingsContent, "Settings");
        } else if ("DASHBOARD".equals(targetView)) {
            showDashboard();
        } else if ("STATISTICS".equals(targetView)) {
            showStatistics();
        } else if ("SALES_HISTORY".equals(targetView)) {
            showSalesHistoryPage();
        } else if ("RESERVATIONS".equals(targetView)) {
            showReservationsPage();
        } else if ("DEBTS".equals(targetView)) {
            showDebtsPage();
        } else if ("INVENTORY".equals(targetView)) {
            showManageProductsPage();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProductList();
        updateNotificationBadge();
        if (dashboardContent.getVisibility() == View.VISIBLE) {
            updateDashboardData();
        }
    }

    private void setupCartAdapter() {
        rvCart = findViewById(R.id.rvCart);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        tvCartTotal2 = findViewById(R.id.tvCartTotal2);
        etSearchCart = findViewById(R.id.etSearchCart);

        rvCart.setLayoutManager(new GridLayoutManager(this, 2));
        cartAdapter = new CartAdapter(cart, new CartAdapter.OnCartActionListener() {
            @Override
            public void onIncrease(CartItem item) {
                if (item.quantity < item.product.stock) {
                    item.quantity++;
                    updateTotals();
                    cartAdapter.notifyDataSetChanged();
                    updateCartQuantitiesInAdapter();
                } else {
                    Toast.makeText(POSActivity.this, "Maximum stock reached", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDecrease(CartItem item) {
                if (item.quantity > 1) {
                    item.quantity--;
                    updateTotals();
                    cartAdapter.notifyDataSetChanged();
                    updateCartQuantitiesInAdapter();
                }
            }

            @Override
            public void onRemove(CartItem item) {
                cart.remove(item);
                cartAdapter.notifyDataSetChanged();
                updateTotals();
                updateCartQuantitiesInAdapter();
            }
        });
        rvCart.setAdapter(cartAdapter);

        if (etSearchCart != null) {
            etSearchCart.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterCart(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterCart(String query) {
        List<CartItem> filtered = new ArrayList<>();
        for (CartItem item : cart) {
            if (item.product.name.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        cartAdapter.updateList(filtered);
    }

    private void updateTotals() {
        totalAmount = 0;
        for (CartItem item : cart) {
            totalAmount += item.product.price * item.quantity;
        }
        String totalStr = String.format("Total: ₱%.2f", totalAmount);
        if (tvCartTotal != null) tvCartTotal.setText(totalStr);
        if (tvCartTotal2 != null) tvCartTotal2.setText(totalStr);
        updateCheckoutButton();
        calculateChange();
    }

    private void updateCartQuantitiesInAdapter() {
        java.util.Map<Integer, Integer> quantities = new java.util.HashMap<>();
        for (CartItem item : cart) {
            quantities.put(item.product.id, item.quantity);
        }
        if (adapter != null) {
            adapter.setCartQuantities(quantities);
        }
    }


    private void calculateChange() {
        double paid = getPaidAmount();
        double change = Math.max(0, paid - totalAmount);
        if (tvChange != null) tvChange.setText(String.format("Change: ₱%.2f", change));
    }

    private double getPaidAmount() {
        if (etAmountPaid == null) return 0;
        String s = etAmountPaid.getText().toString();
        try {
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void filterProducts(String query) {
        new Thread(() -> {
            List<Product> filtered = db.productDao().searchProducts(dataOwnerId, "%" + query + "%");
            runOnUiThread(() -> adapter.updateList(filtered));
        }).start();
    }

    private void onAddToCart(Product product) {
        if (product.stock <= 0) {
            Toast.makeText(this, "Product out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        for (CartItem item : cart) {
            if (item.product.id == product.id) {
                if (item.quantity < product.stock) {
                    item.quantity++;
                    updateTotals();
                    cartAdapter.notifyDataSetChanged();
                    updateCartQuantitiesInAdapter();
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Maximum stock reached", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }

        cart.add(new CartItem(product, 1));
        updateTotals();
        cartAdapter.notifyDataSetChanged();
        updateCartQuantitiesInAdapter();
        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
    }

    private void showDashboard() {
        switchContent(dashboardContent, "Dashboard");
        updateDashboardData();
    }

    private void showStatistics() {
        statsVisitCount++;
        getSharedPreferences("StatsPrefs", MODE_PRIVATE).edit().putInt("visit_count", statsVisitCount).apply();
        if (tvStatsVisitCount != null) {
            tvStatsVisitCount.setText(String.valueOf(statsVisitCount));
        }
        switchContent(statisticsContent, "Statistics");
        updateStatisticsData();
    }

    private void initDashboardViews() {
        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvNetIncome = findViewById(R.id.tvNetIncome);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvTotalSalesCount = findViewById(R.id.tvTotalSalesCount);
        tvItemsSold = findViewById(R.id.tvItemsSold);
        tvLowStock = findViewById(R.id.tvLowStock);
        revenueChart = findViewById(R.id.revenueChart);
        salesCategoryChart = findViewById(R.id.salesCategoryChart);
        stockCategoryChart = findViewById(R.id.stockCategoryChart);

        // Initialize Statistics Views
        tvStatsTotalRevenue = findViewById(R.id.tvStatsTotalRevenue);
        tvStatsTotalExpenses = findViewById(R.id.tvStatsTotalExpenses);
        tvStatsNetIncome = findViewById(R.id.tvStatsNetIncome);
        tvStatsVisitCount = findViewById(R.id.tvStatsVisitCount);
        expenseRevenueChart = findViewById(R.id.expenseRevenueChart);
        topProductsChart = findViewById(R.id.topProductsChart);
    }

    private void setupChart() {
        if (revenueChart == null) return;
        revenueChart.getDescription().setEnabled(false);
        revenueChart.setDrawGridBackground(false);
        revenueChart.getLegend().setEnabled(true);
        revenueChart.setExtraBottomOffset(45f); // Space for rotated labels

        // Interaction settings for smooth scrolling
        revenueChart.setTouchEnabled(true);
        revenueChart.setDragEnabled(true);
        revenueChart.setScaleXEnabled(true);
        revenueChart.setScaleYEnabled(false);
        revenueChart.setPinchZoom(false);
        revenueChart.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setLabelCount(6);

        revenueChart.getAxisRight().setEnabled(false);

        revenueChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // Modal removed for cleaner UI; visual trend is focus
            }

            @Override
            public void onNothingSelected() {}
        });
    }

    private void showSaleDetailDialog(Sale sale) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss a", Locale.getDefault());
        String dateStr = sdf.format(new Date(sale.timestamp));

        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(dateStr).append("\n");
        sb.append("Total: ₱").append(String.format("%.2f", sale.totalAmount)).append("\n");
        sb.append("Paid: ₱").append(String.format("%.2f", sale.amountPaid)).append("\n");
        sb.append("Change: ₱").append(String.format("%.2f", sale.change)).append("\n\n");
        sb.append("Items:\n").append(sale.itemsSummary);

        new AlertDialog.Builder(this)
                .setTitle("Transaction Details")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void setupStatisticsCharts() {
        if (expenseRevenueChart != null) {
            expenseRevenueChart.getDescription().setEnabled(false);
            expenseRevenueChart.setDrawGridBackground(false);
            expenseRevenueChart.getLegend().setEnabled(true);
            expenseRevenueChart.setExtraBottomOffset(45f); // Space for rotated labels

            // Interaction settings for smooth scrolling
            expenseRevenueChart.setTouchEnabled(true);
            expenseRevenueChart.setDragEnabled(true);
            expenseRevenueChart.setScaleXEnabled(true);
            expenseRevenueChart.setScaleYEnabled(false);
            expenseRevenueChart.setPinchZoom(false);
            expenseRevenueChart.setDoubleTapToZoomEnabled(false);

            XAxis sxAxis = expenseRevenueChart.getXAxis();
            sxAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            sxAxis.setLabelRotationAngle(-45f);
            sxAxis.setGranularity(1f);
            sxAxis.setDrawGridLines(false);
            sxAxis.setAvoidFirstLastClipping(true);
            sxAxis.setLabelCount(6);

            expenseRevenueChart.getAxisRight().setEnabled(false);

            expenseRevenueChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    // Modal removed
                }

                @Override
                public void onNothingSelected() {}
            });
        }
        if (topProductsChart != null) {
            topProductsChart.getDescription().setEnabled(false);
            topProductsChart.setDrawGridBackground(false);
            topProductsChart.getLegend().setEnabled(false);
            topProductsChart.getXAxis().setDrawGridLines(false);
            topProductsChart.getAxisLeft().setDrawGridLines(false);
            topProductsChart.getAxisRight().setEnabled(false);
        }
    }

    private void showExpenseDetailDialog(Expense expense) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss a", Locale.getDefault());
        String dateStr = sdf.format(new Date(expense.timestamp));

        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(dateStr).append("\n");
        sb.append("Title: ").append(expense.title).append("\n");
        sb.append("Amount: ₱").append(String.format("%.2f", expense.amount));

        new AlertDialog.Builder(this)
                .setTitle("Expense Details")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }


    private void updateDashboardData() {
        new Thread(() -> {
            long today = getStartOfDay();
            double todayRev = db.saleDao().getRevenueSince(today, dataOwnerId);
            double totalRev = db.saleDao().getTotalRevenue(dataOwnerId);
            double totalExp = db.expenseDao().getTotalExpenses(dataOwnerId);
            int totalProd = db.productDao().getTotalProductCount(dataOwnerId);
            int salesCount = db.saleDao().getSalesCount(dataOwnerId);
            int lowStock = db.productDao().getLowStockCount(dataOwnerId);
            List<Sale> allSales = db.saleDao().getAllSales(dataOwnerId);
            List<Product> allProducts = db.productDao().getAllProductsSync(dataOwnerId);

            // Pre-calculate Chart Data in Background to avoid "loggy" UI
            List<Entry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd hh:mm a", Locale.US);

            if (!allSales.isEmpty()) {
                // Sort sales by timestamp ascending
                Collections.sort(allSales, (s1, s2) -> Long.compare(s1.timestamp, s2.timestamp));
                for (int i = 0; i < allSales.size(); i++) {
                    Sale sale = allSales.get(i);
                    entries.add(new Entry(i, (float) sale.totalAmount));
                    labels.add(sdf.format(new Date(sale.timestamp)));
                }
            } else {
                // Add dummy data if empty to show empty chart state properly
                entries.add(new Entry(0, 0f));
                labels.add("");
            }

            // Pre-calculate Category Data
            Map<String, Double> salesByCat = new HashMap<>();
            Map<String, Product> prodMap = new HashMap<>();
            for (Product p : allProducts) prodMap.put(p.name, p);
            
            for (Sale sale : allSales) {
                String[] items = sale.itemsSummary.split(", ");
                for (String itemStr : items) {
                    if (itemStr.isEmpty()) continue;
                    String[] parts = itemStr.split(" x");
                    if (parts.length < 2) continue;
                    String pName = parts[0];
                    int qty = Integer.parseInt(parts[1]);
                    Product p = prodMap.get(pName);
                    String cat = (p != null) ? p.category : "Unknown";
                    double rev = (p != null) ? p.price * qty : 0;
                    salesByCat.put(cat, salesByCat.getOrDefault(cat, 0.0) + rev);
                }
            }

            Map<String, Integer> stockByCat = new HashMap<>();
            for (Product p : allProducts) {
                stockByCat.put(p.category, stockByCat.getOrDefault(p.category, 0) + p.stock);
            }

            runOnUiThread(() -> {
                if (tvTodaySales != null) tvTodaySales.setText(String.format("₱%.2f", todayRev));
                if (tvTotalRevenue != null) tvTotalRevenue.setText(String.format("₱%.2f", totalRev));
                if (tvTotalExpenses != null) tvTotalExpenses.setText(String.format("₱%.2f", totalExp));
                if (tvNetIncome != null) tvNetIncome.setText(String.format("₱%.2f", totalRev - totalExp));
                if (tvTotalProducts != null) tvTotalProducts.setText(String.valueOf(totalProd));
                if (tvTotalSalesCount != null) tvTotalSalesCount.setText(String.valueOf(salesCount));
                if (tvLowStock != null) tvLowStock.setText(String.valueOf(lowStock));
                
                if (tvNotificationBadge != null) {
                    if (lowStock > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(String.valueOf(lowStock));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                }

                currentChartSales = new ArrayList<>(allSales);
                renderDashboardCharts(entries, labels, salesByCat, stockByCat);
            });
        }).start();
    }

    private void renderDashboardCharts(List<Entry> entries, List<String> labels, Map<String, Double> salesByCat, Map<String, Integer> stockByCat) {
        if (revenueChart != null) {
            LineDataSet dataSet = new LineDataSet(entries, "Checkout Transactions");
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_green));
            dataSet.setFillAlpha(40);
            dataSet.setLineWidth(3f);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleHoleRadius(2.5f);
            dataSet.setColor(ContextCompat.getColor(this, R.color.primary_green));
            dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_green));
            dataSet.setDrawValues(false);
            dataSet.setHighlightEnabled(true);
            dataSet.setDrawHorizontalHighlightIndicator(false);
            dataSet.setDrawVerticalHighlightIndicator(false);
            dataSet.setHighLightColor(ContextCompat.getColor(this, R.color.primary_green));
            dataSet.setValueTextSize(9f);

            revenueChart.setData(new LineData(dataSet));
            XAxis xAxis = revenueChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setLabelCount(6);
            revenueChart.invalidate();

            // Enable horizontal scrolling
            revenueChart.setVisibleXRangeMaximum(6); // Show 6 entries for better spacing
            if (entries.size() > 6) {
                revenueChart.moveViewToX(entries.size() - 1); // Scroll to the end
            }
        }

        updatePieChartData(salesCategoryChart, salesByCat, "Revenue by Category");
        updatePieChartDataInteger(stockCategoryChart, stockByCat, "Stock by Category");
    }

    private void updatePieChartData(PieChart pieChart, Map<String, Double> data, String label) {
        if (pieChart == null) return;
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void updatePieChartDataInteger(PieChart pieChart, Map<String, Integer> data, String label) {
        if (pieChart == null) return;
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Low Stock Notifications";
            String description = "Notifies when a product is low on stock";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void triggerLowStockNotification(String productName, int remainingStock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
                return;
            }
        }

        Intent intent = new Intent(this, POSActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Low Stock Alert!")
                .setContentText(productName + " is low on stock (" + remainingStock + " left)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(productName.hashCode(), builder.build());
    }

    private void updateNotificationBadge() {
        new Thread(() -> {
            int count = db.productDao().getLowStockCount(dataOwnerId);
            runOnUiThread(() -> {
                if (tvNotificationBadge != null) {
                    if (count > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(String.valueOf(count));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                }
            });
        }).start();
    }

    private void showLowStockDialog() {
        new Thread(() -> {
            List<Product> lowStockItems = db.productDao().getLowStockProducts(dataOwnerId);
            runOnUiThread(() -> {
                if (lowStockItems.isEmpty()) {
                    Toast.makeText(this, "No low stock items", Toast.LENGTH_SHORT).show();
                    return;
                }

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_low_stock, null);
                RecyclerView rvLowStock = dialogView.findViewById(R.id.rvLowStock);
                EditText etSearchLow = dialogView.findViewById(R.id.etSearchLowStock);

                rvLowStock.setLayoutManager(new LinearLayoutManager(this));
                ProductAdapter lowStockAdapter = new ProductAdapter(new ArrayList<>(lowStockItems), product -> {
                    // Password prompt before showing product dialog for adding stock
                    promptForInventoryPassword(() -> showProductDialog(product));
                });
                lowStockAdapter.setLowStockMode(true);
                rvLowStock.setAdapter(lowStockAdapter);

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Low Stock Alert")
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .create();

                etSearchLow.addTextChangedListener(new TextWatcher() {
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
                    @Override public void afterTextChanged(Editable s) {}
                });

                dialog.show();
            });
        }).start();
    }

    private long getStartOfDay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void updateChartData(List<Sale> sales) {
        // This is now handled inside updateDashboardData() and renderDashboardCharts()
    }

    private void updateStatisticsData() {
        new Thread(() -> {
            double totalRev = db.saleDao().getTotalRevenue(dataOwnerId);
            double totalExp = db.expenseDao().getTotalExpenses(dataOwnerId);
            List<Sale> sales = db.saleDao().getAllSales(dataOwnerId);
            List<Expense> expenses = db.expenseDao().getAllExpensesSync(dataOwnerId);
            
            // Per-transaction Revenue
            List<Entry> revEntries = new ArrayList<>();
            List<String> revLabels = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd hh:mm a", Locale.US);
            Collections.sort(sales, (s1, s2) -> Long.compare(s1.timestamp, s2.timestamp));
            for (int i = 0; i < sales.size(); i++) {
                Sale s = sales.get(i);
                revEntries.add(new Entry(i, (float) s.totalAmount));
                revLabels.add(sdf.format(new Date(s.timestamp)));
            }
            if (revEntries.isEmpty()) {
                revEntries.add(new Entry(0, 0f));
                revLabels.add("");
            }

            // Per-transaction Expenses
            List<Entry> expEntries = new ArrayList<>();
            List<String> expLabels = new ArrayList<>();
            SimpleDateFormat sdfExp = new SimpleDateFormat("MM/dd hh:mm a", Locale.US);
            Collections.sort(expenses, (e1, e2) -> Long.compare(e1.timestamp, e2.timestamp));
            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);
                expEntries.add(new Entry(i, (float) e.amount));
                expLabels.add(sdfExp.format(new Date(e.timestamp)));
            }
            if (expEntries.isEmpty()) {
                expEntries.add(new Entry(0, 0f));
                expLabels.add("");
            }

            Map<String, Integer> productSales = new HashMap<>();
            for (Sale sale : sales) {
                String[] items = sale.itemsSummary.split(", ");
                for (String itemStr : items) {
                    if (itemStr.isEmpty()) continue;
                    String[] parts = itemStr.split(" x");
                    if (parts.length >= 2) {
                        String name = parts[0];
                        int qty = Integer.parseInt(parts[1]);
                        productSales.put(name, productSales.getOrDefault(name, 0) + qty);
                    }
                }
            }

            runOnUiThread(() -> {
                if (tvStatsTotalRevenue != null) tvStatsTotalRevenue.setText(String.format("₱%.2f", totalRev));
                if (tvStatsTotalExpenses != null) tvStatsTotalExpenses.setText(String.format("₱%.2f", totalExp));
                if (tvStatsNetIncome != null) tvStatsNetIncome.setText(String.format("₱%.2f", totalRev - totalExp));
                
                statsChartSales = new ArrayList<>(sales);
                statsChartExpenses = new ArrayList<>(expenses);

                renderStatisticsChartsTransaction(revEntries, revLabels, expEntries, expLabels, productSales);
            });
        }).start();
    }

    private void renderStatisticsChartsTransaction(List<Entry> revEntries, List<String> revLabels, List<Entry> expEntries, List<String> expLabels, Map<String, Integer> productSales) {
        if (expenseRevenueChart != null) {
            LineDataSet revSet = new LineDataSet(revEntries, "Checkout Revenue");
            revSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            revSet.setColor(Color.GREEN); revSet.setCircleColor(Color.GREEN);
            revSet.setLineWidth(2.5f); revSet.setDrawValues(false);
            revSet.setHighlightEnabled(true);
            revSet.setDrawHorizontalHighlightIndicator(false);
            revSet.setDrawVerticalHighlightIndicator(false);
            revSet.setCircleRadius(5f);
            revSet.setCircleHoleRadius(2.5f);
            revSet.setHighLightColor(Color.GREEN);
            revSet.setValueTextSize(8f);
            revSet.setDrawFilled(true);
            revSet.setFillAlpha(50);
            revSet.setFillColor(Color.GREEN);

            LineDataSet expSet = new LineDataSet(expEntries, "Expense Records");
            expSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            expSet.setColor(Color.RED); expSet.setCircleColor(Color.RED);
            expSet.setLineWidth(2.5f); expSet.setDrawValues(false);
            expSet.setHighlightEnabled(true);
            expSet.setDrawHorizontalHighlightIndicator(false);
            expSet.setDrawVerticalHighlightIndicator(false);
            expSet.setCircleRadius(5f);
            expSet.setCircleHoleRadius(2.5f);
            expSet.setHighLightColor(Color.RED);
            expSet.setValueTextSize(8f);
            expSet.setDrawFilled(true);
            expSet.setFillAlpha(50);
            expSet.setFillColor(Color.RED);

            LineData lineData = new LineData(revSet, expSet);
            expenseRevenueChart.setData(lineData);
            
            // XAxis labels: Use the longer list of labels to ensure coverage
            List<String> combinedLabels = new ArrayList<>();
            int maxSize = Math.max(revLabels.size(), expLabels.size());
            List<String> sourceLabels = (revLabels.size() >= expLabels.size()) ? revLabels : expLabels;
            
            for (int i = 0; i < maxSize; i++) {
                combinedLabels.add(sourceLabels.get(i));
            }
            
            expenseRevenueChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(combinedLabels));
            expenseRevenueChart.getXAxis().setGranularity(1f);
            expenseRevenueChart.getXAxis().setGranularityEnabled(true);
            expenseRevenueChart.getXAxis().setLabelCount(6);
            expenseRevenueChart.invalidate();

            // Enable horizontal scrolling
            expenseRevenueChart.setVisibleXRangeMaximum(6); // Show 6 entries for better spacing
            if (maxSize > 6) {
                expenseRevenueChart.moveViewToX(maxSize - 1); // Scroll to the end
            }
        }

        if (topProductsChart != null) {
            List<Map.Entry<String, Integer>> list = new ArrayList<>(productSales.entrySet());
            Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int count = Math.min(list.size(), 5);
            for (int i = 0; i < count; i++) {
                entries.add(new BarEntry(i, list.get(i).getValue()));
                labels.add(list.get(i).getKey());
            }
            BarDataSet dataSet = new BarDataSet(entries, "Top 5 Products");
            dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
            topProductsChart.setData(new BarData(dataSet));
            topProductsChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            topProductsChart.invalidate();
        }
    }

    private void switchContent(View view, String title) {
        if (view == null) return;

        if (cashierContent != null) cashierContent.setVisibility(View.GONE);
        if (dashboardContent != null) dashboardContent.setVisibility(View.GONE);
        if (reportsContent != null) reportsContent.setVisibility(View.GONE);
        if (salesHistoryContent != null) salesHistoryContent.setVisibility(View.GONE);
        if (cartContent != null) cartContent.setVisibility(View.GONE);
        if (statisticsContent != null) statisticsContent.setVisibility(View.GONE);
        if (reservations_content != null) reservations_content.setVisibility(View.GONE);
        if (debts_content != null) debts_content.setVisibility(View.GONE);
        if (settingsContent != null) settingsContent.setVisibility(View.GONE);
        if (productsContent != null) productsContent.setVisibility(View.GONE);

        view.setVisibility(View.VISIBLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Green POS System");
        }

        if (btnViewCart != null) {
            btnViewCart.setVisibility(view == cashierContent ? View.VISIBLE : View.GONE);
        }

        // Ensure the toggle bar button (hamburger menu) is not lost
        if (toggle != null) {
            if (view == cartContent) {
                // For Cart, we show a back button to Cashier
                toggle.setDrawerIndicatorEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toggle.syncState();
                toolbar.setNavigationOnClickListener(v -> switchContent(cashierContent, "Green POS System"));
            } else {
                // For all other menu items, keep the hamburger toggle visible
                toggle.setDrawerIndicatorEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                toggle.syncState();
                toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
            }
        }

        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(ContextCompat.getColor(this, R.color.primary_green));
        }
    }

    private void updateCheckoutButton() {
        if (btnCheckout != null) btnCheckout.setEnabled(!cart.isEmpty());
        if (btnCheckout2 != null) btnCheckout2.setEnabled(!cart.isEmpty());
    }

    private void showSalesHistoryPage() {
        switchContent(salesHistoryContent, "Sales History");
        new Thread(() -> {
            List<Sale> sales;
            if ("Staff".equals(userRole)) {
                sales = db.saleDao().getStaffSalesSync(userId);
            } else {
                sales = db.saleDao().getAllSalesSync(dataOwnerId);
            }
            runOnUiThread(() -> saleAdapter.updateList(sales));
        }).start();
    }

    private void refreshProductList() {
        new Thread(() -> {
            List<Product> products = db.productDao().getAllProductsSync(dataOwnerId);
            runOnUiThread(() -> {
                adapter.updateList(products);
                updateCartQuantitiesInAdapter();
            });
        }).start();
    }

    private void exportProductsToExcel() {
        createDocumentLauncher.launch("Products_Export.xlsx");
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
                    android.content.Intent intent = new android.content.Intent(POSActivity.this, com.example.bluepos.LoginActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showReservationsPage() {
        switchContent(reservations_content, "Reservations");
        refreshReservations();
    }

    private void showDebtsPage() {
        switchContent(debts_content, "Debts");
        refreshDebts();
    }

    private void setupDebtAdapter() {
        debtAdapter = new DebtAdapter(debts, new DebtAdapter.OnDebtActionListener() {
            @Override
            public void onPay(Debt debt) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Confirm Payment")
                        .setMessage("Mark this debt as paid?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            new Thread(() -> {
                                debt.status = "Paid";
                                String currentUserName = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "Unknown");
                                Sale sale = new Sale(debt.amount, debt.amount, 0, System.currentTimeMillis(), "Debt Payment: " + debt.productName, userId, currentUserName, dataOwnerId);
                                long saleId = db.saleDao().insert(sale);
                                debt.associatedSaleId = (int) saleId;
                                db.debtDao().update(debt);
                                runOnUiThread(POSActivity.this::refreshDebts);
                            }).start();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

            @Override
            public void onDelete(Debt debt) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Delete Debt")
                        .setMessage("Are you sure you want to delete this record?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            verifyPasswordAndExecute(() -> {
                                new Thread(() -> {
                                    db.debtDao().delete(debt);
                                    runOnUiThread(POSActivity.this::refreshDebts);
                                }).start();
                            });
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

            @Override
            public void onUndo(Debt debt) {
                new Thread(() -> {
                    debt.status = "Unpaid";
                    if (debt.associatedSaleId != -1) {
                        db.saleDao().deleteById(debt.associatedSaleId);
                        debt.associatedSaleId = -1;
                    }
                    db.debtDao().update(debt);
                    runOnUiThread(POSActivity.this::refreshDebts);
                }).start();
            }
        });
        rvDebts.setAdapter(debtAdapter);
    }

    private void refreshDebts() {
        new Thread(() -> {
            List<Debt> allDebts = db.debtDao().getAllDebtsSync(userId);
            runOnUiThread(() -> {
                debts.clear();
                debts.addAll(allDebts);
                if (etSearchDebts != null && !etSearchDebts.getText().toString().isEmpty()) {
                    filterDebts(etSearchDebts.getText().toString());
                } else {
                    debtAdapter.setDebts(new ArrayList<>(debts));
                    debtAdapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void showAddDebtDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_debt, null);
        EditText etName = dialogView.findViewById(R.id.etDebtCustomerName);
        AutoCompleteTextView autoProduct = dialogView.findViewById(R.id.autoCompleteDebtProduct);
        EditText etQty = dialogView.findViewById(R.id.etDebtQuantity);
        TextView tvAmountDisplay = dialogView.findViewById(R.id.tvDebtAmountDisplay);

        final List<Product>[] productsWrapper = (List<Product>[]) new List[1];
        final double[] currentTotal = {0.0};

        new Thread(() -> {
            productsWrapper[0] = db.productDao().getAllProductsSync(dataOwnerId);
            List<String> productNames = new ArrayList<>();
            for (Product p : productsWrapper[0]) productNames.add(p.name);

            runOnUiThread(() -> {
                android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, productNames);
                autoProduct.setAdapter(adapter);
                autoProduct.setThreshold(0);
                autoProduct.setOnClickListener(v -> autoProduct.showDropDown());
                autoProduct.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) autoProduct.showDropDown();
                });

                autoProduct.setOnItemClickListener((parent, view, position, id) -> {
                    if (etQty.getText().toString().isEmpty() || etQty.getText().toString().equals("0")) {
                        etQty.setText("1");
                    }
                    currentTotal[0] = calculateDebtAmount(autoProduct, etQty, tvAmountDisplay, productsWrapper[0]);
                });
            });
        }).start();

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                currentTotal[0] = calculateDebtAmount(autoProduct, etQty, tvAmountDisplay, productsWrapper[0]);
            }
        };
        etQty.addTextChangedListener(watcher);
        autoProduct.addTextChangedListener(watcher);

        new AlertDialog.Builder(this)
                .setTitle("Add New Debt")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String product = autoProduct.getText().toString();
                    String qtyStr = etQty.getText().toString();
                    int qty = Integer.parseInt(qtyStr.isEmpty() ? "0" : qtyStr);

                    if (name.isEmpty() || product.isEmpty() || qty <= 0 || currentTotal[0] <= 0) {
                        Toast.makeText(this, "Please fill required fields and valid quantity/product", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        Debt debt = new Debt(name, product, qty, currentTotal[0], System.currentTimeMillis(), "Unpaid", "", dataOwnerId);
                        db.debtDao().insert(debt);
                        runOnUiThread(POSActivity.this::refreshDebts);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private double calculateDebtAmount(AutoCompleteTextView autoProduct, EditText etQty, TextView tvAmountDisplay, List<Product> products) {
        if (products == null) return 0.0;
        String selectedName = autoProduct.getText().toString();
        String qtyStr = etQty.getText().toString();
        int qty = Integer.parseInt(qtyStr.isEmpty() ? "0" : qtyStr);
        
        for (Product p : products) {
            if (p.name.equals(selectedName)) {
                double total = p.price * qty;
                tvAmountDisplay.setText(String.format("₱%.2f", total));
                return total;
            }
        }
        tvAmountDisplay.setText("₱0.00");
        return 0.0;
    }

    private void setupReservationAdapter() {
        reservationAdapter = new ReservationAdapter(reservations, new ReservationAdapter.OnReservationActionListener() {
            @Override
            public void onComplete(Reservation reservation) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Complete Reservation")
                        .setMessage("Complete this reservation and create a sale?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            new Thread(() -> {
                                reservation.status = "Completed";
                                String currentUserName = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "Unknown");
                                Sale sale = new Sale(reservation.totalAmount, reservation.totalAmount, 0, System.currentTimeMillis(), "Reservation: " + reservation.itemsSummary, userId, currentUserName, dataOwnerId);
                                long saleId = db.saleDao().insert(sale);
                                reservation.associatedSaleId = (int) saleId;
                                db.reservationDao().update(reservation);
                                runOnUiThread(POSActivity.this::refreshReservations);
                            }).start();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

            @Override
            public void onCancel(Reservation reservation) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Cancel Reservation")
                        .setMessage("Are you sure you want to delete this reservation permanently?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            verifyPasswordAndExecute(() -> {
                                new Thread(() -> {
                                    db.reservationDao().delete(reservation);
                                    runOnUiThread(POSActivity.this::refreshDebts);
                                }).start();
                            });
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

            @Override
            public void onUndo(Reservation reservation) {
                new Thread(() -> {
                    if ("Completed".equals(reservation.status) && reservation.associatedSaleId != null) {
                        db.saleDao().deleteById(reservation.associatedSaleId);
                        reservation.associatedSaleId = null;
                    }
                    reservation.status = "Pending";
                    db.reservationDao().update(reservation);
                    runOnUiThread(POSActivity.this::refreshReservations);
                }).start();
            }

            @Override
            public void onItemClick(Reservation reservation) {
                showEditReservationDialog(reservation);
            }
        });
        rvReservations.setAdapter(reservationAdapter);
    }

    private void refreshReservations() {
        new Thread(() -> {
            List<Reservation> allReservations = db.reservationDao().getAllReservationsSync(userId);
            runOnUiThread(() -> {
                reservations.clear();
                reservations.addAll(allReservations);
                reservationAdapter.notifyDataSetChanged();
                if (etSearchReservations != null && !etSearchReservations.getText().toString().isEmpty()) {
                    filterReservations(etSearchReservations.getText().toString());
                }
            });
        }).start();
    }

    private void filterReservations(String query) {
        List<Reservation> filtered = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.customerName.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(r);
            }
        }
        reservationAdapter.setReservations(filtered);
    }

    private void filterDebts(String query) {
        List<Debt> filtered = new ArrayList<>();
        for (Debt d : debts) {
            if (d.customerName.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(d);
            }
        }
        debtAdapter.setDebts(filtered);
    }

    private void showManageProductsPage() {
        // Prompt for password before showing content
        EditText etPassword = new EditText(this);
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle("Security Check")
                .setMessage("Please enter owner password to access inventory:")
                .setView(etPassword)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String password = etPassword.getText().toString();
                    new Thread(() -> {
                        User user = db.userDao().getUserById(userId);
                        boolean authorized = user != null && password.equals(user.password);
                        runOnUiThread(() -> {
                            if (authorized) {
                                switchContent(productsContent, "Inventory Management");
                                refreshManageProducts();
                            } else {
                                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshManageProducts() {
        new Thread(() -> {
            List<Product> products;
            if (selectedCategoryManage.equals("All Categories")) {
                products = db.productDao().getAllProductsSync(dataOwnerId);
            } else {
                products = db.productDao().getProductsByCategory(dataOwnerId, selectedCategoryManage);
            }
            runOnUiThread(() -> manageAdapter.updateList(products));
        }).start();
    }

    private void setupSearchManage() {
        searchViewManage.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearchManage(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearchManage(newText);
                return true;
            }
        });
    }

    private void performSearchManage(String query) {
        new Thread(() -> {
            List<Product> filtered;
            if (selectedCategoryManage.equals("All Categories")) {
                filtered = db.productDao().searchProducts(dataOwnerId, "%" + query + "%");
            } else {
                filtered = db.productDao().searchProductsWithCategory(dataOwnerId, selectedCategoryManage, "%" + query + "%");
            }
            runOnUiThread(() -> manageAdapter.updateList(filtered));
        }).start();
    }

    private String selectedCategoryManage = "All Categories";
    private void setupCategoryFilterManage() {
        new Thread(() -> {
            List<String> categories = db.productDao().getUniqueCategories(dataOwnerId);
            List<String> filterOptions = new ArrayList<>();
            filterOptions.add("All Categories");
            filterOptions.addAll(categories);

            runOnUiThread(() -> {
                android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filterOptions);
                autoCompleteCategoryManage.setAdapter(adapter);
                
                // Add TextWatcher to detect typing/selection for real-time filtering
                autoCompleteCategoryManage.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        selectedCategoryManage = s.toString();
                        if (selectedCategoryManage.isEmpty()) {
                            selectedCategoryManage = "All Categories";
                        }
                        refreshManageProductsWithCategory();
                    }
                });

                autoCompleteCategoryManage.setOnItemClickListener((parent, view, position, id) -> {
                    selectedCategoryManage = filterOptions.get(position);
                    searchViewManage.setQuery("", false); // Clear search when category changes
                    refreshManageProductsWithCategory();
                });
            });
        }).start();
    }

    private void refreshManageProductsWithCategory() {
        new Thread(() -> {
            List<Product> products;
            if (selectedCategoryManage.equals("All Categories") || selectedCategoryManage.trim().isEmpty()) {
                products = db.productDao().getAllProductsSync(dataOwnerId);
            } else {
                // Use LIKE for partial matching if typing manually
                products = db.productDao().getProductsByCategory(dataOwnerId, "%" + selectedCategoryManage + "%");
            }
            runOnUiThread(() -> manageAdapter.updateList(products));
        }).start();
    }

    private void promptForInventoryPassword(Runnable onSuccess) {
        EditText etPassword = new EditText(this);
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        new AlertDialog.Builder(this)
                .setTitle("Inventory Security")
                .setMessage("Please enter password to modify stock:")
                .setView(etPassword)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = etPassword.getText().toString();
                    new Thread(() -> {
                        User user = db.userDao().getUserById(userId);
                        if (user != null && user.password.equals(password)) {
                            runOnUiThread(onSuccess);
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProductDialog(Product product) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_product, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etCost = dialogView.findViewById(R.id.etCost);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etLimit = dialogView.findViewById(R.id.etLimit);
        AutoCompleteTextView autoCompleteCategory = dialogView.findViewById(R.id.autoCompleteDialogCategory);
        android.widget.CheckBox cbHasExpiration = dialogView.findViewById(R.id.cbHasExpiration);
        EditText etExpirationDate = dialogView.findViewById(R.id.etExpirationDate);
        View tilExpirationDate = dialogView.findViewById(R.id.tilExpirationDate);

        final java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

        cbHasExpiration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilExpirationDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        etExpirationDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                cal.set(java.util.Calendar.YEAR, year);
                cal.set(java.util.Calendar.MONTH, month);
                cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                etExpirationDate.setText(sdf.format(cal.getTime()));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // Fetch existing categories from DB
        new Thread(() -> {
            List<String> dbCategories = db.productDao().getUniqueCategories(userId);
            runOnUiThread(() -> {
                List<String> categoryList = new ArrayList<>();
                for (String cat : dbCategories) {
                    if (cat != null && !cat.isEmpty() && !cat.equalsIgnoreCase("General")) {
                        categoryList.add(cat);
                    }
                }
                categoryList.add(0, "General");

                android.widget.ArrayAdapter<String> catAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryList);
                autoCompleteCategory.setAdapter(catAdapter);

                autoCompleteCategory.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        autoCompleteCategory.showDropDown();
                    }
                });
                autoCompleteCategory.setOnClickListener(v -> autoCompleteCategory.showDropDown());

                if (product != null) {
                    autoCompleteCategory.setText(product.category, false);
                    cbHasExpiration.setChecked(product.hasExpiration);
                    if (product.hasExpiration) {
                        tilExpirationDate.setVisibility(View.VISIBLE);
                        if (product.expirationDate != null) {
                            cal.setTimeInMillis(product.expirationDate);
                            etExpirationDate.setText(sdf.format(cal.getTime()));
                        }
                    }
                } else {
                    autoCompleteCategory.setText("General", false);
                }
            });
        }).start();

        String title = "Add New Product";
        if (product != null) {
            title = "Edit Product";
            etName.setText(product.name);
            etCost.setText(String.valueOf(product.cost));
            etPrice.setText(String.valueOf(product.price));
            etQuantity.setText(String.valueOf(product.stock));
            etLimit.setText(String.valueOf(product.minStock));
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    double cost = Double.parseDouble(etCost.getText().toString().isEmpty() ? "0" : etCost.getText().toString());
                    double price = Double.parseDouble(etPrice.getText().toString().isEmpty() ? "0" : etPrice.getText().toString());
                    int qty = Integer.parseInt(etQuantity.getText().toString().isEmpty() ? "0" : etQuantity.getText().toString());
                    int limit = Integer.parseInt(etLimit.getText().toString().isEmpty() ? "0" : etLimit.getText().toString());
                    String category = autoCompleteCategory.getText().toString().trim();
                    if (category.isEmpty()) category = "General";

                    boolean hasExp = cbHasExpiration.isChecked();
                    Long expDate = null;
                    if (hasExp && !etExpirationDate.getText().toString().isEmpty()) {
                        try {
                            expDate = sdf.parse(etExpirationDate.getText().toString()).getTime();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    final String finalCategory = category;
                    final Long finalExpDate = expDate;
                    new Thread(() -> {
                        if (product == null) {
                            Product p = new Product(name, cost, price, qty, limit, finalCategory, userId, hasExp, finalExpDate);
                            db.productDao().insert(p);
                        } else {
                            product.name = name;
                            product.cost = cost;
                            product.price = price;
                            product.stock = qty;
                            product.minStock = limit;
                            product.category = finalCategory;
                            product.hasExpiration = hasExp;
                            product.expirationDate = finalExpDate;
                            db.productDao().update(product);
                        }
                        runOnUiThread(() -> {
                            refreshManageProducts();
                            refreshProductList();
                            updateNotificationBadge();
                            Toast.makeText(this, "Product Saved under " + finalCategory, Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveExpenseManage() {
        String title = etExpenseTitleManage.getText().toString();
        String amountStr = etExpenseAmountManage.getText().toString();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        new Thread(() -> {
            Expense expense = new Expense(title, amount, System.currentTimeMillis(), userId);
            db.expenseDao().insert(expense);
            runOnUiThread(() -> {
                etExpenseTitleManage.setText("");
                etExpenseAmountManage.setText("");
                updateExpenseUIManage();
                Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void updateExpenseUIManage() {
        new Thread(() -> {
            long today = getStartOfDay();
            double todayExp = db.expenseDao().getExpensesSince(today, userId);
            double totalExp = db.expenseDao().getTotalExpenses(userId);
            double totalRev = db.saleDao().getTotalRevenue(userId);
            List<Expense> recent = db.expenseDao().getAllExpensesSync(userId);

            runOnUiThread(() -> {
                if (tvExpTodayManage != null) tvExpTodayManage.setText(String.format("₱%.2f", todayExp));
                if (tvExpTotalManage != null) tvExpTotalManage.setText(String.format("₱%.2f", totalExp));
                if (tvExpRevenueManage != null) tvExpRevenueManage.setText(String.format("₱%.2f", totalRev));
                if (tvExpNetManage != null) tvExpNetManage.setText(String.format("₱%.2f", totalRev - totalExp));
                manageExpenseAdapter.updateList(recent);
            });
        }).start();
    }

    private void onEditExpenseManage(Expense expense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Using an inline layout construction because dialog_add_expense.xml is missing
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Expense Title");
        etTitle.setText(expense.title);
        layout.addView(etTitle);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Amount");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setText(String.valueOf(expense.amount));
        layout.addView(etAmount);

        builder.setView(layout);
        builder.setTitle("Edit Expense");

        builder.setPositiveButton("Save", (dialog, which) -> {
            String title = etTitle.getText().toString();
            String amountStr = etAmount.getText().toString();

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            expense.title = title;
            expense.amount = Double.parseDouble(amountStr);

            new Thread(() -> {
                db.expenseDao().update(expense);
                runOnUiThread(POSActivity.this::updateExpenseUIManage);
            }).start();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private Product selectedProductForRes = null;

    private void showAddReservationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reservation, null);
        EditText etName = dialogView.findViewById(R.id.etResCustomerName);
        EditText etContact = dialogView.findViewById(R.id.etResContact);
        AutoCompleteTextView autoProduct = dialogView.findViewById(R.id.autoCompleteResProduct);
        EditText etQty = dialogView.findViewById(R.id.etResQuantity);
        TextView tvAmountDisplay = dialogView.findViewById(R.id.tvResAmountDisplay);

        final List<Product>[] allProducts = (List<Product>[]) new List[1];
        final double[] currentTotal = {0.0};

        new Thread(() -> {
            allProducts[0] = db.productDao().getAllProductsSync(dataOwnerId);
            List<String> productNames = new ArrayList<>();
            for (Product p : allProducts[0]) productNames.add(p.name);

            runOnUiThread(() -> {
                android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, productNames);
                autoProduct.setAdapter(adapter);
                autoProduct.setThreshold(0);
                autoProduct.setOnClickListener(v -> autoProduct.showDropDown());
                autoProduct.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) autoProduct.showDropDown();
                });
                autoProduct.setOnItemClickListener((parent, view, position, id) -> {
                    if (etQty.getText().toString().isEmpty() || etQty.getText().toString().equals("0")) {
                        etQty.setText("1");
                    }
                    currentTotal[0] = calculateResAmount(autoProduct, etQty, tvAmountDisplay, allProducts[0]);
                });
            });
        }).start();

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                currentTotal[0] = calculateResAmount(autoProduct, etQty, tvAmountDisplay, allProducts[0]);
            }
        };
        etQty.addTextChangedListener(watcher);
        autoProduct.addTextChangedListener(watcher);

        new AlertDialog.Builder(this)
                .setTitle("Add New Reservation")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String contact = etContact.getText().toString();
                    String productName = autoProduct.getText().toString();
                    String qtyStr = etQty.getText().toString();

                    if (name.isEmpty() || productName.isEmpty() || qtyStr.isEmpty() || currentTotal[0] <= 0) {
                        Toast.makeText(this, "Please fill required fields and valid quantity/product", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int qty = Integer.parseInt(qtyStr);
                    String itemsSummary = productName + " x" + qty;

                    new Thread(() -> {
                        Reservation res = new Reservation(name, contact, itemsSummary, currentTotal[0], System.currentTimeMillis(), "Pending", dataOwnerId);
                        db.reservationDao().insert(res);
                        runOnUiThread(POSActivity.this::refreshReservations);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private double calculateResAmount(AutoCompleteTextView autoProduct, EditText etQty, TextView tvAmountDisplay, List<Product> products) {
        if (products == null) return 0.0;
        String selectedName = autoProduct.getText().toString();
        String qtyStr = etQty.getText().toString();
        int qty = Integer.parseInt(qtyStr.isEmpty() ? "0" : qtyStr);

        for (Product p : products) {
            if (p.name.equals(selectedName)) {
                double total = p.price * qty;
                tvAmountDisplay.setText(String.format("₱%.2f", total));
                return total;
            }
        }
        tvAmountDisplay.setText("₱0.00");
        return 0.0;
    }

    private void showEditReservationDialog(Reservation reservation) {
        if (!"Pending".equalsIgnoreCase(reservation.status)) {
            Toast.makeText(this, "Only pending reservations can be edited", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reservation, null);
        EditText etName = dialogView.findViewById(R.id.etResCustomerName);
        EditText etContact = dialogView.findViewById(R.id.etResContact);
        AutoCompleteTextView autoProduct = dialogView.findViewById(R.id.autoCompleteResProduct);
        EditText etQty = dialogView.findViewById(R.id.etResQuantity);
        TextView tvAmountDisplay = dialogView.findViewById(R.id.tvResAmountDisplay);

        etName.setText(reservation.customerName);
        etContact.setText(reservation.contactInfo);

        String existingProduct = "";
        int existingQty = 1;
        if (reservation.itemsSummary != null && reservation.itemsSummary.contains(" x")) {
            int lastIndex = reservation.itemsSummary.lastIndexOf(" x");
            existingProduct = reservation.itemsSummary.substring(0, lastIndex);
            try {
                existingQty = Integer.parseInt(reservation.itemsSummary.substring(lastIndex + 2));
            } catch (Exception ignored) {}
        } else {
            existingProduct = reservation.itemsSummary;
        }

        autoProduct.setText(existingProduct);
        etQty.setText(String.valueOf(existingQty));
        tvAmountDisplay.setText(String.format("₱%.2f", reservation.totalAmount));

        final List<Product>[] allProducts = (List<Product>[]) new List[1];
        final double[] currentTotal = {reservation.totalAmount};

        new Thread(() -> {
            allProducts[0] = db.productDao().getAllProductsSync(dataOwnerId);
            List<String> productNames = new ArrayList<>();
            for (Product p : allProducts[0]) productNames.add(p.name);

            runOnUiThread(() -> {
                android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, productNames);
                autoProduct.setAdapter(adapter);
                autoProduct.setThreshold(0);

                android.text.TextWatcher watcher = new android.text.TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(android.text.Editable s) {
                        currentTotal[0] = calculateResAmount(autoProduct, etQty, tvAmountDisplay, allProducts[0]);
                    }
                };
                etQty.addTextChangedListener(watcher);
                autoProduct.addTextChangedListener(watcher);

                autoProduct.setOnItemClickListener((parent, view, position, id) -> {
                    currentTotal[0] = calculateResAmount(autoProduct, etQty, tvAmountDisplay, allProducts[0]);
                });
            });
        }).start();

        new AlertDialog.Builder(this)
                .setTitle("Edit Reservation")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String contact = etContact.getText().toString();
                    String productName = autoProduct.getText().toString();
                    String qtyStr = etQty.getText().toString();

                    if (name.isEmpty() || productName.isEmpty() || qtyStr.isEmpty() || currentTotal[0] <= 0) {
                        Toast.makeText(this, "Please fill required fields and valid quantity/product", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int qty = Integer.parseInt(qtyStr);
                    reservation.customerName = name;
                    reservation.contactInfo = contact;
                    reservation.itemsSummary = productName + " x" + qty;
                    reservation.totalAmount = currentTotal[0];

                    new Thread(() -> {
                        db.reservationDao().update(reservation);
                        runOnUiThread(POSActivity.this::refreshReservations);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Reservation")
                            .setMessage("Are you sure you want to delete this reservation permanently?")
                            .setPositiveButton("Delete", (dialog1, which1) -> {
                                verifyPasswordAndExecute(() -> {
                                    new Thread(() -> {
                                        db.reservationDao().delete(reservation);
                                        runOnUiThread(POSActivity.this::refreshReservations);
                                    }).start();
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }

    private void showProductSelectionDialog(java.util.function.Consumer<Product> onProductSelected) {
        View view = getLayoutInflater().inflate(R.layout.dialog_select_product, null);
        EditText etSearch = view.findViewById(R.id.etSearchProduct);
        RecyclerView rvSelection = view.findViewById(R.id.rvProductSelection);
        rvSelection.setLayoutManager(new LinearLayoutManager(this));

        List<Product> fullList = new ArrayList<>();
        List<Product> filteredList = new ArrayList<>();

        class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {
            @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup p, int t) {
                return new ViewHolder(getLayoutInflater().inflate(R.layout.item_product_selection, p, false));
            }
            @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
                Product prod = filteredList.get(p);
                h.name.setText(prod.name);
                h.info.setText(String.format("Stock: %d | Price: ₱%.2f", prod.stock, prod.price));
                h.itemView.setOnClickListener(v -> {
                    onProductSelected.accept(prod);
                    ((AlertDialog) view.getTag()).dismiss();
                });
            }
            @Override public int getItemCount() { return filteredList.size(); }
            class ViewHolder extends RecyclerView.ViewHolder {
                TextView name, info;
                ViewHolder(View v) { super(v); name = v.findViewById(R.id.tvProductName); info = v.findViewById(R.id.tvProductInfo); }
            }
        }

        SelectionAdapter adapter = new SelectionAdapter();
        rvSelection.setAdapter(adapter);

        new Thread(() -> {
            fullList.addAll(db.productDao().getAllProductsSync(userId));
            runOnUiThread(() -> {
                filteredList.addAll(fullList);
                adapter.notifyDataSetChanged();
            });
        }).start();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filteredList.clear();
                String query = s.toString().toLowerCase();
                for (Product p : fullList) {
                    if (p.name.toLowerCase().contains(query)) filteredList.add(p);
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Product")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();
        view.setTag(dialog);
        dialog.show();
    }

    private void showResetDataDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        view.findViewById(R.id.etName).setVisibility(android.view.View.GONE);
        view.findViewById(R.id.etEmail).setVisibility(android.view.View.GONE);
        view.findViewById(R.id.etPassword).setVisibility(android.view.View.GONE);
        ((com.google.android.material.textfield.TextInputLayout)view.findViewById(R.id.tilOldPassword)).setHint("Enter Password to Reset");
        com.google.android.material.textfield.TextInputEditText etConfirmPassword = view.findViewById(R.id.etOldPassword);

        new AlertDialog.Builder(this)
                .setTitle("Reset My Data")
                .setMessage("This will permanently delete ALL your products, sales, expenses, and reservations. Are you sure?")
                .setView(view)
                .setPositiveButton("Reset Now", (dialog, which) -> {
                    String password = etConfirmPassword.getText().toString();
                    new Thread(() -> {
                        User user = db.userDao().getUserById(userId);
                        if (user != null && user.password.equals(password)) {
                            // Delete data
                            db.productDao().deleteAllByUserId(user.id);
                            db.saleDao().deleteAllByUserId(user.id);
                            db.expenseDao().deleteAllByUserId(user.id);
                            db.reservationDao().deleteAllByUserId(user.id);
                            db.debtDao().deleteAllByUserId(user.id);

                            runOnUiThread(() -> {
                                refreshProductList();
                                updateNotificationBadge();
                                Toast.makeText(this, "All data reset successfully", Toast.LENGTH_SHORT).show();
                            });
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
        com.google.android.material.textfield.TextInputEditText etName = view.findViewById(R.id.etName);
        com.google.android.material.textfield.TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        com.google.android.material.textfield.TextInputEditText etOldPassword = view.findViewById(R.id.etOldPassword);
        com.google.android.material.textfield.TextInputEditText etPassword = view.findViewById(R.id.etPassword);

        new Thread(() -> {
            User user = db.userDao().getUserById(userId);
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
                        User user = db.userDao().getUserById(userId);
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

                            android.content.SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                            editor.putString("userEmail", email);
                            editor.putString("username", name);
                            editor.apply();

                            runOnUiThread(() -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void verifyPasswordAndExecute(Runnable action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Password");
        View view = getLayoutInflater().inflate(R.layout.dialog_verify_password, null);
        EditText etPassword = view.findViewById(R.id.etVerifyPassword);
        builder.setView(view);
        builder.setPositiveButton("Verify", (dialog, which) -> {
            String password = etPassword.getText().toString();
            new Thread(() -> {
                User user = db.userDao().getUserById(userId);
                if (user != null && user.password.equals(password)) {
                    runOnUiThread(action);
                } else {
                    runOnUiThread(() -> Toast.makeText(POSActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
            // Re-open DB after export
            db = AppDatabase.getDatabase(this);
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
                            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            User newUser = db.userDao().getUserByEmail(prefs.getString("userEmail", ""));
                            if (newUser != null) {
                                // Reassign all imported data to the current user
                                db.productDao().updateUserIdForAll(newUser.id);
                                db.saleDao().updateUserIdForAll(newUser.id);
                                db.expenseDao().updateUserIdForAll(newUser.id);
                                db.reservationDao().updateUserIdForAll(newUser.id);
                                db.debtDao().updateUserIdForAll(newUser.id);

                                android.content.SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("userId", newUser.id);
                                editor.apply();
                                userId = newUser.id;
                                runOnUiThread(() -> {
                                    refreshProductList();
                                    updateNotificationBadge();
                                    Toast.makeText(this, "Data imported and merged to your account.", Toast.LENGTH_LONG).show();
                                });
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

    private void saveExcelToUri(Uri uri) {
        new Thread(() -> {
            List<Product> products = db.productDao().getAllProductsSync(userId);
            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream out = getContentResolver().openOutputStream(uri)) {

                Sheet sheet = workbook.createSheet("Products");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Name", "Category", "Cost", "Price", "Stock", "Min Stock", "Expiration Date"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                int rowNum = 1;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                for (Product p : products) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(p.name);
                    row.createCell(1).setCellValue(p.category);
                    row.createCell(2).setCellValue(p.cost);
                    row.createCell(3).setCellValue(p.price);
                    row.createCell(4).setCellValue(p.stock);
                    row.createCell(5).setCellValue(p.minStock);
                    if (p.hasExpiration && p.expirationDate != null) {
                        row.createCell(6).setCellValue(sdf.format(new java.util.Date(p.expirationDate)));
                    } else {
                        row.createCell(6).setCellValue("N/A");
                    }
                }

                workbook.write(out);
                runOnUiThread(() -> Toast.makeText(this, "Products exported to Excel", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
