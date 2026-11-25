package tala.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection
 * ------------------------------------------------------------
 * The ONE place where the MySQL connection is made.
 *
 * The settings are read from a small file, db.properties, in the project
 * folder (next to run.bat). That file stays on your computer only: it is
 * listed in .gitignore, so your MySQL password is never uploaded to GitHub.
 * Copy db.properties.example to db.properties and fill in your own values:
 *
 *   db.host=localhost
 *   db.port=3306
 *   db.name=tala_accounting
 *   db.user=root
 *   db.password=          (XAMPP's root password is empty)
 *
 * Without the file, those defaults are used. You can also set any of them
 * when starting Java, which wins over the file:
 *   java -Dtala.db.port=3307 -Dtala.db.password=secret ... tala.Main
 */
public class DBConnection {

    private static final java.util.Properties FILE = loadSettingsFile();

    public static final String HOST = setting("host", "localhost");
    public static final int PORT = Integer.parseInt(setting("port", "3306").trim());
    public static final String DATABASE_NAME = setting("name", "tala_accounting");
    public static final String USERNAME = setting("user", "root");
    private static final String PASSWORD = setting("password", "");

    /** -Dtala.db.<key> first, then db.<key> in db.properties, then the default */
    private static String setting(String key, String fallback) {
        String fromJava = System.getProperty("tala.db." + key);
        if (fromJava != null) {
            return fromJava;
        }
        return FILE.getProperty("db." + key, fallback);
    }

    /** Reads db.properties from the folder the program was started in (empty if it is missing) */
    private static java.util.Properties loadSettingsFile() {
        java.util.Properties p = new java.util.Properties();
        java.io.File file = new java.io.File(System.getProperty("tala.db.config", "db.properties"));
        if (file.isFile()) {
            try (java.io.Reader in = new java.io.InputStreamReader(
                    new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
                p.load(in);
            } catch (java.io.IOException e) {
                System.err.println("Could not read " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }
        return p;
    }

    // The class name of the MySQL JDBC driver (inside mysql-connector-j.jar)
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    // Extra options added to the end of the connection URL
    private static final String URL_OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila";

    /**
     * Opens a connection to the tala_accounting database.
     * Always close it after use (we use try-with-resources for that).
     */
    public static Connection getConnection() throws SQLException {
        loadDriver();
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE_NAME + URL_OPTIONS;
        return DriverManager.getConnection(url, USERNAME, PASSWORD);
    }

    /**
     * Opens a connection to the MySQL server itself (no database selected).
     * Used only once at start-up to create the database if it is missing.
     */
    public static Connection getServerConnection() throws SQLException {
        loadDriver();
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + URL_OPTIONS;
        return DriverManager.getConnection(url, USERNAME, PASSWORD);
    }

    /**
     * Makes sure the MySQL driver class can be found.
     * If the connector .jar was not added to the project, we explain it clearly.
     */
    private static void loadDriver() throws SQLException {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL Connector/J was not found.\n"
                            + "Put mysql-connector-j-<version>.jar inside the lib folder\n"
                            + "(or open the project as a Maven project so it downloads it).",
                    e);
        }
    }

    /** Quick yes/no check used by the "MySQL connected" label in the top bar. */
    public static boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
