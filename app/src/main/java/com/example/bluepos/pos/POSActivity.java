package com.example.bluepos.pos;

import android.content.Intent;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationView;

import com.example.bluepos.R;
import com.example.bluepos.ProductsActivity;
import com.example.bluepos.SettingsActivity;
import com.example.bluepos.LoginActivity;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class POSActivity extends AppCompatActivity {

    private AppDatabase db;
    private ProductAdapter adapter;
    private List<CartItem> cart = new ArrayList<>();
    private double totalAmount = 0.0;
    private Button btnCheckout, btnCheckout2;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private EditText etAmountPaid, etSearch, etSearchCart;
    private TextView tvChange, tvCartTotal, tvCartTotal2;
    private RecyclerView rvCart, rvSalesHistory;
    private CartAdapter cartAdapter;
    private SaleAdapter saleAdapter;
    private View cashierContent, dashboardContent, reportsContent, salesHistoryContent, cartContent, statisticsContent, reservations_content, debts_content, settingsContent;
    private ImageButton btnViewCart;
    private RecyclerView rvReservations, rvDebts;
    private ReservationAdapter reservationAdapter;
    private DebtAdapter debtAdapter;
    private List<Reservation> reservations = new ArrayList<>();
    private List<Debt> debts = new ArrayList<>();

    private TextView tvNetIncome, tvTotalRevenue, tvTotalExpenses, tvTodaySales, tvTotalProducts, tvTotalSalesCount, tvItemsSold, tvLowStock;
    private TextView tvStatsTotalRevenue, tvStatsTotalExpenses, tvStatsNetIncome;
    private LineChart revenueChart, expenseRevenueChart;
    private HorizontalBarChart topProductsChart;
    private PieChart salesCategoryChart, stockCategoryChart;
    private TextView tvNotificationBadge;
    private View btnNotification;
    private Button btnExportProducts;

    private int userId;

    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            uri -> {
                if (uri != null) {
                    saveExcelToUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getInt("userId", -1);
        }

        if (userId == -1) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = AppDatabase.getDatabase(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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
            for (CartItem item : cart) {
                summary.append(item.product.name).append(" x").append(item.quantity).append(", ");
                
                // Update product stock
                item.product.stock -= item.quantity;
                db.productDao().update(item.product);
            }

            Sale sale = new Sale(totalAmount, paid, paid - totalAmount, System.currentTimeMillis(), summary.toString(), userId);
            db.saleDao().insert(sale);

            cart.clear();
            cartAdapter.notifyDataSetChanged();
            updateTotals();
            refreshProductList();
            updateNotificationBadge();
            Toast.makeText(this, "Checkout successful", Toast.LENGTH_SHORT).show();
            switchContent(cashierContent, "BluePOS System");
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

        rvDebts = findViewById(R.id.rvDebts);
        rvDebts.setLayoutManager(new LinearLayoutManager(this));
        setupDebtAdapter();

        findViewById(R.id.btnAddReservation).setOnClickListener(v -> showAddReservationDialog());
        findViewById(R.id.btnAddDebt).setOnClickListener(v -> showAddDebtDialog());

        initDashboardViews();
        setupChart();

        btnNotification = findViewById(R.id.btnNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnNotification.setOnClickListener(v -> showLowStockDialog());

        btnExportProducts = findViewById(R.id.btnExportProducts);
        if (btnExportProducts != null) {
            btnExportProducts.setOnClickListener(v -> exportProductsToExcel());
        }

        // Setup settings buttons
        findViewById(R.id.cardEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class).putExtra("ACTION", "EDIT_PROFILE"));
        });
        findViewById(R.id.cardExportData).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class).putExtra("ACTION", "EXPORT"));
        });
        findViewById(R.id.cardImportData).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class).putExtra("ACTION", "IMPORT"));
        });
        findViewById(R.id.cardResetData).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class).putExtra("ACTION", "RESET"));
        });

        // Handle TARGET_VIEW intent
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
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pos) {
                switchContent(cashierContent, "BluePOS System");
            } else if (id == R.id.nav_dashboard) {
                showDashboard();
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(POSActivity.this, ProductsActivity.class));
            } else if (id == R.id.nav_reports) {
                switchContent(reportsContent, "Reports");
            } else if (id == R.id.nav_sales_history) {
                showSalesHistoryPage();
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

        // Initialize with POS content
        switchContent(cashierContent, "BluePOS System");
        
        refreshProductList();
        updateNotificationBadge();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                if (cashierContent.getVisibility() == View.VISIBLE) {
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    switchContent(cashierContent, "BluePOS System");
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProductList();
        updateNotificationBadge();
        if (dashboardContent.getVisibility() == View.VISIBLE) updateDashboardData();
    }

    private void setupCartAdapter() {
        rvCart = findViewById(R.id.rvCart);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        tvCartTotal2 = findViewById(R.id.tvCartTotal2);
        etSearchCart = findViewById(R.id.etSearchCart);

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cart, new CartAdapter.OnCartActionListener() {
            @Override
            public void onIncrease(CartItem item) {
                if (item.quantity < item.product.stock) {
                    item.quantity++;
                    cartAdapter.notifyDataSetChanged();
                    updateTotals();
                } else {
                    Toast.makeText(POSActivity.this, "Not enough stock", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDecrease(CartItem item) {
                if (item.quantity > 1) {
                    item.quantity--;
                } else {
                    cart.remove(item);
                }
                cartAdapter.notifyDataSetChanged();
                updateTotals();
            }

            @Override
            public void onRemove(CartItem item) {
                cart.remove(item);
                cartAdapter.notifyDataSetChanged();
                updateTotals();
            }
        });
        rvCart.setAdapter(cartAdapter);

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

    private void filterCart(String query) {
        List<CartItem> filteredList = new ArrayList<>();
        for (CartItem item : cart) {
            if (item.product.name.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        cartAdapter.updateList(filteredList);
    }

    private void updateTotals() {
        totalAmount = 0;
        for (CartItem item : cart) {
            totalAmount += item.product.price * item.quantity;
        }
        String totalStr = String.format("Total: ₱%.2f", totalAmount);
        tvCartTotal.setText(totalStr);
        if (tvCartTotal2 != null) tvCartTotal2.setText(totalStr);
        updateCheckoutButton();
        calculateChange();
    }

    private void notifyAdapters() {
        adapter.notifyDataSetChanged();
        cartAdapter.notifyDataSetChanged();
    }

    private void calculateChange() {
        double paid = getPaidAmount();
        double change = paid - totalAmount;
        tvChange.setText(String.format("Change: ₱%.2f", Math.max(0, change)));
    }

    private double getPaidAmount() {
        String s = etAmountPaid.getText().toString();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void filterProducts(String query) {
        List<Product> filtered = db.productDao().searchProducts(userId, query);
        adapter.updateList(filtered);
    }

    private void onAddToCart(Product product) {
        if (product.stock <= 0) {
            Toast.makeText(this, "Out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        for (CartItem item : cart) {
            if (item.product.id == product.id) {
                if (item.quantity < product.stock) {
                    item.quantity++;
                    cartAdapter.notifyDataSetChanged();
                    updateTotals();
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Max stock reached", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }

        cart.add(new CartItem(product, 1));
        cartAdapter.notifyDataSetChanged();
        updateTotals();
        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
    }

    private void showDashboard() {
        switchContent(dashboardContent, "Dashboard");
        updateDashboardData();
    }

    private void showStatistics() {
        switchContent(statisticsContent, "Statistics");
        updateStatisticsData();
    }

    private void initDashboardViews() {
        tvNetIncome = findViewById(R.id.tvNetIncome);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvTotalSalesCount = findViewById(R.id.tvTotalSalesCount);
        tvItemsSold = findViewById(R.id.tvItemsSold);
        tvLowStock = findViewById(R.id.tvLowStock);
        revenueChart = findViewById(R.id.revenueChart);
        salesCategoryChart = findViewById(R.id.salesCategoryChart);
        stockCategoryChart = findViewById(R.id.stockCategoryChart);

        tvStatsTotalRevenue = findViewById(R.id.tvStatsTotalRevenue);
        tvStatsTotalExpenses = findViewById(R.id.tvStatsTotalExpenses);
        tvStatsNetIncome = findViewById(R.id.tvStatsNetIncome);
        expenseRevenueChart = findViewById(R.id.expenseRevenueChart);
        topProductsChart = findViewById(R.id.topProductsChart);
    }

    private void setupChart() {
        revenueChart.getDescription().setEnabled(false);
        revenueChart.setDrawGridBackground(false);
        revenueChart.getLegend().setEnabled(true);

        XAxis xAxis = revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        revenueChart.getAxisRight().setEnabled(false);

        // Stats Chart
        expenseRevenueChart.getDescription().setEnabled(false);
        expenseRevenueChart.getLegend().setEnabled(true);
        XAxis exXAxis = expenseRevenueChart.getXAxis();
        exXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        exXAxis.setDrawGridLines(false);
        expenseRevenueChart.getAxisRight().setEnabled(false);

        // Top Products Chart
        topProductsChart.getDescription().setEnabled(false);
        topProductsChart.setDrawGridBackground(false);
        XAxis topXAxis = topProductsChart.getXAxis();
        topXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        topXAxis.setDrawGridLines(false);
        topProductsChart.getAxisRight().setEnabled(false);
    }

    private void setupPieChart(PieChart pieChart) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.R.color.white);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelColor(android.R.color.black);
        pieChart.setEntryLabelTextSize(12f);

        pieChart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                PieEntry pe = (PieEntry) e;
                String category = pe.getLabel();
                float value = pe.getValue();
                String unit = pieChart == salesCategoryChart ? "₱" : "items";
                showCategoryDetailDialog(category, value, unit);
            }

            @Override
            public void onNothingSelected() {}
        });
    }

    private void showCategoryDetailDialog(String category, float value, String unit) {
        List<Product> products = db.productDao().getAll(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("Details for category: ").append(category).append("\n\n");
        
        for (Product p : products) {
            if (p.category.equals(category)) {
                sb.append("- ").append(p.name).append(": ")
                  .append(unit.equals("₱") ? "" : p.stock + " in stock")
                  .append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Category: " + category)
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void updateDashboardData() {
        List<Sale> allSales = db.saleDao().getAllSales(userId);
        List<Product> allProducts = db.productDao().getAll(userId);
        List<Expense> allExpenses = db.expenseDao().getAllExpenses(userId);

        double totalRevenue = 0;
        double todayRevenue = 0;
        int itemsSold = 0;
        long startOfDay = getStartOfDay();

        for (Sale s : allSales) {
            totalRevenue += s.totalAmount;
            if (s.timestamp >= startOfDay) {
                todayRevenue += s.totalAmount;
            }
            // Parse summary "Item A x2, Item B x1"
            String[] parts = s.itemsSummary.split(", ");
            for (String part : parts) {
                if (part.contains(" x")) {
                    try {
                        String q = part.substring(part.lastIndexOf(" x") + 2).trim();
                        // Remove any trailing commas or spaces that might have been split incorrectly
                        if (q.endsWith(",")) q = q.substring(0, q.length() - 1);
                        itemsSold += Integer.parseInt(q);
                    } catch (Exception ignored) {}
                }
            }
        }

        double totalExpense = 0;
        for (Expense e : allExpenses) totalExpense += e.amount;

        tvTotalRevenue.setText(String.format("₱%.2f", totalRevenue));
        tvTotalExpenses.setText(String.format("₱%.2f", totalExpense));
        tvNetIncome.setText(String.format("₱%.2f", totalRevenue - totalExpense));
        tvTodaySales.setText(String.format("₱%.2f", todayRevenue));
        tvTotalProducts.setText(String.valueOf(allProducts.size()));
        tvTotalSalesCount.setText(String.valueOf(allSales.size()));
        tvItemsSold.setText(String.valueOf(itemsSold));

        int lowStockCount = 0;
        for (Product p : allProducts) {
            if (p.stock <= p.minStock) lowStockCount++;
        }
        tvLowStock.setText(String.valueOf(lowStockCount));

        updateChartData(allSales);
        updateCategoryCharts(allSales, allProducts);
    }

    private void updateCategoryCharts(List<Sale> sales, List<Product> products) {
        // Sales by Category
        Map<String, Double> salesByCategory = new HashMap<>();
        for (Sale s : sales) {
            String[] parts = s.itemsSummary.split(", ");
            for (String part : parts) {
                if (part.contains(" x")) {
                    String name = part.substring(0, part.lastIndexOf(" x"));
                    // This is inefficient, ideally Sale stores product IDs or Categories
                    for (Product p : products) {
                        if (p.name.equals(name)) {
                            double amount = p.price * Integer.parseInt(part.substring(part.lastIndexOf(" x") + 2));
                            salesByCategory.put(p.category, salesByCategory.getOrDefault(p.category, 0.0) + amount);
                            break;
                        }
                    }
                }
            }
        }
        setupPieChart(salesCategoryChart);
        updatePieChartData(salesCategoryChart, salesByCategory, "Sales by Category");

        // Stock by Category
        Map<String, Integer> stockByCategory = new HashMap<>();
        for (Product p : products) {
            stockByCategory.put(p.category, stockByCategory.getOrDefault(p.category, 0) + p.stock);
        }
        setupPieChart(stockCategoryChart);
        updatePieChartDataInteger(stockCategoryChart, stockByCategory, "Stock by Category");
    }

    private void updatePieChartData(PieChart chart, Map<String, Double> data, String label) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }
        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.invalidate();
    }

    private void updatePieChartDataInteger(PieChart chart, Map<String, Integer> data, String label) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }
        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.invalidate();
    }

    private void updateNotificationBadge() {
        List<Product> products = db.productDao().getAll(userId);
        int lowStockCount = 0;
        for (Product p : products) {
            if (p.stock <= p.minStock) lowStockCount++;
        }

        if (lowStockCount > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(String.valueOf(lowStockCount));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
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
                EditText etSearchLowStock = dialogView.findViewById(R.id.etSearchLowStock);
                RecyclerView rvLowStock = dialogView.findViewById(R.id.rvLowStock);
                rvLowStock.setLayoutManager(new LinearLayoutManager(this));

                // Reusing ProductAdapter but with a no-op click listener or custom logic if needed
                ProductAdapter lowStockAdapter = new ProductAdapter(new ArrayList<>(lowStockItems), product -> {});
                lowStockAdapter.setLowStockMode(true);
                rvLowStock.setAdapter(lowStockAdapter);

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

                new AlertDialog.Builder(this)
                        .setTitle("Low Stock Products")
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .show();
            });
        }).start();
    }

    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void updateChartData(List<Sale> sales) {
        List<Entry> entries = new ArrayList<>();
        Map<String, Double> dailySales = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // Last 7 days including today
        List<String> last7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String date = sdf.format(cal.getTime());
            last7Days.add(date);
            dailySales.put(date, 0.0);
        }

        for (Sale s : sales) {
            String date = sdf.format(new Date(s.timestamp));
            if (dailySales.containsKey(date)) {
                dailySales.put(date, dailySales.get(date) + s.totalAmount);
            }
        }

        for (int i = 0; i < last7Days.size(); i++) {
            entries.add(new Entry(i, dailySales.get(last7Days.get(i)).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Sales");
        dataSet.setColor(getResources().getColor(R.color.primary_green));
        dataSet.setCircleColor(getResources().getColor(R.color.primary_green));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        dataSet.setFillColor(getResources().getColor(R.color.primary_green));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);

        revenueChart.setData(new LineData(dataSet));
        revenueChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < last7Days.size()) {
                    return last7Days.get(index);
                }
                return "";
            }
        });
        revenueChart.animateY(1000);
        revenueChart.invalidate();
    }

    private void updateStatisticsData() {
        List<Sale> allSales = db.saleDao().getAllSales(userId);
        List<Expense> allExpenses = db.expenseDao().getAllExpenses(userId);
        List<Product> allProducts = db.productDao().getAll(userId);

        double totalRevenue = 0;
        for (Sale s : allSales) totalRevenue += s.totalAmount;
        double totalExpense = 0;
        for (Expense e : allExpenses) totalExpense += e.amount;

        tvStatsTotalRevenue.setText(String.format("₱%.2f", totalRevenue));
        tvStatsTotalExpenses.setText(String.format("₱%.2f", totalExpense));
        tvStatsNetIncome.setText(String.format("₱%.2f", totalRevenue - totalExpense));

        updateExpenseRevenueChart(allSales, allExpenses);
        updateTopProductsChart(allSales, allProducts);
    }

    private void updateExpenseRevenueChart(List<Sale> sales, List<Expense> expenses) {
        Map<String, Double> dailyRevenue = new HashMap<>();
        Map<String, Double> dailyExpense = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // Last 15 days
        List<String> dates = new ArrayList<>();
        for (int i = 14; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String date = sdf.format(cal.getTime());
            dates.add(date);
            dailyRevenue.put(date, 0.0);
            dailyExpense.put(date, 0.0);
        }

        for (Sale s : sales) {
            String date = sdf.format(new Date(s.timestamp));
            if (dailyRevenue.containsKey(date)) {
                dailyRevenue.put(date, dailyRevenue.get(date) + s.totalAmount);
            }
        }
        for (Expense e : expenses) {
            String date = sdf.format(new Date(e.timestamp));
            if (dailyExpense.containsKey(date)) {
                dailyExpense.put(date, dailyExpense.get(date) + e.amount);
            }
        }

        List<Entry> revEntries = new ArrayList<>();
        List<Entry> expEntries = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            revEntries.add(new Entry(i, dailyRevenue.get(dates.get(i)).floatValue()));
            expEntries.add(new Entry(i, dailyExpense.get(dates.get(i)).floatValue()));
        }

        LineDataSet revSet = new LineDataSet(revEntries, "Revenue");
        revSet.setColor(getResources().getColor(R.color.primary_green));
        revSet.setCircleColor(getResources().getColor(R.color.primary_green));
        revSet.setLineWidth(2f);
        revSet.setDrawFilled(true);
        revSet.setFillAlpha(30);
        revSet.setFillColor(getResources().getColor(R.color.primary_green));

        LineDataSet expSet = new LineDataSet(expEntries, "Expenses");
        expSet.setColor(0xFFF44336); // Red
        expSet.setCircleColor(0xFFF44336);
        expSet.setLineWidth(2f);
        expSet.setDrawFilled(true);
        expSet.setFillAlpha(30);
        expSet.setFillColor(0xFFF44336);

        LineData data = new LineData(revSet, expSet);
        expenseRevenueChart.setData(data);
        expenseRevenueChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dates.size()) {
                    return dates.get(index);
                }
                return "";
            }
        });
        expenseRevenueChart.animateX(1000);
        expenseRevenueChart.invalidate();
    }

    private void updateTopProductsChart(List<Sale> sales, List<Product> products) {
        Map<String, Integer> productQty = new HashMap<>();
        for (Sale s : sales) {
            String[] parts = s.itemsSummary.split(", ");
            for (String part : parts) {
                if (part.contains(" x")) {
                    try {
                        String name = part.substring(0, part.lastIndexOf(" x")).trim();
                        String qtyStr = part.substring(part.lastIndexOf(" x") + 2).trim();
                        if (qtyStr.endsWith(",")) qtyStr = qtyStr.substring(0, qtyStr.length() - 1);
                        int qty = Integer.parseInt(qtyStr);
                        productQty.put(name, productQty.getOrDefault(name, 0) + qty);
                    } catch (Exception ignored) {}
                }
            }
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(productQty.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int count = Math.min(5, list.size());
        for (int i = 0; i < count; i++) {
            entries.add(new BarEntry(i, list.get(i).getValue()));
            labels.add(list.get(i).getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Top 5 Products");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        BarData data = new BarData(dataSet);
        topProductsChart.setData(data);
        topProductsChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        topProductsChart.invalidate();
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

        view.setVisibility(View.VISIBLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (view == cashierContent) {
            if (btnViewCart != null) btnViewCart.setVisibility(View.VISIBLE);
        } else {
            if (btnViewCart != null) btnViewCart.setVisibility(View.GONE);
        }
    }

    private void updateCheckoutButton() {
        String text = String.format("Checkout (₱%.2f)", totalAmount);
        btnCheckout.setText(text);
        if (btnCheckout2 != null) btnCheckout2.setText(text);
    }

    private void showSalesHistoryPage() {
        switchContent(salesHistoryContent, "Sales History");
        List<Sale> sales = db.saleDao().getAllSales(userId);
        saleAdapter.updateList(sales);
    }

    private void refreshProductList() {
        List<Product> products = db.productDao().getAll(userId);
        adapter.updateList(products);
        notifyAdapters();
    }

    private void exportProductsToExcel() {
        createDocumentLauncher.launch("Products_Export.xlsx");
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    finish();
                })
                .setNegativeButton("Cancel", null)
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
                        .setTitle("Mark as Paid")
                        .setMessage("Has this debt of ₱" + String.format("%.2f", debt.amount) + " for " + debt.quantity + "x " + debt.productName + " been fully paid?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Record a sale when paid
                            String summary = debt.productName + " (Debt Payment) x" + debt.quantity;
                            Sale sale = new Sale(debt.amount, debt.amount, 0, System.currentTimeMillis(), summary, userId);
                            long saleId = db.saleDao().insert(sale);

                            debt.status = "Paid";
                            debt.associatedSaleId = (int) saleId;
                            db.debtDao().update(debt);

                            refreshDebts();
                            Toast.makeText(POSActivity.this, "Debt marked as paid and recorded in history", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

            @Override
            public void onUndo(Debt debt) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Undo Payment")
                        .setMessage("Revert this debt to Unpaid? The associated sale record will be deleted.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if (debt.associatedSaleId != -1) {
                                db.saleDao().deleteById(debt.associatedSaleId);
                            }
                            debt.status = "Unpaid";
                            debt.associatedSaleId = -1;
                            db.debtDao().update(debt);
                            refreshDebts();
                            Toast.makeText(POSActivity.this, "Payment undone", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

            @Override
            public void onDelete(Debt debt) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Delete Debt Record")
                        .setMessage("Are you sure you want to delete this debt record?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.debtDao().delete(debt);
                            refreshDebts();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        rvDebts.setAdapter(debtAdapter);
    }

    private void refreshDebts() {
        debts = db.debtDao().getAllDebts(userId);
        if (debtAdapter != null) {
            debtAdapter.setDebts(debts);
        }
    }

    private void showAddDebtDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_debt, null);

        EditText etName = view.findViewById(R.id.etDebtCustomerName);
        Spinner spinnerProduct = view.findViewById(R.id.spinnerDebtProduct);
        EditText etQuantity = view.findViewById(R.id.etDebtQuantity);
        EditText etAmount = view.findViewById(R.id.etDebtAmount);
        EditText etNote = view.findViewById(R.id.etDebtNote);

        List<Product> productList = db.productDao().getAll(userId);
        List<String> productNames = new ArrayList<>();
        for (Product p : productList) {
            productNames.add(p.name + " (₱" + p.price + ", Stock: " + p.stock + ")");
        }

        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productNames);
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProduct.setAdapter(productAdapter);

        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Product selected = productList.get(position);
                etAmount.setText(String.valueOf(selected.price));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(view)
                .setTitle("Add New Debt")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String qtyStr = etQuantity.getText().toString();
                    String amountStr = etAmount.getText().toString();
                    String note = etNote.getText().toString();

                    if (name.isEmpty() || qtyStr.isEmpty() || amountStr.isEmpty() || productList.isEmpty()) {
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = Integer.parseInt(qtyStr);
                    double amount = Double.parseDouble(amountStr) * quantity;
                    Product selectedProduct = productList.get(spinnerProduct.getSelectedItemPosition());

                    if (selectedProduct.stock < quantity) {
                        Toast.makeText(this, "Insufficient stock", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Deduct stock immediately when loaning
                    selectedProduct.stock -= quantity;
                    db.productDao().update(selectedProduct);

                    Debt debt = new Debt(name, selectedProduct.name, quantity, amount, System.currentTimeMillis(), "Unpaid", note, userId);
                    db.debtDao().insert(debt);
                    refreshDebts();
                    refreshProductList();
                    Toast.makeText(this, "Debt recorded and stock updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupReservationAdapter() {
        reservationAdapter = new ReservationAdapter(reservations, new ReservationAdapter.OnReservationActionListener() {
            @Override
            public void onComplete(Reservation reservation) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Complete Reservation")
                        .setMessage("Do you want to complete this reservation and record a sale?")
                        .setPositiveButton("Complete", (dialog, which) -> {
                            // Record a sale
                            Sale sale = new Sale(reservation.totalAmount, reservation.totalAmount, 0, System.currentTimeMillis(), reservation.itemsSummary, userId);
                            long saleId = db.saleDao().insert(sale);

                            reservation.status = "Completed";
                            reservation.associatedSaleId = (int) saleId;
                            db.reservationDao().update(reservation);
                            refreshReservations();
                            Toast.makeText(POSActivity.this, "Reservation completed and sale recorded", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onCancel(Reservation reservation) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Cancel Reservation")
                        .setMessage("Are you sure you want to cancel this reservation? Stock will be restored.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Restore stock
                            String[] parts = reservation.itemsSummary.split(", ");
                            for (String part : parts) {
                                if (part.isEmpty()) continue;
                                try {
                                    int xIndex = part.lastIndexOf(" x");
                                    String name = part.substring(0, xIndex);
                                    int qty = Integer.parseInt(part.substring(xIndex + 2));
                                    Product p = db.productDao().getProductByName(name, userId);
                                    if (p != null) {
                                        p.stock += qty;
                                        db.productDao().update(p);
                                    }
                                } catch (Exception e) {
                                    Log.e("POSActivity", "Error restoring stock: " + e.getMessage());
                                }
                            }
                            reservation.status = "Cancelled";
                            db.reservationDao().update(reservation);
                            refreshReservations();
                            refreshProductList();
                            Toast.makeText(POSActivity.this, "Reservation cancelled and stock restored", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }

            @Override
            public void onUndo(Reservation reservation) {
                new AlertDialog.Builder(POSActivity.this)
                        .setTitle("Undo Status")
                        .setMessage("Revert this reservation to Pending? (If it was completed, the sale record will be deleted; if it was cancelled, stock will be re-deducted)")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if ("Completed".equalsIgnoreCase(reservation.status)) {
                                if (reservation.associatedSaleId != null && reservation.associatedSaleId != -1) {
                                    db.saleDao().deleteById(reservation.associatedSaleId);
                                    reservation.associatedSaleId = -1;
                                }
                            } else if ("Cancelled".equalsIgnoreCase(reservation.status)) {
                                // Re-deduct stock
                                String[] parts = reservation.itemsSummary.split(", ");
                                for (String part : parts) {
                                    if (part.isEmpty()) continue;
                                    try {
                                        int xIndex = part.lastIndexOf(" x");
                                        String name = part.substring(0, xIndex);
                                        int qty = Integer.parseInt(part.substring(xIndex + 2));
                                        Product p = db.productDao().getProductByName(name, userId);
                                        if (p != null) {
                                            p.stock -= qty;
                                            db.productDao().update(p);
                                        }
                                    } catch (Exception e) {
                                        Log.e("POSActivity", "Error re-deducting stock: " + e.getMessage());
                                    }
                                }
                            }
                            reservation.status = "Pending";
                            db.reservationDao().update(reservation);
                            refreshReservations();
                            refreshProductList();
                            Toast.makeText(POSActivity.this, "Reservation reverted to Pending", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        rvReservations.setAdapter(reservationAdapter);
    }

    private void refreshReservations() {
        reservations = db.reservationDao().getAllReservations(userId);
        if (reservationAdapter != null) {
            reservationAdapter.setReservations(reservations);
        }
    }

    private void showAddReservationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_reservation, null);

        EditText etCustomerName = view.findViewById(R.id.etResCustomerName);
        EditText etContact = view.findViewById(R.id.etResContact);
        Spinner spinnerProduct = view.findViewById(R.id.spinnerResProduct);
        EditText etQuantity = view.findViewById(R.id.etResQuantity);
        TextView tvSummary = view.findViewById(R.id.tvResSummary);
        Button btnAdd = view.findViewById(R.id.btnResAddItem);

        List<Product> productList = db.productDao().getAll(userId);
        List<String> productNames = new ArrayList<>();
        for (Product p : productList) productNames.add(p.name + " (Stock: " + p.stock + ")");

        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productNames);
        spinnerProduct.setAdapter(productAdapter);

        final List<CartItem> resItems = new ArrayList<>();
        final double[] resTotal = {0};

        btnAdd.setOnClickListener(v -> {
            if (productList.isEmpty()) return;
            Product p = productList.get(spinnerProduct.getSelectedItemPosition());
            String qtyStr = etQuantity.getText().toString();
            if (qtyStr.isEmpty()) return;
            int qty = Integer.parseInt(qtyStr);

            if (qty > p.stock) {
                Toast.makeText(this, "Not enough stock", Toast.LENGTH_SHORT).show();
                return;
            }

            resItems.add(new CartItem(p, qty));
            resTotal[0] += p.price * qty;
            
            StringBuilder sb = new StringBuilder();
            for (CartItem item : resItems) {
                sb.append(item.product.name).append(" x").append(item.quantity).append("\n");
            }
            sb.append("Total: ₱").append(String.format("%.2f", resTotal[0]));
            tvSummary.setText(sb.toString());
        });

        builder.setView(view)
                .setTitle("Add Reservation")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etCustomerName.getText().toString();
                    if (name.isEmpty() || resItems.isEmpty()) {
                        Toast.makeText(this, "Name and items are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    StringBuilder summary = new StringBuilder();
                    for (CartItem item : resItems) {
                        summary.append(item.product.name).append(" x").append(item.quantity).append(", ");
                        
                        // Deduct stock for reservation
                        item.product.stock -= item.quantity;
                        db.productDao().update(item.product);
                    }

                    Reservation res = new Reservation(name, etContact.getText().toString(), 
                        summary.toString(), resTotal[0], System.currentTimeMillis(), "Pending", userId);
                    db.reservationDao().insert(res);
                    refreshReservations();
                    refreshProductList();
                    Toast.makeText(this, "Reservation added", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveExcelToUri(Uri uri) {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Products");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Name");
            headerRow.createCell(2).setCellValue("Category");
            headerRow.createCell(3).setCellValue("Price");
            headerRow.createCell(4).setCellValue("Stock");
            headerRow.createCell(5).setCellValue("Min Stock");

            List<Product> products = db.productDao().getAll(userId);
            int rowNum = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.id);
                row.createCell(1).setCellValue(p.name);
                row.createCell(2).setCellValue(p.category);
                row.createCell(3).setCellValue(p.price);
                row.createCell(4).setCellValue(p.stock);
                row.createCell(5).setCellValue(p.minStock);
            }

            workbook.write(outputStream);
            Toast.makeText(this, "Exported successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("POSActivity", "Error exporting Excel", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
