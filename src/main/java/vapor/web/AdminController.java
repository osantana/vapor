package vapor.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import vapor.model.Game;
import vapor.repository.CatalogRepository;

public class AdminController {
    private Views views;
    private CatalogRepository catalogRepository;

    public AdminController(Views views, CatalogRepository catalogRepository) {
        this.views = views;
        this.catalogRepository = catalogRepository;
    }

    public void queue(Context ctx) throws IOException, SQLException {
        Messages messages = views.messages(ctx);
        List<Game> pending = catalogRepository.listPending();
        if (pending.isEmpty()) {
            String body = views.template(ctx, "message").set("text", messages.get("admin.empty")).render();
            views.render(ctx, messages.get("admin.title"), body);
            return;
        }

        String games = "";
        for (Game game : pending) {
            Template row = views.template(ctx, "admin_game");
            row.set("id", game.getId());
            row.set("title", game.getTitle());
            row.set("publisher", game.getCreatorName());
            row.set("description", game.getDescription());
            row.set("price", Format.price(game.getPrice(), messages));
            row.set("notes", game.getReviewNotes());
            games += row.render();
        }
        Template page = views.template(ctx, "admin");
        page.setHtml("games", games);
        views.render(ctx, messages.get("admin.title"), page.render());
    }

    public void review(Context ctx) throws SQLException {
        Game game = catalogRepository.findById(Requests.uuid(Requests.form(ctx, "gameId")));
        String decision = Requests.form(ctx, "decision");
        String notes = Requests.form(ctx, "notes");

        if (!decision.equals("approve") && notes.isEmpty()) {
            Requests.flashError(ctx, "error.notesRequired");
            ctx.redirect("/admin");
            return;
        }
        if (decision.equals("approve")) {
            game.approve();
        }
        if (decision.equals("reject")) {
            game.reject(notes);
        }
        if (decision.equals("changes")) {
            game.requestChanges(notes);
        }
        if (!decision.equals("approve") && !decision.equals("reject") && !decision.equals("changes")) {
            throw new BadRequestResponse();
        }
        catalogRepository.updateReview(game);
        Requests.flash(ctx, "admin.reviewed");
        ctx.redirect("/admin");
    }
}
