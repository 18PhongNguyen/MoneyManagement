package vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View; // Import View for Snackbar

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar; // Import Snackbar
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private List<Expense> expenseList;
    private List<Budget> budgetList;
    private SharedPreferences sharedPreferences;
    private String currentCurrency = "VND";
    private DecimalFormat decimalFormat;

    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private StatisticsFragment statisticsFragment;
    private AiInsightsFragment aiInsightsFragment;
    private BudgetFragment budgetFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cố định ứng dụng ở chế độ sáng (Light Mode)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        updateDecimalFormat();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = currentUser.getUid();
        Log.d(TAG, "onCreate: Current User ID: " + userId);

        expenseList = new ArrayList<>();
        budgetList = new ArrayList<>();

        homeFragment = new HomeFragment();
        historyFragment = new HistoryFragment();
        statisticsFragment = new StatisticsFragment();
        aiInsightsFragment = new AiInsightsFragment();
        budgetFragment = new BudgetFragment();

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

        Log.d(TAG, "onCreate: Starting Firestore listeners to fetch existing data.");
        listenForExpenses();
        listenForBudgets();
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Thêm hoạt ảnh chuyển đổi Fragment
        fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right,  // enter
                R.anim.slide_out_left,  // exit
                R.anim.slide_in_left,   // popEnter
                R.anim.slide_out_right  // popExit
        );

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
        if (id == R.id.action_sign_out) {
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
        decimalFormat = new DecimalFormat("#,##0");
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
                    Snackbar.make(findViewById(android.R.id.content), "Đã thêm khoản chi/thu", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addExpense: Error adding expense: " + expense.getDescription(), e);
                    Snackbar.make(findViewById(android.R.id.content), "Lỗi khi thêm: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    public void updateExpense(Expense expense) {
        if (expense.getId() == null) {
            Snackbar.make(findViewById(android.R.id.content), "Không tìm thấy ID khoản mục để cập nhật.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        DocumentReference expenseRef = db.collection("users").document(userId)
                .collection("expenses").document(expense.getId());
        expenseRef.set(expense)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateExpense: Successfully updated expense: " + expense.getDescription() + " with ID: " + expense.getId());
                    Snackbar.make(findViewById(android.R.id.content), "Đã cập nhật khoản chi/thu", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateExpense: Error updating expense: " + expense.getDescription(), e);
                    Snackbar.make(findViewById(android.R.id.content), "Lỗi khi cập nhật: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    public void deleteExpense(String expenseId) {
        db.collection("users").document(userId).collection("expenses").document(expenseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "deleteExpense: Successfully deleted expense with ID: " + expenseId);
                    Snackbar.make(findViewById(android.R.id.content), "Đã xóa khoản chi/thu", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteExpense: Error deleting expense with ID: " + expenseId, e);
                    Snackbar.make(findViewById(android.R.id.content), "Lỗi khi xóa: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
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
                    expenseList.clear();

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
                    if (homeFragment != null && homeFragment.isAdded()) homeFragment.updateUI();
                    if (historyFragment != null && historyFragment.isAdded()) historyFragment.updateUI();
                    if (statisticsFragment != null && statisticsFragment.isAdded()) statisticsFragment.updateUI();
                    if (budgetFragment != null && budgetFragment.isAdded()) budgetFragment.updateUI();
                });
    }

    public void addBudget(Budget budget) {
        db.collection("users").document(userId).collection("budgets")
                .add(budget)
                .addOnSuccessListener(documentReference -> {
                    Snackbar.make(findViewById(android.R.id.content), "Đã thêm ngân sách.", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(findViewById(android.R.id.content), "Lỗi khi thêm ngân sách: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    Log.e(TAG, "Error adding budget document", e);
                });
    }

    public void updateBudget(Budget budget) {
        if (budget.getId() == null) {
            Snackbar.make(findViewById(android.R.id.content), "Không tìm thấy ID ngân sách để cập nhật.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        DocumentReference budgetRef = db.collection("users").document(userId)
                .collection("budgets").document(budget.getId());
        budgetRef.set(budget)
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã cập nhật ngân sách.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Snackbar.make(findViewById(android.R.id.content), "Lỗi khi cập nhật ngân sách: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating budget document", e);
                });
    }

    public void deleteBudget(String budgetId) {
        db.collection("users").document(userId).collection("budgets").document(budgetId)
                .delete()
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã xóa ngân sách.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Snackbar.make(findViewById(android.R.id.content), "Lỗi khi xóa: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
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

    //region Sign Out
    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Snackbar.make(findViewById(android.R.id.content), "Đã đăng xuất.", Snackbar.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
    //endregion

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MainActivity destroyed.");
    }
}
