package vapor.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import vapor.NotFoundException;

public class Game {
    private long id;
    private final long creatorId;
    private String creatorName = "";
    private final String title;
    private final String description;
    private String systemRequirements = "";
    private String coverUrl = "";
    private BigDecimal price;
    private final LocalDate releaseDate;
    private GameStatus status;
    private String reviewNotes = "";
    private final OffsetDateTime createdAt;
    private final List<Genre> genres = new ArrayList<>();
    private final List<GameAsset> assets = new ArrayList<>();

    public Game(long id, long creatorId, String title, String description, BigDecimal price,
                LocalDate releaseDate, GameStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.releaseDate = releaseDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void approve() {
        status = GameStatus.APPROVED;
        reviewNotes = "";
    }

    public void reject(String reason) {
        status = GameStatus.REJECTED;
        reviewNotes = reason;
    }

    public void requestChanges(String notes) {
        status = GameStatus.PENDING;
        reviewNotes = notes;
    }

    public void updatePrice(BigDecimal newPrice) {
        price = newPrice;
    }

    public boolean isApproved() {
        return status == GameStatus.APPROVED;
    }

    public boolean isFree() {
        return price.signum() == 0;
    }

    public boolean isCreatedBy(long userId) {
        return creatorId == userId;
    }

    public void addGenre(Genre genre) {
        genres.add(genre);
    }

    public void addAsset(GameAsset asset) {
        assets.add(asset);
    }

    public GameAsset findAsset(long assetId) {
        for (GameAsset asset : assets) {
            if (asset.getId() == assetId) {
                return asset;
            }
        }
        throw new NotFoundException("Asset " + assetId + " not found");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreatorId() {
        return creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSystemRequirements() {
        return systemRequirements;
    }

    public void setSystemRequirements(String systemRequirements) {
        this.systemRequirements = systemRequirements;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public List<GameAsset> getAssets() {
        return assets;
    }
}
