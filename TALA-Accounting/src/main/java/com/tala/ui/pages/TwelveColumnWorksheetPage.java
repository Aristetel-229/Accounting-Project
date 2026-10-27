package com.tala.ui.pages;

import com.tala.model.AccountOption;
import com.tala.service.JournalService;
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
import java.util.List;

/** One accounting worksheet containing the five debit/credit column pairs. */
public class TwelveColumnWorksheetPage extends BasePage {
    public TwelveColumnWorksheetPage(MainFrame frame) { super(frame); }

    @Override protected String title() { return "12 Columnar"; }
    @Override protected String hint() { return "Unadjusted trial balance, adjustments, adjusted trial balance, income statement, and balance sheet."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        JButton adjustment = new RoundedButton("Add Adjustment");
        adjustment.addActionListener(e -> showAdjustmentDialog());
        JButton refresh = new RoundedButton("Refresh", RoundedButton.Kind.SECONDARY);
        refresh.addActionListener(e -> show());
        p.add(actionBar(adjustment, refresh), BorderLayout.NORTH);

        String sql = """
            SELECT a.account_code, a.account_name, a.account_type,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND (je.reference_no IS NULL OR je.reference_no NOT LIKE 'ADJ-%') THEN jel.debit ELSE 0 END),0) AS unadj_dr,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND (je.reference_no IS NULL OR je.reference_no NOT LIKE 'ADJ-%') THEN jel.credit ELSE 0 END),0) AS unadj_cr,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND je.reference_no LIKE 'ADJ-%' THEN jel.debit ELSE 0 END),0) AS adj_dr,
                   COALESCE(SUM(CASE WHEN je.status='POSTED' AND je.reference_no LIKE 'ADJ-%' THEN jel.credit ELSE 0 END),0) AS adj_cr
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON jel.account_id=a.id
            LEFT JOIN journal_entries je ON je.id=jel.journal_entry_id
            WHERE a.is_active=TRUE
            GROUP BY a.id,a.account_code,a.account_name,a.account_type
            ORDER BY a.account_code
            """;
        DefaultTableModel raw = readTable(sql, new String[]{"Code","Account","Type","Unadj Dr","Unadj Cr","Adj Dr","Adj Cr"});
        DefaultTableModel model = newTableModel(new String[]{"Code","Account","Unadjusted Dr","Unadjusted Cr","Adjustments Dr","Adjustments Cr","Adjusted Dr","Adjusted Cr","Income Stmt Dr","Income Stmt Cr","Balance Sheet Dr","Balance Sheet Cr"});
        BigDecimal[] totals = new BigDecimal[10];
        java.util.Arrays.fill(totals, BigDecimal.ZERO);
        BigDecimal income = BigDecimal.ZERO;

        for (int i=0; i<raw.getRowCount(); i++) {
            BigDecimal udr = amount(raw,i,3), ucr = amount(raw,i,4), adr = amount(raw,i,5), acr = amount(raw,i,6);
            BigDecimal net = udr.subtract(ucr).add(adr).subtract(acr);
            String type = raw.getValueAt(i,2).toString();
            BigDecimal idr=BigDecimal.ZERO, icr=BigDecimal.ZERO, bdr=BigDecimal.ZERO, bcr=BigDecimal.ZERO;
            if ("REVENUE".equals(type)) { icr=positive(net.negate()); idr=positive(net); income=income.add(net.negate()); }
            else if ("EXPENSE".equals(type)) { idr=positive(net); icr=positive(net.negate()); income=income.subtract(net); }
            else if ("ASSET".equals(type)) { bdr=positive(net); bcr=positive(net.negate()); }
            else { bcr=positive(net.negate()); bdr=positive(net); }
            BigDecimal[] values={udr,ucr,adr,acr,positive(net),positive(net.negate()),idr,icr,bdr,bcr};
            for(int j=0;j<values.length;j++) totals[j]=totals[j].add(values[j]);
            Object[] row=new Object[12]; row[0]=raw.getValueAt(i,0); row[1]=raw.getValueAt(i,1);
            for(int j=0;j<values.length;j++) row[j+2]=values[j].signum()==0?"":Money.currency(values[j]);
            model.addRow(row);
        }
        // Net income closes the worksheet: profit is an income-statement debit and a balance-sheet credit;
        // a loss is the opposite. This is what makes both pairs balance.
        BigDecimal[] closed = totals.clone();
        Object[] netRow = new Object[12];
        netRow[1] = income.signum() >= 0 ? "NET INCOME" : "NET LOSS";
        if (income.signum() >= 0) {
            closed[6] = closed[6].add(income);  netRow[8] = Money.currency(income);
            closed[9] = closed[9].add(income);  netRow[11] = Money.currency(income);
        } else {
            closed[7] = closed[7].add(income.abs());  netRow[9] = Money.currency(income.abs());
            closed[8] = closed[8].add(income.abs());  netRow[10] = Money.currency(income.abs());
        }
        model.addRow(netRow);

        Object[] totalRow = new Object[12]; totalRow[1] = "GRAND TOTAL";
        for (int j = 0; j < closed.length; j++) totalRow[j + 2] = Money.currency(closed[j]);
        model.addRow(totalRow);

        // Debit minus credit for each of the five column pairs. Every one must be zero.
        Object[] diffRow = new Object[12]; diffRow[1] = "BALANCE DIFFERENCE";
        BigDecimal maxDifference = BigDecimal.ZERO;
        for (int pair = 0; pair < 5; pair++) {
            BigDecimal diff = closed[pair * 2].subtract(closed[pair * 2 + 1]);
            diffRow[2 + pair * 2] = Money.currency(diff.abs());
            if (diff.abs().compareTo(maxDifference) > 0) maxDifference = diff.abs();
        }
        model.addRow(diffRow);

        JTable table=Tables.create(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths={65,200,125,125,130,130,120,120,130,130,135,135};
        for(int i=0;i<widths.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getTableHeader().setToolTipText("Column pairs: Unadjusted Trial Balance | Adjustments | Adjusted Trial Balance | Income Statement | Balance Sheet");
        JScrollPane scroll=Tables.scroll(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        p.add(scroll,BorderLayout.CENTER);

        boolean balanced = maxDifference.signum() == 0;
        JLabel status=new JLabel((income.signum() >= 0 ? "Net income: " : "Net loss: ") + Money.currency(income.abs())
                + "     |     Balance difference: " + Money.currency(maxDifference)
                + (balanced ? "  (balanced)" : "  (NOT balanced - review entries)"));
        status.setBorder(new EmptyBorder(8,4,0,4)); status.setFont(Theme.BOLD);
        status.setForeground(balanced ? Theme.GREEN_DARK : Theme.DANGER);
        p.add(status,BorderLayout.SOUTH);
        return p;
    }

    private BigDecimal amount(DefaultTableModel m,int row,int column) {
        Object value=m.getValueAt(row,column);
        return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());
    }
    private BigDecimal positive(BigDecimal n) { return n.signum()>0?n:BigDecimal.ZERO; }

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

