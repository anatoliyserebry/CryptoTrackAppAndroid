package com.example.cryptotrackappandroid.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cryptotrackappandroid.R;
import com.example.cryptotrackappandroid.data.ApiCallback;
import com.example.cryptotrackappandroid.data.ApiClient;
import com.example.cryptotrackappandroid.data.ApiSource;
import com.example.cryptotrackappandroid.data.ChartRange;
import com.example.cryptotrackappandroid.data.ChartSeries;
import com.example.cryptotrackappandroid.data.CryptoCurrency;
import com.example.cryptotrackappandroid.data.PriceAlert;
import com.example.cryptotrackappandroid.data.SessionManager;
import com.example.cryptotrackappandroid.notifications.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {
    public static final String EXTRA_CRYPTO = "extra_crypto";
    private static final int REQUEST_NOTIFICATIONS = 48;

    private CryptoCurrency currency;
    private SessionManager sessionManager;
    private ApiClient apiClient;
    private ImageButton favoriteButton;
    private TextView changeText;
    private TextView chartSelectedText;
    private TextView chartMetaText;
    private TextView priceAlertStatusText;
    private TextInputEditText priceAlertInput;
    private MaterialButton removePriceAlertButton;
    private SparklineView chartView;
    private ApiSource selectedSource = ApiSource.AUTO;
    private ChartRange selectedRange = ChartRange.DAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Serializable serializable = getCryptoExtra();
        if (!(serializable instanceof CryptoCurrency)) {
            finish();
            return;
        }

        currency = (CryptoCurrency) serializable;
        sessionManager = new SessionManager(this);
        NotificationHelper.createChannel(this);
        selectedSource = sessionManager.getApiSource();
        apiClient = new ApiClient(selectedSource);
        currency.setFavorite(sessionManager.isFavorite(currency.getId()));

        favoriteButton = findViewById(R.id.detailFavoriteButton);
        changeText = findViewById(R.id.detailChangeText);
        chartSelectedText = findViewById(R.id.chartSelectedText);
        chartMetaText = findViewById(R.id.chartMetaText);
        priceAlertStatusText = findViewById(R.id.priceAlertStatusText);
        priceAlertInput = findViewById(R.id.priceAlertInput);
        removePriceAlertButton = findViewById(R.id.removePriceAlertButton);
        chartView = findViewById(R.id.detailChartView);
        chartView.setDetailed(true);
        chartView.setOnSelectionChangeListener((index, timestamp, price) ->
                chartSelectedText.setText(formatSelectedPoint(timestamp, price)));

        bindCurrency();
        setupRangeActions();
        setupPriceAlertActions();
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> navigateBack());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        loadChart(ChartRange.DAY);
    }

    private void bindCurrency() {
        boolean positive = currency.getChangePercent24h() >= 0;

        TextView titleText = findViewById(R.id.detailTitleText);
        TextView avatarText = findViewById(R.id.detailAvatarText);
        TextView nameText = findViewById(R.id.detailNameText);
        TextView symbolText = findViewById(R.id.detailSymbolText);
        TextView priceText = findViewById(R.id.detailPriceText);
        TextView marketCapText = findViewById(R.id.marketCapText);
        TextView volumeText = findViewById(R.id.volumeText);
        TextView descriptionText = findViewById(R.id.detailDescriptionText);

        titleText.setText(currency.getSymbol());
        avatarText.setText(Formatters.initials(currency.getSymbol()));
        nameText.setText(currency.getName());
        symbolText.setText(currency.getSymbol() + " / " + currency.getSource());
        priceText.setText(Formatters.price(currency.getPriceUsd()));
        changeText.setText(Formatters.change(currency.getChangePercent24h()) + " / 24h");
        updateChangeStyle(positive);
        marketCapText.setText(Formatters.compactUsd(currency.getMarketCapUsd()));
        volumeText.setText(Formatters.compactUsd(currency.getVolume24hUsd()));
        chartView.setValues(currency.getHistory(), positive);
        chartMetaText.setText("Loading market data...");
        descriptionText.setText(currency.getName() + " (" + currency.getSymbol() + ") is displayed through "
                + currency.getSource()
                + ". The chart can now be explored point by point across 1D, 7D and 30D.");
        refreshFavoriteIcon();
        renderPriceAlert();
    }

    private void setupRangeActions() {
        findViewById(R.id.rangeDayButton).setOnClickListener(v -> loadChart(ChartRange.DAY));
        findViewById(R.id.rangeWeekButton).setOnClickListener(v -> loadChart(ChartRange.WEEK));
        findViewById(R.id.rangeMonthButton).setOnClickListener(v -> loadChart(ChartRange.MONTH));
    }

    private void setupPriceAlertActions() {
        MaterialButton savePriceAlertButton = findViewById(R.id.savePriceAlertButton);
        savePriceAlertButton.setOnClickListener(v -> savePriceAlert());
        removePriceAlertButton.setOnClickListener(v -> removePriceAlert());
    }

    private void loadChart(ChartRange range) {
        selectedRange = range;
        chartMetaText.setText("Loading " + range.getKey() + " from " + sourceLabelForLoading() + "...");
        apiClient.fetchChart(sessionManager.getToken(), currency, range, selectedSource, new ApiCallback<ChartSeries>() {
            @Override
            public void onSuccess(ChartSeries result) {
                bindChart(result);
            }

            @Override
            public void onError(Exception error) {
                chartMetaText.setText("Unable to load the " + selectedRange.getKey() + " chart");
            }
        });
    }

    private void bindChart(ChartSeries chartSeries) {
        boolean positive = chartSeries.isPositive();
        chartView.setSeries(chartSeries.getTimestamps(), chartSeries.getPrices(), positive);
        changeText.setText(Formatters.change(chartSeries.getChangePercent()) + " / " + chartSeries.getRange().getKey());
        updateChangeStyle(positive);

        String sourceText = chartSeries.isEstimated()
                ? "Local estimate"
                : chartSeries.getSourceLabel();
        chartMetaText.setText(sourceText
                + " - " + chartSeries.getPrices().length + " points"
                + " - min " + Formatters.price(chartSeries.getMinPrice())
                + " / max " + Formatters.price(chartSeries.getMaxPrice()));
    }

    private void updateChangeStyle(boolean positive) {
        changeText.setTextColor(ContextCompat.getColor(this, positive ? R.color.positive : R.color.negative));
        changeText.setBackgroundResource(positive ? R.drawable.bg_change_positive : R.drawable.bg_change_negative);
    }

    private String formatSelectedPoint(long timestamp, double price) {
        String pattern = selectedRange == ChartRange.DAY ? "HH:mm" : "dd MMM HH:mm";
        String formattedTime = new SimpleDateFormat(pattern, Locale.US).format(new Date(timestamp));
        return Formatters.price(price) + " - " + formattedTime;
    }

    private String sourceLabelForLoading() {
        if (selectedSource == ApiSource.AUTO) {
            return "the best available API";
        }
        return selectedSource.getDisplayName();
    }

    @SuppressWarnings("deprecation")
    private Serializable getCryptoExtra() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getIntent().getSerializableExtra(EXTRA_CRYPTO, CryptoCurrency.class);
        }
        return getIntent().getSerializableExtra(EXTRA_CRYPTO);
    }

    private void toggleFavorite() {
        boolean favorite = !currency.isFavorite();
        currency.setFavorite(favorite);
        sessionManager.setFavorite(currency.getId(), favorite);
        refreshFavoriteIcon();
        apiClient.setFavorite(sessionManager.getToken(), currency.getId(), favorite, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception error) {
            }
        });
    }

    private void refreshFavoriteIcon() {
        favoriteButton.setImageResource(currency.isFavorite() ? R.drawable.ic_star_24 : R.drawable.ic_star_border_24);
        favoriteButton.setColorFilter(ContextCompat.getColor(
                this,
                currency.isFavorite() ? R.color.brand_secondary : R.color.text_inverse
        ));
    }

    private void savePriceAlert() {
        Double targetPrice = parsePriceAlertTarget();
        if (targetPrice == null || targetPrice <= 0.0) {
            Snackbar.make(priceAlertInput, "Enter a positive target price", Snackbar.LENGTH_SHORT).show();
            return;
        }

        boolean triggerAbove = currency.getPriceUsd() <= 0.0 || targetPrice >= currency.getPriceUsd();
        sessionManager.setPriceAlert(currency.getSymbol(), targetPrice, triggerAbove);
        renderPriceAlert();
        enableNotificationsForAlerts();
        Snackbar.make(priceAlertInput, "Price alert saved", Snackbar.LENGTH_SHORT).show();
    }

    private void removePriceAlert() {
        sessionManager.removePriceAlert(currency.getSymbol());
        priceAlertInput.setText("");
        renderPriceAlert();
        Snackbar.make(priceAlertInput, "Price alert removed", Snackbar.LENGTH_SHORT).show();
    }

    private void renderPriceAlert() {
        PriceAlert alert = sessionManager.getPriceAlert(currency.getSymbol());
        String currentPrice = "Current price: " + Formatters.price(currency.getPriceUsd()) + ". ";

        if (alert == null) {
            priceAlertStatusText.setText(currentPrice + "No target alert set.");
            removePriceAlertButton.setEnabled(false);
            return;
        }

        priceAlertInput.setText(formatEditablePrice(alert.getTargetPriceUsd()));
        if (priceAlertInput.getText() != null) {
            priceAlertInput.setSelection(priceAlertInput.getText().length());
        }

        String direction = alert.isTriggerAbove() ? "rises to" : "falls to";
        String status = alert.isTriggered()
                ? "Target reached. Save a new price to reactivate it."
                : "Notify when " + alert.getSymbol() + " " + direction + " " + Formatters.price(alert.getTargetPriceUsd()) + ".";
        priceAlertStatusText.setText(currentPrice + status);
        removePriceAlertButton.setEnabled(true);
    }

    private void enableNotificationsForAlerts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        sessionManager.setNotificationsEnabled(true);
    }

    private Double parsePriceAlertTarget() {
        if (priceAlertInput == null || priceAlertInput.getText() == null) {
            return null;
        }
        String raw = priceAlertInput.getText().toString().trim().replace(" ", "").replace(",", ".");
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formatEditablePrice(double price) {
        String formatted = String.format(Locale.US, "%.8f", price);
        return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        sessionManager.setNotificationsEnabled(granted);
        Snackbar.make(
                priceAlertInput,
                granted ? "Notifications enabled" : "Notification permission denied",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
