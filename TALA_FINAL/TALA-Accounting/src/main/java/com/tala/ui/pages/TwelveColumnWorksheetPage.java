package com.tala.ui.pages;

import com.tala.model.AccountOption;
import com.tala.service.JournalService;
import com.tala.service.WorksheetService;
import com.tala.ui.BasePage;
import com.tala.ui.MainFrame;
import com.tala.ui.Theme;
import com.tala.ui.components.RoundedButton;
import com.tala.ui.components.Tables;
import com.tala.util.Money;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** One accounting worksheet containing the six debit/credit column pairs (12 value columns). */
public class TwelveColumnWorksheetPage extends BasePage {
    public TwelveColumnWorksheetPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "12 Columnar"; }
    @Override protected String hint() { return "Unadjusted trial balance, adjustments, adjusted trial balance, income statement, owner's equity, and balance sheet."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        JButton adjustment = new RoundedButton("Add Adjustment");
        adjustment.addActionListener(e -> showAdjustmentDialog());
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        p.add(actionBar(adjustment, refresh), BorderLayout.NORTH);

        // Only accounts that actually have posted entries are listed, so the sheet starts empty.
        WorksheetService.Result result;
        try {
            result = WorksheetService.compute(WorksheetService.loadAccounts());
            frame.setStatus(" Connected to MySQL");
        } catch (SQLException ex) {
            frame.setStatus(" Database error");
            error("Unable to load the worksheet: " + ex.getMessage());
            result = WorksheetService.compute(java.util.List.of());
        }

        DefaultTableModel model = newTableModel(new String[]{"Code", "Account",
                "Unadjusted Dr", "Unadjusted Cr", "Adjustments Dr", "Adjustments Cr",
                "Adjusted Dr", "Adjusted Cr", "Income Stmt Dr", "Income Stmt Cr",
                "Owner's Equity Dr", "Owner's Equity Cr", "Balance Sheet Dr", "Balance Sheet Cr"});

        if (!result.isEmpty()) {
            for (WorksheetService.Line line : result.lines) {
                model.addRow(row(line.code, line.name, line.v, true));
            }
            model.addRow(row("", result.income.signum() >= 0 ? "NET INCOME" : "NET LOSS", result.netIncomeRow, true));
            model.addRow(row("", "ENDING OWNER'S EQUITY", result.endingEquityRow, true));
            model.addRow(row("", "GRAND TOTAL", result.totals, false));

            // Debit minus credit of each column pair. Every one must read 0.00.
            Object[] diffRow = new Object[14];
            Arrays.fill(diffRow, "");
            diffRow[1] = "BALANCE DIFFERENCE";
            for (int pair = 0; pair < WorksheetService.PAIRS; pair++) {
                diffRow[2 + pair * 2] = Money.currency(result.differences[pair].abs());
            }
            model.addRow(diffRow);
        }

        JTable table = Tables.create(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {65, 200, 120, 120, 125, 125, 115, 115, 125, 125, 135, 135, 130, 130};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getTableHeader().setToolTipText("Column pairs: Unadjusted Trial Balance | Adjustments | Adjusted Trial Balance | Income Statement | Owner's Equity | Balance Sheet");
        JScrollPane scroll = Tables.scroll(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        p.add(scroll, BorderLayout.CENTER);

        BigDecimal maxDifference = result.maxDifference();
        boolean balanced = maxDifference.signum() == 0;
        String text = result.isEmpty()
                ? "No posted entries yet. Post a journal entry and its accounts will appear here."
                : (result.income.signum() >= 0 ? "Net income: " : "Net loss: ") + Money.currency(result.income.abs())
                  + "     |     Balance difference: " + Money.currency(maxDifference)
                  + (balanced ? "  (balanced)" : "  (NOT balanced - review entries)");
        JLabel status = new JLabel(text);
        status.setBorder(new EmptyBorder(8, 4, 0, 4)); status.setFont(Theme.BOLD);
        status.setForeground(balanced ? Theme.GREEN_DARK : Theme.DANGER);
        p.add(status, BorderLayout.SOUTH);
        return p;
    }

    /** Builds one table row; zero amounts are left blank on account lines, but shown on total rows. */
    private Object[] row(String code, String name, BigDecimal[] values, boolean blankZeros) {
        Object[] row = new Object[14];
        row[0] = code; row[1] = name;
        for (int j = 0; j < values.length; j++) {
            row[j + 2] = (blankZeros && values[j].signum() == 0) ? "" : Money.currency(values[j]);
        }
        return row;
    }

    private void showAdjustmentDialog() {
        final List<AccountOption> accounts;
        try { accounts=JournalService.loadActiveAccounts(); }
        catch(SQLException ex) { error("Could not load accounts: "+ex.getMessage()); return; }
        JPanel form=new JPanel(new GridLayout(0,2,8,8));
        JTextField date=new JTextField(LocalDate.now().toString());
        JTextField reference=new JTextField("ADJ-"+(System.currentTimeMillis()%1000000));
        JTextField description=new JTextField(); JComboBox<AccountOption> debit=new JComboBox<>(), credit=new JComboBox<>();
        for(AccountOption a:accounts){debit.addItem(a);credit.addItem(a);}
        JTextField amount=new JTextField();
        form.add(new JLabel("Date (YYYY-MM-DD)")); form.add(date);
        form.add(new JLabel("Reference")); form.add(reference);
        form.add(new JLabel("Description")); form.add(description);
        form.add(new JLabel("Debit account")); form.add(debit);
        form.add(new JLabel("Credit account")); form.add(credit);
        form.add(new JLabel("Amount (₱)")); form.add(amount);
        int result=JOptionPane.showConfirmDialog(frame,form,"Post an adjusting entry",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result!=JOptionPane.OK_OPTION)return;
        if(description.getText().isBlank()||reference.getText().isBlank()||debit.getSelectedItem()==null||credit.getSelectedItem()==null){error("Complete all adjustment fields.");return;}
        if(!reference.getText().trim().toUpperCase().startsWith("ADJ-")){error("Adjustment reference must start with ADJ- so it appears in the adjustment columns.");return;}
        if(((AccountOption)debit.getSelectedItem()).id==((AccountOption)credit.getSelectedItem()).id){error("Choose different debit and credit accounts.");return;}
        try {
            Date d=Date.valueOf(LocalDate.parse(date.getText().trim())); BigDecimal n=Money.parse(amount.getText().trim());
            if(n.signum()<=0)throw new IllegalArgumentException();
            JournalService.postTwoLineEntry(d,reference.getText().trim(),description.getText().trim(),(AccountOption)debit.getSelectedItem(),(AccountOption)credit.getSelectedItem(),n);
            frame.setStatus(" Posted adjusting entry"); show();
        } catch(Exception ex){error("Could not post adjustment. Check the date, amount, and unique reference number.");}
    }
}

