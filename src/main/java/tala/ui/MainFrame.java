package tala.ui;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.dao.SettingsDAO;
import tala.dao.UserDAO;
import tala.model.User;
import tala.service.AuthService;
import tala.ui.components.Icons;
import tala.ui.components.SideNav;
import tala.ui.pages.AdminPage;
import tala.ui.pages.BasePage;
import tala.ui.pages.ChartOfAccountsPage;
import tala.ui.pages.ColumnarPage;
import tala.ui.pages.DashboardPage;
import tala.ui.pages.GeneralJournalPage;
import tala.ui.pages.GeneralLedgerPage;
import tala.ui.pages.JournalEntryPage;
import tala.ui.theme.AppImages;
import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MainFrame
 * ------------------------------------------------------------
 * The main window (light design). It holds:
 *   - the purple "ledger book" SideNav on the left
 *   - all pages, stacked in a CardLayout (only one is visible at a time)
 *   - a small "Menu" button that appears when the sidebar is hidden
 *
 * showPage("Journal Entry") switches pages and reloads that page's data.
 *
 * Who sees what:
 *   User  (user access)    -> the six accounting pages
 *   Admin   (admin access) -> the six accounting pages + the Admin page
 */
public class MainFrame extends JFrame {

    // Page names (also used as the sidebar labels)
    public static final String DASHBOARD = "Dashboard";
    public static final String JOURNAL_ENTRY = "Journal Entry";
    public static final String GENERAL_JOURNAL = "General Journal";
    public static final String CHART_OF_ACCOUNTS = "Chart of Accounts";
    public static final String GENERAL_LEDGER = "General Ledger";
    public static final String COLUMNAR = "12 Columnar";
    public static final String ADMIN = "Admin";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pageArea = new PageArea();
    private final SideNav sideNav = new SideNav();
    private final JButton menuButton = new MenuButton();
    private final SlideLayer slide = new SlideLayer();
    private final Map<String, BasePage> pages = new LinkedHashMap<>();
    private final User currentUser;
    private final SettingsDAO settingsDAO;
    private final UserDAO userDAO;

    /** Where the sidebar's left edge is (0 = open, -SideNav.WIDTH = hidden); animated */
    private int sideX = 0;
    /** Where the pages start; only changes once per slide (so the pages are not re-laid-out every frame) */
    private int pageLeft = SideNav.WIDTH;
    private boolean navOpen = true;
    private Timer navTimer;

    public MainFrame(User currentUser, AccountDAO accountDAO, JournalDAO journalDAO,
                     SettingsDAO settingsDAO, UserDAO userDAO) {
        super("TALA - Accounting System  -  " + currentUser.getFullName()
                + (currentUser.isAdmin() ? "  [Admin]" : "  [User]"));
        this.currentUser = currentUser;
        this.settingsDAO = settingsDAO;
        this.userDAO = userDAO;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1366, 860);
        setLocationRelativeTo(null);   // center on screen
        setIconImages(AppImages.getWindowIcons());   // the TALA logo in the title bar and taskbar

        // 1) create every page once (sidebar order = the design's ledger index)
        addPage(DASHBOARD, Icons.DASHBOARD, new DashboardPage(this, accountDAO, journalDAO));
        addPage(JOURNAL_ENTRY, Icons.PEN, new JournalEntryPage(this, accountDAO, journalDAO, settingsDAO));
        addPage(GENERAL_JOURNAL, Icons.BOOK, new GeneralJournalPage(this, accountDAO, journalDAO));
        addPage(CHART_OF_ACCOUNTS, Icons.LIST, new ChartOfAccountsPage(this, accountDAO, journalDAO));
        addPage(GENERAL_LEDGER, Icons.LAYERS, new GeneralLedgerPage(this, accountDAO, journalDAO));
        addPage(COLUMNAR, Icons.TABLE, new ColumnarPage(this, accountDAO, journalDAO));
        if (currentUser.isAdmin()) {   // admin access only
            addPage(ADMIN, Icons.SHIELD, new AdminPage(this, accountDAO, journalDAO, settingsDAO, userDAO));
        }
        sideNav.setUser(currentUser.getFullName(), currentUser.getInitials(), currentUser.getRole(), e -> logOut());
        sideNav.setOnCollapse(e -> setNavOpen(false));
        menuButton.addActionListener(e -> setNavOpen(true));
        menuButton.setVisible(false);

        // 2) put the window together: sidebar | pages, the Menu button floats on top
        JPanel root = new JPanel(null) {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false;   // the Menu button overlaps the page
            }

            @Override
            public void doLayout() {
                int w = getWidth(), h = getHeight();
                sideNav.setBounds(sideX, 0, SideNav.WIDTH, h);
                pageArea.setBounds(pageLeft, 0, w - pageLeft, h);
                Dimension m = menuButton.getPreferredSize();
                menuButton.setBounds(16, 14, m.width, m.height);
            }
        };
        root.setBackground(Theme.PAGE_BG);
        root.add(menuButton);   // first = on top
        root.add(sideNav);
        root.add(pageArea);
        setContentPane(root);

        // 3) first page: Admin for admins, Dashboard for users
        //    (keyboard focus goes to the page, not to the first link)
        showPage(currentUser.isAdmin() ? ADMIN : DASHBOARD);
        pageArea.setFocusable(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                pageArea.requestFocusInWindow();
            }
        });
    }

    private void addPage(final String name, String iconName, BasePage page) {
        pages.put(name, page);
        pageArea.add(page, name);
        sideNav.addTab(name, iconName, e -> showPage(name));
    }

    /**
     * Slides the sidebar out (open = false) or back in (open = true) in about 0.3 s.
     *
     * Before the slide, the page is pictured twice: laid out WITH the sidebar
     * and laid out at FULL width. During the slide only pictures move on an
     * overlay (the window's glass pane): the "with sidebar" page slides along
     * with the sidebar while the other layout fades in, so the content glides
     * into place instead of jumping. The pictures are drawn 1:1 at the
     * screen's own resolution (no scaling per frame), which keeps it smooth.
     */
    private void setNavOpen(boolean open) {
        if (navOpen == open || (navTimer != null && navTimer.isRunning())) {
            return;
        }
        navOpen = open;
        menuButton.setVisible(false);

        // picture 1: the page as it is now
        java.awt.image.BufferedImage before = snapshot(pageArea);
        // lay the page out for the new state, then picture 2
        pageLeft = open ? SideNav.WIDTH : 0;
        for (BasePage page : pages.values()) {
            page.setMenuSpace(!open);
        }
        getContentPane().revalidate();
        getContentPane().validate();
        java.awt.image.BufferedImage after = snapshot(pageArea);
        java.awt.image.BufferedImage nav = snapshot(sideNav);

        // "narrow" = the page laid out next to the sidebar, "full" = without it
        slide.start(open ? after : before, open ? before : after, nav);
        setGlassPane(slide);
        slide.setVisible(true);

        final int from = open ? -SideNav.WIDTH : 0, to = open ? 0 : -SideNav.WIDTH;
        final long start = System.nanoTime();
        final int duration = 300;
        navTimer = new Timer(8, e -> {
            float t = Math.min(1f, (System.nanoTime() - start) / 1_000_000f / duration);
            // ease-in-out (smootherstep): gentle start, gentle stop
            float eased = t * t * t * (t * (t * 6 - 15) + 10);
            slide.setFrame(Math.round(from + (to - from) * eased));
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                sideX = to;
                getContentPane().revalidate();
                getContentPane().validate();
                slide.setVisible(false);
                slide.clear();
                menuButton.setVisible(!navOpen);
                getContentPane().repaint();
            }
        });
        navTimer.setCoalesce(true);
        navTimer.start();
    }

    /** A picture of a component at the screen's own pixel size, in a fast (accelerated) image */
    private java.awt.image.BufferedImage snapshot(javax.swing.JComponent c) {
        java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        double scale = gc == null ? 1 : gc.getDefaultTransform().getScaleX();
        int w = Math.max(1, c.getWidth()), h = Math.max(1, c.getHeight());
        int pw = (int) Math.ceil(w * scale), ph = (int) Math.ceil(h * scale);
        java.awt.image.BufferedImage img = gc == null
                ? new java.awt.image.BufferedImage(pw, ph, java.awt.image.BufferedImage.TYPE_INT_RGB)
                : gc.createCompatibleImage(pw, ph, java.awt.Transparency.OPAQUE);
        Graphics2D g = img.createGraphics();
        g.setColor(Theme.PAGE_BG);
        g.fillRect(0, 0, pw, ph);
        g.scale(scale, scale);
        c.paint(g);
        g.dispose();
        return img;
    }

    /** Switches to a page and reloads its data */
    public void showPage(String name) {
        BasePage page = pages.get(name);
        if (page == null) {
            return;
        }
        page.refreshData();
        cardLayout.show(pageArea, name);
        sideNav.setActive(name);
    }

    /** Opens the 12 Columnar page and its Add adjustment pop-up */
    public void openAddAdjustment() {
        showPage(COLUMNAR);
        ((ColumnarPage) pages.get(COLUMNAR)).openAdjustmentDialog();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public SettingsDAO getSettingsDAO() {
        return settingsDAO;
    }

    /** Asks, then closes this window and goes back to the log-in screen */
    public void logOut() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out of TALA?", "Log out",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        new AuthService(userDAO, settingsDAO).logout();
        dispose();
        new LoginFrame().setVisible(true);
    }

    public void setDatabaseStatus(boolean connected) {
        sideNav.setConnected(connected);
    }

    // ------------------------------------------------------------ small inner components

    /** The page stack; it draws the sidebar's soft shadow over the page's left edge */
    private class PageArea extends JPanel {
        PageArea() {
            super(cardLayout);
            setBackground(Theme.PAGE_BG);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // the sidebar's soft shadow "6px 0 28px rgba(46,16,101,.22)" (only while it is open)
            if (pageLeft > 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(46, 16, 101, 46), 22, 0, new Color(46, 16, 101, 0)));
                g2.fillRect(0, 0, 22, getHeight());
                g2.dispose();
            }
        }
    }

    /** The overlay that shows the sliding pictures of the page and the sidebar */
    private static class SlideLayer extends JPanel {
        private java.awt.image.BufferedImage narrow, full, nav;
        private int sideX;

        SlideLayer() {
            setOpaque(true);
            setDoubleBuffered(true);
            // swallow clicks while sliding
            addMouseListener(new java.awt.event.MouseAdapter() { });
        }

        void start(java.awt.image.BufferedImage narrow, java.awt.image.BufferedImage full, java.awt.image.BufferedImage nav) {
            this.narrow = narrow;
            this.full = full;
            this.nav = nav;
        }

        /** Moves to the next frame and draws it right away (steady frames, no waiting in the repaint queue) */
        void setFrame(int x) {
            sideX = x;
            if (isShowing()) {
                paintImmediately(0, 0, getWidth(), getHeight());
                java.awt.Toolkit.getDefaultToolkit().sync();
            }
        }

        void clear() {
            narrow = null;
            full = null;
            nav = null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (narrow == null || full == null || nav == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            // draw in device pixels: the pictures were made at the screen's scale, so no resampling
            java.awt.geom.AffineTransform t = g2.getTransform();
            double scale = t.getScaleX();
            g2.setTransform(java.awt.geom.AffineTransform.getTranslateInstance(t.getTranslateX(), t.getTranslateY()));
            int w = (int) Math.ceil(getWidth() * scale), h = (int) Math.ceil(getHeight() * scale);
            int navW = (int) Math.round(SideNav.WIDTH * scale);
            int edge = (int) Math.round((sideX + SideNav.WIDTH) * scale);   // where the sidebar ends
            float closed = -sideX / (float) SideNav.WIDTH;                  // 0 = open ... 1 = hidden

            // the full-width page underneath, then the page-next-to-the-sidebar on top, fading out as it closes
            g2.drawImage(full, 0, 0, null);
            if (closed < 1f) {
                java.awt.Composite old = g2.getComposite();
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f - closed));
                g2.drawImage(narrow, edge, 0, null);
                g2.setComposite(old);
            }
            if (edge > 0) {
                int sh = (int) Math.round(22 * scale);
                g2.setPaint(new GradientPaint(edge, 0, new Color(46, 16, 101, 46), edge + sh, 0, new Color(46, 16, 101, 0)));
                g2.fillRect(edge, 0, sh, h);
                g2.drawImage(nav, edge - navW, 0, null);
            }
            g2.dispose();
        }
    }

    /** The purple "Menu" pill shown when the sidebar is hidden */
    private static class MenuButton extends JButton {
        MenuButton() {
            super("Menu");
            setFont(Theme.semibold(12.5f));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Show menu");
            setHorizontalAlignment(RIGHT);
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(36 + getFontMetrics(getFont()).stringWidth(getText()) + 16, 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float t = Hover.of(this).value();
            int w = getWidth(), h = getHeight() - 2;
            g2.setColor(new Color(46, 16, 101, Math.round(40 + 30 * t)));
            g2.fillRoundRect(1, 3, w - 2, h - 1, 20, 20);
            g2.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.PURPLE_DEEP, new Color(0x3b1578), t), w, h,
                    Theme.mix(Theme.PURPLE_DARK, new Color(0x6d28d9), t)));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 20, 20);
            // hamburger icon
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = h / 2;
            g2.drawLine(12, cy - 5, 26, cy - 5);
            g2.drawLine(12, cy, 26, cy);
            g2.drawLine(12, cy + 5, 22 + Math.round(4 * t), cy + 5);
            if (isFocusOwner()) {
                g2.setColor(Theme.YELLOW);
                g2.drawRoundRect(1, 1, w - 3, h - 3, 20, 20);
            }
            // the word "Menu" to the right of the icon
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), 36, (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}
