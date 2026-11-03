package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import vapor.Database;
import vapor.model.Cart;
import vapor.model.CartItem;

public class CartRepository {
    private final Database database;

    public CartRepository(Database database) {
        this.database = database;
    }

    public Cart findByUser(UUID userId) throws SQLException {
        String sql = "SELECT c.*, g.title FROM cart_items c JOIN games g ON g.id = c.game_id"
                + " WHERE c.user_id = ? ORDER BY c.added_at";
        Cart cart = new Cart(userId);
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                cart.restoreItem(new CartItem(
                        rows.getObject("id", UUID.class),
                        rows.getObject("game_id", UUID.class),
                        rows.getString("title"),
                        rows.getBigDecimal("unit_price"),
                        rows.getObject("added_at", OffsetDateTime.class)));
            }
        }
        return cart;
    }

    public int countItems(UUID userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cart_items WHERE user_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            ResultSet row = statement.executeQuery();
            row.next();
            return row.getInt(1);
        }
    }

    public void addItem(UUID userId, CartItem item) throws SQLException {
        String sql = "INSERT INTO cart_items (id, user_id, game_id, unit_price, added_at) VALUES (?, ?, ?, ?, ?)"
                + " ON CONFLICT (user_id, game_id) DO NOTHING";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, item.getId());
            statement.setObject(2, userId);
            statement.setObject(3, item.getGameId());
            statement.setBigDecimal(4, item.getUnitPrice());
            statement.setObject(5, item.getAddedAt());
            statement.executeUpdate();
        }
    }

    public void removeItem(UUID userId, UUID gameId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND game_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setObject(2, gameId);
            statement.executeUpdate();
        }
    }
}
