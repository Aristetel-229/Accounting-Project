package com.tala.ui.pages;

import com.tala.service.LedgerService;
import com.tala.service.LedgerService.AccountLedger;
import com.tala.service.LedgerService.Posting;
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * General Ledger: a separate ledger for every account that has been used, like the T-account pages
 * of a real ledger book. Each one lists that account's transactions with a running balance and ends
 * with a "Total / ending balance" row. Accounts are shown two to a row.
 */
public class GeneralLedgerPage extends BasePage {
    private static final String ALL = "All accounts";
    private static final String[] COLUMNS = {"Date", "Description", "Ref", "Debit", "Credit", "Balance"};
    /** The description column never shrinks below this; the other columns are sized to their content. */
    private static final int DESC_MIN = 90;
    private static final int CELL_PAD = 11;      // left + right padding of a cell, plus a little slack
    private static final int CARD_GAP = 10;
    private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font SMALL_BOLD = new Font("SansSerif", Font.BOLD, 12);

    public GeneralLedgerPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "General Ledger"; }

    @Override protected String hint() {
        return "One ledger per account you have used: every transaction on that account, its running balance, total and ending balance.";
    }

    @Override protected JComponent build() {
        JPanel p = pagePanel();

        List<AccountLedger> ledgers = new ArrayList<>();
        try {
            ledgers = LedgerService.load();
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            frame.setStatus(" Database error");
            JOptionPane.showMessageDialog(frame,
                    "Unable to load data. Check that MySQL is running and database/tala.sql has been executed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());

        if (ledgers.isEmpty()) {
            JLabel empty = new JLabel("No posted entries yet. Each account you use will get its own ledger here.", SwingConstants.CENTER);
            empty.setFont(Theme.BASE);
            empty.setForeground(Theme.MUTED);
            empty.setOpaque(true);
            empty.setBackground(Color.WHITE);
            empty.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            p.add(empty, BorderLayout.CENTER);
            p.add(actionBar(refresh), BorderLayout.SOUTH);
            return p;
        }

        CardsPanel cards = new CardsPanel();
        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        JComboBox<String> filter = new JComboBox<>();
        filter.addItem(ALL);
        for (AccountLedger l : ledgers) filter.addItem(label(l));
        final List<AccountLedger> all = ledgers;
        Runnable render = () -> {
            String chosen = (String) filter.getSelectedItem();
            List<AccountLedger> shown = new ArrayList<>();
            for (AccountLedger l : all) if (ALL.equals(chosen) || label(l).equals(chosen)) shown.add(l);
            fill(cards, shown);
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        };
        filter.addActionListener(e -> render.run());
        render.run();

        JLabel filterLabel = new JLabel("Account:");
        filterLabel.setFont(Theme.BASE);
        filterLabel.setForeground(Theme.TEXT);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.add(filterLabel);
        bar.add(filter);
        bar.add(refresh);

        p.add(bar, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private static String label(AccountLedger l) {
        return l.code + " · " + l.name;
    }

    /** Lays the ledgers out two to a row (a last odd one sits alone on the left). */
    static void fill(CardsPanel cards, List<AccountLedger> ledgers) {
        cards.removeAll();
        // One set of column widths for every card, so the ledgers line up like pages of the same book.
        int[] widths = fixedWidths(ledgers);
        int card = DESC_MIN + 2;                                  // + the card border
        for (int c = 0; c < widths.length; c++) if (c != 1) card += widths[c];
        // two cards + the gap between them + the panel's side padding; narrower windows scroll sideways
        cards.minWidth = card * 2 + CARD_GAP + 16;
        for (int i = 0; i < ledgers.size(); i += 2) {
            JPanel row = new JPanel(new GridLayout(1, 2, CARD_GAP, 0));   // both cards in a row share one height
            row.setOpaque(false);
            row.add(top(ledgerCard(ledgers.get(i), widths)));
            row.add(i + 1 < ledgers.size() ? top(ledgerCard(ledgers.get(i + 1), widths)) : emptyCell());
            cards.add(row);
            cards.add(Box.createVerticalStrut(12));
        }
        cards.revalidate();
        cards.repaint();
    }

    private static JComponent top(JComponent card) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setOpaque(false);
        cell.add(card, BorderLayout.NORTH);                          // keep the card at the top of its cell
        return cell;
    }

    private static JComponent emptyCell() {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        return cell;
    }

    /** One account's ledger: title block, transactions with running balance, and the total / ending balance row. */
    static JComponent ledgerCard(AccountLedger l, int[] fixed) {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (int i = 0; i < l.postings.size(); i++) model.addRow(rowFor(l, i));
        model.addRow(totalRow(l));

        JTable table = Tables.create(model);
        table.setRowSelectionAllowed(false);
        table.setRowHeight(26);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < COLUMNS.length; i++) {
            var col = table.getColumnModel().getColumn(i);
            if (i == 1) {                                   // description takes whatever space is left
                col.setMinWidth(DESC_MIN);
                col.setPreferredWidth(Math.max(DESC_MIN, fixed[1]));
            } else {                                        // numbers, dates and references always fit
                col.setMinWidth(fixed[i]);
                col.setMaxWidth(fixed[i]);
                col.setPreferredWidth(fixed[i]);
            }
        }
        // Compact header: smaller font, amounts right-aligned over their columns.
        TableCellRenderer headerBase = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer((t, v, sel, foc, r, col) -> {
            JLabel h = (JLabel) headerBase.getTableCellRendererComponent(t, v, sel, foc, r, col);
            h.setFont(SMALL_BOLD);
            h.setBorder(new EmptyBorder(8, 5, 8, 5));
            h.setHorizontalAlignment(col >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return h;
        });
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));
        // Compact cells so six columns fit in half the page width.
        TableCellRenderer base = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (t, v, sel, foc, r, col) -> {
            JLabel cell = (JLabel) base.getTableCellRendererComponent(t, v, sel, foc, r, col);
            boolean total = r == t.getRowCount() - 1;
            cell.setFont(total ? SMALL_BOLD : SMALL);
            cell.setBorder(new EmptyBorder(0, 5, 0, 5));
            String text = v == null ? "" : v.toString();
            if (total && col == 1) {                        // shorten the label if the column is narrow
                int room = t.getColumnModel().getColumn(1).getWidth() - CELL_PAD;
                FontMetrics fm = cell.getFontMetrics(SMALL_BOLD);
                text = fm.stringWidth(text) <= room ? text : fm.stringWidth("Total / ending") <= room ? "Total / ending" : "Total";
                cell.setText(text);
            }
            if (col == 1) cell.setToolTipText(text.isEmpty() ? null : v.toString());
            return cell;
        });

        JLabel name = new JLabel(l.name);
        name.setFont(Theme.BOLD.deriveFont(16f));
        name.setForeground(Theme.GREEN_DARK);
        JLabel sub = new JLabel("Account " + l.code + " · " + capitalize(l.type) + " · Normal balance: "
                + (l.debitNormal ? "Debit" : "Credit"));
        sub.setFont(Theme.HINT);
        sub.setForeground(Theme.MUTED);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(new EmptyBorder(10, 12, 10, 12));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(name);
        head.add(sub);

        JPanel grid = new JPanel(new BorderLayout());
        grid.add(table.getTableHeader(), BorderLayout.NORTH);
        grid.add(table, BorderLayout.CENTER);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        card.add(head, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /**
     * Width every column needs to show its widest cell (and header) without clipping, across all the
     * ledgers shown. Index 1 (description) is only the width it would like, capped at 260.
     */
    private static int[] fixedWidths(List<AccountLedger> ledgers) {
        JLabel probe = new JLabel();
        FontMetrics fm = probe.getFontMetrics(SMALL), fb = probe.getFontMetrics(SMALL_BOLD);
        int[] w = new int[COLUMNS.length];
        for (int c = 0; c < COLUMNS.length; c++) w[c] = fb.stringWidth(COLUMNS[c]) + CELL_PAD;
        for (AccountLedger l : ledgers) {
            for (int i = 0; i < l.postings.size(); i++) {
                Object[] row = rowFor(l, i);
                for (int c = 0; c < COLUMNS.length; c++) w[c] = Math.max(w[c], fm.stringWidth(String.valueOf(row[c])) + CELL_PAD);
            }
            Object[] total = totalRow(l);                         // bold; its label shortens itself, so skip column 1
            for (int c = 0; c < COLUMNS.length; c++) {
                if (c != 1) w[c] = Math.max(w[c], fb.stringWidth(String.valueOf(total[c])) + CELL_PAD);
            }
        }
        w[1] = Math.min(w[1], 260);
        return w;
    }

    private static Object[] rowFor(AccountLedger l, int i) {
        Posting po = l.postings.get(i);
        return new Object[]{
                shortDate(po.date()),
                po.description(),
                po.reference() == null ? "" : po.reference(),
                po.debit().signum() > 0 ? Money.currency(po.debit()) : "",
                po.credit().signum() > 0 ? Money.currency(po.credit()) : "",
                balance(l.running.get(i))};
    }

    private static Object[] totalRow(AccountLedger l) {
        return new Object[]{"", "Total / ending balance", "",
                Money.currency(l.totalDebit), Money.currency(l.totalCredit), balance(l.endingNet())};
    }

    /** "Oct. 28" */
    private static String shortDate(LocalDate d) {
        return d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ". " + d.getDayOfMonth();
    }

    /** A running or ending balance on its real side: "₱44600.00 DR" or "₱500.00 CR". */
    static String balance(BigDecimal net) {
        if (net.signum() == 0) return Money.currency(BigDecimal.ZERO);
        return Money.currency(net.abs()) + (net.signum() > 0 ? " DR" : " CR");
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase(Locale.ENGLISH);
    }

    /** Vertical stack of rows that fills the viewport width, but scrolls sideways when narrower than {@code minWidth}. */
    static final class CardsPanel extends JPanel implements Scrollable {
        int minWidth = 0;

        CardsPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.BACKGROUND);
            setBorder(new EmptyBorder(10, 8, 0, 8));
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(r.height - 40, 24); }
        @Override public boolean getScrollableTracksViewportWidth() {
            Container parent = getParent();
            return parent == null || parent.getWidth() >= minWidth;
        }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, minWidth), d.height);
        }
    }
}
