package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import vapor.Database;
import vapor.model.Order;
import vapor.model.OrderItem;
import vapor.model.OrderStatus;
import vapor.model.PaymentMethod;
import vapor.model.SalesMetrics;

public class OrderRepository {
    private final Database database;

    public OrderRepository(Database database) {
        this.database = database;
    }

    // Saves the order, fills the library and empties the cart in a single transaction.
    public Order saveCompletedOrder(Order order) throws SQLException {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            insertOrder(connection, order);
            for (OrderItem item : order.getItems()) {
                insertItem(connection, order, item);
                insertLibraryEntry(connection, order, item);
            }
            clearCart(connection, order.getUserId());
            connection.commit();
        }
        return order;
    }

    private void insertOrder(Connection connection, Order order) throws SQLException {
        String sql = "INSERT INTO orders (user_id, total, payment_method, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, order.getUserId());
            statement.setBigDecimal(2, order.getTotal());
            statement.setString(3, order.getPaymentMethod().name().toLowerCase());
            statement.setString(4, order.getStatus().name().toLowerCase());
            statement.setObject(5, order.getCreatedAt());
            ResultSet row = statement.executeQuery();
            row.next();
            order.setId(row.getLong("id"));
        }
    }

    private void insertItem(Connection connection, Order order, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, game_id, price_paid) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, order.getId());
            statement.setLong(2, item.getGameId());
            statement.setBigDecimal(3, item.getPricePaid());
            statement.executeUpdate();
        }
    }

    private void insertLibraryEntry(Connection connection, Order order, OrderItem item) throws SQLException {
        String sql = "INSERT INTO library_entries (user_id, game_id, acquired_at) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, order.getUserId());
            statement.setLong(2, item.getGameId());
            statement.setObject(3, order.getCreatedAt());
            statement.executeUpdate();
        }
    }

    private void clearCart(Connection connection, long userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    public List<Order> findByUser(long userId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                orders.add(rowToOrder(connection, rows));
            }
        }
        return orders;
    }

    public List<SalesMetrics> salesByCreator(long creatorId) throws SQLException {
        String sql = "SELECT g.id, g.title, COUNT(i.id) AS units_sold,"
                + " COALESCE(SUM(i.price_paid), 0) AS gross_revenue"
                + " FROM games g"
                + " LEFT JOIN order_items i ON i.game_id = g.id"
                + " WHERE g.creator_id = ?"
                + " GROUP BY g.id, g.title ORDER BY g.title";
        List<SalesMetrics> metrics = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, creatorId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                metrics.add(new SalesMetrics(
                        rows.getLong("id"),
                        rows.getString("title"),
                        rows.getInt("units_sold"),
                        rows.getBigDecimal("gross_revenue")));
            }
        }
        return metrics;
    }

    private Order rowToOrder(Connection connection, ResultSet row) throws SQLException {
        Order order = new Order(
                row.getLong("id"),
                row.getLong("user_id"),
                row.getBigDecimal("total"),
                PaymentMethod.valueOf(row.getString("payment_method").toUpperCase()),
                OrderStatus.valueOf(row.getString("status").toUpperCase()),
                row.getObject("created_at", OffsetDateTime.class));
        String sql = "SELECT i.*, g.title FROM order_items i JOIN games g ON g.id = i.game_id"
                + " WHERE i.order_id = ? ORDER BY g.title";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, order.getId());
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                order.addItem(new OrderItem(
                        rows.getLong("game_id"),
                        rows.getString("title"),
                        rows.getBigDecimal("price_paid")));
            }
        }
        return order;
    }
}
