package app.dao;

import app.model.CartItem;
import app.model.Order;
import app.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private final String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/restaurantdb");
    private final String user = System.getenv().getOrDefault("DB_USER", "root");
    private final String password = System.getenv().getOrDefault("DB_PASS", "abigail");

    public OrderDao() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public boolean saveOrder(String orderId, String customerName, String tableNumber, List<CartItem> cart) {
        String insertOrderSQL = "INSERT INTO orders (id, customer_name, table_number, total, timestamp, status) VALUES (?, ?, ?, ?, ?, ?)";
        String insertItemSQL = "INSERT INTO order_items (order_id, menu_id, name, quantity, price, total) VALUES (?, ?, ?, ?, ?, ?)";

        double total = cart.stream().mapToDouble(CartItem::getTotal).sum();
        long timestamp = System.currentTimeMillis();

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement orderStmt = conn.prepareStatement(insertOrderSQL)) {
                orderStmt.setString(1, orderId);
                orderStmt.setString(2, customerName);
                orderStmt.setString(3, tableNumber);
                orderStmt.setDouble(4, total);
                orderStmt.setLong(5, timestamp);
                orderStmt.setString(6, "NEW");
                orderStmt.executeUpdate();
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSQL)) {
                for (CartItem ci : cart) {
                    itemStmt.setString(1, orderId);
                    itemStmt.setString(2, ci.getItem().getId());
                    itemStmt.setString(3, ci.getItem().getName());
                    itemStmt.setInt(4, ci.getQuantity());
                    itemStmt.setDouble(5, ci.getItem().getPrice());
                    itemStmt.setDouble(6, ci.getTotal());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String customerName = rs.getString("customer_name");
                String tableNumber = rs.getString("table_number");
                double total = rs.getDouble("total");
                long timestamp = rs.getLong("timestamp");
                String status = rs.getString("status");

                List<OrderItem> items = getItemsByOrderId(id);

                orders.add(new Order(id, customerName, tableNumber, items, total, timestamp, status));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    private List<OrderItem> getItemsByOrderId(String orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id=?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(new OrderItem(
                        rs.getString("menu_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}
