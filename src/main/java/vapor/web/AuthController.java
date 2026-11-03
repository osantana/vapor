package vapor.web;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.model.User;
import vapor.service.AuthService;

public class AuthController {
    private final Views views;
    private final AuthService authService;

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
            Requests.flashError(ctx, exception.getMessage());
            renderRegister(ctx, name, email);
            return;
        }
        Requests.flash(ctx, "Conta criada. Entre para continuar.");
        ctx.redirect("/login");
    }

    private void renderRegister(Context ctx, String name, String email) throws IOException, SQLException {
        Template page = views.template(ctx, "register");
        page.set("name", name);
        page.set("email", email);
        views.render(ctx, "Cadastro", page.render());
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
            Requests.flashError(ctx, exception.getMessage());
            renderLogin(ctx, email);
            return;
        }
        ctx.sessionAttribute("userId", user.getId());
        ctx.sessionAttribute("userName", user.getName());
        ctx.sessionAttribute("administrator", user.isAdministrator());
        ctx.redirect("/");
    }

    private void renderLogin(Context ctx, String email) throws IOException, SQLException {
        Template page = views.template(ctx, "login");
        page.set("email", email);
        views.render(ctx, "Entrar", page.render());
    }

    public void logout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.redirect("/");
    }
}
