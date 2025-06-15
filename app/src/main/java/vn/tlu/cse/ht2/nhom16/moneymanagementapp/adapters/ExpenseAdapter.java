package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat; // Import ContextCompat

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity; // Đảm bảo đúng package cho MainActivity
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

import java.text.DecimalFormat;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;
    private Context context;
    private DecimalFormat decimalFormat;
    private String currentCurrency;

    public ExpenseAdapter(List<Expense> expenseList, Context context, String initialCurrency) {
        this.expenseList = expenseList;
        this.context = context;
        this.currentCurrency = initialCurrency;
        // DecimalFormat ban đầu, sẽ được cập nhật từ MainActivity
        this.decimalFormat = new DecimalFormat("#,##0.00");
    }

    // Setter để cập nhật định dạng tiền tệ từ MainActivity
    public void setDecimalFormat(DecimalFormat newFormat) {
        this.decimalFormat = newFormat;
    }

    // Setter để cập nhật đơn vị tiền tệ từ MainActivity
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

        // Hiển thị số tiền với định dạng và đơn vị tiền tệ
        holder.tvAmount.setText(String.format("Số tiền: %s %s", decimalFormat.format(expense.getAmount()), currentCurrency));

        // Đặt màu cho số tiền dựa trên loại (thu nhập/chi tiêu)
        if (expense.getType().equals("income")) {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green_income));
        } else {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.red_expense));
        }

        holder.tvType.setText("Loại: " + (expense.getType().equals("income") ? "Thu nhập" : "Chi tiêu"));
        holder.tvCategory.setText("Danh mục: " + expense.getCategory());
        if (expense.getTimestamp() != null) {
            holder.tvTimestamp.setText("Thời gian: " + DateFormat.format("dd-MM-yyyy HH:mm", expense.getTimestamp()));
        } else {
            holder.tvTimestamp.setText("Thời gian: N/A");
        }

        holder.btnEdit.setOnClickListener(v -> showEditDialog(expense));
        holder.btnDelete.setOnClickListener(v -> ((MainActivity) context).deleteExpense(expense.getId()));
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
        EditText etEditType = dialogView.findViewById(R.id.et_edit_type);
        EditText etEditCategory = dialogView.findViewById(R.id.et_edit_category);

        etEditDescription.setText(expense.getDescription());
        etEditAmount.setText(String.valueOf(expense.getAmount()));
        etEditType.setText(expense.getType());
        etEditCategory.setText(expense.getCategory());

        builder.setTitle("Sửa Khoản Mục")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newDescription = etEditDescription.getText().toString().trim();
                    String newAmountStr = etEditAmount.getText().toString().trim();
                    String newType = etEditType.getText().toString().trim();
                    String newCategory = etEditCategory.getText().toString().trim();

                    if (newDescription.isEmpty() || newAmountStr.isEmpty() || newType.isEmpty() || newCategory.isEmpty()) {
                        Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newType.equalsIgnoreCase("income") && !newType.equalsIgnoreCase("expense")) {
                        Toast.makeText(context, "Loại phải là 'income' hoặc 'expense'", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newAmount = Double.parseDouble(newAmountStr);

                    expense.setDescription(newDescription);
                    expense.setAmount(newAmount);
                    expense.setType(newType.toLowerCase());
                    expense.setCategory(newCategory);

                    ((MainActivity) context).updateExpense(expense);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
