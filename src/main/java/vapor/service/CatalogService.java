package vapor.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vapor.BusinessRuleException;
import vapor.NotFoundException;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.repository.CatalogRepository;

public class CatalogService {
    private static final BigDecimal MAXIMUM_PRICE = new BigDecimal("99999999.99");

    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public static BigDecimal parsePrice(String text) {
        String clean = text.trim().replace(",", ".");
        if (!clean.matches("[0-9]{1,8}(\\.[0-9]{1,2})?")) {
            throw new BusinessRuleException("error.invalidPrice");
        }
        BigDecimal price = new BigDecimal(clean);
        if (price.compareTo(MAXIMUM_PRICE) > 0) {
            throw new BusinessRuleException("error.invalidPrice");
        }
        return price;
    }

    public static boolean isWebAddress(String url) {
        return url.length() <= 500 && (url.startsWith("https://") || url.startsWith("http://"));
    }

    // Returns the message keys of every problem found; an empty list means the game is valid.
    public List<String> validate(Game game) {
        List<String> errors = new ArrayList<>();
        if (game.getTitle().isEmpty() || game.getTitle().length() > 200) {
            errors.add("error.titleRequired");
        }
        if (game.getDescription().isEmpty()) {
            errors.add("error.descriptionRequired");
        }
        if (game.getReleaseDate() == null) {
            errors.add("error.releaseDateRequired");
        }
        if (game.getGenres().isEmpty()) {
            errors.add("error.genreRequired");
        }
        if (!game.getCoverUrl().isEmpty() && !isWebAddress(game.getCoverUrl())) {
            errors.add("error.invalidCoverUrl");
        }
        if (game.getAssets().isEmpty()) {
            errors.add("error.assetRequired");
        }
        for (GameAsset asset : game.getAssets()) {
            if (!isWebAddress(asset.getDownloadUrl()) || asset.getFileSize() <= 0) {
                errors.add("error.invalidAsset");
                break;
            }
        }
        return errors;
    }

    public Game registerGame(Game game) throws SQLException {
        return catalogRepository.insert(game);
    }

    public Game changePrice(long userId, long gameId, BigDecimal price) throws SQLException {
        Game game = catalogRepository.findById(gameId);
        if (!game.isCreatedBy(userId)) {
            throw new NotFoundException("Game " + gameId + " not found");
        }
        game.updatePrice(price);
        catalogRepository.updatePrice(game);
        return game;
    }
}
