package vapor.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

// Runs before every request: sends anonymous users to the login page and hides admin pages.
public class Security implements Handler {
    private static final String[] LOGIN_REQUIRED = {
        "/cart", "/checkout", "/orders", "/library", "/publisher", "/admin"
    };

    @Override
    public void handle(Context ctx) {
        String path = ctx.path();
        if (requiresLogin(path) && !Requests.isLoggedIn(ctx)) {
            ctx.redirect("/login");
            ctx.skipRemainingHandlers();
            return;
        }
        if (path.startsWith("/admin") && !Requests.isAdministrator(ctx)) {
            throw new NotFoundResponse();
        }
    }

    private boolean requiresLogin(String path) {
        for (String prefix : LOGIN_REQUIRED) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}
