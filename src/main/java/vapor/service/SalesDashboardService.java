package vapor.service;

import java.sql.SQLException;
import java.util.List;

import vapor.model.SalesMetrics;
import vapor.repository.OrderRepository;

public class SalesDashboardService {
    private final OrderRepository orderRepository;

    public SalesDashboardService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<SalesMetrics> getMetrics(long creatorId) throws SQLException {
        return orderRepository.salesByCreator(creatorId);
    }

    public String exportCsv(long creatorId) throws SQLException {
        String csv = "game,units_sold,gross_revenue,net_revenue\n";
        for (SalesMetrics metrics : getMetrics(creatorId)) {
            csv += escape(metrics.getGameTitle()) + ","
                    + metrics.getUnitsSold() + ","
                    + metrics.getGrossRevenue() + ","
                    + metrics.getNetRevenue() + "\n";
        }
        return csv;
    }

    // Quotes the text and neutralizes formulas, so spreadsheets never execute a game title.
    public static String escape(String text) {
        String safe = text.replace("\"", "\"\"");
        if (safe.matches("^[=+\\-@].*")) {
            safe = "'" + safe;
        }
        return "\"" + safe + "\"";
    }
}
