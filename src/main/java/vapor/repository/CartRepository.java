package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import vapor.Database;
import vapor.model.Cart;
import vapor.model.CartItem;

public class CartRepository {
    private final Database database;

    public CartRepository(Database database) {
        this.database = database;
    }

    public Cart findByUser(long userId) throws SQLException {
        String sql = "SELECT c.*, g.title FROM cart_items c JOIN games g ON g.id = c.game_id"
                + " WHERE c.user_id = ? ORDER BY c.added_at";
        Cart cart = new Cart(userId);
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                cart.restoreItem(new CartItem(
                        rows.getLong("game_id"),
                        rows.getString("title"),
                        rows.getBigDecimal("unit_price"),
                        rows.getObject("added_at", OffsetDateTime.class)));
            }
        }
        return cart;
    }

    public int countItems(long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cart_items WHERE user_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ResultSet row = statement.executeQuery();
            row.next();
            return row.getInt(1);
        }
    }

    public void addItem(long userId, CartItem item) throws SQLException {
        String sql = "INSERT INTO cart_items (user_id, game_id, unit_price, added_at) VALUES (?, ?, ?, ?)"
                + " ON CONFLICT (user_id, game_id) DO NOTHING";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, item.getGameId());
            statement.setBigDecimal(3, item.getUnitPrice());
            statement.setObject(4, item.getAddedAt());
            statement.executeUpdate();
        }
    }

    public void removeItem(long userId, long gameId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND game_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, gameId);
            statement.executeUpdate();
        }
    }
}
