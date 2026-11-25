package tala.model;

import java.time.LocalDateTime;

/**
 * User
 * ------------------------------------------------------------
 * One person who can log in to TALA (a row of the "users" table).
 *
 *   role   : "User" (user access)  or  "Admin" (admin access)
 *   status : "Active"   - can log in
 *            "Pending"  - signed up, waiting for an admin to approve
 *            "Inactive" - turned off by an admin
 *
 * The password itself is never kept here; only its hash lives in MySQL.
 */
public class User {

    public static final String ADMIN = "Admin";
    public static final String USER = "User";

    public static final String ACTIVE = "Active";
    public static final String PENDING = "Pending";
    public static final String INACTIVE = "Inactive";

    private final int id;
    private final String fullName;
    private final String username;
    private final String role;
    private final String status;
    private final LocalDateTime lastLogin;   // null = never logged in

    public User(int id, String fullName, String username, String role,
                String status, LocalDateTime lastLogin) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.status = status;
        this.lastLogin = lastLogin;
    }

    public int getId()                 { return id; }
    public String getFullName()        { return fullName; }
    public String getUsername()        { return username; }
    public String getRole()            { return role; }
    public String getStatus()          { return status; }
    public LocalDateTime getLastLogin() { return lastLogin; }

    public boolean isAdmin()   { return ADMIN.equals(role); }
    public boolean isActive()  { return ACTIVE.equals(status); }
    public boolean isPending() { return PENDING.equals(status); }

    /** "Juan Dela Cruz" -> "JD" (for the little round avatar) */
    public String getInitials() {
        StringBuilder sb = new StringBuilder();
        for (String word : fullName.trim().split("\\s+")) {
            if (!word.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    /** The first word of the name, for "Hi, Juan" */
    public String getFirstName() {
        String[] words = fullName.trim().split("\\s+");
        return words.length == 0 ? username : words[0];
    }

    @Override
    public String toString() {
        return fullName + " (@" + username + ")";
    }
}
