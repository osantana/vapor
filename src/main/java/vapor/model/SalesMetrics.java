package vapor.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class SalesMetrics {
    private static final BigDecimal PUBLISHER_SHARE = new BigDecimal("0.70");

    private UUID gameId;
    private String gameTitle;
    private int unitsSold;
    private BigDecimal grossRevenue;

    public SalesMetrics(UUID gameId, String gameTitle, int unitsSold, BigDecimal grossRevenue) {
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.unitsSold = unitsSold;
        this.grossRevenue = grossRevenue;
    }

    public BigDecimal getNetRevenue() {
        return grossRevenue.multiply(PUBLISHER_SHARE).setScale(2, RoundingMode.HALF_EVEN);
    }

    public UUID getGameId() {
        return gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public int getUnitsSold() {
        return unitsSold;
    }

    public BigDecimal getGrossRevenue() {
        return grossRevenue;
    }
}
