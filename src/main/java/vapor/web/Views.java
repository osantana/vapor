package vapor.web;

import java.io.IOException;
import java.sql.SQLException;

import io.javalin.http.Context;

import vapor.repository.CartRepository;

public class Views {
    private final CartRepository cartRepository;

    public Views(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public Template template(Context ctx, String name) throws IOException {
        return Template.load(name);
    }

    public void render(Context ctx, String title, String body) throws IOException, SQLException {
        Template layout = template(ctx, "layout");
        layout.set("title", title);
        layout.setHtml("nav", nav(ctx));
        layout.setHtml("flash", flash(ctx));
        layout.setHtml("body", body);
        ctx.html(layout.render());
    }

    public void renderMessage(Context ctx, String title, String text) throws IOException, SQLException {
        String body = template(ctx, "message").set("text", text).render();
        render(ctx, title, body);
    }

    private String nav(Context ctx) throws IOException, SQLException {
        if (!Requests.isLoggedIn(ctx)) {
            return template(ctx, "nav_guest").render();
        }
        String adminLink = "";
        if (Requests.isAdministrator(ctx)) {
            adminLink = template(ctx, "nav_admin_link").render();
        }
        String userName = ctx.sessionAttribute("userName");
        Template nav = template(ctx, "nav_user");
        nav.set("userName", userName);
        nav.set("cartCount", cartRepository.countItems(Requests.currentUserId(ctx)));
        nav.setHtml("adminLink", adminLink);
        return nav.render();
    }

    // The flash message is shown once, on the next page the user sees.
    private String flash(Context ctx) throws IOException {
        String text = ctx.sessionAttribute("flash");
        if (text == null) {
            return "";
        }
        String kind = ctx.sessionAttribute("flashKind");
        ctx.sessionAttribute("flash", null);
        Template flash = template(ctx, "flash");
        flash.set("kind", kind);
        flash.set("text", text);
        return flash.render();
    }
}
