package com.accountpro.ui.pages;

import com.accountpro.ui.BasePage;
import com.accountpro.ui.components.Tables;
import com.accountpro.ui.MainFrame;
import com.accountpro.util.Money;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

public class IncomeStatementPage extends BasePage {
    public IncomeStatementPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Income Statement (All Posted Entries)"; }

    @Override protected String hint() { return "Revenue minus expenses = net income or loss."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        String sql = """
            SELECT a.account_code, a.account_name, a.account_type,
                   COALESCE(SUM(jel.debit),0) AS debits,
                   COALESCE(SUM(jel.credit),0) AS credits
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id = a.id
            LEFT JOIN journal_entries je ON je.id = jel.journal_entry_id AND je.status = 'POSTED'
            WHERE a.account_type IN ('REVENUE','EXPENSE') AND a.is_active=TRUE
            GROUP BY a.id, a.account_code, a.account_name, a.account_type
            ORDER BY a.account_type, a.account_code
            """;
        DefaultTableModel raw = readTable(sql, new String[]{"Code", "Account", "Type", "Debits", "Credits"});
        DefaultTableModel display = newTableModel(new String[]{"Code", "Account", "Type", "Balance"});
        BigDecimal revenue = BigDecimal.ZERO, expenses = BigDecimal.ZERO;
        for (int i = 0; i < raw.getRowCount(); i++) {
            BigDecimal dr = new BigDecimal(raw.getValueAt(i, 3).toString());
            BigDecimal cr = new BigDecimal(raw.getValueAt(i, 4).toString());
            String type = raw.getValueAt(i, 2).toString();
            BigDecimal balance = type.equals("REVENUE") ? cr.subtract(dr) : dr.subtract(cr);
            if (type.equals("REVENUE")) revenue = revenue.add(balance);
            else expenses = expenses.add(balance);
            if (balance.signum() != 0) display.addRow(new Object[]{
                    raw.getValueAt(i, 0), raw.getValueAt(i, 1), type, Money.currency(balance)});
        }
        display.addRow(new Object[]{"", "TOTAL REVENUE", "", Money.currency(revenue)});
        display.addRow(new Object[]{"", "TOTAL EXPENSES", "", Money.currency(expenses)});
        display.addRow(new Object[]{"", "NET INCOME / (LOSS)", "", Money.currency(revenue.subtract(expenses))});
        JTable table = Tables.create(display);
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        return p;
    }
}
