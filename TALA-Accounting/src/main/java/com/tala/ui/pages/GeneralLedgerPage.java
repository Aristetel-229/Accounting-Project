package com.tala.ui.pages;

import com.tala.ui.BasePage;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.ui.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class GeneralLedgerPage extends BasePage {
    public GeneralLedgerPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Ledger"; }

    @Override protected String hint() { return "Every posted debit and credit. Click a column header to sort."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        String sql = """
            SELECT je.entry_date, je.reference_no, a.account_code, a.account_name,
                   jel.debit, jel.credit, je.description
            FROM journal_entry_lines jel
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            ORDER BY je.entry_date, je.id, jel.id
            """;
        DefaultTableModel model = readTable(sql,
                new String[]{"Date", "Reference", "Code", "Account", "Debit", "Credit", "Description"});
        JTable table = Tables.create(model);
        table.setAutoCreateRowSorter(true);
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        p.add(actionBar(refresh), BorderLayout.SOUTH);
        return p;
    }
}
