package vapor.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CartItem {
    private final long gameId;
    private final String gameTitle;
    private final BigDecimal unitPrice;
    private final OffsetDateTime addedAt;

    public CartItem(long gameId, String gameTitle, BigDecimal unitPrice, OffsetDateTime addedAt) {
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.unitPrice = unitPrice;
        this.addedAt = addedAt;
    }

    public long getGameId() {
        return gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }
}
