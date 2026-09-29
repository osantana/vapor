package vapor.service;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import vapor.BusinessRuleException;
import vapor.NotFoundException;
import vapor.model.LibraryEntry;
import vapor.model.Order;
import vapor.model.OrderItem;
import vapor.model.Refund;
import vapor.model.RefundStatus;
import vapor.repository.LibraryRepository;
import vapor.repository.OrderRepository;
import vapor.repository.RefundRepository;

public class RefundService {
    private RefundPolicy refundPolicy;
    private PaymentGateway paymentGateway;
    private OrderRepository orderRepository;
    private LibraryRepository libraryRepository;
    private RefundRepository refundRepository;

    public RefundService(RefundPolicy refundPolicy, PaymentGateway paymentGateway,
                         OrderRepository orderRepository, LibraryRepository libraryRepository,
                         RefundRepository refundRepository) {
        this.refundPolicy = refundPolicy;
        this.paymentGateway = paymentGateway;
        this.orderRepository = orderRepository;
        this.libraryRepository = libraryRepository;
        this.refundRepository = refundRepository;
    }

    public Refund request(UUID userId, UUID orderItemId, String reason) throws SQLException {
        if (reason.trim().isEmpty()) {
            throw new BusinessRuleException("error.refundReasonRequired");
        }
        Order order = orderRepository.findByItem(orderItemId);
        if (!order.belongsTo(userId)) {
            throw new NotFoundException("Order item " + orderItemId + " not found");
        }
        OrderItem item = order.findItem(orderItemId);
        if (item.isRefunded()) {
            throw new BusinessRuleException("error.alreadyRefunded");
        }

        LibraryEntry entry = libraryRepository.findEntry(userId, item.getGameId());
        String rejection = refundPolicy.rejectionReason(order.getCreatedAt(), entry.getMinutesPlayed(),
                OffsetDateTime.now());
        if (!rejection.isEmpty()) {
            throw new BusinessRuleException(rejection);
        }

        Refund refund = new Refund(UUID.randomUUID(), orderItemId, reason.trim(), RefundStatus.REQUESTED,
                OffsetDateTime.now());
        refund.approve();
        refundRepository.saveApprovedRefund(refund, entry);
        paymentGateway.refund(order, item.getPricePaid());
        return refund;
    }
}
