package tala;

import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import tala.database.DBConnection;
import tala.database.DatabaseSetup;
import tala.ui.LoginFrame;
import tala.ui.theme.Theme;

/**
 * Main  -  START THE PROGRAM HERE
 * ------------------------------------------------------------
 *   1. Use the computer's normal window style
 *   2. Prepare the MySQL database (creates it the first time)
 *   3. Show the log-in screen (LoginFrame); after a good log in
 *      it opens the main window for that user (User or Admin)
 *
 * Project layout (where to look for what):
 *   tala.database  -> connecting to MySQL, creating the tables
 *   tala.model     -> plain data classes (Account, JournalEntry, ...)
 *   tala.dao       -> all the SQL (read/save accounts and entries)
 *   tala.service   -> all the accounting math (ledger, worksheet, totals)
 *   tala.util      -> money and date helpers
 *   tala.ui        -> the screens (pages) and reusable UI pieces
 */
public class Main {

    public static void main(String[] args) {
        // Everything that touches Swing should run on the "Event Dispatch Thread"
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        useSystemLookAndFeel();
        Theme.checkPesoSign();

        try {
            DatabaseSetup.prepareDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "TALA could not connect to MySQL.\n\n"
                    + "Please check:\n"
                    + "  1. MySQL is running (in XAMPP, press Start on MySQL).\n"
                    + "  2. The username and password in tala/database/DBConnection.java\n"
                    + "     (now: user \"" + DBConnection.USERNAME + "\" on " + DBConnection.HOST + ":" + DBConnection.PORT + ").\n"
                    + "  3. mysql-connector-j-<version>.jar is in the lib folder.\n\n"
                    + "Details: " + e.  getMessage(),
                    "Cannot connect to the database", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        new LoginFrame().setVisible(true);
    }

    /** The light TALA look for every window and dialog (see ui/theme/TalaLook.java) */
    private static void useSystemLookAndFeel() {
        tala.ui.theme.TalaLook.install();
    }
}
