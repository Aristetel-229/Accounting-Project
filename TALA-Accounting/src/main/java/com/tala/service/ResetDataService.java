package com.tala.service;

import com.tala.db.DB;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Clears everything the user has entered (journal entries and adjustments). The Chart of Accounts is kept. */
public final class ResetDataService {
    private ResetDataService() {}

    public static void resetInputs() throws SQLException {
        try (Connection c = DB.getConnection()) {
            boolean original = c.getAutoCommit();
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM journal_entry_lines");
                st.executeUpdate("DELETE FROM journal_entries");
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(original);
            }
        }
    }
}
