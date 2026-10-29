import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All database access for Misty Swirl POS (XAMPP MySQL / MariaDB).
 * Run misty_swirl_pos.sql in phpMyAdmin first, and add the MySQL JDBC driver
 * (mysql-connector-j) to the NetBeans project libraries.
 */
public class DBConnection {

    // CHANGE THESE IF YOUR XAMPP SETUP IS DIFFERENT
    private static final String URL =
            "jdbc:mysql://localhost:3306/MSPOS1"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";   // XAMPP default is empty

    // ORDER RECORD MODEL (loaded from the orders + order_items tables)
    public static class OrderRecord {
        public final String orderId;
        public final String timestamp;
        public final String role;
        public final Map<String, Integer> items = new LinkedHashMap<>();
        public final Map<String, Integer> unitPrices = new LinkedHashMap<>();
        public final double total;
        public final String paymentMethod;

        OrderRecord(String orderId, String timestamp, String role,
                    double total, String paymentMethod) {
            this.orderId = orderId;
            this.timestamp = timestamp;
            this.role = role;
            this.total = total;
            this.paymentMethod = paymentMethod;
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // PASSWORDS ARE STORED AS SHA-256 HASHES (same as SHA2(..., 256) in the SQL script)
    public static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // LOGIN
    public static boolean validateLogin(String role, String username, String password)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE role = ? AND username = ? AND password_hash = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, username);
            ps.setString(3, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static String getUsername(String role) throws SQLException {
        String sql = "SELECT username FROM users WHERE role = ? LIMIT 1";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        }
    }

    public static void updateCredentials(String role, String newUsername, String newPassword)
            throws SQLException {
        String sql = "UPDATE users SET username = ?, password_hash = ? WHERE role = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setString(2, hash(newPassword));
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    // PRODUCTS AND STOCK
    public static void loadProducts(Map<String, Integer> prices, Map<String, Integer> stocks)
            throws SQLException {
        String sql = "SELECT name, price, stock FROM products ORDER BY id";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            prices.clear();
            stocks.clear();
            while (rs.next()) {
                prices.put(rs.getString("name"), rs.getInt("price"));
                stocks.put(rs.getString("name"), rs.getInt("stock"));
            }
        }
    }

    // Refresh only the stock numbers (prices and product list stay as they are)
    public static void loadStocks(Map<String, Integer> stocks) throws SQLException {
        String sql = "SELECT name, stock FROM products";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stocks.put(rs.getString("name"), rs.getInt("stock"));
            }
        }
    }

    public static void updateStocks(Map<String, Integer> newStocks) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE name = ?";
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (Map.Entry<String, Integer> e : newStocks.entrySet()) {
                    ps.setInt(1, e.getValue());
                    ps.setString(2, e.getKey());
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    // SAVE A COMPLETED ORDER: order row + item rows + stock deduction, all in one transaction
    public static String saveOrder(String role, String paymentMethod, double total,
                                   Map<String, Integer> cart, Map<String, Integer> prices)
            throws SQLException {
        String insertOrder = "INSERT INTO orders (processed_by, payment_method, total) VALUES (?, ?, ?)";
        String insertItem = "INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price) "
                + "SELECT ?, id, name, ?, ? FROM products WHERE name = ?";
        String deductStock = "UPDATE products SET stock = stock - ? WHERE name = ? AND stock >= ?";

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                long orderNumber;
                try (PreparedStatement ps = con.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, role);
                    ps.setString(2, paymentMethod);
                    ps.setDouble(3, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderNumber = keys.getLong(1);
                    }
                }

                try (PreparedStatement item = con.prepareStatement(insertItem);
                     PreparedStatement stock = con.prepareStatement(deductStock)) {
                    for (Map.Entry<String, Integer> e : cart.entrySet()) {
                        String name = e.getKey();
                        int qty = e.getValue();

                        stock.setInt(1, qty);
                        stock.setString(2, name);
                        stock.setInt(3, qty);
                        if (stock.executeUpdate() == 0) {
                            throw new SQLException("Not enough stock for: " + name);
                        }

                        item.setLong(1, orderNumber);
                        item.setInt(2, qty);
                        item.setInt(3, prices.get(name));
                        item.setString(4, name);
                        item.executeUpdate();
                    }
                }

                con.commit();
                return "#ORD-" + orderNumber;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    // COMPLETED ORDERS HISTORY (oldest first, like the original list)
    public static List<OrderRecord> loadOrders() throws SQLException {
        List<OrderRecord> list = new ArrayList<>();
        Map<Long, OrderRecord> byId = new LinkedHashMap<>();

        String orderSql = "SELECT id, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS ts, "
                + "processed_by, payment_method, total FROM orders ORDER BY id";
        String itemSql = "SELECT order_id, product_name, quantity, unit_price "
                + "FROM order_items ORDER BY id";

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery(orderSql)) {
                while (rs.next()) {
                    OrderRecord rec = new OrderRecord("#ORD-" + rs.getLong("id"),
                            rs.getString("ts"), rs.getString("processed_by"),
                            rs.getDouble("total"), rs.getString("payment_method"));
                    byId.put(rs.getLong("id"), rec);
                    list.add(rec);
                }
            }
            try (ResultSet rs = st.executeQuery(itemSql)) {
                while (rs.next()) {
                    OrderRecord rec = byId.get(rs.getLong("order_id"));
                    if (rec == null) continue;
                    String name = rs.getString("product_name");
                    rec.items.merge(name, rs.getInt("quantity"), Integer::sum);
                    rec.unitPrices.put(name, rs.getInt("unit_price"));
                }
            }
        }
        return list;
    }
}
