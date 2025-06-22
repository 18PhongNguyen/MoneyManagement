package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.CategorySummary;

import java.text.DecimalFormat;
import java.util.List;

public class CategorySummaryAdapter extends RecyclerView.Adapter<CategorySummaryAdapter.CategorySummaryViewHolder> {

    private List<CategorySummary> categorySummaryList;
    private DecimalFormat decimalFormat;
    private String currentCurrency;
    private Context context; // Thêm context để truy cập tài nguyên drawable

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
        this.context = parent.getContext(); // Lấy context từ parent
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_summary, parent, false);
        return new CategorySummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategorySummaryViewHolder holder, int position) {
        CategorySummary summary = categorySummaryList.get(position);
        holder.tvCategoryName.setText(summary.getCategoryName());
        holder.tvCategoryPercentage.setText(String.format("%.2f%%", summary.getPercentage()));
        holder.tvCategoryAmount.setText(String.format("%s %s", decimalFormat.format(-summary.getAmount()), currentCurrency)); // Chi tiêu nên hiển thị số âm

        holder.tvCategoryAmount.setTextColor(context.getResources().getColor(R.color.red_expense));

        // Thiết lập icon tùy thuộc vào danh mục
        holder.ivCategoryIcon.setImageResource(getCategoryIcon(summary.getCategoryName()));
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

    // Phương thức để trả về icon drawable ID dựa trên tên danh mục
    private int getCategoryIcon(String categoryName) {
        switch (categoryName) {
            case "Ăn uống":
                return R.drawable.ic_category_food;
            case "Đi lại":
                return R.drawable.ic_category_transport;
            // Thêm các trường hợp khác cho các danh mục khác
            case "Nhà ở": // Giả định có ic_category_home.xml
            case "Mua sắm": // Giả định có ic_category_shopping.xml
            case "Giải trí": // Giả định có ic_category_entertainment.xml
            case "Hóa đơn": // Giả định có ic_category_bills.xml
            case "Giáo dục": // Giả định có ic_category_education.xml
            case "Sức khỏe": // Giả định có ic_category_health.xml
            case "Quà tặng": // Giả định có ic_category_gift.xml
            case "Lương": // Giả định có ic_income_salary.xml
            case "Thưởng": // Giả định có ic_income_bonus.xml
            case "Đầu tư": // Giả định có ic_income_investment.xml
                // Nếu bạn có các icon cụ thể cho những cái này, hãy thêm vào đây
                // Ví dụ: return R.drawable.ic_category_home;
            default:
                return R.drawable.ic_category_default; // Icon mặc định nếu không khớp
        }
    }
}
