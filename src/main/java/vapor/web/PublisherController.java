package vapor.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.model.GameStatus;
import vapor.model.Genre;
import vapor.model.Platform;
import vapor.model.SalesMetrics;
import vapor.repository.CatalogRepository;
import vapor.service.CatalogService;
import vapor.service.SalesDashboardService;

public class PublisherController {
    private static final long BYTES_PER_MEGABYTE = 1024 * 1024;

    private Views views;
    private CatalogRepository catalogRepository;
    private CatalogService catalogService;
    private SalesDashboardService salesDashboardService;

    public PublisherController(Views views, CatalogRepository catalogRepository, CatalogService catalogService,
                               SalesDashboardService salesDashboardService) {
        this.views = views;
        this.catalogRepository = catalogRepository;
        this.catalogService = catalogService;
        this.salesDashboardService = salesDashboardService;
    }

    public void dashboard(Context ctx) throws IOException, SQLException {
        Messages messages = views.messages(ctx);
        long userId = Requests.currentUserId(ctx);

        String games = "";
        for (Game game : catalogRepository.findByCreator(userId)) {
            Template row = views.template(ctx, "publisher_game");
            row.set("id", game.getId());
            row.set("title", game.getTitle());
            row.set("status", messages.get("status." + game.getStatus().name().toLowerCase()));
            row.set("notes", game.getReviewNotes());
            row.set("price", game.getPrice());
            games += row.render();
        }

        String sales = "";
        for (SalesMetrics metrics : salesDashboardService.getMetrics(userId)) {
            Template row = views.template(ctx, "publisher_sale");
            row.set("title", metrics.getGameTitle());
            row.set("units", metrics.getUnitsSold());
            row.set("gross", Format.money(metrics.getGrossRevenue()));
            row.set("net", Format.money(metrics.getNetRevenue()));
            sales += row.render();
        }

        Template page = views.template(ctx, "publisher");
        page.setHtml("games", games);
        page.setHtml("sales", sales);
        views.render(ctx, messages.get("publisher.title"), page.render());
    }

    public void salesCsv(Context ctx) throws SQLException {
        ctx.contentType("text/csv; charset=utf-8");
        ctx.header("Content-Disposition", "attachment; filename=\"sales.csv\"");
        ctx.result(salesDashboardService.exportCsv(Requests.currentUserId(ctx)));
    }

    public void changePrice(Context ctx) throws SQLException {
        long gameId = Requests.id(Requests.form(ctx, "gameId"));
        try {
            BigDecimal price = CatalogService.parsePrice(Requests.form(ctx, "price"));
            catalogService.changePrice(Requests.currentUserId(ctx), gameId, price);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessageKey());
            ctx.redirect("/publisher");
            return;
        }
        Requests.flash(ctx, "publisher.priceChanged");
        ctx.redirect("/publisher");
    }

    public void showGameForm(Context ctx) throws IOException, SQLException {
        renderGameForm(ctx, new ArrayList<>());
    }

    public void createGame(Context ctx) throws IOException, SQLException {
        List<String> errors = new ArrayList<>();
        Game game = readGame(ctx, errors);
        errors.addAll(catalogService.validate(game));
        if (!errors.isEmpty()) {
            renderGameForm(ctx, errors);
            return;
        }
        catalogService.registerGame(game);
        Requests.flash(ctx, "publisher.gameSubmitted");
        ctx.redirect("/publisher");
    }

    private Game readGame(Context ctx, List<String> errors) throws SQLException {
        BigDecimal price = BigDecimal.ZERO;
        try {
            price = CatalogService.parsePrice(Requests.form(ctx, "price"));
        } catch (BusinessRuleException exception) {
            errors.add(exception.getMessageKey());
        }
        LocalDate releaseDate = null;
        try {
            releaseDate = LocalDate.parse(Requests.form(ctx, "releaseDate"));
        } catch (DateTimeParseException exception) {
            // the validation reports the missing release date
        }

        Game game = new Game(0, Requests.currentUserId(ctx), Requests.form(ctx, "title"),
                Requests.form(ctx, "description"), price, releaseDate, GameStatus.PENDING,
                OffsetDateTime.now());
        game.setSystemRequirements(Requests.form(ctx, "requirements"));
        game.setCoverUrl(Requests.form(ctx, "coverUrl"));

        List<String> chosenGenres = ctx.formParams("genre");
        for (Genre genre : catalogRepository.listGenres()) {
            if (chosenGenres.contains(String.valueOf(genre.getId()))) {
                game.addGenre(genre);
            }
        }
        for (Platform platform : catalogRepository.listPlatforms()) {
            String url = Requests.form(ctx, "url_" + platform.getId());
            if (!url.isEmpty()) {
                long size = readSize(Requests.form(ctx, "size_" + platform.getId()));
                game.addAsset(new GameAsset(0, platform, size, url));
            }
        }
        return game;
    }

    // An invalid size becomes zero, which the validation rejects.
    private long readSize(String megabytes) {
        if (!megabytes.matches("[0-9]{1,7}")) {
            return 0;
        }
        return Long.parseLong(megabytes) * BYTES_PER_MEGABYTE;
    }

    private void renderGameForm(Context ctx, List<String> errors) throws IOException, SQLException {
        Messages messages = views.messages(ctx);

        String errorItems = "";
        for (String error : errors) {
            errorItems += views.template(ctx, "flash")
                    .set("kind", "error")
                    .set("text", messages.get(error))
                    .render();
        }

        List<String> chosenGenres = ctx.formParams("genre");
        String genres = "";
        for (Genre genre : catalogRepository.listGenres()) {
            String checked = "";
            if (chosenGenres.contains(String.valueOf(genre.getId()))) {
                checked = "checked";
            }
            Template checkbox = views.template(ctx, "game_form_genre");
            checkbox.set("id", genre.getId());
            checkbox.set("name", genre.getName());
            checkbox.set("checked", checked);
            genres += checkbox.render();
        }

        String platforms = "";
        for (Platform platform : catalogRepository.listPlatforms()) {
            Template fields = views.template(ctx, "game_form_platform");
            fields.set("id", platform.getId());
            fields.set("name", platform.getName());
            fields.set("url", Requests.form(ctx, "url_" + platform.getId()));
            fields.set("size", Requests.form(ctx, "size_" + platform.getId()));
            platforms += fields.render();
        }

        Template page = views.template(ctx, "game_form");
        page.setHtml("errors", errorItems);
        page.set("title", Requests.form(ctx, "title"));
        page.set("description", Requests.form(ctx, "description"));
        page.set("requirements", Requests.form(ctx, "requirements"));
        page.set("coverUrl", Requests.form(ctx, "coverUrl"));
        page.set("price", Requests.form(ctx, "price"));
        page.set("releaseDate", Requests.form(ctx, "releaseDate"));
        page.setHtml("genres", genres);
        page.setHtml("platforms", platforms);
        views.render(ctx, messages.get("gameForm.title"), page.render());
    }
}
