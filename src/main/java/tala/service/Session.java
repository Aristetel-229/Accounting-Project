package tala.service;

import tala.model.User;

/**
 * Session
 * ------------------------------------------------------------
 * Remembers WHO is logged in while the program runs.
 * LoginFrame calls start(user) after a good log in;
 * "Log out" calls end().
 *
 * Any page can ask Session.isAdmin() to show or hide admin-only things.
 */
public final class Session {

    private static User user;

    private Session() { }

    public static void start(User loggedIn) { user = loggedIn; }
    public static void end()                { user = null; }

    /** The logged-in user, or null before log in */
    public static User getUser() { return user; }

    public static boolean isAdmin() {
        return user != null && user.isAdmin();
    }
}
