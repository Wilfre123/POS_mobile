package com.example.bluepos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
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
import com.example.bluepos.pos.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductsActivity extends AppCompatActivity implements ProductManageAdapter.OnProductActionListener, ExpenseAdapter.OnExpenseActionListener {

    private AppDatabase db;
    private ProductManageAdapter adapter;
    private ExpenseAdapter expenseAdapter;
    private RecyclerView rvProducts, rvRecentExpenses;
    private SearchView searchView;
    private boolean isAuthorized = false;
    private int userId;

    private View productsSection, expensesSection, historySection;
    private View cardAddExpense, cardOverview;
    private EditText etExpenseTitle, etExpenseAmount;
    private TextView tvExpToday, tvExpTotal, tvExpRevenue, tvExpNet;
    private android.widget.Button btnAddExpense;
    private android.widget.AutoCompleteTextView autoCompleteCategory;
    private String selectedCategory = "All Categories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        db = AppDatabase.getDatabase(this);

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
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        rvProducts = findViewById(R.id.rvProducts);
        searchView = findViewById(R.id.searchView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddProduct);

        productsSection = findViewById(R.id.products_section);
        expensesSection = findViewById(R.id.expenses_section);
        historySection = findViewById(R.id.history_section);
        cardAddExpense = findViewById(R.id.cardAddExpense);
        cardOverview = findViewById(R.id.cardOverview);

        // Expense Views
        etExpenseTitle = findViewById(R.id.etExpenseTitle);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        
        tvExpToday = findViewById(R.id.tvExpToday);
        tvExpTotal = findViewById(R.id.tvExpTotal);
        tvExpRevenue = findViewById(R.id.tvExpRevenue);
        tvExpNet = findViewById(R.id.tvExpNet);
        rvRecentExpenses = findViewById(R.id.rvRecentExpenses);
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);

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
                    historySection.setVisibility(View.GONE);
                    refreshList();
                } else if (tab.getPosition() == 1) {
                    productsSection.setVisibility(View.GONE);
                    expensesSection.setVisibility(View.VISIBLE);
                    historySection.setVisibility(View.GONE);
                    updateExpenseUI();
                } else {
                    productsSection.setVisibility(View.GONE);
                    expensesSection.setVisibility(View.GONE);
                    historySection.setVisibility(View.VISIBLE);
                    updateExpenseUI();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupSearch();
        setupCategoryFilter();
        
        // Hide content until authorized
        hideContent();
        promptForPassword();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                ProductsActivity.this.getOnBackPressedDispatcher().onBackPressed();
            }
        });
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
                    new Thread(() -> {
                        User user = db.userDao().getUserByPassword(password);
                        runOnUiThread(() -> {
                            if (user != null) {
                                isAuthorized = true;
                                this.userId = user.id;
                                // Save to prefs to maintain session
                                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putInt("userId", userId).apply();
                                
                                showContent();
                                setupCategoryFilter(); // Refresh category filter with correct userId
                                Toast.makeText(this, "Access Granted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Incorrect Owner Password", Toast.LENGTH_SHORT).show();
                                finish(); // Close activity on wrong password
                            }
                        });
                    }).start();
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
        if (query.isEmpty() && (selectedCategory == null || selectedCategory.equals("All Categories"))) {
            results = db.productDao().getAll(userId);
        } else {
            // Apply both search and category filter
            List<Product> all = db.productDao().getAll(userId);
            results = new ArrayList<>();
            for (Product p : all) {
                boolean matchesQuery = query.isEmpty() || p.name.toLowerCase().contains(query.toLowerCase());
                boolean matchesCategory = selectedCategory.equals("All Categories") || p.category.equalsIgnoreCase(selectedCategory);
                if (matchesQuery && matchesCategory) {
                    results.add(p);
                }
            }
        }
        adapter.updateList(results);
    }

    private void setupCategoryFilter() {
        new Thread(() -> {
            List<String> categories = db.productDao().getUniqueCategories(userId);
            List<String> filterList = new ArrayList<>();
            filterList.add("All Categories");
            for (String cat : categories) {
                if (cat != null && !cat.isEmpty() && !cat.equalsIgnoreCase("Add New...")) {
                    filterList.add(cat);
                }
            }

            runOnUiThread(() -> {
                android.widget.ArrayAdapter<String> catAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filterList);
                autoCompleteCategory.setAdapter(catAdapter);
                autoCompleteCategory.setText("All Categories", false);

                // Show dropdown when focused to show all categories
                autoCompleteCategory.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        autoCompleteCategory.showDropDown();
                    }
                });
                autoCompleteCategory.setOnClickListener(v -> autoCompleteCategory.showDropDown());

                autoCompleteCategory.setOnItemClickListener((parent, view, position, id) -> {
                    selectedCategory = filterList.get(position);
                    performSearch(searchView.getQuery().toString());
                });

                autoCompleteCategory.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.toString().isEmpty()) {
                            selectedCategory = "All Categories";
                            performSearch(searchView.getQuery().toString());
                        }
                    }
                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });
            });
        }).start();
    }

    private void refreshList() {
        new Thread(() -> {
            List<Product> products = db.productDao().getAll(userId);
            runOnUiThread(() -> {
                adapter.updateList(products);
            });
        }).start();
    }

    private void showProductDialog(Product product) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etCost = dialogView.findViewById(R.id.etCost);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etLimit = dialogView.findViewById(R.id.etLimit);
        AutoCompleteTextView autoCompleteCategory = dialogView.findViewById(R.id.autoCompleteDialogCategory);
        android.widget.CheckBox cbHasExpiration = dialogView.findViewById(R.id.cbHasExpiration);
        EditText etExpirationDate = dialogView.findViewById(R.id.etExpirationDate);

        final java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

        cbHasExpiration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etExpirationDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
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

                // Show dropdown when focused to show all categories
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
                        etExpirationDate.setVisibility(View.VISIBLE);
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
                            refreshList();
                            Toast.makeText(this, "Product Saved under " + finalCategory, Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEdit(Expense expense) {
        EditText etTitle = new EditText(this);
        etTitle.setText(expense.title);
        etTitle.setHint("Title");
        etTitle.setTextColor(getResources().getColor(R.color.black));

        EditText etAmount = new EditText(this);
        etAmount.setText(String.valueOf(expense.amount));
        etAmount.setHint("Amount");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setTextColor(getResources().getColor(R.color.black));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        layout.addView(etTitle);
        layout.addView(etAmount);

        new AlertDialog.Builder(this)
                .setTitle("Edit Expense")
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    if (!title.isEmpty() && !amountStr.isEmpty()) {
                        expense.title = title;
                        expense.amount = Double.parseDouble(amountStr);
                        db.expenseDao().update(expense);
                        updateExpenseUI();
                        Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void onDelete(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.expenseDao().delete(expense);
                    updateExpenseUI();
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
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
            expenseAdapter = new ExpenseAdapter(expenses, this);
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
