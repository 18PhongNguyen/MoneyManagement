package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.ExpenseAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvExpensesHistory;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList; // Danh sách này sẽ được cập nhật từ MainActivity

    private ProgressBar pbLoadingHistory;
    private TextView tvEmptyHistoryState;

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
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvExpensesHistory = view.findViewById(R.id.rv_expenses_history);
        pbLoadingHistory = view.findViewById(R.id.pb_loading_history);
        tvEmptyHistoryState = view.findViewById(R.id.tv_empty_history_state);

        expenseList = new ArrayList<>(); // Khởi tạo danh sách trống

        // Truyền context của activity (MainActivity) và đơn vị tiền tệ cho adapter
        expenseAdapter = new ExpenseAdapter(expenseList, activity, activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvExpensesHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExpensesHistory.setAdapter(expenseAdapter);

        updateUI(); // Cập nhật UI ban đầu

        return view;
    }

    public void updateUI() {
        if (activity == null || !isAdded()) return;

        // Khi dữ liệu được cập nhật, ẩn ProgressBar
        pbLoadingHistory.setVisibility(View.GONE);

        expenseList.clear();
        expenseList.addAll(activity.getExpenseList());

        expenseAdapter.setDecimalFormat(activity.getDecimalFormat());
        expenseAdapter.setCurrentCurrency(activity.getCurrentCurrency());
        expenseAdapter.notifyDataSetChanged();

        // Hiển thị trạng thái rỗng nếu danh sách trống, nếu không thì hiển thị RecyclerView
        if (expenseList.isEmpty()) {
            rvExpensesHistory.setVisibility(View.GONE);
            tvEmptyHistoryState.setVisibility(View.VISIBLE);
        } else {
            rvExpensesHistory.setVisibility(View.VISIBLE);
            tvEmptyHistoryState.setVisibility(View.GONE);
        }
    }
}
