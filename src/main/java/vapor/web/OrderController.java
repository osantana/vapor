package vapor.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.model.Order;
import vapor.model.OrderItem;
import vapor.model.PaymentMethod;
import vapor.repository.OrderRepository;
import vapor.service.CheckoutService;

public class OrderController {
    private final Views views;
    private final OrderRepository orderRepository;
    private final CheckoutService checkoutService;

    public OrderController(Views views, OrderRepository orderRepository, CheckoutService checkoutService) {
        this.views = views;
        this.orderRepository = orderRepository;
        this.checkoutService = checkoutService;
    }

    public void checkout(Context ctx) throws SQLException {
        PaymentMethod method = readPaymentMethod(ctx);
        String cardNumber = Requests.form(ctx, "cardNumber");
        Order order;
        try {
            order = checkoutService.checkout(Requests.currentUserId(ctx), method, cardNumber);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessageKey());
            ctx.redirect("/cart");
            return;
        }
        Requests.flash(ctx, "order.confirmed", String.valueOf(order.getId()));
        ctx.redirect("/orders");
    }

    private PaymentMethod readPaymentMethod(Context ctx) {
        String method = Requests.form(ctx, "paymentMethod");
        if (method.equals("pix")) {
            return PaymentMethod.PIX;
        }
        if (method.equals("credit_card")) {
            return PaymentMethod.CREDIT_CARD;
        }
        throw new BadRequestResponse();
    }

    public void list(Context ctx) throws IOException, SQLException {
        Messages messages = views.messages(ctx);
        List<Order> orders = orderRepository.findByUser(Requests.currentUserId(ctx));
        if (orders.isEmpty()) {
            String body = views.template(ctx, "message").set("text", messages.get("orders.empty")).render();
            views.render(ctx, messages.get("orders.title"), body);
            return;
        }

        String rendered = "";
        for (Order order : orders) {
            rendered += renderOrder(ctx, order, messages);
        }
        Template page = views.template(ctx, "orders");
        page.setHtml("orders", rendered);
        views.render(ctx, messages.get("orders.title"), page.render());
    }

    private String renderOrder(Context ctx, Order order, Messages messages) throws IOException {
        String items = "";
        for (OrderItem item : order.getItems()) {
            Template row = views.template(ctx, "order_item");
            row.set("title", item.getGameTitle());
            row.set("price", Format.price(item.getPricePaid(), messages));
            items += row.render();
        }
        Template template = views.template(ctx, "order");
        template.set("id", order.getId());
        template.set("date", Format.dateTime(order.getCreatedAt()));
        template.set("method", messages.get("payment." + order.getPaymentMethod().name().toLowerCase()));
        template.set("total", Format.money(order.getTotal()));
        template.setHtml("items", items);
        return template.render();
    }
}
