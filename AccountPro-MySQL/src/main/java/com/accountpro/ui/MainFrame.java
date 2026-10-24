package com.accountpro.ui;

import com.accountpro.ui.components.NavButton;
import com.accountpro.ui.pages.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/** Application window: sidebar navigation, header, content area and status bar. */
public class MainFrame extends JFrame {
    private final JPanel content = new JPanel(new BorderLayout(12, 12));
    private final JLabel status = new JLabel(" Ready");
    private final Map<String, NavButton> navButtons = new HashMap<>();

    public MainFrame() {
        setTitle("AccountPro | Desktop Accounting System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        DashboardPage dashboard = new DashboardPage(this);

        add(buildSidebar(dashboard), BorderLayout.WEST);
        add(buildRightPanel(), BorderLayout.CENTER);
        dashboard.show();
    }

    private JPanel buildSidebar(DashboardPage dashboard) {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.NAVY);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(18, 12, 18, 12));

        JLabel brand = new JLabel("  AccountPro");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        brand.setBorder(new EmptyBorder(4, 0, 24, 0));
        sidebar.add(brand);

        addNav(sidebar, "Dashboard", "DashboardPage", "See your totals at a glance",
                dashboard::show);
        addNav(sidebar, "Chart of Accounts", "AccountsPage", "The list of all accounts",
                () -> new AccountsPage(this).show());
        addNav(sidebar, "Journal Entries", "JournalPage", "Record a transaction",
                () -> new JournalPage(this).show());
        addNav(sidebar, "General Ledger", "LedgerPage", "Every posted debit and credit",
                () -> new LedgerPage(this).show());
        addNav(sidebar, "Trial Balance", "TrialBalancePage", "Check that debits equal credits",
                () -> new TrialBalancePage(this).show());
        addNav(sidebar, "Income Statement", "IncomeStatementPage", "Revenue minus expenses",
                () -> new IncomeStatementPage(this).show());
        addNav(sidebar, "Balance Sheet", "BalanceSheetPage", "Assets = Liabilities + Equity",
                () -> new BalanceSheetPage(this).show());
        addNav(sidebar, "Cash Flow", "CashFlowPage", "Cash and bank movements",
                () -> new CashFlowPage(this).show());
        addNav(sidebar, "Invoices & Payments", "InvoicesPage", "Bill customers and record payments",
                () -> new InvoicesPage(this).show());
        addNav(sidebar, "Settings", "SettingsPage", "Reset data for a fresh start",
                () -> new SettingsPage(this, dashboard).show());
        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("Student Edition");
        footer.setForeground(new Color(210, 220, 235));
        footer.setBorder(new EmptyBorder(10, 4, 0, 4));
        sidebar.add(footer);
        return sidebar;
    }

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BACKGROUND);

        JLabel top = new JLabel("  ACCOUNTING WORKSPACE");
        top.setFont(new Font("SansSerif", Font.BOLD, 13));
        top.setBorder(new EmptyBorder(15, 12, 15, 12));
        top.setOpaque(true);
        top.setBackground(Color.WHITE);
        right.add(top, BorderLayout.NORTH);

        content.setBorder(new EmptyBorder(18, 18, 18, 18));
        content.setBackground(Theme.BACKGROUND);
        right.add(content, BorderLayout.CENTER);

        status.setBorder(new EmptyBorder(8, 12, 8, 12));
        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        right.add(status, BorderLayout.SOUTH);
        return right;
    }

    private void addNav(JPanel sidebar, String label, String key, String tooltip, Runnable action) {
        NavButton button = new NavButton(label);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> action.run());
        navButtons.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(4));
    }

    /** Replaces the current page content (called by {@link BasePage#show()}). */
    public void setPage(String navKey, String title, String hint, JComponent component) {
        navButtons.forEach((key, button) -> button.setActive(key.equals(navKey)));
        content.removeAll();

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(title);
        label.setFont(Theme.TITLE);
        label.setForeground(Theme.NAVY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label);
        if (!hint.isBlank()) {
            JLabel sub = new JLabel(hint);
            sub.setFont(Theme.HINT);
            sub.setForeground(Theme.MUTED);
            sub.setBorder(new EmptyBorder(2, 0, 0, 0));
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            heading.add(sub);
        }
        content.add(heading, BorderLayout.NORTH);
        content.add(component, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    public void setStatus(String text) {
        status.setText(text);
    }
}
