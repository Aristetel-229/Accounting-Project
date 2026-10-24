package com.accountpro.service;

import com.accountpro.db.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Database access for invoices and invoice payments. */
public final class InvoiceService {
    private InvoiceService() {}

    public static void save(String customer, String invoiceNo, Date date,
                            String description, BigDecimal total) throws SQLException {
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO invoices(invoice_no,customer_name,invoice_date,description,total_amount,amount_paid,status) "
                             + "VALUES(?,?,?,?,?,0,'UNPAID')")) {
            ps.setString(1, invoiceNo);
            ps.setString(2, customer);
            ps.setDate(3, date);
            ps.setString(4, description);
            ps.setBigDecimal(5, total);
            ps.executeUpdate();
        }
    }

    /** Records a payment in one transaction; rejects payments above the outstanding balance. */
    public static void recordPayment(String invoiceNo, BigDecimal amount) throws SQLException {
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            try {
                BigDecimal total, paid;
                try (PreparedStatement q = c.prepareStatement(
                        "SELECT total_amount,amount_paid FROM invoices WHERE invoice_no=? FOR UPDATE")) {
                    q.setString(1, invoiceNo);
                    try (ResultSet rs = q.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Invoice not found.");
                        total = rs.getBigDecimal(1);
                        paid = rs.getBigDecimal(2);
                    }
                }
                if (amount.compareTo(total.subtract(paid)) > 0) {
                    throw new IllegalArgumentException("Payment cannot exceed outstanding balance.");
                }
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO invoice_payments(invoice_no,payment_date,amount) VALUES(?,?,?)")) {
                    ins.setString(1, invoiceNo);
                    ins.setDate(2, Date.valueOf(LocalDate.now()));
                    ins.setBigDecimal(3, amount);
                    ins.executeUpdate();
                }
                try (PreparedStatement upd = c.prepareStatement(
                        "UPDATE invoices SET amount_paid=amount_paid+?, "
                                + "status=CASE WHEN amount_paid+? >= total_amount THEN 'PAID' ELSE 'PARTIAL' END "
                                + "WHERE invoice_no=?")) {
                    upd.setBigDecimal(1, amount);
                    upd.setBigDecimal(2, amount);
                    upd.setString(3, invoiceNo);
                    upd.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
