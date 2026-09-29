package vapor.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import vapor.NotFoundException;

public class Order {
    private UUID id;
    private UUID userId;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private OffsetDateTime createdAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order(UUID id, UUID userId, BigDecimal total, PaymentMethod paymentMethod, OrderStatus status,
                 OffsetDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void markAsPaid() {
        status = OrderStatus.PAID;
    }

    public void cancel() {
        status = OrderStatus.CANCELLED;
    }

    public boolean belongsTo(UUID otherUserId) {
        return userId.equals(otherUserId);
    }

    public OrderItem findItem(UUID itemId) {
        for (OrderItem item : items) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        throw new NotFoundException("Order item " + itemId + " not found");
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
