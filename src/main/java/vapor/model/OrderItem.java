package vapor.model;

import java.math.BigDecimal;

public class OrderItem {
    private final long gameId;
    private final String gameTitle;
    private final BigDecimal pricePaid;

    public OrderItem(long gameId, String gameTitle, BigDecimal pricePaid) {
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.pricePaid = pricePaid;
    }

    public long getGameId() {
        return gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }
}
