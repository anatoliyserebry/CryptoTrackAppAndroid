package com.example.cryptotrackappandroid.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.cryptotrackappandroid.data.ApiSource;
import com.example.cryptotrackappandroid.data.CryptoCurrency;
import com.example.cryptotrackappandroid.data.SessionManager;
import com.example.cryptotrackappandroid.notifications.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements CryptoAdapter.Listener {
    private enum Screen {
        MARKET,
        FAVORITES,
        PORTFOLIO,
        CONVERTER
    }

    private static final int REQUEST_NOTIFICATIONS = 31;
    private static final double ALERT_PERCENT = 5.0;
    private static final String[] FIAT_CODES = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"};

    private final List<CryptoCurrency> allCurrencies = new ArrayList<>();
    private final List<CryptoCurrency> portfolioSpinnerCurrencies = new ArrayList<>();
    private final List<String> converterOptionCodes = new ArrayList<>();
    private final Map<String, CryptoCurrency> currenciesBySymbol = new HashMap<>();
    private final Map<String, Double> lastPrices = new HashMap<>();
    private final Map<String, Double> fiatRates = new LinkedHashMap<>();

    private SessionManager sessionManager;
    private ApiClient apiClient;
    private ApiSource currentApiSource = ApiSource.AUTO;
    private CryptoAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView updatedAtText;
    private TextView marketCountText;
    private TextView favoriteCountText;
    private TextInputEditText searchInput;
    private SwitchMaterial notificationSwitch;
    private Spinner apiSourceSpinner;
    private View marketControls;
    private View marketContent;
    private View portfolioContent;
    private View converterContent;
    private Spinner portfolioCryptoSpinner;
    private TextInputEditText portfolioAmountInput;
    private TextView portfolioTotalText;
    private TextView portfolioChangeText;
    private TextView portfolioPositionCountText;
    private TextView portfolioEmptyText;
    private LinearLayout portfolioHoldingList;
    private Spinner converterFromSpinner;
    private Spinner converterToSpinner;
    private TextInputEditText converterAmountInput;
    private TextView converterResultText;
    private TextView converterRateText;
    private boolean showingFavorites = false;
    private boolean refreshingConverterOptions = false;
    private boolean fiatRatesFromNetwork = false;
    private Screen currentScreen = Screen.MARKET;
    private String lastMarketStatus;

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
        currentApiSource = sessionManager.getApiSource();
        apiClient = new ApiClient(currentApiSource);
        initializeFiatRates();
        setContentView(R.layout.activity_main);
        lastMarketStatus = getString(R.string.updating_data);
        bindViews();
        setupList();
        setupActions();
        refreshPortfolioCryptoSpinner();
        refreshConverterOptions();
        loadCurrencies(true);
        loadFiatRates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!allCurrencies.isEmpty()) {
            applyFavorites(allCurrencies);
            indexCurrencies();
            refreshPortfolioCryptoSpinner();
            refreshConverterOptions();
            renderCurrentScreen();
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
        apiSourceSpinner = findViewById(R.id.apiSourceSpinner);
        marketControls = findViewById(R.id.marketControls);
        marketContent = findViewById(R.id.marketContent);
        portfolioContent = findViewById(R.id.portfolioContent);
        converterContent = findViewById(R.id.converterContent);
        portfolioCryptoSpinner = findViewById(R.id.portfolioCryptoSpinner);
        portfolioAmountInput = findViewById(R.id.portfolioAmountInput);
        portfolioTotalText = findViewById(R.id.portfolioTotalText);
        portfolioChangeText = findViewById(R.id.portfolioChangeText);
        portfolioPositionCountText = findViewById(R.id.portfolioPositionCountText);
        portfolioEmptyText = findViewById(R.id.portfolioEmptyText);
        portfolioHoldingList = findViewById(R.id.portfolioHoldingList);
        converterFromSpinner = findViewById(R.id.converterFromSpinner);
        converterToSpinner = findViewById(R.id.converterToSpinner);
        converterAmountInput = findViewById(R.id.converterAmountInput);
        converterResultText = findViewById(R.id.converterResultText);
        converterRateText = findViewById(R.id.converterRateText);
        notificationSwitch.setChecked(sessionManager.areNotificationsEnabled());
    }

    private void setupList() {
        RecyclerView recyclerView = findViewById(R.id.cryptoRecycler);
        adapter = new CryptoAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupActions() {
        setupApiSourceSpinner();

        ImageButton refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> {
            loadCurrencies(false);
            if (currentScreen == Screen.CONVERTER) {
                loadFiatRates();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_favorites) {
                showScreen(Screen.FAVORITES);
            } else if (itemId == R.id.nav_portfolio) {
                showScreen(Screen.PORTFOLIO);
            } else if (itemId == R.id.nav_converter) {
                showScreen(Screen.CONVERTER);
            } else {
                showScreen(Screen.MARKET);
            }
            return true;
        });

        searchInput.addTextChangedListener(afterTextChanged(this::filterAndRender));

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
                return;
            }
            sessionManager.setNotificationsEnabled(isChecked);
            if (isChecked) {
                Snackbar.make(notificationSwitch, "Notifications enabled", Snackbar.LENGTH_SHORT).show();
            }
        });

        portfolioCryptoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePortfolioAmountInput();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        MaterialButton savePortfolioButton = findViewById(R.id.savePortfolioButton);
        savePortfolioButton.setOnClickListener(v -> savePortfolioHolding());

        MaterialButton removePortfolioButton = findViewById(R.id.removePortfolioButton);
        removePortfolioButton.setOnClickListener(v -> removeSelectedPortfolioHolding());

        AdapterView.OnItemSelectedListener converterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!refreshingConverterOptions) {
                    renderConverter();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        converterFromSpinner.setOnItemSelectedListener(converterListener);
        converterToSpinner.setOnItemSelectedListener(converterListener);
        converterAmountInput.addTextChangedListener(afterTextChanged(this::renderConverter));

        ImageButton converterSwapButton = findViewById(R.id.converterSwapButton);
        converterSwapButton.setOnClickListener(v -> swapConverterSelection());
    }

    private void setupApiSourceSpinner() {
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                ApiSource.displayNames()
        );
        sourceAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        apiSourceSpinner.setAdapter(sourceAdapter);
        apiSourceSpinner.setSelection(currentApiSource.ordinal(), false);
        apiSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ApiSource selectedSource = ApiSource.values()[position];
                if (selectedSource == currentApiSource) {
                    return;
                }
                currentApiSource = selectedSource;
                sessionManager.setApiSource(selectedSource);
                apiClient = new ApiClient(selectedSource);
                allCurrencies.clear();
                lastPrices.clear();
                indexCurrencies();
                refreshPortfolioCryptoSpinner();
                refreshConverterOptions();
                renderCurrentScreen();
                loadCurrencies(true);
                Snackbar.make(apiSourceSpinner, "API: " + selectedSource.getDisplayName(), Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
                indexCurrencies();
                lastMarketStatus = Formatters.updatedNow() + " - " + loadedSourceLabel(result);
                refreshPortfolioCryptoSpinner();
                refreshConverterOptions();
                updateHeaderSubtitle();
                renderCurrentScreen();
            }

            @Override
            public void onError(Exception error) {
                progressBar.setVisibility(View.GONE);
                emptyText.setText(R.string.empty_market);
                emptyText.setVisibility(View.VISIBLE);
                Snackbar.make(emptyText, "Unable to fetch market data", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void loadFiatRates() {
        apiClient.fetchFiatRates(new ApiCallback<Map<String, Double>>() {
            @Override
            public void onSuccess(Map<String, Double> result) {
                fiatRates.clear();
                fiatRates.putAll(defaultFiatRates());
                fiatRates.putAll(result);
                fiatRatesFromNetwork = true;
                renderConverter();
            }

            @Override
            public void onError(Exception error) {
                fiatRatesFromNetwork = false;
                renderConverter();
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

    private void showScreen(Screen screen) {
        currentScreen = screen;
        showingFavorites = screen == Screen.FAVORITES;
        boolean showingMarket = screen == Screen.MARKET || screen == Screen.FAVORITES;
        marketControls.setVisibility(showingMarket ? View.VISIBLE : View.GONE);
        marketContent.setVisibility(showingMarket ? View.VISIBLE : View.GONE);
        portfolioContent.setVisibility(screen == Screen.PORTFOLIO ? View.VISIBLE : View.GONE);
        converterContent.setVisibility(screen == Screen.CONVERTER ? View.VISIBLE : View.GONE);
        updateHeaderSubtitle();
        renderCurrentScreen();
    }

    private void renderCurrentScreen() {
        if (currentScreen == Screen.PORTFOLIO) {
            renderPortfolio();
        } else if (currentScreen == Screen.CONVERTER) {
            renderConverter();
        } else {
            filterAndRender();
        }
    }

    private void updateHeaderSubtitle() {
        if (currentScreen == Screen.PORTFOLIO) {
            int count = sessionManager.getPortfolioHoldings().size();
            updatedAtText.setText("Local portfolio - " + count + (count == 1 ? " position" : " positions"));
        } else if (currentScreen == Screen.CONVERTER) {
            updatedAtText.setText(fiatRatesFromNetwork ? "Fiat rates updated" : "Indicative fiat rates");
        } else {
            updatedAtText.setText(lastMarketStatus != null ? lastMarketStatus : getString(R.string.updating_data));
        }
    }

    private void updateStats() {
        int favoriteCount = 0;
        for (CryptoCurrency currency : allCurrencies) {
            if (currency.isFavorite()) {
                favoriteCount++;
            }
        }
        marketCountText.setText(allCurrencies.size() + (allCurrencies.size() == 1 ? " asset" : " assets"));
        favoriteCountText.setText(favoriteCount + (favoriteCount == 1 ? " favorite" : " favorites"));
    }

    private void indexCurrencies() {
        currenciesBySymbol.clear();
        for (CryptoCurrency currency : allCurrencies) {
            String symbol = normalizeSymbol(currency.getSymbol());
            if (!symbol.isEmpty() && !currenciesBySymbol.containsKey(symbol)) {
                currenciesBySymbol.put(symbol, currency);
            }
        }
    }

    private void refreshPortfolioCryptoSpinner() {
        String selectedSymbol = selectedPortfolioSymbol();
        portfolioSpinnerCurrencies.clear();
        portfolioSpinnerCurrencies.addAll(allCurrencies);

        List<String> labels = new ArrayList<>();
        for (CryptoCurrency currency : portfolioSpinnerCurrencies) {
            labels.add(currency.getName() + " (" + currency.getSymbol() + ")");
        }
        if (labels.isEmpty()) {
            labels.add(getString(R.string.loading_market_data));
        }

        ArrayAdapter<String> portfolioAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, labels);
        portfolioAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        portfolioCryptoSpinner.setAdapter(portfolioAdapter);

        int selection = 0;
        if (selectedSymbol != null) {
            for (int i = 0; i < portfolioSpinnerCurrencies.size(); i++) {
                if (selectedSymbol.equals(normalizeSymbol(portfolioSpinnerCurrencies.get(i).getSymbol()))) {
                    selection = i;
                    break;
                }
            }
        }
        portfolioCryptoSpinner.setSelection(selection, false);
        updatePortfolioAmountInput();
    }

    private void updatePortfolioAmountInput() {
        CryptoCurrency selected = selectedPortfolioCurrency();
        if (selected == null) {
            portfolioAmountInput.setText("");
            return;
        }
        Double amount = sessionManager.getPortfolioHoldings().get(normalizeSymbol(selected.getSymbol()));
        portfolioAmountInput.setText(amount != null ? Formatters.quantity(amount) : "");
        if (portfolioAmountInput.getText() != null) {
            portfolioAmountInput.setSelection(portfolioAmountInput.getText().length());
        }
    }

    private void savePortfolioHolding() {
        CryptoCurrency selected = selectedPortfolioCurrency();
        if (selected == null) {
            Snackbar.make(portfolioContent, "Prices are still loading", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Double amount = parseAmount(portfolioAmountInput);
        if (amount == null || amount <= 0.0) {
            Snackbar.make(portfolioContent, "Enter a positive quantity", Snackbar.LENGTH_SHORT).show();
            return;
        }
        sessionManager.setPortfolioHolding(selected.getSymbol(), amount);
        renderPortfolio();
        updateHeaderSubtitle();
        Snackbar.make(portfolioContent, "Position saved", Snackbar.LENGTH_SHORT).show();
    }

    private void removeSelectedPortfolioHolding() {
        CryptoCurrency selected = selectedPortfolioCurrency();
        if (selected == null) {
            return;
        }
        sessionManager.removePortfolioHolding(selected.getSymbol());
        portfolioAmountInput.setText("");
        renderPortfolio();
        updateHeaderSubtitle();
        Snackbar.make(portfolioContent, "Position removed", Snackbar.LENGTH_SHORT).show();
    }

    private void renderPortfolio() {
        Map<String, Double> holdings = sessionManager.getPortfolioHoldings();
        portfolioHoldingList.removeAllViews();

        double totalValue = 0.0;
        double previousTotal = 0.0;
        double totalChange = 0.0;
        Set<String> renderedSymbols = new HashSet<>();

        for (CryptoCurrency currency : allCurrencies) {
            String symbol = normalizeSymbol(currency.getSymbol());
            Double amount = holdings.get(symbol);
            if (amount == null || amount <= 0.0) {
                continue;
            }
            double value = amount * currency.getPriceUsd();
            double previousValue = previousValue(value, currency.getChangePercent24h());
            totalValue += value;
            previousTotal += previousValue;
            totalChange += value - previousValue;
            renderedSymbols.add(symbol);
            addPortfolioRow(symbol, currency.getName(), amount, value, currency.getChangePercent24h(), true);
        }

        for (Map.Entry<String, Double> entry : holdings.entrySet()) {
            String symbol = normalizeSymbol(entry.getKey());
            if (renderedSymbols.contains(symbol)) {
                continue;
            }
            addPortfolioRow(symbol, symbol, entry.getValue(), 0.0, 0.0, false);
        }

        double totalChangePercent = previousTotal > 0.0 ? (totalChange / previousTotal) * 100.0 : 0.0;
        portfolioTotalText.setText(Formatters.price(totalValue));
        portfolioChangeText.setText((totalChange >= 0.0 ? "+" : "-")
                + Formatters.price(Math.abs(totalChange))
                + " (" + Formatters.change(totalChangePercent) + ")");
        portfolioChangeText.setTextColor(ContextCompat.getColor(this, totalChange >= 0.0 ? R.color.positive : R.color.negative));
        portfolioPositionCountText.setText(holdings.size() + (holdings.size() == 1 ? " position" : " positions"));
        portfolioEmptyText.setVisibility(holdings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addPortfolioRow(
            String symbol,
            String name,
            double amount,
            double valueUsd,
            double changePercent,
            boolean priced
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(12), dp(10), dp(12));
        row.setBackgroundResource(R.drawable.bg_stat_box);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));

        TextView avatar = new TextView(this);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_coin_avatar);
        avatar.setText(Formatters.initials(symbol));
        avatar.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
        avatar.setTextSize(13);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(avatar, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(12), 0, dp(8), 0);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );

        TextView nameText = createPortfolioText(name, R.color.text_primary, 16, true, 1);
        TextView amountText = createPortfolioText(Formatters.quantity(amount) + " " + symbol, R.color.text_secondary, 13, false, 1);
        TextView valueText = createPortfolioText(
                priced ? Formatters.price(valueUsd) + " - " + Formatters.change(changePercent) : "Price unavailable",
                priced && changePercent < 0.0 ? R.color.negative : priced ? R.color.positive : R.color.text_secondary,
                13,
                false,
                2
        );

        details.addView(nameText);
        details.addView(amountText);
        details.addView(valueText);
        row.addView(details, detailsParams);

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setBackgroundResource(R.drawable.bg_surface_action_button);
        deleteButton.setContentDescription(getString(R.string.remove_position));
        deleteButton.setImageResource(R.drawable.ic_delete_24);
        deleteButton.setOnClickListener(v -> {
            sessionManager.removePortfolioHolding(symbol);
            renderPortfolio();
            updateHeaderSubtitle();
            Snackbar.make(portfolioContent, "Position removed", Snackbar.LENGTH_SHORT).show();
        });
        row.addView(deleteButton, new LinearLayout.LayoutParams(dp(44), dp(44)));

        row.setOnClickListener(v -> selectPortfolioSymbol(symbol));
        portfolioHoldingList.addView(row, rowParams);
    }

    private TextView createPortfolioText(String text, int colorRes, int sizeSp, boolean bold, int maxLines) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(this, colorRes));
        textView.setTextSize(sizeSp);
        if (maxLines == 1) {
            textView.setSingleLine(true);
        } else {
            textView.setSingleLine(false);
            textView.setMaxLines(maxLines);
        }
        textView.setEllipsize(TextUtils.TruncateAt.END);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return textView;
    }

    private void selectPortfolioSymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        for (int i = 0; i < portfolioSpinnerCurrencies.size(); i++) {
            if (normalized.equals(normalizeSymbol(portfolioSpinnerCurrencies.get(i).getSymbol()))) {
                portfolioCryptoSpinner.setSelection(i);
                updatePortfolioAmountInput();
                return;
            }
        }
    }

    private CryptoCurrency selectedPortfolioCurrency() {
        int position = portfolioCryptoSpinner.getSelectedItemPosition();
        if (position >= 0 && position < portfolioSpinnerCurrencies.size()) {
            return portfolioSpinnerCurrencies.get(position);
        }
        return null;
    }

    private String selectedPortfolioSymbol() {
        CryptoCurrency selected = selectedPortfolioCurrency();
        return selected != null ? normalizeSymbol(selected.getSymbol()) : null;
    }

    private void refreshConverterOptions() {
        String selectedFrom = selectedConverterCode(converterFromSpinner);
        String selectedTo = selectedConverterCode(converterToSpinner);
        List<String> codes = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();

        for (CryptoCurrency currency : allCurrencies) {
            String symbol = normalizeSymbol(currency.getSymbol());
            if (symbol.isEmpty() || added.contains(symbol)) {
                continue;
            }
            added.add(symbol);
            codes.add(symbol);
            labels.add(currency.getSymbol() + " - " + currency.getName());
        }

        for (String code : FIAT_CODES) {
            if (added.add(code)) {
                codes.add(code);
                labels.add(code + " - " + fiatDisplayName(code));
            }
        }

        converterOptionCodes.clear();
        converterOptionCodes.addAll(codes);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, labels);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);

        refreshingConverterOptions = true;
        converterFromSpinner.setAdapter(adapter);
        converterToSpinner.setAdapter(adapter);

        String defaultFrom = !allCurrencies.isEmpty() ? normalizeSymbol(allCurrencies.get(0).getSymbol()) : "USD";
        String fromCode = converterOptionCodes.contains(selectedFrom) ? selectedFrom : defaultFrom;
        String toCode = converterOptionCodes.contains(selectedTo) ? selectedTo : ("USD".equals(fromCode) ? "EUR" : "USD");
        setConverterSelection(converterFromSpinner, fromCode);
        setConverterSelection(converterToSpinner, toCode);
        refreshingConverterOptions = false;
        renderConverter();
    }

    private void swapConverterSelection() {
        int fromPosition = converterFromSpinner.getSelectedItemPosition();
        int toPosition = converterToSpinner.getSelectedItemPosition();
        if (fromPosition < 0 || toPosition < 0) {
            return;
        }
        refreshingConverterOptions = true;
        converterFromSpinner.setSelection(toPosition, false);
        converterToSpinner.setSelection(fromPosition, false);
        refreshingConverterOptions = false;
        renderConverter();
    }

    private void setConverterSelection(Spinner spinner, String code) {
        int index = converterOptionCodes.indexOf(code);
        if (index >= 0) {
            spinner.setSelection(index, false);
        }
    }

    private void renderConverter() {
        if (converterResultText == null || converterRateText == null) {
            return;
        }

        Double amount = parseAmount(converterAmountInput);
        String fromCode = selectedConverterCode(converterFromSpinner);
        String toCode = selectedConverterCode(converterToSpinner);
        if (amount == null || fromCode == null || toCode == null) {
            converterResultText.setText("Enter an amount");
            converterRateText.setText(getString(R.string.converter_rate));
            return;
        }

        Double amountUsd = amountToUsd(amount, fromCode);
        Double converted = amountUsd != null ? usdToCode(amountUsd, toCode) : null;
        Double rateUsd = amountToUsd(1.0, fromCode);
        Double rate = rateUsd != null ? usdToCode(rateUsd, toCode) : null;

        if (converted == null || rate == null) {
            converterResultText.setText("Rate unavailable");
            converterRateText.setText("Crypto prices are still loading");
            return;
        }

        converterResultText.setText(formatConverted(converted, toCode));
        converterRateText.setText("1 " + fromCode + " = " + formatConverted(rate, toCode)
                + " - " + (fiatRatesFromNetwork ? "rates updated" : "indicative rate"));
    }

    private String selectedConverterCode(Spinner spinner) {
        if (spinner == null) {
            return null;
        }
        int position = spinner.getSelectedItemPosition();
        if (position >= 0 && position < converterOptionCodes.size()) {
            return converterOptionCodes.get(position);
        }
        return null;
    }

    private Double amountToUsd(double amount, String code) {
        String normalized = normalizeSymbol(code);
        CryptoCurrency currency = currenciesBySymbol.get(normalized);
        if (currency != null && currency.getPriceUsd() > 0.0) {
            return amount * currency.getPriceUsd();
        }
        Double fiatRate = fiatRates.get(normalized);
        if (fiatRate != null && fiatRate > 0.0) {
            return amount / fiatRate;
        }
        return null;
    }

    private Double usdToCode(double amountUsd, String code) {
        String normalized = normalizeSymbol(code);
        CryptoCurrency currency = currenciesBySymbol.get(normalized);
        if (currency != null && currency.getPriceUsd() > 0.0) {
            return amountUsd / currency.getPriceUsd();
        }
        Double fiatRate = fiatRates.get(normalized);
        if (fiatRate != null && fiatRate > 0.0) {
            return amountUsd * fiatRate;
        }
        return null;
    }

    private String formatConverted(double value, String code) {
        if (fiatRates.containsKey(normalizeSymbol(code))) {
            return Formatters.fiat(value, code);
        }
        return Formatters.cryptoAmount(value, code);
    }

    private Double parseAmount(TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return null;
        }
        String raw = input.getText().toString().trim().replace(" ", "").replace(",", ".");
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private double previousValue(double currentValue, double changePercent) {
        double factor = 1.0 + (changePercent / 100.0);
        if (factor <= 0.0) {
            return currentValue;
        }
        return currentValue / factor;
    }

    private String loadedSourceLabel(List<CryptoCurrency> currencies) {
        if (currentApiSource != ApiSource.AUTO) {
            return currentApiSource.getDisplayName();
        }
        if (currencies != null && !currencies.isEmpty()) {
            return currencies.get(0).getSource();
        }
        return ApiSource.AUTO.getDisplayName();
    }

    private TextWatcher afterTextChanged(Runnable action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                action.run();
            }
        };
    }

    private void initializeFiatRates() {
        fiatRates.clear();
        fiatRates.putAll(defaultFiatRates());
    }

    private Map<String, Double> defaultFiatRates() {
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("USD", 1.0);
        rates.put("EUR", 0.85455);
        rates.put("GBP", 0.74026);
        rates.put("JPY", 156.56);
        rates.put("CHF", 0.78534);
        rates.put("CAD", 1.3668);
        rates.put("AUD", 1.399);
        return rates;
    }

    private String fiatDisplayName(String code) {
        switch (code) {
            case "EUR":
                return "Euro";
            case "GBP":
                return "British pound";
            case "JPY":
                return "Japanese yen";
            case "CHF":
                return "Swiss franc";
            case "CAD":
                return "Canadian dollar";
            case "AUD":
                return "Australian dollar";
            case "USD":
            default:
                return "US dollar";
        }
    }

    private String normalizeSymbol(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
        Snackbar.make(emptyText, favorite ? "Added to favorites" : "Removed from favorites", Snackbar.LENGTH_SHORT).show();
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
        Snackbar.make(notificationSwitch, granted ? "Notifications enabled" : "Notification permission denied", Snackbar.LENGTH_LONG).show();
    }
}
