package com.accountpro.ui.pages;

import com.accountpro.ui.BasePage;
import com.accountpro.ui.components.Tables;
import com.accountpro.ui.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CashFlowPage extends BasePage {
    public CashFlowPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Cash Flow — Cash and Bank Movements"; }

    @Override protected String hint() { return "Money moving in and out of your cash and bank accounts."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        String sql = """
            SELECT je.entry_date, je.reference_no, je.description, a.account_name,
                   jel.debit, jel.credit
            FROM journal_entry_lines jel
            JOIN journal_entries je ON je.id=jel.journal_entry_id
            JOIN accounts a ON a.id=jel.account_id
            WHERE je.status='POSTED' AND a.account_type='ASSET'
              AND (a.account_name LIKE '%Cash%' OR a.account_name LIKE '%Bank%')
            ORDER BY je.entry_date,je.id,jel.id
            """;
        DefaultTableModel model = readTable(sql,
                new String[]{"Date", "Reference", "Description", "Cash/Bank Account", "Debit Inflow", "Credit Outflow"});
        JTable table = Tables.create(model);
        table.setAutoCreateRowSorter(true);
        JLabel note = new JLabel("  Cash/bank movements from posted entries. Classifications are not yet separated into operating, investing, and financing activities.");
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }
}
