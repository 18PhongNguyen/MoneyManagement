package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log; // Import Log for debugging
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton; // Import RadioButton
import android.widget.RadioGroup; // Import RadioGroup
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity; // Để gọi hàm từ MainActivity
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.CategorySummaryAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.CategorySummary;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment"; // Tag cho Logcat
    private EditText etDescription, etAmount, etCategory;
    private RadioGroup rgType; // Thay thế EditText etType bằng RadioGroup
    private RadioButton rbIncome, rbExpense; // Thêm RadioButton cho income và expense
    private Button btnAddExpense;
    private TextView tvAccountBalance, tvIncomeTotal, tvExpenseTotal;
    private RecyclerView rvTopExpenses;
    private CategorySummaryAdapter categorySummaryAdapter;
    private List<CategorySummary> topExpensesList;
    private FloatingActionButton fabAddTransaction;

    private MainActivity activity; // Tham chiếu đến MainActivity để tương tác

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
        Log.d(TAG, "onCreateView: HomeFragment created.");
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Ánh xạ các View
        etDescription = view.findViewById(R.id.et_description);
        etAmount = view.findViewById(R.id.et_amount);
        etCategory = view.findViewById(R.id.et_category);
        rgType = view.findViewById(R.id.rg_type); // Ánh xạ RadioGroup
        rbIncome = view.findViewById(R.id.rb_income); // Ánh xạ RadioButton Income
        rbExpense = view.findViewById(R.id.rb_expense); // Ánh xạ RadioButton Expense
        btnAddExpense = view.findViewById(R.id.btn_add_expense);
        tvAccountBalance = view.findViewById(R.id.tv_account_balance);
        tvIncomeTotal = view.findViewById(R.id.tv_income_total);
        tvExpenseTotal = view.findViewById(R.id.tv_expense_total);
        rvTopExpenses = view.findViewById(R.id.rv_top_expenses);
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction);

        topExpensesList = new ArrayList<>();
        categorySummaryAdapter = new CategorySummaryAdapter(topExpensesList, activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvTopExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTopExpenses.setAdapter(categorySummaryAdapter);

        // Lắng nghe sự kiện thêm khoản chi/thu
        btnAddExpense.setOnClickListener(v -> addOrUpdateExpense());
        fabAddTransaction.setOnClickListener(v -> {
            // Có thể hiển thị một dialog hoặc mở một activity mới để thêm giao dịch
            // Hoặc đơn giản là cuộn lên phần nhập liệu và reset các trường
            etDescription.requestFocus();
            clearInputFields();
            Toast.makeText(activity, "Nhập thông tin giao dịch mới", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // Phương thức để thêm hoặc cập nhật một khoản chi/thu
    private void addOrUpdateExpense() {
        String description = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = etCategory.getText().toString().trim();

        // Lấy loại giao dịch từ RadioGroup
        int selectedTypeId = rgType.getCheckedRadioButtonId();
        String type;
        if (selectedTypeId == -1) {
            Toast.makeText(activity, "Vui lòng chọn loại giao dịch (Thu/Chi)", Toast.LENGTH_SHORT).show();
            return;
        } else if (selectedTypeId == R.id.rb_income) {
            type = "income";
        } else { // R.id.rb_expense
            type = "expense";
        }

        if (description.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(activity, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        Expense newExpense = new Expense(description, amount, type.toLowerCase(), category);
        activity.addExpense(newExpense); // Gọi hàm thêm trong MainActivity

        clearInputFields();
    }

    // Xóa nội dung trong các trường nhập liệu
    private void clearInputFields() {
        etDescription.setText("");
        etAmount.setText("");
        etCategory.setText("");
        rgType.clearCheck(); // Xóa lựa chọn của RadioGroup
    }

    // Cập nhật giao diện người dùng dựa trên dữ liệu mới
    public void updateUI() {
        Log.d(TAG, "updateUI: Called for HomeFragment.");
        if (activity == null || !isAdded()) {
            Log.w(TAG, "updateUI: HomeFragment not attached or not added. Skipping UI update.");
            return;
        }

        List<Expense> currentExpenses = activity.getExpenseList();
        Log.d(TAG, "updateUI: currentExpenses size from MainActivity: " + (currentExpenses != null ? currentExpenses.size() : "null"));

        DecimalFormat decimalFormat = activity.getDecimalFormat();
        String currentCurrency = activity.getCurrentCurrency();

        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseCategoryData = new HashMap<>();

        if (currentExpenses != null) {
            for (Expense expense : currentExpenses) {
                if (expense.getType().equals("income")) {
                    totalIncome += expense.getAmount();
                } else if (expense.getType().equals("expense")) {
                    totalExpense += expense.getAmount();
                    String category = expense.getCategory();
                    expenseCategoryData.put(category, expenseCategoryData.getOrDefault(category, 0.0) + expense.getAmount());
                }
            }
        } else {
            Log.w(TAG, "updateUI: currentExpenses list is null in HomeFragment.");
        }


        double balance = totalIncome - totalExpense;
        tvAccountBalance.setText(String.format("%s %s", decimalFormat.format(balance), currentCurrency));
        tvIncomeTotal.setText(String.format("Tổng thu: %s %s", decimalFormat.format(totalIncome), currentCurrency));
        tvExpenseTotal.setText(String.format("Tổng chi: %s %s", decimalFormat.format(totalExpense), currentCurrency));
        Log.d(TAG, "updateUI: HomeFragment totals - Income: " + totalIncome + ", Expense: " + totalExpense + ", Balance: " + balance);


        // Cập nhật Top Expenses
        topExpensesList.clear();
        double finalTotalExpense = totalExpense; // Tạo biến final để dùng trong lambda
        expenseCategoryData.forEach((category, amount) -> {
            double percentage = (finalTotalExpense > 0) ? (amount / finalTotalExpense) * 100 : 0;
            topExpensesList.add(new CategorySummary(category, percentage, amount));
        });
        Log.d(TAG, "updateUI: HomeFragment topExpensesList size: " + topExpensesList.size());


        // Sắp xếp theo số tiền giảm dần
        Collections.sort(topExpensesList, (o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));

        categorySummaryAdapter.setDecimalFormat(decimalFormat); // Cập nhật định dạng tiền tệ cho adapter
        categorySummaryAdapter.setCurrentCurrency(currentCurrency); // Cập nhật đơn vị tiền tệ cho adapter
        categorySummaryAdapter.notifyDataSetChanged();
    }
}
