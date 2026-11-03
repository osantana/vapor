package vapor.web;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import vapor.BusinessRuleException;
import vapor.NotFoundException;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.model.LibraryEntry;
import vapor.repository.CatalogRepository;
import vapor.repository.LibraryRepository;
import vapor.service.DownloadService;

public class LibraryController {
    private Views views;
    private LibraryRepository libraryRepository;
    private CatalogRepository catalogRepository;
    private DownloadService downloadService;

    public LibraryController(Views views, LibraryRepository libraryRepository,
                             CatalogRepository catalogRepository, DownloadService downloadService) {
        this.views = views;
        this.libraryRepository = libraryRepository;
        this.catalogRepository = catalogRepository;
        this.downloadService = downloadService;
    }

    public void list(Context ctx) throws IOException, SQLException {
        Messages messages = views.messages(ctx);
        String query = Requests.query(ctx, "q");
        List<LibraryEntry> entries = libraryRepository.findByUser(Requests.currentUserId(ctx));

        String games = "";
        for (LibraryEntry entry : entries) {
            // ponytail: one query per game; join in SQL if libraries get big
            Game game = catalogRepository.findById(entry.getGameId());
            if (game.getTitle().toLowerCase().contains(query.toLowerCase())) {
                games += renderGame(ctx, game, entry);
            }
        }
        if (games.isEmpty()) {
            games = views.template(ctx, "message").set("text", messages.get("library.empty")).render();
        }

        Template page = views.template(ctx, "library");
        page.set("query", query);
        page.setHtml("games", games);
        views.render(ctx, messages.get("library.title"), page.render());
    }

    private String renderGame(Context ctx, Game game, LibraryEntry entry) throws IOException {
        String downloads = "";
        for (GameAsset asset : game.getAssets()) {
            Template download = views.template(ctx, "library_download");
            download.set("gameId", game.getId());
            download.set("assetId", asset.getId());
            download.set("platform", asset.getPlatform().getLabel());
            download.set("size", asset.getFileSizeInMegabytes());
            downloads += download.render();
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
        template.setHtml("downloads", downloads);
        return template.render();
    }

    public void createDownloadLink(Context ctx) throws SQLException, GeneralSecurityException {
        long gameId = Requests.id(Requests.form(ctx, "gameId"));
        long assetId = Requests.id(Requests.form(ctx, "assetId"));
        try {
            String link = downloadService.generateDownloadUrl(Requests.currentUserId(ctx), gameId, assetId,
                    Instant.now().getEpochSecond());
            ctx.redirect(link);
        } catch (BusinessRuleException exception) {
            Requests.flashError(ctx, exception.getMessageKey());
            ctx.redirect("/library");
        }
    }

    public void download(Context ctx) throws SQLException, GeneralSecurityException {
        long gameId = Requests.id(ctx.pathParam("gameId"));
        long assetId = Requests.id(ctx.pathParam("assetId"));
        long expires;
        try {
            expires = Long.parseLong(Requests.query(ctx, "expires"));
        } catch (NumberFormatException exception) {
            throw new BadRequestResponse();
        }
        try {
            String fileUrl = downloadService.resolveDownload(gameId, assetId, expires,
                    Requests.query(ctx, "signature"), Instant.now().getEpochSecond());
            ctx.redirect(fileUrl);
        } catch (BusinessRuleException exception) {
            throw new NotFoundException("Download link expired or invalid");
        }
    }
}
