package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import vapor.Database;
import vapor.model.LibraryEntry;

public class LibraryRepository {
    private final Database database;

    public LibraryRepository(Database database) {
        this.database = database;
    }

    public List<LibraryEntry> findByUser(long userId) throws SQLException {
        String sql = "SELECT * FROM library_entries WHERE user_id = ? ORDER BY acquired_at DESC";
        List<LibraryEntry> entries = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                entries.add(new LibraryEntry(rows.getLong("game_id"),
                        rows.getObject("acquired_at", OffsetDateTime.class)));
            }
        }
        return entries;
    }

    public boolean hasGame(long userId, long gameId) throws SQLException {
        String sql = "SELECT 1 FROM library_entries WHERE user_id = ? AND game_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, gameId);
            ResultSet row = statement.executeQuery();
            return row.next();
        }
    }
}
