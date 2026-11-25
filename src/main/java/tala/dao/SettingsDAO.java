package tala.dao;

import tala.database.DBConnection;
import tala.model.AccountingPeriod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * SettingsDAO
 * ------------------------------------------------------------
 * Small app settings saved in the "app_settings" table as key = value,
 * so they are remembered the next time the program starts.
 *
 * Used for the accounting period:
 *   period_from = 2026-11
 *   period_to   = 2026-12
 *
 * and for the Admin page:
 *   closed_periods          = 2026-09,2026-10   (months nobody can post into)
 *   last_backup             = 2026-11-20 21:12
 *   users_can_delete ... = true / false         (the System settings switches)
 */
public class SettingsDAO {

    private static final String PERIOD_FROM = "period_from";
    private static final String PERIOD_TO = "period_to";
    private static final String CLOSED_PERIODS = "closed_periods";
    public static final String LAST_BACKUP = "last_backup";

    // ---------- the Admin page's "System settings" switches ----------
    /** Old switch (entries can no longer be deleted from the app); kept so old saved settings still read */
    public static final String USERS_CAN_DELETE = "users_can_delete";
    public static final String USERS_CAN_ADD_ACCOUNTS = "users_can_add_accounts";
    public static final String USERS_CAN_DELETE_ACCOUNTS = "users_can_delete_accounts";
    public static final String USERS_CAN_CHANGE_PERIOD = "users_can_change_period";
    public static final String LOCK_CLOSED_PERIODS = "lock_closed_periods";
    public static final String SIGNUPS_NEED_APPROVAL = "signups_need_approval";

    /** Each switch: { key, title, explanation, value when never saved } */
    public static final String[][] SWITCHES = {
        {USERS_CAN_ADD_ACCOUNTS, "Users can add accounts", "Off: only admins add to the Chart of Accounts", "true"},
        {USERS_CAN_DELETE_ACCOUNTS, "Users can delete accounts", "Off: only admins remove accounts from the Chart of Accounts", "false"},
        {USERS_CAN_CHANGE_PERIOD, "Users can change the period", "Off: only admins pick the From / To months", "true"},
        {LOCK_CLOSED_PERIODS, "Lock closed periods", "No posting or deleting in a closed month", "true"},
        {SIGNUPS_NEED_APPROVAL, "Sign-ups need approval", "New user accounts wait for an admin", "true"},
    };

    /** One setting's value, or null if it was never saved */
    public String getSetting(String key) throws SQLException {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Saves a setting: inserts it the first time, updates it after that */
    public void saveSetting(String key, String value) throws SQLException {
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)"
                   + " ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    /** The saved accounting period, or this month only if none was saved yet */
    public AccountingPeriod getAccountingPeriod() throws SQLException {
        String from = getSetting(PERIOD_FROM);
        String to = getSetting(PERIOD_TO);
        try {
            if (from != null && to != null) {
                return new AccountingPeriod(YearMonth.parse(from), YearMonth.parse(to));
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            // a broken value in the table: fall back to this month
        }
        return AccountingPeriod.ofMonth(YearMonth.now());
    }

    public void saveAccountingPeriod(AccountingPeriod period) throws SQLException {
        saveSetting(PERIOD_FROM, period.getFrom().toString());   // "2026-11"
        saveSetting(PERIOD_TO, period.getTo().toString());
    }

    // ------------------------------------------------------------ switches (true / false)

    /** A switch's value, or its default if it was never saved */
    public boolean getFlag(String key) throws SQLException {
        String value = getSetting(key);
        if (value == null) {
            for (String[] sw : SWITCHES) {
                if (sw[0].equals(key)) {
                    return Boolean.parseBoolean(sw[3]);
                }
            }
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    public void saveFlag(String key, boolean on) throws SQLException {
        saveSetting(key, String.valueOf(on));
    }

    /** Every switch with its current value, in the order of SWITCHES */
    public Map<String, Boolean> getAllFlags() throws SQLException {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        for (String[] sw : SWITCHES) {
            flags.put(sw[0], getFlag(sw[0]));
        }
        return flags;
    }

    // ------------------------------------------------------------ closed periods

    /** The months an admin has closed, oldest first */
    public TreeSet<YearMonth> getClosedMonths() throws SQLException {
        TreeSet<YearMonth> months = new TreeSet<>();
        String value = getSetting(CLOSED_PERIODS);
        if (value != null) {
            for (String part : value.split(",")) {
                try {
                    if (!part.trim().isEmpty()) {
                        months.add(YearMonth.parse(part.trim()));
                    }
                } catch (DateTimeParseException ignored) {
                    // skip a broken value
                }
            }
        }
        return months;
    }

    public void setMonthClosed(YearMonth month, boolean closed) throws SQLException {
        TreeSet<YearMonth> months = getClosedMonths();
        if (closed) {
            months.add(month);
        } else {
            months.remove(month);
        }
        StringBuilder sb = new StringBuilder();
        for (YearMonth m : months) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(m);
        }
        saveSetting(CLOSED_PERIODS, sb.toString());
    }

    /**
     * True if nobody may post into (or delete from) this date's month:
     * the month is closed AND the "Lock closed periods" switch is on.
     */
    public boolean isLocked(LocalDate date) throws SQLException {
        return date != null && getFlag(LOCK_CLOSED_PERIODS) && getClosedMonths().contains(YearMonth.from(date));
    }
}
