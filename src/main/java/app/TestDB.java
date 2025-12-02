package app;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestDB {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/restaurantdb", "root", "Rahasiadong1215");
        System.out.println("Connected!");
        conn.close();
    }
}
