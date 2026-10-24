package com.accountpro.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formatting and parsing of peso amounts. */
public final class Money {
    private Money() {}

    public static String currency(BigDecimal amount) {
        return "₱" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Parses an amount with at most two decimals; throws if it has more. */
    public static BigDecimal parse(String text) {
        return new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY);
    }
}
