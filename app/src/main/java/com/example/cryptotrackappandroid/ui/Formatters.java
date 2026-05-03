package com.example.cryptotrackappandroid.ui;

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

    public static String updatedNow() {
        return "Updated at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }
}
