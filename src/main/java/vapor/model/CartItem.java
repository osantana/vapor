package vapor.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CartItem {
    private UUID id;
    private UUID gameId;
    private String gameTitle;
    private BigDecimal unitPrice;
    private OffsetDateTime addedAt;

    public CartItem(UUID id, UUID gameId, String gameTitle, BigDecimal unitPrice, OffsetDateTime addedAt) {
        this.id = id;
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.unitPrice = unitPrice;
        this.addedAt = addedAt;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }
}
