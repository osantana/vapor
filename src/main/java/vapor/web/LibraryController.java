package vapor.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.model.LibraryEntry;
import vapor.repository.CatalogRepository;
import vapor.repository.LibraryRepository;
import vapor.service.DownloadService;

public class LibraryController {
    private final Views views;
    private final LibraryRepository libraryRepository;
    private final CatalogRepository catalogRepository;
    private final DownloadService downloadService;

    public LibraryController(Views views, LibraryRepository libraryRepository,
                             CatalogRepository catalogRepository, DownloadService downloadService) {
        this.views = views;
        this.libraryRepository = libraryRepository;
        this.catalogRepository = catalogRepository;
        this.downloadService = downloadService;
    }

    public void list(Context ctx) throws IOException, SQLException {
        String query = Requests.query(ctx, "q");
        List<LibraryEntry> entries = libraryRepository.findByUser(Requests.currentUserId(ctx));

        StringBuilder games = new StringBuilder();
        for (LibraryEntry entry : entries) {
            Game game = catalogRepository.findById(entry.getGameId());
            if (game.getTitle().toLowerCase().contains(query.toLowerCase())) {
                games.append(renderGame(ctx, game, entry));
            }
        }
        if (games.isEmpty()) {
            games.append(views.template(ctx, "message").set("text", "Nenhum jogo na sua biblioteca.").render());
        }

        Template page = views.template(ctx, "library");
        page.set("query", query);
        page.setHtml("games", games.toString());
        views.render(ctx, "Biblioteca", page.render());
    }

    private String renderGame(Context ctx, Game game, LibraryEntry entry) throws IOException {
        StringBuilder downloads = new StringBuilder();
        for (GameAsset asset : game.getAssets()) {
            Template download = views.template(ctx, "library_download");
            download.set("gameId", game.getId());
            download.set("assetId", asset.getId());
            download.set("platform", asset.getPlatform().getLabel());
            download.set("size", asset.getFileSizeInMegabytes());
            downloads.append(download.render());
        }
        String cover = game.getCoverUrl();
        if (cover.isEmpty()) {
            cover = "/cover.svg";
        }
        Template template = views.template(ctx, "library_game");
        template.set("id", game.getId());
        template.set("title", game.getTitle());
        template.set("cover", cover);
        template.set("acquiredAt", Format.dateTime(entry.getAcquiredAt()));
        template.setHtml("downloads", downloads.toString());
        return template.render();
    }

    public void download(Context ctx) throws SQLException {
        long gameId = Requests.id(Requests.form(ctx, "gameId"));
        long assetId = Requests.id(Requests.form(ctx, "assetId"));
        try {
            ctx.redirect(downloadService.downloadUrl(Requests.currentUserId(ctx), gameId, assetId));
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessage());
            ctx.redirect("/library");
        }
    }
}
