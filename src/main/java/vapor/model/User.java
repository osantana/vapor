package vapor.model;

import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class User {
    private UUID id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private OffsetDateTime createdAt;

    public User(UUID id, String name, String email, String passwordHash, Role role, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public boolean authenticate(String password) throws GeneralSecurityException {
        return PasswordHasher.matches(password, passwordHash);
    }

    public void changePassword(String newPassword) throws GeneralSecurityException {
        passwordHash = PasswordHasher.hash(newPassword);
    }

    public boolean isAdministrator() {
        return role == Role.ADMINISTRATOR;
    }

    public UUID getId() {
        return id;
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
