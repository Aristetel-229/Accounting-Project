package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.ActivityDAO;
import tala.dao.JournalDAO;
import tala.dao.SettingsDAO;
import tala.dao.UserDAO;
import tala.database.DBConnection;
import tala.model.JournalEntry;
import tala.model.User;
import tala.service.AuthService;
import tala.service.BackupService;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.FormField;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.Segmented;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.StatCard;
import tala.ui.components.TalaTable;
import tala.ui.components.ToggleSwitch;
import tala.ui.components.WrapLabel;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * AdminPage  (admin access only)
 * ------------------------------------------------------------
 *   - tiles: total users, active users, pending approval, open period
 *   - Users: search, filter by role, and for the selected user:
 *       Approve / Activate / Deactivate / Make admin / Reset password / Delete
 *   - Add user (user or admin), Database info + Back up
 *   - Accounting periods: close a month to lock its entries
 *   - System settings: switches that apply to every user
 *   - Recent activity: the latest lines of the activity log
 *
 * Safety rules: you cannot deactivate, demote or delete yourself,
 * and TALA always keeps at least one active admin.
 */
public class AdminPage extends BasePage {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final SettingsDAO settingsDAO;
    private final UserDAO userDAO;
    private final ActivityDAO activityDAO = new ActivityDAO();
    private final AuthService auth;

    // tiles
    private final StatCard totalTile = new StatCard(Icons.USERS, Theme.PURPLE_TINT, Theme.PURPLE,
            "Total users", "", StatCard.Look.NORMAL);
    private final StatCard activeTile = new StatCard(Icons.CHECK, Theme.GREEN_BG, Theme.GREEN,
            "Active", "", StatCard.Look.NORMAL);
    private final StatCard pendingTile = new StatCard(Icons.BULB, Theme.AMBER_BG, Theme.AMBER,
            "Waiting for approval", "", StatCard.Look.NORMAL);
    private final StatCard periodTile = new StatCard(Icons.LAYERS, Theme.BLUE_BG, Theme.BLUE,
            "Open period", "", StatCard.Look.NORMAL);

    // section tabs: Overview | Users | Accounting periods | Settings | Activity & database
    private final JPanel tabHost = new JPanel(new BorderLayout());
    private final Map<String, JComponent> tabPanels = new java.util.LinkedHashMap<>();
    private final Map<String, Segmented.Tab> tabButtons = new java.util.LinkedHashMap<>();
    private static final String TAB_OVERVIEW = "Overview", TAB_USERS = "Users", TAB_PERIODS = "Accounting periods",
            TAB_SETTINGS = "Settings", TAB_ACTIVITY = "Activity & database";

    // overview cards (colored like the design: purple, yellow, green, blue)
    private final OverviewCard ovUsers = new OverviewCard("Users", new Color(0x5b21b6), new Color(0x8b5cf6),
            new Color(0xf4efff), new Color(0xe3d8fb), new Color(0x3b1578), false);
    private final OverviewCard ovPeriods = new OverviewCard("Accounting periods", new Color(0xd98e04), new Color(0xfbcf4b),
            new Color(0xfff8e1), new Color(0xf6e2a6), new Color(0x6a4a0a), true);
    private final OverviewCard ovSettings = new OverviewCard("Settings", new Color(0x0f8a6a), new Color(0x34c79a),
            new Color(0xe9faf3), new Color(0xc8eedd), new Color(0x0d5c47), false);
    private final OverviewCard ovActivity = new OverviewCard("Activity & database", new Color(0x2457d6), new Color(0x6b95f5),
            new Color(0xeef3ff), new Color(0xd3e0fb), new Color(0x1c3f99), false);

    // users
    private final ReadOnlyTableModel userModel =
            new ReadOnlyTableModel("Name", "Username", "Role", "Status", "Last log in");
    private final TalaTable userTable;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> shownUsers = new ArrayList<>();
    private final Card usersCard = new Card("Users", "");
    private final JTextField searchField = new JTextField();
    private final List<Chip> chips = new ArrayList<>();
    private String roleFilter = "All";
    private final JLabel noUsersLabel = new JLabel("No users match. Try another name or role.", JLabel.CENTER);
    private final JLabel selectedLabel = new JLabel();
    private final AppButton approveButton = new AppButton("Approve", Icons.get(Icons.CHECK, 16, Color.WHITE), AppButton.Style.PRIMARY);
    private final AppButton toggleButton = new AppButton("Deactivate", AppButton.Style.DANGER);
    private final AppButton roleButton = new AppButton("Make admin", Icons.get(Icons.SHIELD, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
    private final AppButton resetButton = new AppButton("Reset password", Icons.get(Icons.KEY, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
    private final AppButton deleteButton = new AppButton("Delete user", Icons.get(Icons.TRASH, 16, Theme.RED), AppButton.Style.DANGER);

    // add user
    private final JTextField nameField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[] {User.USER, User.ADMIN});
    private final WrapLabel addMessage = new WrapLabel("");

    // database / periods / settings / activity
    private final JPanel dbRows = new JPanel(new GridLayout(0, 2, 10, 10));
    private final JPanel periodRows = new JPanel();
    private final Card periodsCard = new Card("Accounting periods", "Close a month to lock its entries");
    private final JPanel settingRows = new JPanel();
    private final JPanel activityRows = new JPanel();

    public AdminPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO,
                     SettingsDAO settingsDAO, UserDAO userDAO) {
        super(mainFrame, accountDAO, journalDAO);
        useFixedHeight();   // tiles + tabs stay put; the section under the tabs fills the window
        this.settingsDAO = settingsDAO;
        this.userDAO = userDAO;
        this.auth = new AuthService(userDAO, settingsDAO);

        userTable = new TalaTable(userModel) {
            @Override
            protected Color[] pillColors(String text) {
                switch (text) {
                    case User.ADMIN:    return new Color[] {Theme.YELLOW, Theme.PURPLE_DEEP};
                    case User.USER:     return new Color[] {Theme.BLUE_BG, Theme.BLUE};
                    case User.ACTIVE:   return new Color[] {Theme.GREEN_BG, Theme.GREEN};
                    case User.PENDING:  return new Color[] {Theme.AMBER_BG, Theme.AMBER};
                    case User.INACTIVE: return new Color[] {Theme.RED_BG, Theme.RED};
                    default:            return super.pillColors(text);
                }
            }
        };
        userTable.setPillColumns(2, 3);
        userTable.setColumnWidths(0, 175, 95, 95, 130);
        userTable.getSelectionModel().addListSelectionListener(e -> updateUserButtons());

        // ---- header: Add user (yellow) + Back up database
        PageHeader header = new PageHeader(Icons.SHIELD, "Admin Panel",
                "Manage who can use TALA, open and close accounting periods, and adjust system settings.");
        AppButton addUserTop = new AppButton("Add user", AppButton.Style.PRIMARY);
        addUserTop.addActionListener(e -> {
            showTab(TAB_USERS);
            nameField.requestFocusInWindow();
        });
        AppButton backup = new AppButton("Back up database", AppButton.Style.SECONDARY);
        backup.addActionListener(e -> backUp());
        header.addAction(addUserTop);
        header.addAction(backup);
        addSection(header);

        // ---- tiles
        JPanel tiles = new JPanel(new GridLayout(1, 4, 12, 0));
        tiles.setOpaque(false);
        for (StatCard tile : new StatCard[] {totalTile, activeTile, pendingTile, periodTile}) {
            tiles.add(tile);
        }
        addSection(tiles);

        // ---- section tabs
        Segmented.Bar bar = new Segmented.Bar();
        for (String name : new String[] {TAB_OVERVIEW, TAB_USERS, TAB_PERIODS, TAB_SETTINGS, TAB_ACTIVITY}) {
            Segmented.Tab tab = new Segmented.Tab(name);
            tab.addActionListener(e -> showTab(name));
            tabButtons.put(name, tab);
            bar.add(tab);
        }
        addSection(Segmented.leftAligned(bar));

        // ---- the panels behind the tabs
        JPanel overview = new JPanel(new GridLayout(2, 2, 14, 14));
        overview.setOpaque(false);
        overview.add(ovUsers);
        overview.add(ovPeriods);
        overview.add(ovSettings);
        overview.add(ovActivity);
        ovUsers.setAction("Manage users", () -> showTab(TAB_USERS));
        ovPeriods.setAction("Manage periods", () -> showTab(TAB_PERIODS));
        ovSettings.setAction("Change settings", () -> showTab(TAB_SETTINGS));
        ovActivity.setAction("View all activity", () -> showTab(TAB_ACTIVITY));
        tabPanels.put(TAB_OVERVIEW, overview);
        tabPanels.put(TAB_USERS, scrollInside(twoColumns(createUsersCard(), 0.66, createAddUserCard(), 0.34)));
        tabPanels.put(TAB_PERIODS, scrollInside(createPeriodsCard()));
        tabPanels.put(TAB_SETTINGS, scrollInside(createSettingsCard()));
        tabPanels.put(TAB_ACTIVITY, scrollInside(twoColumns(createActivityCard(), 0.62, createDatabaseCard(), 0.38)));
        tabHost.setOpaque(false);
        addFillSection(tabHost);
        showTab(TAB_OVERVIEW);
    }

    /** The other tabs scroll inside the area under the tabs (the page itself stays put) */
    private static JComponent scrollInside(JComponent content) {
        JPanel column = new tala.ui.components.ScrollablePanel(new BorderLayout());
        column.setOpaque(false);
        column.add(content, BorderLayout.NORTH);
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(column);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        sp.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        return sp;
    }

    /** Shows one section under the tabs */
    private void showTab(String name) {
        tabHost.removeAll();
        tabHost.add(tabPanels.get(name), BorderLayout.CENTER);
        for (Map.Entry<String, Segmented.Tab> e : tabButtons.entrySet()) {
            e.getValue().setSelected(e.getKey().equals(name));
        }
        tabHost.revalidate();
        tabHost.repaint();
    }

    // =================================================================
    //  Building the cards
    // =================================================================

    private Card createUsersCard() {
        Theme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(220, 46));
        searchField.setToolTipText("Type part of a name or username");
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setFont(Theme.bold(15));
        searchLabel.setForeground(Theme.TITLE);
        searchLabel.setLabelFor(searchField);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        search.add(searchLabel);
        search.add(searchField);
        usersCard.addToHeader(search);
        usersCard.setHeaderIcon(new IconBox(Icons.USERS, 40, Theme.YELLOW, Theme.PURPLE_DEEP));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { fillUsers(); }
            public void removeUpdate(DocumentEvent e)  { fillUsers(); }
            public void changedUpdate(DocumentEvent e) { fillUsers(); }
        });

        Segmented.Bar chipBar = new Segmented.Bar();
        JComponent chipRow = Segmented.leftAligned(chipBar);
        chipRow.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
        for (String[] info : new String[][] {{"All", "All"}, {User.ADMIN, "Admins"}, {User.USER, "Users"}, {User.PENDING, "Pending"}}) {
            Chip chip = new Chip(info[0], info[1]);
            chip.addActionListener(e -> {
                roleFilter = chip.key;
                fillUsers();
            });
            chips.add(chip);
            chipBar.add(chip);
        }

        noUsersLabel.setFont(Theme.regular(15));
        noUsersLabel.setForeground(Theme.MUTED);
        noUsersLabel.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(chipRow, BorderLayout.NORTH);
        body.add(userTable.inPanel(), BorderLayout.CENTER);
        body.add(noUsersLabel, BorderLayout.SOUTH);
        usersCard.setBody(body);

        // footer: what to do with the selected user
        approveButton.addActionListener(e -> approveSelected());
        toggleButton.addActionListener(e -> toggleSelected());
        roleButton.addActionListener(e -> changeRoleOfSelected());
        resetButton.addActionListener(e -> resetPasswordOfSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        selectedLabel.setFont(Theme.bold(15));
        selectedLabel.setForeground(Theme.TITLE);

        JPanel buttons = new JPanel(new GridLayout(0, 3, 8, 8));
        buttons.setOpaque(false);
        for (AppButton b : new AppButton[] {approveButton, toggleButton, roleButton, resetButton, deleteButton}) {
            buttons.add(b);
        }
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.add(selectedLabel, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.CENTER);
        usersCard.setFooter(footer);
        updateUserButtons();
        return usersCard;
    }

    private Card createAddUserCard() {
        Card card = new Card("Add user", "They can log in right away");
        card.setHeaderIcon(new IconBox(Icons.PLUS, 40, Theme.YELLOW, Theme.PURPLE_DEEP));
        Theme.styleField(nameField);
        Theme.styleField(usernameField);
        Theme.styleField(passwordField);
        Theme.styleCombo(roleCombo);
        addMessage.setFont(Theme.bold(14.5f));

        AppButton add = new AppButton("Add user", Icons.get(Icons.PLUS, 16, Color.WHITE), AppButton.Style.PRIMARY);
        add.addActionListener(e -> addUser());
        add.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        JComponent[] parts = {
            new FormField("Full name", nameField, "e.g. Juan Dela Cruz"),
            new FormField("Username", usernameField, "e.g. ucmn-jdelacruz"),
            new FormField("Password", passwordField, "At least " + AuthService.MIN_PASSWORD + " characters"),
            new FormField("Role", roleCombo, "User = user access, Admin = admin access"),
        };
        for (JComponent part : parts) {
            part.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(part);
            form.add(Box.createVerticalStrut(10));
        }
        addMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        add.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(addMessage);
        form.add(Box.createVerticalStrut(8));
        form.add(add);
        card.setBody(form);
        return card;
    }

    private Card createDatabaseCard() {
        Card card = new Card("Database", null);
        card.setHeaderIcon(new IconBox(Icons.DATABASE, 40, Theme.BLUE_BG, Theme.BLUE));
        card.addToHeader(Badge.ok("Connected"));
        dbRows.setOpaque(false);
        dbRows.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));
        card.setBody(dbRows);
        return card;
    }

    private Card createPeriodsCard() {
        periodsCard.setHeaderIcon(new IconBox(Icons.LOCK, 40, Theme.PURPLE_TINT, Theme.PURPLE));
        periodRows.setOpaque(false);
        periodRows.setLayout(new BoxLayout(periodRows, BoxLayout.Y_AXIS));
        periodRows.setBorder(BorderFactory.createEmptyBorder(4, 20, 12, 20));
        periodsCard.setBody(periodRows);
        return periodsCard;
    }

    private Card createSettingsCard() {
        Card card = new Card("System settings", "Apply to every user");
        card.setHeaderIcon(new IconBox(Icons.SLIDERS, 40, Theme.PURPLE_TINT, Theme.PURPLE));
        settingRows.setOpaque(false);
        settingRows.setLayout(new BoxLayout(settingRows, BoxLayout.Y_AXIS));
        settingRows.setBorder(BorderFactory.createEmptyBorder(4, 20, 12, 16));
        card.setBody(settingRows);
        return card;
    }

    private Card createActivityCard() {
        Card card = new Card("Recent activity", "Latest actions in the system");
        card.setHeaderIcon(new IconBox(Icons.CLOCK, 40, Theme.PURPLE_TINT, Theme.PURPLE));
        activityRows.setOpaque(false);
        activityRows.setLayout(new BoxLayout(activityRows, BoxLayout.Y_AXIS));
        activityRows.setBorder(BorderFactory.createEmptyBorder(4, 20, 12, 20));
        card.setBody(activityRows);
        return card;
    }

    // =================================================================
    //  Data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            allUsers.clear();
            allUsers.addAll(userDAO.getAllUsers());
            fillUsers();

            int active = 0, pending = 0;
            for (User u : allUsers) {
                if (u.isActive()) active++;
                if (u.isPending()) pending++;
            }
            totalTile.setValue(String.valueOf(allUsers.size()));
            activeTile.setValue(String.valueOf(active));
            pendingTile.setValue(String.valueOf(pending));
            periodTile.setValue(settingsDAO.getAccountingPeriod().getLabel());
            tabButtons.get(TAB_USERS).setCount(allUsers.size());

            List<JournalEntry> entries = journalDAO.getAllEntries();
            fillDatabase(entries.size());
            fillPeriods(entries);
            fillSettings();
            fillActivity();
            fillOverview(entries, active);
            revalidate();
            repaint();
        } catch (SQLException e) {
            showError("Could not load the admin data from MySQL.", e);
        }
    }

    /** Shows the users that match the chip and the search box */
    private void fillUsers() {
        User keep = selectedUser();
        String search = searchField.getText().trim().toLowerCase();
        userModel.clear();
        shownUsers.clear();
        for (User u : allUsers) {
            boolean chipMatches = roleFilter.equals("All")
                    || (roleFilter.equals(User.PENDING) ? u.isPending() : u.getRole().equals(roleFilter));
            boolean searchMatches = search.isEmpty()
                    || u.getFullName().toLowerCase().contains(search)
                    || u.getUsername().contains(search);
            if (chipMatches && searchMatches) {
                shownUsers.add(u);
                userModel.addRow(new Object[] {u.getFullName(), "@" + u.getUsername(), u.getRole(),
                        u.getStatus(), lastLogin(u.getLastLogin())});
            }
        }
        for (Chip chip : chips) {
            int count = 0;
            for (User u : allUsers) {
                if (chip.key.equals("All") || (chip.key.equals(User.PENDING) ? u.isPending() : u.getRole().equals(chip.key))) {
                    count++;
                }
            }
            chip.setCount(count);
            chip.setSelected(chip.key.equals(roleFilter));
        }
        // keep the same person selected after a refresh
        if (keep != null) {
            for (int i = 0; i < shownUsers.size(); i++) {
                if (shownUsers.get(i).getId() == keep.getId()) {
                    userTable.setRowSelectionInterval(i, i);
                }
            }
        }
        usersCard.setSubtitle("Showing " + shownUsers.size() + " of " + allUsers.size() + " users");
        noUsersLabel.setVisible(shownUsers.isEmpty());
        updateUserButtons();
        revalidate();
        repaint();
    }

    private void fillDatabase(int entryCount) {
        String version = "MySQL";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {
            if (rs.next()) {
                version = "MySQL " + rs.getString(1);
            }
        } catch (SQLException ignored) {
            // keep "MySQL"
        }
        String lastBackup;
        try {
            lastBackup = settingsDAO.getSetting(SettingsDAO.LAST_BACKUP);
        } catch (SQLException e) {
            lastBackup = null;
        }
        dbRows.removeAll();
        addInfo(dbRows, "Server", version);
        addInfo(dbRows, "Database", DBConnection.DATABASE_NAME);
        addInfo(dbRows, "Last backup", lastBackup == null ? "Never" : lastBackup);
        addInfo(dbRows, "Records", entryCount + (entryCount == 1 ? " entry" : " entries"));
    }

    /**
     * One row per month: every month of the accounting period,
     * every month that has entries, and every closed month.
     */
    private void fillPeriods(List<JournalEntry> entries) throws SQLException {
        Map<YearMonth, Integer> counts = new TreeMap<>();
        for (JournalEntry e : entries) {
            counts.merge(YearMonth.from(e.getDate()), 1, Integer::sum);
        }
        tala.model.AccountingPeriod period = settingsDAO.getAccountingPeriod();
        TreeSet<YearMonth> months = new TreeSet<>(counts.keySet());
        for (YearMonth m = period.getFrom(); !m.isAfter(period.getTo()); m = m.plusMonths(1)) {
            months.add(m);
        }
        TreeSet<YearMonth> closed = settingsDAO.getClosedMonths();
        months.addAll(closed);
        boolean lockOn = settingsDAO.getFlag(SettingsDAO.LOCK_CLOSED_PERIODS);
        periodsCard.setSubtitle(lockOn ? "Close a month to lock its entries"
                : "Lock switch is OFF: closed months are not locked");

        periodRows.removeAll();
        int i = 0;
        for (YearMonth m : months.descendingSet()) {
            int n = counts.getOrDefault(m, 0);
            boolean isClosed = closed.contains(m);
            boolean inPeriod = !m.isBefore(period.getFrom()) && !m.isAfter(period.getTo());
            String note = (inPeriod ? "Current period  ·  " : "") + n + (n == 1 ? " entry" : " entries");

            AppButton action = new AppButton(isClosed ? "Open" : "Close", isClosed ? AppButton.Style.SECONDARY : AppButton.Style.DANGER);
            action.setFont(Theme.bold(14));
            action.addActionListener(e -> setClosed(m, !isClosed));
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            right.add(isClosed ? Badge.bad("Closed") : Badge.ok("Open"));
            right.add(action);
            periodRows.add(row(m.format(MONTH), note, right, i++ < months.size() - 1));
        }
    }

    private void fillSettings() throws SQLException {
        settingRows.removeAll();
        Map<String, Boolean> flags = settingsDAO.getAllFlags();
        String[][] switches = SettingsDAO.SWITCHES;
        for (int i = 0; i < switches.length; i++) {
            final String key = switches[i][0];
            final String title = switches[i][1];
            ToggleSwitch sw = new ToggleSwitch(title);
            sw.setSelected(flags.get(key));
            sw.addActionListener(e -> {
                try {
                    settingsDAO.saveFlag(key, sw.isSelected());
                    ActivityDAO.record("turned " + (sw.isSelected() ? "ON" : "OFF") + " \"" + title + "\"");
                    refreshData();
                } catch (SQLException ex) {
                    sw.setSelected(!sw.isSelected());
                    showError("Could not save the setting.", ex);
                }
            });
            settingRows.add(row(title, switches[i][2], sw, i < switches.length - 1));
        }
    }

    private void fillActivity() throws SQLException {
        activityRows.removeAll();
        List<ActivityDAO.Activity> list = activityDAO.getRecent(8);
        if (list.isEmpty()) {
            JLabel none = new JLabel("Nothing yet.");
            none.setFont(Theme.regular(15));
            none.setForeground(Theme.MUTED);
            activityRows.add(none);
            return;
        }
        LocalDate today = LocalDate.now();
        for (int i = 0; i < list.size(); i++) {
            ActivityDAO.Activity a = list.get(i);
            String when = a.time.toLocalDate().equals(today) ? a.time.format(TIME) : a.time.format(SHORT_DAY);
            JLabel time = new JLabel(when);
            time.setFont(Theme.regular(13.5f));
            time.setForeground(Theme.MUTED);
            time.setPreferredSize(new Dimension(70, 20));
            time.setVerticalAlignment(JLabel.TOP);
            WrapLabel what = new WrapLabel("");
            what.setFont(Theme.regular(14.5f));
            what.setForeground(Theme.TEXT);
            what.setText(a.who + " " + a.what);

            JPanel line = new FitPanel(new BorderLayout(8, 0));
            line.setOpaque(false);
            line.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, i < list.size() - 1 ? 1 : 0, 0, Theme.ROW_LINE),
                    BorderFactory.createEmptyBorder(9, 0, 9, 0)));
            line.add(time, BorderLayout.WEST);
            line.add(what, BorderLayout.CENTER);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            activityRows.add(line);
        }
    }

    // =================================================================
    //  Actions
    // =================================================================

    private void addUser() {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim().toLowerCase();
        String password = new String(passwordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();
        try {
            String problem = auth.checkNewUser(name, username, password);
            if (problem != null) {
                showAddMessage(problem, false);
                return;
            }
            userDAO.addUser(name, username, password, role, User.ACTIVE);
            ActivityDAO.record("added " + role.toLowerCase() + " @" + username);
            nameField.setText("");
            usernameField.setText("");
            passwordField.setText("");
            roleFilter = "All";
            searchField.setText("");
            refreshData();
            showAddMessage("Added " + name + " as " + role.toLowerCase() + ".", true);
        } catch (SQLException e) {
            showError("Could not save the user.", e);
        }
    }

    private void approveSelected() {
        User u = selectedUser();
        if (u == null || !u.isPending()) {
            return;
        }
        changeStatus(u, User.ACTIVE, "approved @" + u.getUsername());
    }

    private void toggleSelected() {
        User u = selectedUser();
        if (u == null || isMe(u)) {
            return;
        }
        if (u.isActive()) {
            if (wouldRemoveLastAdmin(u)) {
                return;
            }
            changeStatus(u, User.INACTIVE, "deactivated @" + u.getUsername());
        } else {
            changeStatus(u, User.ACTIVE, "activated @" + u.getUsername());
        }
    }

    private void changeRoleOfSelected() {
        User u = selectedUser();
        if (u == null || isMe(u)) {
            return;
        }
        String newRole = u.isAdmin() ? User.USER : User.ADMIN;
        if (u.isAdmin() && wouldRemoveLastAdmin(u)) {
            return;
        }
        boolean yes = confirm("Change role",
                "Give " + u.getFullName() + " " + (newRole.equals(User.ADMIN) ? "ADMIN access" : "USER access") + "?\n"
                + (newRole.equals(User.ADMIN) ? "Admins can manage users, periods and settings." : "They will lose the Admin page."),
                "Yes, make " + newRole.toLowerCase(), "Cancel");
        if (!yes) {
            return;
        }
        try {
            userDAO.setRole(u.getId(), newRole);
            ActivityDAO.record("changed @" + u.getUsername() + " to " + newRole.toLowerCase());
            refreshData();
        } catch (SQLException e) {
            showError("Could not change the role.", e);
        }
    }

    private void resetPasswordOfSelected() {
        User u = selectedUser();
        if (u == null) {
            return;
        }
        JPasswordField field = new JPasswordField();
        Theme.styleField(field);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("New password for " + u.getFullName() + " (@" + u.getUsername() + "):"), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        int choice = JOptionPane.showConfirmDialog(this, panel, "Reset password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        String password = new String(field.getPassword());
        if (password.length() < AuthService.MIN_PASSWORD) {
            showInfo("Passwords need at least " + AuthService.MIN_PASSWORD + " characters. Nothing was changed.");
            return;
        }
        try {
            userDAO.resetPassword(u.getId(), password);
            ActivityDAO.record("reset the password of @" + u.getUsername());
            showInfo("The password of @" + u.getUsername() + " was changed.");
            refreshData();
        } catch (SQLException e) {
            showError("Could not reset the password.", e);
        }
    }

    private void deleteSelected() {
        User u = selectedUser();
        if (u == null || isMe(u) || wouldRemoveLastAdmin(u)) {
            return;
        }
        boolean yes = confirm("Delete user",
                "Delete " + u.getFullName() + " (@" + u.getUsername() + ")?\n"
                + "They can no longer log in. Their journal entries stay in the books.",
                "Delete @" + u.getUsername(), "Keep");
        if (!yes) {
            return;
        }
        try {
            userDAO.deleteUser(u.getId());
            ActivityDAO.record("deleted user @" + u.getUsername());
            userTable.clearSelection();
            refreshData();
        } catch (SQLException e) {
            showError("Could not delete the user.", e);
        }
    }

    private void setClosed(YearMonth month, boolean close) {
        try {
            settingsDAO.setMonthClosed(month, close);
            ActivityDAO.record((close ? "closed " : "opened ") + month.format(MONTH));
            refreshData();
        } catch (SQLException e) {
            showError("Could not save the period.", e);
        }
    }

    private void backUp() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save the database backup");
        chooser.setFileFilter(new FileNameExtensionFilter("SQL file (*.sql)", "sql"));
        chooser.setSelectedFile(new File(DBConnection.DATABASE_NAME + "_backup_" + LocalDate.now() + ".sql"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".sql")) {
            file = new File(file.getParentFile(), file.getName() + ".sql");
        }
        try {
            int rows = new BackupService().backup(file);
            String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH));
            settingsDAO.saveSetting(SettingsDAO.LAST_BACKUP, when);
            ActivityDAO.record("backed up " + DBConnection.DATABASE_NAME);
            refreshData();
            showInfo("Backup saved (" + rows + " rows):\n" + file.getAbsolutePath()
                    + "\n\nTo restore it, run the file in MySQL Workbench or phpMyAdmin.");
        } catch (SQLException | IOException e) {
            showError("The backup was NOT saved.", e);
        }
    }

    // =================================================================
    //  Helpers
    // =================================================================

    private User selectedUser() {
        int row = userTable.getSelectedRow();
        return row < 0 || row >= shownUsers.size() ? null : shownUsers.get(userTable.convertRowIndexToModel(row));
    }

    private boolean isMe(User u) {
        User me = mainFrame.getCurrentUser();
        return me != null && me.getId() == u.getId();
    }

    /** True (and explains why) if this change would leave TALA with no active admin */
    private boolean wouldRemoveLastAdmin(User u) {
        try {
            if (u.isAdmin() && u.isActive() && userDAO.countActiveAdmins() <= 1) {
                showInfo(u.getFullName() + " is the only active admin.\nAdd or activate another admin first.");
                return true;
            }
        } catch (SQLException e) {
            showError("Could not check the admins.", e);
            return true;
        }
        return false;
    }

    private void changeStatus(User u, String status, String logText) {
        try {
            userDAO.setStatus(u.getId(), status);
            ActivityDAO.record(logText);
            refreshData();
        } catch (SQLException e) {
            showError("Could not change the user.", e);
        }
    }

    /** Turns the footer buttons on/off and renames them for the selected user */
    private void updateUserButtons() {
        User u = selectedUser();
        boolean any = u != null;
        boolean me = any && isMe(u);
        selectedLabel.setText(!any ? "Select a user in the table to manage them"
                : "Selected: " + u.getFullName() + "  (@" + u.getUsername() + ")" + (me ? "  -  that's you" : ""));
        approveButton.setEnabled(any && u.isPending());
        toggleButton.setText(any && u.isActive() ? "Deactivate" : "Activate");
        toggleButton.setEnabled(any && !me);
        roleButton.setText(any && u.isAdmin() ? "Make user" : "Make admin");
        roleButton.setEnabled(any && !me);
        resetButton.setEnabled(any);
        deleteButton.setEnabled(any && !me);
    }

    private void showAddMessage(String text, boolean ok) {
        addMessage.setText(text);
        addMessage.setForeground(ok ? Theme.GREEN : Theme.RED);
    }

    private static String lastLogin(LocalDateTime time) {
        if (time == null) {
            return "Never";
        }
        LocalDate day = time.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return "Today, " + time.format(TIME);
        }
        if (day.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        return time.format(DAY);
    }

    private static void addInfo(JPanel grid, String label, String value) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel small = new JLabel(label);
        small.setFont(Theme.regular(13.5f));
        small.setForeground(Theme.MUTED);
        JLabel big = new JLabel(value);
        big.setFont(Theme.bold(15.5f));
        big.setForeground(Theme.TITLE);
        big.setToolTipText(value);
        box.add(small);
        box.add(big);
        grid.add(box);
    }

    /** A row:  Title / small note  ...........  [right side] */
    private static JPanel row(String title, String note, JComponent right, boolean lineBelow) {
        JLabel t = new JLabel(title);
        t.setFont(Theme.bold(15.5f));
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));   // room for the last letter (bold Poppins runs wide)
        t.setForeground(Theme.TITLE);
        WrapLabel n = new WrapLabel(note);   // long notes wrap instead of being cut
        n.setFont(Theme.regular(13.5f));
        n.setForeground(Theme.MUTED);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(t);
        texts.add(n);

        JPanel row = new FitPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, lineBelow ? 1 : 0, 0, Theme.ROW_LINE),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        row.add(texts, BorderLayout.CENTER);
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setOpaque(false);
        holder.add(right);
        row.add(holder, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** A panel that never grows taller than it needs (so rows stay at the top of a card) */
    private static class FitPanel extends JPanel {
        FitPanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** A role filter segment with a count, e.g. ( Users 12 ); purple when selected */
    private static class Chip extends Segmented.Tab {
        final String key;

        Chip(String key, String label) {
            super(label);
            this.key = key;
        }
    }

    // =================================================================
    //  Overview
    // =================================================================

    /** Fills the four colored overview cards */
    private void fillOverview(List<JournalEntry> entries, int active) throws SQLException {
        // ---- users: admins / users split, then who is waiting
        int admins = 0, users = 0;
        List<User> waiting = new ArrayList<>();
        for (User u : allUsers) {
            if (u.isAdmin()) admins++; else users++;
            if (u.isPending()) waiting.add(u);
        }
        ovUsers.setSubtitle(allUsers.size() + " accounts · " + active + " active");
        JPanel ub = ovUsers.body();
        ub.removeAll();
        ub.add(new SplitBar(new double[] {admins, users}, new Color[] {Theme.YELLOW, new Color(0xc4b5fd)}));
        ub.add(legend(new String[] {"Admins " + admins, "Users " + users}, new Color[] {Theme.GOLD_DARK, new Color(0xa78bfa)}));
        for (int i = 0; i < waiting.size() && i < 2; i++) {
            final User u = waiting.get(i);
            AppButton approve = new AppButton("Approve", AppButton.Style.PRIMARY);
            approve.setCompact(true);
            approve.addActionListener(e -> changeStatus(u, User.ACTIVE, "approved @" + u.getUsername()));
            ub.add(personRow(u, approve));
        }
        if (waiting.isEmpty()) {
            ub.add(ovRow("No one is waiting for approval", Badge.ok("All clear"), ovUsers));
        }

        // ---- periods: a short timeline + open period + entries in it
        tala.model.AccountingPeriod period = settingsDAO.getAccountingPeriod();
        TreeSet<YearMonth> closed = settingsDAO.getClosedMonths();
        YearMonth now = YearMonth.now();
        YearMonth center = !now.isBefore(period.getFrom()) && !now.isAfter(period.getTo()) ? now : period.getTo();
        int inPeriod = 0;
        for (JournalEntry e : entries) {
            YearMonth m = YearMonth.from(e.getDate());
            if (!m.isBefore(period.getFrom()) && !m.isAfter(period.getTo())) {
                inPeriod++;
            }
        }
        ovPeriods.setSubtitle("Users post into the open period");
        JPanel pb = ovPeriods.body();
        pb.removeAll();
        JPanel steps = new FitPanel(new GridLayout(1, 3, 8, 0));
        steps.setOpaque(false);
        steps.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        steps.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (YearMonth m : new YearMonth[] {center.minusMonths(1), center, center.plusMonths(1)}) {
            boolean isClosed = closed.contains(m);
            boolean inside = !m.isBefore(period.getFrom()) && !m.isAfter(period.getTo());
            String status = isClosed ? "Closed" : inside ? "Open" : m.isAfter(period.getTo()) ? "Upcoming" : "Past";
            steps.add(new TimelineStep(m.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH),
                    status, inside && !isClosed && m.equals(center)));
        }
        pb.add(steps);
        pb.add(ovRow("Open now", boldLabel(period.getLabel(), ovPeriods.ink), ovPeriods));
        pb.add(ovRow("Entries this period", boldLabel(String.valueOf(inPeriod), ovPeriods.ink), ovPeriods));

        // ---- settings: On / Off for each switch
        Map<String, Boolean> flags = settingsDAO.getAllFlags();
        JPanel sb = ovSettings.body();
        sb.removeAll();
        int on = 0;
        for (String[] sw : SettingsDAO.SWITCHES) {
            boolean isOn = Boolean.TRUE.equals(flags.get(sw[0]));
            if (isOn) on++;
            Badge b = isOn ? dotPill("On", new Color(0xe3f6ec), new Color(0x0f8a6a))
                    : dotPill("Off", new Color(0xeef0f4), new Color(0x6b7280));
            sb.add(ovRow(sw[1], b, ovSettings));
        }
        ovSettings.setSubtitle(on + " of " + SettingsDAO.SWITCHES.length + " switched on");

        // ---- activity & database: last backup + the latest three actions
        String lastBackup;
        try {
            lastBackup = settingsDAO.getSetting(SettingsDAO.LAST_BACKUP);
        } catch (SQLException e) {
            lastBackup = null;
        }
        ovActivity.setSubtitle("Last backup " + (lastBackup == null ? "never" : lastBackup));
        ovActivity.setHeaderBadge("Connected");
        JPanel ab = ovActivity.body();
        ab.removeAll();
        List<ActivityDAO.Activity> logs = activityDAO.getRecent(3);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < logs.size(); i++) {
            ActivityDAO.Activity a = logs.get(i);
            String when = a.time.toLocalDate().equals(today) ? a.time.format(TIME) : a.time.format(SHORT_DAY);
            ab.add(new LogStep(when, a.who, a.what, ovActivity.c1, i == logs.size() - 1));
        }
        if (logs.isEmpty()) {
            ab.add(ovRow("Nothing yet", new JLabel(""), ovActivity));
        }
        for (OverviewCard card : new OverviewCard[] {ovUsers, ovPeriods, ovSettings, ovActivity}) {
            card.revalidate();
            card.repaint();
        }
    }

    /** A small pill with a dot before the word, e.g.  ( • On ) */
    private static Badge dotPill(String text, Color bg, final Color ink) {
        Badge b = new Badge(text, bg, ink);
        b.setFont(Theme.semibold(12));
        b.setIconTextGap(6);
        b.setIcon(new javax.swing.Icon() {
            public int getIconWidth()  { return 6; }
            public int getIconHeight() { return 6; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ink);
                g2.fillOval(x, y, 6, 6);
                g2.dispose();
            }
        });
        return b;
    }

    private static JLabel boldLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.semibold(13));
        l.setForeground(color);
        return l;
    }

    /** "Label ............ [right]" with a thin line under it */
    private static JPanel ovRow(String text, JComponent right, OverviewCard card) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.regular(13));
        l.setForeground(Theme.MUTED);
        JPanel row = new FitPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xefeaf7)),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        row.add(l, BorderLayout.CENTER);
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setOpaque(false);
        holder.add(right);
        row.add(holder, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JPanel legend(String[] words, Color[] colors) {
        JPanel row = new FitPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        for (int i = 0; i < words.length; i++) {
            final Color c = colors[i];
            JLabel l = new JLabel(words[i]);
            l.setFont(Theme.regular(12.5f));
            l.setForeground(Theme.MUTED);
            l.setIcon(new javax.swing.Icon() {
                public int getIconWidth()  { return 8; }
                public int getIconHeight() { return 8; }
                public void paintIcon(Component cmp, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(c);
                    g2.fillOval(x, y, 8, 8);
                    g2.dispose();
                }
            });
            l.setIconTextGap(6);
            l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
            row.add(l);
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** A white box: (CM) Carlo Mendoza / Waiting for approval ...... [Approve] */
    private JPanel personRow(final User u, JComponent action) {
        JPanel row = new FitPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ovUsers.soft);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.setColor(ovUsers.line);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 8));
        JComponent avatar = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(32, 32); }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int y = (getHeight() - 32) / 2;
                g2.setPaint(new java.awt.GradientPaint(0, y, Theme.PURPLE_DARK, 32, y + 32, Theme.PURPLE_BRIGHT));
                g2.fillOval(0, y, 32, 32);
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.bold(11.5f));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String ini = u.getInitials();
                g2.drawString(ini, (32 - fm.stringWidth(ini)) / 2, y + (32 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        JLabel name = new JLabel(u.getFullName());
        name.setFont(Theme.semibold(13));
        name.setForeground(ovUsers.ink);
        JLabel note = new JLabel("Waiting for approval");
        note.setFont(Theme.regular(12));
        note.setForeground(Theme.MUTED);
        JPanel words = new JPanel(new GridLayout(2, 1));
        words.setOpaque(false);
        words.add(name);
        words.add(note);
        row.add(avatar, BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setOpaque(false);
        holder.add(action);
        row.add(holder, BorderLayout.EAST);
        JPanel spaced = new FitPanel(new BorderLayout());
        spaced.setOpaque(false);
        spaced.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        spaced.add(row, BorderLayout.CENTER);
        spaced.setAlignmentX(Component.LEFT_ALIGNMENT);
        return spaced;
    }

    /**
     * One overview card (2 x 2 grid on the Overview tab):
     *
     *   [■] Users                               [badge]
     *       6 accounts · 4 active
     *   ------------------------------------------------
     *   body (scrolls inside when it does not fit)
     *   ------------------------------------------------
     *   [ Manage users -> ]        soft tinted button, bottom left
     *
     * White card with a small colored square before the title. Hover: the
     * outline glows in the card's color and the shadow deepens.
     */
    private static class OverviewCard extends tala.ui.components.RoundedPanel {
        final Color c1, c2, soft, line, ink;
        private final JLabel subtitle = new JLabel(" ");
        private final JPanel badgeSlot = new JPanel(new java.awt.GridBagLayout());
        private final JPanel body = new tala.ui.components.ScrollablePanel(null);
        private final JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        OverviewCard(String title, Color c1, Color c2, Color soft, Color line, Color ink, boolean darkText) {
            super(new BorderLayout());
            this.c1 = c1;
            this.c2 = c2;
            this.soft = soft;
            this.line = line;
            this.ink = ink;
            setRadius(14);
            setBorderColor(new Color(0xe8e2f4)).setBorderWidth(1.2f);
            setHoverable(c1);

            JLabel t = new JLabel(title);
            t.setFont(Theme.semibold(15));
            t.setForeground(Theme.TITLE);
            t.setIcon(new javax.swing.Icon() {     // the small colored square before the title
                public int getIconWidth()  { return 10; }
                public int getIconHeight() { return 10; }
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(OverviewCard.this.c1);
                    g2.fillRoundRect(x, y, 10, 10, 3, 3);
                    g2.dispose();
                }
            });
            t.setIconTextGap(10);
            t.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            subtitle.setFont(Theme.regular(12.5f));
            subtitle.setForeground(Theme.MUTED);
            subtitle.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            JPanel titles = new JPanel(new GridLayout(2, 1));
            titles.setOpaque(false);
            titles.add(t);
            titles.add(subtitle);

            JPanel head = new JPanel(new BorderLayout(8, 0));
            head.setOpaque(false);
            head.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
            head.add(titles, BorderLayout.CENTER);
            badgeSlot.setOpaque(false);
            head.add(badgeSlot, BorderLayout.EAST);

            // the body scrolls inside the card, so every card keeps the same size
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(BorderFactory.createEmptyBorder(2, 14, 8, 10));
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(body);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
            scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(16);

            foot.setOpaque(false);
            foot.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xefeaf7)),
                    BorderFactory.createEmptyBorder(6, 12, 0, 12)));

            add(head, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(foot, BorderLayout.SOUTH);
        }

        JPanel body() {
            return body;
        }

        void setSubtitle(String text) {
            subtitle.setText(text);
        }

        void setHeaderBadge(String text) {
            badgeSlot.removeAll();
            Badge b = new Badge(text, soft, c1);
            b.setFont(Theme.semibold(12));
            badgeSlot.add(b);
        }

        void setAction(String text, Runnable action) {
            foot.add(new SoftButton(text, this, action));
        }
    }

    /** "Manage users ->": a soft tinted button in the card's color; it darkens and nudges its arrow on hover */
    private static class SoftButton extends JButton {
        private final OverviewCard card;

        SoftButton(String text, OverviewCard card, Runnable action) {
            super(text);
            this.card = card;
            setFont(Theme.semibold(13));
            setForeground(card.c1.darker());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(LEFT);
            tala.ui.theme.Hover.of(this);
            addActionListener(e -> action.run());
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(12 + fm.stringWidth(getText()) + 8 + 14 + 12, 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float t = tala.ui.theme.Hover.of(this).value();
            int w = getWidth() - 1, h = 32, y = 2;
            g2.setColor(Theme.mix(card.soft, Theme.mix(card.soft, card.c2, 0.35f), t));
            g2.fillRoundRect(0, y, w, h, 16, 16);
            g2.setColor(Theme.mix(card.line, card.c2, t * 0.6f));
            g2.setStroke(new java.awt.BasicStroke(1.2f));
            g2.drawRoundRect(0, y, w, h, 16, 16);
            if (isFocusOwner()) {
                g2.setColor(Theme.FOCUS);
                g2.drawRoundRect(1, y + 1, w - 2, h - 2, 16, 16);
            }
            g2.setFont(getFont());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            Color ink = Theme.mix(card.c1, card.ink, 0.5f);
            g2.setColor(ink);
            int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), 12, ty);
            // arrow
            float ax = 12 + fm.stringWidth(getText()) + 8 + 2 * t, ay = y + h / 2f;
            g2.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.draw(new java.awt.geom.Line2D.Float(ax, ay, ax + 10, ay));
            g2.draw(new java.awt.geom.Line2D.Float(ax + 6, ay - 4, ax + 10, ay));
            g2.draw(new java.awt.geom.Line2D.Float(ax + 6, ay + 4, ax + 10, ay));
            g2.dispose();
        }
    }

    /** A rounded bar split into colored parts (counts) */
    private static class SplitBar extends JComponent {
        private final double[] parts;
        private final Color[] colors;

        SplitBar(double[] parts, Color[] colors) {
            this.parts = parts;
            this.colors = colors;
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(100, 26); }
        @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 26); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), y = 8, h = 10;
            java.awt.Shape track = new java.awt.geom.RoundRectangle2D.Float(0, y, w, h, h, h);
            g2.setColor(new Color(0xf0edf6));
            g2.fill(track);
            g2.clip(track);
            double total = 0;
            for (double p : parts) total += p;
            int x = 0;
            for (int i = 0; i < parts.length && total > 0; i++) {
                int pw = (int) Math.round(parts[i] / total * w);
                g2.setColor(colors[i]);
                g2.fillRect(x, y, pw, h);
                x += pw;
            }
            g2.dispose();
        }
    }

    /** One month box:  October / Closed ; the open month glows yellow. It rises a little on hover. */
    private static class TimelineStep extends JComponent {
        private final String month, status;
        private final boolean current;

        TimelineStep(String month, String status, boolean current) {
            this.month = month;
            this.status = status;
            this.current = current;
            tala.ui.theme.Hover.of(this);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(90, 58); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float t = tala.ui.theme.Hover.of(this).value();
            int w = getWidth() - 1, h = getHeight() - 5;
            float y = 2 - 1.5f * t;
            java.awt.geom.RoundRectangle2D box = new java.awt.geom.RoundRectangle2D.Float(0, y, w, h, 16, 16);
            if (current) {
                g2.setColor(new Color(217, 142, 4, 45));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(1, 5, w - 2, h, 16, 16));
                g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(0xfff3c4), w, h, new Color(0xfde68a)));
            } else {
                g2.setColor(Theme.mix(new Color(0xfaf8fd), new Color(0xf3eeff), t));
            }
            g2.fill(box);
            g2.setColor(current ? new Color(0xf3c64a) : Theme.mix(new Color(0xebe6f5), new Color(0xc9b8f5), t));
            g2.setStroke(new java.awt.BasicStroke(1.2f));
            g2.draw(box);

            g2.setFont(Theme.regular(11.5f));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.setColor(current ? new Color(0x8a5a00) : Theme.MUTED);
            g2.drawString(month, (w - fm.stringWidth(month)) / 2f, y + 20);
            g2.setFont(Theme.semibold(13.5f));
            fm = g2.getFontMetrics();
            g2.setColor(current ? new Color(0x5a3a00) : Theme.TITLE);
            g2.drawString(status, (w - fm.stringWidth(status)) / 2f, y + 40);
            g2.dispose();
        }
    }

    /** One line of the activity timeline: (dot) 9:12 PM / Maria Santos backed up ... */
    private static class LogStep extends JPanel {
        private final Color dot;
        private final boolean last;

        LogStep(String when, String who, String what, Color dot, boolean last) {
            super(new BorderLayout(0, 2));
            this.dot = dot;
            this.last = last;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 20, 6, 0));
            JLabel time = new JLabel(when);
            time.setFont(Theme.semibold(12));
            time.setForeground(dot);
            WrapLabel text = new WrapLabel("");
            text.setFont(Theme.semibold(13));
            text.setForeground(new Color(0x1c3f99));
            text.setText(esc(who) + " <span style='color:#7a7392'>" + esc(what) + "</span>");
            add(time, BorderLayout.NORTH);
            add(text, BorderLayout.CENTER);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        private static String esc(String s) {
            return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!last) {
                g2.setColor(new Color(0xd3e0fb));
                g2.fillRect(7, 24, 2, getHeight() - 16);
            }
            g2.setColor(new Color(0xdbe6ff));
            g2.fillOval(0, 10, 15, 15);
            g2.setColor(dot);
            g2.fillOval(3, 13, 9, 9);
            g2.dispose();
        }
    }
}
