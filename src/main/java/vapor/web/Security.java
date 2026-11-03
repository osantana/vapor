package vapor.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.NotFoundResponse;

// Runs before every request: CSRF protection, login checks and security headers.
public class Security implements Handler {
    private static final String[] LOGIN_REQUIRED = {
        "/cart", "/checkout", "/orders", "/library", "/publisher", "/admin"
    };

    @Override
    public void handle(Context ctx) {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "same-origin");
        ctx.header("Content-Security-Policy",
                "default-src 'self'; img-src 'self' https: http:; frame-ancestors 'none'");

        checkCsrfToken(ctx);

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

    private void checkCsrfToken(Context ctx) {
        String expected = ctx.sessionAttribute("csrf");
        if (expected == null) {
            expected = UUID.randomUUID().toString();
            ctx.sessionAttribute("csrf", expected);
        }
        if (ctx.method() != HandlerType.POST) {
            return;
        }
        String received = Requests.form(ctx, "csrf");
        boolean matches = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new ForbiddenResponse();
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
