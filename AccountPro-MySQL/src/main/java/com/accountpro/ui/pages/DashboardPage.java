package com.accountpro.ui.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.accountpro.db.DB;
import com.accountpro.service.ReportService;
import com.accountpro.ui.BasePage;
import com.accountpro.ui.MainFrame;
import com.accountpro.ui.ResetDataAction;
import com.accountpro.ui.Theme;
import com.accountpro.ui.components.Cards;
import com.accountpro.ui.components.RoundedButton;
import com.accountpro.util.Money;

public class DashboardPage extends BasePage {
    private final JLabel assetsValue = new JLabel("₱0.00");
    private final JLabel liabilitiesValue = new JLabel("₱0.00");
    private final JLabel revenueValue = new JLabel("₱0.00");
    private final JLabel expensesValue = new JLabel("₱0.00");

    public DashboardPage(MainFrame frame) {
        super(frame);
    }

    @Override protected String title() { return "Dashboard"; }

    @Override public void show() {
        super.show();
        refresh();
    }

    @Override protected String hint() { return "Your business at a glance. Hover a card for details."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.setOpaque(false);
        cards.add(Cards.metric("ASSET BALANCES", "What the business owns", assetsValue, Theme.ASSET));
        cards.add(Cards.metric("LIABILITY BALANCES", "What the business owes", liabilitiesValue, Theme.LIABILITY));
        cards.add(Cards.metric("TOTAL REVENUE", "Money earned", revenueValue, Theme.REVENUE));
        cards.add(Cards.metric("TOTAL EXPENSES", "Money spent", expensesValue, Theme.EXPENSE));

        JButton refresh = new RoundedButton("Refresh Dashboard", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> refresh());
        JButton resetData = new RoundedButton("Reset System Data", RoundedButton.Kind.DANGER);
        resetData.addActionListener(e -> ResetDataAction.confirmAndRun(frame, this::show));

        JPanel dashboardActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        dashboardActions.setOpaque(false);
        dashboardActions.add(refresh);
        dashboardActions.add(resetData);

        JPanel lower = new JPanel(new BorderLayout());
        lower.setOpaque(false);
        JTextArea note = new JTextArea(
                "Getting started\n\n" +
                "  1.  Open Journal Entries and record a transaction.\n" +
                "  2.  Check the General Ledger to see it posted.\n" +
                "  3.  View the Trial Balance, Income Statement and Balance Sheet.\n\n" +
                "Every entry needs equal debit and credit amounts.\n" );
        note.setEditable(false);
        note.setFont(new Font("SansSerif", Font.PLAIN, 14));
        note.setBackground(Color.WHITE);
        note.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(18, 18, 18, 18)));
        note.setForeground(Theme.TEXT);
        lower.add(note, BorderLayout.CENTER);
        lower.add(dashboardActions, BorderLayout.SOUTH);

        p.add(cards, BorderLayout.NORTH);
        p.add(lower, BorderLayout.CENTER);
        return p;
    }

    private void refresh() {
        try (Connection c = DB.getConnection()) {
            assetsValue.setText(Money.currency(ReportService.accountTypeBalance(c, "ASSET")));
            liabilitiesValue.setText(Money.currency(ReportService.accountTypeBalance(c, "LIABILITY")));
            revenueValue.setText(Money.currency(ReportService.accountTypeBalance(c, "REVENUE")));
            expensesValue.setText(Money.currency(ReportService.accountTypeBalance(c, "EXPENSE")));
            frame.setStatus(" Connected to MySQL • " + LocalDate.now());
        } catch (SQLException ex) {
            frame.setStatus(" MySQL connection failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to MySQL.\nRun database/accountpro.sql and check DB.java settings.\n\n" + ex.getMessage(),
                    "Database Connection", JOptionPane.ERROR_MESSAGE);
        }
    }
}
