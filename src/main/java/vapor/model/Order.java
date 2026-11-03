package vapor.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private long id;
    private final long userId;
    private final BigDecimal total;
    private final PaymentMethod paymentMethod;
    private OrderStatus status;
    private final OffsetDateTime createdAt;
    private final List<OrderItem> items = new ArrayList<>();

    public Order(long id, long userId, BigDecimal total, PaymentMethod paymentMethod, OrderStatus status,
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
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
