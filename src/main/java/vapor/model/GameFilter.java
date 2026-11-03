package vapor.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Zero in genreId or platformId means "no filter".
public class GameFilter {
    public static final int PAGE_SIZE = 20;

    private String query = "";
    private long genreId = 0;
    private long platformId = 0;
    private BigDecimal minimumPrice;
    private BigDecimal maximumPrice;
    private LocalDate releasedFrom;
    private String sort = "relevance";
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

    public long getPlatformId() {
        return platformId;
    }

    public void setPlatformId(long platformId) {
        this.platformId = platformId;
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

    public LocalDate getReleasedFrom() {
        return releasedFrom;
    }

    public void setReleasedFrom(LocalDate releasedFrom) {
        this.releasedFrom = releasedFrom;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
