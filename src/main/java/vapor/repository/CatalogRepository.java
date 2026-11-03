package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import vapor.Database;
import vapor.NotFoundException;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.model.GameFilter;
import vapor.model.GameStatus;
import vapor.model.Genre;
import vapor.model.Platform;

public class CatalogRepository {
    private static final String SELECT_GAMES = "SELECT g.*, u.name AS creator_name FROM games g"
            + " JOIN users u ON u.id = g.creator_id";

    private final Database database;

    public CatalogRepository(Database database) {
        this.database = database;
    }

    public Game findById(long gameId) throws SQLException {
        String sql = SELECT_GAMES + " WHERE g.id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gameId);
            ResultSet row = statement.executeQuery();
            if (!row.next()) {
                throw new NotFoundException("Game " + gameId + " not found");
            }
            return rowToGame(connection, row);
        }
    }

    public List<Game> findByCreator(long creatorId) throws SQLException {
        String sql = SELECT_GAMES + " WHERE g.creator_id = ? ORDER BY g.created_at DESC";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, creatorId);
            return rowsToGames(connection, statement.executeQuery());
        }
    }

    public List<Game> listPending() throws SQLException {
        String sql = SELECT_GAMES + " WHERE g.status = 'pending' ORDER BY g.created_at";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return rowsToGames(connection, statement.executeQuery());
        }
    }

    // Returns one game more than the page size when there is a next page.
    public List<Game> search(GameFilter filter) throws SQLException {
        String sql = SELECT_GAMES + " WHERE g.status = 'approved'";
        List<Object> parameters = new ArrayList<>();
        if (!filter.getQuery().isEmpty()) {
            sql += " AND g.title ILIKE ?";
            parameters.add("%" + filter.getQuery() + "%");
        }
        if (filter.getGenreId() != 0) {
            sql += " AND g.id IN (SELECT game_id FROM game_genres WHERE genre_id = ?)";
            parameters.add(filter.getGenreId());
        }
        if (filter.getPlatform() != null) {
            sql += " AND g.id IN (SELECT game_id FROM game_assets WHERE platform = ?)";
            parameters.add(filter.getPlatform().getCode());
        }
        if (filter.getMinimumPrice() != null) {
            sql += " AND g.price >= ?";
            parameters.add(filter.getMinimumPrice());
        }
        if (filter.getMaximumPrice() != null) {
            sql += " AND g.price <= ?";
            parameters.add(filter.getMaximumPrice());
        }
        if (filter.getReleasedFrom() != null) {
            sql += " AND g.release_date >= ?";
            parameters.add(filter.getReleasedFrom());
        }
        sql += " ORDER BY " + orderBy(filter.getSort()) + " LIMIT ? OFFSET ?";
        parameters.add(GameFilter.PAGE_SIZE + 1);
        parameters.add(filter.offset());

        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setObject(index + 1, parameters.get(index));
            }
            return rowsToGames(connection, statement.executeQuery());
        }
    }

    private String orderBy(String sort) {
        if (sort.equals("price")) {
            return "g.price ASC, g.id";
        }
        if (sort.equals("release")) {
            return "g.release_date DESC, g.id";
        }
        return "g.created_at DESC, g.id";
    }

    public Game insert(Game game) throws SQLException {
        String sql = "INSERT INTO games (creator_id, title, description, system_requirements, cover_url,"
                + " price, release_date, status, review_notes, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, game.getCreatorId());
                statement.setString(2, game.getTitle());
                statement.setString(3, game.getDescription());
                statement.setString(4, game.getSystemRequirements());
                statement.setString(5, game.getCoverUrl());
                statement.setBigDecimal(6, game.getPrice());
                statement.setObject(7, game.getReleaseDate());
                statement.setString(8, game.getStatus().name().toLowerCase());
                statement.setString(9, game.getReviewNotes());
                statement.setObject(10, game.getCreatedAt());
                ResultSet row = statement.executeQuery();
                row.next();
                game.setId(row.getLong("id"));
            }
            insertGenres(connection, game);
            insertAssets(connection, game);
            connection.commit();
        }
        return game;
    }

    private void insertGenres(Connection connection, Game game) throws SQLException {
        String sql = "INSERT INTO game_genres (game_id, genre_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Genre genre : game.getGenres()) {
                statement.setLong(1, game.getId());
                statement.setLong(2, genre.getId());
                statement.executeUpdate();
            }
        }
    }

    private void insertAssets(Connection connection, Game game) throws SQLException {
        String sql = "INSERT INTO game_assets (game_id, platform, file_size, download_url) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (GameAsset asset : game.getAssets()) {
                statement.setLong(1, game.getId());
                statement.setString(2, asset.getPlatform().getCode());
                statement.setLong(3, asset.getFileSize());
                statement.setString(4, asset.getDownloadUrl());
                statement.executeUpdate();
            }
        }
    }

    public void updateReview(Game game) throws SQLException {
        String sql = "UPDATE games SET status = ?, review_notes = ? WHERE id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, game.getStatus().name().toLowerCase());
            statement.setString(2, game.getReviewNotes());
            statement.setLong(3, game.getId());
            statement.executeUpdate();
        }
    }

    public void updatePrice(Game game) throws SQLException {
        String sql = "UPDATE games SET price = ? WHERE id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, game.getPrice());
            statement.setLong(2, game.getId());
            statement.executeUpdate();
        }
    }

    public List<Genre> listGenres() throws SQLException {
        List<Genre> genres = new ArrayList<>();
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            ResultSet rows = statement.executeQuery("SELECT * FROM genres ORDER BY name");
            while (rows.next()) {
                genres.add(new Genre(rows.getLong("id"), rows.getString("name")));
            }
        }
        return genres;
    }

    private List<Game> rowsToGames(Connection connection, ResultSet rows) throws SQLException {
        List<Game> games = new ArrayList<>();
        while (rows.next()) {
            games.add(rowToGame(connection, rows));
        }
        return games;
    }

    // ponytail: genres and assets cost two extra queries per game; join them in one query if the catalog gets slow
    private Game rowToGame(Connection connection, ResultSet row) throws SQLException {
        Game game = new Game(
                row.getLong("id"),
                row.getLong("creator_id"),
                row.getString("title"),
                row.getString("description"),
                row.getBigDecimal("price"),
                row.getObject("release_date", LocalDate.class),
                GameStatus.valueOf(row.getString("status").toUpperCase()),
                row.getObject("created_at", OffsetDateTime.class));
        game.setCreatorName(row.getString("creator_name"));
        game.setSystemRequirements(row.getString("system_requirements"));
        game.setCoverUrl(row.getString("cover_url"));
        game.setReviewNotes(row.getString("review_notes"));
        loadGenres(connection, game);
        loadAssets(connection, game);
        return game;
    }

    private void loadGenres(Connection connection, Game game) throws SQLException {
        String sql = "SELECT g.* FROM genres g JOIN game_genres gg ON g.id = gg.genre_id"
                + " WHERE gg.game_id = ? ORDER BY g.name";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, game.getId());
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                game.addGenre(new Genre(rows.getLong("id"), rows.getString("name")));
            }
        }
    }

    private void loadAssets(Connection connection, Game game) throws SQLException {
        String sql = "SELECT * FROM game_assets WHERE game_id = ? ORDER BY platform";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, game.getId());
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                game.addAsset(new GameAsset(
                        rows.getLong("id"),
                        Platform.fromCode(rows.getString("platform")),
                        rows.getLong("file_size"),
                        rows.getString("download_url")));
            }
        }
    }
}
