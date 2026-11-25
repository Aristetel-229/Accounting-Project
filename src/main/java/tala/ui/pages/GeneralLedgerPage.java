package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.LedgerRow;
import tala.service.AccountingService;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.RoundedPanel;
import tala.ui.components.ScrollablePanel;
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
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GeneralLedgerPage
 * ------------------------------------------------------------
 * Left card "Accounts used" (fixed size, the list scrolls inside):
 *   a search box and every account that has been used, with its balance.
 * Right card (fixed size, scrolls inside): the chosen account's totals and
 *   its ledger (the rows scroll, the Total / ending row stays put).
 *
 * A balance on the "wrong" side (e.g. Cash with a CR balance)
 * is flagged as an ABNORMAL balance so you can check the entries.
 */
public class GeneralLedgerPage extends BasePage {

    // ---- left: accounts used
    private final Card accountsCard = new Card("Accounts used", "");
    private final JTextField searchField = new JTextField();
    private final JPanel accountList = new ScrollablePanel(null);
    private final JLabel noMatchLabel = new JLabel("No account matches. Try another name or code.", JLabel.CENTER);

    // ---- right: the ledger card
    private final Card ledgerCard = new Card("", "");
    private final Badge abnormalBadge = Badge.warn("Abnormal balance: check these entries");
    private final JLabel tile1Title = new JLabel(), tile2Title = new JLabel(), tile3Title = new JLabel();
    private final JLabel tile1 = tileValue(), tile2 = tileValue(), tile3 = tileValue();

    // one account
    private final ReadOnlyTableModel model = new ReadOnlyTableModel("Date", "Description", "Ref", "Debit", "Credit", "Balance");
    private final ReadOnlyTableModel totalModel = new ReadOnlyTableModel("Date", "Description", "Ref", "Debit", "Credit", "Balance");
    private final TalaTable table;
    private TalaTable totalTable;

    private List<Account> usedAccounts = new ArrayList<>();
    private List<JournalEntry> entries = new ArrayList<>();
    private int selectedAccountId = -1;   // -1 = none yet (the first account is picked)
    private boolean selectedIsAbnormal = false;

    public GeneralLedgerPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);
        useFixedHeight();

        table = new TalaTable(model) {
            @Override protected Color cellForeground(int row, int column) {
                if (column == 3) return Theme.DEBIT;
                if (column == 4) return Theme.CREDIT;
                return column == 5 && selectedIsAbnormal ? Theme.AMBER : Theme.TEXT;
            }
        };
        table.setRightAligned(3, 4, 5);
        table.setPillColumns(2);
        table.setColumnWidths(82, 0, 116, 104, 104, 178);
        table.setRowSelectionAllowed(false);

        PageHeader header = new PageHeader(Icons.LAYERS, "General Ledger",
                "One ledger per account you have used: every transaction on that account, "
                + "its running balance, total and ending balance. Pick an account on the left.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        header.addAction(refresh);
        addSection(header);

        // ================= left card =================
        Theme.styleField(searchField);
        Theme.setPlaceholder(searchField, "Name or code, e.g. 1100");
        final javax.swing.Timer searchDelay = new javax.swing.Timer(180, e -> {
            buildAccountList();
            showSelected();
        });
        searchDelay.setRepeats(false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { searchDelay.restart(); }
            public void removeUpdate(DocumentEvent e)  { searchDelay.restart(); }
            public void changedUpdate(DocumentEvent e) { searchDelay.restart(); }
        });
        JPanel search = new JPanel(new BorderLayout());
        search.setOpaque(false);
        search.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        search.add(new tala.ui.components.FormField("Search accounts", searchField, null), BorderLayout.CENTER);

        accountList.setOpaque(false);
        accountList.setLayout(new BoxLayout(accountList, BoxLayout.Y_AXIS));
        accountList.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        noMatchLabel.setFont(Theme.regular(13.5f));
        noMatchLabel.setForeground(Theme.MUTED);
        noMatchLabel.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));

        JPanel leftBody = new JPanel(new BorderLayout());
        leftBody.setOpaque(false);
        leftBody.add(search, BorderLayout.NORTH);
        leftBody.add(scroller(accountList), BorderLayout.CENTER);
        accountsCard.setBody(leftBody);

        // ================= right card =================
        ledgerCard.addToHeader(abnormalBadge);

        JPanel tiles = new JPanel(new GridLayout(1, 3, 12, 0));
        tiles.setOpaque(false);
        tiles.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));
        tiles.add(tile(tile1Title, tile1, Theme.TINT_PURPLE, Theme.EDGE_PURPLE));
        tiles.add(tile(tile2Title, tile2, Theme.TINT_YELLOW, Theme.EDGE_YELLOW));
        tiles.add(tile(tile3Title, tile3, Theme.TINT_GREEN, Theme.EDGE_GREEN));

        // one account: rows scroll, the Total / ending row stays at the bottom
        totalTable = new TalaTable(totalModel) {
            @Override protected boolean rowBold(int row) { return true; }
            @Override protected Color rowBackground(int row, boolean selected) { return Theme.CREAM; }
            @Override protected Color cellForeground(int row, int column) {
                return column == 5 && selectedIsAbnormal ? Theme.AMBER : Theme.TITLE;
            }
        };
        totalTable.setColumnModel(table.getColumnModel());
        totalTable.setRightAligned(3, 4, 5);
        totalTable.setRowSelectionAllowed(false);
        totalTable.setFocusable(false);
        JPanel totalStrip = new JPanel(new BorderLayout());
        totalStrip.setOpaque(false);
        totalStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CREAM_LINE),
                BorderFactory.createEmptyBorder(0, 0, 0, 12)));
        totalStrip.add(totalTable, BorderLayout.CENTER);
        JPanel oneView = new JPanel(new BorderLayout());
        oneView.setOpaque(false);
        oneView.add(table.inScroll(), BorderLayout.CENTER);
        oneView.add(totalStrip, BorderLayout.SOUTH);

        JPanel rightBody = new JPanel(new BorderLayout());
        rightBody.setOpaque(false);
        rightBody.add(tiles, BorderLayout.NORTH);
        rightBody.add(oneView, BorderLayout.CENTER);
        ledgerCard.setBody(rightBody);

        addFillSection(fillColumns(accountsCard, 0.3, ledgerCard, 0.7));
    }

    /** A see-through scroll area for a list inside a card */
    private static JScrollPane scroller(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        return sp;
    }

    @Override
    public void refreshData() {
        try {
            entries = journalDAO.getAllEntries();
            usedAccounts = accounting.getUsedAccounts(accountDAO.getAllAccounts(), entries);

            // keep the same account selected if it still exists
            boolean stillThere = false;
            for (Account a : usedAccounts) {
                if (a.getId() == selectedAccountId) {
                    stillThere = true;
                }
            }
            if (!stillThere) {
                selectedAccountId = usedAccounts.isEmpty() ? -1 : usedAccounts.get(0).getId();
            }
            buildAccountList();
            showSelected();
        } catch (SQLException e) {
            showError("Could not load the ledger from MySQL.", e);
        }
    }

    private void select(int accountId) {
        selectedAccountId = accountId;
        buildAccountList();
        showSelected();
    }

    /** The accounts that match the search box */
    private List<Account> matching() {
        String q = searchField.getText().trim().toLowerCase();
        List<Account> list = new ArrayList<>();
        for (Account a : usedAccounts) {
            if (q.isEmpty() || a.getName().toLowerCase().contains(q) || String.valueOf(a.getCode()).contains(q)) {
                list.add(a);
            }
        }
        return list;
    }

    /** Rebuilds the clickable list on the left */
    private void buildAccountList() {
        accountList.removeAll();
        int abnormalCount = 0;
        for (Account a : usedAccounts) {
            if (accounting.getBalance(a, entries).signum() < 0) {
                abnormalCount++;
            }
        }

        List<Account> shown = matching();
        for (Account a : shown) {
            BigDecimal balance = accounting.getBalance(a, entries);
            boolean abnormal = balance.signum() < 0;
            String balanceText = Money.peso(balance.abs()) + " " + AccountingService.sideOf(a, balance);
            AccountButton button = new AccountButton(a.getName(), a.getCode() + " · " + a.getType().getLabel(),
                    balanceText, abnormal ? "Abnormal balance" : null, abnormal, a.getId() == selectedAccountId);
            button.addActionListener(e -> select(a.getId()));
            addRow(button);
        }
        if (usedAccounts.isEmpty()) {
            noMatchLabel.setText("No accounts have been used yet. Post a journal entry first.");
            addRow(noMatchLabel);
        } else if (shown.isEmpty()) {
            noMatchLabel.setText("No account matches. Try another name or code.");
            addRow(noMatchLabel);
        }

        String sub = usedAccounts.size() + (usedAccounts.size() == 1 ? " account" : " accounts");
        if (abnormalCount > 0) {
            sub += " · " + abnormalCount + " need a look";
        }
        accountsCard.setSubtitle(sub);
        accountList.revalidate();
        accountList.repaint();
    }

    private void addRow(JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountList.add(row);
        accountList.add(Box.createVerticalStrut(2));
    }

    /** Fills the right card with the selected account's ledger */
    private void showSelected() {
        showOne();
        revalidate();
        repaint();
    }

    private void showOne() {
        model.clear();
        totalModel.clear();
        Account account = null;
        for (Account a : usedAccounts) {
            if (a.getId() == selectedAccountId) {
                account = a;
            }
        }
        if (account == null) {
            // nothing posted yet
            selectedIsAbnormal = false;
            ledgerCard.setTitle("No account selected");
            ledgerCard.setSubtitle("Post a journal entry and its accounts appear here.");
            abnormalBadge.setVisible(false);
            tile1Title.setText("Total debits");
            tile2Title.setText("Total credits");
            tile3Title.setText("Ending balance");
            tile1.setText(Money.peso(BigDecimal.ZERO));
            tile2.setText(Money.peso(BigDecimal.ZERO));
            tile3.setText(Money.peso(BigDecimal.ZERO));
            tile3.setForeground(Theme.TITLE);
            return;
        }
        List<LedgerRow> rows = accounting.buildLedger(account, entries);
        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO, ending = BigDecimal.ZERO;
        for (LedgerRow r : rows) {
            totalDebit = totalDebit.add(r.getDebit());
            totalCredit = totalCredit.add(r.getCredit());
            ending = r.getRunningBalance();
            model.addRow(new Object[] {
                    DateUtil.toShort(r.getDate()), r.getDescription(), r.getReferenceNo(),
                    Money.formatOrBlank(r.getDebit()), Money.formatOrBlank(r.getCredit()),
                    Money.format(r.getRunningBalance().abs()) + " " + AccountingService.sideOf(account, r.getRunningBalance())});
        }
        String endingText = Money.peso(ending.abs()) + " " + AccountingService.sideOf(account, ending);
        totalModel.addRow(new Object[] {"Total", "Ending balance", "", Money.format(totalDebit), Money.format(totalCredit), endingText});

        selectedIsAbnormal = ending.signum() < 0;
        ledgerCard.setTitle(account.getName());
        ledgerCard.setSubtitle(account.getCode() + " · " + account.getType().getLabel()
                + " · Normal balance: " + account.getNormalBalance());
        abnormalBadge.setVisible(selectedIsAbnormal);
        tile1Title.setText("Total debits");
        tile2Title.setText("Total credits");
        tile3Title.setText("Ending balance");
        tile1.setText(Money.peso(totalDebit));
        tile2.setText(Money.peso(totalCredit));
        tile3.setText(endingText);
        tile3.setForeground(selectedIsAbnormal ? Theme.AMBER : Theme.TITLE);
    }

    // ------------------------------------------------------------ small parts

    private static JLabel tileValue() {
        JLabel label = new JLabel();
        label.setFont(Theme.bold(18));
        label.setForeground(Theme.TITLE);
        return label;
    }

    /** A tinted total tile that rises on hover */
    private static JPanel tile(JLabel title, JLabel value, Color tint, Color edge) {
        RoundedPanel tile = new RoundedPanel(new BorderLayout(0, 2));
        tile.setRadius(12).setFill(Color.WHITE).setGradient(tint, 0f).setBorderColor(edge);
        tile.setHoverable(edge);
        tile.setLift(true);
        tile.setBorder(BorderFactory.createEmptyBorder(3 + 8, 3 + 12, 9 + 8, 3 + 12));
        title.setFont(Theme.medium(12.5f));
        title.setForeground(Theme.TEXT_SOFT);
        tile.add(title, BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        return tile;
    }

    /** One row in the left list: name, code, balance and a note; lilac when selected, soft hover */
    private static class AccountButton extends JButton {
        private final boolean selected;

        AccountButton(String title, String metaText, String balance, String note, boolean warn, boolean selected) {
            this.selected = selected;
            setLayout(new BorderLayout(10, 0));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            getAccessibleContext().setAccessibleName(title + ", balance " + balance);
            Hover.of(this);

            JLabel name = new JLabel(title);
            name.setFont(Theme.semibold(14));
            name.setForeground(Theme.TITLE);
            JLabel meta = new JLabel(metaText);
            meta.setFont(Theme.regular(12));
            meta.setForeground(Theme.MUTED);
            JPanel left = new JPanel(new GridLayout(2, 1));
            left.setOpaque(false);
            left.add(name);
            left.add(meta);

            JLabel amount = new JLabel(balance, JLabel.RIGHT);
            amount.setFont(Theme.bold(13.5f));
            amount.setForeground(warn && note != null && note.startsWith("Abnormal") ? Theme.AMBER : Theme.TITLE);
            JLabel noteLabel = new JLabel(note == null ? " " : note, JLabel.RIGHT);
            noteLabel.setFont(note != null && note.startsWith("Abnormal") ? Theme.bold(11.5f) : Theme.regular(11.5f));
            noteLabel.setForeground(warn ? new Color(0x7a4f01) : Theme.MUTED);
            JPanel right = new JPanel(new GridLayout(2, 1));
            right.setOpaque(false);
            right.add(amount);
            right.add(noteLabel);

            add(left, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(200, 56); }
        @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 56); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float t = Hover.of(this).value();
            if (selected) {
                g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(0xefe8ff), getWidth(), 0, new Color(0xfffaeb)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(new Color(0xc9b8f5));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            } else if (t > 0 || isFocusOwner()) {
                g2.setColor(Theme.alpha(Theme.PURPLE_HOVER, Math.round(255 * Math.max(t, isFocusOwner() ? 1f : 0f))));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(Theme.alpha(Theme.CARD_HOVER_BORDER, Math.round(200 * t)));
                g2.fillRoundRect(0, 12, 3, getHeight() - 24, 3, 3);
            }
            g2.dispose();
        }
    }
}
