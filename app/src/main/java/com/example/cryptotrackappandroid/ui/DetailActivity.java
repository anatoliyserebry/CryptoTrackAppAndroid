package com.example.cryptotrackappandroid.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cryptotrackappandroid.R;
import com.example.cryptotrackappandroid.data.ApiCallback;
import com.example.cryptotrackappandroid.data.ApiClient;
import com.example.cryptotrackappandroid.data.CryptoCurrency;
import com.example.cryptotrackappandroid.data.SessionManager;

import java.io.Serializable;

public class DetailActivity extends AppCompatActivity {
    public static final String EXTRA_CRYPTO = "extra_crypto";

    private CryptoCurrency currency;
    private SessionManager sessionManager;
    private ApiClient apiClient;
    private ImageButton favoriteButton;
    private TextView changeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Serializable serializable = getIntent().getSerializableExtra(EXTRA_CRYPTO);
        if (!(serializable instanceof CryptoCurrency)) {
            finish();
            return;
        }

        currency = (CryptoCurrency) serializable;
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient();
        currency.setFavorite(sessionManager.isFavorite(currency.getId()));

        favoriteButton = findViewById(R.id.detailFavoriteButton);
        changeText = findViewById(R.id.detailChangeText);

        bindCurrency();
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
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
        SparklineView chartView = findViewById(R.id.detailChartView);

        titleText.setText(currency.getSymbol());
        avatarText.setText(Formatters.initials(currency.getSymbol()));
        nameText.setText(currency.getName());
        symbolText.setText(currency.getSymbol() + " / " + currency.getSource());
        priceText.setText(Formatters.price(currency.getPriceUsd()));
        changeText.setText(Formatters.change(currency.getChangePercent24h()) + " / 24h");
        changeText.setTextColor(ContextCompat.getColor(this, positive ? R.color.positive : R.color.negative));
        changeText.setBackgroundResource(positive ? R.drawable.bg_change_positive : R.drawable.bg_change_negative);
        marketCapText.setText(Formatters.compactUsd(currency.getMarketCapUsd()));
        volumeText.setText(Formatters.compactUsd(currency.getVolume24hUsd()));
        chartView.setValues(currency.getHistory(), positive);
        descriptionText.setText(currency.getName() + " (" + currency.getSymbol() + ") from "
                + currency.getSource()
                + ". This screen shows live price, 24h movement, market capitalization and trading volume.");
        refreshFavoriteIcon();
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
    }
}
