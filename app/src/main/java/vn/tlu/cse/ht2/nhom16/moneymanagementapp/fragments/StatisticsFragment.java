package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";
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
        Log.d(TAG, "onCreateView: StatisticsFragment created.");
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        pieChart = view.findViewById(R.id.pie_chart_statistics);
        tvIncomeTotalStatistics = view.findViewById(R.id.tv_income_total_statistics);
        tvExpenseTotalStatistics = view.findViewById(R.id.tv_expense_total_statistics);
        tvDateRange = view.findViewById(R.id.tv_date_range);
        rvTopExpensesStatistics = view.findViewById(R.id.rv_top_expenses_statistics);

        topExpensesList = new ArrayList<>();
        // Truyền DecimalFormat và currentCurrency từ MainActivity (giờ đã cố định)
        categorySummaryAdapter = new CategorySummaryAdapter(topExpensesList, activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvTopExpensesStatistics.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTopExpensesStatistics.setAdapter(categorySummaryAdapter);

        setupPieChart();

        tvDateRange.setOnClickListener(v -> Toast.makeText(activity, "Chọn phạm vi ngày (Chưa triển khai)", Toast.LENGTH_SHORT).show());

        // updateUI() sẽ được gọi từ MainActivity khi dữ liệu sẵn sàng
        // updateUI();

        return view;
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);

        // Cố định màu cho lỗ và vòng tròn trong suốt (chế độ sáng)
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        Log.d(TAG, "setupPieChart: Setting Hole/Transparent Circle Color to WHITE (Fixed Day Mode).");

        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(1400);
        pieChart.getLegend().setEnabled(true);

        // Cố định màu chữ nhãn Entry (chế độ sáng)
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        Log.d(TAG, "setupPieChart: Setting Entry Label Color to BLACK (Fixed Day Mode).");
    }

    public void updateUI() {
        Log.d(TAG, "updateUI: Called for StatisticsFragment.");
        if (activity == null || !isAdded()) {
            Log.w(TAG, "updateUI: StatisticsFragment not attached or not added. Skipping UI update.");
            return;
        }

        List<Expense> allExpenses = activity.getExpenseList();
        Log.d(TAG, "updateUI: Total expenses from MainActivity: " + (allExpenses != null ? allExpenses.size() : "null"));

        DecimalFormat decimalFormat = activity.getDecimalFormat();
        String currentCurrency = activity.getCurrentCurrency();

        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseCategoryData = new HashMap<>();

        Calendar currentCal = Calendar.getInstance();
        int currentMonth = currentCal.get(Calendar.MONTH);
        int currentYear = currentCal.get(Calendar.YEAR);
        Log.d(TAG, "updateUI: Current Month/Year for filtering: " + (currentMonth + 1) + "/" + currentYear);


        tvDateRange.setText(String.format("Phạm vi ngày: Tháng %d/%d", currentMonth + 1, currentYear));

        List<Expense> monthlyExpenses = new ArrayList<>();
        if (allExpenses != null) {
            for (Expense expense : allExpenses) {
                if (expense.getTimestamp() != null) {
                    Calendar expenseCal = Calendar.getInstance();
                    expenseCal.setTime(expense.getTimestamp());
                    if (expenseCal.get(Calendar.MONTH) == currentMonth && expenseCal.get(Calendar.YEAR) == currentYear) {
                        monthlyExpenses.add(expense);
                    }
                } else {
                    Log.w(TAG, "updateUI: Expense with null timestamp found: " + expense.getDescription());
                }
            }
        }
        Log.d(TAG, "updateUI: Filtered " + monthlyExpenses.size() + " expenses for current month.");


        for (Expense expense : monthlyExpenses) {
            if (expense.getType().equals("income")) {
                totalIncome += expense.getAmount();
            } else if (expense.getType().equals("expense")) {
                totalExpense += expense.getAmount();
                String category = expense.getCategory();
                expenseCategoryData.put(category, expenseCategoryData.getOrDefault(category, 0.0) + expense.getAmount());
            }
        }

        tvIncomeTotalStatistics.setText(String.format("Tổng thu: %s %s", decimalFormat.format(totalIncome), currentCurrency));
        tvExpenseTotalStatistics.setText(String.format("Tổng chi: %s %s", decimalFormat.format(totalExpense), currentCurrency));
        Log.d(TAG, "updateUI: StatisticsFragment totals - Income: " + totalIncome + ", Expense: " + totalExpense);


        ArrayList<PieEntry> entries = new ArrayList<>();
        if (totalExpense > 0) {
            for (Map.Entry<String, Double> entry : expenseCategoryData.entrySet()) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
                Log.d(TAG, "updateUI: Pie Entry - Category: " + entry.getKey() + ", Amount: " + entry.getValue());
            }
        } else {
            entries.add(new PieEntry(1f, "Không có dữ liệu"));
            Log.d(TAG, "updateUI: No expenses for current month, adding 'No Data' entry.");
        }

        PieDataSet dataSet = new PieDataSet(entries, "Chi tiêu theo danh mục");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
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
        // Cố định màu chữ giá trị (chế độ sáng)
        data.setValueTextColor(Color.BLACK);
        Log.d(TAG, "updateUI: Setting Value Text Color to BLACK (Fixed Day Mode).");

        pieChart.setData(data);
        pieChart.invalidate();

        topExpensesList.clear();
        double finalTotalExpense = totalExpense;
        expenseCategoryData.forEach((category, amount) -> {
            double percentage = (finalTotalExpense > 0) ? (amount / finalTotalExpense) * 100 : 0;
            topExpensesList.add(new CategorySummary(category, percentage, amount));
        });

        Collections.sort(topExpensesList, (o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));

        categorySummaryAdapter.setDecimalFormat(decimalFormat);
        categorySummaryAdapter.setCurrentCurrency(currentCurrency);
        categorySummaryAdapter.notifyDataSetChanged();
        Log.d(TAG, "updateUI: StatisticsFragment topExpensesList size: " + topExpensesList.size());
    }
}
