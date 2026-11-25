package tala.dao;

import tala.database.DBConnection;
import tala.model.User;
import tala.service.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ActivityDAO
 * ------------------------------------------------------------
 * The "activity_log" table: one short line for each thing someone
 * did (logged in, posted JE-0003, approved a user ...).
 * The Admin page shows the latest lines under "Recent activity".
 */
public class ActivityDAO {

    /** One line of the log */
    public static class Activity {
        public final LocalDateTime time;
        public final String who;
        public final String what;

        Activity(LocalDateTime time, String who, String what) {
            this.time = time;
            this.who = who;
            this.what = what;
        }
    }

    /**
     * Writes "what" for the user who is logged in now.
     * A log line is never worth stopping the program for, so errors are ignored.
     */
    public static void record(String what) {
        User user = Session.getUser();
        record(user == null ? "System" : user.getFullName(), what);
    }

    /** Writes a line for a named person (used before anyone is logged in, e.g. sign up) */
    public static void record(String who, String what) {
        String sql = "INSERT INTO activity_log (who, action) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cut(who, 100));
            ps.setString(2, cut(what, 255));
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // the action itself already worked; a missing log line is not a problem
        }
    }

    /** The newest lines first */
    public List<Activity> getRecent(int howMany) throws SQLException {
        String sql = "SELECT created_at, who, action FROM activity_log ORDER BY log_id DESC LIMIT ?";
        List<Activity> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, howMany);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Activity(rs.getTimestamp(1).toLocalDateTime(), rs.getString(2), rs.getString(3)));
                }
            }
        }
        return list;
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
