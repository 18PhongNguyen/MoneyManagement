package vn.tlu.cse.ht2.nhom16.moneymanagementapp.models;

import com.google.firebase.firestore.Exclude;

public class Budget {
    private String id;
    private String category;
    private double amount;
    private long monthYear; // Để theo dõi ngân sách theo tháng/năm (ví dụ: 202310 cho tháng 10 năm 2023)

    public Budget() {
        // Constructor không đối số là bắt buộc cho Firebase Firestore
    }

    public Budget(String category, double amount, long monthYear) {
        this.category = category;
        this.amount = amount;
        this.monthYear = monthYear;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(long monthYear) {
        this.monthYear = monthYear;
    }
}
