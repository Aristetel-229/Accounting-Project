package com.tala.service;

import com.tala.db.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Calculations for the 12-column worksheet.
 *
 * The 12 value columns are six debit/credit pairs, in this order:
 *   0 Unadjusted Trial Balance | 1 Adjustments | 2 Adjusted Trial Balance
 *   3 Income Statement | 4 Owner's Equity | 5 Balance Sheet
 * Index of a pair's debit column = pair * 2, credit column = pair * 2 + 1.
 */
public final class WorksheetService {
    private WorksheetService() {}

    public static final int PAIRS = 6;
    public static final int COLS = PAIRS * 2;

    /** One account's net movement (debits minus credits) before and after adjustments. */
    public static final class AccountRow {
        public final String code, name, type;
        public final BigDecimal unadjNet, adjNet;
        public AccountRow(String code, String name, String type, BigDecimal unadjNet, BigDecimal adjNet) {
            this.code = code; this.name = name; this.type = type;
            this.unadjNet = unadjNet; this.adjNet = adjNet;
        }
    }

    /** One worksheet line: an account with its 12 values. */
    public static final class Line {
        public final String code, name;
        public final BigDecimal[] v;
        Line(String code, String name, BigDecimal[] v) { this.code = code; this.name = name; this.v = v; }
    }

    /** Everything the page needs to draw the worksheet. */
    public static final class Result {
        public List<Line> lines = new ArrayList<>();
        public BigDecimal income = BigDecimal.ZERO;                 // revenue - expenses (negative = loss)
        public BigDecimal[] netIncomeRow = zeros();                 // balancing figures for net income / loss
        public BigDecimal[] endingEquityRow = zeros();              // balancing figures for ending owner's equity
        public BigDecimal[] totals = zeros();                       // grand total of every column
        public BigDecimal[] differences = new BigDecimal[PAIRS];    // debit - credit of each pair (must be 0)
        public boolean isEmpty() { return lines.isEmpty(); }
        public BigDecimal maxDifference() {
            BigDecimal max = BigDecimal.ZERO;
            for (BigDecimal d : differences) if (d.abs().compareTo(max) > 0) max = d.abs();
            return max;
        }
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] a = new BigDecimal[COLS];
        Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static BigDecimal pos(BigDecimal n) { return n.signum() > 0 ? n : BigDecimal.ZERO; }

    /** Pure calculation (no database), so it can be tested on its own. */
    public static Result compute(List<AccountRow> accounts) {
        Result r = new Result();
        for (AccountRow a : accounts) {
            BigDecimal[] v = zeros();
            BigDecimal adjusted = a.unadjNet.add(a.adjNet);          // debit-positive balance after adjustments
            v[0] = pos(a.unadjNet);  v[1] = pos(a.unadjNet.negate());
            v[2] = pos(a.adjNet);    v[3] = pos(a.adjNet.negate());
            v[4] = pos(adjusted);    v[5] = pos(adjusted.negate());
            switch (a.type) {
                case "REVENUE", "EXPENSE" -> {
                    v[6] = pos(adjusted); v[7] = pos(adjusted.negate());
                    r.income = r.income.subtract(adjusted);          // revenue (credit) adds, expense (debit) subtracts
                }
                case "EQUITY" -> { v[8] = pos(adjusted); v[9] = pos(adjusted.negate()); }
                default -> { v[10] = pos(adjusted); v[11] = pos(adjusted.negate()); } // ASSET, LIABILITY
            }
            for (int j = 0; j < COLS; j++) r.totals[j] = r.totals[j].add(v[j]);
            r.lines.add(new Line(a.code, a.name, v));
        }
        if (r.isEmpty()) {
            Arrays.fill(r.differences, BigDecimal.ZERO);
            return r;
        }
        // Net income: debit in Income Statement, credit in Owner's Equity (a loss is the opposite).
        if (r.income.signum() >= 0) {
            r.netIncomeRow[6] = r.income;           r.netIncomeRow[9] = r.income;
        } else {
            r.netIncomeRow[7] = r.income.negate();  r.netIncomeRow[8] = r.income.negate();
        }
        for (int j = 0; j < COLS; j++) r.totals[j] = r.totals[j].add(r.netIncomeRow[j]);

        // Ending owner's equity: balances the Owner's Equity pair and carries to the Balance Sheet credit side.
        BigDecimal ending = r.totals[9].subtract(r.totals[8]);
        if (ending.signum() >= 0) {
            r.endingEquityRow[8] = ending;           r.endingEquityRow[11] = ending;
        } else {
            r.endingEquityRow[9] = ending.negate();  r.endingEquityRow[10] = ending.negate();
        }
        for (int j = 0; j < COLS; j++) r.totals[j] = r.totals[j].add(r.endingEquityRow[j]);

        for (int p = 0; p < PAIRS; p++) r.differences[p] = r.totals[p * 2].subtract(r.totals[p * 2 + 1]);
        return r;
    }

    /** Loads only accounts that have posted lines (an empty database gives an empty worksheet). */
    public static List<AccountRow> loadAccounts() throws SQLException {
        String sql = """
            SELECT a.account_code, a.account_name, a.account_type,
                   SUM(CASE WHEN je.reference_no IS NULL OR je.reference_no NOT LIKE 'ADJ-%'
                            THEN jel.debit - jel.credit ELSE 0 END) AS unadj_net,
                   SUM(CASE WHEN je.reference_no LIKE 'ADJ-%'
                            THEN jel.debit - jel.credit ELSE 0 END) AS adj_net
            FROM accounts a
            JOIN journal_entry_lines jel ON jel.account_id = a.id
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            WHERE je.status = 'POSTED'
            GROUP BY a.id, a.account_code, a.account_name, a.account_type
            ORDER BY a.account_code
            """;
        List<AccountRow> rows = new ArrayList<>();
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new AccountRow(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getBigDecimal(4), rs.getBigDecimal(5)));
            }
        }
        return rows;
    }
}
