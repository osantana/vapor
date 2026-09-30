package vapor.service;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import vapor.BusinessRuleException;
import vapor.NotFoundException;
import vapor.model.PasswordHasher;
import vapor.model.Role;
import vapor.model.User;
import vapor.repository.UserRepository;

public class AuthService {
    private static final int MAXIMUM_PASSWORD_LENGTH = 128;

    private UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static boolean isValidEmail(String email) {
        return email.length() <= 254 && email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    public static boolean isStrongPassword(String password) {
        if (password.length() < 8 || password.length() > MAXIMUM_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int index = 0; index < password.length(); index++) {
            char character = password.charAt(index);
            if (Character.isLetter(character)) {
                hasLetter = true;
            }
            if (Character.isDigit(character)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    public User register(String name, String email, String password)
            throws SQLException, GeneralSecurityException {
        return createUser(name, email, password, Role.USER);
    }

    public void ensureAdministrator(String email, String password)
            throws SQLException, GeneralSecurityException {
        if (userRepository.emailExists(normalize(email))) {
            return;
        }
        createUser("Administrator", email, password, Role.ADMINISTRATOR);
    }

    private User createUser(String name, String email, String password, Role role)
            throws SQLException, GeneralSecurityException {
        String cleanName = name.trim();
        String cleanEmail = normalize(email);
        if (cleanName.isEmpty() || cleanName.length() > 120) {
            throw new BusinessRuleException("error.invalidName");
        }
        if (!isValidEmail(cleanEmail)) {
            throw new BusinessRuleException("error.invalidEmail");
        }
        if (!isStrongPassword(password)) {
            throw new BusinessRuleException("error.weakPassword");
        }
        if (userRepository.emailExists(cleanEmail)) {
            throw new BusinessRuleException("error.emailTaken");
        }
        User user = new User(UUID.randomUUID(), cleanName, cleanEmail, PasswordHasher.hash(password), role,
                OffsetDateTime.now());
        return userRepository.save(user);
    }

    public User login(String email, String password) throws SQLException, GeneralSecurityException {
        if (password.length() > MAXIMUM_PASSWORD_LENGTH) {
            throw new BusinessRuleException("error.invalidCredentials");
        }
        User user;
        try {
            user = userRepository.findByEmail(normalize(email));
        } catch (NotFoundException exception) {
            throw new BusinessRuleException("error.invalidCredentials");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (user.isLocked(now)) {
            throw new BusinessRuleException("error.accountLocked");
        }
        if (!user.authenticate(password)) {
            user.registerFailedLogin(now);
            userRepository.updateLoginState(user);
            throw new BusinessRuleException("error.invalidCredentials");
        }
        user.registerSuccessfulLogin();
        userRepository.updateLoginState(user);
        return user;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
