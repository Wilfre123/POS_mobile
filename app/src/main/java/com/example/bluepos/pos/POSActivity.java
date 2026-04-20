package com.example.bluepos.pos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluepos.LoginActivity;
import com.example.bluepos.MainActivity;
import com.example.bluepos.ProductsActivity;
import com.example.bluepos.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class POSActivity extends AppCompatActivity implements ProductAdapter.OnAddToCartListener {

    private AppDatabase db;
    private ProductAdapter adapter;
    private List<CartItem> cart = new ArrayList<>();
    private double totalAmount = 0.0;
    private Button btnCheckout;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private EditText etAmountPaid, etSearch, etSearchCart;
    private TextView tvChange, tvCartTotal;
    private RecyclerView rvCart, rvSalesHistory;
    private CartAdapter cartAdapter;
    private SaleAdapter saleAdapter;
    private View cashierContent, dashboardContent, reportsContent, salesHistoryContent, cartContent, statisticsContent;
    private ImageButton btnViewCart;
    private TextView tvNetIncome, tvTotalRevenue, tvTotalExpenses, tvTodaySales, tvTotalProducts, tvTotalSalesCount, tvItemsSold, tvLowStock, tvStatsTotalRevenue, tvStatsTotalExpenses, tvStatsNetIncome;
    private LineChart revenueChart, expenseRevenueChart;
    private HorizontalBarChart topProductsChart;
    private PieChart salesCategoryChart, stockCategoryChart;
    private TextView tvNotificationBadge;
    private View btnNotification;
    private Button btnExportProducts;

    private int userId;
    private androidx.activity.result.ActivityResultLauncher<String> createDocumentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);

        db = AppDatabase.getInstance(this);
        
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);

            if (id == R.id.nav_pos || id == R.id.nav_cashier) {
                switchContent(cashierContent, "Cashier");
            } else if (id == R.id.nav_dashboard) {
                showDashboard();
            } else if (id == R.id.nav_statistics) {
                showStatistics();
            } else if (id == R.id.nav_reports) {
                switchContent(reportsContent, "Reports");
            } else if (id == R.id.nav_sales_history) {
                showSalesHistoryPage();
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(this, ProductsActivity.class));
            } else if (id == R.id.nav_logout) {
                showLogoutDialog();
            } else {
                Toast.makeText(this, "Feature coming soon: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        RecyclerView rvProducts = findViewById(R.id.rvProducts);
        btnCheckout = findViewById(R.id.btnCheckout);
        FloatingActionButton btnAddProduct = findViewById(R.id.btnAddProduct);
        etAmountPaid = findViewById(R.id.etAmountPaid);
        tvChange = findViewById(R.id.tvChange);
        etSearch = findViewById(R.id.etSearch);
        
        // Content Views Initialization
        cashierContent = findViewById(R.id.cashier_content);
        dashboardContent = findViewById(R.id.dashboard_content);
        reportsContent = findViewById(R.id.reports_content);
        btnExportProducts = findViewById(R.id.btnExportProducts);
        salesHistoryContent = findViewById(R.id.sales_history_content);
        cartContent = findViewById(R.id.cart_content);
        statisticsContent = findViewById(R.id.statistics_content);

        // Cart Panel Initialization
        rvCart = findViewById(R.id.rvCart);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        etSearchCart = findViewById(R.id.etSearchCart);
        btnViewCart = findViewById(R.id.btnViewCart);
        rvSalesHistory = findViewById(R.id.rvSales);

        btnViewCart.setOnClickListener(v -> switchContent(cartContent, "Current Order"));

        btnExportProducts.setOnClickListener(v -> exportProductsToExcel());

        initDashboardViews();

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvCart.setLayoutManager(new GridLayoutManager(this, 2)); // Two column grid for cart
        rvSalesHistory.setLayoutManager(new LinearLayoutManager(this));
        
        setupCartAdapter();
        refreshProductList();

        btnAddProduct.setOnClickListener(v -> showAddProductDialog());

        btnCheckout.setOnClickListener(v -> {
            if (cart.isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
            } else {
                double paid = getPaidAmount();
                if (paid < totalAmount) {
                    Toast.makeText(this, "Insufficient payment!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Deduct stock and build items summary
                StringBuilder summary = new StringBuilder();
                for (int i = 0; i < cart.size(); i++) {
                    CartItem item = cart.get(i);
                    db.productDao().update(item.product);
                    summary.append(item.product.name).append(" x").append(item.quantity);
                    if (i < cart.size() - 1) summary.append(", ");
                }

                // Save Sale to Database
                double change = paid - totalAmount;
                Sale sale = new Sale(totalAmount, paid, change, System.currentTimeMillis(), summary.toString(), userId);
                db.saleDao().insert(sale);

                Toast.makeText(this, "Sale Completed! Change: ₱" + String.format(Locale.US, "%.2f", change), Toast.LENGTH_LONG).show();
                cart.clear();
                totalAmount = 0.0;
                etAmountPaid.setText("");
                updateTotals();
                refreshProductList();
            }
        });

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

        btnNotification = findViewById(R.id.btnNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnNotification.setOnClickListener(v -> showLowStockDialog());
        updateNotificationBadge();

        createDocumentLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                uri -> {
                    if (uri != null) {
                        saveExcelToUri(uri);
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProductList();
        updateNotificationBadge();
    }

    private void setupCartAdapter() {
        cartAdapter = new CartAdapter(cart, new CartAdapter.OnCartActionListener() {
            @Override
            public void onIncrease(CartItem item) {
                if (item.product.quantity > 0) {
                    item.product.quantity--;
                    item.quantity++;
                    updateTotals();
                    notifyAdapters();
                    updateNotificationBadge();
                } else {
                    Toast.makeText(POSActivity.this, "No more stock!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDecrease(CartItem item) {
                if (item.quantity > 1) {
                    item.product.quantity++;
                    item.quantity--;
                    updateTotals();
                    notifyAdapters();
                    updateNotificationBadge();
                }
            }

            @Override
            public void onRemove(CartItem item) {
                item.product.quantity += item.quantity;
                cart.remove(item);
                updateTotals();
                notifyAdapters();
                updateNotificationBadge();
            }
        });
        rvCart.setAdapter(cartAdapter);
    }

    private void filterCart(String query) {
        String q = query.toLowerCase().trim();
        List<CartItem> filtered = new ArrayList<>();
        if (q.isEmpty()) {
            filtered.addAll(cart);
        } else {
            for (CartItem item : cart) {
                if (item.product.name.toLowerCase().contains(q)) {
                    filtered.add(item);
                }
            }
        }
        if (cartAdapter != null) cartAdapter.updateList(filtered);
    }

    private void updateTotals() {
        totalAmount = 0;
        for (CartItem item : cart) {
            totalAmount += item.product.price * item.quantity;
        }
        String totalStr = String.format(Locale.US, "Total: ₱%.2f", totalAmount);
        if (tvCartTotal != null) tvCartTotal.setText(totalStr);
        updateCheckoutButton();
        calculateChange();
    }

    private void notifyAdapters() {
        if (adapter != null) adapter.notifyDataSetChanged();
        if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
    }

    private void calculateChange() {
        double paid = getPaidAmount();
        double change = paid - totalAmount;
        if (change < 0) change = 0;
        tvChange.setText(String.format(Locale.US, "Change: ₱%.2f", change));
    }

    private double getPaidAmount() {
        try {
            String s = etAmountPaid.getText().toString();
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void filterProducts(String query) {
        List<Product> filtered;
        if (query.isEmpty()) {
            filtered = db.productDao().getAll(userId);
        } else {
            filtered = db.productDao().searchProducts(userId, query);
        }
        if (adapter != null) adapter.updateList(filtered);
    }

    private void showAddProductDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etCost = dialogView.findViewById(R.id.etCost);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etLimit = dialogView.findViewById(R.id.etLimit);
        android.widget.Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);

        String[] categories = {"General", "Food", "Beverages", "Electronics", "Clothing", "Others"};
        android.widget.ArrayAdapter<String> catAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString();
                    double cost = Double.parseDouble(etCost.getText().toString().isEmpty() ? "0" : etCost.getText().toString());
                    double price = Double.parseDouble(etPrice.getText().toString().isEmpty() ? "0" : etPrice.getText().toString());
                    int qty = Integer.parseInt(etQuantity.getText().toString().isEmpty() ? "0" : etQuantity.getText().toString());
                    int limit = Integer.parseInt(etLimit.getText().toString().isEmpty() ? "0" : etLimit.getText().toString());
                    String category = spinnerCategory.getSelectedItem().toString();

                    Product p = new Product(name, cost, price, qty, limit, category, userId);
                    db.productDao().insert(p);
                    refreshProductList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshProductList() {
        List<Product> products = db.productDao().getAll(userId);
        if (adapter == null) {
            adapter = new ProductAdapter(products, this);
            ((RecyclerView)findViewById(R.id.rvProducts)).setAdapter(adapter);
        } else {
            adapter.updateList(products);
        }
    }

    @Override
    public void onAddToCart(Product product) {
        if (product.quantity <= 0) {
            Toast.makeText(this, "Out of Stock!", Toast.LENGTH_SHORT).show();
            return;
        }

        product.quantity--; // Instant subtraction from UI object
        
        boolean found = false;
        for (CartItem item : cart) {
            if (item.product.id == product.id) {
                item.quantity++;
                found = true;
                break;
            }
        }
        
        if (!found) {
            cart.add(new CartItem(product, 1));
        }

        updateTotals();
        notifyAdapters();
        updateNotificationBadge();
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

        expenseRevenueChart = findViewById(R.id.expenseRevenueChart);
        topProductsChart = findViewById(R.id.topProductsChart);
        tvStatsTotalRevenue = findViewById(R.id.tvStatsTotalRevenue);
        tvStatsTotalExpenses = findViewById(R.id.tvStatsTotalExpenses);
        tvStatsNetIncome = findViewById(R.id.tvStatsNetIncome);

        setupChart();
    }

    private void setupChart() {
        if (revenueChart != null) {
            revenueChart.getDescription().setEnabled(false);
            revenueChart.setDrawGridBackground(false);
            revenueChart.getLegend().setEnabled(false);
            revenueChart.getXAxis().setDrawGridLines(false);
            revenueChart.getAxisLeft().setDrawGridLines(false);
            revenueChart.getAxisRight().setEnabled(false);
        }

        setupPieChart(salesCategoryChart);
        setupPieChart(stockCategoryChart);

        if (expenseRevenueChart != null) {
            expenseRevenueChart.getDescription().setEnabled(false);
            expenseRevenueChart.setDrawGridBackground(false);
            expenseRevenueChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            expenseRevenueChart.getXAxis().setDrawGridLines(false);
            expenseRevenueChart.getAxisLeft().setDrawGridLines(false);
            expenseRevenueChart.getAxisRight().setEnabled(false);
            expenseRevenueChart.getLegend().setEnabled(true);
        }

        if (topProductsChart != null) {
            topProductsChart.getDescription().setEnabled(false);
            topProductsChart.setDrawGridBackground(false);
            topProductsChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            topProductsChart.getXAxis().setDrawGridLines(false);
            topProductsChart.getAxisLeft().setDrawGridLines(false);
            topProductsChart.getAxisRight().setEnabled(false);
        }
    }

    private void setupPieChart(PieChart pieChart) {
        if (pieChart == null) return;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.graphics.Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelColor(android.graphics.Color.BLACK);
        pieChart.setEntryLabelTextSize(10f);
    }

    private void updateDashboardData() {
        List<Sale> allSales = db.saleDao().getAllSales(userId);
        List<Product> allProducts = db.productDao().getAll(userId);

        double totalExpenses = db.expenseDao().getTotalExpenses(userId);
        double totalRevenue = 0;
        double todaySales = 0;
        int itemsSold = 0;
        int lowStockCount = 0;

        long startOfDay = getStartOfDay();

        for (Sale sale : allSales) {
            totalRevenue += sale.totalAmount;
            if (sale.timestamp >= startOfDay) {
                todaySales += sale.totalAmount;
            }
            // Estimate items sold from summary "Name xQty, ..."
            String[] parts = sale.itemsSummary.split(",");
            for (String part : parts) {
                try {
                    String qtyStr = part.substring(part.lastIndexOf('x') + 1).trim();
                    itemsSold += Integer.parseInt(qtyStr);
                } catch (Exception ignored) {}
            }
        }

        for (Product p : allProducts) {
            if (p.quantity <= p.quantityLimit) {
                lowStockCount++;
            }
        }

        double netIncome = totalRevenue - totalExpenses;

        tvNetIncome.setText(String.format(Locale.US, "₱%.2f", netIncome));
        tvTotalRevenue.setText(String.format(Locale.US, "₱%.2f", totalRevenue));
        tvTotalExpenses.setText(String.format(Locale.US, "₱%.2f", totalExpenses));
        tvTodaySales.setText(String.format(Locale.US, "₱%.2f", todaySales));
        tvTotalProducts.setText(String.valueOf(allProducts.size()));
        tvTotalSalesCount.setText(String.valueOf(allSales.size()));
        tvItemsSold.setText(String.valueOf(itemsSold));
        tvLowStock.setText(String.valueOf(lowStockCount));

        updateChartData(allSales);
        updateCategoryCharts(allSales, allProducts);
        updateNotificationBadge();
    }

    private void updateCategoryCharts(List<Sale> sales, List<Product> products) {
        // Sales by Category
        java.util.Map<String, Double> salesMap = new java.util.HashMap<>();
        for (Sale sale : sales) {
            // This is complex because Sale only stores itemsSummary as String
            // A more robust app would store SaleItems in a separate table
            String[] parts = sale.itemsSummary.split(",");
            for (String part : parts) {
                try {
                    String name = part.substring(0, part.lastIndexOf('x')).trim();
                    // Find product category by name (Simplified)
                    for (Product p : products) {
                        if (p.name.equals(name)) {
                            salesMap.put(p.category, salesMap.getOrDefault(p.category, 0.0) + p.price);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Stock by Category
        java.util.Map<String, Integer> stockMap = new java.util.HashMap<>();
        for (Product p : products) {
            stockMap.put(p.category, stockMap.getOrDefault(p.category, 0) + p.quantity);
        }

        updatePieChartData(salesCategoryChart, salesMap, "Sales");
        updatePieChartDataInteger(stockCategoryChart, stockMap, "Stock");
    }

    private void updatePieChartData(PieChart chart, java.util.Map<String, Double> dataMap, String label) {
        if (chart == null || dataMap.isEmpty()) return;
        List<PieEntry> entries = new ArrayList<>();
        for (java.util.Map.Entry<String, Double> entry : dataMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(chart));
        data.setValueTextSize(11f);
        data.setValueTextColor(android.graphics.Color.BLACK);
        chart.setData(data);
        chart.invalidate();
    }

    private void updatePieChartDataInteger(PieChart chart, java.util.Map<String, Integer> dataMap, String label) {
        if (chart == null || dataMap.isEmpty()) return;
        List<PieEntry> entries = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : dataMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(chart));
        data.setValueTextSize(11f);
        data.setValueTextColor(android.graphics.Color.BLACK);
        chart.setData(data);
        chart.invalidate();
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge == null) return;
        new Thread(() -> {
            List<Product> allProducts = db.productDao().getAll(userId);
            int lowStockCount = 0;
            for (Product p : allProducts) {
                if (p.quantity <= p.quantityLimit) {
                    lowStockCount++;
                }
            }
            int finalLowStockCount = lowStockCount;
            runOnUiThread(() -> {
                if (finalLowStockCount > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(String.valueOf(finalLowStockCount));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void showLowStockDialog() {
        new Thread(() -> {
            List<Product> allProducts = db.productDao().getAll(userId);
            List<Product> lowStockItems = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.quantity <= p.quantityLimit) {
                    lowStockItems.add(p);
                }
            }

            runOnUiThread(() -> {
                if (lowStockItems.isEmpty()) {
                    Toast.makeText(this, "No low stock items", Toast.LENGTH_SHORT).show();
                    return;
                }

                StringBuilder message = new StringBuilder();
                for (Product p : lowStockItems) {
                    message.append("• ").append(p.name)
                            .append(" (Stock: ").append(p.quantity)
                            .append(", Limit: ").append(p.quantityLimit).append(")\n");
                }

                new AlertDialog.Builder(this)
                        .setTitle("Low Stock Warning")
                        .setMessage(message.toString())
                        .setPositiveButton("Manage Inventory", (dialog, which) -> {
                            startActivity(new Intent(this, ProductsActivity.class));
                        })
                        .setNegativeButton("Close", null)
                        .show();
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
        if (revenueChart == null) return;

        List<Entry> entries = new ArrayList<>();
        // Group sales by day for the last 7 days (Simplified for now)
        for (int i = 0; i < sales.size(); i++) {
            entries.add(new Entry(i, (float) sales.get(i).totalAmount));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue");
        dataSet.setColor(getResources().getColor(R.color.primary_green));
        dataSet.setCircleColor(getResources().getColor(R.color.primary_green));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        revenueChart.setData(lineData);
        revenueChart.invalidate();
    }

    private void updateStatisticsData() {
        List<Sale> allSales = db.saleDao().getAllSales(userId);
        List<Expense> allExpenses = db.expenseDao().getAllExpenses(userId);
        List<Product> allProducts = db.productDao().getAll(userId);

        updateExpenseRevenueChart(allSales, allExpenses);
        updateTopProductsChart(allSales, allProducts);
    }

    private void updateExpenseRevenueChart(List<Sale> sales, List<Expense> expenses) {
        if (expenseRevenueChart == null) return;

        // Group by day for the last 20 days
        java.util.Map<String, Double> revenueMap = new java.util.TreeMap<>();
        java.util.Map<String, Double> expenseMap = new java.util.TreeMap<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd", Locale.US);

        long twentyDaysAgo = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L);

        double totalRevenue = 0;
        double totalExpenses = 0;

        for (Sale sale : sales) {
            if (sale.timestamp >= twentyDaysAgo) {
                String day = sdf.format(new java.util.Date(sale.timestamp));
                double amount = sale.totalAmount;
                revenueMap.put(day, revenueMap.getOrDefault(day, 0.0) + amount);
                totalRevenue += amount;
            }
        }

        for (Expense expense : expenses) {
            if (expense.timestamp >= twentyDaysAgo) {
                String day = sdf.format(new java.util.Date(expense.timestamp));
                double amount = expense.amount;
                expenseMap.put(day, expenseMap.getOrDefault(day, 0.0) + amount);
                totalExpenses += amount;
            }
        }

        if (tvStatsTotalRevenue != null) tvStatsTotalRevenue.setText(String.format(Locale.US, "₱%.2f", totalRevenue));
        if (tvStatsTotalExpenses != null) tvStatsTotalExpenses.setText(String.format(Locale.US, "₱%.2f", totalExpenses));
        if (tvStatsNetIncome != null) tvStatsNetIncome.setText(String.format(Locale.US, "₱%.2f", totalRevenue - totalExpenses));

        List<Entry> revenueEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int i = 0;
        // Ensure all days are represented
        for (long t = twentyDaysAgo; t <= System.currentTimeMillis(); t += 24 * 60 * 60 * 1000L) {
            String day = sdf.format(new java.util.Date(t));
            labels.add(day);
            revenueEntries.add(new Entry(i, revenueMap.getOrDefault(day, 0.0).floatValue()));
            expenseEntries.add(new Entry(i, expenseMap.getOrDefault(day, 0.0).floatValue()));
            i++;
        }

        LineDataSet revSet = new LineDataSet(revenueEntries, "Revenue");
        revSet.setColor(android.graphics.Color.parseColor("#4CAF50")); // Green
        revSet.setCircleColor(android.graphics.Color.parseColor("#4CAF50"));
        revSet.setLineWidth(2f);
        revSet.setCircleRadius(3f);
        revSet.setDrawValues(false);

        LineDataSet expSet = new LineDataSet(expenseEntries, "Expenses");
        expSet.setColor(android.graphics.Color.parseColor("#F44336")); // Red
        expSet.setCircleColor(android.graphics.Color.parseColor("#F44336"));
        expSet.setLineWidth(2f);
        expSet.setCircleRadius(3f);
        expSet.setDrawValues(false);

        LineData data = new LineData(revSet, expSet);
        expenseRevenueChart.setData(data);
        
        expenseRevenueChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) return labels.get(index);
                return "";
            }
        });
        expenseRevenueChart.getXAxis().setGranularity(1f);
        expenseRevenueChart.invalidate();
    }

    private void updateTopProductsChart(List<Sale> sales, List<Product> products) {
        if (topProductsChart == null) return;

        java.util.Map<String, Integer> unitsMap = new java.util.HashMap<>();
        java.util.Map<String, Double> revenueMap = new java.util.HashMap<>();

        for (Sale sale : sales) {
            String[] parts = sale.itemsSummary.split(",");
            for (String part : parts) {
                try {
                    int xIndex = part.lastIndexOf('x');
                    if (xIndex == -1) continue;
                    String name = part.substring(0, xIndex).trim();
                    String qtyStr = part.substring(xIndex + 1).trim();
                    int qty = Integer.parseInt(qtyStr);
                    
                    unitsMap.put(name, unitsMap.getOrDefault(name, 0) + qty);
                    
                    // Find price
                    double price = 0;
                    for (Product p : products) {
                        if (p.name.equals(name)) {
                            price = p.price;
                            break;
                        }
                    }
                    revenueMap.put(name, revenueMap.getOrDefault(name, 0.0) + (price * qty));
                } catch (Exception ignored) {}
            }
        }

        // Sort by units sold and take top 10
        List<java.util.Map.Entry<String, Integer>> sortedList = new ArrayList<>(unitsMap.entrySet());
        sortedList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        if (sortedList.size() > 10) sortedList = sortedList.subList(0, 10);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < sortedList.size(); i++) {
            String name = sortedList.get(i).getKey();
            float units = sortedList.get(i).getValue().floatValue();
            float revenue = revenueMap.getOrDefault(name, 0.0).floatValue();
            // Stacked bar: Units and Revenue (Note: different scales might look weird, maybe better as grouped or just one)
            // But user asked for both. Let's use revenue as it's usually more important for "Top Sold"
            entries.add(new BarEntry(i, new float[]{units, revenue}));
            labels.add(name);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Units & Revenue");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setStackLabels(new String[]{"Units Sold", "Revenue (₱)"});

        BarData data = new BarData(dataSet);
        topProductsChart.setData(data);
        topProductsChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) return labels.get(index);
                return "";
            }
        });
        topProductsChart.invalidate();
    }

    private void switchContent(View viewToShow, String title) {
        if (viewToShow.getVisibility() == View.VISIBLE) return;

        View[] allViews = {cashierContent, dashboardContent, reportsContent, salesHistoryContent, cartContent, statisticsContent};
        
        for (View v : allViews) {
            if (v.getVisibility() == View.VISIBLE) {
                v.setVisibility(View.GONE);
            }
        }

        viewToShow.setVisibility(View.VISIBLE);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        viewToShow.startAnimation(fadeIn);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            if (viewToShow == cartContent) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                if (toolbar.getNavigationIcon() != null) {
                    toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
                }
                toolbar.setNavigationOnClickListener(v -> switchContent(cashierContent, "Cashier"));
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
            }
        }
    }

    private void updateCheckoutButton() {
        btnCheckout.setText(String.format(Locale.US, "Checkout (₱%.2f)", totalAmount));
    }

    private void showSalesHistoryPage() {
        switchContent(salesHistoryContent, "Sales History");
        
        List<Sale> sales = db.saleDao().getAllSales(userId);
        if (saleAdapter == null) {
            saleAdapter = new SaleAdapter(sales);
            rvSalesHistory.setAdapter(saleAdapter);
        } else {
            saleAdapter.updateList(sales);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Do you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showSalesHistoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sales_history, null);
        RecyclerView rvSales = dialogView.findViewById(R.id.rvSales);
        rvSales.setLayoutManager(new LinearLayoutManager(this));

        List<Sale> sales = db.saleDao().getAllSales(userId);
        SaleAdapter saleAdapter = new SaleAdapter(sales);
        rvSales.setAdapter(saleAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.8)
            );
        }
    }

    private void exportProductsToExcel() {
        createDocumentLauncher.launch("Products_Report_" + System.currentTimeMillis() + ".xlsx");
    }

    private void saveExcelToUri(android.net.Uri uri) {
        new Thread(() -> {
            List<Product> products = db.productDao().getAll(userId);
            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                
                Sheet sheet = workbook.createSheet("Products");

                // Create header row
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("ID");
                headerRow.createCell(1).setCellValue("Name");
                headerRow.createCell(2).setCellValue("Category");
                headerRow.createCell(3).setCellValue("Cost");
                headerRow.createCell(4).setCellValue("Price");
                headerRow.createCell(5).setCellValue("Quantity");
                headerRow.createCell(6).setCellValue("Stock Limit");

                // Fill data
                int rowNum = 1;
                for (Product product : products) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(product.id);
                    row.createCell(1).setCellValue(product.name);
                    row.createCell(2).setCellValue(product.category);
                    row.createCell(3).setCellValue(product.cost);
                    row.createCell(4).setCellValue(product.price);
                    row.createCell(5).setCellValue(product.quantity);
                    row.createCell(6).setCellValue(product.quantityLimit);
                }

                workbook.write(outputStream);
                
                runOnUiThread(() -> Toast.makeText(this, "Excel report saved successfully", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error saving Excel: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
