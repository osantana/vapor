package vapor.model;

import java.time.OffsetDateTime;

public class User {
    private long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final OffsetDateTime createdAt;

    public User(long id, String name, String email, String passwordHash, Role role, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public boolean isAdministrator() {
        return role == Role.ADMINISTRATOR;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
