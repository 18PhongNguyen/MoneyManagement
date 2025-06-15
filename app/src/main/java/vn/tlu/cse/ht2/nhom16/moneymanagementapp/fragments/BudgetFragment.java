package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log; // Import Log class
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.BudgetAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Budget;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

public class BudgetFragment extends Fragment {

    private static final String TAG = "BudgetFragment"; // Tag for Logcat

    private EditText etBudgetCategory, etBudgetAmount;
    private Button btnAddBudget;
    private RecyclerView rvBudgets;
    private BudgetAdapter budgetAdapter;
    private List<Budget> budgetList;

    private MainActivity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString() + " must be MainActivity");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        etBudgetCategory = view.findViewById(R.id.et_budget_category);
        etBudgetAmount = view.findViewById(R.id.et_budget_amount);
        btnAddBudget = view.findViewById(R.id.btn_add_budget);
        rvBudgets = view.findViewById(R.id.rv_budgets);

        budgetList = new ArrayList<>();
        budgetAdapter = new BudgetAdapter(budgetList, activity, activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(budgetAdapter);

        btnAddBudget.setOnClickListener(v -> addBudget());

        // Cập nhật UI với dữ liệu hiện có
        updateUI();

        return view;
    }

    private void addBudget() {
        String category = etBudgetCategory.getText().toString().trim();
        String amountStr = etBudgetAmount.getText().toString().trim();

        if (category.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(activity, "Vui lòng điền đầy đủ thông tin ngân sách.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        // Lấy tháng và năm hiện tại để gán cho ngân sách
        Calendar cal = Calendar.getInstance();
        long monthYear = cal.get(Calendar.YEAR) * 100L + (cal.get(Calendar.MONTH) + 1); // Format YYYYMM

        Budget newBudget = new Budget(category, amount, monthYear);
        activity.addBudget(newBudget); // Gọi hàm thêm ngân sách trong MainActivity

        clearInputFields();
    }

    private void clearInputFields() {
        etBudgetCategory.setText("");
        etBudgetAmount.setText("");
    }

    public void updateUI() {
        if (activity == null || !isAdded()) {
            Log.d(TAG, "updateUI: Fragment not attached or not added yet.");
            return;
        }

        // Lấy danh sách ngân sách từ MainActivity
        budgetList.clear();
        budgetList.addAll(activity.getBudgetList());
        Log.d(TAG, "updateUI: Number of budgets loaded: " + budgetList.size());
        for (Budget b : budgetList) {
            Log.d(TAG, "  Budget: " + b.getCategory() + ", Amount: " + b.getAmount() + ", MonthYear: " + b.getMonthYear());
        }


        // Tính toán tổng chi tiêu theo danh mục cho tháng hiện tại
        Map<String, Double> currentMonthExpensesByCategory = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        long currentMonthYear = cal.get(Calendar.YEAR) * 100L + (cal.get(Calendar.MONTH) + 1);
        Log.d(TAG, "updateUI: Current MonthYear for expenses: " + currentMonthYear);


        List<Expense> allExpenses = activity.getExpenseList();
        Log.d(TAG, "updateUI: Total expenses available: " + allExpenses.size());

        for (Expense expense : allExpenses) {
            if (expense.getType().equals("expense") && expense.getTimestamp() != null) {
                Calendar expenseCal = Calendar.getInstance();
                expenseCal.setTime(expense.getTimestamp());
                long expenseMonthYear = expenseCal.get(Calendar.YEAR) * 100L + (expenseCal.get(Calendar.MONTH) + 1);

                Log.d(TAG, "  Processing expense: " + expense.getDescription() + ", Type: " + expense.getType() + ", Category: " + expense.getCategory() + ", Amount: " + expense.getAmount() + ", Timestamp: " + expense.getTimestamp() + ", MonthYear: " + expenseMonthYear);

                if (expenseMonthYear == currentMonthYear) {
                    String category = expense.getCategory();
                    currentMonthExpensesByCategory.put(category, currentMonthExpensesByCategory.getOrDefault(category, 0.0) + expense.getAmount());
                    Log.d(TAG, "    Matched expense to current month. Category: " + category + ", Current total: " + currentMonthExpensesByCategory.get(category));
                }
            } else {
                Log.d(TAG, "  Skipped expense (not expense type or null timestamp): " + expense.getDescription());
            }
        }
        Log.d(TAG, "updateUI: Calculated expenses by category for current month: " + currentMonthExpensesByCategory);


        // Cập nhật Adapter
        budgetAdapter.setDecimalFormat(activity.getDecimalFormat());
        budgetAdapter.setCurrentCurrency(activity.getCurrentCurrency());
        budgetAdapter.setCurrentMonthExpensesByCategory(currentMonthExpensesByCategory); // Truyền dữ liệu chi tiêu
        budgetAdapter.notifyDataSetChanged();
        Log.d(TAG, "updateUI: Budget adapter notified.");
    }
}
