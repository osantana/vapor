package vapor.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SalesMetrics {
    private static final BigDecimal PUBLISHER_SHARE = new BigDecimal("0.70");

    private final long gameId;
    private final String gameTitle;
    private final int unitsSold;
    private final BigDecimal grossRevenue;

    public SalesMetrics(long gameId, String gameTitle, int unitsSold, BigDecimal grossRevenue) {
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.unitsSold = unitsSold;
        this.grossRevenue = grossRevenue;
    }

    public BigDecimal getNetRevenue() {
        return grossRevenue.multiply(PUBLISHER_SHARE).setScale(2, RoundingMode.HALF_EVEN);
    }

    public long getGameId() {
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
