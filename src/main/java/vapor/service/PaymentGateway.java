package vapor.service;

import java.math.BigDecimal;
import java.util.logging.Logger;

import vapor.BusinessRuleException;
import vapor.model.Order;
import vapor.model.PaymentMethod;

// Simulated payment provider: no real charge happens and card data is never stored.
// Card numbers ending in 0000 are declined, so the refused payment flow can be tested.
public class PaymentGateway {
    private static final Logger logger = Logger.getLogger(PaymentGateway.class.getName());

    public void charge(Order order, String cardNumber) {
        if (order.getPaymentMethod() == PaymentMethod.PIX) {
            logger.info("PIX payment approved for order " + order.getId());
            return;
        }
        String digits = cardNumber.replace(" ", "");
        if (!digits.matches("[0-9]{13,19}")) {
            throw new BusinessRuleException("error.invalidCard");
        }
        if (digits.endsWith("0000")) {
            throw new BusinessRuleException("error.paymentDeclined");
        }
        logger.info("Card payment approved for order " + order.getId());
    }

    public void refund(Order order, BigDecimal amount) {
        logger.info("Refund of " + amount + " sent for order " + order.getId());
    }
}
