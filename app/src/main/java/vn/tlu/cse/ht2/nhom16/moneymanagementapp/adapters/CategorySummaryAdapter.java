package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;



import java.text.DecimalFormat;
import java.util.List;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.CategorySummary;

public class CategorySummaryAdapter extends RecyclerView.Adapter<CategorySummaryAdapter.CategorySummaryViewHolder> {

    private List<CategorySummary> categorySummaryList;
    private DecimalFormat decimalFormat;
    private String currentCurrency;

    public CategorySummaryAdapter(List<CategorySummary> categorySummaryList, DecimalFormat decimalFormat, String currentCurrency) {
        this.categorySummaryList = categorySummaryList;
        this.decimalFormat = decimalFormat;
        this.currentCurrency = currentCurrency;
    }

    public void setDecimalFormat(DecimalFormat decimalFormat) {
        this.decimalFormat = decimalFormat;
    }

    public void setCurrentCurrency(String currentCurrency) {
        this.currentCurrency = currentCurrency;
    }

    @NonNull
    @Override
    public CategorySummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_summary, parent, false);
        return new CategorySummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategorySummaryViewHolder holder, int position) {
        CategorySummary summary = categorySummaryList.get(position);
        holder.tvCategoryName.setText(summary.getCategoryName());
        holder.tvCategoryPercentage.setText(String.format("%.2f%%", summary.getPercentage()));
        holder.tvCategoryAmount.setText(String.format("%s %s", decimalFormat.format(-summary.getAmount()), currentCurrency)); // Chi tiêu nên hiển thị số âm

        // Thiết lập màu cho số tiền chi tiêu
        holder.tvCategoryAmount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red_expense));

        // Thiết lập icon tùy thuộc vào danh mục (có thể mở rộng sau)
        // holder.ivCategoryIcon.setImageResource(R.drawable.ic_category_default);
    }

    @Override
    public int getItemCount() {
        return categorySummaryList.size();
    }

    static class CategorySummaryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName, tvCategoryPercentage, tvCategoryAmount;

        public CategorySummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvCategoryPercentage = itemView.findViewById(R.id.tv_category_percentage);
            tvCategoryAmount = itemView.findViewById(R.id.tv_category_amount);
        }
    }
}
