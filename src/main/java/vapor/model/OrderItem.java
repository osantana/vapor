package vapor.model;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {
    private UUID id;
    private UUID gameId;
    private String gameTitle;
    private BigDecimal pricePaid;
    private boolean refunded;

    public OrderItem(UUID id, UUID gameId, String gameTitle, BigDecimal pricePaid, boolean refunded) {
        this.id = id;
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.pricePaid = pricePaid;
        this.refunded = refunded;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    public boolean isRefunded() {
        return refunded;
    }
}
