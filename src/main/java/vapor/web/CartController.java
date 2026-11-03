package vapor.web;

import java.io.IOException;
import java.sql.SQLException;

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
    private final Views views;
    private final CartRepository cartRepository;
    private final CatalogRepository catalogRepository;
    private final LibraryRepository libraryRepository;

    public CartController(Views views, CartRepository cartRepository, CatalogRepository catalogRepository,
                          LibraryRepository libraryRepository) {
        this.views = views;
        this.cartRepository = cartRepository;
        this.catalogRepository = catalogRepository;
        this.libraryRepository = libraryRepository;
    }

    public void show(Context ctx) throws IOException, SQLException {
        Cart cart = cartRepository.findByUser(Requests.currentUserId(ctx));
        if (cart.isEmpty()) {
            views.renderMessage(ctx, "Carrinho", "Seu carrinho está vazio.");
            return;
        }

        String items = "";
        for (CartItem item : cart.getItems()) {
            Template row = views.template(ctx, "cart_item");
            row.set("gameId", item.getGameId());
            row.set("title", item.getGameTitle());
            row.set("price", Format.price(item.getUnitPrice()));
            items += row.render();
        }

        Template page = views.template(ctx, "cart");
        page.setHtml("items", items);
        page.set("total", Format.money(cart.total()));
        views.render(ctx, "Carrinho", page.render());
    }

    public void add(Context ctx) throws SQLException {
        long userId = Requests.currentUserId(ctx);
        Game game = catalogRepository.findById(Requests.id(Requests.form(ctx, "gameId")));
        if (!game.isApproved()) {
            throw new NotFoundException("Jogo " + game.getId() + " não encontrado");
        }
        if (libraryRepository.hasGame(userId, game.getId())) {
            Requests.flashError(ctx, "Este jogo já está na sua biblioteca.");
            ctx.redirect("/games/" + game.getId());
            return;
        }

        Cart cart = cartRepository.findByUser(userId);
        try {
            CartItem item = cart.addItem(game);
            cartRepository.addItem(userId, item);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessage());
            ctx.redirect("/games/" + game.getId());
            return;
        }
        Requests.flash(ctx, "Jogo adicionado ao carrinho.");
        ctx.redirect("/cart");
    }

    public void remove(Context ctx) throws SQLException {
        long gameId = Requests.id(Requests.form(ctx, "gameId"));
        cartRepository.removeItem(Requests.currentUserId(ctx), gameId);
        Requests.flash(ctx, "Jogo removido do carrinho.");
        ctx.redirect("/cart");
    }
}
