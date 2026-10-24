package com.accountpro.service;

import com.accountpro.db.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Performs a full, transactional reset to the starter AccountPro data. */
public final class ResetDataService {
    private ResetDataService() {}

    private static final String INSERT_ACCOUNT = """
        INSERT INTO accounts (account_code, account_name, account_type, normal_balance, description)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String[][] DEFAULT_ACCOUNTS = {
        {"1010", "Cash on Hand", "ASSET", "DEBIT", "Cash held by the business"},
        {"1020", "Cash in Bank", "ASSET", "DEBIT", "Business bank account"},
        {"1100", "Accounts Receivable", "ASSET", "DEBIT", "Amounts owed by customers"},
        {"1200", "Inventory", "ASSET", "DEBIT", "Inventory held for sale"},
        {"1500", "Office Equipment", "ASSET", "DEBIT", "Office equipment and computers"},
        {"1590", "Accumulated Depreciation", "ASSET", "CREDIT", "Accumulated depreciation contra-asset"},
        {"2010", "Accounts Payable", "LIABILITY", "CREDIT", "Amounts owed to suppliers"},
        {"2020", "Salaries Payable", "LIABILITY", "CREDIT", "Unpaid salary obligations"},
        {"2030", "Taxes Payable", "LIABILITY", "CREDIT", "Taxes payable"},
        {"2100", "Loans Payable", "LIABILITY", "CREDIT", "Outstanding loans"},
        {"3010", "Owner's Capital", "EQUITY", "CREDIT", "Owner capital contributions"},
        {"3020", "Owner's Drawings", "EQUITY", "DEBIT", "Owner withdrawals"},
        {"4010", "Sales Revenue", "REVENUE", "CREDIT", "Revenue from sales"},
        {"4020", "Service Revenue", "REVENUE", "CREDIT", "Revenue from services"},
        {"4090", "Other Income", "REVENUE", "CREDIT", "Other business income"},
        {"5010", "Cost of Goods Sold", "EXPENSE", "DEBIT", "Cost of goods sold"},
        {"6010", "Rent Expense", "EXPENSE", "DEBIT", "Rent and lease expenses"},
        {"6020", "Salaries Expense", "EXPENSE", "DEBIT", "Employee salaries"},
        {"6030", "Utilities Expense", "EXPENSE", "DEBIT", "Electricity, water, and internet"},
        {"6040", "Supplies Expense", "EXPENSE", "DEBIT", "Office and operating supplies"},
        {"6050", "Transportation Expense", "EXPENSE", "DEBIT", "Transportation expenses"}
    };

    /** Deletes operational records and custom accounts, then restores the starter accounts. */
    public static void resetAllData() throws SQLException {
        try (Connection connection = DB.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                // Delete child records before parent records to satisfy foreign keys.
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM invoice_payments");
                    statement.executeUpdate("DELETE FROM invoices");
                    statement.executeUpdate("DELETE FROM journal_entry_lines");
                    statement.executeUpdate("DELETE FROM journal_entries");
                    statement.executeUpdate("DELETE FROM accounts");
                }

                try (PreparedStatement statement = connection.prepareStatement(INSERT_ACCOUNT)) {
                    for (String[] account : DEFAULT_ACCOUNTS) {
                        for (int i = 0; i < account.length; i++) {
                            statement.setString(i + 1, account[i]);
                        }
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }
}
