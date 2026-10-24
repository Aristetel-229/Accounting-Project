package com.accountpro.ui.pages;

import com.accountpro.ui.BasePage;
import com.accountpro.ui.components.RoundedButton;
import com.accountpro.ui.components.Tables;
import com.accountpro.ui.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AccountsPage extends BasePage {
    public AccountsPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Chart of Accounts"; }

    @Override protected String hint() { return "All the accounts your business can use, grouped by type."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        String[] columns = {"Code", "Account Name", "Type", "Normal Balance", "Active"};
        DefaultTableModel model = readTable(
                "SELECT account_code, account_name, account_type, normal_balance, is_active FROM accounts ORDER BY account_code",
                columns);
        JTable table = Tables.create(model);
        p.add(Tables.scroll(table), BorderLayout.CENTER);
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        p.add(actionBar(refresh), BorderLayout.SOUTH);
        return p;
    }
}
