package vapor.service;

import java.sql.SQLException;
import java.time.OffsetDateTime;

import vapor.BusinessRuleException;
import vapor.model.Cart;
import vapor.model.CartItem;
import vapor.model.Order;
import vapor.model.OrderItem;
import vapor.model.OrderStatus;
import vapor.model.PaymentMethod;
import vapor.repository.CartRepository;
import vapor.repository.LibraryRepository;
import vapor.repository.OrderRepository;

public class CheckoutService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final LibraryRepository libraryRepository;
    private final PaymentGateway paymentGateway;

    public CheckoutService(CartRepository cartRepository, OrderRepository orderRepository,
                           LibraryRepository libraryRepository, PaymentGateway paymentGateway) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.libraryRepository = libraryRepository;
        this.paymentGateway = paymentGateway;
    }

    public Order checkout(long userId, PaymentMethod method, String cardNumber) throws SQLException {
        Cart cart = cartRepository.findByUser(userId);
        if (cart.isEmpty()) {
            throw new BusinessRuleException("Seu carrinho está vazio.");
        }

        Order order = new Order(0, userId, cart.total(), method, OrderStatus.PENDING, OffsetDateTime.now());
        for (CartItem item : cart.getItems()) {
            if (libraryRepository.hasGame(userId, item.getGameId())) {
                throw new BusinessRuleException("Este jogo já está na sua biblioteca.");
            }
            order.addItem(new OrderItem(item.getGameId(), item.getGameTitle(), item.getUnitPrice()));
        }

        // A declined payment throws here, before anything is saved, so the cart stays intact.
        paymentGateway.charge(order, cardNumber);
        order.markAsPaid();
        return orderRepository.saveCompletedOrder(order);
    }
}
