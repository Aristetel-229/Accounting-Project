package com.tala.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.tala.db.DB;

/** Base class for every screen: shared layout and database-to-table helpers. */
public abstract class BasePage {
    protected final MainFrame frame;

    protected BasePage(MainFrame frame) {
        this.frame = frame;
    }

    /** Title shown above the page. */
    protected abstract String title();

    /** One-line explanation shown under the title (helps students know what the page is for). */
    protected String hint() { return ""; }

    /** Builds the page contents. */
    protected abstract JComponent build();

    /** Builds the page and displays it in the main window. */
    public void show() {
        frame.setPage(getClass().getSimpleName(), title(), hint(), build());
    }

    protected JPanel pagePanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setOpaque(false);
        return p;
    }

    /** Right-aligned row holding one or more buttons. */
    protected JPanel actionBar(JComponent... buttons) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        for (JComponent b : buttons) bar.add(b);
        return bar;
    }

    protected void addField(JPanel panel, GridBagConstraints g, int row, String label, Component field) {
        g.gridx = 0; g.gridy = row; g.weightx = 0.35;
        JLabel l = new JLabel(label);
        l.setFont(Theme.BASE);
        l.setForeground(Theme.TEXT);
        panel.add(l, g);
        g.gridx = 1; g.weightx = 1;
        panel.add(field, g);
    }

    protected void error(String message) {
        JOptionPane.showMessageDialog(frame, message, "Validation / Database Error", JOptionPane.ERROR_MESSAGE);
    }

    protected DefaultTableModel newTableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    protected DefaultTableModel readTable(String sql, String[] columns) {
        DefaultTableModel model = newTableModel(columns);
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[count];
                for (int i = 0; i < count; i++) {
                    Object value = rs.getObject(i + 1);
                    row[i] = value == null ? "" : value;
                }
                model.addRow(row);
            }
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            frame.setStatus(" Database error");
            JOptionPane.showMessageDialog(frame,
                    "Unable to load data. Check that MySQL is running and database/tala.sql has been executed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return model;
    }
}
