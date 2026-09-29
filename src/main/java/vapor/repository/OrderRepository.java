package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import vapor.Database;
import vapor.NotFoundException;
import vapor.model.Order;
import vapor.model.OrderItem;
import vapor.model.OrderStatus;
import vapor.model.PaymentMethod;
import vapor.model.SalesMetrics;

public class OrderRepository {
    private Database database;

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
        String sql = "INSERT INTO orders (id, user_id, total, payment_method, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, order.getId());
            statement.setObject(2, order.getUserId());
            statement.setBigDecimal(3, order.getTotal());
            statement.setString(4, order.getPaymentMethod().name().toLowerCase());
            statement.setString(5, order.getStatus().name().toLowerCase());
            statement.setObject(6, order.getCreatedAt());
            statement.executeUpdate();
        }
    }

    private void insertItem(Connection connection, Order order, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (id, order_id, game_id, price_paid) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, item.getId());
            statement.setObject(2, order.getId());
            statement.setObject(3, item.getGameId());
            statement.setBigDecimal(4, item.getPricePaid());
            statement.executeUpdate();
        }
    }

    private void insertLibraryEntry(Connection connection, Order order, OrderItem item) throws SQLException {
        String sql = "INSERT INTO library_entries (id, user_id, game_id, acquired_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, order.getUserId());
            statement.setObject(3, item.getGameId());
            statement.setObject(4, order.getCreatedAt());
            statement.executeUpdate();
        }
    }

    private void clearCart(Connection connection, UUID userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.executeUpdate();
        }
    }

    public List<Order> findByUser(UUID userId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                orders.add(rowToOrder(connection, rows));
            }
        }
        return orders;
    }

    public Order findByItem(UUID orderItemId) throws SQLException {
        String sql = "SELECT o.* FROM orders o JOIN order_items i ON i.order_id = o.id WHERE i.id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, orderItemId);
            ResultSet row = statement.executeQuery();
            if (!row.next()) {
                throw new NotFoundException("Order item " + orderItemId + " not found");
            }
            return rowToOrder(connection, row);
        }
    }

    public List<SalesMetrics> salesByCreator(UUID creatorId) throws SQLException {
        String sql = "SELECT g.id, g.title, COUNT(i.id) AS units_sold,"
                + " COALESCE(SUM(i.price_paid), 0) AS gross_revenue"
                + " FROM games g"
                + " LEFT JOIN order_items i ON i.game_id = g.id"
                + " AND i.id NOT IN (SELECT order_item_id FROM refunds WHERE status = 'approved')"
                + " WHERE g.creator_id = ?"
                + " GROUP BY g.id, g.title ORDER BY g.title";
        List<SalesMetrics> metrics = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, creatorId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                metrics.add(new SalesMetrics(
                        rows.getObject("id", UUID.class),
                        rows.getString("title"),
                        rows.getInt("units_sold"),
                        rows.getBigDecimal("gross_revenue")));
            }
        }
        return metrics;
    }

    private Order rowToOrder(Connection connection, ResultSet row) throws SQLException {
        Order order = new Order(
                row.getObject("id", UUID.class),
                row.getObject("user_id", UUID.class),
                row.getBigDecimal("total"),
                PaymentMethod.valueOf(row.getString("payment_method").toUpperCase()),
                OrderStatus.valueOf(row.getString("status").toUpperCase()),
                row.getObject("created_at", OffsetDateTime.class));
        String sql = "SELECT i.*, g.title, r.id AS refund_id FROM order_items i"
                + " JOIN games g ON g.id = i.game_id"
                + " LEFT JOIN refunds r ON r.order_item_id = i.id AND r.status = 'approved'"
                + " WHERE i.order_id = ? ORDER BY g.title";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, order.getId());
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                boolean refunded = rows.getObject("refund_id") != null;
                order.addItem(new OrderItem(
                        rows.getObject("id", UUID.class),
                        rows.getObject("game_id", UUID.class),
                        rows.getString("title"),
                        rows.getBigDecimal("price_paid"),
                        refunded));
            }
        }
        return order;
    }
}
