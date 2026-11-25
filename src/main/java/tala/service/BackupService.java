package tala.service;

import tala.database.DBConnection;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * BackupService
 * ------------------------------------------------------------
 * "Back up database" on the Admin page. Writes ONE .sql file with
 * every table and every row, using plain JDBC (no mysqldump needed).
 *
 * To restore: open the file in MySQL Workbench / phpMyAdmin and run it.
 * It drops and re-creates the tables, so the data goes back exactly
 * to how it was when the backup was made.
 */
public class BackupService {

    /** Parents first, so the restore can rebuild the foreign keys in order */
    private static final String[] TABLES = {
        "accounts", "journal_entries", "journal_lines", "app_settings", "users", "activity_log"
    };

    /** Writes the backup and returns how many rows were saved */
    public int backup(File file) throws SQLException, IOException {
        int rows = 0;
        try (Connection conn = DBConnection.getConnection();
             PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {

            out.println("-- TALA Accounting System - database backup");
            out.println("-- Made on " + LocalDateTime.now().withNano(0) + " from " + DBConnection.DATABASE_NAME);
            out.println("-- Restore: run this whole file in MySQL Workbench or phpMyAdmin.");
            out.println();
            out.println("CREATE DATABASE IF NOT EXISTS " + DBConnection.DATABASE_NAME + ";");
            out.println("USE " + DBConnection.DATABASE_NAME + ";");
            out.println("SET FOREIGN_KEY_CHECKS = 0;");
            out.println();

            for (String table : TABLES) {
                rows += writeTable(conn, out, table);
            }

            out.println("SET FOREIGN_KEY_CHECKS = 1;");
        }
        return rows;
    }

    private int writeTable(Connection conn, PrintWriter out, String table) throws SQLException {
        int rows = 0;
        try (Statement st = conn.createStatement()) {
            out.println("-- ------------------------------------------------------------");
            out.println("-- " + table);
            out.println("-- ------------------------------------------------------------");
            out.println("DROP TABLE IF EXISTS " + table + ";");
            try (ResultSet rs = st.executeQuery("SHOW CREATE TABLE " + table)) {
                if (rs.next()) {
                    out.println(rs.getString(2) + ";");
                }
            }

            try (ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columns = meta.getColumnCount();
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder("INSERT INTO ").append(table).append(" VALUES (");
                    for (int c = 1; c <= columns; c++) {
                        if (c > 1) {
                            sb.append(", ");
                        }
                        sb.append(sqlValue(rs.getObject(c)));
                    }
                    out.println(sb.append(");"));
                    rows++;
                }
            }
            out.println();
        }
        return rows;
    }

    /** A Java value written the way SQL expects it: NULL, 12.50 or 'text' */
    private static String sqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof LocalDateTime) {
            return "'" + value.toString().replace('T', ' ') + "'";   // 2026-11-23 21:12:05
        }
        String text = value.toString()
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "'" + text + "'";
    }
}
