package com.tala.ui;

import com.tala.service.ResetDataService;

import javax.swing.*;
import java.sql.SQLException;

/** Two-step "Reset Data" confirmation flow. */
public final class ResetDataAction {
    private ResetDataAction() {}

    public static void confirmAndRun(MainFrame frame, Runnable afterReset) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "This will permanently delete ALL journal entries and adjustments.\n"
                        + "Your Chart of Accounts will be kept.\n\nDo you want to continue?",
                "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        String confirmation = JOptionPane.showInputDialog(frame,
                "This cannot be undone. Type RESET to confirm:", "Final Confirmation", JOptionPane.WARNING_MESSAGE);
        if (confirmation == null || !"RESET".equals(confirmation.trim())) {
            JOptionPane.showMessageDialog(frame, "Reset cancelled. No data was changed.",
                    "Reset Cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            ResetDataService.resetInputs();
            frame.setStatus(" TALA data reset");
            JOptionPane.showMessageDialog(frame, "All entries have been cleared.",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            afterReset.run();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Could not reset data. Nothing was changed.\n\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
