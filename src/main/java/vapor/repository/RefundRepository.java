package vapor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import vapor.Database;
import vapor.model.LibraryEntry;
import vapor.model.Refund;

public class RefundRepository {
    private Database database;

    public RefundRepository(Database database) {
        this.database = database;
    }

    // Saves the refund and removes the game from the library in a single transaction.
    public Refund saveApprovedRefund(Refund refund, LibraryEntry entry) throws SQLException {
        String refundSql = "INSERT INTO refunds (id, order_item_id, reason, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?)";
        String librarySql = "DELETE FROM library_entries WHERE id = ?";
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(refundSql)) {
                statement.setObject(1, refund.getId());
                statement.setObject(2, refund.getOrderItemId());
                statement.setString(3, refund.getReason());
                statement.setString(4, refund.getStatus().name().toLowerCase());
                statement.setObject(5, refund.getCreatedAt());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(librarySql)) {
                statement.setObject(1, entry.getId());
                statement.executeUpdate();
            }
            connection.commit();
        }
        return refund;
    }
}
