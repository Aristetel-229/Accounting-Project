package tala.service;

import tala.model.Worksheet;
import tala.model.WorksheetRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * WorksheetExcel
 * ------------------------------------------------------------
 * Saves the 12-column worksheet as a COLORED Excel workbook (.xlsx).
 *
 * A CSV file can only hold plain text, so it always opens plain in Excel.
 * An .xlsx file is a small zip of XML files that also describe colors,
 * fonts, borders and number formats. This class writes those XML files
 * itself, so no extra library (like Apache POI) is needed:
 *
 *   - a purple title banner and a lilac line with the period and date
 *   - each column group (Unadjusted, Adjustments, ...) in its own color
 *   - debits in purple, credits in amber, soft zebra stripes on the rows
 *   - cream total rows, a gold double line under the grand total and
 *     green (or red) balance sheet totals
 *   - frozen headers (Code and Account stay visible while scrolling)
 *   - a short summary under the table, printed landscape on one page wide
 */
public final class WorksheetExcel {

    private static final String[] GROUPS = {"Unadjusted trial balance", "Adjustments", "Adjusted trial balance",
            "Income statement", "Owner's equity", "Balance sheet"};
    /** per group: { header fill, Dr/Cr row fill, text color } */
    private static final String[][] GROUP_COLORS = {
        {"EFE9FD", "F7F4FE", "5B21B6"},   // Unadjusted trial balance: violet
        {"FDF4DC", "FEF9EC", "B7791F"},   // Adjustments: amber
        {"E8ECFC", "F3F5FE", "3046B8"},   // Adjusted trial balance: blue
        {"E5F5EC", "F2FAF5", "15803D"},   // Income statement: green
        {"FCE8EF", "FEF3F7", "B4235E"},   // Owner's equity: rose
        {"E0F3F2", "EFF9F8", "0F766E"},   // Balance sheet: teal
    };
    private static final int COLS = 2 + WorksheetRow.CELL_COUNT;   // A..N
    private static final int FIRST_DATA_ROW = 6;

    private WorksheetExcel() { }

    /** Writes the worksheet to an .xlsx file */
    public static void write(Worksheet ws, String periodLabel, File file) throws IOException {
        Styles st = new Styles();
        List<String> rows = new ArrayList<>();
        List<String> merges = new ArrayList<>();
        String last = col(COLS - 1);

        // ---- 1-2: title banner + period line
        int title = st.xf(st.font(true, false, 16, "FFFFFF"), st.gradient("2E0F6B", "6D28D9"), 0, 0, "left", false);
        int sub = st.xf(st.font(false, true, 10, "5C5577"), st.fill("EFE8FF"), 0, 0, "left", false);
        int titleFill = st.xf(0, st.gradient("2E0F6B", "6D28D9"), 0, 0, null, false);
        int subFill = st.xf(0, st.fill("EFE8FF"), 0, 0, null, false);
        StringBuilder r1 = new StringBuilder(text(1, 0, title, "TALA · 12-Column Worksheet"));
        StringBuilder r2 = new StringBuilder(text(2, 0, sub, "Accounting period: " + periodLabel
                + "   ·   Amounts in PHP   ·   Exported "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH))));
        for (int c = 1; c < COLS; c++) {
            r1.append(blank(1, c, titleFill));
            r2.append(blank(2, c, subFill));
        }
        rows.add(row(1, 32, r1));
        rows.add(row(2, 20, r2));
        merges.add("A1:" + last + "1");
        merges.add("A2:" + last + "2");
        rows.add(row(3, 8, new StringBuilder()));

        // ---- 4-5: Code | Account | group titles over Dr / Cr
        int headInk = st.font(true, false, 11, "4C1D95");
        int headBorder = st.border("thin:C9B8F5", "thin:C9B8F5", "thin:C9B8F5", "medium:A78BFA");
        int headLeft = st.xf(headInk, st.fill("EFE8FF"), headBorder, 0, "left", false);
        StringBuilder r4 = new StringBuilder(text(4, 0, headLeft, "Code")).append(text(4, 1, headLeft, "Account"));
        StringBuilder r5 = new StringBuilder(blank(5, 0, headLeft)).append(blank(5, 1, headLeft));
        for (int g = 0; g < GROUPS.length; g++) {
            int ink = st.font(true, false, 11, GROUP_COLORS[g][2]);
            int top = st.xf(ink, st.fill(GROUP_COLORS[g][0]),
                    st.border("medium:A78BFA", "thin:C9B8F5", "thick:" + GROUP_COLORS[g][2], "thin:C9B8F5"), 0, "center", false);
            int subDr = st.xf(st.font(true, false, 10, GROUP_COLORS[g][2]), st.fill(GROUP_COLORS[g][1]),
                    st.border("medium:A78BFA", "thin:E4DDF2", "thin:C9B8F5", "medium:A78BFA"), 0, "right", false);
            int subCr = st.xf(st.font(true, false, 10, GROUP_COLORS[g][2]), st.fill(GROUP_COLORS[g][1]),
                    st.border("thin:E4DDF2", "thin:C9B8F5", "thin:C9B8F5", "medium:A78BFA"), 0, "right", false);
            int c = 2 + g * 2;
            r4.append(text(4, c, top, GROUPS[g])).append(blank(4, c + 1, top));
            r5.append(text(5, c, subDr, "Dr")).append(text(5, c + 1, subCr, "Cr"));
            merges.add(col(c) + "4:" + col(c + 1) + "4");
        }
        merges.add("A4:A5");
        merges.add("B4:B5");
        rows.add(row(4, 24, r4));
        rows.add(row(5, 20, r5));

        // ---- the rows
        int r = FIRST_DATA_ROW;
        int index = 0;
        for (WorksheetRow wr : ws.getRows()) {
            String kind = wr.getKind();
            boolean account = WorksheetRow.ACCOUNT.equals(kind);
            boolean grand = WorksheetRow.GRAND_TOTAL.equals(kind);
            boolean total = WorksheetRow.TOTAL.equals(kind);
            String fill = account ? (index % 2 == 1 ? "FBF9FF" : "FFFFFF") : grand ? "EFE7FF" : "FAF8FF";
            String topLine = total ? "medium:2E1065" : "thin:E4DDF2";
            String bottomLine = grand ? "double:2E1065" : "thin:E4DDF2";
            boolean bold = !account;

            int codeStyle = st.xf(st.font(bold || account, false, 11, "231B3A"), st.fill(fill),
                    st.border("thin:E4DDF2", "thin:E4DDF2", topLine, bottomLine), 0, "left", false);
            int nameStyle = st.xf(st.font(bold, false, 11, bold ? "231B3A" : "2A2240"), st.fill(fill),
                    st.border("thin:E4DDF2", "medium:C9B8F5", topLine, bottomLine), 0, "left", false);
            StringBuilder cells = new StringBuilder();
            String code = wr.getCode() == null ? "" : wr.getCode().trim();
            // account codes are saved as numbers (no green "number stored as text" marks in Excel)
            cells.append(code.matches("[0-9]{1,9}") ? number(r, 0, codeStyle, new BigDecimal(code)) : text(r, 0, codeStyle, code));
            cells.append(text(r, 1, nameStyle, wr.getName() == null ? "" : wr.getName()));
            for (int c = 0; c < WorksheetRow.CELL_COUNT; c++) {
                boolean dr = c % 2 == 0;
                String ink = account ? (dr ? "6D4FC9" : "A8740C") : "2E1065";
                // each group keeps its soft color down the whole column
                String cellFill = account ? (index % 2 == 1 ? GROUP_COLORS[c / 2][0] : GROUP_COLORS[c / 2][1])
                        : grand ? GROUP_COLORS[c / 2][0] : GROUP_COLORS[c / 2][1];
                if (grand && c >= WorksheetRow.BS_DR) {      // the balance sheet's final pair
                    cellFill = ws.isBalanced() ? "E8F6EE" : "FDECEF";
                    ink = ws.isBalanced() ? "1F7A45" : "B4233C";
                }
                int style = st.xf(st.font(bold, false, 11, ink), st.fill(cellFill),
                        st.border(dr ? "medium:C9B8F5" : "thin:E4DDF2", "thin:E4DDF2", topLine, bottomLine),
                        164, "right", false);
                BigDecimal amount = wr.get(c);
                boolean show = amount != null && (grand || amount.signum() != 0);
                cells.append(show ? number(r, 2 + c, style, amount) : blank(r, 2 + c, style));
            }
            rows.add(row(r, account ? 19 : 21, cells));
            r++;
            if (account) {
                index++;
            }
        }

        // ---- summary under the table
        r++;
        int label = st.xf(st.font(true, false, 11, "4C1D95"), st.fill("F6F2FF"),
                st.border("thin:E4DDF2", "thin:E4DDF2", "thin:E4DDF2", "thin:E4DDF2"), 0, "left", false);
        int money = st.xf(st.font(true, false, 11, "231B3A"), st.fill("FFFFFF"),
                st.border("thin:E4DDF2", "thin:E4DDF2", "thin:E4DDF2", "thin:E4DDF2"), 165, "right", false);
        int sumHead = st.xf(st.font(true, false, 12, "FFFFFF"), st.fill("7C3AED"), 0, 0, "left", false);
        StringBuilder sh = new StringBuilder(text(r, 1, sumHead, "Summary")).append(blank(r, 2, sumHead)).append(blank(r, 3, sumHead));
        merges.add("C" + r + ":D" + r);
        rows.add(row(r++, 22, sh));
        boolean loss = ws.getNetIncome() != null && ws.getNetIncome().signum() < 0;
        Object[][] summary = {
            {"Unadjusted trial balance total", ws.getUnadjustedTotal()},
            {"Adjustments total (" + ws.getAdjustingEntryCount() + " entries)", ws.getAdjustmentsTotal()},
            {loss ? "Net loss" : "Net income", ws.getNetIncome() == null ? null : ws.getNetIncome().abs()},
            {"Ending capital", ws.getEndingCapital()},
            {"Balance sheet difference", ws.getBalanceSheetDifference()},
        };
        for (Object[] s : summary) {
            StringBuilder line = new StringBuilder(text(r, 1, label, (String) s[0]));
            BigDecimal v = (BigDecimal) s[1];
            line.append(v == null ? blank(r, 2, money) : number(r, 2, money, v)).append(blank(r, 3, money));
            merges.add("C" + r + ":D" + r);
            rows.add(row(r++, 20, line));
        }
        boolean ok = ws.isBalanced();
        int status = st.xf(st.font(true, false, 11, ok ? "1F7A45" : "B4233C"), st.fill(ok ? "E8F6EE" : "FDECEF"),
                st.border("thin:E4DDF2", "thin:E4DDF2", "thin:E4DDF2", "thin:E4DDF2"), 0, "center", false);
        StringBuilder sl = new StringBuilder(text(r, 1, label, "Status"))
                .append(text(r, 2, status, ok ? "Balanced ✓" : "Not balanced")).append(blank(r, 3, status));
        merges.add("C" + r + ":D" + r);
        rows.add(row(r, 20, sl));

        // ---- the sheet
        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
             .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
             .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
             .append("<sheetPr><tabColor rgb=\"FF5B21B6\"/><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
             .append("<dimension ref=\"A1:").append(last).append(r).append("\"/>")
             .append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\" zoomScale=\"100\">")
             .append("<pane xSplit=\"2\" ySplit=\"5\" topLeftCell=\"C6\" activePane=\"bottomRight\" state=\"frozen\"/>")
             .append("<selection pane=\"bottomRight\" activeCell=\"C6\" sqref=\"C6\"/></sheetView></sheetViews>")
             .append("<sheetFormatPr defaultRowHeight=\"18\"/>")
             .append("<cols><col min=\"1\" max=\"1\" width=\"9\" customWidth=\"1\"/>")
             .append("<col min=\"2\" max=\"2\" width=\"34\" customWidth=\"1\"/>")
             .append("<col min=\"3\" max=\"").append(COLS).append("\" width=\"14.5\" customWidth=\"1\"/></cols>")
             .append("<sheetData>");
        for (String row : rows) {
            sheet.append(row);
        }
        sheet.append("</sheetData><mergeCells count=\"").append(merges.size()).append("\">");
        for (String m : merges) {
            sheet.append("<mergeCell ref=\"").append(m).append("\"/>");
        }
        sheet.append("</mergeCells>")
             .append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.5\" header=\"0.3\" footer=\"0.3\"/>")
             .append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
             .append("</worksheet>");

        // ---- the zip
        try (OutputStream fos = new FileOutputStream(file); ZipOutputStream zip = new ZipOutputStream(fos)) {
            put(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                    + "</Types>");
            put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            put(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                    + "<sheets><sheet name=\"12 Columnar\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                    + "<definedNames><definedName name=\"_xlnm.Print_Titles\" localSheetId=\"0\">'12 Columnar'!$4:$5</definedName></definedNames>"
                    + "</workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                    + "</Relationships>");
            put(zip, "xl/styles.xml", st.toXml());
            put(zip, "xl/worksheets/sheet1.xml", sheet.toString());
        }
    }

    // ------------------------------------------------------------ cells and rows

    private static String col(int c) {
        return String.valueOf((char) ('A' + c));   // 14 columns: A..N
    }

    private static String row(int r, double height, CharSequence cells) {
        return "<row r=\"" + r + "\" ht=\"" + height + "\" customHeight=\"1\">" + cells + "</row>";
    }

    private static String text(int r, int c, int style, String value) {
        return "<c r=\"" + col(c) + r + "\" s=\"" + style + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                + esc(value) + "</t></is></c>";
    }

    private static String number(int r, int c, int style, BigDecimal value) {
        return "<c r=\"" + col(c) + r + "\" s=\"" + style + "\"><v>" + value.toPlainString() + "</v></c>";
    }

    private static String blank(int r, int c, int style) {
        return "<c r=\"" + col(c) + r + "\" s=\"" + style + "\"/>";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Collects the fonts, fills, borders and cell styles used, each only once */
    private static final class Styles {
        private final List<String> fonts = new ArrayList<>();
        private final List<String> fills = new ArrayList<>();
        private final List<String> borders = new ArrayList<>();
        private final Map<String, Integer> xfs = new LinkedHashMap<>();

        Styles() {
            fonts.add("<font><sz val=\"11\"/><color rgb=\"FF2A2240\"/><name val=\"Arial\"/><family val=\"2\"/></font>");
            fills.add("<fill><patternFill patternType=\"none\"/></fill>");
            fills.add("<fill><patternFill patternType=\"gray125\"/></fill>");
            borders.add("<border><left/><right/><top/><bottom/><diagonal/></border>");
            xf(0, 0, 0, 0, null, false);   // style 0 = the default
        }

        int font(boolean bold, boolean italic, double size, String rgb) {
            return index(fonts, "<font>" + (bold ? "<b/>" : "") + (italic ? "<i/>" : "")
                    + "<sz val=\"" + size + "\"/><color rgb=\"FF" + rgb + "\"/><name val=\"Arial\"/><family val=\"2\"/></font>");
        }

        int gradient(String fromRgb, String toRgb) {
            return index(fills, "<fill><gradientFill degree=\"0\"><stop position=\"0\"><color rgb=\"FF" + fromRgb
                    + "\"/></stop><stop position=\"1\"><color rgb=\"FF" + toRgb + "\"/></stop></gradientFill></fill>");
        }

        int fill(String rgb) {
            return index(fills, "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF" + rgb
                    + "\"/><bgColor indexed=\"64\"/></patternFill></fill>");
        }

        /** Each side as "style:RRGGBB" (style = thin, medium or double), or null for none */
        int border(String left, String right, String top, String bottom) {
            return index(borders, "<border>" + side("left", left) + side("right", right)
                    + side("top", top) + side("bottom", bottom) + "<diagonal/></border>");
        }

        private static String side(String name, String spec) {
            if (spec == null) {
                return "<" + name + "/>";
            }
            String[] p = spec.split(":");
            return "<" + name + " style=\"" + p[0] + "\"><color rgb=\"FF" + p[1] + "\"/></" + name + ">";
        }

        int xf(int font, int fill, int border, int numFmt, String align, boolean wrap) {
            String xml = "<xf numFmtId=\"" + numFmt + "\" fontId=\"" + font + "\" fillId=\"" + fill
                    + "\" borderId=\"" + border + "\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\""
                    + " applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\""
                    + (align == null ? "" : " horizontal=\"" + align + "\"") + (wrap ? " wrapText=\"1\"" : "")
                    + " indent=\"" + ("left".equals(align) ? 1 : 0) + "\"/></xf>";
            Integer i = xfs.get(xml);
            if (i == null) {
                i = xfs.size();
                xfs.put(xml, i);
            }
            return i;
        }

        private static int index(List<String> list, String xml) {
            int i = list.indexOf(xml);
            if (i < 0) {
                list.add(xml);
                i = list.size() - 1;
            }
            return i;
        }

        String toXml() {
            StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                    + "<numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.00\"/>"
                    + "<numFmt numFmtId=\"165\" formatCode=\"&quot;₱&quot;#,##0.00\"/></numFmts>");
            b.append("<fonts count=\"").append(fonts.size()).append("\">");
            fonts.forEach(b::append);
            b.append("</fonts><fills count=\"").append(fills.size()).append("\">");
            fills.forEach(b::append);
            b.append("</fills><borders count=\"").append(borders.size()).append("\">");
            borders.forEach(b::append);
            b.append("</borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>");
            b.append("<cellXfs count=\"").append(xfs.size()).append("\">");
            xfs.keySet().forEach(b::append);
            b.append("</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>")
             .append("</styleSheet>");
            return b.toString();
        }
    }
}
