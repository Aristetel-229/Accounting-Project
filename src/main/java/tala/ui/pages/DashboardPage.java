package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.DashboardSummary;
import tala.model.JournalEntry;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Card;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.RoundedPanel;
import tala.ui.components.TalaTable;
import tala.ui.theme.Hover;
import tala.ui.theme.Theme;
import tala.util.DateUtil;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DashboardPage
 * ------------------------------------------------------------
 * The books at a glance (light design, "ledger-style summary"):
 *
 *   +-- Income statement ---------------------------+  +-- Posted entries --+
 *   |  [Revenue]  -  [Expenses]  =  [Net income]     |  |  (ticket)          |
 *   +-----------------------------------------------+  |   4                |
 *   +-- Balance position (a T-account) --------------+  |  JE-7322   Nov 3   |
 *   |  Total assets  Dr  |  Cr  Total liabilities     |  |  [ New entry ]     |
 *   +-----------------------------------------------+  +--------------------+
 *   +-- Recent entries (table) ----------------------------------------------+
 *
 * Hover effects: the three summary cards rise and their outline glows,
 * each term of the equation lifts on its own, the ticket's reference rows
 * light up, and table rows turn lilac.
 */
public class DashboardPage extends BasePage {

    private static final Color KICKER = new Color(0x7c6f99);
    private static final Color EM = new Color(0x8a7fa6);
    private static final Color DEEP = new Color(0x1e1535);

    private final PageHeader header;

    // income statement
    private final JLabel periodPill = pill("", new Color(0xf1ebff), Theme.PURPLE);
    private final Term revenueTerm = new Term(Icons.TREND_UP, new Color(0xdcf5e8), new Color(0x17663d), "Revenue", "Money earned", false);
    private final Term expensesTerm = new Term(Icons.TREND_DOWN, new Color(0xfde3e1), new Color(0xb42318), "Expenses", "Money spent", false);
    private final Term netTerm = new Term(Icons.PESO, new Color(255, 255, 255, 38), Theme.YELLOW, "Net income", "Break-even so far", true);

    // balance position
    private final JLabel balancedPill = pill("Balanced", new Color(0xdcf5e8), new Color(0x17663d));
    private final TAccount tAccount = new TAccount();

    // posted entries ticket
    private final Ticket ticket = new Ticket();

    // recent entries
    private final ReadOnlyTableModel recentModel = new ReadOnlyTableModel("Date", "Reference", "Description", "Amount (" + Money.PESO_SIGN.trim() + ")");
    private final TalaTable recentTable = new TalaTable(recentModel);
    private final JLabel recentEmpty = new JLabel("No entries yet. Record your first transaction in Journal Entry.");

    public DashboardPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);

        // ---------- page header ----------
        header = new PageHeader(Icons.DASHBOARD, "Dashboard", "Your books at a glance, based on all posted entries.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_INK), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        AppButton reset = new AppButton("Reset data", AppButton.Style.DANGER);
        reset.addActionListener(e -> resetData());
        header.addAction(refresh);
        header.addAction(reset);
        addSection(header);

        // ---------- income statement + balance position | posted entries ticket ----------
        JPanel summary = new JPanel(new GridBagLayout());
        summary.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 6, 8);
        summary.add(createIncomeCard(), c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 8);
        summary.add(createBalanceCard(), c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 2;
        c.weightx = 0;
        c.insets = new Insets(0, 6, 0, 0);
        summary.add(ticket, c);
        addSection(summary);

        // ---------- recent entries ----------
        addSection(createRecentCard());
        finishSections();
    }

    // =================================================================
    //  Building the cards
    // =================================================================

    private RoundedPanel createIncomeCard() {
        RoundedPanel card = summaryCard();
        card.add(headRow("Income statement", periodPill), BorderLayout.NORTH);

        JPanel eq = new JPanel(new GridBagLayout());
        eq.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0; c.weightx = 1;    eq.add(revenueTerm, c);
        c.gridx = 1; c.weightx = 0;    eq.add(operator("−"), c);
        c.gridx = 2; c.weightx = 1;    eq.add(expensesTerm, c);
        c.gridx = 3; c.weightx = 0;    eq.add(operator("="), c);
        c.gridx = 4; c.weightx = 1.25; eq.add(netTerm, c);
        eq.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        card.add(eq, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel createBalanceCard() {
        RoundedPanel card = summaryCard();
        card.add(headRow("Balance position", balancedPill), BorderLayout.NORTH);
        tAccount.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        card.add(tAccount, BorderLayout.CENTER);
        return card;
    }

    private Card createRecentCard() {
        Card card = new Card("Recent entries", "The latest transactions you posted");
        JButton viewAll = new LinkButton("View all in General Journal");
        viewAll.addActionListener(e -> mainFrame.showPage(MainFrame.GENERAL_JOURNAL));
        card.addToHeader(viewAll);

        recentTable.setRightAligned(3);
        recentTable.setPillColumns(1);
        recentTable.setColumnWidths(200, 220, 0, 180);

        recentEmpty.setFont(Theme.regular(14));
        recentEmpty.setForeground(Theme.MUTED);
        recentEmpty.setHorizontalAlignment(JLabel.CENTER);
        recentEmpty.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        body.add(recentTable.inPanel(), BorderLayout.CENTER);
        body.add(recentEmpty, BorderLayout.SOUTH);
        card.setBody(body);
        return card;
    }

    /** A white summary card that rises on hover */
    private static RoundedPanel summaryCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorderColor(new Color(0xe8e2f4)).setBorderWidth(1.2f);
        card.setLift(true);
        card.setBorder(BorderFactory.createEmptyBorder(3 + 14, 3 + 18, 9 + 16, 3 + 18));
        return card;
    }

    /** "INCOME STATEMENT ........ [Nov. 2026]" */
    private static JComponent headRow(String kicker, JLabel pill) {
        JLabel k = new JLabel(kicker.toUpperCase());
        k.setFont(Theme.spaced(Theme.semibold(11), 0.12f));
        k.setForeground(KICKER);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(k, BorderLayout.WEST);
        row.add(pill, BorderLayout.EAST);
        return row;
    }

    private static JLabel operator(String text) {
        JLabel op = new JLabel(text, JLabel.CENTER);
        op.setFont(Theme.semibold(22));
        op.setForeground(new Color(0xc4b5fd));
        op.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        return op;
    }

    /** A small rounded label like [Nov. 2026] or [Balanced ✓] */
    private static JLabel pill(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setOpaque(false);
        label.setBackground(bg);
        label.setForeground(fg);
        label.setFont(Theme.semibold(11));
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        label.setHorizontalTextPosition(JLabel.LEFT);
        label.setIconTextGap(4);
        return label;
    }

    // =================================================================
    //  Loading data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            List<Account> accounts = accountDAO.getAllAccounts();
            List<JournalEntry> entries = journalDAO.getAllEntries();
            DashboardSummary s = accounting.buildSummary(accounts, entries);
            List<JournalEntry> sorted = accounting.sortByDate(entries);

            // header line + the month pill
            if (sorted.isEmpty()) {
                header.setDescription("Your books at a glance, based on all posted entries. No entries posted yet.");
                periodPill.setText(DateUtil.toMonth(java.time.LocalDate.now()));
            } else {
                JournalEntry last = sorted.get(sorted.size() - 1);
                header.setDescription("Your books at a glance, based on all posted entries. Last entry posted "
                        + DateUtil.toLong(last.getDate()) + ".");
                periodPill.setText(DateUtil.toMonth(last.getDate()));
            }

            // income statement
            revenueTerm.setAmount(Money.peso(s.totalRevenue));
            expensesTerm.setAmount(Money.peso(s.totalExpenses));
            netTerm.setTitle(s.netIncome.signum() < 0 ? "Net loss" : "Net income");
            netTerm.setAmount(Money.peso(s.netIncome));
            netTerm.setNote(s.netIncome.signum() > 0 ? "Profit so far" : s.netIncome.signum() < 0
                    ? "Expenses are higher" : "Break-even so far");
            revenueTerm.setToolTipText("<html><b>Total revenue</b><br>All revenue accounts: credits - debits.</html>");
            expensesTerm.setToolTipText("<html><b>Total expenses</b><br>All expense accounts: debits - credits.</html>");
            netTerm.setToolTipText("<html><b>" + (s.netIncome.signum() < 0 ? "Net loss" : "Net income") + "</b><br>"
                    + "Revenue - Expenses<br>= " + Money.peso(s.totalRevenue) + " - " + Money.peso(s.totalExpenses)
                    + "<br>= " + Money.peso(s.netIncome) + "</html>");

            // balance position
            tAccount.setValues(s.totalAssets, s.totalLiabilities, s.totalEquity);
            tAccount.setToolTipText("<html><b>Assets = Liabilities + Owner's equity</b><br>"
                    + Money.peso(s.totalAssets) + " = " + Money.peso(s.totalLiabilities) + " + "
                    + Money.peso(s.totalEquity) + "<br>Contra assets (Accumulated Depreciation) reduce assets.</html>");
            if (s.isEquationBalanced()) {
                balancedPill.setText("Balanced");
                balancedPill.setIcon(Icons.get(Icons.CHECK, 12, new Color(0x17663d)));
                balancedPill.setBackground(new Color(0xdcf5e8));
                balancedPill.setForeground(new Color(0x17663d));
            } else {
                balancedPill.setText("Not balanced");
                balancedPill.setIcon(null);
                balancedPill.setBackground(Theme.RED_BG);
                balancedPill.setForeground(Theme.RED);
            }

            // ticket: count + the latest three references
            int adjusting = 0;
            for (JournalEntry e : entries) {
                if (e.isAdjusting()) {
                    adjusting++;
                }
            }
            List<String[]> latest = new ArrayList<>();
            for (int i = sorted.size() - 1; i >= 0 && latest.size() < 3; i--) {
                JournalEntry e = sorted.get(i);
                latest.add(new String[] {e.getReferenceNo(), DateUtil.toShort(e.getDate())});
            }
            ticket.setValues(s.entryCount, latest);
            ticket.setToolTipText("<html><b>Posted entries</b><br>" + (s.entryCount - adjusting) + " regular + "
                    + adjusting + " adjusting " + (adjusting == 1 ? "entry" : "entries") + "</html>");

            // recent entries: newest 5
            recentModel.clear();
            for (int i = sorted.size() - 1; i >= 0 && i >= sorted.size() - 5; i--) {
                JournalEntry e = sorted.get(i);
                recentModel.addRow(new Object[] {
                        DateUtil.toLong(e.getDate()), e.getReferenceNo(), e.getDescription(),
                        Money.format(e.getTotalDebit())});
            }
            recentEmpty.setVisible(sorted.isEmpty());

            mainFrame.setDatabaseStatus(true);
            revalidate();
            repaint();
        } catch (SQLException e) {
            mainFrame.setDatabaseStatus(false);
            showError("Could not load the dashboard from MySQL.", e);
        }
    }

    /** Reset data: deletes every journal entry after the user confirms */
    private void resetData() {
        boolean yes = confirm("Reset data",
                "Delete ALL journal entries and adjustments?\n"
                + "Your chart of accounts stays. This cannot be undone.",
                "Delete all entries", "Keep my data");
        if (!yes) {
            return;
        }
        try {
            journalDAO.deleteAllEntries();
            refreshData();
            showInfo("All entries were deleted. You can start fresh.");
        } catch (SQLException e) {
            showError("Could not delete the entries.", e);
        }
    }

    // =================================================================
    //  Inner components
    // =================================================================

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Small rounded icon square */
    private static void iconSquare(Graphics2D g2, String icon, int x, int y, int size, Color bg, Color fg) {
        g2.setColor(bg);
        g2.fillRoundRect(x, y, size, size, 14, 14);
        int s = Math.round(size * 0.56f);
        Icons.get(icon, s, fg).paintIcon(null, g2, x + (size - s) / 2, y + (size - s) / 2);
    }

    /**
     * One term of "Revenue - Expenses = Net income": a soft tile with an icon,
     * a name, the amount and a short note. Net income is the deep purple one.
     * Hover: it rises 2px and its shadow deepens.
     */
    private static class Term extends JComponent {
        private final String icon;
        private final Color iconBg, iconFg;
        private final boolean net;
        private String title, note, amount = "-";

        Term(String icon, Color iconBg, Color iconFg, String title, String note, boolean net) {
            this.icon = icon;
            this.iconBg = iconBg;
            this.iconFg = iconFg;
            this.title = title;
            this.note = note;
            this.net = net;
            Hover.of(this);
        }

        void setAmount(String amount) { this.amount = amount; repaint(); }
        void setTitle(String title)   { this.title = title; repaint(); }
        void setNote(String note)     { this.note = note; repaint(); }

        @Override public Dimension getPreferredSize() { return new Dimension(150, 118); }
        @Override public Dimension getMinimumSize()   { return new Dimension(110, 118); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float t = Hover.of(this).value();
            int w = getWidth() - 1, h = getHeight() - 7;
            float y = 3 - 2 * t;
            // shadow "0 2px 6px rgba(46,16,101,.07)" (net: "0 8px 18px .3")
            for (int i = 4; i >= 1; i--) {
                int a = net ? Math.round((16 + 10 * t) / i) : Math.round((5 + 8 * t) / i);
                g2.setColor(new Color(46, 16, 101, a));
                g2.fill(new RoundRectangle2D.Float(1 - i * 0.5f, 3 + i * (1 + t), w - 1 + i, h, 24 + i, 24 + i));
            }
            RoundRectangle2D box = new RoundRectangle2D.Float(1, y, w - 1, h, 24, 24);
            if (net) {
                g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[] {0f, 0.6f, 1f},
                        new Color[] {new Color(0x2e0f6b), new Color(0x5b21b6), new Color(0x7c3aed)}));
                g2.fill(box);
            } else {
                g2.setColor(new Color(0xfaf8fe));
                g2.fill(box);
                g2.setColor(Theme.mix(new Color(0xeee8f8), new Color(0xc9b8f5), t));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(box);
            }
            int px = 13, top = Math.round(y) + 11;
            iconSquare(g2, icon, px, top, 24, iconBg, iconFg);

            g2.setFont(Theme.semibold(12));
            g2.setColor(net ? new Color(0xe3d6ff) : Theme.LABEL);
            g2.drawString(title, px, top + 44);

            g2.setFont(Theme.bold(net ? 21 : 18));
            g2.setColor(net ? Theme.YELLOW : DEEP);
            FontMetrics fm = g2.getFontMetrics();
            String shown = fit(amount, fm, w - px * 2);
            g2.drawString(shown, px, top + 68);

            g2.setFont(Theme.regular(11.5f));
            g2.setColor(net ? new Color(0xcbb9f5) : EM);
            g2.drawString(note, px, top + 86);
            g2.dispose();
        }
    }

    /** Shortens a text with "..." if it does not fit */
    private static String fit(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "…") > width) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    /**
     * The balance position drawn as a T-account:
     *
     *   [=] Total assets  Dr          |          Cr  Total liabilities [=]
     *   P15,000.00  What it owns      |   What it owes   P15,000.00
     *   [=========== assets ======]   |   [== equity ==|== liabilities ==]
     */
    private static class TAccount extends JComponent {
        private BigDecimal assets = BigDecimal.ZERO, liabilities = BigDecimal.ZERO, equity = BigDecimal.ZERO;
        private float shownA, shownL, shownE;          // animated bar fractions
        private final javax.swing.Timer grow;

        TAccount() {
            Hover.of(this);
            grow = new javax.swing.Timer(15, e -> {
                float[] target = targets();
                shownA += (target[0] - shownA) * 0.18f;
                shownL += (target[1] - shownL) * 0.18f;
                shownE += (target[2] - shownE) * 0.18f;
                if (Math.abs(target[0] - shownA) + Math.abs(target[1] - shownL) + Math.abs(target[2] - shownE) < 0.003f) {
                    shownA = target[0];
                    shownL = target[1];
                    shownE = target[2];
                    ((javax.swing.Timer) e.getSource()).stop();
                }
                repaint();
            });
        }

        void setValues(BigDecimal assets, BigDecimal liabilities, BigDecimal equity) {
            this.assets = assets;
            this.liabilities = liabilities;
            this.equity = equity;
            shownA = shownL = shownE = 0;      // the bars grow in each time the page opens
            grow.restart();
        }

        /** { assets share, liabilities share, equity share } */
        private float[] targets() {
            double l = Math.max(0, liabilities.doubleValue()), e = Math.max(0, equity.doubleValue());
            double right = l + e;
            return new float[] {
                    assets.signum() > 0 ? 1f : 0f,
                    right > 0 ? (float) (l / right) : 0f,
                    right > 0 ? (float) (e / right) : 0f};
        }

        @Override public Dimension getPreferredSize() { return new Dimension(400, 118); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            int top = getInsets().top;
            float t = Hover.of(this).value();
            // the T: a 2px line on top and down the middle
            g2.setColor(Theme.PURPLE_DEEP);
            g2.fillRect(0, top, w, 2);
            g2.fillRect(w / 2 - 1, top, 2, h - top);

            int half = w / 2;
            float[] target = targets();
            int pctL = Math.round(target[1] * 100), pctE = Math.round(target[2] * 100);
            side(g2, 16, top + 12, half - 32, false, Icons.LAYERS, "Total assets", "Dr", Money.peso(assets),
                    "What the business owns", new String[] {"Assets " + (assets.signum() > 0 ? 100 : 0) + "%"},
                    new Color[] {Theme.PURPLE}, t);
            side(g2, half + 16, top + 12, half - 32, true, Icons.FILE, "Total liabilities", "Cr", Money.peso(liabilities),
                    "What the business owes", new String[] {"Liabilities " + pctL + "%", "Equity " + pctE + "%"},
                    new Color[] {Theme.GOLD_DARK, Theme.GREEN_BAR}, t);
            g2.dispose();
        }

        private void side(Graphics2D g2, int x, int y, int w, boolean credit, String icon, String name, String tag,
                          String amount, String note, String[] legend, Color[] legendColors, float hover) {
            Color tagBg = credit ? new Color(0xfff3cc) : new Color(0xefe8fe);
            Color tagFg = credit ? new Color(0x8a5a00) : new Color(0x5b21b6);
            // heading row: icon + name + Dr/Cr tag (mirrored on the credit side)
            g2.setFont(Theme.semibold(12));
            FontMetrics fm = g2.getFontMetrics();
            int nameW = fm.stringWidth(name);
            g2.setFont(Theme.spaced(Theme.bold(9.5f), 0.08f));
            int tagW = g2.getFontMetrics().stringWidth(tag) + 12;
            int rowW = 24 + 7 + nameW + 7 + tagW;
            int rx = credit ? x + w - rowW : x;
            if (credit) {
                drawTag(g2, rx, y + 5, tagW, tag, tagBg, tagFg);
                g2.setFont(Theme.semibold(12));
                g2.setColor(Theme.LABEL);
                g2.drawString(name, rx + tagW + 7, y + 17);
                iconSquare(g2, icon, rx + tagW + 7 + nameW + 7, y, 24, tagBg, tagFg);
            } else {
                iconSquare(g2, icon, rx, y, 24, tagBg, tagFg);
                g2.setFont(Theme.semibold(12));
                g2.setColor(Theme.LABEL);
                g2.drawString(name, rx + 31, y + 17);
                drawTag(g2, rx + 31 + nameW + 7, y + 5, tagW, tag, tagBg, tagFg);
            }

            // amount + note on one line
            g2.setFont(Theme.bold(20));
            fm = g2.getFontMetrics();
            int aw = fm.stringWidth(amount);
            g2.setFont(Theme.regular(11.5f));
            FontMetrics nfm = g2.getFontMetrics();
            int nw = nfm.stringWidth(note);
            int ay = y + 50;
            int ax = credit ? x + w - aw - (nw + 12) : x;
            g2.setFont(Theme.bold(20));
            g2.setColor(DEEP);
            g2.drawString(amount, ax, ay);
            if (aw + nw + 12 <= w) {
                g2.setFont(Theme.regular(11.5f));
                g2.setColor(EM);
                g2.drawString(note, ax + aw + 12, ay - 2);
            }

            // the bar (8px, rounded)
            int by = y + 62, bh = 8 + Math.round(2 * hover);
            RoundRectangle2D track = new RoundRectangle2D.Float(x, by, w, bh, bh, bh);
            g2.setColor(new Color(0xefeaf8));
            g2.fill(track);
            Graphics2D bar = (Graphics2D) g2.create();
            bar.clip(track);
            if (!credit) {
                int fillW = Math.round(w * shownA);
                bar.setPaint(new GradientPaint(x, 0, Theme.PURPLE_DARK, x + Math.max(1, fillW), 0, Theme.PURPLE_BRIGHT));
                bar.fillRect(x, by, fillW, bh);
            } else {
                // liabilities grow in from the right, equity sits to their left
                int lw = Math.round(w * shownL), ew = Math.round(w * shownE);
                bar.setPaint(new GradientPaint(x + w - lw, 0, Theme.YELLOW, x + w, 0, Theme.GOLD_DARK));
                bar.fillRect(x + w - lw, by, lw, bh);
                bar.setColor(Theme.GREEN_BAR);
                bar.fillRect(x + w - lw - ew, by, ew, bh);
            }
            bar.dispose();

            // legend
            g2.setFont(Theme.regular(10.5f));
            fm = g2.getFontMetrics();
            int lx = x, ly = by + bh + 14;
            int total = 0;
            for (String s : legend) {
                total += 13 + fm.stringWidth(s) + 12;
            }
            if (credit) {
                lx = x + w - total + 12;
            }
            for (int i = 0; i < legend.length; i++) {
                g2.setColor(legendColors[i]);
                g2.fillRoundRect(lx, ly - 8, 8, 8, 3, 3);
                g2.setColor(EM);
                g2.drawString(legend[i], lx + 13, ly);
                lx += 13 + fm.stringWidth(legend[i]) + 12;
            }
        }

        private static void drawTag(Graphics2D g2, int x, int y, int w, String text, Color bg, Color fg) {
            g2.setColor(bg);
            g2.fillRoundRect(x, y, w, 15, 9, 9);
            g2.setFont(Theme.spaced(Theme.bold(9.5f), 0.08f));
            g2.setColor(fg);
            g2.drawString(text, x + 6, y + 11);
        }
    }

    /**
     * The yellow "Posted entries" ticket: ruled like a receipt, two notches
     * on its sides, a tilted POSTED stamp, the big count, the latest three
     * references and a New entry button.
     */
    private class Ticket extends RoundedPanel {
        private final JLabel big = new JLabel("0");
        private final JPanel refs = new JPanel();

        Ticket() {
            super(new BorderLayout());
            setRadius(16);
            setFill(new Color(0xfff7d6)).setGradient(new Color(0xfbcf4b), 0f);
            setBorderColor(new Color(0xf3cf5a)).setBorderWidth(1.2f);
            setLift(true);
            setBorder(BorderFactory.createEmptyBorder(3 + 14, 3 + 18, 9 + 16, 3 + 18));

            JLabel k = new JLabel("POSTED ENTRIES");
            k.setFont(Theme.spaced(Theme.semibold(11), 0.12f));
            k.setForeground(new Color(0x6a4a0a));

            big.setFont(Theme.bold(56));
            big.setForeground(Theme.PURPLE_DEEP);
            JLabel note = new JLabel("Journal entries recorded");
            note.setFont(Theme.regular(11.5f));
            note.setForeground(new Color(0x6a4a0a));

            refs.setOpaque(false);
            refs.setLayout(new BoxLayout(refs, BoxLayout.Y_AXIS));

            AppButton newEntry = new AppButton("New entry", Icons.get(Icons.PEN, 15, Color.WHITE), AppButton.Style.PURPLE);
            newEntry.setCompact(true);
            newEntry.addActionListener(e -> mainFrame.showPage(MainFrame.JOURNAL_ENTRY));

            JPanel column = new JPanel();
            column.setOpaque(false);
            column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
            JComponent[] parts = {k, (JComponent) Box.createVerticalGlue(), big, note, tearLine(), refs, newEntry};
            for (JComponent part : parts) {
                part.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            column.add(k);
            column.add(Box.createVerticalStrut(18));
            column.add(Box.createVerticalGlue());
            column.add(big);
            column.add(note);
            column.add(Box.createVerticalStrut(8));
            column.add(parts[4]);
            column.add(Box.createVerticalStrut(8));
            column.add(refs);
            column.add(Box.createVerticalStrut(6));
            newEntry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            column.add(newEntry);
            add(column, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(230, super.getPreferredSize().height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(230, super.getMinimumSize().height);
        }

        void setValues(int count, List<String[]> latest) {
            big.setText(String.valueOf(count));
            refs.removeAll();
            for (String[] r : latest) {
                RefRow row = new RefRow(r[0], r[1]);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                refs.add(row);
                refs.add(Box.createVerticalStrut(3));
            }
            if (latest.isEmpty()) {
                JLabel none = new JLabel("No references yet");
                none.setFont(Theme.regular(11.5f));
                none.setForeground(new Color(0x7a5a12));
                refs.add(none);
            }
            refs.revalidate();
        }

        private JComponent tearLine() {
            JComponent line = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = smooth(g);
                    g2.setColor(new Color(122, 79, 1, 90));
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f));
                    g2.drawLine(-20, getHeight() / 2, getWidth() + 20, getHeight() / 2);
                    g2.dispose();
                }
            };
            line.setPreferredSize(new Dimension(10, 6));
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            return line;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = smooth(g);
            java.awt.Shape shape = cardShape();
            // receipt rules every 24px
            Graphics2D rules = (Graphics2D) g2.create();
            rules.clip(shape);
            rules.setColor(new Color(122, 79, 1, 18));
            for (int y = 23; y < getHeight(); y += 24) {
                rules.fillRect(0, y, getWidth(), 1);
            }
            // a soft white shine in the top-right corner
            rules.setPaint(new java.awt.RadialGradientPaint(getWidth(), 0, 160,
                    new float[] {0f, 1f}, new Color[] {new Color(255, 255, 255, 150), new Color(255, 255, 255, 0)}));
            rules.fillRect(0, 0, getWidth(), getHeight());
            rules.dispose();

            // the tilted POSTED stamp
            Graphics2D stamp = (Graphics2D) g2.create();
            int sx = getWidth() - 84, sy = 22;
            stamp.rotate(Math.toRadians(-14), sx + 34, sy + 10);
            stamp.setColor(new Color(109, 40, 217, 140));
            stamp.setStroke(new BasicStroke(2f));
            stamp.drawRoundRect(sx, sy, 68, 20, 10, 10);
            stamp.setColor(new Color(109, 40, 217, 70));
            stamp.setStroke(new BasicStroke(1f));
            stamp.drawRoundRect(sx - 3, sy - 3, 74, 26, 12, 12);
            stamp.setFont(Theme.spaced(Theme.bold(10), 0.18f));
            stamp.setColor(new Color(109, 40, 217, 150));
            FontMetrics fm = stamp.getFontMetrics();
            stamp.drawString("POSTED", sx + (68 - fm.stringWidth("POSTED")) / 2, sy + 14);
            stamp.dispose();

            // two notches on the sides, at 62% of the height
            int ny = Math.round(getHeight() * 0.62f) - 9;
            g2.setColor(new Color(0xf4effd));
            g2.fillOval(-6, ny, 18, 18);
            g2.fillOval(getWidth() - 12, ny, 18, 18);
            g2.setColor(new Color(0xf3cf5a));
            g2.drawOval(-6, ny, 18, 18);
            g2.drawOval(getWidth() - 12, ny, 18, 18);
            g2.dispose();
        }
    }

    /** "JE-7322 ...... Nov 3" in the ticket; lights up white under the mouse */
    private static class RefRow extends JComponent {
        private final String ref, date;

        RefRow(String ref, String date) {
            this.ref = ref;
            this.date = date;
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(180, 24); }
        @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 24); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float t = Hover.of(this).value();
            g2.setColor(new Color(255, 255, 255, Math.round(140 + 100 * t)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setFont(Theme.mono(11));
            g2.setColor(new Color(0x3b1578));
            FontMetrics fm = g2.getFontMetrics();
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(ref, 8 + Math.round(2 * t), ty);
            g2.setFont(Theme.regular(11));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(0x7a5a12));
            g2.drawString(date, getWidth() - 8 - fm.stringWidth(date), ty);
            g2.dispose();
        }
    }

    /** "View all in General Journal": purple, underlined on hover */
    private static class LinkButton extends JButton {
        LinkButton(String text) {
            super(text);
            setFont(Theme.bold(13));
            setForeground(Theme.PURPLE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Hover.of(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            float t = Hover.of(this).value();
            boolean onPurple = Boolean.TRUE.equals(getClientProperty(tala.ui.components.Card.ON_DARK));
            setForeground(onPurple ? Theme.mix(Theme.YELLOW, new Color(0xfff1a8), t)      // yellow link on a purple header
                                   : Theme.mix(Theme.PURPLE, Theme.PURPLE_DEEP, t));
            super.paintComponent(g);
            Graphics2D g2 = smooth(g);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int tw = fm.stringWidth(getText());
            int x = (getWidth() - tw) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 + 1;
            g2.setColor(getForeground());
            g2.fillRect(x, y, Math.round(tw * t), 1);
            g2.dispose();
        }
    }
}
