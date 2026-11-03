package vapor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void createSchema() throws SQLException, IOException {
        String sql;
        try (InputStream input = Database.class.getResourceAsStream("/schema.sql")) {
            if (input != null) {
                sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        }
    }
}
