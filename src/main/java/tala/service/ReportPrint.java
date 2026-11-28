package tala.service;

import tala.model.Account;
import tala.model.JournalEntry;
import tala.model.JournalLine;
import tala.model.LedgerRow;
import tala.model.Worksheet;
import tala.model.WorksheetRow;
import tala.util.DateUtil;
import tala.util.Money;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * ReportPrint
 * ------------------------------------------------------------
 * "PDF / Print": builds a colored print page (HTML) for the General
 * Journal, the General Ledger or the 12-column worksheet and opens it
 * in the computer's web browser, which shows its print window right
 * away. Choose "Save as PDF" (or "Microsoft Print to PDF") there to
 * get a PDF, or pick a printer.
 */
public final class ReportPrint {

    private ReportPrint() { }

    private static final String CSS =
        ":root{color-scheme:light}*{box-sizing:border-box}body{font-family:Arial,Helvetica,sans-serif;color:#1b1530;background:#fff;margin:0;padding:26px 30px;"
        + "-webkit-print-color-adjust:exact;print-color-adjust:exact}"
        + ".band{background:linear-gradient(120deg,#2e0f6b,#4c1d95 55%,#6d28d9);color:#fff;border-radius:12px;padding:16px 20px;"
        + "display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:14px}"
        + ".band h1{margin:0;font-size:20px;letter-spacing:.01em}.band h1:before{content:'';display:inline-block;width:9px;height:9px;"
        + "border-radius:50%;background:#fddd44;margin-right:10px;vertical-align:middle}"
        + ".band p{margin:4px 0 0;font-size:11.5px;color:#d6c8fa}.band .tag{font-size:11px;color:#fddd44;font-weight:600;letter-spacing:.12em}"
        + "table{border-collapse:collapse;width:100%;font-size:11px;font-variant-numeric:tabular-nums}"
        + "th{background:#e6dbff;color:#3b1578;text-align:left;padding:7px 8px;font-size:9.5px;letter-spacing:.08em;"
        + "text-transform:uppercase;border-bottom:1.5px solid #a78bfa}"
        + "td{padding:5px 8px;border-bottom:1px solid #e9e2f6;background:#fff}.r{text-align:right}.c{text-align:center}"
        + "tbody tr:nth-child(even) td{background:#f8f4ff}"
        + ".dr{color:#6d4fc9}.cr{color:#a8740c}.mute{color:#7a7392;font-style:italic}"
        + ".ref{font:600 9.5px Consolas,monospace;color:#5b21b6;background:#f1ebff;padding:2px 6px;border-radius:5px}"
        + ".day{font-weight:700;color:#8a6200;background:#fdf4d6;padding:2px 8px;border-radius:6px;white-space:nowrap}"
        + ".ind{padding-left:34px}.b{font-weight:700}"
        + "tr.tot td{font-weight:700;background:#e6dbff !important;color:#2e1065;border-top:2px solid #2e1065;border-bottom:3px double #2e1065}"
        + ".acct{margin:18px 0 6px;display:flex;justify-content:space-between;align-items:center;background:#4c1d95;color:#fff;"
        + "border-radius:9px;padding:8px 12px;font-weight:700;font-size:12.5px;break-after:avoid}"
        + ".acct small{font-weight:500;color:#e3d6ff;font-size:10.5px}.bad{color:#b4233c !important}"
        + ".sum{margin-top:14px;display:flex;gap:10px;flex-wrap:wrap}.sum div{background:#f4efff;border:1px solid #ddd0f7;"
        + "border-radius:9px;padding:7px 12px;font-size:11px;color:#5c5577}.sum b{color:#2e1065;margin-left:6px}"
        + ".ok{background:#e8f6ee !important;border-color:#b9e3c9 !important;color:#1f7a45 !important}"
        + ".no{background:#fdecef !important;border-color:#f1c3cc !important;color:#b4233c !important}"
        + ".foot{margin-top:18px;font-size:10px;color:#8a83a3}"
        + "@media print{body{padding:0}.acct,tr{break-inside:avoid}}";

    // ---------------------------------------------------------------- General Journal

    public static void journal(List<JournalEntry> entries, String filterLabel) throws IOException {
        StringBuilder b = start("General Journal", filterLabel + " · " + entries.size()
                + (entries.size() == 1 ? " entry" : " entries") + " · Amounts in PHP", "portrait");
        b.append("<table><thead><tr><th style='width:84px'>Date</th><th>Particulars</th><th class='c' style='width:70px'>Post ref</th>"
                + "<th class='r' style='width:120px'>Debit</th><th class='r' style='width:120px'>Credit</th></tr></thead><tbody>");
        BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
        for (JournalEntry e : entries) {
            boolean first = true;
            for (JournalLine l : e.getLines()) {
                b.append("<tr><td>").append(first ? "<span class='day'>" + esc(DateUtil.toShort(e.getDate())) + "</span>" : "")
                 .append("</td><td").append(l.isDebit() ? " class='b'>" : " class='ind'>").append(esc(l.getAccount().getName()))
                 .append("</td><td class='c'>").append(l.getAccount().getCode()).append("</td><td class='r dr'>")
                 .append(l.isDebit() ? Money.peso(l.getDebit()) : "").append("</td><td class='r cr'>")
                 .append(l.isDebit() ? "" : Money.peso(l.getCredit())).append("</td></tr>");
                first = false;
            }
            b.append("<tr><td></td><td colspan='4' class='mute' style='padding-left:22px'>(").append(esc(e.getDescription()))
             .append(") <span class='ref'>").append(esc(e.getReferenceNo())).append("</span>")
             .append(e.isAdjusting() ? " &nbsp;adjusting entry" : "").append("</td></tr>");
            dr = dr.add(e.getTotalDebit());
            cr = cr.add(e.getTotalCredit());
        }
        b.append("</tbody><tfoot><tr class='tot'><td>Total</td><td></td><td></td><td class='r'>").append(Money.peso(dr))
         .append("</td><td class='r'>").append(Money.peso(cr)).append("</td></tr></tfoot></table>");
        boolean even = dr.compareTo(cr) == 0;
        b.append("<div class='sum'><div>Entries<b>").append(entries.size()).append("</b></div><div>Total debit<b>")
         .append(Money.peso(dr)).append("</b></div><div>Total credit<b>").append(Money.peso(cr)).append("</b></div><div class='")
         .append(even ? "ok'>In balance ✓" : "no'>Out of balance by " + Money.peso(dr.subtract(cr).abs())).append("</div></div>");
        open(finish(b), "TALA-General-Journal");
    }

    // ---------------------------------------------------------------- General Ledger

    public static void ledger(List<Account> used, List<JournalEntry> entries, AccountingService accounting,
                              String periodLabel) throws IOException {
        StringBuilder b = start("General Ledger", periodLabel + " · " + used.size()
                + (used.size() == 1 ? " account" : " accounts") + " · Amounts in PHP", "portrait");
        for (Account a : used) {
            List<LedgerRow> rows = accounting.buildLedger(a, entries);
            BigDecimal end = rows.isEmpty() ? BigDecimal.ZERO : rows.get(rows.size() - 1).getRunningBalance();
            boolean ok = end.signum() >= 0;
            b.append("<div class='acct'><span>").append(a.getCode()).append(" · ").append(esc(a.getName()))
             .append("</span><small>").append(a.getType().getLabel()).append(" · Normal balance: ")
             .append(a.isDebitNormal() ? "Debit" : "Credit").append(ok ? "" : " · <b style='color:#fddd44'>Abnormal balance</b>")
             .append("</small></div>");
            b.append("<table><thead><tr><th style='width:84px'>Date</th><th>Description</th><th class='c' style='width:90px'>Ref</th>"
                    + "<th class='r' style='width:110px'>Debit</th><th class='r' style='width:110px'>Credit</th>"
                    + "<th class='r' style='width:130px'>Balance</th></tr></thead><tbody>");
            BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
            for (LedgerRow r : rows) {
                boolean rowOk = r.getRunningBalance().signum() >= 0;
                b.append("<tr><td>").append(esc(DateUtil.toShort(r.getDate()))).append("</td><td>").append(esc(r.getDescription()))
                 .append("</td><td class='c'><span class='ref'>").append(esc(r.getReferenceNo())).append("</span></td><td class='r dr'>")
                 .append(r.getDebit().signum() == 0 ? "" : Money.peso(r.getDebit())).append("</td><td class='r cr'>")
                 .append(r.getCredit().signum() == 0 ? "" : Money.peso(r.getCredit())).append("</td><td class='r b")
                 .append(rowOk ? "" : " bad").append("'>").append(Money.peso(r.getRunningBalance().abs())).append(' ')
                 .append(AccountingService.sideOf(a, r.getRunningBalance())).append("</td></tr>");
                dr = dr.add(r.getDebit());
                cr = cr.add(r.getCredit());
            }
            b.append("</tbody><tfoot><tr class='tot'><td colspan='3'>Total · ending balance</td><td class='r'>")
             .append(Money.peso(dr)).append("</td><td class='r'>").append(Money.peso(cr)).append("</td><td class='r")
             .append(ok ? "" : " bad").append("'>").append(Money.peso(end.abs())).append(' ')
             .append(AccountingService.sideOf(a, end)).append("</td></tr></tfoot></table>");
        }
        open(finish(b), "TALA-General-Ledger");
    }

    // ---------------------------------------------------------------- 12-column worksheet

    private static final String[] GROUPS = {"Unadjusted trial balance", "Adjustments", "Adjusted trial balance",
            "Income statement", "Owner's equity", "Balance sheet"};
    private static final String[][] BANDS = {   // {text, tint}
        {"#5b21b6", "#efe9fd"}, {"#b7791f", "#fdf4dc"}, {"#3046b8", "#e8ecfc"},
        {"#15803d", "#e5f5ec"}, {"#b4235e", "#fce8ef"}, {"#0f766e", "#e0f3f2"}};

    public static void worksheet(Worksheet ws, String periodLabel) throws IOException {
        StringBuilder b = start("12-Column Worksheet", periodLabel + " · Amounts in PHP", "landscape");
        b.append("<table style='font-size:9.5px'><thead><tr><th rowspan='2'>Code</th><th rowspan='2'>Account</th>");
        for (int g = 0; g < GROUPS.length; g++) {
            b.append("<th colspan='2' class='c' style='background:").append(BANDS[g][1]).append(";color:").append(BANDS[g][0])
             .append(";border-top:3px solid ").append(BANDS[g][0]).append("'>").append(GROUPS[g]).append("</th>");
        }
        b.append("</tr><tr>");
        for (int c = 0; c < WorksheetRow.CELL_COUNT; c++) {
            String[] band = BANDS[c / 2];
            b.append("<th class='r' style='background:").append(band[1]).append(";color:").append(band[0]).append("'>")
             .append(c % 2 == 0 ? "Dr" : "Cr").append("</th>");
        }
        b.append("</tr></thead><tbody>");
        for (WorksheetRow row : ws.getRows()) {
            boolean account = WorksheetRow.ACCOUNT.equals(row.getKind());
            boolean grand = WorksheetRow.GRAND_TOTAL.equals(row.getKind());
            b.append(account ? "<tr>" : "<tr class='tot' style='" + (grand ? "" : "border-bottom:0") + "'>");
            b.append("<td>").append(row.getCode() == null || row.getCode().isEmpty() ? "" : "<span class='ref'>" + esc(row.getCode()) + "</span>")
             .append("</td><td").append(account ? "" : " class='b'").append(">").append(esc(row.getName())).append("</td>");
            for (int c = 0; c < WorksheetRow.CELL_COUNT; c++) {
                BigDecimal v = row.get(c);
                boolean show = v != null && (grand || v.signum() != 0);
                String color = account ? (c % 2 == 0 ? "#6d4fc9" : "#a8740c") : "#2e1065";
                b.append("<td class='r' style='color:").append(show ? color : "#c9c2da").append(";background:")
                 .append(account ? BANDS[c / 2][1] + "99" : "").append("'>").append(show ? Money.format(v) : "–").append("</td>");
            }
            b.append("</tr>");
        }
        b.append("</tbody></table>");
        boolean ok = ws.isBalanced();
        b.append("<div class='sum'><div>").append(ws.getNetIncome().signum() < 0 ? "Net loss" : "Net income").append("<b>")
         .append(Money.peso(ws.getNetIncome().abs())).append("</b></div><div>Ending capital<b>").append(Money.peso(ws.getEndingCapital()))
         .append("</b></div><div>Balance sheet difference<b>").append(Money.peso(ws.getBalanceSheetDifference()))
         .append("</b></div><div class='").append(ok ? "ok'>Balanced ✓" : "no'>Not balanced").append("</div></div>");
        open(finish(b), "TALA-12-Column-Worksheet");
    }

    // ---------------------------------------------------------------- shared

    private static StringBuilder start(String title, String sub, String orientation) {
        StringBuilder b = new StringBuilder("<!DOCTYPE html><html><head><meta charset='utf-8'><title>TALA · ")
                .append(esc(title)).append("</title><style>").append(CSS).append("@page{size:A4 ").append(orientation)
                .append(";margin:12mm}</style></head><body>");
        b.append("<div class='band'><div><div class='tag'>TALA · ACCOUNTING SYSTEM</div><h1>").append(esc(title))
         .append("</h1><p>").append(esc(sub)).append("</p></div><p>Printed ")
         .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH)))
         .append("</p></div>");
        return b;
    }

    private static String finish(StringBuilder b) {
        b.append("<div class='foot'>TALA · Accounting System</div>")
         .append("<script>window.addEventListener('load',function(){setTimeout(function(){window.print();},350);});</script>")
         .append("</body></html>");
        return b.toString();
    }

    /** Saves the page in the temp folder and opens it in the browser (which then shows the print window) */
    private static void open(String html, String name) throws IOException {
        File file = File.createTempFile(name + "-", ".html");
        file.deleteOnExit();
        Files.write(file.toPath(), html.getBytes(StandardCharsets.UTF_8));
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("This computer cannot open a web browser from Java. The print page was saved to "
                    + file.getAbsolutePath());
        }
        Desktop.getDesktop().browse(file.toURI());
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
