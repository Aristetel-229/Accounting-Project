package com.accountpro.ui.pages;

import com.accountpro.service.InvoiceService;
import com.accountpro.ui.BasePage;
import com.accountpro.ui.components.Fields;
import com.accountpro.ui.components.RoundedButton;
import com.accountpro.ui.components.Tables;
import com.accountpro.ui.MainFrame;
import com.accountpro.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

public class InvoicesPage extends BasePage {
    public InvoicesPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "Invoices & Payments"; }

    @Override protected String hint() { return "Create an invoice, then select it in the table to record a payment."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        JTextField customer = Fields.text();
        JTextField invoiceNo = Fields.text("INV-" + System.currentTimeMillis() % 1000000);
        JTextField date = Fields.text(LocalDate.now().toString());
        JTextField description = Fields.text();
        JTextField total = Fields.text();

        int r = 0;
        addField(form, g, r++, "Customer name", customer);
        addField(form, g, r++, "Invoice number", invoiceNo);
        addField(form, g, r++, "Invoice date (YYYY-MM-DD)", date);
        addField(form, g, r++, "Description", description);
        addField(form, g, r++, "Invoice total (₱)", total);

        JButton save = new RoundedButton("Save Invoice");
        save.addActionListener(e -> saveInvoice(customer.getText().trim(), invoiceNo.getText().trim(),
                date.getText().trim(), description.getText().trim(), total.getText().trim()));
        g.gridx = 1; g.gridy = r;
        form.add(save, g);

        DefaultTableModel model = readTable(
                "SELECT invoice_no,customer_name,invoice_date,description,total_amount,amount_paid,"
                        + "(total_amount-amount_paid) AS balance,status FROM invoices ORDER BY invoice_date DESC",
                new String[]{"Invoice No.", "Customer", "Date", "Description", "Total", "Paid", "Balance", "Status"});
        JTable table = Tables.create(model);
        table.setAutoCreateRowSorter(true);

        JButton payment = new RoundedButton("Record Payment for Selected Invoice", RoundedButton.Kind.PRIMARY);
        payment.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected < 0) {
                error("Select an invoice first.");
                return;
            }
            int row = table.convertRowIndexToModel(selected);
            String inv = model.getValueAt(row, 0).toString();
            String bal = model.getValueAt(row, 6).toString();
            recordPayment(inv, bal);
            show();
        });

        JPanel lower = new JPanel(new BorderLayout(8, 8));
        lower.add(Tables.scroll(table), BorderLayout.CENTER);
        lower.add(actionBar(payment), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(form), lower);
        split.setResizeWeight(0.45);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private void saveInvoice(String customer, String invoiceNo, String dateText, String description, String totalText) {
        try {
            if (customer.isBlank() || invoiceNo.isBlank() || description.isBlank()) {
                throw new IllegalArgumentException("Complete all invoice fields.");
            }
            Date date = Date.valueOf(LocalDate.parse(dateText));
            BigDecimal total = Money.parse(totalText);
            if (total.signum() <= 0) throw new IllegalArgumentException("Invoice total must be positive.");
            InvoiceService.save(customer, invoiceNo, date, description, total);
            JOptionPane.showMessageDialog(frame,
                    "Invoice saved. Note: this starter invoice module does not yet create journal entries automatically.");
            show();
        } catch (Exception ex) {
            error("Could not save invoice: " + ex.getMessage());
        }
    }

    private void recordPayment(String invoiceNo, String balanceText) {
        String entered = JOptionPane.showInputDialog(frame,
                "Payment amount (₱) for invoice " + invoiceNo + ":", balanceText);
        if (entered == null) return;
        try {
            BigDecimal amount = Money.parse(entered);
            if (amount.signum() <= 0) throw new IllegalArgumentException("Payment must be positive.");
            InvoiceService.recordPayment(invoiceNo, amount);
            JOptionPane.showMessageDialog(frame, "Payment recorded.");
        } catch (Exception ex) {
            error("Could not record payment: " + ex.getMessage());
        }
    }
}
