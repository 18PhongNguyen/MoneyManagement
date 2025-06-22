package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.google.android.material.snackbar.Snackbar;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.CategorySummaryAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.CategorySummary;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";
    private PieChart pieChart;
    private TextView tvIncomeTotalStatistics, tvExpenseTotalStatistics, tvDateRange;
    private RecyclerView rvTopExpensesStatistics;
    private CategorySummaryAdapter categorySummaryAdapter;
    private List<CategorySummary> topExpensesList;

    private Button btnGenerateAiInsights;
    private TextView tvAiInsights;
    private ProgressBar progressBarAiInsights;

    private MainActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Calendar selectedDate; // Để lưu trữ ngày đã chọn cho việc lọc

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

        btnGenerateAiInsights = view.findViewById(R.id.btn_generate_ai_insights);
        tvAiInsights = view.findViewById(R.id.tv_ai_insights);
        progressBarAiInsights = view.findViewById(R.id.progress_bar_ai_insights);

        topExpensesList = new ArrayList<>();
        categorySummaryAdapter = new CategorySummaryAdapter(topExpensesList, activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvTopExpensesStatistics.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTopExpensesStatistics.setAdapter(categorySummaryAdapter);

        setupPieChart();

        // Khởi tạo ngày được chọn là tháng hiện tại
        selectedDate = Calendar.getInstance();
        updateDateRangeText(); // Cập nhật văn bản hiển thị phạm vi ngày

        tvDateRange.setOnClickListener(v -> showMonthYearPicker()); // Gọi DatePicker

        btnGenerateAiInsights.setOnClickListener(v -> generateAiInsights());

        updateUI(); // Cập nhật UI với dữ liệu hiện có

        return view;
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(1400);
        pieChart.getLegend().setEnabled(true);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
    }

    // Hiển thị DatePickerDialog chỉ cho phép chọn tháng và năm
    private void showMonthYearPicker() {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            updateDateRangeText();
            updateUI(); // Cập nhật lại UI sau khi chọn ngày mới
            Snackbar.make(requireView(), String.format("Thống kê cho tháng %d/%d", month + 1, year), Snackbar.LENGTH_SHORT).show();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateDateRangeText() {
        tvDateRange.setText(String.format("Phạm vi ngày: Tháng %d/%d", selectedDate.get(Calendar.MONTH) + 1, selectedDate.get(Calendar.YEAR)));
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

        // Lọc giao dịch theo tháng và năm được chọn
        int filterMonth = selectedDate.get(Calendar.MONTH);
        int filterYear = selectedDate.get(Calendar.YEAR);
        Log.d(TAG, "updateUI: Filtering for Month/Year: " + (filterMonth + 1) + "/" + filterYear);

        List<Expense> filteredExpenses = new ArrayList<>();
        if (allExpenses != null) {
            for (Expense expense : allExpenses) {
                if (expense.getTimestamp() != null) {
                    Calendar expenseCal = Calendar.getInstance();
                    expenseCal.setTime(expense.getTimestamp());
                    if (expenseCal.get(Calendar.MONTH) == filterMonth && expenseCal.get(Calendar.YEAR) == filterYear) {
                        filteredExpenses.add(expense);
                    }
                } else {
                    Log.w(TAG, "updateUI: Expense with null timestamp found: " + expense.getDescription());
                }
            }
        }
        Log.d(TAG, "updateUI: Filtered " + filteredExpenses.size() + " expenses for selected month.");


        for (Expense expense : filteredExpenses) {
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
        data.setValueTextColor(Color.BLACK);
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

    //region AI Insights Logic (unchanged from previous update)
    private void generateAiInsights() {
        if (activity == null || !isAdded()) {
            Snackbar.make(requireView(), "Ứng dụng chưa sẵn sàng.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        tvAiInsights.setText("Đang tạo gợi ý...");
        progressBarAiInsights.setVisibility(View.VISIBLE);
        btnGenerateAiInsights.setEnabled(false);

        new Thread(() -> {
            String prompt = prepareAiPrompt();
            String insight = callGeminiApi(prompt);

            mainHandler.post(() -> {
                progressBarAiInsights.setVisibility(View.GONE);
                btnGenerateAiInsights.setEnabled(true);
                if (insight != null && !insight.isEmpty()) {
                    tvAiInsights.setText(insight);
                } else {
                    tvAiInsights.setText("Không thể tạo gợi ý chi tiêu lúc này. Vui lòng thử lại.");
                }
            });
        }).start();
    }

    private String prepareAiPrompt() {
        List<Expense> expenses = activity.getExpenseList();
        DecimalFormat decimalFormat = activity.getDecimalFormat();
        String currency = activity.getCurrentCurrency();

        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseCategoryData = new HashMap<>();

        for (Expense expense : expenses) {
            if (expense.getType().equals("income")) {
                totalIncome += expense.getAmount();
            } else if (expense.getType().equals("expense")) {
                totalExpense += expense.getAmount();
                String category = expense.getCategory();
                expenseCategoryData.put(category, expenseCategoryData.getOrDefault(category, 0.0) + expense.getAmount());
            }
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Phân tích thói quen chi tiêu sau đây và đưa ra lời khuyên để quản lý tài chính tốt hơn. Dữ liệu: ");
        promptBuilder.append("Tổng thu nhập: ").append(decimalFormat.format(totalIncome)).append(" ").append(currency).append(". ");
        promptBuilder.append("Tổng chi tiêu: ").append(decimalFormat.format(totalExpense)).append(" ").append(currency).append(". ");
        promptBuilder.append("Chi tiêu theo danh mục: ");

        if (expenseCategoryData.isEmpty()) {
            promptBuilder.append("Không có chi tiêu nào được ghi nhận.");
        } else {
            promptBuilder.append(
                    expenseCategoryData.entrySet().stream()
                            .map(entry -> entry.getKey() + ": " + decimalFormat.format(entry.getValue()) + " " + currency)
                            .collect(Collectors.joining("; "))
            ).append(".");
        }
        promptBuilder.append("\n\n");
        promptBuilder.append("Hãy đưa ra các gợi ý cụ thể, dễ hiểu, ngắn gọn và hữu ích cho người dùng.\n\n");
        promptBuilder.append("Ví dụ về định dạng mong muốn:\n")
                .append("1. Tóm tắt chi tiêu:\n")
                .append("   - ...\n")
                .append("2. Lời khuyên:\n")
                .append("   - ...\n")
                .append("   - ...\n")
                .append("3. Các lĩnh vực có thể cải thiện:\n")
                .append("   - ...\n");


        return promptBuilder.toString();
    }

    private String callGeminiApi(String prompt) {
        String insight = null;
        try {
            String apiKey = "YOUR_GEMINI_API_KEY_HERE"; // REPLACE WITH YOUR ACTUAL API KEY
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject payload = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject userPart = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            partsArray.put(textPart);
            userPart.put("role", "user");
            userPart.put("parts", partsArray);
            contents.put(userPart);
            payload.put("contents", contents);

            Log.d(TAG, "Sending payload: " + payload.toString());

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.d(TAG, "Received response: " + response.toString());

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.has("candidates")) {
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject candidate = candidates.getJSONObject(0);
                            if (candidate.has("content")) {
                                JSONObject content = candidate.getJSONObject("content");
                                if (content.has("parts")) {
                                    JSONArray parts = content.getJSONArray("parts");
                                    if (parts.length() > 0) {
                                        insight = parts.getJSONObject(0).getString("text");
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine = null;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine.trim());
                    }
                    Log.e(TAG, "Error response: " + errorResponse.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling Gemini API: " + e.getMessage(), e);
        }
        return insight;
    }
    //endregion
}
