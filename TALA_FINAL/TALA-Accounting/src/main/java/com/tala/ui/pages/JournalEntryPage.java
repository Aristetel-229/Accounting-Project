package com.tala.ui.pages;

import com.tala.model.AccountOption;
import com.tala.service.JournalService;
import com.tala.ui.BasePage;
import com.tala.ui.components.Fields;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

public class JournalEntryPage extends BasePage {
    public JournalEntryPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Journal Entry"; }

    @Override protected String hint() { return "Record a transaction by choosing what is debited and what is credited. Made a mistake? Select the entry below and delete just that one."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        JTextField date = Fields.text(LocalDate.now().toString());
        JTextField reference = Fields.text("JE-" + System.currentTimeMillis() % 1000000);
        JTextField description = Fields.text();
        JComboBox<AccountOption> debitAccount = new JComboBox<>();
        JComboBox<AccountOption> creditAccount = new JComboBox<>();
        JTextField amount = Fields.text();

        try {
            for (AccountOption option : JournalService.loadActiveAccounts()) {
                debitAccount.addItem(option);
                creditAccount.addItem(option);
            }
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Could not load accounts: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        int row = 0;
        addField(form, g, row++, "Transaction date (YYYY-MM-DD)", date);
        addField(form, g, row++, "Reference number", reference);
        addField(form, g, row++, "Description", description);
        addField(form, g, row++, "Debit account", debitAccount);
        addField(form, g, row++, "Credit account", creditAccount);
        addField(form, g, row++, "Amount (₱)", amount);

        JButton post = new RoundedButton("Validate and Post Entry");
        post.addActionListener(e -> postJournal(date.getText().trim(), reference.getText().trim(),
                description.getText().trim(), (AccountOption) debitAccount.getSelectedItem(),
                (AccountOption) creditAccount.getSelectedItem(), amount.getText().trim()));
        g.gridx = 1; g.gridy = row; g.weightx = 1;
        form.add(post, g);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        // Top: the entry form. Bottom: posted entries, where a single one can be deleted.
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formScroll, buildEntryList());
        split.setBorder(null);
        split.setOpaque(false);
        split.setResizeWeight(0.55);
        split.setDividerSize(8);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    /** Table of posted entries with a button that deletes only the selected entry. */
    private JComponent buildEntryList() {
        String sql = """
            SELECT je.id, je.entry_date, je.reference_no, je.description,
                   GROUP_CONCAT(CASE WHEN jel.debit > 0 THEN a.account_name END SEPARATOR ', ') AS debit_account,
                   GROUP_CONCAT(CASE WHEN jel.credit > 0 THEN a.account_name END SEPARATOR ', ') AS credit_account,
                   SUM(jel.debit) AS amount
            FROM journal_entries je
            JOIN journal_entry_lines jel ON jel.journal_entry_id = je.id
            JOIN accounts a ON a.id = jel.account_id
            WHERE je.status = 'POSTED'
            GROUP BY je.id, je.entry_date, je.reference_no, je.description
            ORDER BY je.entry_date DESC, je.id DESC
            """;
        DefaultTableModel model = readTable(sql,
                new String[]{"ID", "Date", "Reference", "Description", "Debit", "Credit", "Amount"});
        JTable table = Tables.create(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // The ID column stays in the model (so we know which entry to delete) but is hidden from view.
        table.removeColumn(table.getColumnModel().getColumn(0));

        JButton delete = new RoundedButton("Delete Selected Entry", RoundedButton.Kind.DANGER);
        delete.addActionListener(e -> deleteSelected(table, model));

        JLabel title = new JLabel("Posted entries");
        title.setFont(Theme.BOLD);
        title.setForeground(Theme.GREEN_DARK);
        title.setBorder(new EmptyBorder(0, 2, 6, 0));

        JPanel list = new JPanel(new BorderLayout(0, 6));
        list.setOpaque(false);
        list.add(title, BorderLayout.NORTH);
        list.add(Tables.scroll(table), BorderLayout.CENTER);
        list.add(actionBar(delete), BorderLayout.SOUTH);
        return list;
    }

    private void deleteSelected(JTable table, DefaultTableModel model) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            error("Select the entry you want to delete first.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        long id = ((Number) model.getValueAt(row, 0)).longValue();
        String summary = model.getValueAt(row, 2) + " - " + model.getValueAt(row, 3)
                + " (" + model.getValueAt(row, 1) + ", " + Money.currency(new BigDecimal(model.getValueAt(row, 6).toString())) + ")";
        int choice = JOptionPane.showConfirmDialog(frame,
                "Delete this entry?\n\n" + summary + "\n\nOnly this entry is removed. Everything else stays.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            boolean removed = JournalService.deleteEntry(id);
            frame.setStatus(removed ? " Deleted journal entry #" + id : " Entry was already deleted");
            show();
        } catch (Exception ex) {
            error("Could not delete the entry. Nothing was changed.\n\n" + ex.getMessage());
        }
    }

    private void postJournal(String dateText, String reference, String description,
                             AccountOption debit, AccountOption credit, String amountText) {
        if (description.isBlank() || reference.isBlank() || debit == null || credit == null) {
            error("Complete all fields before posting.");
            return;
        }
        if (debit.id == credit.id) {
            error("Choose different debit and credit accounts.");
            return;
        }
        final Date entryDate;
        final BigDecimal amount;
        try {
            entryDate = Date.valueOf(LocalDate.parse(dateText));
            amount = Money.parse(amountText);
            if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        } catch (Exception ex) {
            error("Enter a valid date (YYYY-MM-DD) and a positive amount with no more than two decimal places.");
            return;
        }

        try {
            long entryId = JournalService.postTwoLineEntry(entryDate, reference, description, debit, credit, amount);
            JOptionPane.showMessageDialog(frame, "Journal entry posted successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            frame.setStatus(" Posted journal entry #" + entryId);
            show();
        } catch (Exception ex) {
            error("Posting failed: " + ex.getMessage());
        }
    }
}
