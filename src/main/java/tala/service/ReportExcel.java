package tala.service;

import tala.model.Account;
import tala.model.AccountType;
import tala.model.JournalEntry;
import tala.model.JournalLine;
import tala.model.LedgerRow;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * ReportExcel
 * ------------------------------------------------------------
 * Saves the General Journal and the General Ledger as COLORED Excel
 * workbooks (.xlsx), in the same purple / yellow look as the app:
 *
 *   General Journal  - a purple title banner, lilac column headers,
 *                      each entry in its own soft band (debit line,
 *                      indented credit line, the description in grey),
 *                      a lilac Total row, then a summary and the
 *                      accounts that were used.
 *
 *   General Ledger   - sheet "Summary": one line per account with its
 *                      type, totals, ending balance and a Normal /
 *                      Abnormal mark;  sheet "Ledgers": one colored
 *                      block per account (header band, its postings
 *                      with a running balance, and a Total row).
 *
 * Amounts are real numbers (you can add them up in Excel) and dates
 * are real dates.
 */
public final class ReportExcel {

    // the app's colors
    private static final String INK = "231B3A", INK_SOFT = "5C5577", MUTED = "7A7392",
            PURPLE = "5B21B6", PURPLE_DEEP = "2E1065", HEAD_INK = "3B1578", HEAD_BG = "E6DBFF",
            DEBIT = "6D4FC9", CREDIT = "A8740C", LINE = "E4DDF2", BAND = "F8F4FF",
            GREEN = "1F7A45", GREEN_BG = "E8F6EE", RED = "B4233C", RED_BG = "FDECEF";

    private ReportExcel() { }

    // =================================================================
    //  General Journal
    // =================================================================

    /** entries = the entries shown on screen (oldest first); filterLabel e.g. "November 2026" */
    public static void writeJournal(List<JournalEntry> entries, String filterLabel, File file) throws IOException {
        Xlsx book = new Xlsx();
        Xlsx.Sheet s = book.sheet("General Journal", PURPLE);
        String[] heads = {"Date", "Particulars", "Post ref", "Debit", "Credit", "Reference"};
        int last = heads.length - 1;
        s.width(0, 14).width(1, 46).width(2, 11).width(3, 18).width(4, 18).width(5, 16);
        int r = banner(book, s, "TALA · General Journal", filterLabel + "   ·   " + entries.size()
                + (entries.size() == 1 ? " entry" : " entries") + "   ·   Amounts in PHP   ·   Exported " + now(), last);
        r = headerRow(book, s, r, heads, new String[] {"center", "left", "center", "right", "right", "center"});
        s.freeze(r - 1, 0).printTitles(r - 1, r - 1);

        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO;
        Map<Integer, BigDecimal[]> accounts = new TreeMap<>();      // code -> {debit, credit}
        Map<Integer, String> names = new TreeMap<>();
        int band = 0;
        for (JournalEntry e : entries) {
            String fill = band++ % 2 == 0 ? "FFFFFF" : BAND;
            boolean first = true;
            for (JournalLine line : e.getLines()) {
                Account a = line.getAccount();
                boolean dr = line.isDebit();
                accounts.computeIfAbsent(a.getCode(), k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
                BigDecimal[] t = accounts.get(a.getCode());
                t[0] = t[0].add(line.getDebit());
                t[1] = t[1].add(line.getCredit());
                names.put(a.getCode(), a.getName() + "|" + a.getType().getLabel());

                Xlsx.Row row = s.row(r++, 20);
                int dateStyle = first
                        ? book.style().font(true, false, 10.5, "8A6200").fill("FDF4D6").box("thin:" + LINE)
                                .format(Xlsx.FMT_DATE).align("center").build()
                        : book.style().fill(fill).border("thin:" + LINE, "thin:" + LINE, null, null).build();
                if (first) {
                    row.date(0, dateStyle, e.getDate());
                } else {
                    row.blank(0, dateStyle);
                }
                row.text(1, book.style().font(dr, false, 11, INK).fill(fill).align("left").indent(dr ? 1 : 4)
                        .border(null, "thin:" + LINE, null, null).build(), a.getName());
                row.code(2, book.style().font(false, false, 10.5, PURPLE).fill(fill).align("center")
                        .border(null, "thin:" + LINE, null, null).build(), String.valueOf(a.getCode()));
                int money = book.style().font(false, false, 11, dr ? DEBIT : CREDIT).fill(fill)
                        .format(Xlsx.FMT_MONEY).align("right").border(null, "thin:" + LINE, null, null).build();
                int empty = book.style().fill(fill).border(null, "thin:" + LINE, null, null).build();
                if (dr) {
                    row.number(3, money, line.getDebit()).blank(4, empty);
                } else {
                    row.blank(3, empty).number(4, money, line.getCredit());
                }
                if (first) {
                    row.text(5, book.style().font(true, false, 10, PURPLE).fill("F1EBFF").box("thin:" + LINE)
                            .align("center").build(), e.getReferenceNo());
                } else {
                    row.blank(5, book.style().fill(fill).border(null, "thin:" + LINE, null, null).build());
                }
                first = false;
            }
            // the description line, in grey italics, closing the entry's band
            Xlsx.Row memo = s.row(r++, 19);
            String bottom = "thin:D9CFF0";
            int memoEmpty = book.style().fill(fill).border("thin:" + LINE, null, null, bottom).build();
            memo.blank(0, memoEmpty);
            memo.text(1, book.style().font(false, true, 10.5, MUTED).fill(fill).align("left").indent(2)
                    .border(null, null, null, bottom).build(),
                    "(" + e.getDescription() + ")" + (e.isAdjusting() ? "   - adjusting entry" : ""));
            int memoRest = book.style().fill(fill).border(null, null, null, bottom).build();
            memo.blanks(2, 4, memoRest);
            memo.blank(5, book.style().fill(fill).border(null, "thin:" + LINE, null, bottom).build());

            totalDebit = totalDebit.add(e.getTotalDebit());
            totalCredit = totalCredit.add(e.getTotalCredit());
        }

        // Total row
        r = totalRow(book, s, r, "Total", totalDebit, totalCredit, null, last);

        // Summary + accounts used
        r++;
        boolean even = totalDebit.compareTo(totalCredit) == 0;
        r = sectionTitle(book, s, r, "Summary", 1, 3);
        r = summaryLine(book, s, r, "Entries", BigDecimal.valueOf(entries.size()), Xlsx.FMT_INT);
        r = summaryLine(book, s, r, "Total debit", totalDebit, Xlsx.FMT_PESO);
        r = summaryLine(book, s, r, "Total credit", totalCredit, Xlsx.FMT_PESO);
        r = statusLine(book, s, r, even, even ? "In balance ✓" : "Out of balance by ₱"
                + tala.util.Money.format(totalDebit.subtract(totalCredit).abs()));

        r++;
        r = sectionTitle(book, s, r, "Accounts in this journal", 1, 4);
        Xlsx.Row h = s.row(r++, 20);
        int sh = book.style().font(true, false, 10, HEAD_INK).fill(HEAD_BG).box("thin:D3C3F3").align("left").build();
        int shr = book.style().font(true, false, 10, HEAD_INK).fill(HEAD_BG).box("thin:D3C3F3").align("right").build();
        h.text(1, sh, "ACCOUNT").text(2, sh, "CODE").text(3, shr, "DEBITS").text(4, shr, "CREDITS");
        int i = 0;
        for (Map.Entry<Integer, BigDecimal[]> a : accounts.entrySet()) {
            String fill = i++ % 2 == 0 ? "FFFFFF" : BAND;
            String[] nt = names.get(a.getKey()).split("\\|", 2);
            Xlsx.Row row = s.row(r++, 19);
            row.text(1, book.style().font(false, false, 11, INK).fill(fill).box("thin:" + LINE).align("left").build(), nt[0]);
            row.code(2, book.style().font(false, false, 10.5, PURPLE).fill(fill).box("thin:" + LINE).align("center").build(),
                    String.valueOf(a.getKey()));
            row.number(3, book.style().font(false, false, 11, DEBIT).fill(fill).box("thin:" + LINE).format(Xlsx.FMT_MONEY)
                    .align("right").build(), a.getValue()[0]);
            row.number(4, book.style().font(false, false, 11, CREDIT).fill(fill).box("thin:" + LINE).format(Xlsx.FMT_MONEY)
                    .align("right").build(), a.getValue()[1]);
        }
        book.write(file);
    }

    // =================================================================
    //  General Ledger
    // =================================================================

    /** used = the accounts that have postings (by code); entries = every entry */
    public static void writeLedger(List<Account> used, List<JournalEntry> entries, AccountingService accounting,
                                   String periodLabel, File file) throws IOException {
        Xlsx book = new Xlsx();

        // ---------- sheet 1: Summary ----------
        Xlsx.Sheet sum = book.sheet("Summary", "F7AF1A");
        String[] heads = {"Code", "Account", "Type", "Normal", "Total debit", "Total credit", "Ending balance", "Status"};
        int lastCol = heads.length - 1;
        sum.width(0, 9).width(1, 34).width(2, 12).width(3, 10).width(4, 17).width(5, 17).width(6, 19).width(7, 13);
        int r = banner(book, sum, "TALA · General Ledger · Summary", periodLabel + "   ·   " + used.size()
                + (used.size() == 1 ? " account" : " accounts") + "   ·   Amounts in PHP   ·   Exported " + now(), lastCol);
        r = headerRow(book, sum, r, heads, new String[] {"center", "left", "center", "center", "right", "right", "right", "center"});
        sum.freeze(r - 1, 2).printTitles(r - 1, r - 1).landscape();

        BigDecimal allDr = BigDecimal.ZERO, allCr = BigDecimal.ZERO;
        int abnormal = 0, i = 0;
        for (Account a : used) {
            BigDecimal dr = accounting.getTotalDebits(a, entries);
            BigDecimal cr = accounting.getTotalCredits(a, entries);
            BigDecimal bal = accounting.getBalance(a, entries);
            boolean ok = bal.signum() >= 0;
            if (!ok) {
                abnormal++;
            }
            allDr = allDr.add(dr);
            allCr = allCr.add(cr);
            String fill = i++ % 2 == 0 ? "FFFFFF" : BAND;
            String[] tc = typeColors(a.getType());
            Xlsx.Row row = sum.row(r++, 20);
            row.code(0, book.style().font(true, false, 10.5, PURPLE).fill("F1EBFF").box("thin:" + LINE).align("center").build(),
                    String.valueOf(a.getCode()));
            row.text(1, book.style().font(true, false, 11, INK).fill(fill).box("thin:" + LINE).align("left").build(), a.getName());
            row.text(2, book.style().font(true, false, 10, tc[1]).fill(tc[0]).box("thin:" + LINE).align("center").build(),
                    a.getType().getLabel());
            row.text(3, book.style().font(false, false, 10.5, INK_SOFT).fill(fill).box("thin:" + LINE).align("center").build(),
                    a.isDebitNormal() ? "Debit" : "Credit");
            row.number(4, book.style().font(false, false, 11, DEBIT).fill(fill).box("thin:" + LINE)
                    .format(Xlsx.FMT_MONEY).align("right").build(), dr);
            row.number(5, book.style().font(false, false, 11, CREDIT).fill(fill).box("thin:" + LINE)
                    .format(Xlsx.FMT_MONEY).align("right").build(), cr);
            row.number(6, book.style().font(true, false, 11, ok ? INK : RED).fill(ok ? fill : RED_BG).box("thin:" + LINE)
                    .format(Xlsx.FMT_DRCR).align("right").build(), debitSigned(a, bal));
            row.text(7, book.style().font(true, false, 10, ok ? GREEN : RED).fill(ok ? GREEN_BG : RED_BG).box("thin:" + LINE)
                    .align("center").build(), ok ? "Normal" : "Abnormal");
        }
        boolean even = allDr.compareTo(allCr) == 0;
        Xlsx.Row tot = sum.row(r++, 22);
        String top = "medium:" + PURPLE_DEEP, bottom = "double:" + PURPLE_DEEP;
        int tl = book.style().font(true, false, 11, PURPLE_DEEP).fill(HEAD_BG).border(null, null, top, bottom).align("left").build();
        int tm = book.style().font(true, false, 11, PURPLE_DEEP).fill(HEAD_BG).border(null, null, top, bottom)
                .format(Xlsx.FMT_PESO).align("right").build();
        tot.text(0, tl, "").text(1, tl, "Total").blanks(2, 3, tl).number(4, tm, allDr).number(5, tm, allCr).blank(6, tl);
        tot.text(7, book.style().font(true, false, 10, even ? GREEN : RED).fill(even ? GREEN_BG : RED_BG)
                .border(null, null, top, bottom).align("center").build(), even ? "Balanced ✓" : "Not equal");
        r++;
        Xlsx.Row note = sum.row(r, 18);
        note.text(1, book.style().font(false, true, 10, MUTED).build(), abnormal == 0
                ? "Every account has a normal balance."
                : abnormal + (abnormal == 1 ? " account has" : " accounts have") + " an abnormal balance (shown in red).");

        // ---------- sheet 2: one block per account ----------
        Xlsx.Sheet s = book.sheet("Ledgers", PURPLE);
        String[] lh = {"Date", "Description", "Reference", "Debit", "Credit", "Balance"};
        int last = lh.length - 1;
        s.width(0, 14).width(1, 42).width(2, 15).width(3, 17).width(4, 17).width(5, 19);
        r = banner(book, s, "TALA · General Ledger", periodLabel + "   ·   One ledger per account   ·   Amounts in PHP", last);
        s.printTitles(1, 2);
        for (Account a : used) {
            List<LedgerRow> rows = accounting.buildLedger(a, entries);
            BigDecimal bal = rows.isEmpty() ? BigDecimal.ZERO : rows.get(rows.size() - 1).getRunningBalance();
            boolean ok = bal.signum() >= 0;

            // account band
            Xlsx.Row b = s.row(r, 26);
            int bandL = book.style().font(true, false, 12.5, "FFFFFF").gradient("2E0F6B", "4C1D95").align("left").build();
            int bandR = book.style().font(false, false, 10, "E3D6FF").gradient("4C1D95", "6D28D9").align("right").build();
            b.text(0, bandL, a.getCode() + "  ·  " + a.getName()).blanks(1, 2, bandL);
            b.text(3, bandR, a.getType().getLabel() + "  ·  Normal balance: " + (a.isDebitNormal() ? "Debit" : "Credit")
                    + (ok ? "" : "  ·  ABNORMAL")).blanks(4, last, bandR);
            s.merge(r, 0, r, 2).merge(r, 3, r, last);
            r++;
            r = headerRow(book, s, r, lh, new String[] {"left", "left", "center", "right", "right", "right"});

            BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
            int n = 0;
            for (LedgerRow lr : rows) {
                String fill = n++ % 2 == 0 ? "FFFFFF" : BAND;
                Xlsx.Row row = s.row(r++, 19);
                row.date(0, book.style().font(false, false, 10.5, INK_SOFT).fill(fill).box("thin:" + LINE)
                        .format(Xlsx.FMT_DATE).align("left").build(), lr.getDate());
                row.text(1, book.style().font(false, false, 11, INK).fill(fill).box("thin:" + LINE).align("left").build(),
                        lr.getDescription());
                row.text(2, book.style().font(true, false, 10, PURPLE).fill("F1EBFF").box("thin:" + LINE).align("center").build(),
                        lr.getReferenceNo());
                int dm = book.style().font(false, false, 11, DEBIT).fill(fill).box("thin:" + LINE).format(Xlsx.FMT_MONEY).align("right").build();
                int cm = book.style().font(false, false, 11, CREDIT).fill(fill).box("thin:" + LINE).format(Xlsx.FMT_MONEY).align("right").build();
                int em = book.style().fill(fill).box("thin:" + LINE).build();
                if (lr.getDebit().signum() != 0) row.number(3, dm, lr.getDebit()); else row.blank(3, em);
                if (lr.getCredit().signum() != 0) row.number(4, cm, lr.getCredit()); else row.blank(4, em);
                boolean rowOk = lr.getRunningBalance().signum() >= 0;
                row.number(5, book.style().font(true, false, 11, rowOk ? INK : RED).fill(fill).box("thin:" + LINE)
                        .format(Xlsx.FMT_DRCR).align("right").build(), debitSigned(a, lr.getRunningBalance()));
                dr = dr.add(lr.getDebit());
                cr = cr.add(lr.getCredit());
            }
            r = totalRow(book, s, r, "Total  ·  ending balance", dr, cr, new Object[] {debitSigned(a, bal), ok}, last);
            r++;   // a gap before the next account
        }
        book.write(file);
    }

    // =================================================================
    //  Shared parts
    // =================================================================

    /** Title banner + a lilac info line; returns the next free row (after a small gap) */
    private static int banner(Xlsx book, Xlsx.Sheet s, String title, String sub, int lastCol) {
        int t = book.style().font(true, false, 16, "FFFFFF").gradient("2E0F6B", "6D28D9").align("left").build();
        int st = book.style().font(false, true, 10, INK_SOFT).fill("EFE8FF").align("left").build();
        s.row(1, 34).text(0, t, title).blanks(1, lastCol, t);
        s.row(2, 20).text(0, st, sub).blanks(1, lastCol, st);
        s.merge(1, 0, 1, lastCol).merge(2, 0, 2, lastCol);
        s.row(3, 8);
        return 4;
    }

    /** Lilac column headers in small capitals; returns the next row */
    private static int headerRow(Xlsx book, Xlsx.Sheet s, int r, String[] heads, String[] align) {
        Xlsx.Row row = s.row(r, 24);
        for (int c = 0; c < heads.length; c++) {
            int st = book.style().font(true, false, 10, HEAD_INK).fill(HEAD_BG)
                    .border("thin:D3C3F3", "thin:D3C3F3", "thin:D3C3F3", "medium:A78BFA")
                    .align(align[c]).build();
            row.text(c, st, heads[c].toUpperCase(Locale.ROOT));
        }
        return r + 1;
    }

    /** The lilac Total row with a firm line on top and a double line under it */
    private static int totalRow(Xlsx book, Xlsx.Sheet s, int r, String label, BigDecimal dr, BigDecimal cr,
                                Object[] balance, int last) {
        String top = "medium:" + PURPLE_DEEP, bottom = "double:" + PURPLE_DEEP;
        int l = book.style().font(true, false, 11, PURPLE_DEEP).fill(HEAD_BG).border(null, null, top, bottom).align("left").build();
        int m = book.style().font(true, false, 11, PURPLE_DEEP).fill(HEAD_BG).border(null, null, top, bottom)
                .format(Xlsx.FMT_PESO).align("right").build();
        Xlsx.Row row = s.row(r, 22);
        row.text(0, l, label).blanks(1, 2, l).number(3, m, dr).number(4, m, cr);
        if (balance != null) {
            boolean ok = (Boolean) balance[1];
            row.number(5, book.style().font(true, false, 11, ok ? PURPLE_DEEP : RED).fill(ok ? "FDEFC0" : RED_BG)
                    .border(null, null, top, bottom).format(Xlsx.FMT_DRCR).align("right").build(), (BigDecimal) balance[0]);
        } else {
            row.blanks(5, last, l);
        }
        s.merge(r, 0, r, 2);
        return r + 1;
    }

    private static int sectionTitle(Xlsx book, Xlsx.Sheet s, int r, String text, int from, int to) {
        int st = book.style().font(true, false, 11.5, "FFFFFF").gradient("4C1D95", "7C3AED").align("left").build();
        s.row(r, 22).text(from, st, text).blanks(from + 1, to, st);
        s.merge(r, from, r, to);
        return r + 1;
    }

    private static int summaryLine(Xlsx book, Xlsx.Sheet s, int r, String label, BigDecimal value, int fmt) {
        int l = book.style().font(true, false, 11, "4C1D95").fill("F6F2FF").box("thin:" + LINE).align("left").build();
        int v = book.style().font(true, false, 11, INK).fill("FFFFFF").box("thin:" + LINE).format(fmt).align("right").build();
        s.row(r, 20).text(1, l, label).number(2, v, value).blank(3, v);
        s.merge(r, 2, r, 3);
        return r + 1;
    }

    private static int statusLine(Xlsx book, Xlsx.Sheet s, int r, boolean ok, String text) {
        int l = book.style().font(true, false, 11, "4C1D95").fill("F6F2FF").box("thin:" + LINE).align("left").build();
        int v = book.style().font(true, false, 11, ok ? GREEN : RED).fill(ok ? GREEN_BG : RED_BG).box("thin:" + LINE)
                .align("center").build();
        s.row(r, 20).text(1, l, "Status").text(2, v, text).blank(3, v);
        s.merge(r, 2, r, 3);
        return r + 1;
    }

    /** { background, text } for an account type, like the pills in the app */
    private static String[] typeColors(AccountType type) {
        switch (type) {
            case ASSET:     return new String[] {"F0ECFB", "5B3FB8"};
            case LIABILITY: return new String[] {"FDF4D6", "8A6200"};
            case EQUITY:    return new String[] {"E9EFFD", "2F56C4"};
            case REVENUE:   return new String[] {"E8F6EE", "1F7A45"};
            default:        return new String[] {"FDECEF", "B4233C"};
        }
    }

    /** A normal-side balance as "positive = debit" (Excel then shows it as 1,000.00 Dr / Cr) */
    private static BigDecimal debitSigned(Account a, BigDecimal normalSideBalance) {
        return a.isDebitNormal() ? normalSideBalance : normalSideBalance.negate();
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH));
    }
}
