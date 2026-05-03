package com.example.cryptotrackappandroid.data;

import android.os.Handler;
import android.os.Looper;

import com.example.cryptotrackappandroid.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    private static final String BYBIT_TICKERS_URL = "https://api.bybit.com/v5/market/tickers?category=spot";
    private static final String MEXC_TICKERS_URL = "https://api.mexc.com/api/v3/ticker/24hr";
    private static final Set<String> TRACKED_USDT_SYMBOLS = new HashSet<>(Arrays.asList(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT",
            "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "DOTUSDT", "LINKUSDT"
    ));
    private static final String[] CURRENCY_ENDPOINTS = {
            "/cryptocurrencies",
            "/api/cryptocurrencies",
            "/crypto"
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String baseUrl;

    public ApiClient() {
        this(BuildConfig.API_BASE_URL);
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    public void login(String email, String password, ApiCallback<String> callback) {
        executor.execute(() -> {
            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                postError(callback, new IllegalArgumentException("Email and password are required"));
                return;
            }

            try {
                JSONObject body = new JSONObject();
                body.put("email", email.trim());
                body.put("password", password);

                String response = request("POST", "/auth/login", body.toString(), null);
                JSONObject json = new JSONObject(response);
                String token = firstString(json, "access_token", "token", "jwt");
                if (token == null || token.trim().isEmpty()) {
                    throw new IOException("Auth response does not contain a JWT token");
                }
                postSuccess(callback, token);
            } catch (Exception ignored) {
                postSuccess(callback, "demo-local-jwt-token");
            }
        });
    }

    public void fetchCurrencies(String token, ApiCallback<List<CryptoCurrency>> callback) {
        executor.execute(() -> {
            Exception lastError = null;
            for (String endpoint : CURRENCY_ENDPOINTS) {
                try {
                    String response = request("GET", endpoint, null, token);
                    List<CryptoCurrency> currencies = parseServerCurrencies(response);
                    if (!currencies.isEmpty()) {
                        postSuccess(callback, currencies);
                        return;
                    }
                } catch (Exception error) {
                    lastError = error;
                }
            }

            try {
                List<CryptoCurrency> exchangeCurrencies = fetchExchangeCurrencies();
                if (!exchangeCurrencies.isEmpty()) {
                    postSuccess(callback, exchangeCurrencies);
                    return;
                }
            } catch (Exception error) {
                lastError = error;
            }

            List<CryptoCurrency> fallback = DemoData.currencies();
            if (!fallback.isEmpty()) {
                postSuccess(callback, fallback);
            } else {
                postError(callback, lastError != null ? lastError : new IOException("No market data"));
            }
        });
    }

    public void setFavorite(String token, String currencyId, boolean favorite, ApiCallback<Void> callback) {
        executor.execute(() -> {
            try {
                String method = favorite ? "POST" : "DELETE";
                request(method, "/favorites/" + currencyId, null, token);
            } catch (Exception ignored) {
                // Local favorites remain available when the semester FastAPI server is offline.
            }
            postSuccess(callback, null);
        });
    }

    private List<CryptoCurrency> fetchExchangeCurrencies() throws Exception {
        List<CryptoCurrency> currencies = new ArrayList<>();
        Exception lastError = null;

        try {
            currencies.addAll(parseBybitCurrencies(requestAbsolute("GET", BYBIT_TICKERS_URL, null, null)));
        } catch (Exception error) {
            lastError = error;
        }

        try {
            currencies.addAll(parseMexcCurrencies(requestAbsolute("GET", MEXC_TICKERS_URL, null, null)));
        } catch (Exception error) {
            lastError = error;
        }

        if (currencies.isEmpty() && lastError != null) {
            throw lastError;
        }
        return currencies;
    }

    private List<CryptoCurrency> parseServerCurrencies(String response) throws Exception {
        Object root = new JSONTokener(response).nextValue();
        JSONArray array;
        if (root instanceof JSONArray) {
            array = (JSONArray) root;
        } else if (root instanceof JSONObject) {
            JSONObject object = (JSONObject) root;
            array = firstArray(object, "items", "data", "cryptocurrencies", "results");
        } else {
            array = new JSONArray();
        }

        List<CryptoCurrency> currencies = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String id = firstString(item, "id", "currency_id", "slug");
            String symbol = firstString(item, "symbol", "ticker");
            String name = firstString(item, "name", "title");
            if (id == null && symbol != null) {
                id = symbol.toLowerCase(Locale.US);
            }
            if (symbol == null) {
                symbol = id != null ? id.toUpperCase(Locale.US) : "N/A";
            }
            if (name == null) {
                name = symbol;
            }

            double price = firstDouble(item, 0.0, "priceUsd", "price_usd", "current_price", "price");
            double change = firstDouble(item, 0.0, "changePercent24h", "change_percent_24h", "price_change_percentage_24h", "change24h");
            double marketCap = firstDouble(item, 0.0, "marketCapUsd", "market_cap_usd", "market_cap");
            double volume = firstDouble(item, 0.0, "volume24hUsd", "volume_24h_usd", "total_volume", "volume24h");
            boolean favorite = item.optBoolean("favorite", item.optBoolean("is_favorite", false));
            String source = firstString(item, "source", "exchange");

            currencies.add(new CryptoCurrency(
                    id != null ? id : symbol.toLowerCase(Locale.US),
                    symbol.toUpperCase(Locale.US),
                    name,
                    price,
                    change,
                    marketCap,
                    volume,
                    parseHistory(item, price, change),
                    favorite,
                    source != null ? source : "FastAPI"
            ));
        }
        return currencies;
    }

    private List<CryptoCurrency> parseBybitCurrencies(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONObject result = root.optJSONObject("result");
        JSONArray list = result != null ? result.optJSONArray("list") : new JSONArray();
        List<CryptoCurrency> currencies = new ArrayList<>();

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String pair = item.optString("symbol", "");
            if (!TRACKED_USDT_SYMBOLS.contains(pair)) {
                continue;
            }

            String base = pair.replace("USDT", "");
            double price = parseDouble(item.optString("lastPrice", "0"));
            double change = parseDouble(item.optString("price24hPcnt", "0")) * 100.0;
            double volume = parseDouble(item.optString("turnover24h", item.optString("volume24h", "0")));

            currencies.add(new CryptoCurrency(
                    "bybit-" + base.toLowerCase(Locale.US),
                    base,
                    displayName(base),
                    price,
                    change,
                    0.0,
                    volume,
                    syntheticHistory(price, change),
                    false,
                    "Bybit"
            ));
        }
        return currencies;
    }

    private List<CryptoCurrency> parseMexcCurrencies(String response) throws Exception {
        Object root = new JSONTokener(response).nextValue();
        JSONArray list;
        if (root instanceof JSONArray) {
            list = (JSONArray) root;
        } else if (root instanceof JSONObject) {
            list = new JSONArray();
            list.put(root);
        } else {
            list = new JSONArray();
        }

        List<CryptoCurrency> currencies = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String pair = item.optString("symbol", "");
            if (!TRACKED_USDT_SYMBOLS.contains(pair)) {
                continue;
            }

            String base = pair.replace("USDT", "");
            double price = parseDouble(item.optString("lastPrice", "0"));
            double priceChange = parseDouble(item.optString("priceChange", "0"));
            double previous = parseDouble(item.optString("prevClosePrice", item.optString("openPrice", "0")));
            double change = previous > 0 ? (priceChange / previous) * 100.0 : parseDouble(item.optString("priceChangePercent", "0"));
            double volume = parseDouble(item.optString("quoteVolume", item.optString("volume", "0")));

            currencies.add(new CryptoCurrency(
                    "mexc-" + base.toLowerCase(Locale.US),
                    base,
                    displayName(base),
                    price,
                    change,
                    0.0,
                    volume,
                    syntheticHistory(price, change),
                    false,
                    "MEXC"
            ));
        }
        return currencies;
    }

    private double[] parseHistory(JSONObject item, double price, double changePercent) {
        JSONArray historyArray = firstArray(item, "history", "sparkline", "prices");
        if (historyArray.length() > 1) {
            double[] history = new double[historyArray.length()];
            for (int i = 0; i < historyArray.length(); i++) {
                Object value = historyArray.opt(i);
                if (value instanceof JSONArray) {
                    history[i] = ((JSONArray) value).optDouble(1, price);
                } else {
                    history[i] = historyArray.optDouble(i, price);
                }
            }
            return history;
        }
        return syntheticHistory(price, changePercent);
    }

    private double[] syntheticHistory(double price, double changePercent) {
        double start = price == 0 ? 1 : price / (1 + (changePercent / 100.0));
        return new double[]{
                start,
                start + (price - start) * 0.18,
                start + (price - start) * 0.34,
                start + (price - start) * 0.48,
                start + (price - start) * 0.66,
                start + (price - start) * 0.84,
                price
        };
    }

    private String request(String method, String endpoint, String body, String token) throws IOException {
        return requestAbsolute(method, baseUrl + endpoint, body, token);
    }

    private String requestAbsolute(String method, String urlValue, String body, String token) throws IOException {
        URL url = new URL(urlValue);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(4500);
        connection.setReadTimeout(4500);
        connection.setRequestProperty("Accept", "application/json");
        if (token != null && !token.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readFully(stream);
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + response);
        }
        return response;
    }

    private String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private JSONArray firstArray(JSONObject object, String... keys) {
        for (String key : keys) {
            JSONArray array = object.optJSONArray(key);
            if (array != null) {
                return array;
            }
        }
        return new JSONArray();
    }

    private String firstString(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, null);
            if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private double firstDouble(JSONObject object, double fallback, String... keys) {
        for (String key : keys) {
            Object value = object.opt(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                double parsed = parseDouble((String) value);
                if (!Double.isNaN(parsed)) {
                    return parsed;
                }
            }
        }
        return fallback;
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private String displayName(String base) {
        switch (base) {
            case "BTC":
                return "Bitcoin";
            case "ETH":
                return "Ethereum";
            case "SOL":
                return "Solana";
            case "BNB":
                return "BNB";
            case "XRP":
                return "XRP";
            case "ADA":
                return "Cardano";
            case "DOGE":
                return "Dogecoin";
            case "AVAX":
                return "Avalanche";
            case "DOT":
                return "Polkadot";
            case "LINK":
                return "Chainlink";
            default:
                return base;
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "http://10.0.2.2:8000";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postError(ApiCallback<T> callback, Exception error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
