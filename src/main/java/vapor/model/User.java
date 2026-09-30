package vapor.model;

import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class User {
    private static final int MAXIMUM_FAILED_LOGINS = 5;
    private static final int LOCK_MINUTES = 15;

    private UUID id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private OffsetDateTime createdAt;
    private int failedLogins = 0;
    private OffsetDateTime lockedUntil;

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

    public void restoreLoginState(int failedLogins, OffsetDateTime lockedUntil) {
        this.failedLogins = failedLogins;
        this.lockedUntil = lockedUntil;
    }

    public boolean isLocked(OffsetDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void registerFailedLogin(OffsetDateTime now) {
        failedLogins++;
        if (failedLogins < MAXIMUM_FAILED_LOGINS) {
            return;
        }
        failedLogins = 0;
        lockedUntil = now.plusMinutes(LOCK_MINUTES);
    }

    public void registerSuccessfulLogin() {
        failedLogins = 0;
        lockedUntil = null;
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

    public int getFailedLogins() {
        return failedLogins;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }
}
