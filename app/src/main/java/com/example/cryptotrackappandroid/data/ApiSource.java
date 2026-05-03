package com.example.cryptotrackappandroid.data;

import java.util.Locale;

public enum ApiSource {
    AUTO("auto", "Auto"),
    FASTAPI("fastapi", "FastAPI"),
    COINGECKO("coingecko", "CoinGecko"),
    BYBIT("bybit", "Bybit"),
    MEXC("mexc", "MEXC"),
    BINANCE("binance", "Binance"),
    KRAKEN("kraken", "Kraken");

    private final String key;
    private final String displayName;

    ApiSource(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String[] displayNames() {
        ApiSource[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }

    public static ApiSource fromKey(String key) {
        if (key == null) {
            return AUTO;
        }
        for (ApiSource source : values()) {
            if (source.key.equalsIgnoreCase(key)) {
                return source;
            }
        }
        return AUTO;
    }

    public static ApiSource fromDisplayName(String displayName) {
        if (displayName == null) {
            return AUTO;
        }
        for (ApiSource source : values()) {
            if (source.displayName.equalsIgnoreCase(displayName)) {
                return source;
            }
        }
        return AUTO;
    }

    public static ApiSource fromCurrencySource(String source) {
        if (source == null) {
            return AUTO;
        }
        String normalized = source.trim().toLowerCase(Locale.US);
        if (normalized.contains("coingecko")) {
            return COINGECKO;
        }
        if (normalized.contains("bybit")) {
            return BYBIT;
        }
        if (normalized.contains("mexc")) {
            return MEXC;
        }
        if (normalized.contains("binance")) {
            return BINANCE;
        }
        if (normalized.contains("kraken")) {
            return KRAKEN;
        }
        if (normalized.contains("fastapi")) {
            return FASTAPI;
        }
        return AUTO;
    }
}
