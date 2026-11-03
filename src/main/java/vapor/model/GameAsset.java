package vapor.model;

public class GameAsset {
    private final long id;
    private final Platform platform;
    private final long fileSize;
    private final String downloadUrl;

    public GameAsset(long id, Platform platform, long fileSize, String downloadUrl) {
        this.id = id;
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

    public long getPlatformId() {
        return platform.getId();
    }

    public long getId() {
        return id;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
