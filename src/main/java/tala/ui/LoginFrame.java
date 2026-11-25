package tala.ui;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.dao.SettingsDAO;
import tala.dao.UserDAO;
import tala.database.DBConnection;
import tala.service.AuthService;
import tala.ui.components.Icons;
import tala.ui.theme.AppImages;
import tala.ui.theme.Hover;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.prefs.Preferences;

/**
 * LoginFrame
 * ------------------------------------------------------------
 * The first window. Users and admins log in on the same screen;
 * the role saved in MySQL decides what they see afterwards:
 *
 *   User  -> the accounting pages
 *   Admin -> the accounting pages + the Admin page
 *
 * Look (light design, "TALA Screens (Light)" > Log in):
 *   a soft lavender sky over the campus picture, a white card with the
 *   form on the left and a slanted deep-purple "WELCOME BACK!" panel on the
 *   right (yellow edge, a small journal-entry preview, 7 / 12 / 2 stats).
 *
 * Hover effects: the Log in button rises and glows, links underline,
 * the journal preview straightens up, the stat boxes and chips brighten,
 * and the field underline turns violet while you type.
 */
public class LoginFrame extends JFrame {

    // colors of the light log-in screen
    private static final Color INK = new Color(0x231b3a);
    private static final Color SOFT = new Color(0x5c5577);
    private static final Color MUTED = new Color(0x7a7392);
    private static final Color HINT = new Color(0xa39cb8);
    private static final Color LINE = new Color(0xcfc8e0);
    private static final Color LINK = new Color(0x6d4fc9);
    private static final Color LINK_HOVER = new Color(0x4c35a0);
    private static final Color ERROR = new Color(0xb4233c);
    private static final Color GOLD_1 = new Color(0xfddd44);
    private static final Color GOLD_2 = new Color(0xf7af1a);
    private static final Color VIOLET = new Color(0x6d28d9);

    private static final String PREF_USERNAME = "remembered_username";

    private final UserDAO userDAO = new UserDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final AuthService auth = new AuthService(userDAO, settingsDAO);

    private final HintField usernameField = new HintField(false, "Enter your username");
    private final HintField passwordField = new HintField(true, "Enter your password");
    private final JCheckBox rememberBox = new JCheckBox("Remember me");
    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame() {
        super("TALA - Log in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 720));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setIconImages(AppImages.getWindowIcons());

        Sky sky = new Sky();
        sky.add(createTopBar(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridBagLayout());   // centers the card
        middle.setOpaque(false);
        middle.add(new LoginCard());
        sky.add(middle, BorderLayout.CENTER);

        JLabel foot = new JLabel("TALA · Accounting System", SwingConstants.CENTER);
        foot.setFont(Theme.medium(12));
        foot.setForeground(SOFT);
        foot.setBorder(BorderFactory.createEmptyBorder(0, 24, 18, 24));
        sky.add(foot, BorderLayout.SOUTH);
        setContentPane(sky);

        // "Remember me" from last time
        String remembered = Preferences.userNodeForPackage(LoginFrame.class).get(PREF_USERNAME, "");
        if (!remembered.isEmpty()) {
            usernameField.setText(remembered);
            rememberBox.setSelected(true);
        }
        SwingUtilities.invokeLater(() -> (remembered.isEmpty() ? usernameField : passwordField).input.requestFocusInWindow());
    }

    // =================================================================
    //  Actions
    // =================================================================

    private void doLogin() {
        String username = usernameField.getText();
        String password = new String(((JPasswordField) passwordField.input).getPassword());
        try {
            AuthService.LoginResult result = auth.login(username, password);
            if (!result.ok()) {
                showError(result.error);
                ((JPasswordField) passwordField.input).setText("");
                passwordField.input.requestFocusInWindow();
                return;
            }
            Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
            if (rememberBox.isSelected()) {
                prefs.put(PREF_USERNAME, result.user.getUsername());
            } else {
                prefs.remove(PREF_USERNAME);
            }

            MainFrame frame = new MainFrame(result.user, new AccountDAO(), new JournalDAO(), settingsDAO, userDAO);
            frame.setVisible(true);
            dispose();
        } catch (SQLException e) {
            showError("Could not reach MySQL: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void forgotPassword() {
        JOptionPane.showMessageDialog(this,
                "Passwords are reset by an admin.\n\n"
                + "Ask your instructor or admin to open  Admin > Users,\n"
                + "select your name and press  Reset password.",
                "Forgot password", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openSignUp() {
        SignUpDialog dialog = new SignUpDialog(this, auth);
        dialog.setVisible(true);
        if (dialog.getCreatedUsername() != null) {
            usernameField.setText(dialog.getCreatedUsername());
            ((JPasswordField) passwordField.input).setText("");
            errorLabel.setText(" ");
            passwordField.input.requestFocusInWindow();
        }
    }

    // =================================================================
    //  Top bar:   [logo] TALA / Accounting System      o Connected to MySQL
    // =================================================================

    private JComponent createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(22, 40, 6, 40));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(new LogoSquare());
        brand.add(Box.createHorizontalStrut(12));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("TALA");
        name.setFont(Theme.spaced(Theme.bold(20), 0.06f));
        name.setForeground(INK);
        JLabel tag = new JLabel("Accounting System");
        tag.setFont(Theme.regular(12));
        tag.setForeground(MUTED);
        tag.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        words.add(name);
        words.add(tag);
        brand.add(words);
        bar.add(brand, BorderLayout.WEST);

        final boolean connected = DBConnection.isConnected();
        JLabel status = new JLabel(connected ? "Connected to MySQL" : "MySQL offline") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(new Color(255, 255, 255, 217));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.setColor(new Color(0xebe7f3));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.setColor(connected ? new Color(0x4ade80) : new Color(0xf87171));
                g2.fillOval(14, getHeight() / 2 - 4, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        status.setFont(Theme.regular(13));
        status.setForeground(SOFT);
        status.setBorder(BorderFactory.createEmptyBorder(7, 30, 7, 16));
        JPanel statusHolder = new JPanel(new GridBagLayout());
        statusHolder.setOpaque(false);
        statusHolder.add(status);
        bar.add(statusHolder, BorderLayout.EAST);
        return bar;
    }

    // =================================================================
    //  The card
    // =================================================================

    private class LoginCard extends JPanel {
        static final int PAD = 26;           // space around the card for the shadow
        static final int W = 900, H = 500;   // the card itself

        private final JPanel form = createForm();
        private final JPanel welcome = createWelcome();

        LoginCard() {
            super(null);
            setOpaque(false);
            add(form);
            add(welcome);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(W + PAD * 2, H + PAD * 2);
        }

        @Override
        public void doLayout() {
            int formW = (int) (W * 0.42);
            form.setBounds(PAD, PAD, formW, H);
            Dimension wp = welcome.getPreferredSize();
            int ww = 330;
            welcome.setBounds(PAD + W - 22 - ww, PAD + (H - wp.height) / 2, ww, wp.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int x = PAD, y = PAD;

            // 1) soft purple shadow "0 24px 60px rgba(46,16,101,.18)"
            for (int i = PAD; i > 0; i -= 2) {
                int alpha = (int) (1 + 9 * (1 - i / (double) PAD));
                g2.setColor(new Color(46, 16, 101, alpha));
                g2.fill(new RoundRectangle2D.Double(x - i * 0.6, y - i * 0.4 + 14, W + i * 1.2, H + i * 0.8, 36 + i, 36 + i));
            }

            Shape card = new RoundRectangle2D.Double(x, y, W, H, 36, 36);
            g2.setColor(new Color(255, 255, 255, 240));   // rgba(255,255,255,.94)
            g2.fill(card);

            // 2) the slanted panel on the right (clipped to the rounded card)
            Graphics2D side = (Graphics2D) g2.create();
            side.clip(card);
            // yellow edge: linear-gradient(180deg, #fddd44, #f7af1a)
            side.setPaint(new GradientPaint(0, y, GOLD_1, 0, y + H, GOLD_2));
            side.fillPolygon(slant(x, y, 0.468, 0.748));
            Polygon panel = slant(x, y, 0.47, 0.75);
            side.clip(panel);
            // linear-gradient(140deg, #2e1065 0%, #4c1d95 50%, #6d28d9 100%)
            side.setPaint(new LinearGradientPaint(
                    new Point2D.Double(x + W * 0.47, y), new Point2D.Double(x + W, y + H),
                    new float[] {0f, 0.5f, 1f},
                    new Color[] {new Color(0x2e1065), new Color(0x4c1d95), new Color(0x6d28d9)}));
            side.fillRect(x, y, W, H);
            // ruled lines every 36px
            side.setColor(new Color(255, 255, 255, 13));
            for (int ly = y + 35; ly < y + H; ly += 36) {
                side.fillRect(x, ly, W, 1);
            }
            // radial glows: violet at 70% 100%, yellow at 95% 8%
            glow(side, x + W * 0.70f, y + H, 420, 320, new Color(139, 92, 246, 140));
            glow(side, x + W * 0.95f, y + H * 0.08f, 380, 300, new Color(253, 221, 68, 89));
            side.dispose();

            // 3) lilac border 1.5px #d9cff3
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(0xd9cff3));
            g2.draw(card);
            g2.dispose();
        }

        private void glow(Graphics2D g2, float cx, float cy, float rx, float ry, Color color) {
            float squash = ry / rx;
            AffineTransform t = AffineTransform.getScaleInstance(1, squash);
            Point2D center = new Point2D.Float(cx, cy / squash);
            Color clear = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
            g2.setPaint(new RadialGradientPaint(center, rx, center, new float[] {0f, 0.68f, 1f},
                    new Color[] {color, clear, clear}, RadialGradientPaint.CycleMethod.NO_CYCLE,
                    RadialGradientPaint.ColorSpaceType.SRGB, t));
            g2.fillRect(PAD, PAD, W, H);
        }

        /** Top-left corner at 'top' of the width, bottom-left corner at 'bottom' of the width */
        private Polygon slant(int x, int y, double top, double bottom) {
            Polygon p = new Polygon();
            p.addPoint(x + (int) (W * top), y);
            p.addPoint(x + W, y);
            p.addPoint(x + W, y + H);
            p.addPoint(x + (int) (W * bottom), y + H);
            return p;
        }
    }

    /** Left part of the card: the log-in form */
    private JPanel createForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(42, 40, 30, 40));

        JLabel title = new JLabel("Log in", SwingConstants.CENTER) {
            // a small violet-to-yellow bar under the title
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = smooth(g);
                int bx = (getWidth() - 44) / 2, by = getHeight() - 5;
                g2.setPaint(new GradientPaint(bx, 0, VIOLET, bx + 44, 0, GOLD_1));
                g2.fillRoundRect(bx, by, 44, 4, 4, 4);
                g2.dispose();
            }
        };
        title.setFont(Theme.bold(30));
        title.setForeground(INK);
        title.setVerticalAlignment(SwingConstants.TOP);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        JLabel lead = new JLabel("Sign in to your account", SwingConstants.CENTER);
        lead.setFont(Theme.regular(13.5f));
        lead.setForeground(MUTED);

        // Remember me   ...   Forgot password?
        rememberBox.setOpaque(false);
        rememberBox.setFont(Theme.regular(13));
        rememberBox.setForeground(SOFT);
        rememberBox.setFocusPainted(false);
        rememberBox.setIcon(new CheckIcon(false));
        rememberBox.setSelectedIcon(new CheckIcon(true));
        rememberBox.setIconTextGap(8);
        rememberBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 6));
        rememberBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Hover.of(rememberBox);
        LinkButton forgot = new LinkButton("Forgot password?", false);
        forgot.addActionListener(e -> forgotPassword());
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(rememberBox, BorderLayout.WEST);
        row.add(forgot, BorderLayout.EAST);

        errorLabel.setFont(Theme.semibold(13));
        errorLabel.setForeground(ERROR);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        GoldButton go = new GoldButton("Log in");
        go.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(go);   // Enter key = Log in

        JLabel noAccount = new JLabel("Don't have an account?");
        noAccount.setFont(Theme.regular(13.5f));
        noAccount.setForeground(SOFT);
        LinkButton signUp = new LinkButton("Sign up", true);
        signUp.addActionListener(e -> openSignUp());
        JPanel alt = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        alt.setOpaque(false);
        alt.add(noAccount);
        alt.add(signUp);

        // typing again hides the old error
        java.awt.event.KeyAdapter clear = new java.awt.event.KeyAdapter() {
            @Override public void keyTyped(java.awt.event.KeyEvent e) { errorLabel.setText(" "); }
        };
        usernameField.input.addKeyListener(clear);
        passwordField.input.addKeyListener(clear);

        JComponent[] parts = {title, lead, gap(20), labeled("Username", usernameField, Icons.USER), gap(16),
            labeled("Password", passwordField, Icons.LOCK), gap(4), row, errorLabel, gap(6), go, gap(14), alt};
        for (JComponent part : parts) {
            part.setAlignmentX(Component.CENTER_ALIGNMENT);
            if (!(part instanceof Box.Filler)) {
                part.setMaximumSize(new Dimension(Integer.MAX_VALUE, part.getPreferredSize().height));
            }
            form.add(part);
        }
        return form;
    }

    /** Right part of the card: WELCOME BACK!, a journal entry preview, stats and chips */
    private JPanel createWelcome() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("TALA · ACCOUNTING SYSTEM");
        eyebrow.setFont(Theme.spaced(Theme.semibold(10.5f), 0.18f));
        eyebrow.setForeground(GOLD_1);

        JLabel big = new JLabel("WELCOME BACK!");
        big.setFont(Theme.bold(32));
        big.setForeground(Color.WHITE);

        // three short lines, each lined up on the right edge
        // three short lines, lined up on the right edge
        JLabel text = new JLabel("<html><div style='text-align:right'>Log in to record journal entries,<br>"
                + "post to the ledger and keep your<br>12-column worksheet balanced.</div></html>");
        text.setFont(Theme.regular(13.5f));
        text.setForeground(new Color(0xddd0fb));
        text.setHorizontalAlignment(SwingConstants.RIGHT);
        text.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        stats.setOpaque(false);
        stats.add(new StatBox("7", "MODULES"));
        stats.add(new StatBox("12", "COLUMNS"));
        stats.add(new StatBox("2", "ROLES"));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        chips.setOpaque(false);
        for (String word : new String[] {"Journal", "Ledger", "Worksheet"}) {
            chips.add(new Chip(word));
        }

        JComponent[] parts = {eyebrow, big, text, new MockEntry(), stats, chips};
        for (JComponent c : parts) {
            c.setAlignmentX(Component.RIGHT_ALIGNMENT);
        }
        panel.add(eyebrow);
        panel.add(Box.createVerticalStrut(8));
        panel.add(big);
        panel.add(Box.createVerticalStrut(8));
        panel.add(text);
        panel.add(Box.createVerticalStrut(14));
        panel.add(parts[3]);
        panel.add(Box.createVerticalStrut(12));
        panel.add(stats);
        panel.add(Box.createVerticalStrut(12));
        panel.add(chips);
        return panel;
    }

    private static JComponent gap(int height) {
        return (JComponent) Box.createVerticalStrut(height);
    }

    /** "Username" over an underlined input */
    private static JComponent labeled(String text, HintField field, String iconName) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.semibold(13));
        label.setForeground(SOFT);
        label.setLabelFor(field.input);
        field.setIconName(iconName);

        JPanel box = new JPanel(new BorderLayout(0, 0));
        box.setOpaque(false);
        box.add(label, BorderLayout.NORTH);
        box.add(field, BorderLayout.CENTER);
        return box;
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g2;
    }

    // =================================================================
    //  Small parts
    // =================================================================

    /** Lavender sky with the campus picture along the bottom */
    private static class Sky extends JPanel {
        private final BufferedImage campus = AppImages.getPicture("campus.png");

        Sky() {
            super(new BorderLayout());
            setBackground(new Color(0xf1ebff));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            // linear-gradient(180deg, #e6dcff 0%, #f1ebff 50%, #faf8ff 100%)
            g2.setPaint(new LinearGradientPaint(0, 0, 0, Math.max(1, h), new float[] {0f, 0.5f, 1f},
                    new Color[] {new Color(0xe6dcff), new Color(0xf1ebff), new Color(0xfaf8ff)}));
            g2.fillRect(0, 0, w, h);
            // the campus: full width, sitting on the bottom edge
            if (campus != null) {
                int ph = (int) Math.round(campus.getHeight() * (w / (double) campus.getWidth()));
                g2.drawImage(campus, 0, h - ph, w, ph, null);
            }
            // a light veil so the card stands out: rgba(246,244,251,.30) -> .15
            g2.setPaint(new GradientPaint(0, 0, new Color(246, 244, 251, 77), 0, h, new Color(246, 244, 251, 38)));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    /** A text or password box drawn as a line, with a grey hint while empty */
    private static class HintField extends JPanel {
        final JTextComponent input;
        private String iconName;
        private float focus;            // 0..1, animated: the violet underline grows from the middle
        private final javax.swing.Timer anim;

        HintField(boolean password, String hint) {
            super(new BorderLayout(10, 0));
            setOpaque(false);
            input = password ? new JPasswordField() {
                @Override protected void paintComponent(Graphics g) { super.paintComponent(g); paintHint(this, g, hint); }
            } : new JTextField() {
                @Override protected void paintComponent(Graphics g) { super.paintComponent(g); paintHint(this, g, hint); }
            };
            input.setOpaque(false);
            input.setBorder(BorderFactory.createEmptyBorder());
            input.setForeground(INK);
            input.setCaretColor(VIOLET);
            input.setSelectionColor(new Color(0xddd0fb));
            input.setSelectedTextColor(INK);
            input.setFont(Theme.regular(15));
            input.getAccessibleContext().setAccessibleDescription(hint);
            anim = new javax.swing.Timer(15, e -> {
                float target = input.hasFocus() ? 1f : 0f;
                focus += (target - focus) * 0.25f;
                if (Math.abs(target - focus) < 0.02f) {
                    focus = target;
                    ((javax.swing.Timer) e.getSource()).stop();
                }
                repaint();
            });
            input.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { anim.start(); }
                @Override public void focusLost(FocusEvent e)   { anim.start(); }
            });
            add(input, BorderLayout.CENTER);
            JComponent icon = new JComponent() {
                @Override public Dimension getPreferredSize() { return new Dimension(18, 18); }

                @Override
                protected void paintComponent(Graphics g) {
                    if (iconName == null) {
                        return;
                    }
                    Color c = Theme.mix(MUTED, VIOLET, focus);
                    Icons.get(iconName, 18, c).paintIcon(this, g, 0, (getHeight() - 18) / 2);
                }
            };
            add(icon, BorderLayout.EAST);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
            Hover.of(this);
        }

        void setIconName(String name) { this.iconName = name; repaint(); }

        String getText() { return input.getText(); }
        void setText(String text) { input.setText(text); }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), y = getHeight() - 2;
            float hover = Hover.of(this).value();
            g2.setColor(Theme.mix(LINE, new Color(0xb9a3f3), hover));
            g2.fillRect(0, y, w, 2);
            // violet line growing out from the middle while typing
            int grow = Math.round(w * focus);
            g2.setColor(VIOLET);
            g2.fillRect((w - grow) / 2, y - 1, grow, 3);
            g2.dispose();
        }

        private static void paintHint(JTextComponent field, Graphics g, String hint) {
            if (!field.getText().isEmpty()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setFont(field.getFont());
            g2.setColor(HINT);
            int y = (field.getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(hint, field.getInsets().left, y);
            g2.dispose();
        }
    }

    /** The yellow pill "Log in" button: rises and glows on hover */
    private static class GoldButton extends JButton {
        GoldButton(String text) {
            super(text);
            setFont(Theme.bold(15.5f));
            setForeground(Theme.PURPLE_DEEP);   // dark purple text on the gold button
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Hover.of(this);
            getModel().addChangeListener(e -> repaint());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 56);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float t = Hover.of(this).value();
            boolean down = getModel().isPressed() && getModel().isArmed();
            int w = getWidth() - 1, h = 48;
            float lift = down ? 2f : 3f - 2f * t;     // body top: 3px normally, 1px when hovered
            // warm glow "0 8px 18px rgba(247,175,26,.35)"
            for (int i = 4; i >= 1; i--) {
                g2.setColor(new Color(247, 175, 26, Math.round((10 + 14 * t) / i)));
                g2.fill(new RoundRectangle2D.Float(-i + 2, lift + 2 + i * (1 + t), w + i * 2 - 4, h, h, h));
            }
            g2.setPaint(new GradientPaint(0, 0, Theme.mix(GOLD_1, new Color(0xfee26a), t), w, h,
                    Theme.mix(GOLD_2, new Color(0xf9bb2e), t)));
            g2.fill(new RoundRectangle2D.Float(0, lift, w, h, h, h));
            if (isFocusOwner()) {
                g2.setColor(new Color(0xa78bfa));
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new RoundRectangle2D.Float(1, lift + 1, w - 2, h - 2, h, h));
            }
            // label (with a small arrow that slides in on hover)
            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(getText());
            float ty = lift + (h + fm.getAscent() - fm.getDescent()) / 2f;
            float tx = (w - tw) / 2f - 6 * t;
            g2.drawString(getText(), tx, ty);
            if (t > 0.05f) {
                g2.setColor(Theme.alpha(getForeground(), Math.round(255 * t)));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float ax = tx + tw + 8 + 2 * t, ay = lift + h / 2f;
                g2.draw(new java.awt.geom.Line2D.Float(ax, ay, ax + 9, ay));
                g2.draw(new java.awt.geom.Line2D.Float(ax + 5, ay - 4, ax + 9, ay));
                g2.draw(new java.awt.geom.Line2D.Float(ax + 5, ay + 4, ax + 9, ay));
            }
            g2.dispose();
        }
    }

    /** A text button that looks like a link: darker and underlined on hover */
    private static class LinkButton extends JButton {
        LinkButton(String text, boolean bold) {
            super(text);
            setFont(bold ? Theme.bold(13.5f) : Theme.medium(13));
            setForeground(LINK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setMargin(new java.awt.Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Hover.of(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            float t = Hover.of(this).value();
            setForeground(Theme.mix(LINK, LINK_HOVER, t));
            super.paintComponent(g);
            if (t > 0 || isFocusOwner()) {   // the underline draws in from the left
                Graphics2D g2 = smooth(g);
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics(getFont());
                int tw = fm.stringWidth(getText());
                int x = (getWidth() - tw) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 + 1;
                g2.fillRect(x, y, Math.round(tw * (isFocusOwner() ? 1f : t)), 1);
                g2.dispose();
            }
        }
    }

    /** Rounded check box: lilac outline, violet with a white tick when checked */
    private static class CheckIcon implements Icon {
        private final boolean checked;

        CheckIcon(boolean checked) {
            this.checked = checked;
        }

        @Override public int getIconWidth()  { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = smooth(g);
            float t = c instanceof JComponent ? Hover.of((JComponent) c).value() : 0f;
            if (checked) {
                g2.setColor(Theme.mix(new Color(0x7c3aed), VIOLET, t));
                g2.fillRoundRect(x + 1, y + 1, 16, 16, 7, 7);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[] {x + 5, x + 8, x + 13}, new int[] {y + 9, y + 12, y + 6}, 3);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x + 1, y + 1, 16, 16, 7, 7);
                g2.setColor(Theme.mix(LINE, new Color(0x8b6ee0), t));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x + 1, y + 1, 16, 16, 7, 7);
            }
            g2.dispose();
        }
    }

    /** The small white "Journal entry JE-7322" card, tilted -2deg; it straightens on hover */
    private static class MockEntry extends JComponent {
        MockEntry() {
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(330, 128); }
        @Override public Dimension getMaximumSize()   { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float t = Hover.of(this).value();
            int x = 26, y = 8, w = getWidth() - x - 4, h = 104;
            g2.rotate(Math.toRadians(-2 * (1 - t)), x + w / 2.0, y + h / 2.0);
            g2.translate(0, -2 * t);
            // shadow "0 18px 40px rgba(20,6,50,.35)"
            for (int i = 6; i >= 1; i--) {
                g2.setColor(new Color(20, 6, 50, 9 + Math.round(4 * t)));
                g2.fill(new RoundRectangle2D.Float(x - i, y + 4 + i, w + i * 2, h, 28 + i, 28 + i));
            }
            Shape card = new RoundRectangle2D.Float(x, y, w, h, 28, 28);
            g2.setColor(Color.WHITE);
            g2.fill(card);
            Graphics2D top = (Graphics2D) g2.create();
            top.clip(card);
            top.setColor(GOLD_1);
            top.fillRect(x, y, w, 4);
            top.dispose();

            int px = x + 14, pr = x + w - 14;
            // header: JOURNAL ENTRY ......... [JE-7322]
            g2.setFont(Theme.spaced(Theme.semibold(10.5f), 0.1f));
            g2.setColor(new Color(0x7c6f99));
            g2.drawString("JOURNAL ENTRY", px, y + 24);
            g2.setFont(Theme.mono(10.5f));
            FontMetrics fm = g2.getFontMetrics();
            String ref = "JE-7322";
            int rw = fm.stringWidth(ref) + 14;
            g2.setColor(new Color(0xf1ebff));
            g2.fillRoundRect(pr - rw, y + 12, rw, 17, 10, 10);
            g2.setColor(VIOLET);
            g2.drawString(ref, pr - rw + 7, y + 25);

            // lines
            g2.setFont(Theme.regular(12));
            g2.setColor(new Color(0x2a1f45));
            g2.drawString("Cash", px, y + 48);
            g2.drawString("Accounts Receivable", px + 16, y + 70);
            g2.setFont(Theme.semibold(12));
            fm = g2.getFontMetrics();
            String amt = tala.util.Money.PESO_SIGN + "15,000.00";
            g2.setColor(new Color(0x5b21b6));
            g2.drawString(amt, pr - 92 - fm.stringWidth(amt), y + 48);
            g2.setColor(new Color(0xb7791f));
            g2.drawString(amt, pr - fm.stringWidth(amt), y + 70);
            // dashed row lines
            g2.setColor(new Color(0xe6e0f3));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
            g2.drawLine(px, y + 55, pr, y + 55);
            g2.drawLine(px, y + 77, pr, y + 77);
            // footer
            g2.setFont(Theme.semibold(11));
            g2.setColor(new Color(0x17663d));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[] {px, px + 3, px + 9}, new int[] {y + 91, y + 94, y + 87}, 3);
            g2.drawString("Debits = Credits", px + 14, y + 95);
            g2.setFont(Theme.regular(11));
            g2.setColor(new Color(0x8a7fa6));
            fm = g2.getFontMetrics();
            g2.drawString("Posted Nov. 3", pr - fm.stringWidth("Posted Nov. 3"), y + 95);
            g2.dispose();
        }
    }

    /** A small see-through box with a big yellow number: 7 MODULES */
    private static class StatBox extends JComponent {
        private final String number, word;

        StatBox(String number, String word) {
            this.number = number;
            this.word = word;
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(74, 56); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float t = Hover.of(this).value();
            int w = getWidth() - 1, h = getHeight() - 1;
            // the see-through glass box of the design, a little stronger: a soft white wash
            // (brighter at the top), a crisp white edge and a thin highlight line on top
            float up = 1.5f * t;
            RoundRectangle2D box = new RoundRectangle2D.Float(0.5f, 0.5f - up, w - 1, h - 3, 20, 20);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, Math.round(38 + 18 * t)),
                    0, h, new Color(255, 255, 255, Math.round(16 + 14 * t))));
            g2.fill(box);
            g2.setColor(new Color(255, 255, 255, Math.round(70 + 50 * t)));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(box);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawLine(10, Math.round(2 - up), w - 10, Math.round(2 - up));
            g2.setFont(Theme.bold(21));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.mix(GOLD_1, new Color(0xfef08a), t));
            g2.drawString(number, w - 10 - fm.stringWidth(number), 27 - up);
            g2.setFont(Theme.spaced(Theme.medium(9.5f), 0.08f));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(0xe4dafc));
            g2.drawString(word, w - 10 - fm.stringWidth(word), 44 - up);
            g2.dispose();
        }
    }

    /** A small pill: (Journal) (Ledger) (Worksheet) */
    private static class Chip extends JLabel {
        Chip(String text) {
            super(text);
            setFont(Theme.medium(12));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(4, 11, 4, 11));
            Hover.of(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float t = Hover.of(this).value();
            int w = getWidth() - 1, h = getHeight() - 1;
            // see-through glass pill with a clear white edge; it brightens under the mouse
            g2.setColor(new Color(255, 255, 255, Math.round(32 + 30 * t)));
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(new Color(255, 255, 255, Math.round(80 + 60 * t)));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, w, h, h, h);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** The TALA logo in a rounded square */
    private static class LogoSquare extends JComponent {
        private final BufferedImage picture = AppImages.getLogo(84);

        LogoSquare() {
            Dimension size = new Dimension(42, 42);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            if (picture != null) {
                g2.setPaint(new TexturePaint(picture, new Rectangle(0, 0, 42, 42)));
            } else {
                g2.setColor(Theme.YELLOW);
            }
            g2.fillRoundRect(0, 0, 42, 42, 24, 24);
            if (picture == null) {
                g2.setColor(Theme.PURPLE_DEEP);
                g2.setFont(Theme.bold(24).deriveFont(Font.BOLD));
                g2.drawString("T", 14, 30);
            }
            g2.dispose();
        }
    }
}
