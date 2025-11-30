package app.model;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    private String id;
    private String customerName;
    private String tableNumber;
    private List<OrderItem> items;
    private double total;
    private long timestamp;
    private String status;

    public Order() {
    }

    public Order(String id, String customerName, String tableNumber, List<OrderItem> items,
            double total, long timestamp, String status) {
        this.id = id;
        this.customerName = customerName;
        this.tableNumber = tableNumber;
        this.items = items;
        this.total = total;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Order[%s] Table: %s, Total: %.2f, Status: %s", id, tableNumber, total, status);
    }
}
