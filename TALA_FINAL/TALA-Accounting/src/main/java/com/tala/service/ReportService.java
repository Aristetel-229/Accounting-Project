package com.tala.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Balance calculations used by the dashboard. */
public final class ReportService {
    private ReportService() {}

    /** Balance of an account type as a positive presentation amount (credit-normal types are negated). */
    public static BigDecimal accountTypeBalance(Connection c, String type) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(CASE WHEN je.status = 'POSTED' THEN jel.debit - jel.credit ELSE 0 END), 0) AS balance
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id = a.id
            LEFT JOIN journal_entries je ON je.id = jel.journal_entry_id
            WHERE a.account_type = ? AND a.is_active = TRUE
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal net = rs.getBigDecimal("balance");
                if (net == null) net = BigDecimal.ZERO;
                if ("LIABILITY".equals(type) || "EQUITY".equals(type) || "REVENUE".equals(type)) net = net.negate();
                return net;
            }
        }
    }

    public static int countPostedEntries(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM journal_entries WHERE status = 'POSTED'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
