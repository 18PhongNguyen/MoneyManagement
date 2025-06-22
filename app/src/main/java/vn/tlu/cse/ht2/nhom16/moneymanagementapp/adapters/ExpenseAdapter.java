package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters; // Đảm bảo tên gói này là đúng và nhất quán

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity; // Import đúng MainActivity từ gói 'activities'
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;
    private Context context;
    private DecimalFormat decimalFormat;
    private String currentCurrency;


    public ExpenseAdapter(List<Expense> expenseList, Context context, DecimalFormat decimalFormat, String currentCurrency) {
        this.expenseList = expenseList;
        this.context = context;
        this.decimalFormat = decimalFormat;
        this.currentCurrency = currentCurrency;
    }

    public void setDecimalFormat(DecimalFormat newFormat) {
        this.decimalFormat = newFormat;
    }

    public void setCurrentCurrency(String newCurrency) {
        this.currentCurrency = newCurrency;
    }


    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvDescription.setText("Mô tả: " + expense.getDescription());

        holder.tvAmount.setText(String.format("Số tiền: %s %s", decimalFormat.format(expense.getAmount()), currentCurrency));

        if (expense.getType().equals("income")) {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green_income));
            holder.tvType.setText("Loại: Thu nhập");
        } else {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.red_expense));
            holder.tvType.setText("Loại: Chi tiêu");
        }

        holder.tvCategory.setText("Danh mục: " + expense.getCategory());
        if (expense.getTimestamp() != null) {
            holder.tvTimestamp.setText("Thời gian: " + DateFormat.format("dd-MM-yyyy HH:mm", expense.getTimestamp()));
        } else {
            holder.tvTimestamp.setText("Thời gian: N/A");
        }

        holder.btnEdit.setOnClickListener(v -> showEditDialog(expense));
        holder.btnDelete.setOnClickListener(v -> {
            ((MainActivity) context).deleteExpense(expense.getId());
            Snackbar.make(holder.itemView, "Đã xóa khoản chi/thu", Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvType, tvCategory, tvTimestamp;
        Button btnEdit, btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvType = itemView.findViewById(R.id.tv_type);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    private void showEditDialog(Expense expense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_expense, null);
        builder.setView(dialogView);

        EditText etEditDescription = dialogView.findViewById(R.id.et_edit_description);
        EditText etEditAmount = dialogView.findViewById(R.id.et_edit_amount);
        RadioGroup rgEditType = dialogView.findViewById(R.id.rg_edit_type);
        RadioButton rbEditIncome = dialogView.findViewById(R.id.rb_edit_income);
        RadioButton rbEditExpense = dialogView.findViewById(R.id.rb_edit_expense);
        Spinner spinnerEditCategory = dialogView.findViewById(R.id.spinner_edit_category);
        EditText etEditCustomCategory = dialogView.findViewById(R.id.et_edit_custom_category);

        etEditDescription.setText(expense.getDescription());
        etEditAmount.setText(String.valueOf(expense.getAmount()));

        if (expense.getType().equals("income")) {
            rbEditIncome.setChecked(true);
        } else {
            rbEditExpense.setChecked(true);
        }

        List<String> expenseCategories = new ArrayList<>(List.of(context.getResources().getStringArray(R.array.expense_categories)));
        List<String> incomeCategories = new ArrayList<>(List.of(context.getResources().getStringArray(R.array.income_categories)));
        expenseCategories.add("Thêm danh mục mới...");
        incomeCategories.add("Thêm danh mục mới...");

        // Adapter cho spinner danh mục
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditCategory.setAdapter(categoryAdapter);

        // Hàm cập nhật Spinner dựa trên loại giao dịch
        Runnable updateSpinnerBasedOnType = () -> {
            List<String> currentCategories;
            if (rgEditType.getCheckedRadioButtonId() == R.id.rb_edit_income) {
                currentCategories = incomeCategories;
            } else {
                currentCategories = expenseCategories;
            }
            categoryAdapter.clear();
            categoryAdapter.addAll(currentCategories);
            categoryAdapter.notifyDataSetChanged();

            // Đặt selection cho spinner
            int selectionIndex = currentCategories.indexOf(expense.getCategory());
            if (selectionIndex != -1) {
                spinnerEditCategory.setSelection(selectionIndex);
                etEditCustomCategory.setVisibility(View.GONE);
                etEditCustomCategory.setText("");
            } else {
                // Nếu danh mục hiện tại không có trong danh sách, chọn "Thêm danh mục mới"
                spinnerEditCategory.setSelection(currentCategories.indexOf("Thêm danh mục mới..."));
                etEditCustomCategory.setVisibility(View.VISIBLE);
                etEditCustomCategory.setText(expense.getCategory());
            }
        };

        // Lắng nghe thay đổi của RadioGroup
        rgEditType.setOnCheckedChangeListener((group, checkedId) -> {
            updateSpinnerBasedOnType.run();
        });

        // Lắng nghe sự kiện chọn danh mục từ Spinner
        spinnerEditCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                if (selectedCategory.equals("Thêm danh mục mới...")) {
                    etEditCustomCategory.setVisibility(View.VISIBLE);
                    etEditCustomCategory.requestFocus();
                } else {
                    etEditCustomCategory.setVisibility(View.GONE);
                    etEditCustomCategory.setText(""); // Clear custom field if not 'Add new'
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Gọi lần đầu để thiết lập Spinner đúng cách
        updateSpinnerBasedOnType.run();

        builder.setTitle("Sửa Khoản Mục")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newDescription = etEditDescription.getText().toString().trim();
                    String newAmountStr = etEditAmount.getText().toString().trim();

                    int selectedTypeId = rgEditType.getCheckedRadioButtonId();
                    String newType;
                    if (selectedTypeId == R.id.rb_edit_income) {
                        newType = "income";
                    } else if (selectedTypeId == R.id.rb_edit_expense) {
                        newType = "expense";
                    } else {
                        Snackbar.make(dialogView, "Vui lòng chọn loại giao dịch (Thu/Chi).", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    String newCategory;
                    if (etEditCustomCategory.getVisibility() == View.VISIBLE && !etEditCustomCategory.getText().toString().trim().isEmpty()) {
                        newCategory = etEditCustomCategory.getText().toString().trim();
                    } else if (spinnerEditCategory.getSelectedItem() != null && !spinnerEditCategory.getSelectedItem().toString().equals("Thêm danh mục mới...")) {
                        newCategory = spinnerEditCategory.getSelectedItem().toString();
                    } else {
                        Snackbar.make(dialogView, "Vui lòng chọn hoặc nhập danh mục.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    if (newDescription.isEmpty() || newAmountStr.isEmpty() || newCategory.isEmpty()) {
                        Snackbar.make(dialogView, "Vui lòng điền đầy đủ thông tin.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    double newAmount;
                    try {
                        newAmount = Double.parseDouble(newAmountStr);
                    } catch (NumberFormatException e) {
                        Snackbar.make(dialogView, "Số tiền không hợp lệ.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }


                    expense.setDescription(newDescription);
                    expense.setAmount(newAmount);
                    expense.setType(newType.toLowerCase());
                    expense.setCategory(newCategory);

                    ((MainActivity) context).updateExpense(expense);
                    Snackbar.make(dialogView, "Đã cập nhật khoản chi/thu", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
