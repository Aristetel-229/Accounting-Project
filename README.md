# TALA Accounting System

A double-entry accounting desktop app.
**Main program:** Java (Swing) - **Database:** MySQL

**New in this version: log in, user access and admin access, and an Admin page.**
There are only two roles: **User** and **Admin**.
An older database that still has the role "Student" is upgraded to "User" automatically on start-up.

## Log in

| Side | Username | Password | What they see |
|---|---|---|---|
| User | `ucmn-student123` | `12345678` | the six accounting pages |
| Admin | `ucmn-admin123` | `12345678` | **Admin** (first tab, opens first) + the six accounting pages |

These two accounts are created the first time the program runs (`DatabaseSetup.java`).
Change their passwords on the Admin page (Users > select > **Reset password**).
Passwords are stored as a salted SHA-256 hash, never as plain text (`util/PasswordUtil.java`).
New and reset passwords need **at least 8 characters** (`AuthService.MIN_PASSWORD`).
A database made by an older TALA is updated on start-up: a starting account whose password
is still the old `12345` gets `12345678` (passwords someone changed are left alone).

The log-in screen also has **Remember me** (remembers the username on this computer),
**Forgot password?** (tells the user to ask an admin) and **Sign up** (its own page in the same window: name, username, email and password; anyone can make a
User account; an admin approves it unless "Sign-ups need approval" is off).
The top bar shows who is logged in (green dot = MySQL connected) and a **Log out** button.

## Admin page (admin access only)

| Part | What it does |
|---|---|
| Tiles | Total users, active users, pending approval, open accounting period |
| Users | Search, filter (All / Admins / Users / Pending). Select a user, then **Approve**, **Activate / Deactivate**, **Make admin / Make user**, **Reset password**, **Delete user**. You cannot deactivate, demote or delete yourself, and TALA always keeps at least one active admin. |
| Add user | Full name, username, password, role (User or Admin) |
| Database | Server version, database name, last backup, number of entries. **Back up database** saves one `.sql` file with every table (restore it in MySQL Workbench / phpMyAdmin). |
| Accounting periods | Every month in use. **Close** a month: nobody can post into it or delete from it (Journal Entry and Add adjustment refuse). **Open** it again any time. |
| System settings | Switches for every user: *Users can add accounts* (on), *Users can delete accounts* (off), *Users can change the period* (on), *Lock closed periods* (on), *Sign-ups need approval* (on). Admins are never blocked by the first three. |
| Recent activity | Log-ins, posted/deleted entries, new accounts, adjustments and admin actions |

## The look ("TALA Screens (Light)" design)

The whole app follows the light TALA design canvas: a lavender-to-cream page with soft
yellow and violet glows, a deep purple "ledger book" sidebar (numbered links, section labels,
the active page as a yellow index tab), white cards with lilac outlines and a lilac-to-cream
header strip, lilac table headers, purple text accents and yellow main buttons. The log-in
screen shows the campus picture under a lavender sky. The font is **Arial** on every page.

Hover effects (all animated, see `ui/theme/Hover.java`): cards and tiles rise and their
outline turns violet, buttons lift and brighten, table rows turn lilac, sidebar links slide
and show a gold marker, text boxes darken their outline (violet ring while typing), switches
slide, and the sidebar can be hidden with the [<] button (a "Menu" button brings it back).

| What | Where |
|---|---|
| All colors and fonts | `ui/theme/Theme.java` |
| Light dialogs, lists, scroll bars | `ui/theme/TalaLook.java` (+ `ComboUI`, `FieldUI`, `FieldBorder`, `TalaScrollBarUI`) |
| Hover animations | `ui/theme/Hover.java` |
| Page background | `ui/theme/Backdrop.java` |
| Sidebar | `ui/components/SideNav.java` |
| Cards (shadow, outline, lift) | `ui/components/RoundedPanel.java`, `Card.java`, `StatCard.java` |
| Segmented tabs / filters | `ui/components/Segmented.java` |
| Page title + buttons | `ui/components/PageHeader.java` |
| Log-in campus picture | `src/main/resources/images/campus.png` |

Nothing has to be installed for the font: Arial comes with Windows (another sans-serif font is used if it is missing).
Amounts show the peso sign (₱1,500.00). If a computer's font has no ₱, the app writes `PHP 1,500.00` instead.

Screens (top navigation). Pictures with the practice data are in `docs/screenshots/`.

| Page | What it does |
|---|---|
| Dashboard | Total assets, liabilities, revenue, expenses, net income, entry count, latest entries, accounting-equation check, quick actions, tip. Point at a tile: it lifts up and a tooltip shows how the number was worked out. |
| Journal Entry | Choose the **accounting period** (From month - To month), then post a transaction (debit account, credit account, amount, date, description) with a live T-account preview and checklist. A date outside the period is refused. Posted entries are listed below the form. |
| General Journal | Date / Particulars / Post ref / Debit / Credit, credits indented, explanation line under each entry, totals, search and month filter |
| Chart of Accounts | All accounts, filter by type, search, add a new account (code rules checked). Select an account and press **Delete** to remove it: admins always can, users only when an admin turns on *Users can delete accounts*. An account used in any journal entry is never deleted. |
| General Ledger | Every used account with its balance; per-account ledger with running balance, totals, ending balance, and an "Abnormal balance" warning |
| 12 Columnar | Unadjusted TB, Adjustments, Adjusted TB, Income statement, Owner's equity, Balance sheet (Dr/Cr each). Starts empty, lists only used accounts, last row shows the balance sheet difference (0.00). Add adjustment pop-up. **Export to Excel** saves a colored workbook (.xlsx: purple title, one color per column group, purple debits / amber credits, cream totals, frozen headers, a summary); plain CSV is still offered in the save window. The four tiles on top are hoverable too. |

---

## 1. What you need

1. **JDK 11 or newer** (JDK 17 or 21 is fine)
2. **MySQL** (XAMPP's MySQL/MariaDB works) - start it before running the app
3. **MySQL Connector/J** jar (the JDBC driver). It is already included:
   `lib/mysql-connector-j-8.4.0.jar`. **Maven is not needed.**

## 2. Connection settings (db.properties)

Your MySQL settings live in a small file next to `run.bat`, **`db.properties`**.
It is listed in `.gitignore`, so your password is **never uploaded to GitHub**.
After downloading the project, copy the example and fill in your own values:

```
copy db.properties.example db.properties
```

```properties
db.host=localhost
db.port=3306
db.name=tala_accounting
db.user=root
db.password=          # XAMPP's root password is empty
```

Without the file, those defaults are used. You can also set any value when starting Java,
which wins over the file, e.g.
`java -Dtala.db.password=secret -Dtala.db.port=3307 -cp "out;src/main/resources;lib/*" tala.Main`.
The code that reads them is `src/main/java/tala/database/DBConnection.java`.

## 3. Run it

The **first run creates the database, the tables, the chart of accounts and the two log-ins by itself**
(`DatabaseSetup.java`). You do not have to import the SQL file.

**Option A - double-click `run.bat`** (Windows, no Maven needed)
The connector jar is already in `lib/`. The script compiles with `javac` and starts the app.
(`run.sh` does the same on macOS/Linux.)

**Option B - NetBeans / IntelliJ as a Maven project**
File > Open Project > choose this folder (it has `pom.xml`). Maven downloads the
connector. Run `tala.Main`.

**Option C - NetBeans "Java with Ant" project**
1. New Project > Java with Ant > Java Application (uncheck "Create Main Class").
2. Copy the `tala` folder from `src/main/java/` **and** the `images` folder from
   `src/main/resources/` into the project's `src` folder (the logo lives in `images`).
3. Right-click Libraries > Add JAR/Folder > the connector jar.
4. Right-click `tala/Main.java` > Run File.

**Practice data (optional):** after the app has started once, run
`database/sample_data.sql` in MySQL Workbench / phpMyAdmin. It adds 11 entries and 4
adjustments for November 2026. You should then see:

| Check | Expected |
|---|---|
| Unadjusted trial balance | P163,000.00 = P163,000.00 |
| Net income | P24,000.00 |
| Ending capital | P120,000.00 |
| Total assets = Liabilities + Owner's equity | P141,500.00 = P21,500.00 + P120,000.00 |
| Balance sheet difference | P0.00 |

## 4. How the code is organized

Read the packages in this order:

```
src/main/java/tala/
|-- Main.java                 START HERE: prepares the database, opens the window
|-- database/
|   |-- DBConnection.java     reads db.properties + getConnection()
|   `-- DatabaseSetup.java    creates the database/tables, adds the starting accounts
|-- model/                    plain data classes (no SQL, no screens)
|   |-- Account, AccountType  one account of the chart (1010 Cash, Asset, Debit)
|   |-- AccountingPeriod      From month - To month; is a date inside it?
|   |-- JournalEntry, JournalLine  a transaction and its debit/credit lines
|   |-- LedgerRow             one line of a General Ledger
|   |-- WorksheetRow, Worksheet    the 12 Columnar
|   `-- DashboardSummary      the Dashboard totals
|-- dao/                      ALL the SQL lives here
|   |-- AccountDAO.java       read / add accounts
|   |-- JournalDAO.java       read / post (transaction + rollback) / delete entries
|   |-- SettingsDAO.java      accounting period, admin switches, closed months (app_settings)
|   |-- UserDAO.java          users: log-in check, add, approve, activate, roles, passwords
|   `-- ActivityDAO.java      the activity log shown on the Admin page
|-- service/
|   |-- AccountingService.java  ALL the accounting math (balances, ledger, worksheet)
|   |-- AuthService.java      log in / sign up / rules for usernames and passwords
|   |-- Session.java          who is logged in right now (Session.isAdmin())
|   `-- BackupService.java    "Back up database" (writes a .sql file with JDBC)
|-- util/
|   |-- Money.java            BigDecimal peso formatting and parsing
|   |-- DateUtil.java         YYYY-MM-DD in, "Nov 3, 2026" out
|   `-- PasswordUtil.java     salted SHA-256 password hashes
`-- ui/
    |-- LoginFrame.java       the log-in screen and the Sign up page (first window)
    |-- MainFrame.java        the window: top bar + pages (CardLayout); Admin tab for admins only
    |-- theme/                colors, fonts and the light look (change the look here)
    |-- components/           reusable pieces: cards, buttons, tables, badges, icons
    `-- pages/                one class per screen + AddAdjustmentDialog (+ AdminPage)
```

The rule: **screens never write SQL** (they call a DAO) and **DAOs never do
accounting math** (that is `AccountingService`). Each layer has one job.

## 5. The accounting rules used

* Every entry is saved in two tables: `journal_entries` (date, reference, description, type)
  and `journal_lines` (one row per debit or credit). Posting uses a **transaction**:
  if any line fails, nothing is saved (`rollback`).
* An entry is accepted only if **total debits = total credits**, the amount is above zero, and
  its date is inside the **accounting period** (first day of the From month to the last day of
  the To month). The period is saved in the `app_settings` table, so it is remembered next time.
* Money is always `BigDecimal` in Java and `DECIMAL(15,2)` in MySQL, never `double`.
* Balances:
  * Debit-normal accounts (Assets, Expenses, Owner's Drawing): **debits - credits**
  * Credit-normal accounts (Liabilities, Owner's Capital, Revenue): **credits - debits**
  * Contra accounts (Allowance for Doubtful Accounts, Accumulated Depreciation) are Assets with a
    **Credit** normal balance, so they reduce total assets.
  * A negative result is an **abnormal balance** (e.g. Cash with a credit balance) and is flagged.
* Net income = Revenue - Expenses. Owner's equity = Capital - Drawing + Net income.
* 12 Columnar:
  1. Unadjusted TB = balances from regular entries
  2. Adjustments = debits/credits of adjusting entries (reference `AJE-...`)
  3. Adjusted TB = unadjusted + adjustment debits - adjustment credits
  4. Revenue/Expense go to the Income statement, Capital/Drawing to Owner's equity,
     Assets/Liabilities to the Balance sheet
  5. Net income closes the Income statement and is added to Owner's equity
  6. Ending capital closes Owner's equity and goes to the Balance sheet credit side
  7. The last row shows the balance sheet **difference**, which is 0.00 when the books balance

## 6. Account code guide

| Codes | Type | Normal balance |
|---|---|---|
| 1000-1999 | Asset | Debit (contra assets: Credit) |
| 2000-2999 | Liability | Credit |
| 3000-3999 | Equity | Credit (Drawing: Debit) |
| 4000-4999 | Revenue | Credit |
| 5000-5999 | Expense | Debit |

## 7. The logo

The TALA logo is `src/main/resources/images/logo.png`. It is used in two places:

* the purple sidebar (in a rounded square), drawn by `ui/components/SideNav.java`
* the window's title bar and the Windows taskbar, set in `ui/MainFrame.java`
  with `setIconImages(AppImages.getWindowIcons())`

To change it, replace that file with another **square** PNG (512 x 512 is ideal).
`ui/theme/AppImages.java` loads it and makes the small sizes. If the file is missing,
the app draws a yellow "T" instead.

## 8. Common problems

| Message | Fix |
|---|---|
| "MySQL Connector/J was not found" | The connector jar is not in `lib/` (or not added to the IDE project). |
| "Wrong username or password" | Use `ucmn-student123` / `12345678` or `ucmn-admin123` / `12345678`. If an admin changed them, ask the admin to reset. |
| "Your account is waiting for an admin" | An admin must open Admin > Users, select you and press **Approve**. |
| Admin tab is missing | You logged in as a user. Log out and log in with an admin account. |
| Forgot the only admin password | In MySQL run `DELETE FROM users;` then restart TALA: the two starting log-ins are made again. |
| "Access denied for user 'root'" | Put the right `db.password` in `db.properties` (next to run.bat). |
| "Communications link failure" | MySQL is not running (XAMPP: press Start on MySQL) or the `PORT` is wrong. |
| Amounts show "PHP" instead of ₱ | That computer's Arial font has no peso sign, so the app writes "PHP" automatically. |
| `run.bat` says "invalid flag" | You have an old `run.bat`. The new one works even when the folder name has spaces (OneDrive, "Documents\My Files" ...). |
