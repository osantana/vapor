package vapor.web;

import java.util.UUID;

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

    public static UUID currentUserId(Context ctx) {
        String userId = ctx.sessionAttribute("userId");
        return UUID.fromString(userId);
    }

    public static UUID uuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException exception) {
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

    public static void flash(Context ctx, String messageKey) {
        flash(ctx, messageKey, "");
    }

    public static void flash(Context ctx, String messageKey, String argument) {
        ctx.sessionAttribute("flash", messageKey);
        ctx.sessionAttribute("flashArgument", argument);
        ctx.sessionAttribute("flashError", false);
    }

    public static void flashError(Context ctx, String messageKey) {
        ctx.sessionAttribute("flash", messageKey);
        ctx.sessionAttribute("flashArgument", "");
        ctx.sessionAttribute("flashError", true);
    }
}
