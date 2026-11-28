package tala.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Xlsx
 * ------------------------------------------------------------
 * A very small Excel (.xlsx) writer with colors, fonts, borders,
 * merged cells, frozen headers and print settings - no extra library.
 *
 * An .xlsx file is a zip of XML files. This class collects the
 * styles (each font / fill / border only once), builds each sheet's
 * rows as XML and zips everything up.
 *
 *   Xlsx book = new Xlsx();
 *   Xlsx.Sheet s = book.sheet("General Journal", "5B21B6");
 *   int bold = book.style().font(true, false, 11, "231B3A").fill("EFE8FF").build();
 *   s.row(1, 20).text(0, bold, "Hello").number(1, bold, new BigDecimal("12.50"));
 *   book.write(file);
 */
public final class Xlsx {

    /** Number formats */
    public static final int FMT_NONE = 0, FMT_MONEY = 164, FMT_PESO = 165, FMT_DATE = 166, FMT_DRCR = 167, FMT_INT = 1;

    private final List<String> fonts = new ArrayList<>();
    private final List<String> fills = new ArrayList<>();
    private final List<String> borders = new ArrayList<>();
    private final Map<String, Integer> xfs = new LinkedHashMap<>();
    private final List<Sheet> sheets = new ArrayList<>();

    public Xlsx() {
        fonts.add("<font><sz val=\"11\"/><color rgb=\"FF2A2240\"/><name val=\"Arial\"/><family val=\"2\"/></font>");
        fills.add("<fill><patternFill patternType=\"none\"/></fill>");
        fills.add("<fill><patternFill patternType=\"gray125\"/></fill>");
        borders.add("<border><left/><right/><top/><bottom/><diagonal/></border>");
        xf(0, 0, 0, 0, null, false, 0);   // style 0 = the default
    }

    // ================================================================ styles

    /** Starts a cell style */
    public StyleBuilder style() {
        return new StyleBuilder();
    }

    /** Builds one cell style step by step */
    public final class StyleBuilder {
        private int font = 0, fill = 0, border = 0, numFmt = 0, indent = 0;
        private String align = null;
        private boolean wrap = false;

        /** rgb like "231B3A" */
        public StyleBuilder font(boolean bold, boolean italic, double size, String rgb) {
            font = index(fonts, "<font>" + (bold ? "<b/>" : "") + (italic ? "<i/>" : "")
                    + "<sz val=\"" + size + "\"/><color rgb=\"FF" + rgb + "\"/><name val=\"Arial\"/><family val=\"2\"/></font>");
            return this;
        }

        public StyleBuilder fill(String rgb) {
            fill = index(fills, "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF" + rgb
                    + "\"/><bgColor indexed=\"64\"/></patternFill></fill>");
            return this;
        }

        /** A left-to-right color blend (title banners) */
        public StyleBuilder gradient(String fromRgb, String toRgb) {
            fill = index(fills, "<fill><gradientFill degree=\"0\"><stop position=\"0\"><color rgb=\"FF" + fromRgb
                    + "\"/></stop><stop position=\"1\"><color rgb=\"FF" + toRgb + "\"/></stop></gradientFill></fill>");
            return this;
        }

        /** Each side as "style:RRGGBB" (style = thin, medium, dashed, double), or null for none */
        public StyleBuilder border(String left, String right, String top, String bottom) {
            border = index(borders, "<border>" + side("left", left) + side("right", right)
                    + side("top", top) + side("bottom", bottom) + "<diagonal/></border>");
            return this;
        }

        /** The same line on all four sides */
        public StyleBuilder box(String spec) {
            return border(spec, spec, spec, spec);
        }

        public StyleBuilder format(int numFmt) {
            this.numFmt = numFmt;
            return this;
        }

        /** "left", "center" or "right" */
        public StyleBuilder align(String align) {
            this.align = align;
            return this;
        }

        public StyleBuilder indent(int indent) {
            this.indent = indent;
            return this;
        }

        public StyleBuilder wrap() {
            this.wrap = true;
            return this;
        }

        public int build() {
            return xf(font, fill, border, numFmt, align, wrap, indent);
        }
    }

    private static String side(String name, String spec) {
        if (spec == null) {
            return "<" + name + "/>";
        }
        String[] p = spec.split(":");
        return "<" + name + " style=\"" + p[0] + "\"><color rgb=\"FF" + p[1] + "\"/></" + name + ">";
    }

    private int xf(int font, int fill, int border, int numFmt, String align, boolean wrap, int indent) {
        int ind = indent > 0 ? indent : ("left".equals(align) ? 1 : 0);
        String xml = "<xf numFmtId=\"" + numFmt + "\" fontId=\"" + font + "\" fillId=\"" + fill
                + "\" borderId=\"" + border + "\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\""
                + " applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\""
                + (align == null ? "" : " horizontal=\"" + align + "\"") + (wrap ? " wrapText=\"1\"" : "")
                + " indent=\"" + ind + "\"/></xf>";
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

    // ================================================================ sheets

    /** Adds a sheet (tab). tabRgb colors the tab, e.g. "5B21B6" */
    public Sheet sheet(String name, String tabRgb) {
        Sheet s = new Sheet(name.replaceAll("[\\\\/?*\\[\\]:]", " "), tabRgb);
        sheets.add(s);
        return s;
    }

    /** One worksheet: rows, merged cells, column widths and view / print settings */
    public static final class Sheet {
        private final String name;
        private final String tabRgb;
        private final List<String> rows = new ArrayList<>();
        private final List<String> merges = new ArrayList<>();
        private final List<double[]> widths = new ArrayList<>();
        private int freezeRows = 0, freezeCols = 0;
        private boolean landscape = false;
        private String printTitleRows = null;
        private int lastRow = 1, lastCol = 0;
        private Row open = null;

        private Sheet(String name, String tabRgb) {
            this.name = name;
            this.tabRgb = tabRgb;
        }

        /** Column width (in Excel's "characters") */
        public Sheet width(int col, double width) {
            widths.add(new double[] {col, width});
            return this;
        }

        /** Keeps the first rows / columns visible while scrolling */
        public Sheet freeze(int rows, int cols) {
            this.freezeRows = rows;
            this.freezeCols = cols;
            return this;
        }

        public Sheet landscape() {
            this.landscape = true;
            return this;
        }

        /** Rows repeated at the top of every printed page, e.g. 4 to 4 */
        public Sheet printTitles(int fromRow, int toRow) {
            this.printTitleRows = "$" + fromRow + ":$" + toRow;
            return this;
        }

        /** Merges a block of cells, e.g. merge(1, 0, 1, 5) = A1:F1 */
        public Sheet merge(int r1, int c1, int r2, int c2) {
            merges.add(ref(r1, c1) + ":" + ref(r2, c2));
            return this;
        }

        /** Starts a row (rows must be added top to bottom) */
        public Row row(int r, double height) {
            close();
            open = new Row(this, r, height);
            lastRow = Math.max(lastRow, r);
            return open;
        }

        private void close() {
            if (open != null) {
                rows.add(open.toXml());
                open = null;
            }
        }

        String toXml() {
            close();
            StringBuilder sheet = new StringBuilder();
            sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                 .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                 .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                 .append("<sheetPr>").append(tabRgb == null ? "" : "<tabColor rgb=\"FF" + tabRgb + "\"/>")
                 .append("<pageSetUpPr fitToPage=\"1\"/></sheetPr>")
                 .append("<dimension ref=\"A1:").append(ref(lastRow, Math.max(lastCol, 0))).append("\"/>")
                 .append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\" zoomScale=\"100\">");
            if (freezeRows > 0 || freezeCols > 0) {
                String top = ref(freezeRows + 1, freezeCols);
                String pane = freezeRows > 0 && freezeCols > 0 ? "bottomRight" : freezeRows > 0 ? "bottomLeft" : "topRight";
                sheet.append("<pane").append(freezeCols > 0 ? " xSplit=\"" + freezeCols + "\"" : "")
                     .append(freezeRows > 0 ? " ySplit=\"" + freezeRows + "\"" : "")
                     .append(" topLeftCell=\"").append(top).append("\" activePane=\"").append(pane).append("\" state=\"frozen\"/>")
                     .append("<selection pane=\"").append(pane).append("\" activeCell=\"").append(top)
                     .append("\" sqref=\"").append(top).append("\"/>");
            }
            sheet.append("</sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"18\"/>");
            if (!widths.isEmpty()) {
                sheet.append("<cols>");
                for (double[] w : widths) {
                    int c = (int) w[0] + 1;
                    sheet.append("<col min=\"").append(c).append("\" max=\"").append(c)
                         .append("\" width=\"").append(w[1]).append("\" customWidth=\"1\"/>");
                }
                sheet.append("</cols>");
            }
            sheet.append("<sheetData>");
            for (String row : rows) {
                sheet.append(row);
            }
            sheet.append("</sheetData>");
            if (!merges.isEmpty()) {
                sheet.append("<mergeCells count=\"").append(merges.size()).append("\">");
                for (String m : merges) {
                    sheet.append("<mergeCell ref=\"").append(m).append("\"/>");
                }
                sheet.append("</mergeCells>");
            }
            sheet.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.5\" header=\"0.3\" footer=\"0.3\"/>")
                 .append("<pageSetup orientation=\"").append(landscape ? "landscape" : "portrait")
                 .append("\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
                 .append("</worksheet>");
            return sheet.toString();
        }
    }

    /** One row of cells (cells must be added left to right) */
    public static final class Row {
        private final Sheet sheet;
        private final int r;
        private final double height;
        private final StringBuilder cells = new StringBuilder();

        private Row(Sheet sheet, int r, double height) {
            this.sheet = sheet;
            this.r = r;
            this.height = height;
        }

        public Row text(int c, int style, String value) {
            track(c);
            if (value == null || value.isEmpty()) {
                return blank(c, style);
            }
            cells.append("<c r=\"").append(ref(r, c)).append("\" s=\"").append(style)
                 .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(esc(value)).append("</t></is></c>");
            return this;
        }

        public Row number(int c, int style, BigDecimal value) {
            if (value == null) {
                return blank(c, style);
            }
            track(c);
            cells.append("<c r=\"").append(ref(r, c)).append("\" s=\"").append(style).append("\"><v>")
                 .append(value.toPlainString()).append("</v></c>");
            return this;
        }

        /** A real Excel date (shows with the style's date format) */
        public Row date(int c, int style, LocalDate value) {
            if (value == null) {
                return blank(c, style);
            }
            long serial = ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), value);
            return number(c, style, BigDecimal.valueOf(serial));
        }

        /** Writes whole account codes as numbers (no green "number stored as text" marks) */
        public Row code(int c, int style, String value) {
            String v = value == null ? "" : value.trim();
            return v.matches("[0-9]{1,9}") ? number(c, style, new BigDecimal(v)) : text(c, style, v);
        }

        public Row blank(int c, int style) {
            track(c);
            cells.append("<c r=\"").append(ref(r, c)).append("\" s=\"").append(style).append("\"/>");
            return this;
        }

        /** The same style on a run of empty cells (e.g. the rest of a colored band) */
        public Row blanks(int fromCol, int toCol, int style) {
            for (int c = fromCol; c <= toCol; c++) {
                blank(c, style);
            }
            return this;
        }

        private void track(int c) {
            sheet.lastCol = Math.max(sheet.lastCol, c);
        }

        String toXml() {
            return "<row r=\"" + r + "\" ht=\"" + height + "\" customHeight=\"1\">" + cells + "</row>";
        }
    }

    // ================================================================ writing

    public void write(File file) throws IOException {
        try (OutputStream fos = new FileOutputStream(file); ZipOutputStream zip = new ZipOutputStream(fos)) {
            StringBuilder types = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
            for (int i = 1; i <= sheets.size(); i++) {
                types.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                     .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            }
            types.append("</Types>");
            put(zip, "[Content_Types].xml", types.toString());
            put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");

            StringBuilder book = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rels = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < sheets.size(); i++) {
                Sheet s = sheets.get(i);
                book.append("<sheet name=\"").append(esc(s.name)).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i + 1).append(".xml\"/>");
                if (s.printTitleRows != null) {
                    names.append("<definedName name=\"_xlnm.Print_Titles\" localSheetId=\"").append(i).append("\">'")
                         .append(esc(s.name.replace("'", "''"))).append("'!").append(s.printTitleRows).append("</definedName>");
                }
            }
            book.append("</sheets>");
            if (names.length() > 0) {
                book.append("<definedNames>").append(names).append("</definedNames>");
            }
            book.append("</workbook>");
            rels.append("<Relationship Id=\"rId").append(sheets.size() + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
                .append("</Relationships>");
            put(zip, "xl/workbook.xml", book.toString());
            put(zip, "xl/_rels/workbook.xml.rels", rels.toString());
            put(zip, "xl/styles.xml", stylesXml());
            for (int i = 0; i < sheets.size(); i++) {
                put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheets.get(i).toXml());
            }
        }
    }

    private String stylesXml() {
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"4\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.00\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"&quot;₱&quot;#,##0.00\"/>"
                + "<numFmt numFmtId=\"166\" formatCode=\"mmm d, yyyy\"/>"
                + "<numFmt numFmtId=\"167\" formatCode=\"#,##0.00&quot; Dr&quot;;#,##0.00&quot; Cr&quot;;0.00\"/></numFmts>");
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

    // ================================================================ helpers

    /** Column letters: 0 -> A, 25 -> Z, 26 -> AA */
    public static String col(int c) {
        StringBuilder s = new StringBuilder();
        int n = c + 1;
        while (n > 0) {
            int m = (n - 1) % 26;
            s.insert(0, (char) ('A' + m));
            n = (n - 1) / 26;
        }
        return s.toString();
    }

    public static String ref(int r, int c) {
        return col(c) + r;
    }

    static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                default:
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        b.append(ch);   // other control characters are not allowed in XML
                    }
            }
        }
        return b.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
