package vapor.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import vapor.BusinessRuleException;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.repository.CatalogRepository;
import vapor.repository.LibraryRepository;

public class DownloadService {
    private static final long LINK_DURATION_IN_SECONDS = 3600;

    private String secretKey;
    private LibraryRepository libraryRepository;
    private CatalogRepository catalogRepository;

    public DownloadService(String secretKey, LibraryRepository libraryRepository,
                           CatalogRepository catalogRepository) {
        this.secretKey = secretKey;
        this.libraryRepository = libraryRepository;
        this.catalogRepository = catalogRepository;
    }

    public String generateDownloadUrl(UUID userId, UUID gameId, UUID assetId, long nowInSeconds)
            throws SQLException, GeneralSecurityException {
        if (!libraryRepository.hasGame(userId, gameId)) {
            throw new BusinessRuleException("error.notInLibrary");
        }
        Game game = catalogRepository.findById(gameId);
        GameAsset asset = game.findAsset(assetId);
        long expires = nowInSeconds + LINK_DURATION_IN_SECONDS;
        return "/download/" + gameId + "/" + asset.getId() + "?expires=" + expires
                + "&signature=" + sign(asset.getId(), expires);
    }

    public String resolveDownload(UUID gameId, UUID assetId, long expires, String signature, long nowInSeconds)
            throws SQLException, GeneralSecurityException {
        if (!isValidSignature(assetId, expires, signature, nowInSeconds)) {
            throw new BusinessRuleException("error.downloadLinkInvalid");
        }
        Game game = catalogRepository.findById(gameId);
        GameAsset asset = game.findAsset(assetId);
        return asset.getDownloadUrl();
    }

    public String sign(UUID assetId, long expires) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String content = assetId + ":" + expires;
        byte[] signature = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(signature);
    }

    public boolean isValidSignature(UUID assetId, long expires, String signature, long nowInSeconds)
            throws GeneralSecurityException {
        if (nowInSeconds > expires) {
            return false;
        }
        byte[] expected = sign(assetId, expires).getBytes(StandardCharsets.UTF_8);
        byte[] received = signature.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, received);
    }
}
