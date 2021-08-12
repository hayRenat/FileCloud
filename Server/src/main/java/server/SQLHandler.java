package server;

import java.sql.*;

public class SQLHandler {
    static final String DB_URL = "jdbc:h2:C:\\Java Project\\FileCloud\\DB";
    static final String USER = "admin";
    static final String PASS = "admin";
    static Connection conn = null;
    static Statement stmt = null;

    public static void connect() {
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            stmt.close();
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static String sendAuth(String name, String password) {
        String result = null;
        try {
            stmt = conn.createStatement();
            String sqlpass = "SELECT * FROM USER WHERE USERNAME ='" + name + "' AND PASSWORD = '" + password + "'";
            ResultSet rs = stmt.executeQuery(sqlpass);
            if (rs.next()) {
                result = rs.getString("USERNAME");
                return result;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }
}
