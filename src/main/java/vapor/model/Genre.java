package vapor.model;

import java.util.UUID;

public class Genre {
    private UUID id;
    private String name;

    public Genre(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
