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
import java.util.UUID;

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

    private Database database;

    public CatalogRepository(Database database) {
        this.database = database;
    }

    public Game findById(UUID gameId) throws SQLException {
        String sql = SELECT_GAMES + " WHERE g.id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, gameId);
            ResultSet row = statement.executeQuery();
            if (!row.next()) {
                throw new NotFoundException("Game " + gameId + " not found");
            }
            return rowToGame(connection, row);
        }
    }

    public List<Game> findByCreator(UUID creatorId) throws SQLException {
        String sql = SELECT_GAMES + " WHERE g.creator_id = ? ORDER BY g.created_at DESC";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, creatorId);
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
        if (filter.getGenreId() != null) {
            sql += " AND g.id IN (SELECT game_id FROM game_genres WHERE genre_id = ?)";
            parameters.add(filter.getGenreId());
        }
        if (filter.getPlatformId() != null) {
            sql += " AND g.id IN (SELECT game_id FROM game_platforms WHERE platform_id = ?)";
            parameters.add(filter.getPlatformId());
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
        String sql = "INSERT INTO games (id, creator_id, title, description, system_requirements, cover_url,"
                + " price, release_date, status, review_notes, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, game.getId());
                statement.setObject(2, game.getCreatorId());
                statement.setString(3, game.getTitle());
                statement.setString(4, game.getDescription());
                statement.setString(5, game.getSystemRequirements());
                statement.setString(6, game.getCoverUrl());
                statement.setBigDecimal(7, game.getPrice());
                statement.setObject(8, game.getReleaseDate());
                statement.setString(9, game.getStatus().name().toLowerCase());
                statement.setString(10, game.getReviewNotes());
                statement.setObject(11, game.getCreatedAt());
                statement.executeUpdate();
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
                statement.setObject(1, game.getId());
                statement.setObject(2, genre.getId());
                statement.executeUpdate();
            }
        }
    }

    private void insertAssets(Connection connection, Game game) throws SQLException {
        String platformSql = "INSERT INTO game_platforms (game_id, platform_id) VALUES (?, ?)";
        String assetSql = "INSERT INTO game_assets (id, game_id, platform_id, file_size, download_url)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement platformStatement = connection.prepareStatement(platformSql);
             PreparedStatement assetStatement = connection.prepareStatement(assetSql)) {
            for (GameAsset asset : game.getAssets()) {
                platformStatement.setObject(1, game.getId());
                platformStatement.setObject(2, asset.getPlatformId());
                platformStatement.executeUpdate();

                assetStatement.setObject(1, asset.getId());
                assetStatement.setObject(2, game.getId());
                assetStatement.setObject(3, asset.getPlatformId());
                assetStatement.setLong(4, asset.getFileSize());
                assetStatement.setString(5, asset.getDownloadUrl());
                assetStatement.executeUpdate();
            }
        }
    }

    public void updateReview(Game game) throws SQLException {
        String sql = "UPDATE games SET status = ?, review_notes = ? WHERE id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, game.getStatus().name().toLowerCase());
            statement.setString(2, game.getReviewNotes());
            statement.setObject(3, game.getId());
            statement.executeUpdate();
        }
    }

    public void updatePrice(Game game) throws SQLException {
        String sql = "UPDATE games SET price = ? WHERE id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, game.getPrice());
            statement.setObject(2, game.getId());
            statement.executeUpdate();
        }
    }

    public List<Genre> listGenres() throws SQLException {
        List<Genre> genres = new ArrayList<>();
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            ResultSet rows = statement.executeQuery("SELECT * FROM genres ORDER BY name");
            while (rows.next()) {
                genres.add(new Genre(rows.getObject("id", UUID.class), rows.getString("name")));
            }
        }
        return genres;
    }

    public List<Platform> listPlatforms() throws SQLException {
        List<Platform> platforms = new ArrayList<>();
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            ResultSet rows = statement.executeQuery("SELECT * FROM platforms ORDER BY name");
            while (rows.next()) {
                platforms.add(new Platform(rows.getObject("id", UUID.class), rows.getString("name")));
            }
        }
        return platforms;
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
                row.getObject("id", UUID.class),
                row.getObject("creator_id", UUID.class),
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
            statement.setObject(1, game.getId());
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                game.addGenre(new Genre(rows.getObject("id", UUID.class), rows.getString("name")));
            }
        }
    }

    private void loadAssets(Connection connection, Game game) throws SQLException {
        String sql = "SELECT a.*, p.name AS platform_name FROM game_assets a"
                + " JOIN platforms p ON p.id = a.platform_id WHERE a.game_id = ? ORDER BY p.name";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, game.getId());
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                Platform platform = new Platform(rows.getObject("platform_id", UUID.class),
                        rows.getString("platform_name"));
                game.addAsset(new GameAsset(
                        rows.getObject("id", UUID.class),
                        game.getId(),
                        platform,
                        rows.getLong("file_size"),
                        rows.getString("download_url")));
            }
        }
    }
}
