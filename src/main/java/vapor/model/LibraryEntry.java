package vapor.model;

import java.time.OffsetDateTime;

public class LibraryEntry {
    private final long gameId;
    private final OffsetDateTime acquiredAt;

    public LibraryEntry(long gameId, OffsetDateTime acquiredAt) {
        this.gameId = gameId;
        this.acquiredAt = acquiredAt;
    }

    public long getGameId() {
        return gameId;
    }

    public OffsetDateTime getAcquiredAt() {
        return acquiredAt;
    }
}
