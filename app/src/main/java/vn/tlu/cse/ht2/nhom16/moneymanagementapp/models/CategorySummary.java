package vn.tlu.cse.ht2.nhom16.moneymanagementapp.models;

public class CategorySummary {
    private String categoryName;
    private double percentage;
    private double amount;

    public CategorySummary(String categoryName, double percentage, double amount) {
        this.categoryName = categoryName;
        this.percentage = percentage;
        this.amount = amount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
