package bookstore_app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    private static final String DRIVER_CLASS = "org.postgresql.Driver";

    private static final String URL = "jdbc:postgresql://localhost:5432/bookstore_db";

    private static final String USERNAME = "postgres";

    private static final String PASSWORD = "260124";

    static {
        try {
            Class.forName(DRIVER_CLASS);
            System.out.println("Driver Loaded Successfully");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver Not Found");
        }
    }

    public static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                URL,
                USERNAME,
                PASSWORD
        );
    }
}


//    public static void main(String[] args) {
//
//        try(Connection conn = DBUtil.getConnection()) {
//            System.out.println( "Database Connected Successfully");
//        }
//        catch (Exception e) {
//                throw new RuntimeException(e);
//        }
//    }
//}