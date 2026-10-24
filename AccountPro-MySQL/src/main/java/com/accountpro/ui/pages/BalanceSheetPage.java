package com.accountpro.ui.pages;

import com.accountpro.service.ReportService;
import com.accountpro.ui.BasePage;
import com.accountpro.ui.components.Tables;
import com.accountpro.ui.MainFrame;
import com.accountpro.util.Money;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

public class BalanceSheetPage extends BasePage {
    public BalanceSheetPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Balance Sheet (all posted entries)"; }

    @Override protected String hint() { return "The accounting equation: Assets = Liabilities + Equity."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        String sql = """
            SELECT a.account_code, a.account_name, a.account_type,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' THEN jel.debit ELSE 0 END),0) AS debits,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' THEN jel.credit ELSE 0 END),0) AS credits
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id=a.id
            LEFT JOIN journal_entries je ON je.id=jel.journal_entry_id
            WHERE a.is_active=TRUE AND a.account_type IN ('ASSET','LIABILITY','EQUITY')
            GROUP BY a.id,a.account_code,a.account_name,a.account_type
            ORDER BY FIELD(a.account_type,'ASSET','LIABILITY','EQUITY'),a.account_code
            """;
        DefaultTableModel raw = readTable(sql, new String[]{"Code", "Account", "Type", "Debits", "Credits"});
        DefaultTableModel display = newTableModel(new String[]{"Code", "Account", "Category", "Balance"});
        BigDecimal assets = BigDecimal.ZERO, liabilities = BigDecimal.ZERO, equity = BigDecimal.ZERO;
        for (int i = 0; i < raw.getRowCount(); i++) {
            BigDecimal dr = new BigDecimal(raw.getValueAt(i, 3).toString());
            BigDecimal cr = new BigDecimal(raw.getValueAt(i, 4).toString());
            String type = raw.getValueAt(i, 2).toString();
            BigDecimal bal = type.equals("ASSET") ? dr.subtract(cr) : cr.subtract(dr);
            if (type.equals("ASSET")) assets = assets.add(bal);
            else if (type.equals("LIABILITY")) liabilities = liabilities.add(bal);
            else equity = equity.add(bal);
            if (bal.signum() != 0) display.addRow(new Object[]{
                    raw.getValueAt(i, 0), raw.getValueAt(i, 1), type, Money.currency(bal)});
        }
        // Current-period retained earnings = revenue less expenses, added to equity for presentation.
        BigDecimal netIncome = ReportService.calculateNetIncome();
        equity = equity.add(netIncome);
        display.addRow(new Object[]{"", "Current-period net income", "EQUITY", Money.currency(netIncome)});
        display.addRow(new Object[]{"", "TOTAL ASSETS", "", Money.currency(assets)});
        display.addRow(new Object[]{"", "TOTAL LIABILITIES + EQUITY", "", Money.currency(liabilities.add(equity))});
        display.addRow(new Object[]{"", "BALANCE DIFFERENCE", "", Money.currency(assets.subtract(liabilities.add(equity)).abs())});
        JTable table = Tables.create(display);
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        return p;
    }
}
