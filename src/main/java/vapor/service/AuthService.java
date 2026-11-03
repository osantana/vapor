package vapor.service;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import vapor.BusinessRuleException;
import vapor.NotFoundException;
import vapor.model.Role;
import vapor.model.User;
import vapor.repository.UserRepository;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Good enough for a form: something@domain.tld, without spaces.
    public static boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');
        boolean hasSpace = email.contains(" ");
        return email.length() <= 254 && !hasSpace && at > 0 && dot > at + 1 && dot < email.length() - 1;
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
        if (password.isEmpty()) {
            throw new BusinessRuleException("error.passwordRequired");
        }
        if (userRepository.emailExists(cleanEmail)) {
            throw new BusinessRuleException("error.emailTaken");
        }
        User user = new User(0, cleanName, cleanEmail, PasswordHasher.hash(password), role, OffsetDateTime.now());
        return userRepository.insert(user);
    }

    public User login(String email, String password) throws SQLException, GeneralSecurityException {
        User user;
        try {
            user = userRepository.findByEmail(normalize(email));
        } catch (NotFoundException exception) {
            throw new BusinessRuleException("error.invalidCredentials");
        }
        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("error.invalidCredentials");
        }
        return user;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
