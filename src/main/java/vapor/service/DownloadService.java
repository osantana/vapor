package vapor.service;

import java.sql.SQLException;

import vapor.BusinessRuleException;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.repository.CatalogRepository;
import vapor.repository.LibraryRepository;

public class DownloadService {
    private final LibraryRepository libraryRepository;
    private final CatalogRepository catalogRepository;

    public DownloadService(LibraryRepository libraryRepository, CatalogRepository catalogRepository) {
        this.libraryRepository = libraryRepository;
        this.catalogRepository = catalogRepository;
    }

    public String downloadUrl(long userId, long gameId, long assetId) throws SQLException {
        if (!libraryRepository.hasGame(userId, gameId)) {
            throw new BusinessRuleException("Este jogo não está na sua biblioteca.");
        }
        Game game = catalogRepository.findById(gameId);
        GameAsset asset = game.findAsset(assetId);
        return asset.getDownloadUrl();
    }
}
