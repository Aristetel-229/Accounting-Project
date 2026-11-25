package tala.ui;

import tala.service.AuthService;
import tala.ui.components.AppButton;
import tala.ui.components.FormField;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.WrapLabel;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

/**
 * SignUpDialog
 * ------------------------------------------------------------
 * "Sign up" on the log-in screen. Someone makes their own User account.
 * Depending on the admin switch "Sign-ups need approval" the account
 * is Pending (an admin approves it on the Admin page) or Active.
 *
 * Light design, like the log-in card: a lilac-to-cream header with a
 * yellow icon, white form with lilac text boxes, and a soft footer
 * with Cancel (white) and Create account (yellow).
 */
public class SignUpDialog extends JDialog {

    private final AuthService auth;
    private final JTextField nameField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();
    private final WrapLabel messageLabel = new WrapLabel("");
    private String createdUsername;

    public SignUpDialog(JFrame owner, AuthService auth) {
        super(owner, "TALA - Sign up", true);
        this.auth = auth;

        for (JTextField f : new JTextField[] {nameField, usernameField, passwordField, confirmField}) {
            Theme.styleField(f);
        }
        Theme.setPlaceholder(nameField, "Juan Dela Cruz");
        Theme.setPlaceholder(usernameField, "ucmn-jdelacruz");

        // ---- header: [icon] Create a user account / Admins are added by another admin ...
        JLabel title = new JLabel("Create a user account");
        title.setFont(Theme.bold(22));
        title.setForeground(Theme.TITLE);
        WrapLabel lead = new WrapLabel("Admins are added by another admin on the Admin page.");
        lead.setFont(Theme.regular(13.5f));
        lead.setForeground(Theme.TEXT_SOFT);
        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(lead, BorderLayout.CENTER);
        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(new IconBox(Icons.USER, 42, Theme.YELLOW, Theme.PURPLE_DEEP), BorderLayout.NORTH);

        JPanel header = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = Math.max(1, getWidth());
                // linear-gradient(90deg, #f5f0ff 0%, #ffffff 60%, #fffaeb 100%)
                g2.setPaint(new LinearGradientPaint(0, 0, w, 0, new float[] {0f, 0.6f, 1f},
                        new Color[] {new Color(0xf5f0ff), Color.WHITE, Theme.HEADER_GLOW}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // the violet-to-yellow accent line along the top
                g2.setPaint(new GradientPaint(0, 0, Theme.VIOLET, w, 0, Theme.YELLOW));
                g2.fillRect(0, 0, getWidth(), 4);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(22, 24, 18, 24)));
        header.add(iconHolder, BorderLayout.WEST);
        header.add(titles, BorderLayout.CENTER);

        // ---- form
        messageLabel.setFont(Theme.semibold(13));
        messageLabel.setForeground(Theme.RED);

        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 10, 24));
        Component[] parts = {
            new FormField("Full name", nameField, "e.g. Juan Dela Cruz"),
            Box.createVerticalStrut(8),
            new FormField("Username", usernameField, "Letters, numbers, dot, dash or underscore, e.g. ucmn-jdelacruz"),
            Box.createVerticalStrut(8),
            new FormField("Password", passwordField, "At least " + AuthService.MIN_PASSWORD + " characters"),
            Box.createVerticalStrut(8),
            new FormField("Confirm password", confirmField, "Type the same password again"),
            Box.createVerticalStrut(10),
            messageLabel
        };
        for (Component c : parts) {
            ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(c);
        }

        // ---- footer buttons
        AppButton create = new AppButton("Create account", Icons.get(Icons.CHECK, 16, Theme.ON_YELLOW), AppButton.Style.PRIMARY);
        create.addActionListener(e -> create());
        AppButton cancel = new AppButton("Cancel", AppButton.Style.SECONDARY);
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Theme.FOOTER_BG);
        buttons.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        buttons.add(cancel);
        buttons.add(create);
        getRootPane().setDefaultButton(create);

        // Esc closes the dialog
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        setSize(new Dimension(540, 720));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** The username of the account that was made, or null if the dialog was cancelled */
    public String getCreatedUsername() {
        return createdUsername;
    }

    private void create() {
        String username = usernameField.getText().trim().toLowerCase();
        try {
            String done = auth.signUp(nameField.getText(), username,
                    new String(passwordField.getPassword()), new String(confirmField.getPassword()));
            createdUsername = username;
            JOptionPane.showMessageDialog(this, done, "Account created", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            messageLabel.setText("Could not save the account: " + e.getMessage());
        }
    }
}
