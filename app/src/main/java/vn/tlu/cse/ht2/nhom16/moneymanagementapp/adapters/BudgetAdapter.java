package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;



import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Budget;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgetList;
    private Context context;
    private DecimalFormat decimalFormat;
    private String currentCurrency;
    private Map<String, Double> currentMonthExpensesByCategory; // Để theo dõi chi tiêu đã thực hiện

    public BudgetAdapter(List<Budget> budgetList, Context context, DecimalFormat decimalFormat, String currentCurrency) {
        this.budgetList = budgetList;
        this.context = context;
        this.decimalFormat = decimalFormat;
        this.currentCurrency = currentCurrency;
        this.currentMonthExpensesByCategory = new HashMap<>(); // Khởi tạo rỗng
    }

    // Setter để cập nhật định dạng tiền tệ
    public void setDecimalFormat(DecimalFormat newFormat) {
        this.decimalFormat = newFormat;
    }

    // Setter để cập nhật đơn vị tiền tệ
    public void setCurrentCurrency(String newCurrency) {
        this.currentCurrency = newCurrency;
    }

    // Setter để cập nhật dữ liệu chi tiêu hàng tháng theo danh mục
    public void setCurrentMonthExpensesByCategory(Map<String, Double> expenses) {
        this.currentMonthExpensesByCategory = expenses;
        notifyDataSetChanged(); // Cập nhật khi dữ liệu chi tiêu thay đổi
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

        // Tính toán số tiền đã chi cho danh mục này trong tháng hiện tại
        double spentAmount = currentMonthExpensesByCategory.getOrDefault(budget.getCategory(), 0.0);
        holder.tvBudgetSpent.setText(String.format("%s %s", decimalFormat.format(spentAmount), currentCurrency));

        // Tính toán và hiển thị tiến độ
        int progress = 0;
        if (budget.getAmount() > 0) {
            progress = (int) ((spentAmount / budget.getAmount()) * 100);
            if (progress > 100) progress = 100; // Không vượt quá 100% trên ProgressBar
        }
        holder.pbBudgetProgress.setProgress(progress);

        // Đặt màu cho ProgressBar và văn bản còn lại dựa trên tiến độ
        if (spentAmount > budget.getAmount()) {
            holder.tvBudgetRemaining.setTextColor(context.getResources().getColor(R.color.red_expense));
            holder.tvBudgetRemaining.setText("Vượt ngân sách: " + decimalFormat.format(spentAmount - budget.getAmount()) + " " + currentCurrency);
            holder.pbBudgetProgress.setProgressTintList(context.getResources().getColorStateList(R.color.red_expense));
        } else {
            holder.tvBudgetRemaining.setTextColor(context.getResources().getColor(android.R.color.tab_indicator_text)); // Hoặc màu mặc định
            holder.tvBudgetRemaining.setText("Còn lại: " + decimalFormat.format(budget.getAmount() - spentAmount) + " " + currentCurrency);
            holder.pbBudgetProgress.setProgressTintList(context.getResources().getColorStateList(R.color.green_income));
        }


        // Đặt lắng nghe sự kiện cho nút Sửa
        holder.btnEditBudget.setOnClickListener(v -> showEditDialog(budget));
        // Đặt lắng nghe sự kiện cho nút Xóa
        holder.btnDeleteBudget.setOnClickListener(v -> ((MainActivity) context).deleteBudget(budget.getId()));
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

        EditText etEditCategory = dialogView.findViewById(R.id.et_edit_budget_category);
        EditText etEditAmount = dialogView.findViewById(R.id.et_edit_budget_amount);

        etEditCategory.setText(budget.getCategory());
        etEditAmount.setText(String.valueOf(budget.getAmount()));

        builder.setTitle("Sửa Ngân sách")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newCategory = etEditCategory.getText().toString().trim();
                    String newAmountStr = etEditAmount.getText().toString().trim();

                    if (newCategory.isEmpty() || newAmountStr.isEmpty()) {
                        Toast.makeText(context, "Vui lòng điền đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newAmount = Double.parseDouble(newAmountStr);

                    budget.setCategory(newCategory);
                    budget.setAmount(newAmount);

                    ((MainActivity) context).updateBudget(budget);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
