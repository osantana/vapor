package vapor.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.NotFoundException;
import vapor.model.Cart;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.model.GameFilter;
import vapor.model.Genre;
import vapor.model.Platform;
import vapor.repository.CartRepository;
import vapor.repository.CatalogRepository;
import vapor.repository.LibraryRepository;
import vapor.service.CatalogService;

public class CatalogController {
    private static final String[] FILTER_NAMES = {"q", "genre", "platform", "min", "max", "from", "sort"};
    private static final String[] SORT_CODES = {"relevance", "price", "release"};
    private static final String[] SORT_LABELS = {"Relevância", "Preço", "Data de lançamento"};

    private final Views views;
    private final CatalogRepository catalogRepository;
    private final CartRepository cartRepository;
    private final LibraryRepository libraryRepository;

    public CatalogController(Views views, CatalogRepository catalogRepository, CartRepository cartRepository,
                             LibraryRepository libraryRepository) {
        this.views = views;
        this.catalogRepository = catalogRepository;
        this.cartRepository = cartRepository;
        this.libraryRepository = libraryRepository;
    }

    public void list(Context ctx) throws IOException, SQLException {
        GameFilter filter = readFilter(ctx);
        List<Game> games = catalogRepository.search(filter);

        boolean hasNextPage = games.size() > GameFilter.PAGE_SIZE;
        if (hasNextPage) {
            games.remove(games.size() - 1);
        }

        String cards = "";
        for (Game game : games) {
            cards += card(ctx, game);
        }
        if (games.isEmpty()) {
            cards = views.template(ctx, "message").set("text", "Nenhum jogo encontrado.").render();
        }

        Template page = views.template(ctx, "catalog");
        page.set("query", filter.getQuery());
        page.set("min", Requests.query(ctx, "min"));
        page.set("max", Requests.query(ctx, "max"));
        page.set("from", Requests.query(ctx, "from"));
        page.setHtml("genreOptions", genreOptions(ctx, filter));
        page.setHtml("platformOptions", platformOptions(ctx, filter));
        page.setHtml("sortOptions", sortOptions(ctx, filter));
        page.setHtml("cards", cards);
        page.setHtml("pagination", pagination(ctx, filter, hasNextPage));
        views.render(ctx, "Catálogo", page.render());
    }

    private GameFilter readFilter(Context ctx) {
        GameFilter filter = new GameFilter();
        filter.setQuery(Requests.query(ctx, "q"));
        filter.setSort(Requests.query(ctx, "sort"));
        try {
            String genre = Requests.query(ctx, "genre");
            if (!genre.isEmpty()) {
                filter.setGenreId(Long.parseLong(genre));
            }
            String platform = Requests.query(ctx, "platform");
            if (!platform.isEmpty()) {
                filter.setPlatform(Platform.fromCode(platform));
            }
            String minimum = Requests.query(ctx, "min");
            if (!minimum.isEmpty()) {
                filter.setMinimumPrice(CatalogService.parsePrice(minimum));
            }
            String maximum = Requests.query(ctx, "max");
            if (!maximum.isEmpty()) {
                filter.setMaximumPrice(CatalogService.parsePrice(maximum));
            }
            String releasedFrom = Requests.query(ctx, "from");
            if (!releasedFrom.isEmpty()) {
                filter.setReleasedFrom(LocalDate.parse(releasedFrom));
            }
            String page = Requests.query(ctx, "page");
            if (!page.isEmpty()) {
                filter.setPage(Integer.parseInt(page));
            }
        } catch (BusinessRuleException | DateTimeParseException | IllegalArgumentException exception) {
            throw new BadRequestResponse();
        }
        if (filter.getPage() < 1 || filter.getPage() > 10000) {
            throw new BadRequestResponse();
        }
        return filter;
    }

    private String card(Context ctx, Game game) throws IOException {
        Template card = views.template(ctx, "catalog_card");
        card.set("id", game.getId());
        card.set("title", game.getTitle());
        card.set("cover", cover(game));
        card.set("price", Format.price(game.getPrice()));
        return card.render();
    }

    private String cover(Game game) {
        if (game.getCoverUrl().isEmpty()) {
            return "/cover.svg";
        }
        return game.getCoverUrl();
    }

    private String genreOptions(Context ctx, GameFilter filter) throws IOException, SQLException {
        String options = "";
        for (Genre genre : catalogRepository.listGenres()) {
            boolean selected = genre.getId() == filter.getGenreId();
            options += option(ctx, String.valueOf(genre.getId()), genre.getName(), selected);
        }
        return options;
    }

    private String platformOptions(Context ctx, GameFilter filter) throws IOException {
        String options = "";
        for (Platform platform : Platform.values()) {
            boolean selected = platform == filter.getPlatform();
            options += option(ctx, platform.getCode(), platform.getLabel(), selected);
        }
        return options;
    }

    private String sortOptions(Context ctx, GameFilter filter) throws IOException {
        String options = "";
        for (int index = 0; index < SORT_CODES.length; index++) {
            boolean selected = SORT_CODES[index].equals(filter.getSort());
            options += option(ctx, SORT_CODES[index], SORT_LABELS[index], selected);
        }
        return options;
    }

    private String option(Context ctx, String value, String label, boolean selected) throws IOException {
        String selectedAttribute = "";
        if (selected) {
            selectedAttribute = "selected";
        }
        Template option = views.template(ctx, "option");
        option.set("value", value);
        option.set("label", label);
        option.set("selected", selectedAttribute);
        return option.render();
    }

    private String pagination(Context ctx, GameFilter filter, boolean hasNextPage) throws IOException {
        String link = "/?";
        for (String name : FILTER_NAMES) {
            String value = Requests.query(ctx, name);
            if (!value.isEmpty()) {
                link += name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8) + "&";
            }
        }
        String links = "";
        if (filter.getPage() > 1) {
            links += views.template(ctx, "page_previous")
                    .set("link", link + "page=" + (filter.getPage() - 1)).render();
        }
        if (hasNextPage) {
            links += views.template(ctx, "page_next")
                    .set("link", link + "page=" + (filter.getPage() + 1)).render();
        }
        return links;
    }

    public void detail(Context ctx) throws IOException, SQLException {
        Game game = catalogRepository.findById(Requests.id(ctx.pathParam("id")));
        if (!canView(ctx, game)) {
            throw new NotFoundException("Jogo " + game.getId() + " não encontrado");
        }

        String genres = "";
        for (Genre genre : game.getGenres()) {
            genres += views.template(ctx, "tag").set("text", genre.getName()).render();
        }
        String platforms = "";
        for (GameAsset asset : game.getAssets()) {
            platforms += views.template(ctx, "tag").set("text", asset.getPlatform().getLabel()).render();
        }

        Template page = views.template(ctx, "game");
        page.set("title", game.getTitle());
        page.set("cover", cover(game));
        page.set("description", game.getDescription());
        page.set("requirements", game.getSystemRequirements());
        page.set("publisher", game.getCreatorName());
        page.set("releaseDate", Format.date(game.getReleaseDate()));
        page.set("price", Format.price(game.getPrice()));
        page.set("status", game.getStatus().getLabel());
        page.setHtml("genres", genres);
        page.setHtml("platforms", platforms);
        page.setHtml("action", action(ctx, game));
        views.render(ctx, game.getTitle(), page.render());
    }

    private boolean canView(Context ctx, Game game) {
        if (game.isApproved() || Requests.isAdministrator(ctx)) {
            return true;
        }
        return Requests.isLoggedIn(ctx) && game.isCreatedBy(Requests.currentUserId(ctx));
    }

    private String action(Context ctx, Game game) throws IOException, SQLException {
        if (!game.isApproved()) {
            return "";
        }
        if (!Requests.isLoggedIn(ctx)) {
            return views.template(ctx, "game_action_login").render();
        }
        long userId = Requests.currentUserId(ctx);
        if (libraryRepository.hasGame(userId, game.getId())) {
            return views.template(ctx, "game_action_owned").render();
        }
        Cart cart = cartRepository.findByUser(userId);
        if (cart.contains(game.getId())) {
            return views.template(ctx, "game_action_in_cart").render();
        }
        return views.template(ctx, "game_action_add").set("id", game.getId()).render();
    }
}
