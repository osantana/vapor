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
import vapor.model.LibraryEntry;

public class LibraryRepository {
    private Database database;

    public LibraryRepository(Database database) {
        this.database = database;
    }

    public List<LibraryEntry> findByUser(UUID userId) throws SQLException {
        String sql = "SELECT * FROM library_entries WHERE user_id = ? ORDER BY acquired_at DESC";
        List<LibraryEntry> entries = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                entries.add(rowToEntry(rows));
            }
        }
        return entries;
    }

    public LibraryEntry findEntry(UUID userId, UUID gameId) throws SQLException {
        String sql = "SELECT * FROM library_entries WHERE user_id = ? AND game_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setObject(2, gameId);
            ResultSet row = statement.executeQuery();
            if (!row.next()) {
                throw new NotFoundException("Library entry not found");
            }
            return rowToEntry(row);
        }
    }

    public boolean hasGame(UUID userId, UUID gameId) throws SQLException {
        String sql = "SELECT 1 FROM library_entries WHERE user_id = ? AND game_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setObject(2, gameId);
            ResultSet row = statement.executeQuery();
            return row.next();
        }
    }

    private LibraryEntry rowToEntry(ResultSet row) throws SQLException {
        return new LibraryEntry(
                row.getObject("id", UUID.class),
                row.getObject("user_id", UUID.class),
                row.getObject("game_id", UUID.class),
                row.getObject("acquired_at", OffsetDateTime.class),
                row.getInt("minutes_played"));
    }
}
