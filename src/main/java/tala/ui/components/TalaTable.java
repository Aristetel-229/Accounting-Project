package tala.ui.components;

import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * TalaTable
 * ------------------------------------------------------------
 * A JTable with the light TALA look: lilac header (#efe8ff) with deep
 * purple column names, airy white rows with hairline dividers,
 * right-aligned money columns and colored "pill" columns.
 *
 * Hover effect: the row under the mouse turns a soft lilac (#f8f4ff)
 * and its pills get a little stronger.
 *
 * Pages can override the small "hook" methods below to color
 * or bold special rows (for example a Total row).
 */
public class TalaTable extends JTable {

    /** The hairline under each row (a touch deeper than the lavender card) */
    private static final Color ROW_RULE = new Color(0xe8e0f7);

    private final Set<Integer> rightAligned = new HashSet<>();
    private final Set<Integer> pillColumns = new HashSet<>();
    private final Set<Integer> wrapColumns = new HashSet<>();
    private int baseRowHeight = 42;
    private float fontSize = 14f;
    private int cellPadding = 16;
    private int hoverRow = -1;

    public TalaTable(TableModel model) {
        super(model);
        setFont(Theme.regular(14f));
        setForeground(Theme.TEXT);
        setRowHeight(42);
        setShowVerticalLines(false);
        setShowHorizontalLines(false);          // our renderer draws the row lines
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultRenderer(Object.class, new Renderer());
        setOpaque(false);                         // the white card shows behind the rows
        setBackground(new Color(0, 0, 0, 0));
        setSelectionBackground(Theme.SELECTED_ROW);
        setGridColor(ROW_RULE);

        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(10, 38));
        header.setOpaque(true);
        header.setBackground(Theme.HEAD_BG);

        // row hover
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoverRow(rowAtPoint(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoverRow(-1);
            }
        };
        addMouseMotionListener(hover);
        addMouseListener(hover);
    }

    private void setHoverRow(int row) {
        if (row == hoverRow) {
            return;
        }
        int old = hoverRow;
        hoverRow = row;
        repaintRow(old);
        repaintRow(row);
        setCursor(row >= 0 && clickableRows
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    private boolean clickableRows = false;

    /** Pages that open something on a row click: shows a hand cursor over the rows */
    public void setClickableRows(boolean clickable) {
        this.clickableRows = clickable;
    }

    private void repaintRow(int row) {
        if (row >= 0 && row < getRowCount()) {
            repaint(getCellRect(row, 0, true).union(getCellRect(row, getColumnCount() - 1, true)));
        }
    }

    /** The row under the mouse (-1 = none) */
    public int getHoverRow() {
        return hoverRow;
    }

    // ------------------------------------------------------------ setup helpers

    /** Money columns: right-aligned numbers */
    public void setRightAligned(int... columns) {
        for (int c : columns) {
            rightAligned.add(c);
        }
    }

    /** Columns whose text is drawn as a colored pill (types, references, status) */
    public void setPillColumns(int... columns) {
        for (int c : columns) {
            pillColumns.add(c);
        }
    }

    /** Smaller text and padding, for wide tables like the 12 Columnar worksheet */
    public void setCompact(float fontSize, int cellPadding) {
        this.fontSize = fontSize;
        this.cellPadding = cellPadding;
        setFont(Theme.regular(fontSize));
    }

    /** Preferred widths in pixels; use 0 to leave a column flexible */
    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < getColumnCount(); i++) {
            if (widths[i] > 0) {
                TableColumn col = getColumnModel().getColumn(i);
                col.setPreferredWidth(widths[i]);
                col.setMaxWidth(widths[i] + 60);
            }
        }
    }

    /**
     * The table in its own scroll area (header stays on top, rows scroll),
     * for cards that keep a fixed size and scroll on the inside.
     */
    public javax.swing.JScrollPane inScroll() {
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(this);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        sp.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        JPanel corner = new JPanel();
        corner.setBackground(Theme.HEAD_BG);
        sp.setCorner(javax.swing.ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
        return sp;
    }

    /** Columns whose long text wraps onto a second line (the row grows taller) */
    public void setWrapColumns(int... columns) {
        for (int c : columns) {
            wrapColumns.add(c);
        }
        baseRowHeight = getRowHeight();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                fitRowHeights();
            }
        });
        getModel().addTableModelListener(e -> javax.swing.SwingUtilities.invokeLater(this::fitRowHeights));
    }

    /** Makes rows with wrapped text tall enough for every line */
    public void fitRowHeights() {
        if (wrapColumns.isEmpty()) {
            return;
        }
        java.awt.FontMetrics fm = getFontMetrics(Theme.regular(fontSize));
        for (int row = 0; row < getRowCount(); row++) {
            int lines = 1;
            for (int view = 0; view < getColumnCount(); view++) {
                int model = convertColumnIndexToModel(view);
                if (!wrapColumns.contains(model)) {
                    continue;
                }
                Object v = getValueAt(row, view);
                int avail = getColumnModel().getColumn(view).getWidth() - cellPadding * 2 - 4;
                if (v != null && avail > 20) {
                    lines = Math.max(lines, wrappedLines(v.toString(), fm, avail));
                }
            }
            int h = lines <= 1 ? baseRowHeight : 18 + lines * (fm.getHeight() + 1);
            if (getRowHeight(row) != h) {
                setRowHeight(row, h);
            }
        }
    }

    private static int wrappedLines(String text, java.awt.FontMetrics fm, int width) {
        int lines = 1, lineW = 0;
        for (String word : text.split(" ")) {
            int w = fm.stringWidth(word + " ");
            if (lineW > 0 && lineW + w > width) {
                lines++;
                lineW = 0;
            }
            lineW += w;
        }
        return lines;
    }

    /** The table plus its header in one panel, without a scroll bar (the page scrolls instead) */
    public JComponent inPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(getTableHeader(), BorderLayout.NORTH);
        panel.add(this, BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------ hooks (override in pages)

    /** Background of a row. Default: see-through (white card), or lilac when selected. */
    protected Color rowBackground(int row, boolean selected) {
        return selected ? Theme.SELECTED_ROW : Color.WHITE;
    }

    /** Background of one cell. Default: the row's background. */
    protected Color cellBackground(int row, int column, boolean selected) {
        return rowBackground(row, selected);
    }

    /** Should this row be bold? */
    protected boolean rowBold(int row) {
        return false;
    }

    /** Should this one cell be bold? Default: when its row is bold. */
    protected boolean cellBold(int row, int column) {
        return rowBold(row);
    }

    /** Text color of one cell */
    protected Color cellForeground(int row, int column) {
        return Theme.TEXT;
    }

    /** Extra left padding for one cell (used to indent credit lines) */
    protected int cellIndent(int row, int column) {
        return 0;
    }

    /** Pill colors for a text: { background, text color } */
    protected Color[] pillColors(String text) {
        switch (text) {
            case "Asset":     return new Color[] {new Color(0xf0ecfb), new Color(0x5b3fb8)};
            case "Liability": return new Color[] {Theme.AMBER_BG, Theme.AMBER};
            case "Equity":    return new Color[] {Theme.BLUE_BG, Theme.BLUE};
            case "Revenue":   return new Color[] {Theme.GREEN_BG, Theme.GREEN};
            case "Expense":   return new Color[] {Theme.RED_BG, Theme.RED};
            case "Adjusting":
            case "New":       return new Color[] {Theme.AMBER_BG, Theme.AMBER};
            case "Active":    return new Color[] {Theme.GREEN_BG, Theme.GREEN};
            default:          return new Color[] {Theme.PURPLE_TINT, Theme.PURPLE_DARK};   // references, "Regular"
        }
    }

    // ------------------------------------------------------------ renderers

    private boolean zebra = true;

    /** Soft stripes on every second row (on by default, like the design) */
    public void setZebra(boolean zebra) {
        this.zebra = zebra;
        repaint();
    }

    /**
     * White (or fully see-through) means "no fill": the lavender card shows,
     * with a slightly deeper tint on every second row. Hovered rows turn lilac.
     */
    private Color fillFor(Color c, int row, boolean selected) {
        boolean plain = c == null || c.getAlpha() == 0 || Color.WHITE.equals(c);
        if (row == hoverRow && !selected) {
            return plain ? Theme.ROW_HOVER : Theme.mix(c, new Color(0xe2d4fb), 0.35f);
        }
        if (plain) {
            return zebra && row % 2 == 1 ? Theme.ZEBRA_ROW : null;
        }
        return c;
    }

    /** Draws normal cells and pill cells */
    private class Renderer extends DefaultTableCellRenderer {
        private final PillLabel pill = new PillLabel();
        private Color cellFill;

        /** One reusable padding border per indent (making borders for every cell was slow) */
        private final java.util.Map<Integer, javax.swing.border.Border> paddings = new java.util.HashMap<>();

        @Override
        protected void paintComponent(Graphics g) {
            if (cellFill != null) {
                g.setColor(cellFill);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.setColor(ROW_RULE);                       // the hairline under the row
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
            super.paintComponent(g);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            String text = value == null ? "" : value.toString();
            int modelColumn = convertColumnIndexToModel(column);
            Color background = cellBackground(row, modelColumn, selected);

            if (pillColumns.contains(modelColumn) && !text.isEmpty()) {
                Color[] colors = pillColors(text);
                boolean ref = text.matches("[A-Z]{1,4}-\\d+");
                pill.setup(text, colors[0], colors[1], fillFor(background, row, selected), ref, row == hoverRow,
                        rightAligned.contains(modelColumn));
                return pill;
            }

            if (wrapColumns.contains(modelColumn) && !text.isEmpty()) {
                text = "<html>" + text.replace("&", "&amp;").replace("<", "&lt;") + "</html>";
            }
            super.getTableCellRendererComponent(table, text, false, false, row, column);
            setFont(cellBold(row, modelColumn) ? Theme.semibold(fontSize) : Theme.regular(fontSize));
            setForeground(cellForeground(row, modelColumn));
            setOpaque(false);
            cellFill = fillFor(background, row, selected);
            setHorizontalAlignment(rightAligned.contains(modelColumn) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            int left = cellPadding + cellIndent(row, modelColumn);
            setBorder(paddings.computeIfAbsent(left * 1000 + cellPadding,
                    k -> BorderFactory.createEmptyBorder(0, left, 1, cellPadding)));
            return this;
        }
    }

    /** A small rounded label used inside table cells (references use a mono font, like JE-7322) */
    private static class PillLabel extends JLabel {
        private Color pillColor;
        private Color cellColor;
        private boolean hovered;
        private boolean right;

        PillLabel() {
            setOpaque(false);
        }

        void setup(String text, Color pillColor, Color textColor, Color cellColor, boolean reference,
                   boolean hovered, boolean right) {
            setText(text);
            setFont(reference ? Theme.mono(12f) : Theme.medium(12.5f));
            this.pillColor = pillColor;
            this.cellColor = cellColor;
            this.hovered = hovered;
            this.right = right;
            setForeground(textColor);
            setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(right ? BorderFactory.createEmptyBorder(0, 10, 0, 26)
                    : BorderFactory.createEmptyBorder(0, 26, 0, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (cellColor != null) {
                g2.setColor(cellColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setColor(ROW_RULE);
            g2.fillRect(0, getHeight() - 1, getWidth(), 1);

            int textWidth = g2.getFontMetrics(getFont()).stringWidth(getText());
            int pillHeight = 24;
            int y = (getHeight() - pillHeight) / 2;
            int x = right ? getWidth() - 16 - (textWidth + 20) : 16;
            g2.setColor(hovered ? Theme.mix(pillColor, getForeground(), 0.10f) : pillColor);
            g2.fillRoundRect(x, y, textWidth + 20, pillHeight, pillHeight, pillHeight);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Lilac column headers in small capitals (#e6dbff, ink #3b1578) with a line under them */
    private class HeaderRenderer extends DefaultTableCellRenderer {
        private final java.awt.Font headFont = Theme.spaced(Theme.semibold(11f), 0.05f);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            String text = value == null ? "" : value.toString().toUpperCase(java.util.Locale.ROOT);
            super.getTableCellRendererComponent(table, text, false, false, row, column);
            setFont(headFont);
            setForeground(Theme.TABLE_HEAD);
            setBackground(Theme.HEAD_BG);
            setOpaque(true);
            int modelColumn = convertColumnIndexToModel(column);
            setHorizontalAlignment(rightAligned.contains(modelColumn) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.HEAD_LINE),
                    BorderFactory.createEmptyBorder(0, cellPadding, 0, cellPadding)));
            return this;
        }
    }
}
