CREATE DATABASE IF NOT EXISTS tala
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE tala;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_code VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(120) NOT NULL,
    account_type ENUM('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE') NOT NULL,
    normal_balance ENUM('DEBIT','CREDIT') NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS journal_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entry_date DATE NOT NULL,
    reference_no VARCHAR(40) UNIQUE,
    description VARCHAR(255) NOT NULL,
    status ENUM('POSTED','REVERSED') NOT NULL DEFAULT 'POSTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS journal_entry_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    journal_entry_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    debit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    line_description VARCHAR(255),
    CONSTRAINT fk_line_entry FOREIGN KEY (journal_entry_id)
        REFERENCES journal_entries(id),
    CONSTRAINT fk_line_account FOREIGN KEY (account_id)
        REFERENCES accounts(id),
    CONSTRAINT chk_nonnegative_amounts CHECK (debit >= 0 AND credit >= 0),
    CONSTRAINT chk_one_side CHECK (
        (debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)
    ),
    INDEX idx_lines_account (account_id),
    INDEX idx_entries_date (journal_entry_id)
);

-- Starter Chart of Accounts. Safe to re-run because account_code is unique.
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
('1010','Cash','ASSET','DEBIT','Cash on hand and in bank'),
('1100','Accounts Receivable','ASSET','DEBIT','Amounts owed by customers'),
('1200','Inventory','ASSET','DEBIT','Inventory held for sale'),
('1500','Office Equipment','ASSET','DEBIT','Office equipment and computers'),
('1590','Accumulated Depreciation','ASSET','CREDIT','Accumulated depreciation contra-asset'),
('2010','Accounts Payable','LIABILITY','CREDIT','Amounts owed to suppliers'),
('2020','Salaries Payable','LIABILITY','CREDIT','Unpaid salary obligations'),
('2030','Taxes Payable','LIABILITY','CREDIT','Taxes payable'),
('2100','Loans Payable','LIABILITY','CREDIT','Outstanding loans'),
('3010',"Owner's Capital",'EQUITY','CREDIT','Owner capital contributions'),
('3020',"Owner's Drawings",'EQUITY','DEBIT','Owner withdrawals'),
('4010','Sales Revenue','REVENUE','CREDIT','Revenue from sales'),
('4020','Service Revenue','REVENUE','CREDIT','Revenue from services'),
('4090','Other Income','REVENUE','CREDIT','Other business income'),
('5010','Cost of Goods Sold','EXPENSE','DEBIT','Cost of goods sold'),
('6010','Rent Expense','EXPENSE','DEBIT','Rent and lease expenses'),
('6020','Salaries Expense','EXPENSE','DEBIT','Employee salaries'),
('6030','Utilities Expense','EXPENSE','DEBIT','Electricity, water, and internet'),
('6040','Supplies Expense','EXPENSE','DEBIT','Office and operating supplies'),
('6050','Transportation Expense','EXPENSE','DEBIT','Transportation expenses');

INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
-- ASSETS
('1300','Supplies','ASSET','DEBIT','Supplies on hand not yet used'),
('1310','Prepaid Insurance','ASSET','DEBIT','Insurance paid in advance'),
('1320','Prepaid Rent','ASSET','DEBIT','Rent paid in advance'),
('1510','Furniture','ASSET','DEBIT','Furniture and fixtures'),
('1520','Vehicles','ASSET','DEBIT','Company vehicles'),
-- LIABILITIES
('2040','Unearned Revenue','LIABILITY','CREDIT','Payments received before service is performed'),
('2110','Notes Payable','LIABILITY','CREDIT','Written promissory notes owed'),
-- EXPENSES
('6060','Advertising Expense','EXPENSE','DEBIT','Advertising and promotion costs'),
('6070','Maintenance Expense','EXPENSE','DEBIT','Maintenance costs'),
('6080','Professional Fees Expense','EXPENSE','DEBIT','Fees for professional services'),
('6090','Repairs Expense','EXPENSE','DEBIT','Repairs costs'),
('6100','Interest Expense','EXPENSE','DEBIT','Interest on loans and notes'),
('6110','Insurance Expense','EXPENSE','DEBIT','Insurance used up during the period'),
('6120','Depreciation Expense','EXPENSE','DEBIT','Depreciation of fixed assets');

-- NEWLY ADDED (previously missing)
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance, description) VALUES
-- ASSETS
('1110','Allowance for Doubtful Accounts','ASSET','CREDIT','Contra-asset for estimated uncollectible receivables'),
('1120','Notes Receivable','ASSET','DEBIT','Written promissory notes owed to the business'),
('1130','Interest Receivable','ASSET','DEBIT','Interest earned but not yet received'),
('1330','Advances to Employees','ASSET','DEBIT','Cash advances given to employees'),
('1600','Land','ASSET','DEBIT','Land owned by the business'),
('1610','Building','ASSET','DEBIT','Buildings owned by the business'),
-- LIABILITIES
('2050','Interest Payable','LIABILITY','CREDIT','Interest incurred but not yet paid'),
('2060','Withholding Tax Payable','LIABILITY','CREDIT','Taxes withheld from employees and suppliers'),
('2070','VAT Payable','LIABILITY','CREDIT','Output VAT net of input VAT'),
('2080','SSS/PhilHealth/Pag-IBIG Payable','LIABILITY','CREDIT','Government contributions withheld and employer share'),
-- EQUITY
('3030','Retained Earnings','EQUITY','CREDIT','Accumulated profits retained in the business'),
('3040','Income Summary','EQUITY','CREDIT','Temporary account used during closing entries'),
-- REVENUE
('4030','Interest Income','REVENUE','CREDIT','Interest earned on bank deposits and notes'),
('4110','Sales Returns and Allowances','REVENUE','DEBIT','Contra-revenue for returned or discounted sales'),
('4120','Sales Discounts','REVENUE','DEBIT','Contra-revenue for early payment discounts'),
-- EXPENSES
('6130','Bad Debts Expense','EXPENSE','DEBIT','Uncollectible accounts expense'),
('6140','Taxes and Licenses Expense','EXPENSE','DEBIT','Business permits, licenses, and local taxes'),
('6150','Communication Expense','EXPENSE','DEBIT','Phone, mobile load, and postage'),
('6160','Bank Charges Expense','EXPENSE','DEBIT','Bank fees and service charges'),
('6170','Employee Benefits Expense','EXPENSE','DEBIT','13th month pay, SSS/PhilHealth/Pag-IBIG employer share'),
('6180','Representation Expense','EXPENSE','DEBIT','Meals and client entertainment'),
('6190','Miscellaneous Expense','EXPENSE','DEBIT','Minor expenses not classified elsewhere'),
('7010','Income Tax Expense','EXPENSE','DEBIT','Income tax for the period');
