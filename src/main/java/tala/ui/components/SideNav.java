package tala.ui.components;

import tala.ui.theme.AppImages;
import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SideNav
 * ------------------------------------------------------------
 * The "ledger book" sidebar of the light design (250px wide):
 *
 *   [logo] TALA                     [<]   <- hides the sidebar
 *          Accounting System
 *   ----------------------------------
 *   OVERVIEW
 *   01  [#] Dashboard                     <- active page: a yellow index tab
 *   RECORD                                   that reaches the edge of the book
 *   02  [/] Journal Entry
 *   03  [B] General Journal
 *   BOOKS ...  REPORTS ...  MANAGE ...
 *
 *   +------------------------------+
 *   | (UA) UCMN                    |      <- who is logged in (green dot = MySQL ok)
 *   |      Admin                   |
 *   | [      -> Log out          ] |
 *   +------------------------------+
 *
 * Deep purple gradient (#26094f -> #2e1065 -> #3b1578) with faint ruled
 * lines and a thin gold margin line, like a ledger page.
 *
 * Hover effects: links fade in a soft white band and their icon and text
 * slide 3px to the right; the hide button and Log out brighten.
 */
public class SideNav extends JPanel {

    public static final int WIDTH = 250;

    /** Section label shown above the first link of each group (by page name) */
    private static final Map<String, String> SECTIONS = new LinkedHashMap<>();
    static {
        SECTIONS.put("Dashboard", "Overview");
        SECTIONS.put("Journal Entry", "Record");
        SECTIONS.put("Chart of Accounts", "Books");
        SECTIONS.put("12 Columnar", "Reports");
        SECTIONS.put("Admin", "Manage");
    }

    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private final JPanel list = new JPanel();
    private final JPanel userSlot = new JPanel(new BorderLayout());
    private final JButton collapse = new CollapseButton();
    private Color statusColor = new Color(0x4ade80);
    private Avatar avatar;

    public SideNav() {
        super(new BorderLayout());
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(20, 0, 16, 0));

        // ---- top: brand + hide button
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(0, 0, 0, 12),
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 20))),
                BorderFactory.createEmptyBorder(0, 8, 14, 0)));
        top.add(createBrand(), BorderLayout.CENTER);
        JPanel collapseHolder = new JPanel(new BorderLayout());
        collapseHolder.setOpaque(false);
        collapseHolder.add(collapse, BorderLayout.NORTH);
        top.add(collapseHolder, BorderLayout.EAST);

        // ---- middle: the links
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(false);
        middle.add(list, BorderLayout.NORTH);

        userSlot.setOpaque(false);
        userSlot.setBorder(BorderFactory.createEmptyBorder(12, 14, 0, 14));

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(userSlot, BorderLayout.SOUTH);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, super.getPreferredSize().height);
    }

    /** What happens when the [<] button is pressed (MainFrame hides the sidebar) */
    public void setOnCollapse(ActionListener listener) {
        collapse.addActionListener(listener);
    }

    /** Adds one link. When clicked, it runs the given action. */
    public void addTab(String pageName, String iconName, ActionListener onClick) {
        String section = SECTIONS.get(pageName);
        if (section != null) {
            JLabel label = new JLabel(section.toUpperCase());
            label.setFont(Theme.spaced(Theme.semibold(9.5f), 0.16f));
            label.setForeground(new Color(201, 184, 245, 150));
            label.setBorder(BorderFactory.createEmptyBorder(items.isEmpty() ? 14 : 18, 46, 5, 0));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(label);
        }
        NavItem item = new NavItem(pageName, iconName, items.size() + 1);
        item.addActionListener(onClick);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        items.put(pageName, item);
        list.add(item);
        list.add(Box.createVerticalStrut(1));
    }

    public void setActive(String pageName) {
        for (Map.Entry<String, NavItem> entry : items.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(pageName));
        }
    }

    /** The logged-in person and a Log out button at the bottom */
    public void setUser(String fullName, String initials, String role, ActionListener onLogout) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel who = new JPanel(new BorderLayout(12, 0));
        who.setOpaque(false);
        avatar = new Avatar(initials, "Admin".equals(role));
        avatar.setToolTipText(fullName + "  -  " + role + " access");
        who.add(avatar, BorderLayout.WEST);

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        String first = fullName.trim().split("\\s+")[0];
        String shown = first.length() > 14 ? first.substring(0, 13) + "." : first;
        JLabel name = new JLabel(shown);
        name.setFont(Theme.bold(14));
        name.setForeground(Color.WHITE);
        name.setToolTipText(fullName);
        JLabel roleLabel = new JLabel("Admin".equals(role) ? "Admin" : "User");
        roleLabel.setFont(Theme.medium(12));
        roleLabel.setForeground(Theme.YELLOW_SOFT);
        words.add(Box.createVerticalGlue());
        words.add(name);
        words.add(roleLabel);
        words.add(Box.createVerticalGlue());
        who.add(words, BorderLayout.CENTER);

        JButton logout = new LogoutButton();
        logout.addActionListener(onLogout);

        card.add(who, BorderLayout.CENTER);
        card.add(logout, BorderLayout.SOUTH);
        userSlot.removeAll();
        userSlot.add(card, BorderLayout.CENTER);
        revalidate();
    }

    /** Green dot = connected to MySQL, red dot = offline (shown on the avatar) */
    public void setConnected(boolean connected) {
        statusColor = connected ? new Color(0x4ade80) : new Color(0xf87171);
        if (avatar != null) {
            avatar.setToolTipText(connected ? "Connected to MySQL" : "MySQL is not reachable");
            avatar.repaint();
        }
    }

    // ------------------------------------------------------------ background

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = Math.max(1, getHeight());
        // linear-gradient(180deg, #26094f 0%, #2e1065 45%, #3b1578 100%)
        g2.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[] {0f, 0.45f, 1f},
                new Color[] {Theme.NAV_TOP, Theme.NAV_BG, Theme.NAV_BOTTOM}));
        g2.fillRect(0, 0, w, h);
        // ruled ledger lines every 32px
        g2.setColor(new Color(255, 255, 255, 8));
        for (int y = 31; y < h; y += 32) {
            g2.fillRect(0, y, w, 1);
        }
        // a quiet gold margin line
        g2.setColor(new Color(253, 221, 68, 26));
        g2.fillRect(32, 0, 1, h);
        // inner edge on the right
        g2.setColor(new Color(255, 255, 255, 15));
        g2.fillRect(w - 1, 0, 1, h);
        g2.dispose();
    }

    // ------------------------------------------------------------ parts

    private JComponent createBrand() {
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.add(new Logo(), BorderLayout.WEST);

        JLabel name = new JLabel("TALA");
        name.setFont(Theme.spaced(Theme.bold(17), 0.14f));
        name.setForeground(Color.WHITE);
        JLabel edition = new JLabel("Accounting System");
        edition.setFont(Theme.spaced(Theme.regular(10.5f), 0.06f));
        edition.setForeground(Theme.NAV_MUTED);

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(name);
        words.add(edition);
        brand.add(words, BorderLayout.CENTER);
        return brand;
    }

    /** One link: number, icon and name. Yellow index tab when active. */
    private static class NavItem extends JButton {
        private boolean active;
        private final String iconName;
        private final String number;

        NavItem(String text, String iconName, int index) {
            super(text);
            this.iconName = iconName;
            this.number = String.format("%02d", index);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setHorizontalAlignment(LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName(text);
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(WIDTH, 38); }
        @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 38); }
        @Override public Dimension getMinimumSize()   { return getPreferredSize(); }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float t = Hover.of(this).value();
            int w = getWidth(), h = getHeight();
            // links stop 14px before the edge; the active tab reaches the edge of the book
            int bodyW = active ? w : w - 14;

            // right-rounded shape (corners 10px on the right only)
            Path2D shape = new Path2D.Float();
            int r = 10;
            shape.moveTo(0, 0);
            shape.lineTo(bodyW - r, 0);
            shape.quadTo(bodyW, 0, bodyW, r);
            shape.lineTo(bodyW, h - r);
            shape.quadTo(bodyW, h, bodyW - r, h);
            shape.lineTo(0, h);
            shape.closePath();

            if (active) {
                // shadow "4px 3px 10px rgba(0,0,0,.18)"
                for (int i = 4; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 10));
                    g2.translate(i * 0.8, i * 0.7);
                    g2.fill(shape);
                    g2.translate(-i * 0.8, -i * 0.7);
                }
                g2.setPaint(new LinearGradientPaint(0, 0, bodyW, 0, new float[] {0f, 0.4f, 1f},
                        new Color[] {new Color(0xfde68a), Theme.YELLOW, Theme.GOLD_DARK}));
                g2.fill(shape);
                // a soft shine on hover
                if (t > 0) {
                    g2.setColor(new Color(255, 255, 255, Math.round(40 * t)));
                    g2.fill(shape);
                }
            } else if (t > 0) {
                g2.setColor(new Color(255, 255, 255, Math.round(18 * t)));
                g2.fill(shape);
                // a thin gold marker on the left edge
                g2.setColor(new Color(253, 221, 68, Math.round(170 * t)));
                g2.fillRoundRect(0, 9, 3, h - 18, 3, 3);
            }

            float slide = active ? 0 : 3f * t;

            // number "01" (mono, right-aligned in a 20px box at x=6)
            Font mono = Theme.mono(10f);
            g2.setFont(mono);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(active ? Theme.PURPLE : new Color(253, 221, 68, Math.round(140 + 60 * t)));
            g2.drawString(number, 26 - fm.stringWidth(number), (h + fm.getAscent() - fm.getDescent()) / 2);

            // icon
            Color ink = active ? Theme.PURPLE_DEEP : Theme.mix(Theme.NAV_TEXT, Color.WHITE, t);
            javax.swing.Icon icon = Icons.get(iconName, 16, active ? ink : Theme.alpha(ink, Math.round(190 + 65 * t)));
            icon.paintIcon(this, g2, Math.round(46 + slide), (h - 16) / 2);

            // label
            Font font = active ? Theme.semibold(13f) : Theme.medium(13f);
            g2.setFont(font);
            fm = g2.getFontMetrics();
            g2.setColor(ink);
            g2.drawString(getText(), 46 + 26 + slide, (h + fm.getAscent() - fm.getDescent()) / 2);

            if (isFocusOwner()) {
                g2.setColor(Theme.YELLOW);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, Math.min(bodyW, w) - 3, h - 3, 10, 10));
            }
            g2.dispose();
        }
    }

    /** The small square [<] button that hides the sidebar */
    private static class CollapseButton extends JButton {
        CollapseButton() {
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Hide menu");
            getAccessibleContext().setAccessibleName("Hide menu");
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(30, 30); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float t = Hover.of(this).value();
            g2.setColor(new Color(255, 255, 255, Math.round(20 + 22 * t)));
            g2.fillRoundRect(1, 1, 27, 27, 16, 16);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.drawRoundRect(1, 1, 27, 27, 16, 16);
            g2.setColor(Theme.mix(new Color(0xe3d6ff), Color.WHITE, t));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float dx = -1.5f * t;   // the chevron nudges left on hover
            Path2D p = new Path2D.Float();
            p.moveTo(17 + dx, 9);
            p.lineTo(12 + dx, 15);
            p.lineTo(17 + dx, 21);
            g2.draw(p);
            if (isFocusOwner()) {
                g2.setColor(Theme.YELLOW);
                g2.drawRoundRect(0, 0, 29, 29, 16, 16);
            }
            g2.dispose();
        }
    }

    /** Full-width "Log out" button in the user card */
    private static class LogoutButton extends JButton {
        LogoutButton() {
            super("Log out");
            setFont(Theme.semibold(13.5f));
            setForeground(Theme.YELLOW);
            setIcon(Icons.get(Icons.LOGOUT, 16, Theme.YELLOW));
            setIconTextGap(8);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(100, 38); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float t = Hover.of(this).value();
            g2.setColor(new Color(255, 255, 255, Math.round(31 + 20 * t)));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.setColor(new Color(255, 255, 255, Math.round(64 + 30 * t)));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            if (isFocusOwner()) {
                g2.setColor(Theme.YELLOW);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            }
            g2.dispose();
            setForeground(Theme.mix(Theme.YELLOW, new Color(0xfef08a), t));
            super.paintComponent(g);
        }
    }

    /** Round initials badge with the MySQL status dot */
    private class Avatar extends JComponent {
        private final String initials;
        private final boolean admin;

        Avatar(String initials, boolean admin) {
            this.initials = initials;
            this.admin = admin;
            Dimension size = new Dimension(38, 38);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int y = (getHeight() - 38) / 2;
            g2.setPaint(admin ? new GradientPaint(0, y, Theme.YELLOW, 38, y + 38, Theme.GOLD_DARK)
                    : new GradientPaint(0, y, new Color(0xddd6fe), 38, y + 38, new Color(0xc4b5fd)));
            g2.fillOval(0, y, 38, 38);
            g2.setColor(Theme.PURPLE_DEEP);
            g2.setFont(Theme.bold(13.5f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (38 - fm.stringWidth(initials)) / 2, y + (38 + fm.getAscent() - fm.getDescent()) / 2);
            g2.setColor(Theme.NAV_BOTTOM);
            g2.fillOval(26, y + 26, 13, 13);
            g2.setColor(statusColor);
            g2.fillOval(28, y + 28, 9, 9);
            g2.dispose();
        }
    }

    /** The TALA logo in a rounded square (30px) */
    private static class Logo extends JComponent {
        private final BufferedImage picture = AppImages.getLogo(60);

        Logo() {
            setPreferredSize(new Dimension(30, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int y = 3;
            if (picture != null) {
                g2.setPaint(new TexturePaint(picture, new Rectangle(0, y, 30, 30)));
                g2.fillRoundRect(0, y, 30, 30, 16, 16);
            } else {
                g2.setColor(Theme.YELLOW);
                g2.fillRoundRect(0, y, 30, 30, 16, 16);
                g2.setColor(Theme.PURPLE_DEEP);
                g2.setFont(Theme.bold(18));
                int w = g2.getFontMetrics().stringWidth("T");
                g2.drawString("T", (30 - w) / 2, y + 22);
            }
            g2.dispose();
        }
    }
}
