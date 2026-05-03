package com.example.cryptotrackappandroid.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cryptotrackappandroid.R;
import com.example.cryptotrackappandroid.data.ApiCallback;
import com.example.cryptotrackappandroid.data.ApiClient;
import com.example.cryptotrackappandroid.data.ApiSource;
import com.example.cryptotrackappandroid.data.ChartRange;
import com.example.cryptotrackappandroid.data.ChartSeries;
import com.example.cryptotrackappandroid.data.CryptoCurrency;
import com.example.cryptotrackappandroid.data.SessionManager;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {
    public static final String EXTRA_CRYPTO = "extra_crypto";

    private CryptoCurrency currency;
    private SessionManager sessionManager;
    private ApiClient apiClient;
    private ImageButton favoriteButton;
    private TextView changeText;
    private TextView chartSelectedText;
    private TextView chartMetaText;
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
        selectedSource = sessionManager.getApiSource();
        apiClient = new ApiClient(selectedSource);
        currency.setFavorite(sessionManager.isFavorite(currency.getId()));

        favoriteButton = findViewById(R.id.detailFavoriteButton);
        changeText = findViewById(R.id.detailChangeText);
        chartSelectedText = findViewById(R.id.chartSelectedText);
        chartMetaText = findViewById(R.id.chartMetaText);
        chartView = findViewById(R.id.detailChartView);
        chartView.setDetailed(true);
        chartView.setOnSelectionChangeListener((index, timestamp, price) ->
                chartSelectedText.setText(formatSelectedPoint(timestamp, price)));

        bindCurrency();
        setupRangeActions();
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
    }

    private void setupRangeActions() {
        findViewById(R.id.rangeDayButton).setOnClickListener(v -> loadChart(ChartRange.DAY));
        findViewById(R.id.rangeWeekButton).setOnClickListener(v -> loadChart(ChartRange.WEEK));
        findViewById(R.id.rangeMonthButton).setOnClickListener(v -> loadChart(ChartRange.MONTH));
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

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
