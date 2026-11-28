package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.Worksheet;
import tala.model.WorksheetRow;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.ExportMenu;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.RoundedPanel;
import tala.ui.theme.Theme;
import tala.util.Money;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static tala.model.WorksheetRow.*;

/**
 * ColumnarPage  (the 12 Columnar worksheet)
 * ------------------------------------------------------------
 * Unadjusted TB | Adjustments | Adjusted TB | Income statement | Owner's equity | Balance sheet
 *   (each with a Dr and a Cr column = 12 money columns)
 *
 * Light design ("professional 12-column worksheet"):
 *   - every column group has its own color band: violet, amber, blue,
 *     green, rose and teal (a colored line on top, tinted cells below)
 *   - Code and Account stay in place while the numbers scroll sideways,
 *     and the two-row header stays on top while the rows scroll
 *   - a firm line between groups, a dashed line between Dr and Cr
 *   - total rows: a dark line above, the grand total with a double line
 *
 * It starts EMPTY and lists only the accounts that were actually used.
 * The math is done by AccountingService.buildWorksheet().
 */
public class ColumnarPage extends BasePage {

    private static final String[] SHORT_GROUPS = {"UNADJUSTED TB", "ADJUSTMENTS", "ADJUSTED TB",
            "INCOME STMT.", "OWNER'S EQUITY", "BALANCE SHEET"};
    private static final String[] GROUPS = {"Unadjusted trial balance", "Adjustments", "Adjusted trial balance",
            "Income statement", "Owner's equity", "Balance sheet"};
    /** Each group's color (text, top line) and its soft tint (cells) */
    static final Color[] INK = {new Color(0x5b21b6), new Color(0xb7791f), new Color(0x3046b8),
            new Color(0x15803d), new Color(0xb4235e), new Color(0x0f766e)};
    static final Color[] TINT = {new Color(0xefe9fd), new Color(0xfdf4dc), new Color(0xe8ecfc),
            new Color(0xe5f5ec), new Color(0xfce8ef), new Color(0xe0f3f2)};

    private static final int CODE_W = 62, NAME_W = 180, ROW_H = 34, HEAD_TOP = 34, HEAD_SUB = 26;
    private static final Color ROW_RULE = new Color(0xddd4ee), NAME_RULE = new Color(0xb9a8e6),
            HEAD_BG = new Color(0xf4f0fc), ZEBRA = new Color(91, 33, 182, 9), HOVER = new Color(124, 58, 237, 26),
            DARK = new Color(0x2e1065), NIL = new Color(0xc9c2da);

    private final ReadOnlyTableModel model;
    private final SheetTable fixedTable;    // Code + Account (stays put)
    private final SheetTable numberTable;   // the 12 money columns (scroll sideways)
    private final List<String> rowKinds = new ArrayList<>();
    private int hoverRow = -1;              // shared by both tables
    private String widestAmount = "000,000.00";   // the longest amount shown (sets the font size)
    private int sizedForWidth = -1;
    private float moneySize = 13f;
    private int nameWidth = NAME_W;
    private Worksheet worksheet = new Worksheet();

    private final JLabel tbTile = tileValue(), adjTile = tileValue(), niTile = tileValue(), diffTile = tileValue();
    private final JLabel niTileTitle = new JLabel("Net income");
    private RoundedPanel tbCard, adjCard, niCard, diffCard;   // the four tiles (for their tooltips)
    private final Card card = new Card("12-column worksheet", "");
    private final JLabel footNet = new JLabel(), footDiff = new JLabel();
    private final Badge balanceBadge = Badge.ok("Balanced");
    private final Badge trialBadge = Badge.ok("Trial balance equal");
    private final JLabel emptyLabel = new JLabel(
            "The worksheet starts empty. Post a journal entry and its accounts appear here.", JLabel.CENTER);

    public ColumnarPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);
        useFixedHeight();

        // 14 columns: Code, Account, then Dr/Cr for each of the 6 groups
        String[] columns = new String[2 + CELL_COUNT];
        columns[0] = "Code";
        columns[1] = "Account";
        for (int c = 0; c < CELL_COUNT; c++) {
            columns[2 + c] = (c % 2 == 0) ? "Dr" : "Cr";
        }
        model = new ReadOnlyTableModel(columns);
        fixedTable = new SheetTable(true);
        numberTable = new SheetTable(false);

        // ---------- header with actions ----------
        PageHeader header = new PageHeader(Icons.TABLE, "12 Columnar",
                "Unadjusted trial balance, adjustments, adjusted trial balance, income statement, "
                + "owner's equity and balance sheet. Amounts in " + Money.PESO_SIGN.trim() + ".");
        header.addAction(new ExportMenu("12-Column Worksheet", () -> export("csv"), () -> export("xlsx"), this::printWorksheet));
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        AppButton addAdjustment = new AppButton("Add adjustment", Icons.get(Icons.PLUS, 16, Color.WHITE), AppButton.Style.PRIMARY);
        addAdjustment.addActionListener(e -> openAdjustmentDialog());
        header.addAction(refresh);
        header.addAction(addAdjustment);
        addSection(header);

        // ---------- four slim tiles ----------
        JPanel tiles = new JPanel(new GridLayout(1, 4, 12, 0));
        tiles.setOpaque(false);
        tbCard = statTile(Icons.SCALE, Theme.PURPLE, new JLabel("Trial balance"), tbTile);
        adjCard = statTile(Icons.SLIDERS, Theme.AMBER, new JLabel("Adjustments"), adjTile);
        niCard = statTile(Icons.PESO, Theme.GREEN, niTileTitle, niTile);
        diffCard = statTile(Icons.LAYERS, Theme.BLUE, new JLabel("Balance sheet difference"), diffTile);
        tiles.add(tbCard);
        tiles.add(adjCard);
        tiles.add(niCard);
        tiles.add(diffCard);
        addSection(tiles);

        // ---------- worksheet card: purple header, legend, the sheet scrolling inside ----------
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legend.setOpaque(false);
        legend.add(legendItem("Debit (Dr)", new Color(0x7c3aed)));
        legend.add(legendItem("Credit (Cr)", new Color(0xf7af1a)));
        legend.add(trialBadge);
        card.addToHeader(legend);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        emptyLabel.setFont(Theme.regular(15));
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));
        body.add(emptyLabel, BorderLayout.NORTH);
        body.add(createWorksheetScroll(), BorderLayout.CENTER);
        card.setBody(body);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JPanel numbers = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        numbers.setOpaque(false);
        for (JLabel l : new JLabel[] {footNet, footDiff}) {
            l.setFont(Theme.regular(13.5f));
            l.setForeground(Theme.FOOT_TEXT);
            l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));
            numbers.add(l);
        }
        footer.add(numbers, BorderLayout.WEST);
        footer.add(balanceBadge, BorderLayout.EAST);
        card.setFooter(footer);
        addFillSection(card);
    }

    private String kind(int row) {
        return row < rowKinds.size() ? rowKinds.get(row) : ACCOUNT;
    }

    /**
     * Code + Account on the left (they stay put), the 12 money columns on the
     * right (they scroll sideways). Both share the same rows and scroll up and
     * down together inside the card; the colored header stays on top.
     */
    private JComponent createWorksheetScroll() {
        // the fixed table shows columns 0-1, the number table shows 2-13 (same model, same rows)
        TableColumnModel fixedColumns = fixedTable.getColumnModel();
        TableColumnModel numberColumns = numberTable.getColumnModel();
        for (int i = numberColumns.getColumnCount() - 1; i >= 0; i--) {
            TableColumn column = numberColumns.getColumn(i);
            if (column.getModelIndex() < 2) {
                numberColumns.removeColumn(column);
            }
        }
        for (int i = fixedColumns.getColumnCount() - 1; i >= 0; i--) {
            TableColumn column = fixedColumns.getColumn(i);
            if (column.getModelIndex() >= 2) {
                fixedColumns.removeColumn(column);
            }
        }
        fixedColumns.getColumn(0).setPreferredWidth(CODE_W);
        fixedColumns.getColumn(1).setPreferredWidth(NAME_W);
        fixedTable.setPreferredScrollableViewportSize(new Dimension(CODE_W + NAME_W, 100));
        for (int i = 0; i < numberColumns.getColumnCount(); i++) {
            numberColumns.getColumn(i).setPreferredWidth(80);
            numberColumns.getColumn(i).setMinWidth(30);   // the 12 columns always share the card's width
        }

        JScrollPane scroll = new JScrollPane(numberTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setRowHeaderView(fixedTable);
        scroll.setColumnHeaderView(new GroupHeader(numberTable));
        scroll.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, new FixedHeader());
        JPanel corner = new JPanel();
        corner.setBackground(HEAD_BG);
        scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getRowHeader().setOpaque(false);
        scroll.getColumnHeader().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(ROW_H);
        // the Account column gives up some width on small windows so the 12 columns still fit
        scroll.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = Math.max(120, Math.min(NAME_W, (int) ((scroll.getWidth() - CODE_W) * 0.19)));
                if (w != nameWidth) {
                    nameWidth = w;
                    fixedTable.getColumnModel().getColumn(1).setPreferredWidth(w);
                    fixedTable.getColumnModel().getColumn(1).setWidth(w);
                    fixedTable.setPreferredScrollableViewportSize(new Dimension(CODE_W + w, 100));
                    scroll.getRowHeader().revalidate();
                    scroll.revalidate();
                }
            }
        });
        return scroll;
    }

    // =================================================================
    //  Data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            List<Account> accounts = accountDAO.getAllAccounts();
            List<JournalEntry> entries = journalDAO.getAllEntries();
            worksheet = accounting.buildWorksheet(accounts, entries);

            model.clear();
            rowKinds.clear();
            int accountRows = 0;
            for (WorksheetRow row : worksheet.getRows()) {
                Object[] values = new Object[2 + CELL_COUNT];
                values[0] = row.getCode();
                values[1] = row.getName();
                for (int c = 0; c < CELL_COUNT; c++) {
                    BigDecimal amount = row.get(c);
                    // the grand total row shows every value, even 0.00; other rows hide zeros
                    values[2 + c] = row.getKind().equals(GRAND_TOTAL) ? Money.format(amount) : Money.formatOrBlank(amount);
                }
                model.addRow(values);
                rowKinds.add(row.getKind());
                if (row.getKind().equals(ACCOUNT)) {
                    accountRows++;
                }
            }
            emptyLabel.setVisible(worksheet.isEmpty());
            widestAmount = "0.00";
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 2; c < model.getColumnCount(); c++) {
                    Object v = model.getValueAt(r, c);
                    if (v != null && v.toString().trim().length() > widestAmount.length()) {
                        widestAmount = v.toString().trim();
                    }
                }
            }
            sizedForWidth = -1;

            String period;
            try {
                period = mainFrame.getSettingsDAO().getAccountingPeriod().getLabel();
            } catch (SQLException e) {
                period = "All dates";
            }
            card.setSubtitle(period + " · " + accountRows + (accountRows == 1 ? " account used" : " accounts used"));
            if (worksheet.isTrialBalanceBalanced()) {
                trialBadge.showOk("Trial balance equal");
            } else {
                trialBadge.showBad("Trial balance not equal");
            }

            // tiles and footer
            tbTile.setText(Money.peso(worksheet.getUnadjustedTotal()));
            adjTile.setText(worksheet.getAdjustingEntryCount() == 0 ? "None yet"
                    : Money.peso(worksheet.getAdjustmentsTotal()) + "  (" + worksheet.getAdjustingEntryCount() + ")");
            boolean loss = worksheet.getNetIncome().signum() < 0;
            niTileTitle.setText(loss ? "Net loss" : "Net income");
            niTile.setText(Money.peso(worksheet.getNetIncome().abs()));
            diffTile.setText(Money.peso(worksheet.getBalanceSheetDifference()));
            updateTileTips(loss);

            footNet.setText("<html>" + (loss ? "Net loss" : "Net income") + " <b style='color:#2e1065'>"
                    + Money.peso(worksheet.getNetIncome().abs()) + "</b></html>");
            footDiff.setText("<html>Balance sheet difference <b style='color:#2e1065'>"
                    + Money.peso(worksheet.getBalanceSheetDifference()) + "</b></html>");
            if (worksheet.isBalanced()) {
                balanceBadge.showOk("Balanced");
            } else {
                balanceBadge.showBad("Not balanced");
            }
            revalidate();
            repaint();
        } catch (SQLException e) {
            showError("Could not build the worksheet from MySQL data.", e);
        }
    }

    /** The explanations shown when the mouse rests on a tile */
    private void updateTileTips(boolean loss) {
        if (worksheet.isEmpty()) {
            String empty = "<html>Nothing posted yet. Post a journal entry first.</html>";
            for (RoundedPanel tile : new RoundedPanel[] {tbCard, adjCard, niCard, diffCard}) {
                tile.setToolTipText(empty);
            }
            return;
        }
        tbCard.setToolTipText("<html><b>Unadjusted trial balance</b><br>"
                + "Total of the Dr column: " + Money.peso(worksheet.getUnadjustedTotal()) + "<br>"
                + (worksheet.isTrialBalanceBalanced() ? "Debits = Credits, so it balances." : "Debits and credits do NOT match.")
                + "</html>");
        adjCard.setToolTipText("<html><b>Adjustments</b><br>"
                + worksheet.getAdjustingEntryCount() + " adjusting "
                + (worksheet.getAdjustingEntryCount() == 1 ? "entry" : "entries")
                + " totaling " + Money.peso(worksheet.getAdjustmentsTotal()) + ".<br>"
                + "Use Add adjustment for supplies used, expired insurance,<br>depreciation and accrued expenses.</html>");
        niCard.setToolTipText("<html><b>" + (loss ? "Net loss" : "Net income") + "</b><br>"
                + "Income statement Cr - Dr (revenue - expenses)<br>"
                + "= " + Money.peso(worksheet.getNetIncome()) + "<br>"
                + "Ending capital: " + Money.peso(worksheet.getEndingCapital()) + "</html>");
        diffCard.setToolTipText("<html><b>Balance sheet difference</b><br>"
                + "Balance sheet Dr - (Cr + ending capital).<br>"
                + (worksheet.isBalanced() ? "It is " + Money.peso(BigDecimal.ZERO) + ", so the books balance."
                                          : "It should be " + Money.peso(BigDecimal.ZERO) + ". Check your entries.")
                + "</html>");
    }

    /** Opens the Add adjustment pop-up; reloads the worksheet if one was added */
    public void openAdjustmentDialog() {
        try {
            AddAdjustmentDialog dialog = new AddAdjustmentDialog(mainFrame, accountDAO, journalDAO);
            dialog.setVisible(true);   // waits here until the dialog closes
            if (dialog.wasPosted()) {
                refreshData();
            }
        } catch (SQLException e) {
            showError("Could not open the adjustment form.", e);
        }
    }

    // =================================================================
    //  Export: CSV, colored Excel, PDF / print
    // =================================================================

    private String periodLabel() {
        try {
            return mainFrame.getSettingsDAO().getAccountingPeriod().getLabel();
        } catch (SQLException e) {
            return "-";
        }
    }

    private void export(String extension) {
        if (worksheet.isEmpty()) {
            showInfo("The worksheet is empty, so there is nothing to export yet.");
            return;
        }
        File file = chooseExportFile("Export the 12-column worksheet",
                "TALA-12-Column-Worksheet-" + periodLabel().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+$", ""), extension);
        if (file == null) {
            return;
        }
        try {
            if (extension.equals("csv")) {
                writeCsv(file);
            } else {
                tala.service.WorksheetExcel.write(worksheet, periodLabel(), file);
            }
            showSaved(file);
        } catch (IOException e) {
            showError("Could not save the file. If it is open in Excel, close it and try again.", e);
        }
    }

    /** The plain CSV version (no colors: CSV files cannot hold any) */
    private void writeCsv(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            out.print('﻿');
            out.println(",,Unadjusted,,Adjustments,,Adjusted,,Income statement,,Owner's equity,,Balance sheet,");
            out.println("Code,Account,Dr,Cr,Dr,Cr,Dr,Cr,Dr,Cr,Dr,Cr,Dr,Cr");
            for (WorksheetRow row : worksheet.getRows()) {
                StringBuilder line = new StringBuilder();
                line.append(csv(row.getCode())).append(',').append(csv(row.getName()));
                for (int c = 0; c < CELL_COUNT; c++) {
                    BigDecimal amount = row.get(c);
                    line.append(',').append(amount == null ? "" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                }
                out.println(line);
            }
        }
    }

    private void printWorksheet() {
        if (worksheet.isEmpty()) {
            showInfo("The worksheet is empty, so there is nothing to print yet.");
            return;
        }
        try {
            tala.service.ReportPrint.worksheet(worksheet, periodLabel());
        } catch (IOException e) {
            showError("Could not open the print page.", e);
        }
    }

    // =================================================================
    //  The two tables
    // =================================================================

    /** One of the two worksheet tables; both draw their cells with WorksheetCell */
    private class SheetTable extends JTable {
        private final boolean fixed;

        SheetTable(boolean fixed) {
            super(model);
            this.fixed = fixed;
            setRowHeight(ROW_H);
            setShowGrid(false);
            setIntercellSpacing(new Dimension(0, 0));
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setFocusable(false);
            setRowSelectionAllowed(false);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setAutoResizeMode(fixed ? AUTO_RESIZE_OFF : AUTO_RESIZE_ALL_COLUMNS);
            setTableHeader(null);
            setDefaultRenderer(Object.class, new WorksheetCell());
            MouseAdapter hover = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { setHover(rowAtPoint(e.getPoint())); }
                @Override public void mouseExited(MouseEvent e) { setHover(-1); }
            };
            addMouseMotionListener(hover);
            addMouseListener(hover);
        }

        // we place our own colored header in the scroll pane: stop JTable from replacing it with none
        @Override
        protected void configureEnclosingScrollPane() { }

        // the number columns always fill exactly the card's width
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return !fixed;   // fixed width: all 12 columns fit the card, no sideways scrolling
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int row = rowAtPoint(e.getPoint()), col = columnAtPoint(e.getPoint());
            if (fixed || row < 0 || col < 0) {
                return null;
            }
            int c = convertColumnIndexToModel(col) - 2;
            return GROUPS[c / 2] + " · " + (c % 2 == 0 ? "Debit" : "Credit") + " · " + model.getValueAt(row, 1);
        }
    }

    /** The row under the mouse, shared by both tables so the whole row lights up */
    private void setHover(int row) {
        if (row == hoverRow) {
            return;
        }
        int old = hoverRow;
        hoverRow = row;
        for (SheetTable t : new SheetTable[] {fixedTable, numberTable}) {
            for (int r : new int[] {old, row}) {
                if (r >= 0 && r < t.getRowCount()) {
                    t.repaint(0, r * ROW_H, t.getWidth(), ROW_H);
                }
            }
        }
    }

    /** Draws one worksheet cell: band tint, text, and the grid lines */
    private class WorksheetCell extends JComponent implements TableCellRenderer {
        private String text = "";
        private int modelColumn, row;
        private final Font plain = Theme.regular(13f), bold = Theme.semibold(13f), codeFont = Theme.mono(11.5f);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            this.text = value == null ? "" : value.toString().trim();
            this.modelColumn = table.convertColumnIndexToModel(column);
            this.row = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            int w = getWidth(), h = getHeight();
            String kind = kind(row);
            boolean account = kind.equals(ACCOUNT), grand = kind.equals(GRAND_TOTAL);
            boolean firstTotal = !account && (row == 0 || kind(row - 1).equals(ACCOUNT));
            boolean money = modelColumn >= 2;
            int c = modelColumn - 2, group = money ? c / 2 : -1;
            boolean nil = money && (text.isEmpty() || text.equals("-"));

            // 1) background
            Color bg;
            if (!money) {
                bg = grand ? new Color(0xefe7ff) : account ? Color.WHITE : new Color(0xfaf8ff);
            } else if (grand && c >= BS_DR) {
                bg = worksheet.isBalanced() ? Theme.GREEN_BG : Theme.RED_BG;     // the final balance sheet pair
            } else {
                bg = Theme.mix(Color.WHITE, TINT[group], account ? 0.45f : grand ? 1f : 0.7f);
            }
            g2.setColor(bg);
            g2.fillRect(0, 0, w, h);
            if (account && row % 2 == 1) {
                g2.setColor(ZEBRA);
                g2.fillRect(0, 0, w, h);
            }
            boolean hovered = row == hoverRow && account;
            if (hovered) {
                g2.setColor(HOVER);
                g2.fillRect(0, 0, w, h);
            }

            // 2) lines: rows, groups (firm), Dr | Cr (dashed), the account column edge
            g2.setColor(hovered ? new Color(0xb9a3f3) : ROW_RULE);
            g2.fillRect(0, h - 1, w, 1);
            if (firstTotal) {
                g2.setColor(DARK);
                g2.fillRect(0, 0, w, 2);
            }
            if (grand) {
                g2.setColor(DARK);
                g2.fillRect(0, h - 4, w, 1);
                g2.fillRect(0, h - 1, w, 1);
            }
            if (money) {
                if (c % 2 == 0) {
                    g2.setColor(Theme.mix(new Color(0xcfc4e6), INK[group], 0.45f));
                    g2.fillRect(0, 0, 2, h);
                } else {
                    g2.setColor(new Color(0xd4cbe8));
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {3f, 3f}, 0f));
                    g2.drawLine(0, 0, 0, h);
                }
            } else if (modelColumn == 1) {
                g2.setColor(NAME_RULE);
                g2.fillRect(w - 2, 0, 2, h);
            } else {
                g2.setColor(new Color(0xe2daf2));
                g2.fillRect(w - 1, 0, 1, h);
            }

            // 3) text
            if (modelColumn == 0) {
                if (!text.isEmpty()) {                       // the code in a small violet chip
                    g2.setFont(codeFont);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(text), pw = tw + 12, ph = 20;
                    int px = 12, py = (h - ph) / 2;
                    g2.setColor(new Color(0xf1ebff));
                    g2.fillRoundRect(px, py, pw, ph, 10, 10);
                    g2.setColor(Theme.PURPLE);
                    g2.drawString(text, px + 6, py + (ph + fm.getAscent() - fm.getDescent()) / 2);
                }
            } else {
                g2.setFont(account && modelColumn == 1 ? Theme.medium(13f) : account ? plain : bold);
                FontMetrics fm = g2.getFontMetrics();
                String shown = nil ? "–" : text;
                Color ink;
                if (nil) {
                    ink = NIL;
                } else if (!money) {
                    ink = account ? new Color(0x2a1f45) : DARK;
                } else if (grand && c >= BS_DR) {
                    ink = worksheet.isBalanced() ? Theme.GREEN : Theme.RED;
                } else if (account) {
                    ink = c % 2 == 0 ? Theme.DEBIT : Theme.CREDIT;     // Dr violet, Cr gold
                } else {
                    ink = DARK;
                }
                g2.setColor(ink);
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2 - (grand ? 1 : 0);
                if (money) {
                    // every amount uses ONE size, chosen so the widest amount fits the column
                    if (w != sizedForWidth) {
                        moneySize = fit(g2, Theme.semibold(13f), widestAmount, w - 12, 1).getSize2D();
                        sizedForWidth = w;
                    }
                    g2.setFont(account ? Theme.regular(moneySize) : Theme.semibold(moneySize));
                    fm = g2.getFontMetrics();
                    ty = (h + fm.getAscent() - fm.getDescent()) / 2 - (grand ? 1 : 0);
                    g2.drawString(shown, w - 6 - fm.stringWidth(shown), ty);
                } else {
                    // shorten long account names with "..." so they never run under the numbers
                    int max = w - 24;
                    String t = shown;
                    if (fm.stringWidth(t) > max) {
                        while (t.length() > 1 && fm.stringWidth(t + "…") > max) {
                            t = t.substring(0, t.length() - 1);
                        }
                        t = t + "…";
                    }
                    g2.drawString(t, 12, ty);
                }
            }
            g2.dispose();
        }
    }

    // =================================================================
    //  Headers
    // =================================================================

    /**
     * The colored two-row header over the money columns:
     *   | UNADJUSTED TRIAL BALANCE |  ADJUSTMENTS  | ...    (tinted, a colored line on top)
     *   |        DR   |      CR    |   DR  |   CR  | ...
     * It reads the column widths from the table so it always lines up.
     */
    private static class GroupHeader extends JComponent {
        private final JTable table;

        GroupHeader(JTable table) {
            this.table = table;
            table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
                public void columnMarginChanged(ChangeEvent e) { revalidate(); repaint(); }
                public void columnAdded(TableColumnModelEvent e) { }
                public void columnRemoved(TableColumnModelEvent e) { }
                public void columnMoved(TableColumnModelEvent e) { }
                public void columnSelectionChanged(ListSelectionEvent e) { }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(table.getColumnModel().getTotalColumnWidth(), HEAD_TOP + HEAD_SUB);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            TableColumnModel columns = table.getColumnModel();
            g2.setColor(HEAD_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int[] lefts = new int[columns.getColumnCount() + 1];
            int x = 0;
            for (int i = 0; i < columns.getColumnCount(); i++) {
                lefts[i] = x;
                x += columns.getColumn(i).getWidth();
            }
            lefts[columns.getColumnCount()] = x;

            Font groupFont = Theme.spaced(Theme.semibold(11f), 0.06f);
            Font subFont = Theme.spaced(Theme.semibold(10.5f), 0.1f);
            for (int gIndex = 0; gIndex < GROUPS.length && 2 * gIndex + 1 < columns.getColumnCount(); gIndex++) {
                int left = lefts[gIndex * 2], mid = lefts[gIndex * 2 + 1], right = lefts[gIndex * 2 + 2];
                Color ink = INK[gIndex], tint = TINT[gIndex];
                // group title band
                g2.setColor(tint);
                g2.fillRect(left, 0, right - left, HEAD_TOP + HEAD_SUB);
                g2.setColor(ink);
                g2.fillRect(left, 0, right - left, 3);
                g2.setFont(groupFont);
                FontMetrics fm = g2.getFontMetrics();
                String title = GROUPS[gIndex].toUpperCase(Locale.ROOT);
                if (fm.stringWidth(title) > right - left - 10) {          // narrow: the short name
                    title = SHORT_GROUPS[gIndex];
                }
                if (fm.stringWidth(title) > right - left - 10) {
                    g2.setFont(fit(g2, Theme.semibold(11f), title, right - left - 10, 1));
                    fm = g2.getFontMetrics();
                }
                g2.drawString(title, left + Math.max(6, (right - left - fm.stringWidth(title)) / 2),
                        3 + (HEAD_TOP - 3 + fm.getAscent() - fm.getDescent()) / 2);
                // Dr / Cr labels on a slightly deeper line
                g2.setColor(Theme.mix(tint, ink, 0.08f));
                g2.fillRect(left, HEAD_TOP, right - left, HEAD_SUB);
                g2.setFont(subFont);
                fm = g2.getFontMetrics();
                g2.setColor(ink);
                int ty = HEAD_TOP + (HEAD_SUB + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("DR", mid - 10 - fm.stringWidth("DR"), ty);
                g2.drawString("CR", right - 10 - fm.stringWidth("CR"), ty);
                // lines: firm between groups, dashed between Dr and Cr, under the header
                g2.setColor(Theme.mix(Color.WHITE, ink, 0.45f));
                g2.fillRect(left, 0, 2, HEAD_TOP + HEAD_SUB);
                g2.setColor(Theme.mix(Color.WHITE, ink, 0.3f));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {3f, 3f}, 0f));
                g2.drawLine(mid, HEAD_TOP, mid, HEAD_TOP + HEAD_SUB);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Theme.mix(Color.WHITE, ink, 0.35f));
                g2.fillRect(left, HEAD_TOP + HEAD_SUB - 2, right - left, 2);
            }
            g2.dispose();
        }
    }

    /** The header over Code and Account (stays put, like the columns under it) */
    private class FixedHeader extends JComponent {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(CODE_W + nameWidth, HEAD_TOP + HEAD_SUB);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            int h = getHeight();
            g2.setColor(HEAD_BG);
            g2.fillRect(0, 0, getWidth(), h);
            g2.setFont(Theme.spaced(Theme.semibold(11f), 0.06f));
            g2.setColor(new Color(0x3b1578));
            int ty = h - 10;
            g2.drawString("CODE", 12, ty);
            g2.drawString("ACCOUNT", CODE_W + 12, ty);
            g2.setColor(new Color(0xd9cff3));
            g2.fillRect(0, h - 2, getWidth(), 2);
            g2.setColor(NAME_RULE);
            g2.fillRect(getWidth() - 2, 0, 2, h);
            g2.setColor(new Color(0xe2daf2));
            g2.fillRect(CODE_W - 1, 0, 1, h);
            g2.dispose();
        }
    }

    // =================================================================
    //  Small parts
    // =================================================================

    /** "o Debit (Dr)" in the card header */
    private static JLabel legendItem(String text, Color dot) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.medium(12.5f));
        label.setIconTextGap(7);
        label.setIcon(new Icon() {
            public int getIconWidth()  { return 8; }
            public int getIconHeight() { return 8; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dot);
                g2.fillOval(x, y, 8, 8);
                g2.dispose();
            }
        });
        return label;
    }

    private static JLabel tileValue() {
        JLabel label = new JLabel("-");
        label.setFont(Theme.bold(16));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));   // room for the last glyph
        label.setForeground(Theme.TITLE);
        return label;
    }

    /**
     * One slim colored tile (white -> soft tint, colored outline, white icon box).
     * It is hoverable: it lifts and its border turns its color under the mouse.
     */
    private RoundedPanel statTile(String icon, Color accentColor, JLabel title, JLabel value) {
        RoundedPanel tile = new RoundedPanel(new BorderLayout(10, 0));
        Color[] tint = accentColor.equals(Theme.AMBER) ? new Color[] {Theme.TINT_YELLOW, Theme.EDGE_YELLOW, Theme.TONE_YELLOW}
                : accentColor.equals(Theme.GREEN) ? new Color[] {Theme.TINT_GREEN, Theme.EDGE_GREEN, Theme.TONE_GREEN}
                : accentColor.equals(Theme.BLUE) ? new Color[] {Theme.TINT_BLUE, Theme.EDGE_BLUE, Theme.TONE_BLUE}
                : new Color[] {Theme.TINT_PURPLE, Theme.EDGE_PURPLE, Theme.TONE_PURPLE};
        tile.setRadius(14).setFill(Color.WHITE).setGradient(tint[0], 0f);
        tile.setBorderColor(tint[1]);
        tile.setHoverable(tint[1]);
        tile.setLift(true);
        tile.setBorder(BorderFactory.createEmptyBorder(3 + 8, 3 + 12, 9 + 8, 3 + 12));
        title.setFont(Theme.medium(12f));
        title.setForeground(Theme.TEXT_SOFT);
        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.setOpaque(false);
        texts.add(title);
        texts.add(value);
        JPanel iconHolder = new JPanel(new java.awt.GridBagLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(new IconBox(icon, 30, Color.WHITE, tint[2]).setOutline(new Color(46, 16, 101, 20)));
        tile.add(iconHolder, BorderLayout.WEST);
        tile.add(texts, BorderLayout.CENTER);
        title.setHorizontalAlignment(SwingConstants.LEFT);
        return tile;
    }

    /** The largest size (down to 9px) at which the text fits; weight 0 = regular, 1 = semibold */
    private static Font fit(Graphics2D g, Font font, String text, int maxWidth, int weight) {
        Font f = font;
        for (float size = font.getSize2D() - 0.5f; size >= 7.5f; size -= 0.5f) {
            if (g.getFontMetrics(f).stringWidth(text) <= maxWidth) {
                break;
            }
            f = weight == 0 ? Theme.regular(size) : Theme.semibold(size);
        }
        return f;
    }
}
