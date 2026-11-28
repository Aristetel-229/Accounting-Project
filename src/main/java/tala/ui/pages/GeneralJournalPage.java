package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.JournalEntry;
import tala.model.JournalLine;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.FormField;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.TalaTable;
import tala.ui.theme.Theme;
import tala.util.DateUtil;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * GeneralJournalPage
 * ------------------------------------------------------------
 * The classic journal layout, oldest first:
 *
 *   Date    Particulars              Post ref   Debit       Credit
 *   Nov 1   Cash                     1010       100,000.00
 *              Owner's Capital       3010                   100,000.00
 *           (Owner invested cash)  JE-000001
 */
public class GeneralJournalPage extends BasePage {

    // Kinds of table rows (decide how each row is drawn)
    private static final String DEBIT_ROW = "debit", CREDIT_ROW = "credit", MEMO_ROW = "memo", TOTAL_ROW = "total";

    private final ReadOnlyTableModel model = new ReadOnlyTableModel("Date", "Particulars", "Post ref", "Debit", "Credit");
    private final List<String> rowKinds = new ArrayList<>();
    private final TalaTable table;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> monthCombo = new JComboBox<>();
    private final Card card = new Card("Journal entries", "");
    private final JLabel entriesLabel = footLabel(), debitLabel = footLabel(), creditLabel = footLabel();
    private final Badge balanceBadge = Badge.ok("In balance");
    private final tala.ui.components.StatCard entriesTile = new tala.ui.components.StatCard(Icons.FILE, Theme.PURPLE_TINT, Theme.PURPLE,
            "Entries", "", tala.ui.components.StatCard.Look.NORMAL);
    private final tala.ui.components.StatCard debitTile = new tala.ui.components.StatCard(Icons.TREND_UP, Theme.AMBER_BG, Theme.AMBER,
            "Total debit", "", tala.ui.components.StatCard.Look.NORMAL);
    private final tala.ui.components.StatCard creditTile = new tala.ui.components.StatCard(Icons.TREND_DOWN, Theme.GREEN_BG, Theme.GREEN,
            "Total credit", "", tala.ui.components.StatCard.Look.NORMAL);
    private final tala.ui.components.StatCard accountsTile = new tala.ui.components.StatCard(Icons.LAYERS, Theme.BLUE_BG, Theme.BLUE,
            "Accounts used", "", tala.ui.components.StatCard.Look.NORMAL);
    private final JLabel emptyLabel = new JLabel("No journal entries match. Post one in Journal Entry.", JLabel.CENTER);

    // the Total row stays under the scrolling rows (it shares the table's columns)
    private final ReadOnlyTableModel totalModel = new ReadOnlyTableModel("Date", "Particulars", "Post ref", "Debit", "Credit");
    private TalaTable totalTable;
    private final JPanel totalStrip = new JPanel(new BorderLayout());

    // "Accounts in this journal": a chip per account (Cash · Dr, Accounts Payable · Cr)
    private final ChipStrip accountChips = new ChipStrip();

    private List<JournalEntry> allEntries = new ArrayList<>();
    private final List<JournalEntry> shownEntries = new ArrayList<>();   // what the table shows (for Export)
    private boolean loading = false;   // stops the month box from reloading while we fill it

    public GeneralJournalPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);
        useFixedHeight();

        // The table: memo rows in grey italics, credit lines indented, total row bold on cream
        table = new TalaTable(model) {
            @Override protected boolean rowBold(int row) {
                return kind(row).equals(TOTAL_ROW) || (kind(row).equals(DEBIT_ROW));
            }
            @Override protected Color rowBackground(int row, boolean selected) {
                return kind(row).equals(TOTAL_ROW) ? Theme.HEAD_BG : Color.WHITE;
            }
            @Override protected Color cellForeground(int row, int column) {
                return kind(row).equals(MEMO_ROW) ? Theme.MUTED : Theme.TEXT;
            }
            // the date of each entry sits in a small cream pill ("Nov 3")
            @Override protected Color[] pillColors(String text) {
                return new Color[] {Theme.AMBER_BG, Theme.AMBER};
            }
            @Override protected boolean cellBold(int row, int column) {
                return column == 0 || rowBold(row);
            }
            @Override protected int cellIndent(int row, int column) {
                if (column != 1) return 0;
                if (kind(row).equals(CREDIT_ROW)) return 40;   // credits are indented
                if (kind(row).equals(MEMO_ROW)) return 20;
                return 0;
            }
        };
        table.setRightAligned(3, 4);
        table.setPillColumns(0);
        table.setColumnWidths(120, 0, 110, 170, 170);
        table.setRowSelectionAllowed(false);

        PageHeader header = new PageHeader(Icons.BOOK, "General Journal",
                "Every entry in date order: date, particulars, post reference, debit and credit.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        header.addAction(refresh);
        header.addAction(new tala.ui.components.ExportMenu("General Journal",
                () -> export("csv"), () -> export("xlsx"), this::printJournal));
        addSection(header);

        // four slim summary tiles
        JPanel tiles = new JPanel(new java.awt.GridLayout(1, 4, 12, 0));
        tiles.setOpaque(false);
        tiles.add(entriesTile);
        tiles.add(debitTile);
        tiles.add(creditTile);
        tiles.add(accountsTile);
        addSection(tiles);

        // search + month filter in the card header
        Theme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(240, 46));
        Theme.styleCombo(monthCombo);
        monthCombo.setPreferredSize(new Dimension(190, 46));
        card.addToHeader(new FormField("Search", searchField, null));
        card.addToHeader(new FormField("Month", monthCombo, null));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { fillTable(); }
            public void removeUpdate(DocumentEvent e)  { fillTable(); }
            public void changedUpdate(DocumentEvent e) { fillTable(); }
        });
        monthCombo.addActionListener(e -> { if (!loading) fillTable(); });

        emptyLabel.setFont(Theme.regular(15));
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));

        // pinned Total row: same columns as the table, cream like the design
        totalTable = new TalaTable(totalModel) {
            @Override protected boolean rowBold(int row) { return true; }
            @Override protected Color rowBackground(int row, boolean selected) { return Theme.HEAD_BG; }
            @Override protected Color cellForeground(int row, int column) { return Theme.PURPLE_DEEP; }
        };
        totalTable.setColumnModel(table.getColumnModel());
        totalTable.setRightAligned(3, 4);
        totalTable.setRowSelectionAllowed(false);
        totalTable.setFocusable(false);
        totalStrip.setOpaque(false);
        totalStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(0xc4b0ee)),
                BorderFactory.createEmptyBorder(0, 0, 0, 12)));   // 12 = the scroll bar beside the rows
        totalStrip.add(totalTable, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(emptyLabel, BorderLayout.NORTH);
        south.add(totalStrip, BorderLayout.SOUTH);

        accountChips.setOpaque(false);
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(accountChips, BorderLayout.NORTH);
        body.add(table.inScroll(), BorderLayout.CENTER);
        body.add(south, BorderLayout.SOUTH);
        card.setBody(body);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        totals.setOpaque(false);
        totals.add(entriesLabel);
        totals.add(debitLabel);
        totals.add(creditLabel);
        footer.add(totals, BorderLayout.WEST);
        footer.add(balanceBadge, BorderLayout.EAST);
        card.setFooter(footer);

        addFillSection(card);
    }

    private String kind(int row) {
        return row < rowKinds.size() ? rowKinds.get(row) : DEBIT_ROW;
    }

    @Override
    public void refreshData() {
        try {
            allEntries = accounting.sortByDate(journalDAO.getAllEntries());

            // Month choices: "All months" + every month that has entries (newest first)
            loading = true;
            Object chosen = monthCombo.getSelectedItem();
            Set<String> months = new LinkedHashSet<>();
            for (int i = allEntries.size() - 1; i >= 0; i--) {
                months.add(DateUtil.toMonth(allEntries.get(i).getDate()));
            }
            monthCombo.removeAllItems();
            monthCombo.addItem("All months");
            for (String m : months) {
                monthCombo.addItem(m);
            }
            monthCombo.setSelectedItem(chosen != null && months.contains(chosen) ? chosen : "All months");
            loading = false;

            fillTable();
        } catch (SQLException e) {
            loading = false;
            showError("Could not load the journal from MySQL.", e);
        }
    }

    /** Rebuilds the table rows using the search text and the chosen month */
    private void fillTable() {
        String search = searchField.getText().trim().toLowerCase();
        String month = (String) monthCombo.getSelectedItem();

        model.clear();
        rowKinds.clear();
        shownEntries.clear();
        java.util.Map<Integer, String[]> chips = new java.util.TreeMap<>();   // code -> {name, sides}
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int count = 0;
        java.util.Set<Integer> accountsUsed = new java.util.HashSet<>();

        for (JournalEntry e : allEntries) {
            boolean monthMatches = month == null || month.equals("All months") || DateUtil.toMonth(e.getDate()).equals(month);
            boolean searchMatches = search.isEmpty()
                    || e.getDescription().toLowerCase().contains(search)
                    || e.getReferenceNo().toLowerCase().contains(search);
            if (!monthMatches || !searchMatches) {
                continue;
            }
            count++;
            shownEntries.add(e);

            boolean firstLine = true;
            for (JournalLine line : e.getLines()) {
                String date = firstLine ? DateUtil.toShort(e.getDate()) : "";
                String code = String.valueOf(line.getAccount().getCode());
                accountsUsed.add(line.getAccount().getCode());
                String[] chip = chips.computeIfAbsent(line.getAccount().getCode(),
                        k -> new String[] {line.getAccount().getName(), ""});
                String side = line.isDebit() ? "Dr" : "Cr";
                if (!chip[1].contains(side)) {
                    chip[1] = chip[1].isEmpty() ? side : "Dr/Cr";
                }
                if (line.isDebit()) {
                    model.addRow(new Object[] {date, line.getAccount().getName(), code, Money.peso(line.getDebit()), ""});
                    rowKinds.add(DEBIT_ROW);
                } else {
                    model.addRow(new Object[] {date, line.getAccount().getName(), code, "", Money.peso(line.getCredit())});
                    rowKinds.add(CREDIT_ROW);
                }
                firstLine = false;
            }
            String memo = "(" + e.getDescription() + ")   " + e.getReferenceNo()
                    + (e.isAdjusting() ? "   - adjusting entry" : "");
            model.addRow(new Object[] {"", memo, "", "", ""});
            rowKinds.add(MEMO_ROW);

            totalDebit = totalDebit.add(e.getTotalDebit());
            totalCredit = totalCredit.add(e.getTotalCredit());
        }

        fillChips(chips);
        totalModel.clear();
        totalModel.addRow(new Object[] {"Total", "", "", Money.peso(totalDebit), Money.peso(totalCredit)});
        totalStrip.setVisible(count > 0);
        emptyLabel.setVisible(count == 0);

        card.setSubtitle((month == null ? "All months" : month) + " - " + count + (count == 1 ? " entry" : " entries"));
        entriesLabel.setText("<html>Entries <b>" + count + "</b></html>");
        entriesTile.setValue(String.valueOf(count));
        debitTile.setValue(Money.peso(totalDebit));
        creditTile.setValue(Money.peso(totalCredit));
        accountsTile.setValue(String.valueOf(accountsUsed.size()));
        debitLabel.setText("<html>Total debit <b>" + Money.peso(totalDebit) + "</b></html>");
        creditLabel.setText("<html>Total credit <b>" + Money.peso(totalCredit) + "</b></html>");
        if (totalDebit.compareTo(totalCredit) == 0) {
            balanceBadge.showOk("In balance");
        } else {
            balanceBadge.showBad("Out of balance by " + Money.peso(totalDebit.subtract(totalCredit).abs()));
        }
        revalidate();
        repaint();
    }

    /** The "ACCOUNTS IN THIS JOURNAL" line: violet chips for debits, cream chips for credits */
    private void fillChips(java.util.Map<Integer, String[]> chips) {
        accountChips.removeAll();
        accountChips.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xe6ddf6)),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        JLabel title = new JLabel("ACCOUNTS IN THIS JOURNAL");
        title.setFont(Theme.spaced(Theme.semibold(11.5f), 0.08f));
        title.setForeground(Theme.PURPLE_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 4));
        accountChips.add(title);
        StringBuilder all = new StringBuilder("<html><b>Accounts in this journal</b>");
        for (String[] chip : chips.values()) {
            boolean credit = chip[1].equals("Cr");
            Badge b = new Badge(chip[0] + " · " + chip[1], credit ? Theme.AMBER_BG : new Color(0xf0ecfb),
                    credit ? Theme.AMBER : new Color(0x5b3fb8));
            b.setFont(Theme.semibold(12.5f));
            accountChips.add(b);
            all.append("<br>").append(chip[0].replace("<", "&lt;")).append(" · ").append(chip[1]);
        }
        Badge more = new Badge("", Theme.PURPLE_TINT, Theme.PURPLE_DARK);
        more.setFont(Theme.semibold(12.5f));
        more.setToolTipText(all.append("</html>").toString());
        accountChips.add(more);          // "+12 more" when the chips do not fit on two lines
        accountChips.setVisible(!chips.isEmpty());
        accountChips.revalidate();
        accountChips.repaint();
    }

    // =================================================================
    //  Export (CSV, colored Excel, PDF / print) of the entries shown
    // =================================================================

    private String filterLabel() {
        Object month = monthCombo.getSelectedItem();
        String label = month == null ? "All months" : month.toString();
        String search = searchField.getText().trim();
        return search.isEmpty() ? label : label + " · search \"" + search + "\"";
    }

    private void export(String extension) {
        if (shownEntries.isEmpty()) {
            showInfo("There are no journal entries to export yet.");
            return;
        }
        String base = "TALA-General-Journal-" + filterLabel().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+$", "");
        java.io.File file = chooseExportFile("Export the General Journal", base, extension);
        if (file == null) {
            return;
        }
        try {
            if (extension.equals("xlsx")) {
                tala.service.ReportExcel.writeJournal(shownEntries, filterLabel(), file);
            } else {
                writeCsv(file);
            }
            showSaved(file);
        } catch (java.io.IOException ex) {
            showError("Could not save the file. If it is open in Excel, close it and try again.", ex);
        }
    }

    /** Plain CSV: one line per debit / credit, with the description and reference */
    private void writeCsv(java.io.File file) throws java.io.IOException {
        try (java.io.PrintWriter out = new java.io.PrintWriter(file, "UTF-8")) {
            out.print('﻿');   // lets Excel read the peso sign and accents correctly
            out.println("Date,Account,Post ref,Debit,Credit,Description,Reference,Type");
            BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
            for (JournalEntry e : shownEntries) {
                for (JournalLine line : e.getLines()) {
                    out.println(csv(DateUtil.toInput(e.getDate())) + "," + csv(line.getAccount().getName()) + ","
                            + line.getAccount().getCode() + "," + csvAmount(line.getDebit()) + ","
                            + csvAmount(line.getCredit()) + "," + csv(e.getDescription()) + ","
                            + csv(e.getReferenceNo()) + "," + csv(e.getEntryType()));
                }
                dr = dr.add(e.getTotalDebit());
                cr = cr.add(e.getTotalCredit());
            }
            out.println("Total,,," + csvAmount(dr) + "," + csvAmount(cr) + ",,,");
        }
    }

    private void printJournal() {
        if (shownEntries.isEmpty()) {
            showInfo("There are no journal entries to print yet.");
            return;
        }
        try {
            tala.service.ReportPrint.journal(shownEntries, filterLabel());
        } catch (java.io.IOException ex) {
            showError("Could not open the print page.", ex);
        }
    }

    private static JLabel footLabel() {
        JLabel label = new JLabel();
        label.setFont(Theme.regular(14.5f));
        label.setForeground(Theme.TEXT_SOFT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));
        return label;
    }

    /**
     * The chips line: chips flow onto at most two lines; whatever does not fit
     * is summed up in a last "+N more" chip (its tooltip lists every account).
     * The last child is that "+N more" chip.
     */
    private static class ChipStrip extends JPanel {
        private static final int GAP = 8, ROW_GAP = 6, MAX_ROWS = 2;

        ChipStrip() {
            super(null);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            int rows = place(false);
            java.awt.Insets in = getInsets();
            int h = rows == 0 ? 0 : rows * 26 + (rows - 1) * ROW_GAP;
            return new Dimension(100, in.top + in.bottom + h);
        }

        @Override
        public void doLayout() {
            place(true);
        }

        /** Places the chips (or only counts the lines); returns the number of lines used */
        private int place(boolean apply) {
            int n = getComponentCount();
            if (n == 0) {
                return 0;
            }
            java.awt.Insets in = getInsets();
            int width = getWidth() > 0 ? getWidth() : (getParent() != null ? getParent().getWidth() : 900);
            int max = Math.max(200, width - in.left - in.right);
            Badge more = (Badge) getComponent(n - 1);
            // how many of the chips (title + chips, not "more") fit
            int x = 0, row = 0, fit = 0;
            for (int i = 0; i < n - 1; i++) {
                Dimension d = getComponent(i).getPreferredSize();
                if (x > 0 && x + d.width > max) {
                    row++;
                    x = 0;
                }
                if (row >= MAX_ROWS) {
                    break;
                }
                x += d.width + GAP;
                fit++;
            }
            int hidden = (n - 1) - fit;
            if (hidden > 0) {
                // make room for "+N more" at the end of the last line
                while (true) {
                    more.setText("+" + hidden + " more");
                    int mx = 0, r = 0;
                    for (int i = 0; i < fit; i++) {
                        Dimension d = getComponent(i).getPreferredSize();
                        if (mx > 0 && mx + d.width > max) { r++; mx = 0; }
                        mx += d.width + GAP;
                    }
                    if (mx + more.getPreferredSize().width <= max || fit <= 1) {
                        break;
                    }
                    fit--;
                    hidden++;
                }
            }
            x = 0;
            row = 0;
            int y = in.top;
            for (int i = 0; i < n; i++) {
                Component c = getComponent(i);
                boolean show = i < fit || (i == n - 1 && hidden > 0);
                if (apply) {
                    c.setVisible(show);
                }
                if (!show) {
                    continue;
                }
                Dimension d = c.getPreferredSize();
                if (x > 0 && x + d.width > max) {
                    row++;
                    x = 0;
                    y += 26 + ROW_GAP;
                }
                if (apply) {
                    c.setBounds(in.left + x, y + (26 - d.height) / 2, d.width, d.height);
                }
                x += d.width + GAP;
            }
            return row + 1;
        }
    }
}
