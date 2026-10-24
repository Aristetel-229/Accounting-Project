package com.accountpro.ui;

import com.accountpro.service.ResetDataService;

import javax.swing.*;
import java.sql.SQLException;

/** The two-step "Reset System Data" confirmation flow, shared by Dashboard and Settings. */
public final class ResetDataAction {
    private ResetDataAction() {}

    public static void confirmAndRun(MainFrame frame, Runnable afterReset) {
        int choice = JOptionPane.showConfirmDialog(frame,
                "This will permanently delete ALL journal entries, invoices, invoice payments, and account records.\n"
                        + "The default Chart of Accounts will then be restored.\n\nDo you want to continue?",
                "Confirm Full Data Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        String confirmation = JOptionPane.showInputDialog(frame,
                "This action cannot be undone. Type RESET to confirm:", "Final Confirmation", JOptionPane.WARNING_MESSAGE);
        if (!"RESET".equals(confirmation)) {
            JOptionPane.showMessageDialog(frame, "Reset cancelled. No data was changed.",
                    "Reset Cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            ResetDataService.resetAllData();
            JOptionPane.showMessageDialog(frame,
                    "AccountPro data has been reset. The default Chart of Accounts has been restored.",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            afterReset.run();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Could not reset data. The database transaction was rolled back where possible.\n\n" + ex.getMessage(),
                    "Validation / Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
