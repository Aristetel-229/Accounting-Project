package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.service.AccountingService;
import tala.ui.MainFrame;
import tala.ui.components.ScrollablePanel;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * BasePage
 * ------------------------------------------------------------
 * What every page (Dashboard, Journal Entry, ...) has in common:
 *   - access to the DAOs and the AccountingService
 *   - a scrolling column where sections are stacked top to bottom
 *   - refreshData(), called every time the page is opened
 *   - helpers for showing messages
 */
public abstract class BasePage extends JPanel {

    protected final MainFrame mainFrame;
    protected final AccountDAO accountDAO;
    protected final JournalDAO journalDAO;
    protected final AccountingService accounting = new AccountingService();

    /** The page column (see-through: the page behind it paints the lavender-to-cream backdrop) */
    private final ScrollablePanel column = new ScrollablePanel(new GridBagLayout()) {
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return fixedHeight || super.getScrollableTracksViewportHeight();
        }
    };
    private int nextRow = 0;
    /** true = the page itself never scrolls; its last section fills the window and scrolls inside */
    private boolean fixedHeight = false;

    protected BasePage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(new BorderLayout());
        this.mainFrame = mainFrame;
        this.accountDAO = accountDAO;
        this.journalDAO = journalDAO;
        setOpaque(true);

        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(22, 28, 28, 28));

        JScrollPane scroll = new JScrollPane(column);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        // the backdrop stays still while the content scrolls over it
        scroll.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        // "TALA · Accounting System" footer line, like the design
        javax.swing.JLabel footer = new javax.swing.JLabel("TALA · Accounting System");
        footer.setFont(Theme.regular(11.5f));
        footer.setForeground(new Color(0x6e6787));
        footer.setBorder(BorderFactory.createEmptyBorder(6, 28, 10, 28));
        add(footer, BorderLayout.SOUTH);
    }

    /** Lavender-to-cream page background with soft yellow and violet glows */
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        tala.ui.theme.Backdrop.paint((java.awt.Graphics2D) g, getWidth(), getHeight());
    }

    /** When the sidebar is hidden, the floating "Menu" button needs room above the page title */
    public void setMenuSpace(boolean menuShown) {
        column.setBorder(BorderFactory.createEmptyBorder(menuShown ? 62 : 22, 28, fixedHeight ? 4 : 28, 28));
        column.revalidate();
    }

    /**
     * Fixed layout: the page does not scroll as a whole. The section added
     * with addFillSection() takes all the height that is left, and its
     * cards scroll on the inside (General Journal, General Ledger, Chart of Accounts).
     */
    protected void useFixedHeight() {
        fixedHeight = true;
        column.setBorder(BorderFactory.createEmptyBorder(22, 28, 4, 28));
    }

    /** The last section of a fixed page: it stretches to the bottom of the window */
    protected void addFillSection(JComponent section) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = nextRow++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        column.add(flexible(section), c);
    }

    /** Two columns that both stretch to the full height (for fixed pages) */
    protected JPanel fillColumns(JComponent left, double leftWeight, JComponent right, double rightWeight) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = leftWeight;
        c.insets = new Insets(0, 0, 0, 8);
        row.add(flexible(left), c);
        c.gridx = 1;
        c.weightx = rightWeight;
        c.insets = new Insets(0, 8, 0, 0);
        row.add(flexible(right), c);
        return row;
    }

    /** Wraps a component so it asks for (almost) no width or height: the layout decides both */
    private JComponent flexible(JComponent child) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(1, 240);
            }

            @Override
            public java.awt.Dimension getMinimumSize() {
                return new java.awt.Dimension(1, 160);
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(child, BorderLayout.CENTER);
        return wrapper;
    }

    /** Reloads the page's data from MySQL. Called each time the page is shown. */
    public abstract void refreshData();

    /** Adds a section (header, card, row of cards...) under the previous one */
    protected void addSection(JComponent section) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = nextRow++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 20, 0);
        column.add(shareWidth(section), c);
    }

    /** Call once after the last addSection: pushes everything to the top */
    protected void finishSections() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = nextRow++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        column.add(filler, c);
    }

    /**
     * Two columns side by side (e.g. a table on the left and a form on the right).
     * leftWeight / rightWeight decide how the width is shared.
     */
    protected JPanel twoColumns(JComponent left, double leftWeight, JComponent right, double rightWeight) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;

        c.gridx = 0;
        c.weightx = leftWeight;
        c.insets = new Insets(0, 0, 0, 10);
        row.add(shareWidth(left), c);

        c.gridx = 1;
        c.weightx = rightWeight;
        c.insets = new Insets(0, 10, 0, 0);
        row.add(shareWidth(right), c);
        return row;
    }

    /**
     * Wraps a component so it asks for (almost) no width of its own.
     * GridBagLayout then shares the width ONLY by the weights, e.g. 60% / 40%.
     */
    private JComponent shareWidth(JComponent child) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(1, super.getPreferredSize().height);
            }

            @Override
            public java.awt.Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(child, BorderLayout.CENTER);
        return wrapper;
    }

    /** A column of components with a gap between them (used inside twoColumns) */
    protected JPanel stack(JComponent... parts) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        for (int i = 0; i < parts.length; i++) {
            c.gridy = i;
            c.insets = new Insets(0, 0, i < parts.length - 1 ? 20 : 0, 0);
            panel.add(parts[i], c);
        }
        // an invisible filler at the bottom keeps the parts at the top
        c.gridy = parts.length;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, c);
        return panel;
    }

    // ------------------------------------------------------------ exports

    /** The folder of the last export, so the next save window opens there */
    private static java.io.File lastExportFolder = null;

    /**
     * Asks where to save an export. extension = "xlsx" or "csv".
     * Returns null when cancelled (or when the user did not want to replace a file).
     */
    protected java.io.File chooseExportFile(String title, String baseName, String extension) {
        boolean excel = extension.equals("xlsx");
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(lastExportFolder);
        chooser.setDialogTitle(title);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                excel ? "Excel workbook, colored (*.xlsx)" : "CSV, plain text (*.csv)", extension));
        chooser.setSelectedFile(new java.io.File(baseName + "." + extension));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return null;
        }
        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith("." + extension)) {
            file = new java.io.File(file.getParentFile(), file.getName() + "." + extension);
        }
        lastExportFolder = file.getParentFile();
        if (file.exists() && !confirm("Replace file", file.getName() + " already exists. Replace it?", "Replace", "Cancel")) {
            return null;
        }
        return file;
    }

    /** "Saved to ..." with a button that opens the file right away */
    protected void showSaved(java.io.File file) {
        Object[] options = {"Open file", "OK"};
        int choice = JOptionPane.showOptionDialog(this, "Saved to " + file.getAbsolutePath(), "Export finished",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception e) {
                showError("The file was saved, but it could not be opened here.", e);
            }
        }
    }

    /** A CSV value: quoted when it holds a comma, a quote or a line break */
    protected static String csv(Object value) {
        String v = value == null ? "" : value.toString();
        return v.matches("(?s).*[\",\\r\\n].*") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    /** A plain number for CSV files (no peso sign, no thousands commas) */
    protected static String csvAmount(java.math.BigDecimal amount) {
        return amount == null || amount.signum() == 0 ? "" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    // ------------------------------------------------------------ messages

    protected void showError(String message, Exception e) {
        String details = e == null ? "" : "\n\nDetails: " + e.getMessage();
        JOptionPane.showMessageDialog(this, message + details, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "TALA", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Yes/No question. Returns true if the user picked the first (yes) button. */
    protected boolean confirm(String title, String message, String yesText, String noText) {
        Object[] options = {yesText, noText};
        int choice = JOptionPane.showOptionDialog(this, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }
}
