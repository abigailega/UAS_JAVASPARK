package app.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import app.model.MenuItem;

public class MenuDao {

    private final String url = "jdbc:mysql://localhost:3306/restaurantdb";
    private final String user = "root";
    private final String password = "Rahasiadong1215";

    public MenuDao() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // ========================
    // GET ALL
    // ========================
       public List<MenuItem> getAll() {
    System.out.println("DEBUG: MenuDao.getAll() terpanggil");

    List<MenuItem> list = new ArrayList<>();
    String sql = "SELECT * FROM menu";

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        System.out.println("DEBUG: Query sudah jalan, baca hasil...");

        while (rs.next()) {
            String id = rs.getString("id");
            String name = rs.getString("name");
            System.out.println("DEBUG: row -> " + id + " | " + name);

            list.add(new MenuItem(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    "/images/" + rs.getString("image")
            ));
        }

    } catch (SQLException e) {
        System.out.println("ERROR SQL di getAll(): " + e.getMessage());
        e.printStackTrace();
    }

    System.out.println("DEBUG: Total data dari DB = " + list.size());
    return list;
}


    // FIND BY ID
    // ========================
    public MenuItem findById(String id) {
        String sql = "SELECT * FROM menu WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new MenuItem(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        "/images/" + rs.getString("image")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ========================
    // INSERT
    // ========================
    public boolean add(MenuItem item) {
        String sql = "INSERT INTO menu (id, name, category, description, price, image) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getCategory());
            stmt.setString(4, item.getDescription());
            stmt.setDouble(5, item.getPrice());
            stmt.setString(6, item.getImage().replace("/images/", ""));

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================
    // UPDATE
    // ========================
    public boolean update(MenuItem item) {
        String sql = "UPDATE menu SET name=?, category=?, description=?, price=?, image=? WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getCategory());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getPrice());
            stmt.setString(5, item.getImage().replace("/images/", ""));
            stmt.setString(6, item.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ========================
    // DELETE
    // ========================
    public boolean delete(String id) {
        String sql = "DELETE FROM menu WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
