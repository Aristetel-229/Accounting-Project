package com.tala.ui;

import com.tala.ui.components.NavButton;
import com.tala.ui.pages.*;

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
        setTitle("TALA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildRightPanel(), BorderLayout.CENTER);
        new DashboardPage(this).show();
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.GREEN_DARK);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(18, 12, 18, 12));

        JLabel brand = new JLabel("  TALA");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        brand.setBorder(new EmptyBorder(4, 0, 24, 0));
        sidebar.add(brand);

        addNav(sidebar, "Dashboard", "DashboardPage", "Totals at a glance",
                () -> new DashboardPage(this).show());
        addNav(sidebar, "Journal Entry", "JournalEntryPage", "Record a transaction",
                () -> new JournalEntryPage(this).show());
        addNav(sidebar, "Chart of Accounts", "ChartOfAccountsPage", "The list of all accounts",
                () -> new ChartOfAccountsPage(this).show());
        addNav(sidebar, "Ledger", "GeneralLedgerPage", "Every posted debit and credit",
                () -> new GeneralLedgerPage(this).show());
        addNav(sidebar, "12 Columnar", "TwelveColumnWorksheetPage", "Trial balances, adjustments, income statement and balance sheet",
                () -> new TwelveColumnWorksheetPage(this).show());
        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("Student Edition");
        footer.setForeground(Theme.SIDEBAR_TEXT);
        footer.setBorder(new EmptyBorder(10, 4, 0, 4));
        sidebar.add(footer);
        return sidebar;
    }

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BACKGROUND);

        JLabel top = new JLabel("  TALA");
        top.setFont(new Font("SansSerif", Font.BOLD, 13));
        top.setBorder(new EmptyBorder(15, 12, 15, 12));
        top.setOpaque(true);
        top.setBackground(Color.WHITE);
        top.setForeground(Theme.GREEN_DARK);
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
        label.setForeground(Theme.GREEN_DARK);
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

