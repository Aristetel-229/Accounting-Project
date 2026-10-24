# AccountPro — Java Swing + MySQL

Starter Windows desktop accounting application using Java 17+, Swing, JDBC, and MySQL Connector/J.

## Features
- Desktop dashboard with explicitly visible metric labels
- Chart of Accounts viewer
- Post a basic two-line journal entry (one debit and one credit)
- General Ledger view
- Trial Balance
- Basic Income Statement
- Basic Balance Sheet summary
- Cash/bank movement report
- Invoice register and partial/full payment recording
- MySQL persistence using JDBC

## Requirements
- JDK 17 or newer
- Maven 3.9+
- MySQL Server 8.0+
- MySQL Workbench recommended

## 1. Database setup
Open `database/accountpro.sql` in MySQL Workbench and execute the entire script. For an existing `accountpro` database, rerun the script to create the new invoice tables; it uses `CREATE TABLE IF NOT EXISTS` and will preserve existing rows.

## 2. Configure connection
`db/DB.java` reads these environment variables when provided:
- `ACCOUNTPRO_DB_URL`
- `ACCOUNTPRO_DB_USER`
- `ACCOUNTPRO_DB_PASSWORD`

Default username is `root`, and default password is blank. Set the correct values for your MySQL installation. Avoid hardcoding real credentials in source code.

PowerShell example:
```powershell
$env:ACCOUNTPRO_DB_USER="root"
$env:ACCOUNTPRO_DB_PASSWORD="your_mysql_password"
```

## 3. Run
From the project root:
```bash
mvn clean compile exec:java
```

## Important limitations
This remains an educational prototype, not production accounting software. Invoice/payment records currently do not automatically generate journal entries, so they are not yet reflected in the General Ledger or financial statements. The cash-flow screen lists cash/bank movements and does not yet classify activities as operating, investing, or financing. The Balance Sheet is a simplified all-posted-entries summary and requires review, date filters, and period/retained-earnings handling before practical use. Authentication, approvals, tax handling, and audit controls are not yet implemented.

## Project structure
```
src/main/java/com/accountpro/
├── Main.java                  # entry point
├── db/DB.java                 # JDBC connection settings
├── model/AccountOption.java   # combo-box item
├── service/                   # database logic (no Swing)
│   ├── JournalService.java
│   ├── InvoiceService.java
│   ├── ReportService.java
│   └── ResetDataService.java
├── util/Money.java            # currency format / amount parsing
└── ui/                        # Swing
    ├── MainFrame.java         # window, sidebar, status bar
    ├── BasePage.java          # shared page helpers
    ├── Theme.java             # colors and fonts (re-skin the app here)
    ├── ResetDataAction.java
    ├── components/            # RoundedButton, NavButton, Tables, Fields, Cards
    └── pages/                 # one class per screen
```
