package com.tala.ui.components;

import com.tala.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Creates tables with a styled header, striped rows, row hover, and bold total rows. */
public final class Tables {
    private Tables() {}

    public static JTable create(TableModel model) {
        final int[] hoverRow = {-1};
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.ROW_SELECTED);
        table.setSelectionForeground(Theme.TEXT);
        table.setFont(Theme.BASE);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setBackground(Theme.GREEN_DARK);
                setForeground(Color.WHITE);
                setFont(Theme.BOLD);
                setBorder(new EmptyBorder(8, 10, 8, 10));
                return this;
            }
        });
        header.setPreferredSize(new Dimension(0, 36));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                String text = v == null ? "" : v.toString();
                boolean number = v instanceof Number || text.startsWith("₱");
                setHorizontalAlignment(number ? SwingConstants.RIGHT : SwingConstants.LEFT);
                if (!sel) {
                    setBackground(isTotalRow(t, r) ? Theme.ROW_TOTAL
                            : r == hoverRow[0] ? Theme.ROW_HOVER : (r % 2 == 0 ? Color.WHITE : Theme.ROW_ALT));
                    setForeground(Theme.TEXT);
                }
                setFont(isTotalRow(t, r) ? Theme.BOLD : Theme.BASE);
                return this;
            }
        });

        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { setHover(table, hoverRow, table.rowAtPoint(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { setHover(table, hoverRow, -1); }
        };
        table.addMouseMotionListener(hover);
        table.addMouseListener(hover);
        return table;
    }

    /** Wraps a table in a scroll pane with a light border. */
    public static JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private static void setHover(JTable table, int[] hoverRow, int row) {
        if (hoverRow[0] != row) {
            hoverRow[0] = row;
            table.repaint();
        }
    }

    /** Summary rows (TOTAL, NET INCOME, ...) are shown in bold. */
    private static boolean isTotalRow(JTable t, int viewRow) {
        if (t.getColumnCount() < 2) return false;
        Object v = t.getValueAt(viewRow, 1);
        if (v == null) return false;
        String s = v.toString();
        return s.startsWith("TOTAL") || s.startsWith("GRAND TOTAL") || s.startsWith("NET INCOME") || s.startsWith("NET LOSS")
                || s.startsWith("ENDING") || s.startsWith("BALANCE") || s.startsWith("Total /");
    }
}
