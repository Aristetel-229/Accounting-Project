package tala.ui.components;

import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

/**
 * ExportMenu
 * ------------------------------------------------------------
 * The "Export v" button of the design. It opens a small white menu:
 *
 *   EXPORT GENERAL JOURNAL
 *   [csv]  CSV file        Opens in any spreadsheet app
 *   [xls]  Excel file      Colored and formatted, .xlsx
 *   [pdf]  PDF / Print     Save as PDF from the print window
 *
 * Each choice runs the action the page gave it.
 */
public class ExportMenu extends AppButton {

    private final JPopupMenu popup = new JPopupMenu();
    private boolean open = false;
    private long closedAt = 0;

    public ExportMenu(String what, Runnable csv, Runnable excel, Runnable pdf) {
        super("Export", Icons.get(Icons.DOWNLOAD, 16, Theme.PURPLE_DEEP), Style.SECONDARY);
        setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 36));   // room for the chevron
        setToolTipText("Save the " + what + " as CSV, a colored Excel workbook, or PDF");

        JPanel list = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(new Color(0xe2d9f5));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.dispose();
            }
        };
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel head = new JLabel(("Export " + what).toUpperCase(Locale.ROOT));
        head.setFont(Theme.spaced(Theme.semibold(11f), 0.12f));
        head.setForeground(new Color(0x7c6f99));
        head.setBorder(BorderFactory.createEmptyBorder(7, 10, 5, 10));
        head.setAlignmentX(LEFT_ALIGNMENT);
        list.add(head);
        list.add(new Item(Icons.FILE, new Color(0xe5f5ec), new Color(0x15803d),
                "CSV file", "Opens in any spreadsheet app", csv));
        list.add(new Item(Icons.SHEET, new Color(0xe8ecfc), new Color(0x3046b8),
                "Excel file", "Colored and formatted, .xlsx", excel));
        list.add(new Item(Icons.PRINTER, new Color(0xfce8ef), new Color(0xb4235e),
                "PDF / Print", "Save as PDF from the print window", pdf));

        popup.setBorder(BorderFactory.createEmptyBorder());
        popup.setOpaque(false);
        popup.setBackground(new Color(0, 0, 0, 0));
        popup.add(list);
        popup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) { open = true; repaint(); }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                open = false;
                closedAt = System.currentTimeMillis();
                repaint();
            }
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });

        addActionListener(e -> {
            // a click on the button while the menu is open just closes it
            if (System.currentTimeMillis() - closedAt < 250) {
                return;
            }
            Dimension size = popup.getPreferredSize();
            popup.show(this, getWidth() - size.width, getHeight() + 2);
        });
    }

    /** Draws a small chevron after the text; it flips while the menu is open */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float cx = getWidth() - 24, cy = 2 + (getHeight() - 6) / 2f;
        g2.setColor(Theme.alpha(getForeground(), 190));
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        java.awt.geom.Path2D.Float chev = new java.awt.geom.Path2D.Float();
        float d = open ? -1 : 1;
        chev.moveTo(cx - 4, cy - 2 * d);
        chev.lineTo(cx, cy + 2 * d);
        chev.lineTo(cx + 4, cy - 2 * d);
        g2.draw(chev);
        g2.dispose();
    }

    /** One choice in the menu: a colored icon square, a bold name and a small line under it */
    private class Item extends JPanel {
        private final Runnable action;

        Item(String icon, Color iconBg, Color iconColor, String title, String hint, Runnable action) {
            super(new BorderLayout(10, 0));
            this.action = action;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            setAlignmentX(LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            getAccessibleContext().setAccessibleName(title + ". " + hint);

            JComponent box = new IconBox(icon, 32, iconBg, iconColor);
            JPanel boxHolder = new JPanel(new java.awt.GridBagLayout());
            boxHolder.setOpaque(false);
            boxHolder.add(box);
            add(boxHolder, BorderLayout.WEST);

            JLabel t = new JLabel(title);
            t.setFont(Theme.semibold(13.5f));
            t.setForeground(new Color(0x1b1530));
            JLabel h = new JLabel(hint);
            h.setFont(Theme.regular(11.5f));
            h.setForeground(new Color(0x7d7696));
            JPanel texts = new JPanel(new GridLayout(2, 1));
            texts.setOpaque(false);
            texts.add(t);
            texts.add(h);
            add(texts, BorderLayout.CENTER);

            Hover.of(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) {
                        run();
                    }
                }
            });
            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                        run();
                    }
                }
            });
        }

        private void run() {
            popup.setVisible(false);
            // let the menu close and the window repaint before a save window opens
            SwingUtilities.invokeLater(action);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(268, d.width), d.height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            float t = Hover.of(this).value();
            if (t > 0.01f) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.alpha(new Color(0xf4efff), Math.round(255 * t)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
