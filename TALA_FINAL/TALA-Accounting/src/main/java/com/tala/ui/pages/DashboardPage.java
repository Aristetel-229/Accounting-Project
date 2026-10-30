package com.tala.ui.pages;

import com.tala.db.DB;
import com.tala.service.ReportService;
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.ResetDataAction;
import com.tala.ui.Theme;
import com.tala.ui.components.Cards;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class DashboardPage extends BasePage {
    private static final Color AMBER = new Color(176, 125, 35);

    public DashboardPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Dashboard"; }
    @Override protected String hint() { return "Your books at a glance, based on all posted entries."; }

    @Override protected JComponent build() {
        JLabel assets = new JLabel("₱0.00"), liabilities = new JLabel("₱0.00"), revenue = new JLabel("₱0.00");
        JLabel expenses = new JLabel("₱0.00"), netIncome = new JLabel("₱0.00"), entries = new JLabel("0");
        Color netColor = Theme.GREEN_DARK;

        BigDecimal rev = BigDecimal.ZERO, exp = BigDecimal.ZERO;
        try (Connection c = DB.getConnection()) {
            assets.setText(Money.currency(ReportService.accountTypeBalance(c, "ASSET")));
            liabilities.setText(Money.currency(ReportService.accountTypeBalance(c, "LIABILITY")));
            rev = ReportService.accountTypeBalance(c, "REVENUE");
            exp = ReportService.accountTypeBalance(c, "EXPENSE");
            revenue.setText(Money.currency(rev));
            expenses.setText(Money.currency(exp));
            entries.setText(String.valueOf(ReportService.countPostedEntries(c)));
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            frame.setStatus(" MySQL connection failed");
            JOptionPane.showMessageDialog(frame,
                    "Could not connect to MySQL.\nRun database/tala.sql and check the DB.java settings.\n\n" + ex.getMessage(),
                    "Database Connection", JOptionPane.ERROR_MESSAGE);
        }
        BigDecimal net = rev.subtract(exp);
        netIncome.setText((net.signum() < 0 ? "-" : "") + Money.currency(net.abs()));
        if (net.signum() < 0) netColor = Theme.DANGER;

        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        cards.add(Cards.metric("TOTAL ASSETS", "What the business owns", assets, Theme.PRIMARY));
        cards.add(Cards.metric("TOTAL LIABILITIES", "What the business owes", liabilities, AMBER));
        cards.add(Cards.metric("TOTAL REVENUE", "Money earned", revenue, new Color(22, 163, 74)));
        cards.add(Cards.metric("TOTAL EXPENSES", "Money spent", expenses, Theme.DANGER));
        cards.add(Cards.metric("NET INCOME", "Revenue minus expenses", netIncome, netColor));
        cards.add(Cards.metric("POSTED ENTRIES", "Journal entries recorded", entries, Theme.GREEN_ACTIVE));

        String sql = """
            SELECT je.entry_date, je.reference_no, je.description, SUM(jel.debit) AS amount
            FROM journal_entries je
            JOIN journal_entry_lines jel ON jel.journal_entry_id = je.id
            WHERE je.status = 'POSTED'
            GROUP BY je.id, je.entry_date, je.reference_no, je.description
            ORDER BY je.entry_date DESC, je.id DESC
            LIMIT 8
            """;
        DefaultTableModel model = readTable(sql, new String[]{"Date", "Reference", "Description", "Amount"});
        JTable table = Tables.create(model);
        JScrollPane scroll = Tables.scroll(table);
        JLabel recent = new JLabel("Recent entries");
        recent.setFont(Theme.BOLD);
        recent.setForeground(Theme.GREEN_DARK);
        recent.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));
        JPanel lower = new JPanel(new BorderLayout());
        lower.setOpaque(false);
        lower.add(recent, BorderLayout.NORTH);
        lower.add(scroll, BorderLayout.CENTER);

        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        JButton reset = new RoundedButton("Reset Data", RoundedButton.Kind.DANGER);
        reset.addActionListener(e -> ResetDataAction.confirmAndRun(frame, this::show));

        JPanel p = pagePanel();
        p.add(cards, BorderLayout.NORTH);
        p.add(lower, BorderLayout.CENTER);
        p.add(actionBar(refresh, reset), BorderLayout.SOUTH);
        return p;
    }
}
