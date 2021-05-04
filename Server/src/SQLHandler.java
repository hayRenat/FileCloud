import java.sql.*;

public class SQLHandler {
    static final String JDBC_DRIVER = "org.h2.Driver";
    static final String DB_URL = "jdbc:h2:C:\\Java Project\\FileCloud\\DB";
    static final String USER = "admin";
    static final String PASS = "admin";
    static Connection conn = null;
    static Statement stmt = null;

    public static void connect() {
        try {
            Class.forName(JDBC_DRIVER); //Регистрация драйвера базы данных JDBC
            System.out.println("Соединение с БД");//временно для отслеживания
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Не удалось подключиться к БД");//временно
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            stmt.close();
            conn.close();
        } catch (SQLException throwables) {
            System.out.println("Не удалось закрыть подключение к БД");//временно
            throwables.printStackTrace();
        }
    }

    public static String sendAuth(String name, String password) {
        String result = null;
        try {
            stmt = conn.createStatement();
            String sqlpass = "SELECT * FROM USER WHERE PASSWORD = '" + password + "'";
            System.out.println("Идёт к БД");//временно
            ResultSet rs = stmt.executeQuery(sqlpass);
            System.out.println("ПРИШЛО С БД");//временно
            if (rs.next()) {
                result = rs.getString("PASSWORD");
                System.out.println("Вытащил данные из БД - " + result);//временно
                return result;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }
}
