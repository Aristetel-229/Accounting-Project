package com.accountpro.service;

import com.accountpro.db.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Balance calculations shared by the dashboard and the financial statements. */
public final class ReportService {
    private ReportService() {}

    public static BigDecimal accountTypeBalance(Connection c, String type) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(CASE WHEN je.status = 'POSTED' THEN jel.debit - jel.credit ELSE 0 END), 0) AS balance
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id = a.id
            LEFT JOIN journal_entries je ON je.id = jel.journal_entry_id
            WHERE a.account_type = ? AND a.is_active = TRUE
            """;
        // The join condition above excludes reversed entries by filtering the entry status.
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal net = rs.getBigDecimal("balance");
                if (net == null) net = BigDecimal.ZERO;
                // Return a positive presentation amount based on the category's normal balance.
                if ("LIABILITY".equals(type) || "EQUITY".equals(type) || "REVENUE".equals(type)) {
                    net = net.negate();
                }
                return net;
            }
        }
    }

    public static BigDecimal calculateNetIncome() {
        try (Connection c = DB.getConnection()) {
            BigDecimal rev = accountTypeBalance(c, "REVENUE");
            BigDecimal exp = accountTypeBalance(c, "EXPENSE");
            return rev.subtract(exp);
        } catch (SQLException ex) {
            return BigDecimal.ZERO;
        }
    }
}
