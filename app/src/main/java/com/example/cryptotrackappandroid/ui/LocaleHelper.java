package com.example.cryptotrackappandroid.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public final class LocaleHelper {
    private static final String PREFS = "crypto_track_session";
    private static final String KEY_LANGUAGE = "language_code";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String[] LANGUAGE_CODES = {"en", "fr", "ru"};

    private LocaleHelper() {
    }

    public static Context wrap(Context context) {
        String languageCode = getLanguageCode(context);
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    public static String getLanguageCode(Context context) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String languageCode = preferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
        return isSupported(languageCode) ? languageCode : DEFAULT_LANGUAGE;
    }

    public static void setLanguageCode(Context context, String languageCode) {
        String cleanCode = isSupported(languageCode) ? languageCode : DEFAULT_LANGUAGE;
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_LANGUAGE, cleanCode).apply();
    }

    private static boolean isSupported(String languageCode) {
        if (languageCode == null) {
            return false;
        }
        for (String supportedCode : LANGUAGE_CODES) {
            if (supportedCode.equals(languageCode)) {
                return true;
            }
        }
        return false;
    }
}
