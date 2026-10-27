package com.tala.service;

import com.tala.db.DB;
import com.tala.model.AccountOption;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Database access for journal entries. */
public final class JournalService {
    private JournalService() {}

    public static List<AccountOption> loadActiveAccounts() throws SQLException {
        List<AccountOption> options = new ArrayList<>();
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, account_code, account_name FROM accounts WHERE is_active=TRUE ORDER BY account_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                options.add(new AccountOption(rs.getLong("id"),
                        rs.getString("account_code"), rs.getString("account_name")));
            }
        }
        return options;
    }

    /** Posts a simple two-line entry in one transaction and returns the new entry ID. */
    public static long postTwoLineEntry(Date entryDate, String reference, String description,
                                        AccountOption debit, AccountOption credit, BigDecimal amount) throws SQLException {
        String insertEntry = "INSERT INTO journal_entries(entry_date, reference_no, description, status) VALUES (?, ?, ?, 'POSTED')";
        String insertLine = "INSERT INTO journal_entry_lines(journal_entry_id, account_id, debit, credit, line_description) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            try {
                long entryId;
                try (PreparedStatement ps = c.prepareStatement(insertEntry, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setDate(1, entryDate);
                    ps.setString(2, reference);
                    ps.setString(3, description);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not obtain journal entry ID.");
                        entryId = keys.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(insertLine)) {
                    ps.setLong(1, entryId); ps.setLong(2, debit.id);
                    ps.setBigDecimal(3, amount); ps.setBigDecimal(4, BigDecimal.ZERO);
                    ps.setString(5, description); ps.executeUpdate();

                    ps.setLong(1, entryId); ps.setLong(2, credit.id);
                    ps.setBigDecimal(3, BigDecimal.ZERO); ps.setBigDecimal(4, amount);
                    ps.setString(5, description); ps.executeUpdate();
                }
                c.commit();
                return entryId;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
