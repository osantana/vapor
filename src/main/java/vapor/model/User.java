package vapor.model;

import java.time.OffsetDateTime;

public class User {
    private static final int MAXIMUM_FAILED_LOGINS = 5;
    private static final int LOCK_MINUTES = 15;

    private long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final OffsetDateTime createdAt;
    private int failedLogins = 0;
    private OffsetDateTime lockedUntil;

    public User(long id, String name, String email, String passwordHash, Role role, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
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

    public int getFailedLogins() {
        return failedLogins;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }
}
