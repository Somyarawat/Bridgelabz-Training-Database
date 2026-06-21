package bookstore_app;

import java.sql.*;
import java.util.Scanner;

public class BookStoreApp {

    private static final Scanner scanner =
            new Scanner(System.in);

    public static void main(String[] args) {

        while (true) {

            System.out.println("\n===== BOOK STORE MENU =====");
            System.out.println("1. View Catalog");
            System.out.println("2. Add New Book");
            System.out.println("3. Update Book Price");
            System.out.println("4. Purchase Book");
            System.out.println("5. View Audit History");
            System.out.println("6. Exit");

            System.out.print("Enter Choice: ");

            int choice = scanner.nextInt();

            switch (choice) {

                case 1:
                    viewCatalog();
                    break;

                case 2:
                    addNewBook();
                    break;

                case 3:
                    updateBookPrice();
                    break;

                case 4:
                    purchaseBook();
                    break;

                case 5:
                    viewAuditHistory();
                    break;

                case 6:
                    System.out.println("Thank You");
                    System.exit(0);

                default:
                    System.out.println("Invalid Choice");
            }
        }
    }

    private static void viewCatalog() {

        String sql = "SELECT * FROM books";

        try (
                Connection conn =
                        DBUtil.getConnection();

                Statement stmt =
                        conn.createStatement();

                ResultSet rs =
                        stmt.executeQuery(sql)
        ) {

            while (rs.next()) {

                Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                );

                System.out.println(book);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addNewBook() {

        scanner.nextLine();

        System.out.print("Enter Title: ");
        String title = scanner.nextLine();

        System.out.print("Enter Author: ");
        String author = scanner.nextLine();

        System.out.print("Enter Price: ");
        double price = scanner.nextDouble();

        System.out.print("Enter Stock: ");
        int stock = scanner.nextInt();

        String sql =
                "INSERT INTO books(title,author,price,stock) VALUES(?,?,?,?)";

        try (
                Connection conn =
                        DBUtil.getConnection();

                PreparedStatement ps =
                        conn.prepareStatement(sql)
        ) {

            ps.setString(1, title);
            ps.setString(2, author);
            ps.setDouble(3, price);
            ps.setInt(4, stock);

            ps.executeUpdate();

            System.out.println("Book Added Successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateBookPrice() {

        System.out.print("Enter Book ID: ");
        int id = scanner.nextInt();

        System.out.print("Enter New Price: ");
        double price = scanner.nextDouble();

        String sql =
                "UPDATE books SET price=? WHERE id=?";

        try (
                Connection conn =
                        DBUtil.getConnection();

                PreparedStatement ps =
                        conn.prepareStatement(sql)
        ) {

            ps.setDouble(1, price);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Price Updated");
            else
                System.out.println("Book Not Found");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void purchaseBook() {

        System.out.print("Enter User ID: ");
        int userId = scanner.nextInt();

        System.out.print("Enter Book ID: ");
        int bookId = scanner.nextInt();

        System.out.print("Enter Quantity: ");
        int quantity = scanner.nextInt();

        Connection conn = null;

        try {

            conn = DBUtil.getConnection();

            conn.setAutoCommit(false);

            String bookQuery =
                    "SELECT price, stock FROM books WHERE id=?";

            PreparedStatement bookPs =
                    conn.prepareStatement(bookQuery);

            bookPs.setInt(1, bookId);

            ResultSet bookRs =
                    bookPs.executeQuery();

            if (!bookRs.next()) {
                throw new Exception("Book Not Found");
            }

            double price =
                    bookRs.getDouble("price");

            int stock =
                    bookRs.getInt("stock");

            if (stock < quantity) {
                throw new Exception(
                        "Insufficient Stock");
            }

            double totalAmount =
                    price * quantity;

            String userQuery =
                    "SELECT balance FROM users WHERE user_id=?";

            PreparedStatement userPs =
                    conn.prepareStatement(userQuery);

            userPs.setInt(1, userId);

            ResultSet userRs =
                    userPs.executeQuery();

            if (!userRs.next()) {
                throw new Exception("User Not Found");
            }

            double balance =
                    userRs.getDouble("balance");

            if (balance < totalAmount) {
                throw new Exception(
                        "Insufficient Balance");
            }

            PreparedStatement updateStock =
                    conn.prepareStatement(
                            "UPDATE books SET stock=stock-? WHERE id=?");

            updateStock.setInt(1, quantity);
            updateStock.setInt(2, bookId);

            updateStock.executeUpdate();

            PreparedStatement updateBalance =
                    conn.prepareStatement(
                            "UPDATE users SET balance=balance-? WHERE user_id=?");

            updateBalance.setDouble(1, totalAmount);
            updateBalance.setInt(2, userId);

            updateBalance.executeUpdate();

            PreparedStatement insertOrder =
                    conn.prepareStatement(
                            "INSERT INTO orders(user_id,book_id,quantity,total_amount) VALUES(?,?,?,?)");

            insertOrder.setInt(1, userId);
            insertOrder.setInt(2, bookId);
            insertOrder.setInt(3, quantity);
            insertOrder.setDouble(4, totalAmount);

            insertOrder.executeUpdate();

            conn.commit();

            System.out.println(
                    "Purchase Successful");

        } catch (Exception e) {

            try {

                if (conn != null)
                    conn.rollback();

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            System.out.println(
                    "Transaction Failed");

            System.out.println(
                    e.getMessage());
        }
        finally {

            try {

                if (conn != null)
                    conn.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void viewAuditHistory() {

        String sql =
                "SELECT * FROM price_audit ORDER BY audit_id";

        try (
                Connection conn =
                        DBUtil.getConnection();

                Statement stmt =
                        conn.createStatement();

                ResultSet rs =
                        stmt.executeQuery(sql)
        ) {

            while (rs.next()) {

                System.out.println(
                        "Audit ID: "
                                + rs.getInt("audit_id")
                                + " | Book ID: "
                                + rs.getInt("book_id")
                                + " | Old Price: "
                                + rs.getDouble("old_price")
                                + " | New Price: "
                                + rs.getDouble("new_price")
                                + " | Changed On: "
                                + rs.getTimestamp("changed_on")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}