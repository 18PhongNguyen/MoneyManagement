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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner; // Import Spinner
import android.widget.TextView;
import android.widget.Toast; // Vẫn dùng Toast cho các thông báo ngắn gọn

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
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

    private static final String TAG = "HomeFragment";
    private EditText etDescription, etAmount, etCustomCategory; // etCategory đã được thay bằng spinner
    private RadioGroup rgType;
    private RadioButton rbIncome, rbExpense;
    private Spinner spinnerCategory; // Spinner cho danh mục
    private Button btnAddExpense;
    private TextView tvAccountBalance, tvIncomeTotal, tvExpenseTotal;
    private RecyclerView rvTopExpenses;
    private CategorySummaryAdapter categorySummaryAdapter;
    private List<CategorySummary> topExpensesList;
    private FloatingActionButton fabAddTransaction;

    private MainActivity activity;
    private ArrayAdapter<String> categoryAdapter;
    private List<String> expenseCategories;
    private List<String> incomeCategories;


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
        rgType = view.findViewById(R.id.rg_type);
        rbIncome = view.findViewById(R.id.rb_income);
        rbExpense = view.findViewById(R.id.rb_expense);
        spinnerCategory = view.findViewById(R.id.spinner_category); // Ánh xạ Spinner
        etCustomCategory = view.findViewById(R.id.et_custom_category); // Ánh xạ EditText cho danh mục tùy chỉnh
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

        // Khởi tạo danh sách danh mục từ strings.xml
        expenseCategories = new ArrayList<>(List.of(getResources().getStringArray(R.array.expense_categories)));
        incomeCategories = new ArrayList<>(List.of(getResources().getStringArray(R.array.income_categories)));
        // Thêm tùy chọn "Thêm danh mục mới"
        expenseCategories.add("Thêm danh mục mới...");
        incomeCategories.add("Thêm danh mục mới...");

        // Thiết lập Adapter cho Spinner dựa trên lựa chọn loại giao dịch ban đầu
        updateCategorySpinner(rbIncome.isChecked() ? incomeCategories : expenseCategories);


        // Lắng nghe thay đổi của RadioGroup
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_income) {
                updateCategorySpinner(incomeCategories);
            } else {
                updateCategorySpinner(expenseCategories);
            }
        });

        // Lắng nghe sự kiện chọn danh mục từ Spinner
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                if (selectedCategory.equals("Thêm danh mục mới...")) {
                    etCustomCategory.setVisibility(View.VISIBLE);
                    etCustomCategory.requestFocus();
                } else {
                    etCustomCategory.setVisibility(View.GONE);
                    etCustomCategory.setText(""); // Clear custom field if not 'Add new'
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });


        // Lắng nghe sự kiện thêm khoản chi/thu
        btnAddExpense.setOnClickListener(v -> addOrUpdateExpense());
        fabAddTransaction.setOnClickListener(v -> {
            clearInputFields();
            Snackbar.make(view, "Nhập thông tin giao dịch mới", Snackbar.LENGTH_SHORT).show();
        });

        return view;
    }

    private void updateCategorySpinner(List<String> categories) {
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        // Đặt mặc định chọn mục đầu tiên (hoặc "Khác")
        spinnerCategory.setSelection(categories.indexOf("Khác") != -1 ? categories.indexOf("Khác") : 0);
        etCustomCategory.setVisibility(View.GONE); // Ẩn trường tùy chỉnh khi chuyển loại
        etCustomCategory.setText("");
    }

    // Phương thức để thêm hoặc cập nhật một khoản chi/thu
    private void addOrUpdateExpense() {
        String description = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        // Lấy loại giao dịch từ RadioGroup
        int selectedTypeId = rgType.getCheckedRadioButtonId();
        String type;
        if (selectedTypeId == R.id.rb_income) {
            type = "income";
        } else if (selectedTypeId == R.id.rb_expense) {
            type = "expense";
        } else {
            Snackbar.make(requireView(), "Vui lòng chọn loại giao dịch (Thu/Chi)", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Lấy danh mục từ Spinner hoặc EditText tùy chỉnh
        String category;
        if (etCustomCategory.getVisibility() == View.VISIBLE && !etCustomCategory.getText().toString().trim().isEmpty()) {
            category = etCustomCategory.getText().toString().trim();
        } else if (spinnerCategory.getSelectedItem() != null && !spinnerCategory.getSelectedItem().toString().equals("Thêm danh mục mới...")) {
            category = spinnerCategory.getSelectedItem().toString();
        } else {
            Snackbar.make(requireView(), "Vui lòng chọn hoặc nhập danh mục", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
            Snackbar.make(requireView(), "Vui lòng điền đầy đủ thông tin", Snackbar.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Snackbar.make(requireView(), "Số tiền không hợp lệ.", Snackbar.LENGTH_SHORT).show();
            return;
        }


        Expense newExpense = new Expense(description, amount, type.toLowerCase(), category);
        activity.addExpense(newExpense); // Gọi hàm thêm trong MainActivity

        clearInputFields();
    }

    // Xóa nội dung trong các trường nhập liệu
    private void clearInputFields() {
        etDescription.setText("");
        etAmount.setText("");
        rgType.check(R.id.rb_income); // Đặt mặc định là Thu nhập
        spinnerCategory.setSelection(0); // Đặt lại Spinner về mục đầu tiên
        etCustomCategory.setText("");
        etCustomCategory.setVisibility(View.GONE);
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
