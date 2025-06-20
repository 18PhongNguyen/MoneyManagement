package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.BudgetAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Budget;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetFragment extends Fragment {

    private static final String TAG = "BudgetFragment";
    private Spinner spinnerBudgetCategory;
    private EditText etBudgetAmount, etBudgetCustomCategory;
    private Button btnAddBudget;
    private RecyclerView rvBudgets;
    private TextView tvEmptyBudgetState;

    private BudgetAdapter budgetAdapter;
    private MainActivity activity;

    private ArrayAdapter<String> categoryAdapter;
    private List<String> expenseCategories; // Chỉ dùng danh mục chi tiêu cho ngân sách

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
        Log.d(TAG, "onCreateView: BudgetFragment created.");
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        spinnerBudgetCategory = view.findViewById(R.id.spinner_budget_category);
        etBudgetAmount = view.findViewById(R.id.et_budget_amount);
        etBudgetCustomCategory = view.findViewById(R.id.et_budget_custom_category);
        btnAddBudget = view.findViewById(R.id.btn_add_budget);
        rvBudgets = view.findViewById(R.id.rv_budgets);
        tvEmptyBudgetState = view.findViewById(R.id.tv_empty_budget_state);

        // Khởi tạo danh sách danh mục chi tiêu
        expenseCategories = new ArrayList<>(List.of(getResources().getStringArray(R.array.expense_categories)));
        expenseCategories.add("Thêm danh mục mới..."); // Tùy chọn thêm danh mục mới

        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, expenseCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudgetCategory.setAdapter(categoryAdapter);
        // Đặt mặc định chọn mục "Khác"
        spinnerBudgetCategory.setSelection(expenseCategories.indexOf("Khác") != -1 ? expenseCategories.indexOf("Khác") : 0);


        spinnerBudgetCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                if (selectedCategory.equals("Thêm danh mục mới...")) {
                    etBudgetCustomCategory.setVisibility(View.VISIBLE);
                    etBudgetCustomCategory.requestFocus();
                } else {
                    etBudgetCustomCategory.setVisibility(View.GONE);
                    etBudgetCustomCategory.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });


        budgetAdapter = new BudgetAdapter(activity.getBudgetList(), activity, activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(budgetAdapter);

        btnAddBudget.setOnClickListener(v -> addBudget());

        updateUI(); // Cập nhật UI ban đầu

        return view;
    }

    private void addBudget() {
        // Lấy danh mục từ Spinner hoặc EditText tùy chỉnh
        String category;
        if (etBudgetCustomCategory.getVisibility() == View.VISIBLE && !etBudgetCustomCategory.getText().toString().trim().isEmpty()) {
            category = etBudgetCustomCategory.getText().toString().trim();
        } else if (spinnerBudgetCategory.getSelectedItem() != null && !spinnerBudgetCategory.getSelectedItem().toString().equals("Thêm danh mục mới...")) {
            category = spinnerBudgetCategory.getSelectedItem().toString();
        } else {
            Snackbar.make(requireView(), "Vui lòng chọn hoặc nhập danh mục.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etBudgetAmount.getText().toString().trim();

        if (category.isEmpty() || amountStr.isEmpty()) {
            Snackbar.make(requireView(), "Vui lòng điền đầy đủ thông tin.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Snackbar.make(requireView(), "Ngân sách phải là số dương.", Snackbar.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Snackbar.make(requireView(), "Số tiền không hợp lệ.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        long currentMonthYear = cal.get(Calendar.YEAR) * 100L + (cal.get(Calendar.MONTH) + 1);

        Budget newBudget = new Budget(category, amount, currentMonthYear);
        activity.addBudget(newBudget); // Gọi hàm thêm trong MainActivity

        clearInputFields();
        Snackbar.make(requireView(), "Đã thêm ngân sách.", Snackbar.LENGTH_SHORT).show();
    }

    private void clearInputFields() {
        spinnerBudgetCategory.setSelection(0); // Reset spinner
        etBudgetCustomCategory.setText("");
        etBudgetCustomCategory.setVisibility(View.GONE);
        etBudgetAmount.setText("");
    }

    public void updateUI() {
        Log.d(TAG, "updateUI: Called for BudgetFragment.");
        if (activity == null || !isAdded()) {
            Log.w(TAG, "updateUI: BudgetFragment not attached or not added. Skipping UI update.");
            return;
        }

        List<Budget> currentBudgets = activity.getBudgetList();
        List<Expense> currentExpenses = activity.getExpenseList();

        // Tính toán tổng chi tiêu theo danh mục cho tháng hiện tại
        Map<String, Double> currentMonthExpensesByCategory = new HashMap<>();
        Calendar currentCal = Calendar.getInstance();
        int currentMonth = currentCal.get(Calendar.MONTH);
        int currentYear = currentCal.get(Calendar.YEAR);

        for (Expense expense : currentExpenses) {
            if (expense.getTimestamp() != null && expense.getType().equals("expense")) {
                Calendar expenseCal = Calendar.getInstance();
                expenseCal.setTime(expense.getTimestamp());
                if (expenseCal.get(Calendar.MONTH) == currentMonth && expenseCal.get(Calendar.YEAR) == currentYear) {
                    String category = expense.getCategory();
                    currentMonthExpensesByCategory.put(category, currentMonthExpensesByCategory.getOrDefault(category, 0.0) + expense.getAmount());
                }
            }
        }

        budgetAdapter.setCurrentMonthExpensesByCategory(currentMonthExpensesByCategory); // Cập nhật chi tiêu cho adapter
        budgetAdapter.setDecimalFormat(activity.getDecimalFormat());
        budgetAdapter.setCurrentCurrency(activity.getCurrentCurrency());
        budgetAdapter.notifyDataSetChanged();

        // Xử lý trạng thái rỗng
        if (currentBudgets.isEmpty()) {
            rvBudgets.setVisibility(View.GONE);
            tvEmptyBudgetState.setVisibility(View.VISIBLE);
        } else {
            rvBudgets.setVisibility(View.VISIBLE);
            tvEmptyBudgetState.setVisibility(View.GONE);
        }
    }
}
