package vapor.model;

import java.math.BigDecimal;

// Zero in genreId and null in platform or prices mean "no filter".
public class GameFilter {
    public static final int PAGE_SIZE = 20;

    private String query = "";
    private long genreId = 0;
    private Platform platform;
    private BigDecimal minimumPrice;
    private BigDecimal maximumPrice;
    private int page = 1;

    public int offset() {
        return (page - 1) * PAGE_SIZE;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getGenreId() {
        return genreId;
    }

    public void setGenreId(long genreId) {
        this.genreId = genreId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public BigDecimal getMinimumPrice() {
        return minimumPrice;
    }

    public void setMinimumPrice(BigDecimal minimumPrice) {
        this.minimumPrice = minimumPrice;
    }

    public BigDecimal getMaximumPrice() {
        return maximumPrice;
    }

    public void setMaximumPrice(BigDecimal maximumPrice) {
        this.maximumPrice = maximumPrice;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
