package vapor.model;

import java.util.UUID;

public class GameAsset {
    private final UUID id;
    private final UUID gameId;
    private final Platform platform;
    private final long fileSize;
    private final String downloadUrl;

    public GameAsset(UUID id, UUID gameId, Platform platform, long fileSize, String downloadUrl) {
        this.id = id;
        this.gameId = gameId;
        this.platform = platform;
        this.fileSize = fileSize;
        this.downloadUrl = downloadUrl;
    }

    public long getFileSizeInMegabytes() {
        return fileSize / (1024 * 1024);
    }

    public String getPlatformName() {
        return platform.getName();
    }

    public UUID getPlatformId() {
        return platform.getId();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
