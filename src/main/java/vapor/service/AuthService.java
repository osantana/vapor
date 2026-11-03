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
            throw new BusinessRuleException("Informe um nome de exibição com até 120 caracteres.");
        }
        if (cleanEmail.isEmpty() || cleanEmail.length() > 254) {
            throw new BusinessRuleException("Informe um e-mail.");
        }
        if (password.isEmpty()) {
            throw new BusinessRuleException("Informe uma senha.");
        }
        if (userRepository.emailExists(cleanEmail)) {
            throw new BusinessRuleException("Este e-mail já está cadastrado.");
        }
        User user = new User(0, cleanName, cleanEmail, PasswordHasher.hash(password), role, OffsetDateTime.now());
        return userRepository.insert(user);
    }

    public User login(String email, String password) throws SQLException, GeneralSecurityException {
        User user;
        try {
            user = userRepository.findByEmail(normalize(email));
        } catch (NotFoundException exception) {
            throw new BusinessRuleException("E-mail ou senha inválidos.");
        }
        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("E-mail ou senha inválidos.");
        }
        return user;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
