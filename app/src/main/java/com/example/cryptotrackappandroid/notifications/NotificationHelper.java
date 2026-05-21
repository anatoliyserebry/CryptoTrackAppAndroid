package com.example.cryptotrackappandroid.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.cryptotrackappandroid.R;
import com.example.cryptotrackappandroid.data.CryptoCurrency;
import com.example.cryptotrackappandroid.data.PriceAlert;
import com.example.cryptotrackappandroid.ui.Formatters;
import com.example.cryptotrackappandroid.ui.MainActivity;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "market_alerts";

    private NotificationHelper() {
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "CryptoTrack market alerts",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Significant cryptocurrency price movements and target price alerts");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void showMarketMove(Context context, CryptoCurrency currency, double movePercent) {
        if (!canPostNotifications(context)) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                currency.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = currency.getSymbol() + " " + Formatters.change(movePercent);
        String text = currency.getName() + " is now " + Formatters.price(currency.getPriceUsd());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell_24)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(currency.getId().hashCode(), builder.build());
    }

    public static void showPriceAlert(Context context, CryptoCurrency currency, PriceAlert alert) {
        if (!canPostNotifications(context)) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                ("price_alert_" + alert.getSymbol()).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String direction = alert.isTriggerAbove() ? "reached" : "fell to";
        String title = alert.getSymbol() + " target " + Formatters.price(alert.getTargetPriceUsd());
        String text = currency.getName() + " " + direction + " " + Formatters.price(currency.getPriceUsd());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell_24)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(("price_alert_" + alert.getSymbol()).hashCode(), builder.build());
    }
}
