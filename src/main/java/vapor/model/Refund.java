package vapor.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Refund {
    private UUID id;
    private UUID orderItemId;
    private String reason;
    private RefundStatus status;
    private OffsetDateTime createdAt;

    public Refund(UUID id, UUID orderItemId, String reason, RefundStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.orderItemId = orderItemId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void approve() {
        status = RefundStatus.APPROVED;
    }

    public void reject() {
        status = RefundStatus.REJECTED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public String getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
