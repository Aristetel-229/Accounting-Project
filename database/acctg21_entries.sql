-- ============================================================
--  TALA Accounting System - ACCTG21 Activity (Jan 1 - Feb 28, 2026)
-- ============================================================
--  44 regular entries (JE-000001 .. JE-000044)
--  6 adjusting entries (AJE-000001 .. AJE-000006)
--  Matches the 12-column worksheet (no Mar 31 adjustments).
--
--  Run this AFTER the app has started once (so the tables and the
--  chart of accounts exist). Use an EMPTY journal: if the practice
--  data (sample_data.sql) is loaded, the reference numbers clash.
--  Un-comment the two DELETE lines in STEP 0 to clear old entries.
--
--  Expected results after loading it:
--    Unadjusted trial balance total ..... $130,100.00
--    Adjusted trial balance total ....... $132,000.00
--    Net income ......................... $ 11,750.00
--    Ending capital ..................... $ 68,250.00
--    Total assets (net of acc. dep.) .... $102,150.00
--    Balance sheet difference ........... $      0.00
-- ============================================================

USE tala_accounting;

-- ---------------- STEP 0: (optional) clear old entries ----------------
-- DELETE FROM journal_entries;          -- lines are deleted too (ON DELETE CASCADE)
-- ALTER TABLE journal_entries AUTO_INCREMENT = 1;

-- ---------------- STEP 1: accounts this activity needs ----------------
-- These are not in the starting chart of accounts.
-- INSERT IGNORE skips any that you already added.
INSERT IGNORE INTO accounts (account_code, account_name, account_type, normal_balance) VALUES
(5080, 'Advertising Expense',       'Expense',   'Debit'),
(5090, 'Maintenance Expense',       'Expense',   'Debit'),
(5100, 'Repairs Expense',           'Expense',   'Debit'),
(5110, 'Professional Fees Expense', 'Expense',   'Debit'),
(5120, 'Interest Expense',          'Expense',   'Debit');

-- ---------------- STEP 2: accounting period Jan - Feb 2026 ----------------
-- So the Journal Entry page and reports show these dates.
INSERT INTO app_settings (setting_key, setting_value) VALUES ('period_from', '2026-01')
  ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
INSERT INTO app_settings (setting_key, setting_value) VALUES ('period_to', '2026-02')
  ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- Each block: 1) insert the entry  2) remember its id  3) insert its lines

-- ======================= REGULAR ENTRIES =======================

-- JE-000001  Jan 01: Dr Cash / Cr Owner's Capital  50,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-01', 'JE-000001', 'Owner invested cash to start the business', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 50000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 3010), 0.00, 50000.00);

-- JE-000002  Jan 02: Dr Prepaid Insurance / Cr Cash  2,400
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-02', 'JE-000002', 'Paid 1-year insurance policy in advance', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1310), 2400.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 2400.00);

-- JE-000003  Jan 03: Dr Prepaid Rent / Cr Cash  3,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-03', 'JE-000003', 'Paid 3 months of office rent in advance', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1320), 3000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 3000.00);

-- JE-000004  Jan 04: Dr Equipment / Cr Accounts Payable  15,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-04', 'JE-000004', 'Bought office equipment on account', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1500), 15000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 0.00, 15000.00);

-- JE-000005  Jan 05: Dr Supplies / Cr Cash  1,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-05', 'JE-000005', 'Bought office supplies for cash', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1300), 1200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 1200.00);

-- JE-000006  Jan 06: Dr Cash / Cr Service Revenue  4,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-06', 'JE-000006', 'Provided services for cash', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 4500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 4500.00);

-- JE-000007  Jan 07: Dr Accounts Receivable / Cr Service Revenue  6,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-07', 'JE-000007', 'Billed clients for completed services', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 6200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 6200.00);

-- JE-000008  Jan 08: Dr Advertising Expense / Cr Cash  1,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-08', 'JE-000008', 'Paid for online advertising campaign', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5080), 1500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 1500.00);

-- JE-000009  Jan 09: Dr Cash / Cr Unearned Revenue  2,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-09', 'JE-000009', 'Received cash advance for future services', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 2000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2040), 0.00, 2000.00);

-- JE-000010  Jan 10: Dr Accounts Payable / Cr Cash  5,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-10', 'JE-000010', 'Paid part of accounts payable', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 5000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 5000.00);

-- JE-000011  Jan 11: Dr Cash / Cr Accounts Receivable  3,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-11', 'JE-000011', 'Collected from clients on account', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 3000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 0.00, 3000.00);

-- JE-000012  Jan 12: Dr Supplies / Cr Accounts Payable  800
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-12', 'JE-000012', 'Bought office supplies on credit', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1300), 800.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 0.00, 800.00);

-- JE-000013  Jan 13: Dr Utilities Expense / Cr Cash  650
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-13', 'JE-000013', 'Paid electricity and water', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5030), 650.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 650.00);

-- JE-000014  Jan 14: Dr Salaries Expense / Cr Cash  3,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-14', 'JE-000014', 'Paid bi-weekly salaries', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5010), 3200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 3200.00);

-- JE-000015  Jan 15: Dr Owner's Drawings / Cr Cash  2,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-15', 'JE-000015', 'Owner withdrew cash for personal use', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 3020), 2000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 2000.00);

-- JE-000016  Jan 16: Dr Accounts Receivable / Cr Service Revenue  5,800
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-16', 'JE-000016', 'Billed customer for consulting services', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 5800.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 5800.00);

-- JE-000017  Jan 17: Dr Cash / Cr Notes Payable  20,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-17', 'JE-000017', 'Issued note payable for bank loan', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 20000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2100), 0.00, 20000.00);

-- JE-000018  Jan 18: Dr Vehicles / Cr Cash  18,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-18', 'JE-000018', 'Bought delivery vehicle for cash', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1520), 18000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 18000.00);

-- JE-000019  Jan 19: Dr Maintenance Expense / Cr Cash  400
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-19', 'JE-000019', 'Paid vehicle maintenance and fuel', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5090), 400.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 400.00);

-- JE-000020  Jan 20: Dr Cash / Cr Accounts Receivable  4,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-20', 'JE-000020', 'Collected from customers on account', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 4000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 0.00, 4000.00);

-- JE-000021  Jan 21: Dr Cash / Cr Service Revenue  3,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-21', 'JE-000021', 'Performed repair services for cash', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 3500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 3500.00);

-- JE-000022  Jan 23: Dr Utilities Expense / Cr Cash  600
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-23', 'JE-000022', 'Paid internet and phone subscription', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5030), 600.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 600.00);

-- JE-000023  Jan 24: Dr Accounts Payable / Cr Cash  4,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-24', 'JE-000023', 'Partial payment on equipment debt', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 4000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 4000.00);

-- JE-000024  Jan 25: Dr Professional Fees Expense / Cr Accounts Payable  1,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-25', 'JE-000024', 'Received legal services invoice', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5110), 1200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 0.00, 1200.00);

-- JE-000025  Jan 26: Dr Accounts Receivable / Cr Service Revenue  7,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-26', 'JE-000025', 'Services on account to corporate client', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 7500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 7500.00);

-- JE-000026  Jan 27: Dr Cash / Cr Accounts Receivable  5,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-27', 'JE-000026', 'Partial collection from Jan 26 billing', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 5000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 0.00, 5000.00);

-- JE-000027  Jan 28: Dr Salaries Expense / Cr Cash  3,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-28', 'JE-000027', 'Paid bi-weekly salaries', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5010), 3200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 3200.00);

-- JE-000028  Jan 29: Dr Repairs Expense / Cr Cash  500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-29', 'JE-000028', 'Paid for minor office repairs', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5100), 500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 500.00);

-- JE-000029  Jan 30: Dr Owner's Drawings / Cr Cash  1,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-30', 'JE-000029', 'Owner withdrew cash for personal use', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 3020), 1500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 1500.00);

-- JE-000030  Jan 31: Dr Furniture / Cr Accounts Payable  3,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-31', 'JE-000030', 'Bought office furniture on credit', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1510), 3500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 0.00, 3500.00);

-- JE-000031  Feb 01: Dr Rent Expense / Cr Cash  1,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-01', 'JE-000031', 'Paid warehouse storage rent', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5020), 1000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 1000.00);

-- JE-000032  Feb 02: Dr Accounts Receivable / Cr Service Revenue  4,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-02', 'JE-000032', 'Billed client for completed project', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 4200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 4200.00);

-- JE-000033  Feb 03: Dr Accounts Payable / Cr Cash  1,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-03', 'JE-000033', 'Paid legal fees invoice of Jan 25', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 1200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 1200.00);

-- JE-000034  Feb 05: Dr Cash / Cr Accounts Receivable  3,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-05', 'JE-000034', 'Collected from credit clients', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 3500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 0.00, 3500.00);

-- JE-000035  Feb 07: Dr Advertising Expense / Cr Cash  800
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-07', 'JE-000035', 'Paid for local radio ads', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5080), 800.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 800.00);

-- JE-000036  Feb 09: Dr Cash / Cr Unearned Revenue  3,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-09', 'JE-000036', 'Received retainer advance from new client', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 3000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2040), 0.00, 3000.00);

-- JE-000037  Feb 12: Dr Supplies / Cr Cash  600
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-12', 'JE-000037', 'Bought stationery and printer ink', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1300), 600.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 600.00);

-- JE-000038  Feb 14: Dr Salaries Expense / Cr Cash  3,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-14', 'JE-000038', 'Paid bi-weekly salaries', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5010), 3200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 3200.00);

-- JE-000039  Feb 16: Dr Cash / Cr Service Revenue  5,100
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-16', 'JE-000039', 'Rendered design services for cash', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 5100.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 5100.00);

-- JE-000040  Feb 18: Dr Interest Expense / Cr Cash  200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-18', 'JE-000040', 'Paid interest on bank loan', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5120), 200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 200.00);

-- JE-000041  Feb 20: Dr Cash / Cr Accounts Receivable  2,500
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-20', 'JE-000041', 'Collected from credit customers', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 2500.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1100), 0.00, 2500.00);

-- JE-000042  Feb 22: Dr Cash / Cr Owner's Capital  10,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-22', 'JE-000042', 'Owner''s additional investment', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 10000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 3010), 0.00, 10000.00);

-- JE-000043  Feb 25: Dr Accounts Payable / Cr Cash  2,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-25', 'JE-000043', 'Paid suppliers on account', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 2010), 2000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 2000.00);

-- JE-000044  Feb 28: Dr Salaries Expense / Cr Cash  3,200
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-28', 'JE-000044', 'Paid bi-weekly salaries', 'Regular');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5010), 3200.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1010), 0.00, 3200.00);

-- ============ ADJUSTING ENTRIES ============

-- AJE-000001  Jan 31: Dr Unearned Revenue / Cr Service Revenue  1,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-01-31', 'AJE-000001', 'Unearned revenue earned (client''s advance, Jan 22)', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 2040), 1000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 4010), 0.00, 1000.00);

-- AJE-000002  Feb 28: Dr Insurance Expense / Cr Prepaid Insurance  400
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-28', 'AJE-000002', 'Expired insurance - Jan and Feb', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5050), 400.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1310), 0.00, 400.00);

-- AJE-000003  Feb 28: Dr Rent Expense / Cr Prepaid Rent  2,000
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-28', 'AJE-000003', 'Expired rent - Jan and Feb', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5020), 2000.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1320), 0.00, 2000.00);

-- AJE-000004  Feb 28: Dr Supplies Expense / Cr Supplies  2,100
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-28', 'AJE-000004', 'Office supplies used', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5040), 2100.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1300), 0.00, 2100.00);

-- AJE-000005  Feb 28: Dr Depreciation Expense / Cr Accumulated Depreciation  300
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-28', 'AJE-000005', 'Depreciation on office equipment', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5060), 300.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 1590), 0.00, 300.00);

-- AJE-000006  Feb 28: Dr Salaries Expense / Cr Salaries Payable  1,600
INSERT INTO journal_entries (entry_date, reference_no, description, entry_type)
VALUES ('2026-02-28', 'AJE-000006', 'Accrued unpaid salaries', 'Adjusting');
SET @e = LAST_INSERT_ID();
INSERT INTO journal_lines (entry_id, account_id, debit, credit) VALUES
(@e, (SELECT account_id FROM accounts WHERE account_code = 5010), 1600.00, 0.00),
(@e, (SELECT account_id FROM accounts WHERE account_code = 2020), 0.00, 1600.00);