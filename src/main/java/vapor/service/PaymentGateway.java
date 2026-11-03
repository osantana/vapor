package vapor.service;

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
            logger.info("PIX payment approved for order of " + order.getTotal());
            return;
        }
        String digits = cardNumber.replace(" ", "");
        if (digits.length() < 13 || digits.length() > 19 || !isAllDigits(digits)) {
            throw new BusinessRuleException("Informe um número de cartão válido.");
        }
        if (digits.endsWith("0000")) {
            throw new BusinessRuleException("Pagamento recusado. Seu carrinho foi mantido, tente novamente.");
        }
        logger.info("Card payment approved for order of " + order.getTotal());
    }

    private boolean isAllDigits(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (!Character.isDigit(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
