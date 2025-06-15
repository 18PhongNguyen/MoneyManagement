package vn.tlu.cse.ht2.nhom16.moneymanagementapp.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date; // Chắc chắn là java.util.Date

public class Expense {
    private String id;
    private String description;
    private double amount;
    private String type; // "income" (thu nhập) hoặc "expense" (chi tiêu)
    private String category;
    @ServerTimestamp // Annotation này sẽ tự động điền timestamp khi ghi vào Firestore
    private Date timestamp; // Kiểu dữ liệu phải là java.util.Date

    public Expense() {
        // Constructor không đối số này là bắt buộc cho Firebase Firestore để deserialize dữ liệu
    }

    public Expense(String description, double amount, String type, String category) {
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        // timestamp sẽ được tự động điền bởi ServerTimestamp.
        // Tuy nhiên, đối với dữ liệu mẫu, chúng ta sẽ đặt trực tiếp trong MainActivity.
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
