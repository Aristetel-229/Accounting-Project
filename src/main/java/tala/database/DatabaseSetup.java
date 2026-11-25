package tala.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import tala.util.PasswordUtil;

/**
 * DatabaseSetup
 * ------------------------------------------------------------
 * Runs once when the program starts:
 *   1. creates the tala_accounting database if it does not exist
 *   2. creates the six tables if they do not exist
 *   3. fills the Chart of Accounts if it is still empty
 *   4. adds the two starting log-in accounts if there are no users yet
 *   5. upgrades tables made by an older TALA (role "Student" -> "User")
 *
 * The SQL here is the same as database/tala_accounting.sql,
 * so you can read either one to understand the tables.
 */
public class DatabaseSetup {

    private static final String CREATE_ACCOUNTS =
            "CREATE TABLE IF NOT EXISTS accounts ("
            + " account_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " account_code INT NOT NULL UNIQUE,"
            + " account_name VARCHAR(100) NOT NULL UNIQUE,"
            + " account_type ENUM('Asset','Liability','Equity','Revenue','Expense') NOT NULL,"
            + " normal_balance ENUM('Debit','Credit') NOT NULL,"
            + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final String CREATE_JOURNAL_ENTRIES =
            "CREATE TABLE IF NOT EXISTS journal_entries ("
            + " entry_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " entry_date DATE NOT NULL,"
            + " reference_no VARCHAR(20) NOT NULL UNIQUE,"
            + " description VARCHAR(255) NOT NULL,"
            + " entry_type ENUM('Regular','Adjusting') NOT NULL DEFAULT 'Regular',"
            + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final String CREATE_JOURNAL_LINES =
            "CREATE TABLE IF NOT EXISTS journal_lines ("
            + " line_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " entry_id INT NOT NULL,"
            + " account_id INT NOT NULL,"
            + " debit DECIMAL(15,2) NOT NULL DEFAULT 0.00,"
            + " credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,"
            + " CONSTRAINT fk_line_entry FOREIGN KEY (entry_id) REFERENCES journal_entries(entry_id) ON DELETE CASCADE,"
            + " CONSTRAINT fk_line_account FOREIGN KEY (account_id) REFERENCES accounts(account_id),"
            + " CONSTRAINT chk_line_amount CHECK (debit >= 0 AND credit >= 0"
            + "   AND (debit = 0 OR credit = 0) AND (debit > 0 OR credit > 0)))";

    private static final String CREATE_APP_SETTINGS =
            "CREATE TABLE IF NOT EXISTS app_settings ("
            + " setting_key VARCHAR(50) PRIMARY KEY,"
            + " setting_value VARCHAR(255) NOT NULL)";

    /** People who can log in. role: User = user access, Admin = admin access */
    private static final String CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users ("
            + " user_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " full_name VARCHAR(100) NOT NULL,"
            + " username VARCHAR(40) NOT NULL UNIQUE,"
            + " password_hash VARCHAR(100) NOT NULL,"
            + " role ENUM('User','Admin') NOT NULL DEFAULT 'User',"
            + " status ENUM('Active','Pending','Inactive') NOT NULL DEFAULT 'Active',"
            + " last_login DATETIME NULL,"
            + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    /** One line per action, shown as "Recent activity" on the Admin page */
    private static final String CREATE_ACTIVITY_LOG =
            "CREATE TABLE IF NOT EXISTS activity_log ("
            + " log_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " who VARCHAR(100) NOT NULL,"
            + " action VARCHAR(255) NOT NULL,"
            + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    /**
     * The starting log-in accounts: { full name, username, password, role }.
     * Passwords are saved as a hash (see PasswordUtil), never as plain text.
     * Change these passwords from the Admin page after the first log in.
     */
    private static final String[][] DEFAULT_USERS = {
        {"UCMN Administrator", "ucmn-admin123", "12345678", "Admin"},
        {"UCMN User", "ucmn-student123", "12345678", "User"}
    };

    /** The old starting password (5 characters), replaced because passwords now need 8 */
    private static final String OLD_DEFAULT_PASSWORD = "12345";

    /**
     * The starting Chart of Accounts: { code, name, type, normal balance }.
     * Contra accounts (Allowance, Accumulated Depreciation) are Assets with a Credit balance.
     */
    private static final Object[][] DEFAULT_ACCOUNTS = {
        {1010, "Cash", "Asset", "Debit"},
        {1100, "Accounts Receivable", "Asset", "Debit"},
        {1110, "Allowance for Doubtful Accounts", "Asset", "Credit"},
        {1120, "Notes Receivable", "Asset", "Debit"},
        {1130, "Interest Receivable", "Asset", "Debit"},
        {1200, "Inventory", "Asset", "Debit"},
        {1300, "Supplies", "Asset", "Debit"},
        {1310, "Prepaid Insurance", "Asset", "Debit"},
        {1320, "Prepaid Rent", "Asset", "Debit"},
        {1330, "Advances to Employees", "Asset", "Debit"},
        {1500, "Office Equipment", "Asset", "Debit"},
        {1510, "Furniture", "Asset", "Debit"},
        {1520, "Vehicles", "Asset", "Debit"},
        {1590, "Accumulated Depreciation", "Asset", "Credit"},
        {1600, "Land", "Asset", "Debit"},
        {1610, "Building", "Asset", "Debit"},
        {2010, "Accounts Payable", "Liability", "Credit"},
        {2020, "Salaries Payable", "Liability", "Credit"},
        {2030, "Taxes Payable", "Liability", "Credit"},
        {2040, "Unearned Revenue", "Liability", "Credit"},
        {2100, "Notes Payable", "Liability", "Credit"},
        {3010, "Owner's Capital", "Equity", "Credit"},
        {3020, "Owner's Drawing", "Equity", "Debit"},
        {4010, "Service Revenue", "Revenue", "Credit"},
        {4020, "Interest Income", "Revenue", "Credit"},
        {5010, "Salaries Expense", "Expense", "Debit"},
        {5020, "Rent Expense", "Expense", "Debit"},
        {5030, "Utilities Expense", "Expense", "Debit"},
        {5040, "Supplies Expense", "Expense", "Debit"},
        {5050, "Insurance Expense", "Expense", "Debit"},
        {5060, "Depreciation Expense", "Expense", "Debit"},
        {5070, "Bad Debts Expense", "Expense", "Debit"}
    };

    /**
     * Prepares everything. Throws SQLException if MySQL cannot be reached,
     * so Main can show a helpful message.
     */
    public static void prepareDatabase() throws SQLException {
        createDatabaseIfMissing();
        createTablesIfMissing();
        upgradeOldTables();
        addDefaultAccountsIfEmpty();
        addDefaultUsersIfEmpty();
    }

    /** Step 1: CREATE DATABASE IF NOT EXISTS tala_accounting */
    private static void createDatabaseIfMissing() throws SQLException {
        try (Connection conn = DBConnection.getServerConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DBConnection.DATABASE_NAME);
        }
    }

    /** Step 2: create the tables (nothing happens if they already exist) */
    private static void createTablesIfMissing() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(CREATE_ACCOUNTS);
            st.executeUpdate(CREATE_JOURNAL_ENTRIES);
            st.executeUpdate(CREATE_JOURNAL_LINES);
            st.executeUpdate(CREATE_APP_SETTINGS);
            st.executeUpdate(CREATE_USERS);
            st.executeUpdate(CREATE_ACTIVITY_LOG);
        }
    }

    /** Step 3: insert the starting accounts, but only into an empty table */
    private static void addDefaultAccountsIfEmpty() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {

            // How many accounts are there already?
            int count = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts")) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            if (count > 0) {
                return; // the user already has a chart of accounts; leave it alone
            }

            String sql = "INSERT INTO accounts (account_code, account_name, account_type, normal_balance)"
                       + " VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Object[] row : DEFAULT_ACCOUNTS) {
                    ps.setInt(1, (Integer) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]);
                    ps.setString(4, (String) row[3]);
                    ps.addBatch();          // collect all inserts...
                }
                ps.executeBatch();          // ...then send them in one go
            }
        }
    }

    /** Step 4: the admin and user log-ins, but only into an empty users table */
    private static void addDefaultUsersIfEmpty() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;   // users exist already; leave them alone
                }
            }
            String sql = "INSERT INTO users (full_name, username, password_hash, role, status)"
                       + " VALUES (?, ?, ?, ?, 'Active')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String[] u : DEFAULT_USERS) {
                    ps.setString(1, u[0]);
                    ps.setString(2, u[1]);
                    ps.setString(3, PasswordUtil.hash(u[2]));
                    ps.setString(4, u[3]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    /**
     * Step 5: a database made by an older TALA is brought up to date.
     *   - role 'Student' becomes 'User' (only User and Admin exist now)
     *   - the old "students_can_..." switches are renamed "users_can_..." (their values are kept)
     *   - the starting account's name "UCMN Student" becomes "UCMN User"
     *   - a starting account whose password is STILL the old "12345" gets the new
     *     8-character starting password (a password someone changed is left alone)
     * The old class_group column, if any, is simply no longer used.
     * Nothing happens on a database that is already up to date.
     */
    private static void upgradeOldTables() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {

            String roleType = "";
            try (ResultSet rs = st.executeQuery(
                    "SELECT COLUMN_TYPE FROM information_schema.COLUMNS"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role'")) {
                if (rs.next()) {
                    roleType = rs.getString(1);
                }
            }
            if (roleType.contains("'Student'")) {
                // allow both for a moment, move everyone over, then remove 'Student'
                st.executeUpdate("ALTER TABLE users MODIFY role ENUM('Student','User','Admin') NOT NULL DEFAULT 'User'");
                st.executeUpdate("UPDATE users SET role = 'User' WHERE role = 'Student'");
                st.executeUpdate("ALTER TABLE users MODIFY role ENUM('User','Admin') NOT NULL DEFAULT 'User'");
            }

            st.executeUpdate("UPDATE IGNORE app_settings SET setting_key = CONCAT('users_', SUBSTRING(setting_key, 10))"
                    + " WHERE setting_key LIKE 'students\\_%'");
            st.executeUpdate("UPDATE users SET full_name = 'UCMN User' WHERE full_name = 'UCMN Student'");

            for (String[] u : DEFAULT_USERS) {
                String stored = null;
                try (PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM users WHERE username = ?")) {
                    ps.setString(1, u[1]);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            stored = rs.getString(1);
                        }
                    }
                }
                if (stored != null && PasswordUtil.matches(OLD_DEFAULT_PASSWORD, stored)) {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE username = ?")) {
                        ps.setString(1, PasswordUtil.hash(u[2]));
                        ps.setString(2, u[1]);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }
}
