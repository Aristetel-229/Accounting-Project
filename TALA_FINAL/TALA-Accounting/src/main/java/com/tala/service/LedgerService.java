package com.tala.service;

import com.tala.db.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the general ledger: one separate ledger per account that has posted activity. */
public final class LedgerService {
    private LedgerService() {}

    /** One posted debit/credit line of an account. */
    public record Posting(LocalDate date, String reference, String description, BigDecimal debit, BigDecimal credit) {}

    /** The ledger of a single account: its postings, running balances and totals. */
    public static final class AccountLedger {
        public final String code;
        public final String name;
        public final String type;          // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
        public final boolean debitNormal;  // normal balance is DEBIT
        public final List<Posting> postings = new ArrayList<>();
        /** Running balance after each posting as debits - credits (positive = debit balance). */
        public final List<BigDecimal> running = new ArrayList<>();
        public BigDecimal totalDebit = BigDecimal.ZERO;
        public BigDecimal totalCredit = BigDecimal.ZERO;

        AccountLedger(String code, String name, String type, boolean debitNormal) {
            this.code = code;
            this.name = name;
            this.type = type;
            this.debitNormal = debitNormal;
        }

        void add(Posting p) {
            postings.add(p);
            totalDebit = totalDebit.add(p.debit());
            totalCredit = totalCredit.add(p.credit());
            running.add(totalDebit.subtract(totalCredit));
        }

        /** Ending balance as debits - credits (positive = debit balance, negative = credit balance). */
        public BigDecimal endingNet() {
            return totalDebit.subtract(totalCredit);
        }
    }

    /**
     * Pure grouping step, kept separate from the database so it can be tested on its own.
     * Each row is {code, name, type, debitNormal (Boolean), Posting}; rows of one account are
     * expected to arrive in posting order.
     */
    public static List<AccountLedger> build(List<Object[]> rows) {
        Map<String, AccountLedger> byCode = new LinkedHashMap<>();
        for (Object[] r : rows) {
            AccountLedger l = byCode.computeIfAbsent((String) r[0],
                    k -> new AccountLedger((String) r[0], (String) r[1], (String) r[2], (Boolean) r[3]));
            l.add((Posting) r[4]);
        }
        return new ArrayList<>(byCode.values());
    }

    /** Loads every POSTED line, grouped by account. Accounts without activity are left out. */
    public static List<AccountLedger> load() throws SQLException {
        String sql = """
            SELECT a.account_code, a.account_name, a.account_type, a.normal_balance,
                   je.entry_date, je.reference_no, je.description, jel.debit, jel.credit
            FROM journal_entry_lines jel
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            ORDER BY a.account_code, je.entry_date, je.id, jel.id
            """;
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Posting p = new Posting(rs.getDate("entry_date").toLocalDate(),
                        rs.getString("reference_no"), rs.getString("description"),
                        rs.getBigDecimal("debit"), rs.getBigDecimal("credit"));
                rows.add(new Object[]{rs.getString("account_code"), rs.getString("account_name"),
                        rs.getString("account_type"), "DEBIT".equals(rs.getString("normal_balance")), p});
            }
        }
        return build(rows);
    }
}
