package com.example.ledger.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    private static final BigDecimal MAX = new BigDecimal("99999999.99");
    private static final String MINUS = "\u2212";

    private Money() {
    }

    public static String format(long cents) {
        boolean negative = cents < 0;
        long abs = Math.abs(cents);
        long yuan = abs / 100;
        long fen = abs % 100;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        String text = "¥" + format.format(yuan) + "." + String.format(Locale.US, "%02d", fen);
        return negative ? MINUS + text : text;
    }

    public static String formatChange(int type, long cents) {
        String sign = type == Transaction.INCOME ? "+" : MINUS;
        return sign + format(Math.abs(cents));
    }

    public static String plain(long cents) {
        long abs = Math.abs(cents);
        return (abs / 100) + "." + String.format(Locale.US, "%02d", abs % 100);
    }

    public static long parseYuan(String raw) {
        if (raw == null) {
            return -1L;
        }
        String text = raw.trim()
                .replace("¥", "")
                .replace(",", "")
                .replace("，", "")
                .replace(" ", "");
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.isEmpty()) {
            return -1L;
        }
        try {
            BigDecimal value = new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
            if (value.signum() <= 0 || value.compareTo(MAX) > 0) {
                return -1L;
            }
            return value.movePointRight(2).longValueExact();
        } catch (RuntimeException error) {
            return -1L;
        }
    }
}
