package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.content.res.Resources; // Import Resources
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log; // Import Log for debugging
import android.util.TypedValue; // Import TypedValue
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat; // Import ContextCompat

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.CategorySummaryAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.CategorySummary;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Calendar; // Import Calendar
import java.util.Date; // Import Date
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment"; // Tag for logging

    private PieChart pieChart;
    private TextView tvIncomeTotalStatistics, tvExpenseTotalStatistics, tvDateRange;
    private RecyclerView rvTopExpensesStatistics;
    private CategorySummaryAdapter categorySummaryAdapter;
    private List<CategorySummary> topExpensesList;

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
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        pieChart = view.findViewById(R.id.pie_chart_statistics);
        tvIncomeTotalStatistics = view.findViewById(R.id.tv_income_total_statistics);
        tvExpenseTotalStatistics = view.findViewById(R.id.tv_expense_total_statistics);
        tvDateRange = view.findViewById(R.id.tv_date_range);
        rvTopExpensesStatistics = view.findViewById(R.id.rv_top_expenses_statistics);

        topExpensesList = new ArrayList<>();
        categorySummaryAdapter = new CategorySummaryAdapter(topExpensesList, activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvTopExpensesStatistics.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTopExpensesStatistics.setAdapter(categorySummaryAdapter);

        // Cấu hình Pie Chart ban đầu
        setupPieChart();

        // Lắng nghe sự kiện click vào Date Range (chưa triển khai dialog chọn ngày ở đây)
        tvDateRange.setOnClickListener(v -> Toast.makeText(activity, "Phạm vi ngày hiện tại được hiển thị", Toast.LENGTH_SHORT).show());

        // Cập nhật UI với dữ liệu hiện có
        updateUI();

        return view;
    }

    // Helper để lấy màu từ theme
    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);

        // Lấy màu nền của theme và log ra để kiểm tra
        int holeColor = getThemeColor(android.R.attr.colorBackground);
        Log.d(TAG, "Hole Color (android.R.attr.colorBackground): " + String.format("#%06X", (0xFFFFFF & holeColor)));
        pieChart.setHoleColor(holeColor);
        pieChart.setTransparentCircleColor(holeColor);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(1400);
        pieChart.getLegend().setEnabled(true);

        // Lấy màu chữ chính của theme và log ra để kiểm tra
        int textColorPrimary = getThemeColor(android.R.attr.textColorPrimary);
        Log.d(TAG, "Entry Label Color (android.R.attr.textColorPrimary): " + String.format("#%06X", (0xFFFFFF & textColorPrimary)));
        pieChart.setEntryLabelColor(textColorPrimary); // Sử dụng màu chữ chính của theme
        pieChart.setEntryLabelTextSize(12f);
    }

    // Phương thức công khai để MainActivity có thể gọi cập nhật dữ liệu
    public void updateUI() {
        if (activity == null || !isAdded()) return;

        List<Expense> allExpenses = activity.getExpenseList();
        DecimalFormat decimalFormat = activity.getDecimalFormat();
        String currentCurrency = activity.getCurrentCurrency();

        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseCategoryData = new HashMap<>();

        // Lấy tháng và năm hiện tại để lọc dữ liệu
        Calendar currentCal = Calendar.getInstance();
        int currentMonth = currentCal.get(Calendar.MONTH); // 0-indexed
        int currentYear = currentCal.get(Calendar.YEAR);

        // Cập nhật TextView phạm vi ngày
        tvDateRange.setText(String.format("Phạm vi ngày: Tháng %d/%d", currentMonth + 1, currentYear));

        // Lọc các khoản chi tiêu chỉ trong tháng hiện tại
        List<Expense> monthlyExpenses = new ArrayList<>();
        for (Expense expense : allExpenses) {
            if (expense.getTimestamp() != null) {
                Calendar expenseCal = Calendar.getInstance();
                expenseCal.setTime(expense.getTimestamp());
                if (expenseCal.get(Calendar.MONTH) == currentMonth && expenseCal.get(Calendar.YEAR) == currentYear) {
                    monthlyExpenses.add(expense);
                }
            }
        }

        // Tính toán tổng thu, tổng chi và dữ liệu danh mục dựa trên các khoản chi tiêu của tháng hiện tại
        for (Expense expense : monthlyExpenses) { // Lặp qua danh sách đã lọc
            if (expense.getType().equals("income")) {
                totalIncome += expense.getAmount();
            } else if (expense.getType().equals("expense")) {
                totalExpense += expense.getAmount();
                String category = expense.getCategory();
                expenseCategoryData.put(category, expenseCategoryData.getOrDefault(category, 0.0) + expense.getAmount());
            }
        }

        // Cập nhật tổng thu, tổng chi trên giao diện
        tvIncomeTotalStatistics.setText(String.format("Tổng thu: %s %s", decimalFormat.format(totalIncome), currentCurrency));
        tvExpenseTotalStatistics.setText(String.format("Tổng chi: %s %s", decimalFormat.format(totalExpense), currentCurrency));

        // Cập nhật biểu đồ chi tiêu theo danh mục
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (totalExpense > 0) { // Chỉ thêm entry nếu có chi tiêu để tránh lỗi chia cho 0 hoặc biểu đồ trống
            for (Map.Entry<String, Double> entry : expenseCategoryData.entrySet()) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        } else {
            // Thêm một entry "Không có dữ liệu" nếu không có chi tiêu
            entries.add(new PieEntry(1f, "Không có dữ liệu"));
        }


        PieDataSet dataSet = new PieDataSet(entries, "Chi tiêu theo danh mục");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        // Sử dụng các màu sắc định nghĩa trong colors.xml hoặc ColorTemplate
        for (int c : ColorTemplate.VORDIPLOM_COLORS) colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS) colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS) colors.add(c);
        colors.add(ColorTemplate.getHoloBlue());
        dataSet.setColors(colors);

        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        // Sử dụng màu chữ chính của theme để đảm bảo hiển thị trong cả hai chế độ
        data.setValueTextColor(getThemeColor(android.R.attr.textColorPrimary));
        pieChart.setData(data);
        pieChart.invalidate();

        // Cập nhật Top Expenses by category (RecyclerView)
        topExpensesList.clear();
        double finalTotalExpense = totalExpense; // Tạo biến final để dùng trong lambda
        expenseCategoryData.forEach((category, amount) -> {
            double percentage = (finalTotalExpense > 0) ? (amount / finalTotalExpense) * 100 : 0;
            topExpensesList.add(new CategorySummary(category, percentage, amount));
        });

        // Sắp xếp theo số tiền giảm dần
        Collections.sort(topExpensesList, (o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));

        categorySummaryAdapter.setDecimalFormat(decimalFormat); // Cập nhật định dạng
        categorySummaryAdapter.setCurrentCurrency(currentCurrency); // Cập nhật đơn vị tiền tệ
        categorySummaryAdapter.notifyDataSetChanged();
    }
}
