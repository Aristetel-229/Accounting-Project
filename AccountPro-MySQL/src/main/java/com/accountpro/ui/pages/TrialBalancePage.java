package com.accountpro.ui.pages;

import com.accountpro.ui.BasePage;
import com.accountpro.ui.components.Tables;
import com.accountpro.ui.MainFrame;
import com.accountpro.util.Money;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

public class TrialBalancePage extends BasePage {
    public TrialBalancePage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Trial Balance"; }

    @Override protected String hint() { return "If your books are right, total debits equal total credits."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        String sql = """
            SELECT a.account_code, a.account_name,
                   COALESCE(SUM(jel.debit),0) AS debits,
                   COALESCE(SUM(jel.credit),0) AS credits
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id = a.id
            LEFT JOIN journal_entries je ON je.id = jel.journal_entry_id AND je.status = 'POSTED'
            WHERE a.is_active = TRUE
            GROUP BY a.id, a.account_code, a.account_name
            ORDER BY a.account_code
            """;
        DefaultTableModel raw = readTable(sql, new String[]{"Code", "Account", "Debits", "Credits"});
        DefaultTableModel display = newTableModel(new String[]{"Code", "Account", "Debit Balance", "Credit Balance"});
        BigDecimal totalDr = BigDecimal.ZERO, totalCr = BigDecimal.ZERO;
        for (int i = 0; i < raw.getRowCount(); i++) {
            BigDecimal dr = new BigDecimal(raw.getValueAt(i, 2).toString());
            BigDecimal cr = new BigDecimal(raw.getValueAt(i, 3).toString());
            BigDecimal net = dr.subtract(cr);
            BigDecimal debitBalance = net.signum() > 0 ? net : BigDecimal.ZERO;
            BigDecimal creditBalance = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
            totalDr = totalDr.add(debitBalance);
            totalCr = totalCr.add(creditBalance);
            if (debitBalance.signum() != 0 || creditBalance.signum() != 0) {
                display.addRow(new Object[]{raw.getValueAt(i, 0), raw.getValueAt(i, 1),
                        Money.currency(debitBalance), Money.currency(creditBalance)});
            }
        }
        display.addRow(new Object[]{"", "TOTAL", Money.currency(totalDr), Money.currency(totalCr)});
        JTable table = Tables.create(display);
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        JLabel balance = new JLabel("Difference: " + Money.currency(totalDr.subtract(totalCr).abs()));
        balance.setFont(new Font("SansSerif", Font.BOLD, 14));
        p.add(balance, BorderLayout.SOUTH);
        return p;
    }
}
