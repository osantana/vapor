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
        BigDecimal price;
        try {
            price = new BigDecimal(text.trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("Informe um preço válido, como 59,90.");
        }
        boolean negative = price.signum() < 0;
        boolean tooManyDecimals = price.scale() > 2;
        boolean tooBig = price.compareTo(MAXIMUM_PRICE) > 0;
        if (negative || tooManyDecimals || tooBig) {
            throw new BusinessRuleException("Informe um preço válido, como 59,90.");
        }
        return price.setScale(2);
    }

    public static boolean isWebAddress(String url) {
        return url.length() <= 500 && (url.startsWith("https://") || url.startsWith("http://"));
    }

    // Returns one message for every problem found; an empty list means the game is valid.
    public List<String> validate(Game game) {
        List<String> errors = new ArrayList<>();
        if (game.getTitle().isEmpty() || game.getTitle().length() > 200) {
            errors.add("Informe um título com até 200 caracteres.");
        }
        if (game.getDescription().isEmpty()) {
            errors.add("Informe uma descrição.");
        }
        if (game.getReleaseDate() == null) {
            errors.add("Informe a data de lançamento.");
        }
        if (game.getGenres().isEmpty()) {
            errors.add("Escolha pelo menos um gênero.");
        }
        if (!game.getCoverUrl().isEmpty() && !isWebAddress(game.getCoverUrl())) {
            errors.add("O endereço da capa deve começar com http:// ou https://.");
        }
        if (game.getAssets().isEmpty()) {
            errors.add("Adicione o arquivo de instalação de pelo menos uma plataforma.");
        }
        for (GameAsset asset : game.getAssets()) {
            if (!isWebAddress(asset.getDownloadUrl()) || asset.getFileSize() <= 0) {
                errors.add("Todo arquivo de instalação precisa de endereço válido e tamanho em MB.");
                break;
            }
        }
        return errors;
    }

    public void registerGame(Game game) throws SQLException {
        catalogRepository.insert(game);
    }

    public void changePrice(long userId, long gameId, BigDecimal price) throws SQLException {
        Game game = catalogRepository.findById(gameId);
        if (!game.isCreatedBy(userId)) {
            throw new NotFoundException("Jogo " + gameId + " não encontrado");
        }
        game.updatePrice(price);
        catalogRepository.updatePrice(game);
    }
}
