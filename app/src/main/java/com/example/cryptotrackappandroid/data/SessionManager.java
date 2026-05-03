package com.example.cryptotrackappandroid.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class SessionManager {
    private static final String PREFS = "crypto_track_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_API_SOURCE = "api_source";

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
}
