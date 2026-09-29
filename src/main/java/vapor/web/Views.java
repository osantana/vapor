package vapor.web;

import java.io.IOException;
import java.sql.SQLException;

import io.javalin.http.Context;

import vapor.repository.CartRepository;

public class Views {
    private CartRepository cartRepository;

    public Views(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public Messages messages(Context ctx) {
        return Messages.forLanguage(language(ctx));
    }

    public Template template(Context ctx, String name) throws IOException {
        Template template = Template.load(name, messages(ctx));
        String csrf = ctx.sessionAttribute("csrf");
        template.set("csrf", csrf);
        return template;
    }

    public void render(Context ctx, String title, String body) throws IOException, SQLException {
        Template layout = template(ctx, "layout");
        layout.set("language", language(ctx).replace("_", "-"));
        layout.set("title", title);
        layout.setHtml("nav", nav(ctx));
        layout.setHtml("flash", flash(ctx));
        layout.setHtml("body", body);
        ctx.html(layout.render());
    }

    public void renderMessage(Context ctx, String messageKey) throws IOException, SQLException {
        String text = messages(ctx).get(messageKey);
        String body = template(ctx, "message").set("text", text).render();
        render(ctx, text, body);
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

    private String flash(Context ctx) throws IOException {
        String flashKey = ctx.sessionAttribute("flash");
        if (flashKey == null) {
            return "";
        }
        String argument = ctx.sessionAttribute("flashArgument");
        Boolean error = ctx.sessionAttribute("flashError");
        String kind = "success";
        if (error) {
            kind = "error";
        }
        ctx.sessionAttribute("flash", null);
        Template flash = template(ctx, "flash");
        flash.set("kind", kind);
        flash.set("text", messages(ctx).get(flashKey, argument));
        return flash.render();
    }

    private String language(Context ctx) {
        String chosen = ctx.sessionAttribute("language");
        if (chosen != null) {
            return chosen;
        }
        String accepted = ctx.header("Accept-Language");
        if (accepted != null && accepted.toLowerCase().startsWith("pt")) {
            return "pt_BR";
        }
        return "en";
    }
}
