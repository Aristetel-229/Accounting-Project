package tala.ui.pages;

import tala.dao.AccountDAO;
import tala.dao.JournalDAO;
import tala.dao.ActivityDAO;
import tala.dao.SettingsDAO;
import tala.service.Session;
import tala.model.Account;
import tala.model.AccountType;
import tala.ui.MainFrame;
import tala.ui.components.AppButton;
import tala.ui.components.Badge;
import tala.ui.components.Card;
import tala.ui.components.FormField;
import tala.ui.components.IconBox;
import tala.ui.components.Icons;
import tala.ui.components.PageHeader;
import tala.ui.components.ReadOnlyTableModel;
import tala.ui.components.TalaTable;
import tala.ui.components.WrapLabel;
import tala.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ChartOfAccountsPage
 * ------------------------------------------------------------
 * Lists every account, filtered by type (chips) and by a search box.
 * The form on the right adds a new account, with these rules:
 *   - the code has 4 digits
 *   - the first digit matches the type (1 Asset ... 5 Expense)
 *   - the code and the name are not used yet
 */
public class ChartOfAccountsPage extends BasePage {

    private final ReadOnlyTableModel model = new ReadOnlyTableModel("Code", "Account name", "Type", "Normal balance", "Status");
    private final TalaTable table;
    private final List<Account> allAccounts = new ArrayList<>();
    private final List<Account> shownAccounts = new ArrayList<>();
    private final Set<Integer> addedThisSession = new HashSet<>();   // shown as "New"

    private final Card listCard = new Card("Accounts", "");
    private final JTextField searchField = new JTextField();
    private final JPanel chipRow = new tala.ui.components.Segmented.Bar(true);
    private final List<Chip> chips = new ArrayList<>();
    private String typeFilter = "All";
    private final JLabel emptyLabel = new JLabel("No accounts match your search. Try a different code or name.", JLabel.CENTER);

    // form
    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JComboBox<AccountType> typeCombo = new JComboBox<>(AccountType.values());
    private final JComboBox<String> normalCombo = new JComboBox<>(new String[] {Account.DEBIT, Account.CREDIT});
    private final WrapLabel messageLabel = new WrapLabel("");
    private final AppButton deleteButton = new AppButton("Delete", Icons.get(Icons.TRASH, 15, Theme.RED), AppButton.Style.DANGER);
    private final AppButton addButton = new AppButton("Add account", Icons.get(Icons.PLUS, 15, Color.WHITE), AppButton.Style.PRIMARY);

    public ChartOfAccountsPage(MainFrame mainFrame, AccountDAO accountDAO, JournalDAO journalDAO) {
        super(mainFrame, accountDAO, journalDAO);
        useFixedHeight();   // the cards keep their size and scroll on the inside

        table = new TalaTable(model) {
            @Override protected Color rowBackground(int row, boolean selected) {
                boolean isNew = row < shownAccounts.size() && addedThisSession.contains(shownAccounts.get(row).getId());
                return isNew ? Theme.NEW_ROW : selected ? Theme.SELECTED_ROW : Color.WHITE;
            }
            @Override protected boolean cellBold(int row, int column) {
                return column == 0;   // the code is bold, like the design
            }
            @Override protected Color cellForeground(int row, int column) {
                if (column == 3) {   // normal balance: Debit purple, Credit amber
                    Object v = getModel().getValueAt(row, column);
                    return "Credit".equals(v) ? Theme.CREDIT : Theme.DEBIT;
                }
                return column == 0 ? Theme.TITLE : Theme.TEXT;
            }
        };
        table.setCompact(14f, 16);
        table.setRowHeight(40);
        table.setPillColumns(2, 4);
        table.setColumnWidths(92, 0, 100, 140, 92);
        table.setWrapColumns(1);   // long names (e.g. Allowance for Doubtful Accounts) wrap onto two lines

        PageHeader header = new PageHeader(Icons.LIST, "Chart of Accounts",
                "All the accounts your business can use, grouped by type. Add a new one from the panel on the right.");
        AppButton refresh = new AppButton("Refresh", Icons.get(Icons.REFRESH, 16, Theme.PURPLE_DEEP), AppButton.Style.SECONDARY);
        refresh.addActionListener(e -> refreshData());
        header.addAction(refresh);
        addSection(header);

        // compact segmented filter:  [All accounts 32] Assets 16  Liabilities 5 ...
        String[][] chipInfo = {{"All", "All accounts"}, {"Asset", "Assets"}, {"Liability", "Liabilities"},
                {"Equity", "Equity"}, {"Revenue", "Revenue"}, {"Expense", "Expenses"}};
        for (String[] info : chipInfo) {
            Chip chip = new Chip(info[0], info[1]);
            chip.setCompact(true);
            chip.addActionListener(e -> {
                typeFilter = chip.type;
                fillTable();
            });
            chips.add(chip);
            chipRow.add(chip);
        }
        addSection(tala.ui.components.Segmented.leftAligned(chipRow));

        // accounts (left) | add account + code guide (right, scrolls inside)
        JPanel side = new tala.ui.components.ScrollablePanel(null);
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        Card form = createFormCard();
        Card guide = createGuideCard();
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        guide.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(form);
        side.add(Box.createVerticalStrut(4));
        side.add(guide);
        javax.swing.JScrollPane sideScroll = new javax.swing.JScrollPane(side);
        sideScroll.setBorder(BorderFactory.createEmptyBorder());
        sideScroll.setOpaque(false);
        sideScroll.getViewport().setOpaque(false);
        sideScroll.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        sideScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.getVerticalScrollBar().setUnitIncrement(24);

        addFillSection(fillColumns(createListCard(), 0.66, sideScroll, 0.34));
    }

    // =================================================================
    //  Building the cards
    // =================================================================

    private Card createListCard() {
        Theme.styleField(searchField);
        Theme.makeSearch(searchField, "Code or name");
        searchField.setPreferredSize(new Dimension(214, 42));
        searchField.setToolTipText("Type a code or part of a name");
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setFont(Theme.semibold(12.5f));
        searchLabel.setForeground(Theme.LABEL);
        searchLabel.setLabelFor(searchField);
        search.add(searchLabel);
        search.add(searchField);
        listCard.addToHeader(search);

        // Delete the selected account (admins always; users only when an admin turned it on)
        deleteButton.setCompact(true);
        deleteButton.addActionListener(e -> deleteSelectedAccount());
        listCard.addToHeader(deleteButton);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDeleteButton();
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { fillTable(); }
            public void removeUpdate(DocumentEvent e)  { fillTable(); }
            public void changedUpdate(DocumentEvent e) { fillTable(); }
        });

        emptyLabel.setFont(Theme.regular(14));
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        body.add(table.inScroll(), BorderLayout.CENTER);
        body.add(emptyLabel, BorderLayout.SOUTH);
        listCard.setBody(body);
        return listCard;
    }

    private Card createFormCard() {
        Card card = new Card("Add account", "Shows in the table right away");
        card.setCompactTitle();
        card.setHeaderIconWithDot(new IconBox(Icons.PLUS, 30, Color.WHITE, Theme.PURPLE_DEEP).setOutline(new Color(0xc9b8f5)));

        Theme.styleField(codeField);
        Theme.styleField(nameField);
        Theme.styleCombo(typeCombo);
        Theme.styleCombo(normalCombo);
        Theme.setPlaceholder(codeField, "e.g. 1340");
        Theme.setPlaceholder(nameField, "e.g. Prepaid Advertising");
        for (JComponent f : new JComponent[] {codeField, nameField, typeCombo, normalCombo}) {
            f.setFont(Theme.regular(13.5f));
            f.setPreferredSize(new Dimension(100, 42));
        }

        // choosing a type sets the usual normal balance for you
        typeCombo.addActionListener(e -> {
            AccountType type = (AccountType) typeCombo.getSelectedItem();
            normalCombo.setSelectedItem(type.getUsualNormalBalance());
            codeField.setToolTipText(type.getLabel() + " codes start with " + type.getCodePrefix());
        });

        messageLabel.setFont(Theme.semibold(12.5f));

        AppButton add = addButton;
        add.addActionListener(e -> addAccount());

        // Account code | Type, then Account name, then Normal balance
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.gridy = 0;
        c.gridx = 0; c.weightx = 0.45; c.insets = new Insets(0, 0, 6, 5);
        form.add(new FormField("Account code", codeField, null), c);
        c.gridx = 1; c.weightx = 0.55; c.insets = new Insets(0, 5, 6, 0);
        form.add(new FormField("Type", typeCombo, null), c);
        c.gridy = 1; c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(0, 0, 6, 0);
        form.add(new FormField("Account name", nameField, null), c);
        c.gridy = 2;
        form.add(new FormField("Normal balance", normalCombo, "Auto-set from type. Change only for contra accounts."), c);
        c.gridy = 3; c.insets = new Insets(0, 2, 4, 0);
        form.add(messageLabel, c);
        c.gridy = 4; c.insets = new Insets(2, 0, 0, 0);
        form.add(add, c);
        card.setBody(form);
        return card;
    }

    private Card createGuideCard() {
        Card card = new Card("Code guide", "The first digit tells the account type");
        card.setCompactTitle();
        JPanel rows = new JPanel(new GridLayout(5, 1, 0, 0));
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));
        Color[][] colors = {
            {new Color(0xf0ecfb), new Color(0x5b3fb8)}, {Theme.AMBER_BG, Theme.AMBER}, {Theme.BLUE_BG, Theme.BLUE},
            {Theme.GREEN_BG, Theme.GREEN}, {Theme.RED_BG, Theme.RED}};
        AccountType[] types = AccountType.values();
        for (int i = 0; i < types.length; i++) {
            final Color ink = colors[i][1];
            JLabel range = new JLabel(types[i].getCodePrefix() + "000 – " + types[i].getCodePrefix() + "999");
            range.setFont(Theme.regular(12.5f));
            range.setForeground(Theme.TEXT);
            Badge pill = new Badge(types[i].getLabel(), colors[i][0], ink);
            pill.setFont(Theme.semibold(11.5f));
            pill.setIconTextGap(5);
            pill.setIcon(new javax.swing.Icon() {     // a small dot before the type name
                public int getIconWidth()  { return 6; }
                public int getIconHeight() { return 6; }
                public void paintIcon(Component cmp, java.awt.Graphics g, int x, int y) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ink);
                    g2.fillOval(x, y, 6, 6);
                    g2.dispose();
                }
            });
            JPanel row = new GuideRow();
            row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 8));
            JPanel pillHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pillHolder.setOpaque(false);
            pillHolder.add(pill);
            row.add(pillHolder, BorderLayout.CENTER);
            row.add(range, BorderLayout.EAST);
            rows.add(row);
        }
        card.setBody(rows);
        return card;
    }

    /** One line of the code guide; it turns soft lilac under the mouse */
    private static class GuideRow extends JPanel {
        GuideRow() {
            super(new BorderLayout(8, 0));
            setOpaque(false);
            tala.ui.theme.Hover.of(this);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            float t = tala.ui.theme.Hover.of(this).value();
            g2.setColor(Theme.mix(new Color(0xfaf8fd), new Color(0xf1ebff), t));
            g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 14, 14);
            g2.dispose();
        }
    }

    // =================================================================
    //  Data
    // =================================================================

    @Override
    public void refreshData() {
        try {
            allAccounts.clear();
            allAccounts.addAll(accountDAO.getAllAccounts());
            fillTable();
            applyAdminSettings();
        } catch (SQLException e) {
            showError("Could not load the chart of accounts from MySQL.", e);
        }
    }

    /** Shows the accounts that match the chip and the search box */
    private void fillTable() {
        String search = searchField.getText().trim().toLowerCase();
        model.clear();
        shownAccounts.clear();

        for (Account a : allAccounts) {
            boolean typeMatches = typeFilter.equals("All") || a.getType().getLabel().equals(typeFilter);
            boolean searchMatches = search.isEmpty()
                    || String.valueOf(a.getCode()).startsWith(search)
                    || a.getName().toLowerCase().contains(search);
            if (typeMatches && searchMatches) {
                shownAccounts.add(a);
                String status = addedThisSession.contains(a.getId()) ? "New" : "Active";
                model.addRow(new Object[] {a.getCode(), a.getName(), a.getType().getLabel(), a.getNormalBalance(), status});
            }
        }

        // chip labels with counts, e.g. "Assets 16"
        for (Chip chip : chips) {
            int count = 0;
            for (Account a : allAccounts) {
                if (chip.type.equals("All") || a.getType().getLabel().equals(chip.type)) {
                    count++;
                }
            }
            chip.setCount(count);
            chip.setSelected(chip.type.equals(typeFilter));
        }

        listCard.setSubtitle("Showing " + shownAccounts.size() + " of " + allAccounts.size() + " accounts");
        emptyLabel.setVisible(shownAccounts.isEmpty());
        revalidate();
        repaint();
    }

    /** Checks the form and saves the new account */
    private void addAccount() {
        String codeText = codeField.getText().trim();
        String name = nameField.getText().trim();
        AccountType type = (AccountType) typeCombo.getSelectedItem();
        String normal = (String) normalCombo.getSelectedItem();

        if (!canAddAccounts()) {
            showMessage("An admin turned off adding accounts for users.", false);
            return;
        }
        String error = null;
        try {
            if (!codeText.matches("\\d{4}")) {
                error = "Use a 4-digit account code.";
            } else if (codeText.charAt(0) != type.getCodePrefix()) {
                error = type.getLabel() + " codes start with " + type.getCodePrefix() + ". See the code guide below.";
            } else if (accountDAO.isCodeTaken(Integer.parseInt(codeText))) {
                error = "Code " + codeText + " is already used.";
            } else if (name.isEmpty()) {
                error = "Give the account a name.";
            } else if (name.length() > 100) {
                error = "Keep the name under 100 characters.";
            } else if (accountDAO.isNameTaken(name)) {
                error = "An account named " + name + " already exists.";
            }

            if (error != null) {
                showMessage(error, false);
                return;
            }

            Account account = new Account(0, Integer.parseInt(codeText), name, type, normal);
            accountDAO.addAccount(account);
            addedThisSession.add(account.getId());
            ActivityDAO.record("added account " + account.getCode() + " " + account.getName());

            codeField.setText("");
            nameField.setText("");
            typeFilter = "All";
            searchField.setText("");
            refreshData();
            showMessage("Added " + account.getCode() + " - " + account.getName() + ".", true);
        } catch (SQLException e) {
            showError("Could not save the account.", e);
        }
    }

    /** Admins may always add accounts; users only if the switch is on */
    private boolean canAddAccounts() {
        try {
            return Session.isAdmin() || new SettingsDAO().getFlag(SettingsDAO.USERS_CAN_ADD_ACCOUNTS);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Admins may always delete accounts; users only if the switch is on */
    private boolean canDeleteAccounts() {
        try {
            return Session.isAdmin() || new SettingsDAO().getFlag(SettingsDAO.USERS_CAN_DELETE_ACCOUNTS);
        } catch (SQLException e) {
            return false;
        }
    }

    private Account selectedAccount() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int model = table.convertRowIndexToModel(row);
        return model < shownAccounts.size() ? shownAccounts.get(model) : null;
    }

    /** On only when an account is selected and this person may delete accounts */
    private void updateDeleteButton() {
        boolean allowed = canDeleteAccounts();
        deleteButton.setVisible(allowed);
        deleteButton.setEnabled(allowed && selectedAccount() != null);
        deleteButton.setToolTipText(selectedAccount() == null ? "Select an account in the table first" : null);
    }

    /** Deletes the selected account after asking; accounts used in entries are kept */
    private void deleteSelectedAccount() {
        Account a = selectedAccount();
        if (a == null) {
            return;
        }
        if (!canDeleteAccounts()) {
            showInfo("An admin turned off deleting accounts for users.");
            return;
        }
        try {
            int used = accountDAO.countLinesUsing(a.getId());
            if (used > 0) {
                showInfo(a.getCode() + " - " + a.getName() + " is used in " + used
                        + (used == 1 ? " journal line" : " journal lines") + ", so it cannot be deleted.\n"
                        + "Delete or change those entries first.");
                return;
            }
            boolean yes = confirm("Delete account",
                    "Delete " + a.getCode() + " - " + a.getName() + " from the Chart of Accounts?\n"
                    + "No entry uses it. This cannot be undone.",
                    "Delete " + a.getCode(), "Keep it");
            if (!yes) {
                return;
            }
            accountDAO.deleteAccount(a.getId());
            addedThisSession.remove(a.getId());
            ActivityDAO.record("deleted account " + a.getCode() + " " + a.getName());
            table.clearSelection();
            refreshData();
            showMessage("Deleted " + a.getCode() + " - " + a.getName() + ".", true);
        } catch (SQLException e) {
            showError("Could not delete the account.", e);
        }
    }

    /** Turns the Add account form and the Delete button on or off for users, as an admin set them */
    private void applyAdminSettings() {
        updateDeleteButton();
        boolean canAdd = canAddAccounts();
        addButton.setEnabled(canAdd);
        codeField.setEnabled(canAdd);
        nameField.setEnabled(canAdd);
        typeCombo.setEnabled(canAdd);
        normalCombo.setEnabled(canAdd);
        if (!canAdd) {
            showMessage("An admin turned off adding accounts for users.", false);
        } else if (messageLabel.getText().startsWith("An admin")) {
            showMessage("", true);
        }
    }

    private void showMessage(String text, boolean ok) {
        messageLabel.setText(text);
        messageLabel.setForeground(ok ? Theme.GREEN : Theme.RED);
    }

    // =================================================================
    //  Filter chip
    // =================================================================

    /** A segment of the type filter, e.g.  ( Assets 16 ) ; purple when selected */
    private static class Chip extends tala.ui.components.Segmented.Tab {
        final String type;

        Chip(String type, String label) {
            super(label);
            this.type = type;
        }
    }
}
