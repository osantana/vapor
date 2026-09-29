package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import vapor.Database;
import vapor.NotFoundException;
import vapor.model.Role;
import vapor.model.User;

public class UserRepository {
    private Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    public User findById(UUID userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            ResultSet row = statement.executeQuery();
            if (!row.next()) {
                throw new NotFoundException("User " + userId + " not found");
            }
            return rowToUser(row);
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            ResultSet row = statement.executeQuery();
            if (!row.next()) {
                throw new NotFoundException("User not found");
            }
            return rowToUser(row);
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            ResultSet row = statement.executeQuery();
            return row.next();
        }
    }

    public User save(User user) throws SQLException {
        String sql = "INSERT INTO users (id, name, email, password_hash, role, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)"
                + " ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email,"
                + " password_hash = EXCLUDED.password_hash, role = EXCLUDED.role";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, user.getId());
            statement.setString(2, user.getName());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.setString(5, user.getRole().name().toLowerCase());
            statement.setObject(6, user.getCreatedAt());
            statement.executeUpdate();
        }
        return user;
    }

    private User rowToUser(ResultSet row) throws SQLException {
        return new User(
                row.getObject("id", UUID.class),
                row.getString("name"),
                row.getString("email"),
                row.getString("password_hash"),
                Role.valueOf(row.getString("role").toUpperCase()),
                row.getObject("created_at", OffsetDateTime.class));
    }
}
