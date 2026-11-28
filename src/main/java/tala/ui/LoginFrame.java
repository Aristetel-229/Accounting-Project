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
    private GoldButton loginButton;

    // the Sign up page
    private final HintField suName = new HintField(false, "e.g. Juan Dela Cruz");
    private final HintField suUser = new HintField(false, "At least 3 letters or numbers");
    private final HintField suEmail = new HintField(false, "name@example.com");
    private final HintField suPass = new HintField(true, AuthService.MIN_PASSWORD + "+ characters");
    private final HintField suConfirm = new HintField(true, "Type it again");
    private final JCheckBox agreeBox = new JCheckBox("I agree to the terms of use");
    private final JLabel suError = new JLabel(" ");
    private GoldButton createButton;
    private final boolean needsApproval = readApprovalSetting();

    // the two pages share the window: "login" and "signup"
    private final java.awt.CardLayout views = new java.awt.CardLayout();
    private final JPanel viewHolder = new JPanel(views);

    public LoginFrame() {
        super("TALA - Log in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 760));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setIconImages(AppImages.getWindowIcons());

        Sky sky = new Sky();
        sky.add(createTopBar(), BorderLayout.NORTH);

        JPanel loginPage = new JPanel(new GridBagLayout());   // centers the card
        loginPage.setOpaque(false);
        loginPage.add(new SplitCard(960, 560, 0.42, 0.48, createForm(), createWelcome(), 420));
        JPanel signUpPage = new JPanel(new GridBagLayout());
        signUpPage.setOpaque(false);
        signUpPage.add(new SplitCard(960, 600, 0.42, 0.48, createSignUpForm(), createJoin(), 420));
        viewHolder.setOpaque(false);
        viewHolder.add(loginPage, "login");
        viewHolder.add(signUpPage, "signup");
        sky.add(viewHolder, BorderLayout.CENTER);
        getRootPane().setDefaultButton(loginButton);   // Enter key = Log in

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

    /** Opens the Sign up page (same window: the log-in card is swapped for the sign-up card) */
    private void showSignUp() {
        errorLabel.setText(" ");
        suError.setText(" ");
        views.show(viewHolder, "signup");
        setTitle("TALA - Sign up");
        getRootPane().setDefaultButton(createButton);   // Enter key = Create account
        suName.input.requestFocusInWindow();
    }

    /** Back to the Log in page; username = the new account to fill in (or null) */
    private void showLogIn(String username) {
        views.show(viewHolder, "login");
        setTitle("TALA - Log in");
        getRootPane().setDefaultButton(loginButton);
        if (username != null) {
            usernameField.setText(username);
            ((JPasswordField) passwordField.input).setText("");
            errorLabel.setText(" ");
            passwordField.input.requestFocusInWindow();
        } else {
            usernameField.input.requestFocusInWindow();
        }
    }

    private void doSignUp() {
        if (!agreeBox.isSelected()) {
            suError.setText("Please agree to the terms of use first.");
            return;
        }
        String username = suUser.getText().trim().toLowerCase();
        try {
            String done = auth.signUp(suName.getText(), username, suEmail.getText(),
                    new String(((JPasswordField) suPass.input).getPassword()),
                    new String(((JPasswordField) suConfirm.input).getPassword()));
            JOptionPane.showMessageDialog(this, done, "Account created", JOptionPane.INFORMATION_MESSAGE);
            for (HintField f : new HintField[] {suName, suUser, suEmail, suPass, suConfirm}) {
                f.setText("");
            }
            agreeBox.setSelected(false);
            suError.setText(" ");
            showLogIn(username);
        } catch (IllegalArgumentException e) {
            suError.setText(e.getMessage());
        } catch (SQLException e) {
            suError.setText("Could not save the account: " + e.getMessage());
        }
    }

    private void showTerms() {
        JOptionPane.showMessageDialog(this,
                "TALA is a classroom accounting system.\n\n"
                + "- Use your real name so your instructor or admin knows who you are.\n"
                + "- Keep your password to yourself.\n"
                + "- Enter practice data only. An admin may review, approve or remove accounts.",
                "Terms of use", JOptionPane.INFORMATION_MESSAGE);
    }

    /** The Admin switch "Sign-ups need approval"; if MySQL cannot be read, assume yes */
    private boolean readApprovalSetting() {
        try {
            return settingsDAO.getFlag(SettingsDAO.SIGNUPS_NEED_APPROVAL);
        } catch (SQLException e) {
            return true;
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

    /**
     * The white card with the slanted purple panel on the right, used by both
     * pages: the form on the left, the purple side's content on the right.
     * top / bottom = where the slant starts and ends (part of the width).
     */
    private static class SplitCard extends JPanel {
        static final int PAD = 26;           // space around the card for the shadow
        private final int W, H, sideW;
        private final double top, bottom;
        private final JPanel form, welcome;

        SplitCard(int w, int h, double top, double bottom, JPanel form, JPanel side, int sideW) {
            super(null);
            this.W = w;
            this.H = h;
            this.top = top;
            this.bottom = bottom;
            this.form = form;
            this.welcome = side;
            this.sideW = sideW;
            setOpaque(false);
            add(form);
            add(side);
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
            int ww = sideW;
            welcome.setBounds(PAD + W - 44 - ww, PAD + (H - wp.height) / 2, ww, wp.height);
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
            side.fillPolygon(slant(x, y, top - 0.002, bottom - 0.002));
            Polygon panel = slant(x, y, top, bottom);
            side.clip(panel);
            // linear-gradient(140deg, #2e1065 0%, #4c1d95 50%, #6d28d9 100%)
            side.setPaint(new LinearGradientPaint(
                    new Point2D.Double(x + W * top, y), new Point2D.Double(x + W, y + H),
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
        loginButton = go;

        JLabel noAccount = new JLabel("Don't have an account?");
        noAccount.setFont(Theme.regular(13.5f));
        noAccount.setForeground(SOFT);
        LinkButton signUp = new LinkButton("Sign up", true);
        signUp.addActionListener(e -> showSignUp());
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

    /**
     * Right part of the Log in card (left-aligned): the yellow eyebrow,
     * WELCOME BACK!, one line about TALA, a journal entry card and the stats.
     */
    private JPanel createWelcome() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JComponent eyebrow = new SpacedText("TALA · ACCOUNTING SYSTEM", Theme.bold(13), GOLD_1, 3);
        JLabel big = new JLabel("WELCOME BACK!");
        big.setFont(Theme.bold(42));
        big.setForeground(Color.WHITE);
        JComponent text = paragraph("Log in to record journal entries, post to the ledger and keep your "
                + "12-column worksheet balanced.");

        JComponent[] parts = {eyebrow, big, text, new EntryCard(), new StatsRow()};
        int[] gaps = {6, 6, 22, 22, 0};
        for (int i = 0; i < parts.length; i++) {
            parts[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(parts[i]);
            if (gaps[i] > 0) {
                panel.add(Box.createVerticalStrut(gaps[i]));
            }
        }
        return panel;
    }

    /** A left-aligned paragraph in soft lilac that wraps at the panel's width */
    private static JComponent paragraph(String text) {
        return new Paragraph(text, Theme.regular(16), new Color(0xe4dafc), 420);
    }

    /** Text that wraps word by word inside its width (measured with the real font, so nothing is cut) */
    private static class Paragraph extends JComponent {
        private final String text;
        private final int width;

        Paragraph(String text, java.awt.Font font, Color color, int width) {
            this.text = text;
            this.width = width;
            setFont(font);
            setForeground(color);
        }

        private java.util.List<String> lines(FontMetrics fm, int max) {
            java.util.List<String> lines = new java.util.ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                String next = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && fm.stringWidth(next) > max) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(next);
                }
            }
            lines.add(line.toString());
            return lines;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int lineH = fm.getHeight() + 3;
            // measured a little narrower than the box, so a wider screen font never runs past the edge
            return new Dimension(width, lines(fm, width - 24).size() * lineH + 2);
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int y = fm.getAscent(), lineH = fm.getHeight() + 3;
            for (String line : lines(fm, getWidth() - 24)) {
                g2.drawString(line, 0, y);
                y += lineH;
            }
            g2.dispose();
        }
    }

    /** Left part of the Sign up card: the form */
    private JPanel createSignUpForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));

        JLabel title = new JLabel("Sign up", SwingConstants.CENTER) {
            // the same violet-to-yellow bar under the title as on the Log in page
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
        title.setFont(Theme.bold(28));
        title.setForeground(INK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        JLabel lead = new JLabel("Create your TALA account", SwingConstants.CENTER);
        lead.setFont(Theme.regular(13));
        lead.setForeground(MUTED);

        for (HintField f : new HintField[] {suName, suUser, suEmail, suPass, suConfirm}) {
            f.compact();
            f.input.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override public void keyTyped(java.awt.event.KeyEvent e) { suError.setText(" "); }
            });
        }

        // [ ] I agree to the terms of use   (the words "terms of use" open a short note)
        agreeBox.setText("I agree to the");
        agreeBox.setOpaque(false);
        agreeBox.setFont(Theme.regular(13));
        agreeBox.setForeground(SOFT);
        agreeBox.setFocusPainted(false);
        agreeBox.setIcon(new CheckIcon(false));
        agreeBox.setSelectedIcon(new CheckIcon(true));
        agreeBox.setIconTextGap(8);
        agreeBox.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        agreeBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        agreeBox.addActionListener(e -> suError.setText(" "));
        Hover.of(agreeBox);
        LinkButton terms = new LinkButton("terms of use", false);
        terms.setBorder(BorderFactory.createEmptyBorder(6, 3, 6, 3));
        terms.addActionListener(e -> showTerms());
        JPanel agreeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        agreeRow.setOpaque(false);
        agreeRow.add(agreeBox);
        agreeRow.add(terms);

        suError.setFont(Theme.semibold(12.5f));
        suError.setForeground(ERROR);
        suError.setHorizontalAlignment(SwingConstants.CENTER);

        createButton = new GoldButton("Create account");
        createButton.addActionListener(e -> doSignUp());

        // "New accounts start as User and need admin approval before logging in."
        JLabel note = new JLabel("<html><div style='text-align:center'>New accounts start as "
                + "<b style='color:#6d28d9'>User</b>"
                + (needsApproval ? " and need admin approval<br>before logging in." : " and can log in<br>right away.")
                + "</div></html>", SwingConstants.CENTER);
        note.setFont(Theme.regular(12));
        note.setForeground(MUTED);

        JLabel have = new JLabel("Already have an account?");
        have.setFont(Theme.regular(13.5f));
        have.setForeground(SOFT);
        LinkButton logIn = new LinkButton("Log In", true);
        logIn.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        logIn.addActionListener(e -> showLogIn(null));
        JPanel alt = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        alt.setOpaque(false);
        alt.add(have);
        alt.add(logIn);

        JComponent[] parts = {title, lead, gap(12),
            labeled("Full name", suName, Icons.USER), gap(8),
            labeled("Username", suUser, Icons.AT), gap(8),
            labeled("Email", suEmail, Icons.MAIL), gap(8),
            labeled("Password", suPass, Icons.LOCK), gap(8),
            labeled("Confirm password", suConfirm, Icons.LOCK), gap(6),
            agreeRow, suError, createButton, gap(4), note, alt};
        for (JComponent part : parts) {
            part.setAlignmentX(Component.CENTER_ALIGNMENT);
            if (!(part instanceof Box.Filler)) {
                part.setMaximumSize(new Dimension(Integer.MAX_VALUE, part.getPreferredSize().height));
            }
            form.add(part);
        }
        return form;
    }

    /** Right part of the Sign up card (left-aligned): JOIN TALA! and the three steps on a line */
    private JPanel createJoin() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JComponent eyebrow = new SpacedText("TALA · ACCOUNTING SYSTEM", Theme.bold(13), GOLD_1, 3);
        JLabel big = new JLabel("JOIN TALA!");
        big.setFont(Theme.bold(42));
        big.setForeground(Color.WHITE);
        JComponent text = paragraph("Create your account to start recording journal entries, posting to the "
                + "ledger and building your 12-column worksheet.");
        Steps steps = new Steps(new String[][] {
            {"Create your account", "Fill in your name, username and password"},
            needsApproval ? new String[] {"Wait for approval", "An admin reviews and approves new users"}
                          : new String[] {"No waiting", "New accounts can log in right away"},
            {"Start recording", "Log in and post your first entry"}});

        JComponent[] parts = {eyebrow, big, text, steps};
        int[] gaps = {6, 6, 30, 0};
        for (int i = 0; i < parts.length; i++) {
            parts[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(parts[i]);
            if (gaps[i] > 0) {
                panel.add(Box.createVerticalStrut(gaps[i]));
            }
        }
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

        private int height = 46;

        /** A slightly shorter line with smaller text (the Sign up form has five of them) */
        void compact() {
            height = 38;
            input.setFont(Theme.regular(14));
        }

        String getText() { return input.getText(); }
        void setText(String text) { input.setText(text); }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, height);
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

    /** Draws text with even extra space between the letters (small UPPERCASE labels) */
    private static int drawSpaced(Graphics2D g2, String text, int x, int y, int spacing) {
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            g2.drawString(ch, x, y);
            x += fm.stringWidth(ch) + (i < text.length() - 1 ? spacing : 0);
        }
        return x;
    }

    private static int spacedWidth(FontMetrics fm, String text, int spacing) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += fm.stringWidth(text.substring(i, i + 1)) + (i < text.length() - 1 ? spacing : 0);
        }
        return w;
    }

    /** A one-line label with letter spacing ("T A L A  ·  A C C O U N T I N G") */
    private static class SpacedText extends JComponent {
        private final String text;
        private final int spacing;

        SpacedText(String text, java.awt.Font font, Color color, int spacing) {
            this.text = text;
            this.spacing = spacing;
            setFont(font);
            setForeground(color);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(spacedWidth(fm, text, spacing) + 2, fm.getHeight());
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setFont(getFont());
            g2.setColor(getForeground());
            drawSpaced(g2, text, 0, g2.getFontMetrics().getAscent(), spacing);
            g2.dispose();
        }
    }

    /**
     * The white "JOURNAL ENTRY" card: a debit line, an indented credit line,
     * a Total row with double underlines and "Balanced, ready to post".
     * Two faded cards peek out under it, like a small stack. It rises a
     * little on hover.
     */
    private static class EntryCard extends JComponent {
        private static final int CARD_H = 236, STACK = 16;

        EntryCard() {
            Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(420, CARD_H + STACK + 4); }
        @Override public Dimension getMaximumSize()   { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float t = Hover.of(this).value();
            int w = getWidth() - 1;
            int up = Math.round(2 * t);
            // the stack under the card
            g2.setColor(new Color(255, 255, 255, 46));
            g2.fill(new RoundRectangle2D.Float(28, STACK, w - 56, CARD_H, 22, 22));
            g2.setColor(new Color(255, 255, 255, 80));
            g2.fill(new RoundRectangle2D.Float(14, STACK / 2f, w - 28, CARD_H, 22, 22));
            // soft shadow
            for (int i = 6; i >= 1; i--) {
                g2.setColor(new Color(20, 6, 50, 10));
                g2.fill(new RoundRectangle2D.Float(-i / 2f, 4 + i - up, w + i, CARD_H, 22 + i, 22 + i));
            }
            g2.translate(0, -up);
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, CARD_H, 22, 22));

            int left = 22, right = w - 22, drRight = right - 116;
            // header: JOURNAL ENTRY ............ [JE-7322 · Nov. 3]
            g2.setFont(Theme.bold(12.5f));
            g2.setColor(new Color(0x4c1d95));
            drawSpaced(g2, "JOURNAL ENTRY", left, 36, 2);
            String ref = "JE-7322 · Nov. 3";
            g2.setFont(Theme.bold(12));
            FontMetrics fm = g2.getFontMetrics();
            int pw = fm.stringWidth(ref) + 20;
            g2.setColor(new Color(0xefe8ff));
            g2.fillRoundRect(right - pw, 20, pw, 24, 24, 24);
            g2.setColor(new Color(0x6d28d9));
            g2.drawString(ref, right - pw + 10, 37);

            // column names
            g2.setFont(Theme.regular(12));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(0x8a83a3));
            drawSpaced(g2, "ACCOUNT", left, 68, 2);
            drawSpaced(g2, "DR", drRight - spacedWidth(fm, "DR", 2), 68, 2);
            drawSpaced(g2, "CR", right - spacedWidth(fm, "CR", 2), 68, 2);
            g2.setColor(new Color(0xe6e0f3));
            g2.fillRect(left, 78, right - left, 2);

            // Cash (debit) and Accounts Payable (credit, indented)
            g2.setFont(Theme.regular(15.5f));
            g2.setColor(new Color(0x231b3a));
            g2.drawString("Cash", left, 106);
            g2.drawString(ellipsis(g2, "Accounts Payable", drRight - left - 40 - 70), left + 22, 146);
            g2.setFont(Theme.bold(15.5f));
            fm = g2.getFontMetrics();
            String amt = "15,000.00";
            g2.setColor(new Color(0x5b21b6));
            g2.drawString(amt, drRight - fm.stringWidth(amt), 106);
            g2.setColor(new Color(0xa8740c));
            g2.drawString(amt, right - fm.stringWidth(amt), 146);
            g2.setColor(new Color(0xf0ecf7));
            g2.fillRect(left, 120, right - left, 1);

            // Total with a firm line above and double lines under the amounts
            g2.setColor(new Color(0x2e1065));
            g2.fillRect(left, 160, right - left, 2);
            g2.setFont(Theme.bold(15.5f));
            fm = g2.getFontMetrics();
            g2.drawString("Total", left, 188);
            int aw = fm.stringWidth(amt);
            g2.drawString(amt, drRight - aw, 188);
            g2.drawString(amt, right - aw, 188);
            for (int x2 : new int[] {drRight, right}) {
                int x1 = x2 - Math.max(aw, 104);
                g2.fillRect(x1, 194, x2 - x1, 1);
                g2.fillRect(x1, 197, x2 - x1, 1);
            }

            // Balanced, ready to post
            g2.setColor(new Color(0xbbf7d0));
            g2.fillOval(left - 2, 207, 16, 16);
            g2.setColor(new Color(0x22c55e));
            g2.fillOval(left + 1, 210, 10, 10);
            g2.setFont(Theme.bold(14.5f));
            g2.setColor(new Color(0x17803d));
            g2.drawString("Balanced, ready to post", left + 22, 220);
            g2.dispose();
        }

        private static String ellipsis(Graphics2D g2, String text, int max) {
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(text) <= max) {
                return text;
            }
            String t = text;
            while (t.length() > 1 && fm.stringWidth(t + "…") > max) {
                t = t.substring(0, t.length() - 1);
            }
            return t + "…";
        }
    }

    /** "7 MODULES | 12 COLUMNS | 2 ROLES": a thin line on top, big yellow numbers */
    private static class StatsRow extends JComponent {
        private static final String[][] STATS = {{"7", "MODULES"}, {"12", "COLUMNS"}, {"2", "ROLES"}};

        @Override public Dimension getPreferredSize() { return new Dimension(420, 96); }
        @Override public Dimension getMaximumSize()   { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(0, 4, w, 1);
            int col = w / 3;
            for (int i = 0; i < STATS.length; i++) {
                int x = i * col;
                if (i > 0) {
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillRect(x - 18, 24, 1, 56);
                }
                g2.setFont(Theme.bold(32));
                g2.setColor(GOLD_1);
                g2.drawString(STATS[i][0], x, 58);
                g2.setFont(Theme.regular(13));
                g2.setColor(new Color(0xd6c8fa));
                drawSpaced(g2, STATS[i][1], x, 82, 2);
            }
            g2.dispose();
        }
    }

    /**
     * The sign-up steps on a line: numbered circles joined by a thin rail
     * (the first one filled yellow, the others outlined), each with a bold
     * title and a short line.
     */
    private static class Steps extends JComponent {
        private static final int ROW = 72, D = 36;
        private final String[][] steps;

        Steps(String[][] steps) {
            this.steps = steps;
        }

        @Override public Dimension getPreferredSize() { return new Dimension(420, ROW * (steps.length - 1) + D + 6); }
        @Override public Dimension getMaximumSize()   { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int cx = D / 2 + 1;
            // the rail behind the circles
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillRect(cx - 1, D / 2, 3, ROW * (steps.length - 1));
            for (int i = 0; i < steps.length; i++) {
                int y = i * ROW + 1;
                String n = String.valueOf(i + 1);
                if (i == 0) {
                    g2.setPaint(new GradientPaint(1, y, GOLD_1, 1 + D, y + D, GOLD_2));
                    g2.fillOval(1, y, D, D);
                    g2.setColor(Theme.PURPLE_DEEP);
                } else {
                    g2.setColor(new Color(0x4c1d95));
                    g2.fillOval(1, y, D, D);
                    g2.setColor(GOLD_1);
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawOval(2, y + 1, D - 2, D - 2);
                }
                g2.setFont(Theme.bold(15));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(n, 1 + (D - fm.stringWidth(n)) / 2, y + (D + fm.getAscent() - fm.getDescent()) / 2);
                g2.setFont(Theme.bold(17.5f));
                g2.setColor(Color.WHITE);
                g2.drawString(steps[i][0], D + 20, y + 15);
                g2.setFont(Theme.regular(14.5f));
                g2.setColor(new Color(0xddd0fb));
                g2.drawString(steps[i][1], D + 20, y + 36);
            }
            g2.dispose();
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
