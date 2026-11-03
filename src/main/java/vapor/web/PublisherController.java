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
import vapor.repository.OrderRepository;
import vapor.service.CatalogService;

public class PublisherController {
    private static final long BYTES_PER_MEGABYTE = 1024 * 1024;

    private final Views views;
    private final CatalogRepository catalogRepository;
    private final OrderRepository orderRepository;
    private final CatalogService catalogService;

    public PublisherController(Views views, CatalogRepository catalogRepository, OrderRepository orderRepository,
                               CatalogService catalogService) {
        this.views = views;
        this.catalogRepository = catalogRepository;
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
    }

    public void dashboard(Context ctx) throws IOException, SQLException {
        long userId = Requests.currentUserId(ctx);

        StringBuilder games = new StringBuilder();
        for (Game game : catalogRepository.findByCreator(userId)) {
            Template row = views.template(ctx, "publisher_game");
            row.set("id", game.getId());
            row.set("title", game.getTitle());
            row.set("status", game.getStatus().getLabel());
            row.set("notes", game.getReviewNotes());
            row.set("price", game.getPrice());
            games.append(row.render());
        }

        String sales = "";
        for (SalesMetrics metrics : orderRepository.salesByCreator(userId)) {
            Template row = views.template(ctx, "publisher_sale");
            row.set("title", metrics.getGameTitle());
            row.set("units", metrics.getUnitsSold());
            row.set("gross", Format.money(metrics.getGrossRevenue()));
            row.set("net", Format.money(metrics.getNetRevenue()));
            sales += row.render();
        }

        Template page = views.template(ctx, "publisher");
        page.setHtml("games", games.toString());
        page.setHtml("sales", sales);
        views.render(ctx, "Painel do publicador", page.render());
    }

    public void changePrice(Context ctx) throws SQLException {
        long gameId = Requests.id(Requests.form(ctx, "gameId"));
        try {
            BigDecimal price = CatalogService.parsePrice(Requests.form(ctx, "price"));
            catalogService.changePrice(Requests.currentUserId(ctx), gameId, price);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessage());
            ctx.redirect("/publisher");
            return;
        }
        Requests.flash(ctx, "Preço alterado.");
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
        Requests.flash(ctx, "Jogo enviado para análise.");
        ctx.redirect("/publisher");
    }

    private Game readGame(Context ctx, List<String> errors) throws SQLException {
        BigDecimal price = BigDecimal.ZERO;
        try {
            price = CatalogService.parsePrice(Requests.form(ctx, "price"));
        } catch (BusinessRuleException exception) {
            errors.add(exception.getMessage());
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
        for (Platform platform : Platform.values()) {
            String url = Requests.form(ctx, "url_" + platform.getCode());
            if (!url.isEmpty()) {
                long size = readSize(Requests.form(ctx, "size_" + platform.getCode()));
                game.addAsset(new GameAsset(0, platform, size, url));
            }
        }
        return game;
    }

    // An invalid size becomes zero, which the validation rejects.
    private long readSize(String megabytes) {
        try {
            return Integer.parseInt(megabytes) * BYTES_PER_MEGABYTE;
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void renderGameForm(Context ctx, List<String> errors) throws IOException, SQLException {
        String errorItems = "";
        for (String error : errors) {
            errorItems += views.template(ctx, "flash")
                    .set("kind", "error")
                    .set("text", error)
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
        for (Platform platform : Platform.values()) {
            Template fields = views.template(ctx, "game_form_platform");
            fields.set("id", platform.getCode());
            fields.set("name", platform.getLabel());
            fields.set("url", Requests.form(ctx, "url_" + platform.getCode()));
            fields.set("size", Requests.form(ctx, "size_" + platform.getCode()));
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
        views.render(ctx, "Publicar jogo", page.render());
    }
}
