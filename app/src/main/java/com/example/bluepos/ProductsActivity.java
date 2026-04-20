package com.example.bluepos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluepos.pos.AppDatabase;
import com.example.bluepos.pos.Expense;
import com.example.bluepos.pos.ExpenseAdapter;
import com.example.bluepos.pos.Product;
import com.example.bluepos.pos.ProductManageAdapter;
import com.example.bluepos.pos.Sale;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductsActivity extends AppCompatActivity implements ProductManageAdapter.OnProductActionListener {

    private AppDatabase db;
    private ProductManageAdapter adapter;
    private ExpenseAdapter expenseAdapter;
    private RecyclerView rvProducts, rvRecentExpenses;
    private SearchView searchView;
    private boolean isAuthorized = false;
    private int userId;

    private View productsSection, expensesSection;
    private EditText etExpenseTitle, etExpenseAmount;
    private TextView tvExpToday, tvExpTotal, tvExpRevenue, tvExpNet;
    private android.widget.Button btnAddExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        db = AppDatabase.getInstance(this);

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
            }
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvProducts = findViewById(R.id.rvProducts);
        searchView = findViewById(R.id.searchView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddProduct);

        productsSection = findViewById(R.id.products_section);
        expensesSection = findViewById(R.id.expenses_section);

        // Expense Views
        etExpenseTitle = findViewById(R.id.etExpenseTitle);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        tvExpToday = findViewById(R.id.tvExpToday);
        tvExpTotal = findViewById(R.id.tvExpTotal);
        tvExpRevenue = findViewById(R.id.tvExpRevenue);
        tvExpNet = findViewById(R.id.tvExpNet);
        rvRecentExpenses = findViewById(R.id.rvRecentExpenses);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductManageAdapter(new ArrayList<>(), this);
        rvProducts.setAdapter(adapter);

        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(v -> showProductDialog(null));
        btnAddExpense.setOnClickListener(v -> saveExpense());

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    productsSection.setVisibility(View.VISIBLE);
                    expensesSection.setVisibility(View.GONE);
                    refreshList();
                } else {
                    productsSection.setVisibility(View.GONE);
                    expensesSection.setVisibility(View.VISIBLE);
                    updateExpenseUI();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupSearch();
        
        // Hide content until authorized
        hideContent();
        promptForPassword();
    }

    private void hideContent() {
        findViewById(R.id.main_content).setVisibility(View.GONE);
        findViewById(R.id.tabLayout).setVisibility(View.GONE);
    }

    private void promptForPassword() {
        EditText etPassword = new EditText(this);
        etPassword.setHint("Enter Owner Password");
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("Security Check")
                .setMessage("Please enter your registered password to access Inventory Management.")
                .setView(etPassword)
                .setCancelable(false)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String password = etPassword.getText().toString();
                    
                    // Check if this password exists in the users table
                    boolean isValid = checkOwnerPassword(password);
                    
                    if (isValid) {
                        isAuthorized = true;
                        showContent();
                        Toast.makeText(this, "Access Granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Incorrect Owner Password", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity on wrong password
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private boolean checkOwnerPassword(String password) {
        // We query the database for any user with this password
        // This assumes the registered user is the owner
        return db.userDao().getUserByPassword(password) != null;
    }

    private void showContent() {
        findViewById(R.id.main_content).setVisibility(View.VISIBLE);
        findViewById(R.id.tabLayout).setVisibility(View.VISIBLE);
        refreshList();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });
    }

    private void performSearch(String query) {
        List<Product> results;
        if (query.isEmpty()) {
            results = db.productDao().getAll(userId);
        } else {
            results = db.productDao().searchProducts(userId, query);
        }
        adapter.updateList(results);
    }

    private void refreshList() {
        List<Product> products = db.productDao().getAll(userId);
        adapter.updateList(products);
    }

    private void showProductDialog(Product product) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etCost = dialogView.findViewById(R.id.etCost);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etLimit = dialogView.findViewById(R.id.etLimit);
        android.widget.Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        EditText etCustomCategory = dialogView.findViewById(R.id.etCustomCategory);

        String[] categories = {"General", "Food", "Beverages", "Electronics", "Clothing", "Others"};
        android.widget.ArrayAdapter<String> catAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (categories[position].equals("Others")) {
                    etCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    etCustomCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        String title = "Add Product";
        if (product != null) {
            title = "Edit Product";
            etName.setText(product.name);
            etCost.setText(String.valueOf(product.cost));
            etPrice.setText(String.valueOf(product.price));
            etQuantity.setText(String.valueOf(product.quantity));
            etLimit.setText(String.valueOf(product.quantityLimit));

            boolean found = false;
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(product.category)) {
                    spinnerCategory.setSelection(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                spinnerCategory.setSelection(categories.length - 1); // Select "Others"
                etCustomCategory.setVisibility(View.VISIBLE);
                etCustomCategory.setText(product.category);
            }
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
                    String category = spinnerCategory.getSelectedItem().toString();
                    if (category.equals("Others") && !etCustomCategory.getText().toString().trim().isEmpty()) {
                        category = etCustomCategory.getText().toString().trim();
                    }

                    if (product == null) {
                        Product p = new Product(name, cost, price, qty, limit, category, userId);
                        db.productDao().insert(p);
                    } else {
                        product.name = name;
                        product.cost = cost;
                        product.price = price;
                        product.quantity = qty;
                        product.quantityLimit = limit;
                        product.category = category;
                        db.productDao().update(product);
                    }
                    refreshList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEdit(Product product) {
        showProductDialog(product);
    }

    @Override
    public void onDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.productDao().delete(product);
                    refreshList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveExpense() {
        String title = etExpenseTitle.getText().toString().trim();
        String amountStr = etExpenseAmount.getText().toString().trim();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(title, amount, System.currentTimeMillis(), userId);
            db.expenseDao().insert(expense);

            etExpenseTitle.setText("");
            etExpenseAmount.setText("");
            Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show();
            updateExpenseUI();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateExpenseUI() {
        List<Expense> expenses = db.expenseDao().getAllExpenses(userId);
        if (expenseAdapter == null) {
            expenseAdapter = new ExpenseAdapter(expenses);
            rvRecentExpenses.setAdapter(expenseAdapter);
        } else {
            expenseAdapter.updateList(expenses);
        }

        long startOfDay = getStartOfDay();
        double todayExp = db.expenseDao().getTodayExpenses(userId, startOfDay);
        double totalExp = db.expenseDao().getTotalExpenses(userId);

        List<Sale> allSales = db.saleDao().getAllSales(userId);
        double totalRevenue = 0;
        for (Sale s : allSales) totalRevenue += s.totalAmount;

        double netIncome = totalRevenue - totalExp;

        tvExpToday.setText(String.format(Locale.US, "₱%.2f", todayExp));
        tvExpTotal.setText(String.format(Locale.US, "₱%.2f", totalExp));
        tvExpRevenue.setText(String.format(Locale.US, "₱%.2f", totalRevenue));
        tvExpNet.setText(String.format(Locale.US, "₱%.2f", netIncome));
    }

    private long getStartOfDay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
