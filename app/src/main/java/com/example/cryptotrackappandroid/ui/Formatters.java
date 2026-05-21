package com.example.cryptotrackappandroid.ui;

import android.content.Context;

import com.example.cryptotrackappandroid.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Formatters {
    private Formatters() {
    }

    public static String price(double value) {
        if (value >= 1000) {
            return String.format(Locale.US, "$%,.2f", value);
        }
        if (value >= 1) {
            return String.format(Locale.US, "$%.2f", value);
        }
        if (value >= 0.01) {
            return String.format(Locale.US, "$%.4f", value);
        }
        return String.format(Locale.US, "$%.8f", value);
    }

    public static String compactUsd(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000_000.0) {
            return String.format(Locale.US, "$%.2fT", value / 1_000_000_000_000.0);
        }
        if (abs >= 1_000_000_000.0) {
            return String.format(Locale.US, "$%.2fB", value / 1_000_000_000.0);
        }
        if (abs >= 1_000_000.0) {
            return String.format(Locale.US, "$%.2fM", value / 1_000_000.0);
        }
        if (abs >= 1_000.0) {
            return String.format(Locale.US, "$%.2fK", value / 1_000.0);
        }
        return price(value);
    }

    public static String fiat(double value, String code) {
        String cleanCode = code == null || code.trim().isEmpty() ? "USD" : code.trim().toUpperCase(Locale.US);
        double abs = Math.abs(value);
        if ("USD".equals(cleanCode)) {
            return price(value);
        }
        if ("JPY".equals(cleanCode)) {
            return String.format(Locale.US, "%s %,.0f", cleanCode, value);
        }
        if (abs >= 1.0) {
            return String.format(Locale.US, "%s %,.2f", cleanCode, value);
        }
        if (abs >= 0.01) {
            return String.format(Locale.US, "%s %.4f", cleanCode, value);
        }
        return String.format(Locale.US, "%s %.8f", cleanCode, value);
    }

    public static String cryptoAmount(double value, String symbol) {
        String cleanSymbol = symbol == null || symbol.trim().isEmpty() ? "CRYPTO" : symbol.trim().toUpperCase(Locale.US);
        double abs = Math.abs(value);
        if (abs >= 1.0) {
            return String.format(Locale.US, "%,.6f %s", value, cleanSymbol);
        }
        return String.format(Locale.US, "%.8f %s", value, cleanSymbol);
    }

    public static String quantity(double value) {
        double abs = Math.abs(value);
        if (abs >= 1.0) {
            return String.format(Locale.US, "%,.6f", value);
        }
        return String.format(Locale.US, "%.8f", value);
    }

    public static String change(double value) {
        return String.format(Locale.US, "%s%.2f%%", value >= 0 ? "+" : "", value);
    }

    public static String initials(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return "CT";
        }
        String clean = symbol.trim().toUpperCase(Locale.US);
        return clean.length() <= 3 ? clean : clean.substring(0, 3);
    }

    public static String updatedNow(Context context) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        return context.getString(R.string.updated_at_time, time);
    }
}
