package vapor.web;

import io.javalin.http.Context;

import vapor.NotFoundException;

public class Requests {
    public static boolean isLoggedIn(Context ctx) {
        return ctx.sessionAttribute("userId") != null;
    }

    public static boolean isAdministrator(Context ctx) {
        Boolean administrator = ctx.sessionAttribute("administrator");
        return administrator != null && administrator;
    }

    public static long currentUserId(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        return userId;
    }

    public static long id(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            throw new NotFoundException("Invalid identifier");
        }
    }

    public static String form(Context ctx, String name) {
        String value = ctx.formParam(name);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static String query(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    // The flash message is shown once, on the next page the user sees.
    public static void flash(Context ctx, String text) {
        ctx.sessionAttribute("flash", text);
        ctx.sessionAttribute("flashKind", "success");
    }

    public static void flashError(Context ctx, String text) {
        ctx.sessionAttribute("flash", text);
        ctx.sessionAttribute("flashKind", "error");
    }
}
