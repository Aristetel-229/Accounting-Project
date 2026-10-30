package com.tala.ui.pages;

import com.tala.db.DB;
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * General Journal: every posted entry in date order as
 * DATE | PARTICULARS | POST REF | DEBIT | CREDIT. The debited account comes first, the credited
 * account is indented under it, and an explanation line closes each entry.
 * Only accounts that were actually used appear, and a TOTAL row ends the page.
 */
public class GeneralJournalPage extends BasePage {
    private static final String INDENT = "  ";

    public GeneralJournalPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "General Journal"; }

    @Override protected String hint() {
        return "Every entry in date order: date, particulars, post reference, debit and credit. Only accounts you have used appear.";
    }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        DefaultTableModel model = newTableModel(new String[]{"Date", "Particulars", "Post Ref", "Debit", "Credit"});
        Set<Integer> explanationRows = new HashSet<>();   // rows drawn as the shaded "explanation" line
        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO;
        int entries = 0;

        String sql = """
            SELECT je.id, je.entry_date, je.reference_no, je.description,
                   a.account_code, a.account_name, jel.debit, jel.credit
            FROM journal_entry_lines jel
            JOIN journal_entries je ON je.id = jel.journal_entry_id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            ORDER BY je.entry_date, je.id, (jel.debit > 0) DESC, jel.id
            """;
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            long currentEntry = -1;
            int currentYear = -1;
            String lastDescription = "";
            while (rs.next()) {
                long entryId = rs.getLong("id");
                if (entryId != currentEntry) {
                    if (currentEntry != -1) {                       // close the previous entry
                        explanationRows.add(model.getRowCount());
                        model.addRow(new Object[]{"", lastDescription, "", "", ""});
                        model.addRow(new Object[]{"", "", "", "", ""});   // blank spacer row
                    }
                    LocalDate d = rs.getDate("entry_date").toLocalDate();
                    if (d.getYear() != currentYear) {               // year row, as in the journal sample
                        currentYear = d.getYear();
                        model.addRow(new Object[]{String.valueOf(currentYear), "", "", "", ""});
                    }
                    currentEntry = entryId;
                    entries++;
                    lastDescription = rs.getString("description") + "  ·  " + rs.getString("reference_no");
                    String shortDate = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ". " + d.getDayOfMonth();
                    model.addRow(lineRow(shortDate, rs));
                } else {
                    model.addRow(lineRow("", rs));
                }
                totalDebit = totalDebit.add(rs.getBigDecimal("debit"));
                totalCredit = totalCredit.add(rs.getBigDecimal("credit"));
            }
            if (currentEntry != -1) {                               // close the last entry
                explanationRows.add(model.getRowCount());
                model.addRow(new Object[]{"", lastDescription, "", "", ""});
            }
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            frame.setStatus(" Database error");
            JOptionPane.showMessageDialog(frame,
                    "Unable to load data. Check that MySQL is running and database/tala.sql has been executed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        boolean hasData = model.getRowCount() > 0;
        if (hasData) {
            model.addRow(new Object[]{"", "TOTAL", "", Money.currency(totalDebit), Money.currency(totalCredit)});
        }

        JTable table = Tables.create(model);
        table.setRowSelectionAllowed(false);
        // Keep the standard look, but shade and italicize the explanation lines like the journal sample.
        TableCellRenderer base = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (t, v, sel, foc, r, col) -> {
            Component cell = base.getTableCellRendererComponent(t, v, sel, foc, r, col);
            if (explanationRows.contains(r)) {
                cell.setBackground(Theme.GREEN_LIGHT);
                cell.setFont(Theme.BASE.deriveFont(Font.ITALIC));
            }
            return cell;
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        p.add(Tables.scroll(table), BorderLayout.CENTER);

        JLabel status = new JLabel(!hasData ? "No posted entries yet."
                : entries + (entries == 1 ? " entry" : " entries") + "   |   Total debit " + Money.currency(totalDebit)
                + "   |   Total credit " + Money.currency(totalCredit));
        status.setFont(Theme.BOLD);
        status.setForeground(Theme.GREEN_DARK);
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(status, BorderLayout.WEST);
        south.add(actionBar(refresh), BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    /** One journal line: debit accounts sit at the left, credit accounts are indented. */
    private Object[] lineRow(String date, ResultSet rs) throws SQLException {
        BigDecimal debit = rs.getBigDecimal("debit"), credit = rs.getBigDecimal("credit");
        String name = rs.getString("account_name");
        return new Object[]{date,
                credit.signum() > 0 ? INDENT + name : name,
                rs.getString("account_code"),
                debit.signum() > 0 ? Money.currency(debit) : "",
                credit.signum() > 0 ? Money.currency(credit) : ""};
    }
}
