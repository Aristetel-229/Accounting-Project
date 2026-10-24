package com.accountpro.ui.pages;

import com.accountpro.ui.BasePage;
import com.accountpro.ui.components.RoundedButton;
import com.accountpro.ui.MainFrame;
import com.accountpro.ui.ResetDataAction;
import com.accountpro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SettingsPage extends BasePage {
    private final DashboardPage dashboard;

    public SettingsPage(MainFrame frame, DashboardPage dashboard) {
        super(frame);
        this.dashboard = dashboard;
    }

    @Override protected String title() { return "Settings"; }

    @Override protected String hint() { return "Tools for starting fresh. Use with care."; }

    @Override protected JComponent build() {
        JPanel p = pagePanel();
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Data Management");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel description = new JLabel("Reset AccountPro to its initial state for a fresh demo or testing session.");
        description.setBorder(new EmptyBorder(12, 0, 18, 0));
        JLabel warning = new JLabel("This permanently deletes journal entries, invoices, payments, and custom accounts.");
        warning.setForeground(Theme.DANGER);
        warning.setBorder(new EmptyBorder(0, 0, 18, 0));
        JButton resetData = new RoundedButton("Reset System Data", RoundedButton.Kind.DANGER);
        resetData.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetData.addActionListener(e -> ResetDataAction.confirmAndRun(frame, dashboard::show));

        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        warning.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(description);
        panel.add(warning);
        panel.add(resetData);
        p.add(panel, BorderLayout.NORTH);
        return p;
    }
}
