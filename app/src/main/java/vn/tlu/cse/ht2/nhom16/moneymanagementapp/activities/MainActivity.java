package vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks; // Import Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date; // Import Date class
import java.util.List;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.AiInsightsFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.BudgetFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.HistoryFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.HomeFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.StatisticsFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Budget;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private String userId;

    private BottomNavigationView bottomNavigationView;

    private List<Expense> expenseList; // Danh sách chung cho tất cả các Fragment
    private List<Budget> budgetList; // Danh sách ngân sách
    private SharedPreferences sharedPreferences;
    private String currentCurrency = "VND";
    private DecimalFormat decimalFormat;

    // Các Fragment
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private StatisticsFragment statisticsFragment;
    private AiInsightsFragment aiInsightsFragment;
    private BudgetFragment budgetFragment; // Fragment mới
    private Fragment activeFragment; // Fragment hiện tại đang hiển thị

    private static final String PREF_KEY_DATA_INITIALIZED = "data_initialized";
    private AlertDialog currencyDialog; // Khai báo AlertDialog để có thể dismiss trong onDestroy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int savedThemeMode = getSharedPreferences("app_prefs", MODE_PRIVATE).getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedThemeMode);

        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        currentCurrency = sharedPreferences.getString("currency", "VND");
        updateDecimalFormat();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = currentUser.getUid();
        Log.d(TAG, "onCreate: Current User ID: " + userId); // Log userId

        expenseList = new ArrayList<>();
        budgetList = new ArrayList<>(); // Khởi tạo danh sách ngân sách

        // Khởi tạo các Fragment
        homeFragment = new HomeFragment();
        historyFragment = new HistoryFragment();
        statisticsFragment = new StatisticsFragment();
        aiInsightsFragment = new AiInsightsFragment();
        budgetFragment = new BudgetFragment(); // Khởi tạo fragment ngân sách

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_history) {
                loadFragment(historyFragment);
                return true;
            } else if (itemId == R.id.nav_statistics) {
                loadFragment(statisticsFragment);
                return true;
            } else if (itemId == R.id.nav_ai_insights) {
                loadFragment(aiInsightsFragment);
                return true;
            } else if (itemId == R.id.nav_budget) {
                loadFragment(budgetFragment);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        // --- Logic khởi tạo dữ liệu mẫu và listener ---
        // Nếu đây là lần chạy đầu tiên hoặc dữ liệu ứng dụng đã bị xóa, hãy xóa dữ liệu cũ trước
        // sau đó thêm dữ liệu mẫu và khởi động listener.
        if (!sharedPreferences.getBoolean(PREF_KEY_DATA_INITIALIZED, false)) {
            Log.d(TAG, "onCreate: First run detected or app data cleared. Deleting existing data before adding sample data.");
            deleteAllUserData(() -> {
                Log.d(TAG, "onCreate: Existing user data deleted. Initializing sample data and then starting listeners.");
                addNewSampleDataContent(() -> {
                    Log.d(TAG, "onCreate: Sample data initialization complete. Starting listeners now.");
                    listenForExpenses();
                    listenForBudgets();
                    sharedPreferences.edit().putBoolean(PREF_KEY_DATA_INITIALIZED, true).apply();
                    Toast.makeText(MainActivity.this, "Dữ liệu mẫu đã được thêm và listeners đã khởi tạo!", Toast.LENGTH_LONG).show();
                });
            });
        } else {
            Log.d(TAG, "onCreate: Data already initialized. Starting listeners.");
            listenForExpenses();
            listenForBudgets();
        }
        // ------------------------------------------
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (activeFragment != null && activeFragment != fragment) {
            fragmentTransaction.hide(activeFragment);
        }

        if (fragmentManager.findFragmentByTag(fragment.getClass().getSimpleName()) == null) {
            fragmentTransaction.add(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
        }

        fragmentTransaction.show(fragment);
        fragmentTransaction.commit();
        activeFragment = fragment;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_theme) {
            toggleTheme();
            return true;
        } else if (id == R.id.action_change_currency) {
            showCurrencyChangeDialog();
            return true;
        } else if (id == R.id.action_sign_out) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //region Firebase/Firestore Operations (Accessed by Fragments)
    public List<Expense> getExpenseList() {
        return expenseList;
    }

    public List<Budget> getBudgetList() {
        return budgetList;
    }

    public DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public String getCurrentCurrency() {
        return currentCurrency;
    }

    private void updateDecimalFormat() {
        if (currentCurrency.equals("VND")) {
            decimalFormat = new DecimalFormat("#,##0");
        } else {
            decimalFormat = new DecimalFormat("#,##0.00");
        }
        // Cập nhật adapter của các fragment nếu chúng đã được khởi tạo
        // Việc gọi updateUI ở đây là quan trọng để làm mới dữ liệu khi định dạng tiền tệ thay đổi
        if (homeFragment != null && homeFragment.isAdded()) homeFragment.updateUI();
        if (historyFragment != null && historyFragment.isAdded()) historyFragment.updateUI();
        if (statisticsFragment != null && statisticsFragment.isAdded()) statisticsFragment.updateUI();
        if (budgetFragment != null && budgetFragment.isAdded()) budgetFragment.updateUI(); // Cập nhật budget fragment
    }

    public void addExpense(Expense expense) {
        if (expense.getTimestamp() == null) {
            expense.setTimestamp(new Date());
            Log.d(TAG, "addExpense: Timestamp was null, setting to current Date for expense: " + expense.getDescription());
        }

        db.collection("users").document(userId).collection("expenses")
                .add(expense)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "addExpense: Successfully added expense: " + expense.getDescription() + " with ID: " + documentReference.getId());
                    Toast.makeText(MainActivity.this, "Đã thêm khoản chi/thu", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addExpense: Error adding expense: " + expense.getDescription(), e);
                    Toast.makeText(MainActivity.this, "Lỗi khi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void updateExpense(Expense expense) {
        if (expense.getId() == null) {
            Toast.makeText(this, "Không tìm thấy ID khoản mục để cập nhật.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference expenseRef = db.collection("users").document(userId)
                .collection("expenses").document(expense.getId());
        expenseRef.set(expense)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateExpense: Successfully updated expense: " + expense.getDescription() + " with ID: " + expense.getId());
                    Toast.makeText(MainActivity.this, "Đã cập nhật khoản chi/thu", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateExpense: Error updating expense: " + expense.getDescription(), e);
                    Toast.makeText(MainActivity.this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void deleteExpense(String expenseId) {
        db.collection("users").document(userId).collection("expenses").document(expenseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "deleteExpense: Successfully deleted expense with ID: " + expenseId);
                    Toast.makeText(MainActivity.this, "Đã xóa khoản chi/thu", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteExpense: Error deleting expense with ID: " + expenseId, e);
                    Toast.makeText(MainActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForExpenses() {
        Log.d(TAG, "listenForExpenses: Starting expense listener for userId: " + userId);
        db.collection("users").document(userId).collection("expenses")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "listenForExpenses: Listen failed for expenses.", error);
                        return;
                    }

                    Log.d(TAG, "listenForExpenses: Clearing expenseList (current size: " + expenseList.size() + ")");
                    expenseList.clear(); // Luôn xóa dữ liệu cũ trước khi thêm mới

                    if (value != null) {
                        Log.d(TAG, "listenForExpenses: Received " + value.size() + " expense documents from Firestore.");
                        for (QueryDocumentSnapshot doc : value) {
                            Expense expense = doc.toObject(Expense.class);
                            expense.setId(doc.getId());
                            expenseList.add(expense);
                            Log.d(TAG, "Fetched Expense: ID=" + expense.getId() + ", Desc=" + expense.getDescription() + ", Amt=" + expense.getAmount() + ", Time=" + expense.getTimestamp());
                        }
                    } else {
                        Log.d(TAG, "listenForExpenses: Received null value from Firestore snapshot.");
                    }

                    Log.d(TAG, "listenForExpenses: expenseList updated (new size: " + expenseList.size() + "). Notifying fragments.");
                    // Cập nhật UI của tất cả các fragment liên quan
                    if (homeFragment != null && homeFragment.isAdded()) homeFragment.updateUI();
                    if (historyFragment != null && historyFragment.isAdded()) historyFragment.updateUI();
                    if (statisticsFragment != null && statisticsFragment.isAdded()) statisticsFragment.updateUI();
                    if (budgetFragment != null && budgetFragment.isAdded()) budgetFragment.updateUI();
                });
    }

    // --- Phương thức quản lý Ngân sách ---
    public void addBudget(Budget budget) {
        db.collection("users").document(userId).collection("budgets")
                .add(budget)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Đã thêm ngân sách.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi khi thêm ngân sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error adding budget document", e);
                });
    }

    public void updateBudget(Budget budget) {
        if (budget.getId() == null) {
            Toast.makeText(this, "Không tìm thấy ID ngân sách để cập nhật.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference budgetRef = db.collection("users").document(userId)
                .collection("budgets").document(budget.getId());
        budgetRef.set(budget) // set() sẽ ghi đè toàn bộ tài liệu hiện có
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Đã cập nhật ngân sách.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi khi cập nhật ngân sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating budget document", e);
                });
    }

    public void deleteBudget(String budgetId) {
        db.collection("users").document(userId).collection("budgets").document(budgetId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Đã xóa ngân sách.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting document", e);
                });
    }

    private void listenForBudgets() {
        Log.d(TAG, "listenForBudgets: Starting budget listener for userId: " + userId);
        Calendar cal = Calendar.getInstance();
        long currentMonthYear = cal.get(Calendar.YEAR) * 100L + (cal.get(Calendar.MONTH) + 1);

        db.collection("users").document(userId).collection("budgets")
                .whereEqualTo("monthYear", currentMonthYear)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "listenForBudgets: Listen failed for budgets.", error);
                        return;
                    }

                    Log.d(TAG, "listenForBudgets: Clearing budgetList (current size: " + budgetList.size() + ")");
                    budgetList.clear();
                    if (value != null) {
                        Log.d(TAG, "listenForBudgets: Received " + value.size() + " budget documents from Firestore.");
                        for (QueryDocumentSnapshot doc : value) {
                            Budget budget = doc.toObject(Budget.class);
                            budget.setId(doc.getId());
                            budgetList.add(budget);
                            Log.d(TAG, "Fetched Budget: ID=" + budget.getId() + ", Category=" + budget.getCategory() + ", Amount=" + budget.getAmount());
                        }
                    } else {
                        Log.d(TAG, "listenForBudgets: Received null value from Firestore snapshot.");
                    }
                    Log.d(TAG, "listenForBudgets: budgetList updated (new size: " + budgetList.size() + "). Notifying budget fragment.");
                    if (budgetFragment != null && budgetFragment.isAdded()) budgetFragment.updateUI();
                });
    }
    //endregion

    //region Theme and Currency Management
    private void toggleTheme() {
        Log.d(TAG, "toggleTheme: Attempting to toggle theme.");
        // Đóng overflow menu trước khi recreate() để tránh WindowLeaked
        closeOptionsMenu();
        Log.d(TAG, "toggleTheme: Overflow menu closed.");

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int newThemeMode;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            newThemeMode = AppCompatDelegate.MODE_NIGHT_NO;
            Log.d(TAG, "toggleTheme: Current is NIGHT, setting to DAY.");
        } else {
            newThemeMode = AppCompatDelegate.MODE_NIGHT_YES;
            Log.d(TAG, "toggleTheme: Current is DAY, setting to NIGHT.");
        }
        AppCompatDelegate.setDefaultNightMode(newThemeMode);
        sharedPreferences.edit().putInt("theme_mode", newThemeMode).apply();
        // Cần gọi recreate() để áp dụng theme mới ngay lập tức
        recreate();
        Log.d(TAG, "toggleTheme: Calling recreate() to apply new theme.");
    }

    private void showCurrencyChangeDialog() {
        final String[] currencies = {"VND", "USD", "EUR"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn đơn vị tiền tệ")
                .setItems(currencies, (dialog, which) -> {
                    String selectedCurrency = currencies[which];
                    if (!currentCurrency.equals(selectedCurrency)) {
                        currentCurrency = selectedCurrency;
                        sharedPreferences.edit().putString("currency", currentCurrency).apply();
                        updateDecimalFormat();
                        Toast.makeText(MainActivity.this, "Đơn vị tiền tệ đã thay đổi thành " + currentCurrency, Toast.LENGTH_SHORT).show();
                    }
                });
        currencyDialog = builder.create(); // Lưu tham chiếu đến dialog
        currencyDialog.show();
    }
    //endregion

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(MainActivity.this, "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Đảm bảo dismiss dialog để tránh Window Leaked
        if (currencyDialog != null && currencyDialog.isShowing()) {
            currencyDialog.dismiss();
            Log.d(TAG, "onDestroy: currencyDialog dismissed.");
        }
        Log.d(TAG, "onDestroy: MainActivity destroyed.");
    }

    /**
     * Xóa tất cả dữ liệu chi tiêu và ngân sách của người dùng hiện tại từ Firestore.
     * Phương thức này được gọi khi ứng dụng chạy lần đầu hoặc dữ liệu đã bị xóa,
     * để đảm bảo một bộ dữ liệu mẫu sạch.
     *
     * @param onCompleteCallback Callback sẽ được gọi khi tất cả các thao tác xóa hoàn tất.
     */
    private void deleteAllUserData(final Runnable onCompleteCallback) {
        Log.d(TAG, "deleteAllUserData: Đang xóa tất cả chi tiêu và ngân sách cho người dùng: " + userId);
        List<Task<Void>> deleteTasks = new ArrayList<>();

        // Lấy và thêm các tác vụ xóa cho tất cả các khoản chi tiêu
        db.collection("users").document(userId).collection("expenses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            deleteTasks.add(document.getReference().delete());
                        }
                        Log.d(TAG, "deleteAllUserData: Đã thêm " + task.getResult().size() + " tác vụ xóa chi tiêu.");
                    } else {
                        Log.e(TAG, "deleteAllUserData: Lỗi khi lấy chi tiêu để xóa.", task.getException());
                    }

                    // Lấy và thêm các tác vụ xóa cho tất cả các ngân sách
                    db.collection("users").document(userId).collection("budgets")
                            .get()
                            .addOnCompleteListener(budgetTask -> {
                                if (budgetTask.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : budgetTask.getResult()) {
                                        deleteTasks.add(document.getReference().delete());
                                    }
                                    Log.d(TAG, "deleteAllUserData: Đã thêm " + budgetTask.getResult().size() + " tác vụ xóa ngân sách.");
                                } else {
                                    Log.e(TAG, "deleteAllUserData: Lỗi khi lấy ngân sách để xóa.", budgetTask.getException());
                                }

                                // Đợi cho tất cả các tác vụ xóa hoàn tất
                                if (!deleteTasks.isEmpty()) {
                                    Tasks.whenAllComplete(deleteTasks)
                                            .addOnCompleteListener(allTasks -> {
                                                Log.d(TAG, "deleteAllUserData: Tất cả tác vụ xóa đã hoàn thành.");
                                                if (onCompleteCallback != null) {
                                                    onCompleteCallback.run();
                                                }
                                            });
                                } else {
                                    Log.d(TAG, "deleteAllUserData: Không có dữ liệu để xóa.");
                                    if (onCompleteCallback != null) {
                                        onCompleteCallback.run();
                                    }
                                }
                            });
                });
    }

    private void addNewSampleDataContent(final Runnable onAllAddedCallback) {
        Log.d(TAG, "addNewSampleDataContent: Bắt đầu thêm nội dung dữ liệu mẫu.");

        List<Task<DocumentReference>> addTasks = new ArrayList<>();

        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Lương tháng 6", 15000000, "income", "Lương")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Tiền thưởng", 2000000, "income", "Thưởng")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Tiền ăn uống", 3000000, "expense", "Ăn uống")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Tiền thuê nhà", 4000000, "expense", "Nhà ở")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Điện nước", 800000, "expense", "Hóa đơn")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Mua sắm quần áo", 1200000, "expense", "Mua sắm")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Phí di chuyển", 500000, "expense", "Đi lại")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Giải trí cuối tuần", 700000, "expense", "Giải trí")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Mua sách", 300000, "expense", "Giáo dục")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Quà sinh nhật", 450000, "expense", "Quà tặng")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Internet", 200000, "expense", "Hóa đơn")));
        addTasks.add(db.collection("users").document(userId).collection("expenses").add(new Expense("Gửi xe", 100000, "expense", "Đi lại")));

        Calendar cal = Calendar.getInstance();
        long currentMonthYear = cal.get(Calendar.YEAR) * 100L + (cal.get(Calendar.MONTH) + 1);

        addTasks.add(db.collection("users").document(userId).collection("budgets").add(new Budget("Ăn uống", 4000000, currentMonthYear)));
        addTasks.add(db.collection("users").document(userId).collection("budgets").add(new Budget("Nhà ở", 4500000, currentMonthYear)));
        addTasks.add(db.collection("users").document(userId).collection("budgets").add(new Budget("Đi lại", 700000, currentMonthYear)));
        addTasks.add(db.collection("users").document(userId).collection("budgets").add(new Budget("Mua sắm", 1500000, currentMonthYear)));
        addTasks.add(db.collection("users").document(userId).collection("budgets").add(new Budget("Giải trí", 1000000, currentMonthYear)));

        Tasks.whenAllComplete(addTasks)
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "addNewSampleDataContent: Tất cả dữ liệu mẫu đã được thêm (hoặc cố gắng thêm).");
                    if (onAllAddedCallback != null) {
                        onAllAddedCallback.run();
                    }
                });
    }
}
