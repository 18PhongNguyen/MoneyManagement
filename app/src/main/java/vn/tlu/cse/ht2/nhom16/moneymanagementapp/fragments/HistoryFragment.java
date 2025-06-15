package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;



import java.util.ArrayList;
import java.util.List;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.ExpenseAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

public class HistoryFragment extends Fragment {

    private RecyclerView rvExpensesHistory;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList; // Danh sách này sẽ được cập nhật từ MainActivity

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
        expenseList = new ArrayList<>(); // Khởi tạo danh sách trống

        // Truyền context của activity (MainActivity) và đơn vị tiền tệ cho adapter
        expenseAdapter = new ExpenseAdapter(expenseList, activity, activity.getCurrentCurrency());
        rvExpensesHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExpensesHistory.setAdapter(expenseAdapter);

        // Ban đầu, cập nhật UI với dữ liệu hiện có từ MainActivity
        updateUI();

        return view;
    }

    // Phương thức công khai để MainActivity có thể gọi cập nhật dữ liệu
    public void updateUI() {
        if (activity == null || !isAdded()) return;

        // Xóa danh sách cũ và thêm dữ liệu mới từ MainActivity
        expenseList.clear();
        expenseList.addAll(activity.getExpenseList());

        // Cập nhật định dạng tiền tệ và đơn vị tiền tệ cho adapter
        expenseAdapter.setDecimalFormat(activity.getDecimalFormat());
        expenseAdapter.setCurrentCurrency(activity.getCurrentCurrency());
        expenseAdapter.notifyDataSetChanged();
    }
}
