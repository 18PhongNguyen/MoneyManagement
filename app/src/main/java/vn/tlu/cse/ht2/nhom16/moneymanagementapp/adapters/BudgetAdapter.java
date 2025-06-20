package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Budget;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgetList;
    private Context context;
    private DecimalFormat decimalFormat;
    private String currentCurrency;
    private Map<String, Double> currentMonthExpensesByCategory;

    public BudgetAdapter(List<Budget> budgetList, Context context, DecimalFormat decimalFormat, String currentCurrency) {
        this.budgetList = budgetList;
        this.context = context;
        this.decimalFormat = decimalFormat;
        this.currentCurrency = currentCurrency;
        this.currentMonthExpensesByCategory = new HashMap<>();
    }

    public void setDecimalFormat(DecimalFormat newFormat) {
        this.decimalFormat = newFormat;
    }

    public void setCurrentCurrency(String newCurrency) {
        this.currentCurrency = newCurrency;
    }

    public void setCurrentMonthExpensesByCategory(Map<String, Double> expenses) {
        this.currentMonthExpensesByCategory = expenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);
        holder.tvBudgetCategoryName.setText(budget.getCategory());
        holder.tvBudgetAmount.setText(String.format("%s %s", decimalFormat.format(budget.getAmount()), currentCurrency));

        double spentAmount = currentMonthExpensesByCategory.getOrDefault(budget.getCategory(), 0.0);
        holder.tvBudgetSpent.setText(String.format("%s %s", decimalFormat.format(spentAmount), currentCurrency));

        int progress = 0;
        if (budget.getAmount() > 0) {
            progress = (int) ((spentAmount / budget.getAmount()) * 100);
            if (progress > 100) progress = 100;
        }
        holder.pbBudgetProgress.setProgress(progress);

        if (spentAmount > budget.getAmount()) {
            holder.tvBudgetRemaining.setTextColor(context.getResources().getColor(R.color.red_expense));
            holder.tvBudgetRemaining.setText("Vượt ngân sách: " + decimalFormat.format(spentAmount - budget.getAmount()) + " " + currentCurrency);
            holder.pbBudgetProgress.setProgressTintList(context.getResources().getColorStateList(R.color.red_expense));
        } else {
            holder.tvBudgetRemaining.setTextColor(context.getResources().getColor(android.R.color.tab_indicator_text));
            holder.tvBudgetRemaining.setText("Còn lại: " + decimalFormat.format(budget.getAmount() - spentAmount) + " " + currentCurrency);
            holder.pbBudgetProgress.setProgressTintList(context.getResources().getColorStateList(R.color.green_income));
        }

        holder.btnEditBudget.setOnClickListener(v -> showEditDialog(budget));
        holder.btnDeleteBudget.setOnClickListener(v -> {
            ((MainActivity) context).deleteBudget(budget.getId());
            Snackbar.make(holder.itemView, "Đã xóa ngân sách", Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvBudgetCategoryName, tvBudgetAmount, tvBudgetSpent, tvBudgetRemaining;
        ProgressBar pbBudgetProgress;
        Button btnEditBudget, btnDeleteBudget;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBudgetCategoryName = itemView.findViewById(R.id.tv_budget_category_name);
            tvBudgetAmount = itemView.findViewById(R.id.tv_budget_amount);
            tvBudgetSpent = itemView.findViewById(R.id.tv_budget_spent);
            tvBudgetRemaining = itemView.findViewById(R.id.tv_budget_remaining);
            pbBudgetProgress = itemView.findViewById(R.id.pb_budget_progress);
            btnEditBudget = itemView.findViewById(R.id.btn_edit_budget);
            btnDeleteBudget = itemView.findViewById(R.id.btn_delete_budget);
        }
    }

    private void showEditDialog(Budget budget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_budget, null);
        builder.setView(dialogView);

        Spinner spinnerEditBudgetCategory = dialogView.findViewById(R.id.spinner_edit_budget_category);
        EditText etEditBudgetCustomCategory = dialogView.findViewById(R.id.et_edit_budget_custom_category);
        EditText etEditAmount = dialogView.findViewById(R.id.et_edit_budget_amount);

        // Populate spinner with expense categories (since budget is for expense)
        List<String> expenseCategories = new ArrayList<>(List.of(context.getResources().getStringArray(R.array.expense_categories)));
        expenseCategories.add("Thêm danh mục mới...");

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, expenseCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditBudgetCategory.setAdapter(categoryAdapter);

        // Set initial selection
        int selectionIndex = expenseCategories.indexOf(budget.getCategory());
        if (selectionIndex != -1) {
            spinnerEditBudgetCategory.setSelection(selectionIndex);
            etEditBudgetCustomCategory.setVisibility(View.GONE);
            etEditBudgetCustomCategory.setText("");
        } else {
            spinnerEditBudgetCategory.setSelection(expenseCategories.indexOf("Thêm danh mục mới..."));
            etEditBudgetCustomCategory.setVisibility(View.VISIBLE);
            etEditBudgetCustomCategory.setText(budget.getCategory());
        }

        spinnerEditBudgetCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                if (selectedCategory.equals("Thêm danh mục mới...")) {
                    etEditBudgetCustomCategory.setVisibility(View.VISIBLE);
                    etEditBudgetCustomCategory.requestFocus();
                } else {
                    etEditBudgetCustomCategory.setVisibility(View.GONE);
                    etEditBudgetCustomCategory.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });


        etEditAmount.setText(String.valueOf(budget.getAmount()));

        builder.setTitle("Sửa Ngân sách")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newCategory;
                    if (etEditBudgetCustomCategory.getVisibility() == View.VISIBLE && !etEditBudgetCustomCategory.getText().toString().trim().isEmpty()) {
                        newCategory = etEditBudgetCustomCategory.getText().toString().trim();
                    } else if (spinnerEditBudgetCategory.getSelectedItem() != null && !spinnerEditBudgetCategory.getSelectedItem().toString().equals("Thêm danh mục mới...")) {
                        newCategory = spinnerEditBudgetCategory.getSelectedItem().toString();
                    } else {
                        Snackbar.make(dialogView, "Vui lòng chọn hoặc nhập danh mục.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    String newAmountStr = etEditAmount.getText().toString().trim();

                    if (newCategory.isEmpty() || newAmountStr.isEmpty()) {
                        Snackbar.make(dialogView, "Vui lòng điền đầy đủ thông tin.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    double newAmount;
                    try {
                        newAmount = Double.parseDouble(newAmountStr);
                        if (newAmount <= 0) {
                            Snackbar.make(dialogView, "Ngân sách phải là số dương.", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Snackbar.make(dialogView, "Số tiền không hợp lệ.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    budget.setCategory(newCategory);
                    budget.setAmount(newAmount);

                    ((MainActivity) context).updateBudget(budget);
                    Snackbar.make(dialogView, "Đã cập nhật ngân sách.", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
