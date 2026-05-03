package com.example.cryptotrackappandroid.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptotrackappandroid.R;
import com.example.cryptotrackappandroid.data.ApiCallback;
import com.example.cryptotrackappandroid.data.ApiClient;
import com.example.cryptotrackappandroid.data.CryptoCurrency;
import com.example.cryptotrackappandroid.data.SessionManager;
import com.example.cryptotrackappandroid.notifications.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CryptoAdapter.Listener {
    private static final int REQUEST_NOTIFICATIONS = 31;
    private static final double ALERT_PERCENT = 5.0;

    private final List<CryptoCurrency> allCurrencies = new ArrayList<>();
    private final Map<String, Double> lastPrices = new HashMap<>();

    private SessionManager sessionManager;
    private ApiClient apiClient;
    private CryptoAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView updatedAtText;
    private TextView marketCountText;
    private TextView favoriteCountText;
    private TextInputEditText searchInput;
    private SwitchMaterial notificationSwitch;
    private boolean showingFavorites = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        NotificationHelper.createChannel(this);
        apiClient = new ApiClient();
        setContentView(R.layout.activity_main);
        bindViews();
        setupList();
        setupActions();
        loadCurrencies(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!allCurrencies.isEmpty()) {
            applyFavorites(allCurrencies);
            filterAndRender();
        }
    }

    private void bindViews() {
        progressBar = findViewById(R.id.mainProgress);
        emptyText = findViewById(R.id.emptyText);
        updatedAtText = findViewById(R.id.updatedAtText);
        marketCountText = findViewById(R.id.marketCountText);
        favoriteCountText = findViewById(R.id.favoriteCountText);
        searchInput = findViewById(R.id.searchInput);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        notificationSwitch.setChecked(sessionManager.areNotificationsEnabled());
    }

    private void setupList() {
        RecyclerView recyclerView = findViewById(R.id.cryptoRecycler);
        adapter = new CryptoAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupActions() {
        ImageButton refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> loadCurrencies(false));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            showingFavorites = item.getItemId() == R.id.nav_favorites;
            filterAndRender();
            return true;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndRender();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
                return;
            }
            sessionManager.setNotificationsEnabled(isChecked);
            if (isChecked) {
                Snackbar.make(notificationSwitch, "Уведомления включены", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrencies(boolean showProgress) {
        if (showProgress) {
            progressBar.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
        }

        apiClient.fetchCurrencies(sessionManager.getToken(), new ApiCallback<List<CryptoCurrency>>() {
            @Override
            public void onSuccess(List<CryptoCurrency> result) {
                progressBar.setVisibility(View.GONE);
                applyFavorites(result);
                maybeNotifyMarketMoves(result);
                allCurrencies.clear();
                allCurrencies.addAll(result);
                updatedAtText.setText(Formatters.updatedNow());
                filterAndRender();
            }

            @Override
            public void onError(Exception error) {
                progressBar.setVisibility(View.GONE);
                emptyText.setText(R.string.empty_market);
                emptyText.setVisibility(View.VISIBLE);
                Snackbar.make(emptyText, "Не удалось получить данные", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void applyFavorites(List<CryptoCurrency> currencies) {
        for (CryptoCurrency currency : currencies) {
            if (currency.isFavorite()) {
                sessionManager.setFavorite(currency.getId(), true);
            }
            currency.setFavorite(sessionManager.isFavorite(currency.getId()));
        }
    }

    private void maybeNotifyMarketMoves(List<CryptoCurrency> currencies) {
        boolean enabled = sessionManager.areNotificationsEnabled() && NotificationHelper.canPostNotifications(this);
        for (CryptoCurrency currency : currencies) {
            Double oldPrice = lastPrices.get(currency.getId());
            double newPrice = currency.getPriceUsd();
            if (enabled && oldPrice != null && oldPrice > 0 && newPrice > 0) {
                double movePercent = ((newPrice - oldPrice) / oldPrice) * 100.0;
                if (Math.abs(movePercent) >= ALERT_PERCENT) {
                    NotificationHelper.showMarketMove(this, currency, movePercent);
                }
            }
            lastPrices.put(currency.getId(), newPrice);
        }
    }

    private void filterAndRender() {
        String query = searchInput.getText() != null ? searchInput.getText().toString().trim().toLowerCase(Locale.US) : "";
        List<CryptoCurrency> visible = new ArrayList<>();
        for (CryptoCurrency currency : allCurrencies) {
            if (showingFavorites && !currency.isFavorite()) {
                continue;
            }
            boolean matches = query.isEmpty()
                    || currency.getName().toLowerCase(Locale.US).contains(query)
                    || currency.getSymbol().toLowerCase(Locale.US).contains(query);
            if (matches) {
                visible.add(currency);
            }
        }

        adapter.submitList(visible);
        emptyText.setText(showingFavorites ? R.string.empty_favorites : R.string.empty_market);
        emptyText.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        updateStats();
    }

    private void updateStats() {
        int favoriteCount = 0;
        for (CryptoCurrency currency : allCurrencies) {
            if (currency.isFavorite()) {
                favoriteCount++;
            }
        }
        marketCountText.setText(allCurrencies.size() + " активов");
        favoriteCountText.setText(favoriteCount + " в избранном");
    }

    @Override
    public void onCryptoClick(CryptoCurrency currency) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_CRYPTO, currency);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(CryptoCurrency currency) {
        boolean favorite = !currency.isFavorite();
        currency.setFavorite(favorite);
        sessionManager.setFavorite(currency.getId(), favorite);
        apiClient.setFavorite(sessionManager.getToken(), currency.getId(), favorite, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception error) {
            }
        });
        filterAndRender();
        Snackbar.make(emptyText, favorite ? "Добавлено в избранное" : "Удалено из избранного", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        sessionManager.setNotificationsEnabled(granted);
        notificationSwitch.setChecked(granted);
        Snackbar.make(notificationSwitch, granted ? "Уведомления включены" : "Разрешение на уведомления не выдано", Snackbar.LENGTH_LONG).show();
    }
}
