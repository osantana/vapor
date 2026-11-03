package vapor.web;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.model.User;
import vapor.service.AuthService;

public class AuthController {
    private Views views;
    private AuthService authService;

    public AuthController(Views views, AuthService authService) {
        this.views = views;
        this.authService = authService;
    }

    public void showRegister(Context ctx) throws IOException, SQLException {
        renderRegister(ctx, "", "");
    }

    public void register(Context ctx) throws IOException, SQLException, GeneralSecurityException {
        String name = Requests.form(ctx, "name");
        String email = Requests.form(ctx, "email");
        String password = ctx.formParam("password");
        if (password == null) {
            password = "";
        }
        try {
            authService.register(name, email, password);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessageKey());
            renderRegister(ctx, name, email);
            return;
        }
        Requests.flash(ctx, "register.success");
        ctx.redirect("/login");
    }

    private void renderRegister(Context ctx, String name, String email) throws IOException, SQLException {
        Template page = views.template(ctx, "register");
        page.set("name", name);
        page.set("email", email);
        views.render(ctx, views.messages(ctx).get("register.title"), page.render());
    }

    public void showLogin(Context ctx) throws IOException, SQLException {
        renderLogin(ctx, "");
    }

    public void login(Context ctx) throws IOException, SQLException, GeneralSecurityException {
        String email = Requests.form(ctx, "email");
        String password = ctx.formParam("password");
        if (password == null) {
            password = "";
        }
        User user;
        try {
            user = authService.login(email, password);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessageKey());
            renderLogin(ctx, email);
            return;
        }
        // A new session id after login prevents session fixation.
        ctx.req().changeSessionId();
        ctx.sessionAttribute("userId", user.getId());
        ctx.sessionAttribute("userName", user.getName());
        ctx.sessionAttribute("administrator", user.isAdministrator());
        ctx.redirect("/");
    }

    private void renderLogin(Context ctx, String email) throws IOException, SQLException {
        Template page = views.template(ctx, "login");
        page.set("email", email);
        views.render(ctx, views.messages(ctx).get("login.title"), page.render());
    }

    public void logout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.redirect("/");
    }

    public void changeLanguage(Context ctx) {
        String code = Requests.form(ctx, "code");
        if (code.equals("pt_BR") || code.equals("en")) {
            ctx.sessionAttribute("language", code);
        }
        ctx.redirect("/");
    }
}
