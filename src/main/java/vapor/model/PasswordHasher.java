package vapor.model;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Hashes passwords with PBKDF2, which ships with the JDK.
// The stored text has the format "iterations:salt:hash", with salt and hash in hexadecimal.
public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600000;
    private static final int SALT_SIZE_IN_BYTES = 16;
    private static final int HASH_SIZE_IN_BITS = 256;

    public static String hash(String password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_SIZE_IN_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = calculate(password, salt, ITERATIONS);
        HexFormat hex = HexFormat.of();
        return ITERATIONS + ":" + hex.formatHex(salt) + ":" + hex.formatHex(hash);
    }

    public static boolean matches(String password, String storedHash) throws GeneralSecurityException {
        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }
        HexFormat hex = HexFormat.of();
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = hex.parseHex(parts[1]);
        byte[] expected = hex.parseHex(parts[2]);
        byte[] received = calculate(password, salt, iterations);
        return MessageDigest.isEqual(expected, received);
    }

    private static byte[] calculate(String password, byte[] salt, int iterations)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_SIZE_IN_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }
}
