package com.example.cryptotrackappandroid.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SessionManager {
    private static final String PREFS = "crypto_track_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_API_SOURCE = "api_source";
    private static final String KEY_PORTFOLIO = "portfolio_holdings";
    private static final String KEY_PRICE_ALERTS = "price_alerts";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public void saveToken(String token) {
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public Set<String> getFavorites() {
        return new HashSet<>(preferences.getStringSet(KEY_FAVORITES, new HashSet<>()));
    }

    public boolean isFavorite(String currencyId) {
        return getFavorites().contains(currencyId);
    }

    public void setFavorite(String currencyId, boolean favorite) {
        Set<String> favorites = getFavorites();
        if (favorite) {
            favorites.add(currencyId);
        } else {
            favorites.remove(currencyId);
        }
        preferences.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public boolean areNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS, false);
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public ApiSource getApiSource() {
        return ApiSource.fromKey(preferences.getString(KEY_API_SOURCE, ApiSource.AUTO.getKey()));
    }

    public void setApiSource(ApiSource source) {
        preferences.edit().putString(KEY_API_SOURCE, source != null ? source.getKey() : ApiSource.AUTO.getKey()).apply();
    }

    public Map<String, Double> getPortfolioHoldings() {
        Set<String> entries = preferences.getStringSet(KEY_PORTFOLIO, new HashSet<>());
        Map<String, Double> holdings = new LinkedHashMap<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String symbol = normalizeSymbol(parts[0]);
            if (symbol.isEmpty()) {
                continue;
            }
            try {
                double amount = Double.parseDouble(parts[1]);
                if (amount > 0.0) {
                    holdings.put(symbol, amount);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return holdings;
    }

    public void setPortfolioHolding(String symbol, double amount) {
        Map<String, Double> holdings = getPortfolioHoldings();
        String cleanSymbol = normalizeSymbol(symbol);
        if (cleanSymbol.isEmpty()) {
            return;
        }
        if (amount > 0.0) {
            holdings.put(cleanSymbol, amount);
        } else {
            holdings.remove(cleanSymbol);
        }
        savePortfolioHoldings(holdings);
    }

    public void removePortfolioHolding(String symbol) {
        setPortfolioHolding(symbol, 0.0);
    }

    public Map<String, PriceAlert> getPriceAlerts() {
        Set<String> entries = preferences.getStringSet(KEY_PRICE_ALERTS, new HashSet<>());
        Map<String, PriceAlert> alerts = new LinkedHashMap<>();
        for (String entry : entries) {
            PriceAlert alert = parsePriceAlert(entry);
            if (alert != null) {
                alerts.put(alert.getSymbol(), alert);
            }
        }
        return alerts;
    }

    public PriceAlert getPriceAlert(String symbol) {
        return getPriceAlerts().get(normalizeSymbol(symbol));
    }

    public void setPriceAlert(String symbol, double targetPriceUsd, boolean triggerAbove) {
        String cleanSymbol = normalizeSymbol(symbol);
        if (cleanSymbol.isEmpty()) {
            return;
        }
        if (targetPriceUsd <= 0.0) {
            removePriceAlert(cleanSymbol);
            return;
        }
        Map<String, PriceAlert> alerts = getPriceAlerts();
        alerts.put(cleanSymbol, new PriceAlert(cleanSymbol, targetPriceUsd, triggerAbove, false));
        savePriceAlerts(alerts);
    }

    public void setPriceAlertTriggered(String symbol, boolean triggered) {
        String cleanSymbol = normalizeSymbol(symbol);
        Map<String, PriceAlert> alerts = getPriceAlerts();
        PriceAlert alert = alerts.get(cleanSymbol);
        if (alert == null) {
            return;
        }
        alerts.put(cleanSymbol, new PriceAlert(
                cleanSymbol,
                alert.getTargetPriceUsd(),
                alert.isTriggerAbove(),
                triggered
        ));
        savePriceAlerts(alerts);
    }

    public void removePriceAlert(String symbol) {
        Map<String, PriceAlert> alerts = getPriceAlerts();
        alerts.remove(normalizeSymbol(symbol));
        savePriceAlerts(alerts);
    }

    private void savePortfolioHoldings(Map<String, Double> holdings) {
        Set<String> entries = new HashSet<>();
        for (Map.Entry<String, Double> entry : holdings.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0.0) {
                entries.add(normalizeSymbol(entry.getKey()) + ":" + entry.getValue());
            }
        }
        preferences.edit().putStringSet(KEY_PORTFOLIO, entries).apply();
    }

    private PriceAlert parsePriceAlert(String entry) {
        if (entry == null) {
            return null;
        }
        String[] parts = entry.split(":", 4);
        if (parts.length != 4) {
            return null;
        }
        String symbol = normalizeSymbol(parts[0]);
        if (symbol.isEmpty()) {
            return null;
        }
        try {
            double targetPriceUsd = Double.parseDouble(parts[1]);
            if (targetPriceUsd <= 0.0) {
                return null;
            }
            boolean triggerAbove = Boolean.parseBoolean(parts[2]);
            boolean triggered = Boolean.parseBoolean(parts[3]);
            return new PriceAlert(symbol, targetPriceUsd, triggerAbove, triggered);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void savePriceAlerts(Map<String, PriceAlert> alerts) {
        Set<String> entries = new HashSet<>();
        for (PriceAlert alert : alerts.values()) {
            if (alert == null || alert.getSymbol() == null || alert.getTargetPriceUsd() <= 0.0) {
                continue;
            }
            entries.add(normalizeSymbol(alert.getSymbol())
                    + ":" + alert.getTargetPriceUsd()
                    + ":" + alert.isTriggerAbove()
                    + ":" + alert.isTriggered());
        }
        preferences.edit().putStringSet(KEY_PRICE_ALERTS, entries).apply();
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.US);
    }
}
