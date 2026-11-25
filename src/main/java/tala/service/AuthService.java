package tala.service;

import tala.dao.ActivityDAO;
import tala.dao.SettingsDAO;
import tala.dao.UserDAO;
import tala.model.User;
import tala.util.PasswordUtil;

import java.sql.SQLException;

/**
 * AuthService
 * ------------------------------------------------------------
 * Log in, sign up and the rules for new usernames and passwords.
 *
 * Starting accounts (created the first time the program runs):
 *   user side  : ucmn-student123 / 12345678   (role User)
 *   admin side : ucmn-admin123   / 12345678   (role Admin)
 */
public class AuthService {

    /** New and reset passwords need at least this many characters (letters, numbers, spaces...) */
    public static final int MIN_PASSWORD = 8;

    private final UserDAO userDAO;
    private final SettingsDAO settingsDAO;

    public AuthService(UserDAO userDAO, SettingsDAO settingsDAO) {
        this.userDAO = userDAO;
        this.settingsDAO = settingsDAO;
    }

    /** What happened when someone pressed Log in */
    public static class LoginResult {
        public final User user;        // set when the log in worked
        public final String error;     // set when it did not

        private LoginResult(User user, String error) {
            this.user = user;
            this.error = error;
        }

        public boolean ok() { return user != null; }
    }

    /**
     * Checks a username and password.
     * The same message is used for "no such user" and "wrong password",
     * so nobody can find out which usernames exist.
     */
    public LoginResult login(String username, String password) throws SQLException {
        String name = username == null ? "" : username.trim().toLowerCase();
        if (name.isEmpty() || password == null || password.isEmpty()) {
            return new LoginResult(null, "Enter your username and password.");
        }

        String stored = userDAO.getPasswordHash(name);
        if (stored == null || !PasswordUtil.matches(password, stored)) {
            return new LoginResult(null, "Wrong username or password.");
        }

        User user = userDAO.findByUsername(name);
        if (user.isPending()) {
            return new LoginResult(null, "Your account is waiting for an admin to approve it.");
        }
        if (!user.isActive()) {
            return new LoginResult(null, "This account is turned off. Ask an admin to activate it.");
        }

        userDAO.touchLastLogin(user.getId());
        Session.start(user);
        ActivityDAO.record("logged in as " + user.getRole().toLowerCase());
        return new LoginResult(user, null);
    }

    public void logout() {
        if (Session.getUser() != null) {
            ActivityDAO.record("logged out");
        }
        Session.end();
    }

    /**
     * Someone creates their own (User) account from the log-in screen.
     * Returns a message for the screen; throws IllegalArgumentException if a rule is broken.
     */
    public String signUp(String fullName, String username, String password, String confirm)
            throws SQLException {
        String user = username == null ? "" : username.trim().toLowerCase();
        String problem = checkNewUser(fullName, user, password);
        if (problem == null && !password.equals(confirm)) {
            problem = "The two passwords are not the same.";
        }
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }

        boolean needsApproval = settingsDAO.getFlag(SettingsDAO.SIGNUPS_NEED_APPROVAL);
        String status = needsApproval ? User.PENDING : User.ACTIVE;
        userDAO.addUser(fullName.trim(), user, password, User.USER, status);
        ActivityDAO.record(fullName.trim(), "signed up as @" + user);
        return needsApproval
                ? "Account created. An admin has to approve it before you can log in."
                : "Account created. You can log in now.";
    }

    /** The rules for a new account. Returns null if everything is fine. */
    public String checkNewUser(String fullName, String username, String password) throws SQLException {
        String name = fullName == null ? "" : fullName.trim();
        if (name.split("\\s+").length < 2) {
            return "Enter a first and last name.";
        }
        if (name.length() > 100) {
            return "Keep the name under 100 characters.";
        }
        if (!username.matches("[a-z0-9._-]{3,40}")) {
            return "Usernames are 3-40 letters, numbers, dots, dashes or underscores (e.g. ucmn-jdelacruz).";
        }
        if (userDAO.isUsernameTaken(username)) {
            return "Username @" + username + " is already taken.";
        }
        if (password == null || password.length() < MIN_PASSWORD) {
            return "Passwords need at least " + MIN_PASSWORD + " characters.";
        }
        return null;
    }
}
