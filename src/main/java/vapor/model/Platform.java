package vapor.model;

public class Platform {
    private final long id;
    private final String name;

    public Platform(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
