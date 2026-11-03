package vapor.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import vapor.BusinessRuleException;

public class Cart {
    private final UUID userId;
    private final List<CartItem> items = new ArrayList<>();

    public Cart(UUID userId) {
        this.userId = userId;
    }

    public CartItem addItem(Game game) {
        if (contains(game.getId())) {
            throw new BusinessRuleException("error.alreadyInCart");
        }
        CartItem item = new CartItem(UUID.randomUUID(), game.getId(), game.getTitle(), game.getPrice(),
                OffsetDateTime.now());
        items.add(item);
        return item;
    }

    public void restoreItem(CartItem item) {
        items.add(item);
    }

    public boolean contains(UUID gameId) {
        for (CartItem item : items) {
            if (item.getGameId().equals(gameId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public BigDecimal total() {
        BigDecimal total = new BigDecimal("0.00");
        for (CartItem item : items) {
            total = total.add(item.getUnitPrice());
        }
        return total;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<CartItem> getItems() {
        return items;
    }
}
