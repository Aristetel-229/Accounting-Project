package com.tala.ui.pages;

import com.tala.model.AccountOption;
import com.tala.service.JournalService;
import com.tala.ui.BasePage;
import com.tala.ui.components.Fields;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

public class JournalEntryPage extends BasePage {
    public JournalEntryPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Journal Entry"; }

    @Override protected String hint() { return "Record a transaction by choosing what is debited and what is credited."; }

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

        JTextArea help = new JTextArea("Tip: every entry has a debit and a credit of the same amount.\n" +
                "Example: Debit Rent Expense and Credit Cash on Hand.\n" +
                "The database transaction is rolled back if posting fails.");
        help.setEditable(false);
        help.setBackground(Theme.GREEN_LIGHT);
        help.setFont(Theme.HINT);
        help.setForeground(Theme.TEXT);
        help.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        p.add(formScroll, BorderLayout.CENTER);
        p.add(help, BorderLayout.SOUTH);
        return p;
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
