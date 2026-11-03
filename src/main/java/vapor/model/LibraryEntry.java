package vapor.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class LibraryEntry {
    private final UUID id;
    private final UUID userId;
    private final UUID gameId;
    private final OffsetDateTime acquiredAt;
    private final int minutesPlayed;

    public LibraryEntry(UUID id, UUID userId, UUID gameId, OffsetDateTime acquiredAt, int minutesPlayed) {
        this.id = id;
        this.userId = userId;
        this.gameId = gameId;
        this.acquiredAt = acquiredAt;
        this.minutesPlayed = minutesPlayed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getGameId() {
        return gameId;
    }

    public OffsetDateTime getAcquiredAt() {
        return acquiredAt;
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }
}
