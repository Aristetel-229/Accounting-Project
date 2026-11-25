package tala.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtil
 * ------------------------------------------------------------
 * Passwords are NEVER saved as plain text. We save a "hash":
 *
 *   salt   = 16 random bytes, different for every user
 *   hash   = SHA-256 of (salt + password), repeated 10,000 times
 *   stored = "salt:hash"   (both written in Base64)
 *
 * To check a password we hash what was typed with the same salt
 * and compare the results. Only Java's built-in classes are used,
 * so no extra library is needed.
 */
public final class PasswordUtil {

    private static final int ROUNDS = 10_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    /** Makes a new "salt:hash" for a password */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(digest(salt, password));
    }

    /** True if the typed password matches the stored "salt:hash" */
    public static boolean matches(String password, String stored) {
        if (password == null || stored == null || !stored.contains(":")) {
            return false;
        }
        try {
            String[] parts = stored.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            // MessageDigest.isEqual takes the same time for every input (safer than Arrays.equals)
            return MessageDigest.isEqual(expected, digest(salt, password));
        } catch (IllegalArgumentException e) {
            return false;   // a broken value in the table
        }
    }

    private static byte[] digest(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] result = md.digest(password.getBytes(StandardCharsets.UTF_8));
            for (int i = 1; i < ROUNDS; i++) {
                md.reset();
                result = md.digest(result);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from this Java", e);   // never happens
        }
    }
}
