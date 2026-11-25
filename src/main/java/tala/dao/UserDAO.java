package tala.dao;

import tala.database.DBConnection;
import tala.model.User;
import tala.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO
 * ------------------------------------------------------------
 * All the SQL for the "users" table: log-in checks, the Admin
 * page's user list, adding users, approving, turning on/off,
 * changing roles and resetting passwords.
 */
public class UserDAO {

    private static final String COLUMNS =
            "user_id, full_name, username, role, status, last_login";

    /** Every user, admins first, then by name */
    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY role = 'Admin' DESC, full_name";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(readUser(rs));
            }
        }
        return users;
    }

    /** One user by username, or null if there is none */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readUser(rs) : null;
            }
        }
    }

    /** The stored "salt:hash" of a user's password, or null if the user does not exist */
    public String getPasswordHash(String username) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public boolean isUsernameTaken(String username) throws SQLException {
        return findByUsername(username) != null;
    }

    /** Saves a new user. The password is hashed here, never stored as typed. */
    public void addUser(String fullName, String username, String password, String role,
                        String status) throws SQLException {
        String sql = "INSERT INTO users (full_name, username, password_hash, role, status)"
                   + " VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, role);
            ps.setString(5, status);
            ps.executeUpdate();
        }
    }

    public void setStatus(int userId, String status) throws SQLException {
        update("UPDATE users SET status = ? WHERE user_id = ?", status, userId);
    }

    public void setRole(int userId, String role) throws SQLException {
        update("UPDATE users SET role = ? WHERE user_id = ?", role, userId);
    }

    public void resetPassword(int userId, String newPassword) throws SQLException {
        update("UPDATE users SET password_hash = ? WHERE user_id = ?", PasswordUtil.hash(newPassword), userId);
    }

    public void deleteUser(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** Called after a successful log in */
    public void touchLastLogin(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET last_login = NOW() WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** How many admins can still log in (we never let this reach zero) */
    public int countActiveAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'Admin' AND status = 'Active'";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ------------------------------------------------------------ helpers

    private void update(String sql, String value, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private static User readUser(ResultSet rs) throws SQLException {
        Timestamp last = rs.getTimestamp("last_login");
        return new User(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("status"),
                last == null ? null : last.toLocalDateTime());
    }
}
