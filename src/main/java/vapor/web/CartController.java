package vapor.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.NotFoundException;
import vapor.model.Cart;
import vapor.model.CartItem;
import vapor.model.Game;
import vapor.repository.CartRepository;
import vapor.repository.CatalogRepository;
import vapor.repository.LibraryRepository;

public class CartController {
    private Views views;
    private CartRepository cartRepository;
    private CatalogRepository catalogRepository;
    private LibraryRepository libraryRepository;

    public CartController(Views views, CartRepository cartRepository, CatalogRepository catalogRepository,
                          LibraryRepository libraryRepository) {
        this.views = views;
        this.cartRepository = cartRepository;
        this.catalogRepository = catalogRepository;
        this.libraryRepository = libraryRepository;
    }

    public void show(Context ctx) throws IOException, SQLException {
        Messages messages = views.messages(ctx);
        Cart cart = cartRepository.findByUser(Requests.currentUserId(ctx));
        if (cart.isEmpty()) {
            String body = views.template(ctx, "message").set("text", messages.get("cart.empty")).render();
            views.render(ctx, messages.get("cart.title"), body);
            return;
        }

        String items = "";
        for (CartItem item : cart.getItems()) {
            Template row = views.template(ctx, "cart_item");
            row.set("gameId", item.getGameId());
            row.set("title", item.getGameTitle());
            row.set("price", Format.price(item.getUnitPrice(), messages));
            items += row.render();
        }

        Template page = views.template(ctx, "cart");
        page.setHtml("items", items);
        page.set("total", Format.money(cart.total()));
        views.render(ctx, messages.get("cart.title"), page.render());
    }

    public void add(Context ctx) throws SQLException {
        UUID userId = Requests.currentUserId(ctx);
        Game game = catalogRepository.findById(Requests.uuid(Requests.form(ctx, "gameId")));
        if (!game.isApproved()) {
            throw new NotFoundException("Game " + game.getId() + " not found");
        }
        if (libraryRepository.hasGame(userId, game.getId())) {
            Requests.flashError(ctx, "error.alreadyInLibrary");
            ctx.redirect("/games/" + game.getId());
            return;
        }

        Cart cart = cartRepository.findByUser(userId);
        try {
            CartItem item = cart.addItem(game);
            cartRepository.addItem(userId, item);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessageKey());
            ctx.redirect("/games/" + game.getId());
            return;
        }
        Requests.flash(ctx, "cart.added");
        ctx.redirect("/cart");
    }

    public void remove(Context ctx) throws SQLException {
        UUID gameId = Requests.uuid(Requests.form(ctx, "gameId"));
        cartRepository.removeItem(Requests.currentUserId(ctx), gameId);
        Requests.flash(ctx, "cart.removed");
        ctx.redirect("/cart");
    }
}
