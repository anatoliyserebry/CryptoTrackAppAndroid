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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    private static final String BYBIT_BASE_URL = "https://api.bybit.com";
    private static final String BYBIT_TICKERS_URL = BYBIT_BASE_URL + "/v5/market/tickers?category=spot";
    private static final String MEXC_BASE_URL = "https://api.mexc.com";
    private static final String MEXC_TICKERS_URL = MEXC_BASE_URL + "/api/v3/ticker/24hr";
    private static final String BINANCE_BASE_URL = "https://api.binance.com";
    private static final String BINANCE_TICKERS_URL = BINANCE_BASE_URL
            + "/api/v3/ticker/24hr?symbols=%5B%22BTCUSDT%22,%22ETHUSDT%22,%22SOLUSDT%22,%22BNBUSDT%22,%22XRPUSDT%22,"
            + "%22ADAUSDT%22,%22DOGEUSDT%22,%22AVAXUSDT%22,%22DOTUSDT%22,%22LINKUSDT%22%5D";
    private static final String KRAKEN_BASE_URL = "https://api.kraken.com";
    private static final String KRAKEN_TICKERS_URL = KRAKEN_BASE_URL
            + "/0/public/Ticker?pair=XBTUSD,ETHUSD,SOLUSD,BNBUSD,XRPUSD,ADAUSD,DOGEUSD,AVAXUSD,DOTUSD,LINKUSD";
    private static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3";
    private static final String COINGECKO_MARKETS_URL = COINGECKO_BASE_URL
            + "/coins/markets?vs_currency=usd&ids=bitcoin,ethereum,solana,binancecoin,ripple,cardano,dogecoin,avalanche-2,polkadot,chainlink"
            + "&order=market_cap_desc&per_page=20&page=1&sparkline=true&price_change_percentage=24h,7d,30d";
    private static final String FIAT_RATES_URL = "https://api.frankfurter.app/latest?from=USD&to=EUR,GBP,JPY,CHF,CAD,AUD";
    private static final Set<String> TRACKED_USDT_SYMBOLS = new HashSet<>(Arrays.asList(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT",
            "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "DOTUSDT", "LINKUSDT"
    ));
    private static final Map<String, String> COINGECKO_IDS_BY_SYMBOL = new HashMap<>();
    private static final Map<String, String> KRAKEN_PAIRS_BY_SYMBOL = new HashMap<>();
    private static final String[] CURRENCY_ENDPOINTS = {
            "/cryptocurrencies",
            "/api/cryptocurrencies",
            "/crypto"
    };

    static {
        COINGECKO_IDS_BY_SYMBOL.put("BTC", "bitcoin");
        COINGECKO_IDS_BY_SYMBOL.put("ETH", "ethereum");
        COINGECKO_IDS_BY_SYMBOL.put("SOL", "solana");
        COINGECKO_IDS_BY_SYMBOL.put("BNB", "binancecoin");
        COINGECKO_IDS_BY_SYMBOL.put("XRP", "ripple");
        COINGECKO_IDS_BY_SYMBOL.put("ADA", "cardano");
        COINGECKO_IDS_BY_SYMBOL.put("DOGE", "dogecoin");
        COINGECKO_IDS_BY_SYMBOL.put("AVAX", "avalanche-2");
        COINGECKO_IDS_BY_SYMBOL.put("DOT", "polkadot");
        COINGECKO_IDS_BY_SYMBOL.put("LINK", "chainlink");

        KRAKEN_PAIRS_BY_SYMBOL.put("BTC", "XBTUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("ETH", "ETHUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("SOL", "SOLUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("BNB", "BNBUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("XRP", "XRPUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("ADA", "ADAUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("DOGE", "DOGEUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("AVAX", "AVAXUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("DOT", "DOTUSD");
        KRAKEN_PAIRS_BY_SYMBOL.put("LINK", "LINKUSD");
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String baseUrl;
    private final ApiSource source;

    public ApiClient() {
        this(BuildConfig.API_BASE_URL, ApiSource.AUTO);
    }

    public ApiClient(ApiSource source) {
        this(BuildConfig.API_BASE_URL, source);
    }

    public ApiClient(String baseUrl) {
        this(baseUrl, ApiSource.AUTO);
    }

    public ApiClient(String baseUrl, ApiSource source) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.source = source != null ? source : ApiSource.AUTO;
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
            for (ApiSource candidate : currencySources()) {
                try {
                    List<CryptoCurrency> currencies = fetchCurrenciesFromSource(candidate, token);
                    if (!currencies.isEmpty()) {
                        postSuccess(callback, currencies);
                        return;
                    }
                } catch (Exception error) {
                    lastError = error;
                }
            }

            List<CryptoCurrency> fallback = DemoData.currencies();
            if (!fallback.isEmpty()) {
                postSuccess(callback, fallback);
            } else {
                postError(callback, lastError != null ? lastError : new IOException("No market data"));
            }
        });
    }

    public void fetchChart(
            String token,
            CryptoCurrency currency,
            ChartRange range,
            ApiSource requestedSource,
            ApiCallback<ChartSeries> callback
    ) {
        executor.execute(() -> {
            ChartRange selectedRange = range != null ? range : ChartRange.DAY;
            ApiSource selectedSource = requestedSource != null ? requestedSource : source;
            Exception lastError = null;

            for (ApiSource candidate : chartSources(selectedSource, currency)) {
                try {
                    ChartSeries chartSeries = fetchChartFromSource(candidate, token, currency, selectedRange);
                    if (chartSeries.getPrices().length > 1) {
                        postSuccess(callback, chartSeries);
                        return;
                    }
                } catch (Exception error) {
                    lastError = error;
                }
            }

            ChartSeries synthetic = syntheticSeries(currency, selectedRange, selectedSource, lastError != null);
            postSuccess(callback, synthetic);
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

    public void fetchFiatRates(ApiCallback<Map<String, Double>> callback) {
        executor.execute(() -> {
            try {
                JSONObject root = new JSONObject(requestAbsolute("GET", FIAT_RATES_URL, null, null));
                JSONObject ratesObject = root.optJSONObject("rates");
                Map<String, Double> rates = new LinkedHashMap<>();
                rates.put("USD", 1.0);
                if (ratesObject != null) {
                    String[] codes = {"EUR", "GBP", "JPY", "CHF", "CAD", "AUD"};
                    for (String code : codes) {
                        double rate = ratesObject.optDouble(code, Double.NaN);
                        if (!Double.isNaN(rate) && rate > 0.0) {
                            rates.put(code, rate);
                        }
                    }
                }
                if (rates.size() <= 1) {
                    throw new IOException("No fiat rates");
                }
                postSuccess(callback, rates);
            } catch (Exception error) {
                postError(callback, error);
            }
        });
    }

    private List<ApiSource> currencySources() {
        if (source == ApiSource.AUTO) {
            return Arrays.asList(
                    ApiSource.FASTAPI,
                    ApiSource.COINGECKO,
                    ApiSource.BYBIT,
                    ApiSource.MEXC,
                    ApiSource.BINANCE,
                    ApiSource.KRAKEN
            );
        }
        return Collections.singletonList(source);
    }

    private List<ApiSource> chartSources(ApiSource selectedSource, CryptoCurrency currency) {
        if (selectedSource != ApiSource.AUTO) {
            return Collections.singletonList(selectedSource);
        }

        LinkedHashSet<ApiSource> ordered = new LinkedHashSet<>();
        ApiSource currencySource = currency != null ? ApiSource.fromCurrencySource(currency.getSource()) : ApiSource.AUTO;
        if (currencySource != ApiSource.AUTO) {
            ordered.add(currencySource);
        }
        ordered.add(ApiSource.COINGECKO);
        ordered.add(ApiSource.BYBIT);
        ordered.add(ApiSource.MEXC);
        ordered.add(ApiSource.BINANCE);
        ordered.add(ApiSource.KRAKEN);
        ordered.add(ApiSource.FASTAPI);
        return new ArrayList<>(ordered);
    }

    private List<CryptoCurrency> fetchCurrenciesFromSource(ApiSource candidate, String token) throws Exception {
        switch (candidate) {
            case FASTAPI:
                return fetchServerCurrencies(token);
            case COINGECKO:
                return parseCoinGeckoCurrencies(requestAbsolute("GET", COINGECKO_MARKETS_URL, null, null));
            case BYBIT:
                return parseBybitCurrencies(requestAbsolute("GET", BYBIT_TICKERS_URL, null, null));
            case MEXC:
                return parseMexcCurrencies(requestAbsolute("GET", MEXC_TICKERS_URL, null, null));
            case BINANCE:
                return parseBinanceCurrencies(requestAbsolute("GET", BINANCE_TICKERS_URL, null, null));
            case KRAKEN:
                return parseKrakenCurrencies(requestAbsolute("GET", KRAKEN_TICKERS_URL, null, null));
            case AUTO:
            default:
                return new ArrayList<>();
        }
    }

    private ChartSeries fetchChartFromSource(
            ApiSource candidate,
            String token,
            CryptoCurrency currency,
            ChartRange range
    ) throws Exception {
        switch (candidate) {
            case FASTAPI:
                return fetchServerChart(token, currency, range);
            case COINGECKO:
                return fetchCoinGeckoChart(currency, range);
            case BYBIT:
                return fetchBybitChart(currency, range);
            case MEXC:
                return fetchMexcChart(currency, range);
            case BINANCE:
                return fetchBinanceChart(currency, range);
            case KRAKEN:
                return fetchKrakenChart(currency, range);
            case AUTO:
            default:
                throw new IOException("Unsupported chart source");
        }
    }

    private List<CryptoCurrency> fetchServerCurrencies(String token) throws Exception {
        Exception lastError = null;
        for (String endpoint : CURRENCY_ENDPOINTS) {
            try {
                List<CryptoCurrency> currencies = parseServerCurrencies(request("GET", endpoint, null, token));
                if (!currencies.isEmpty()) {
                    return currencies;
                }
            } catch (Exception error) {
                lastError = error;
            }
        }
        throw lastError != null ? lastError : new IOException("No server currencies");
    }

    private ChartSeries fetchServerChart(String token, CryptoCurrency currency, ChartRange range) throws Exception {
        String id = safePathSegment(currency.getId());
        String symbol = safePathSegment(currency.getSymbol().toLowerCase(Locale.US));
        String[] endpoints = {
                "/cryptocurrencies/" + id + "/history?range=" + range.getKey(),
                "/api/cryptocurrencies/" + id + "/history?range=" + range.getKey(),
                "/crypto/" + id + "/history?range=" + range.getKey(),
                "/cryptocurrencies/" + symbol + "/history?range=" + range.getKey(),
                "/api/cryptocurrencies/" + symbol + "/history?range=" + range.getKey()
        };

        Exception lastError = null;
        for (String endpoint : endpoints) {
            try {
                return parseServerChart(request("GET", endpoint, null, token), range);
            } catch (Exception error) {
                lastError = error;
            }
        }
        throw lastError != null ? lastError : new IOException("No FastAPI chart data");
    }

    private ChartSeries fetchCoinGeckoChart(CryptoCurrency currency, ChartRange range) throws Exception {
        String coinId = coingeckoId(currency);
        if (coinId == null) {
            throw new IOException("No CoinGecko id for " + currency.getSymbol());
        }

        String url = COINGECKO_BASE_URL
                + "/coins/" + safePathSegment(coinId)
                + "/market_chart?vs_currency=usd&days=" + range.getDays();
        JSONObject root = new JSONObject(requestAbsolute("GET", url, null, null));
        List<Point> points = parsePointArray(root.optJSONArray("prices"), System.currentTimeMillis() - range.durationMillis(), range.getCandleMillis());
        return pointsToSeries(points, range, ApiSource.COINGECKO, ApiSource.COINGECKO.getDisplayName(), false);
    }

    private ChartSeries fetchBybitChart(CryptoCurrency currency, ChartRange range) throws Exception {
        long end = System.currentTimeMillis();
        long start = end - range.durationMillis();
        String symbol = tradingPair(currency);
        String url = BYBIT_BASE_URL
                + "/v5/market/kline?category=spot"
                + "&symbol=" + encodeQuery(symbol)
                + "&interval=" + encodeQuery(range.getBybitInterval())
                + "&start=" + start
                + "&end=" + end
                + "&limit=" + range.getLimit();
        JSONObject root = new JSONObject(requestAbsolute("GET", url, null, null));
        JSONObject result = root.optJSONObject("result");
        JSONArray list = result != null ? result.optJSONArray("list") : null;
        List<Point> points = parseKlineArray(list, 0, 4);
        return pointsToSeries(points, range, ApiSource.BYBIT, ApiSource.BYBIT.getDisplayName(), false);
    }

    private ChartSeries fetchMexcChart(CryptoCurrency currency, ChartRange range) throws Exception {
        long end = System.currentTimeMillis();
        long start = end - range.durationMillis();
        String symbol = tradingPair(currency);
        String url = MEXC_BASE_URL
                + "/api/v3/klines?symbol=" + encodeQuery(symbol)
                + "&interval=" + encodeQuery(range.getMexcInterval())
                + "&startTime=" + start
                + "&endTime=" + end
                + "&limit=" + range.getLimit();
        JSONArray list = new JSONArray(requestAbsolute("GET", url, null, null));
        List<Point> points = parseKlineArray(list, 0, 4);
        return pointsToSeries(points, range, ApiSource.MEXC, ApiSource.MEXC.getDisplayName(), false);
    }

    private ChartSeries fetchBinanceChart(CryptoCurrency currency, ChartRange range) throws Exception {
        long end = System.currentTimeMillis();
        long start = end - range.durationMillis();
        String symbol = tradingPair(currency);
        String url = BINANCE_BASE_URL
                + "/api/v3/klines?symbol=" + encodeQuery(symbol)
                + "&interval=" + encodeQuery(range.getMexcInterval())
                + "&startTime=" + start
                + "&endTime=" + end
                + "&limit=" + range.getLimit();
        JSONArray list = new JSONArray(requestAbsolute("GET", url, null, null));
        List<Point> points = parseKlineArray(list, 0, 4);
        return pointsToSeries(points, range, ApiSource.BINANCE, ApiSource.BINANCE.getDisplayName(), false);
    }

    private ChartSeries fetchKrakenChart(CryptoCurrency currency, ChartRange range) throws Exception {
        long start = (System.currentTimeMillis() - range.durationMillis()) / 1000L;
        String url = KRAKEN_BASE_URL
                + "/0/public/OHLC?pair=" + encodeQuery(krakenPair(currency))
                + "&interval=" + krakenInterval(range)
                + "&since=" + start;
        JSONObject root = new JSONObject(requestAbsolute("GET", url, null, null));
        JSONArray errors = root.optJSONArray("error");
        if (errors != null && errors.length() > 0) {
            throw new IOException("Kraken error: " + errors.toString());
        }
        JSONObject result = root.optJSONObject("result");
        JSONArray list = firstResultArray(result);
        List<Point> points = parseKlineArray(list, 0, 4);
        return pointsToSeries(points, range, ApiSource.KRAKEN, ApiSource.KRAKEN.getDisplayName(), false);
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
                    source != null ? source : ApiSource.FASTAPI.getDisplayName()
            ));
        }
        return currencies;
    }

    private List<CryptoCurrency> parseCoinGeckoCurrencies(String response) throws Exception {
        JSONArray list = new JSONArray(response);
        List<CryptoCurrency> currencies = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String id = item.optString("id", "");
            String symbol = item.optString("symbol", "").toUpperCase(Locale.US);
            String name = item.optString("name", symbol);
            double price = item.optDouble("current_price", 0.0);
            double change = item.optDouble("price_change_percentage_24h", 0.0);
            double marketCap = item.optDouble("market_cap", 0.0);
            double volume = item.optDouble("total_volume", 0.0);
            JSONObject sparkline = item.optJSONObject("sparkline_in_7d");
            JSONArray prices = sparkline != null ? sparkline.optJSONArray("price") : null;

            currencies.add(new CryptoCurrency(
                    id,
                    symbol,
                    name,
                    price,
                    change,
                    marketCap,
                    volume,
                    parseNumericHistory(prices, price, change),
                    false,
                    ApiSource.COINGECKO.getDisplayName()
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
                    ApiSource.BYBIT.getDisplayName()
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
                    ApiSource.MEXC.getDisplayName()
            ));
        }
        return currencies;
    }

    private List<CryptoCurrency> parseBinanceCurrencies(String response) throws Exception {
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
            double change = parseDouble(item.optString("priceChangePercent", "0"));
            double quoteVolume = parseDouble(item.optString("quoteVolume", "0"));
            double volume = quoteVolume > 0.0
                    ? quoteVolume
                    : parseDouble(item.optString("volume", "0")) * price;

            currencies.add(new CryptoCurrency(
                    "binance-" + base.toLowerCase(Locale.US),
                    base,
                    displayName(base),
                    price,
                    change,
                    0.0,
                    volume,
                    syntheticHistory(price, change),
                    false,
                    ApiSource.BINANCE.getDisplayName()
            ));
        }
        return currencies;
    }

    private List<CryptoCurrency> parseKrakenCurrencies(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONArray errors = root.optJSONArray("error");
        if (errors != null && errors.length() > 0) {
            throw new IOException("Kraken error: " + errors.toString());
        }

        JSONObject result = root.optJSONObject("result");
        List<CryptoCurrency> currencies = new ArrayList<>();
        if (result == null) {
            return currencies;
        }

        JSONArray keys = result.names();
        if (keys == null) {
            return currencies;
        }

        for (int i = 0; i < keys.length(); i++) {
            String pairKey = keys.optString(i);
            JSONObject item = result.optJSONObject(pairKey);
            String base = krakenSymbolFromPairKey(pairKey);
            if (item == null || base == null) {
                continue;
            }

            JSONArray close = item.optJSONArray("c");
            JSONArray volume = item.optJSONArray("v");
            double price = close != null ? parseDoubleValue(close.opt(0)) : 0.0;
            double open = parseDoubleValue(item.opt("o"));
            double change = open > 0.0 ? ((price - open) / open) * 100.0 : 0.0;
            double baseVolume = volume != null ? parseDoubleValue(volume.opt(1)) : 0.0;

            currencies.add(new CryptoCurrency(
                    "kraken-" + base.toLowerCase(Locale.US),
                    base,
                    displayName(base),
                    price,
                    change,
                    0.0,
                    baseVolume * price,
                    syntheticHistory(price, change),
                    false,
                    ApiSource.KRAKEN.getDisplayName()
            ));
        }
        return currencies;
    }

    private ChartSeries parseServerChart(String response, ChartRange range) throws Exception {
        Object root = new JSONTokener(response).nextValue();
        JSONArray array = null;
        JSONArray timestamps = null;
        if (root instanceof JSONArray) {
            array = (JSONArray) root;
        } else if (root instanceof JSONObject) {
            JSONObject object = (JSONObject) root;
            timestamps = firstArray(object, "timestamps", "times");
            array = firstArray(object, "prices", "history", "sparkline", "data", "items", "results");
        }

        long start = System.currentTimeMillis() - range.durationMillis();
        List<Point> points = parsePointArray(array, start, range.getCandleMillis(), timestamps);
        return pointsToSeries(points, range, ApiSource.FASTAPI, ApiSource.FASTAPI.getDisplayName(), false);
    }

    private double[] parseHistory(JSONObject item, double price, double changePercent) {
        JSONArray historyArray = firstArray(item, "history", "sparkline", "prices");
        return parseNumericHistory(historyArray, price, changePercent);
    }

    private double[] parseNumericHistory(JSONArray historyArray, double price, double changePercent) {
        if (historyArray != null && historyArray.length() > 1) {
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

    private ChartSeries syntheticSeries(CryptoCurrency currency, ChartRange range, ApiSource requestedSource, boolean fromError) {
        int count = range.getLimit();
        long end = System.currentTimeMillis();
        long start = end - range.durationMillis();
        long[] timestamps = new long[count];
        double[] prices = new double[count];
        double current = currency != null ? currency.getPriceUsd() : 1.0;
        double baseChange = currency != null ? currency.getChangePercent24h() : 0.0;
        double rangeChange = baseChange * Math.sqrt(Math.max(1, range.getDays()));
        double first = current == 0.0 ? 1.0 : current / (1.0 + (rangeChange / 100.0));
        double amplitude = Math.max(Math.abs(current - first) * 0.22, Math.max(current, 1.0) * 0.004);

        for (int i = 0; i < count; i++) {
            double progress = count == 1 ? 1.0 : (double) i / (double) (count - 1);
            double trend = first + (current - first) * progress;
            double wave = Math.sin(progress * Math.PI * 4.0) * amplitude * (1.0 - Math.abs(0.5 - progress));
            timestamps[i] = start + Math.round((end - start) * progress);
            prices[i] = Math.max(0.0, trend + wave);
        }
        prices[count - 1] = current;

        String label = requestedSource == ApiSource.AUTO && currency != null
                ? currency.getSource()
                : requestedSource.getDisplayName();
        if (fromError) {
            label = label + " estimate";
        }
        return new ChartSeries(timestamps, prices, range, requestedSource, label, true);
    }

    private List<Point> parsePointArray(JSONArray array, long start, long stepMillis) {
        return parsePointArray(array, start, stepMillis, null);
    }

    private List<Point> parsePointArray(JSONArray array, long start, long stepMillis, JSONArray fallbackTimestamps) {
        List<Point> points = new ArrayList<>();
        if (array == null) {
            return points;
        }
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            long time = timestampFromArray(fallbackTimestamps, i, start + (i * stepMillis));
            double price = Double.NaN;

            if (value instanceof JSONArray) {
                JSONArray row = (JSONArray) value;
                time = normalizeTimestamp(parseLong(row.opt(0), time));
                price = parseDoubleValue(row.opt(1));
            } else if (value instanceof JSONObject) {
                JSONObject item = (JSONObject) value;
                time = normalizeTimestamp(firstLong(item, time, "timestamp", "time", "date", "openTime", "closeTime"));
                price = firstDouble(item, Double.NaN, "price", "close", "closePrice", "value", "current_price");
            } else {
                price = parseDoubleValue(value);
            }

            if (!Double.isNaN(price) && price > 0.0) {
                points.add(new Point(time, price));
            }
        }
        return points;
    }

    private List<Point> parseKlineArray(JSONArray array, int timestampIndex, int closeIndex) {
        List<Point> points = new ArrayList<>();
        if (array == null) {
            return points;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONArray row = array.optJSONArray(i);
            if (row == null) {
                continue;
            }
            long timestamp = normalizeTimestamp(parseLong(row.opt(timestampIndex), 0L));
            double close = parseDoubleValue(row.opt(closeIndex));
            if (timestamp > 0 && !Double.isNaN(close) && close > 0.0) {
                points.add(new Point(timestamp, close));
            }
        }
        return points;
    }

    private ChartSeries pointsToSeries(
            List<Point> points,
            ChartRange range,
            ApiSource source,
            String sourceLabel,
            boolean estimated
    ) throws IOException {
        if (points == null || points.size() < 2) {
            throw new IOException("Not enough chart points");
        }
        Collections.sort(points, Comparator.comparingLong(point -> point.timestamp));

        long[] timestamps = new long[points.size()];
        double[] prices = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            timestamps[i] = point.timestamp;
            prices[i] = point.price;
        }
        return new ChartSeries(timestamps, prices, range, source, sourceLabel, estimated);
    }

    private String request(String method, String endpoint, String body, String token) throws IOException {
        return requestAbsolute(method, baseUrl + endpoint, body, token);
    }

    private String requestAbsolute(String method, String urlValue, String body, String token) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlValue);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "CryptoTrack Android");
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
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + ": " + response);
            }
            return response;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

    private JSONArray firstResultArray(JSONObject object) {
        if (object == null) {
            return new JSONArray();
        }
        JSONArray names = object.names();
        if (names == null) {
            return new JSONArray();
        }
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            if ("last".equalsIgnoreCase(key)) {
                continue;
            }
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
            double parsed = parseDoubleValue(value);
            if (!Double.isNaN(parsed)) {
                return parsed;
            }
        }
        return fallback;
    }

    private long firstLong(JSONObject object, long fallback, String... keys) {
        for (String key : keys) {
            Object value = object.opt(key);
            long parsed = parseLong(value, Long.MIN_VALUE);
            if (parsed != Long.MIN_VALUE) {
                return parsed;
            }
        }
        return fallback;
    }

    private double parseDouble(String value) {
        double parsed = parseDoubleValue(value);
        return Double.isNaN(parsed) ? 0.0 : parsed;
    }

    private double parseDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            String clean = ((String) value).trim();
            if (clean.isEmpty() || "null".equalsIgnoreCase(clean)) {
                return Double.NaN;
            }
            try {
                return Double.parseDouble(clean);
            } catch (NumberFormatException ignored) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private long parseLong(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String clean = ((String) value).trim();
            if (clean.isEmpty() || "null".equalsIgnoreCase(clean)) {
                return fallback;
            }
            try {
                return Long.parseLong(clean);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private long timestampFromArray(JSONArray timestamps, int index, long fallback) {
        if (timestamps == null || index >= timestamps.length()) {
            return fallback;
        }
        return normalizeTimestamp(parseLong(timestamps.opt(index), fallback));
    }

    private long normalizeTimestamp(long timestamp) {
        if (timestamp > 0 && timestamp < 10_000_000_000L) {
            return timestamp * 1000L;
        }
        return timestamp;
    }

    private String coingeckoId(CryptoCurrency currency) {
        if (currency == null) {
            return null;
        }
        if (ApiSource.fromCurrencySource(currency.getSource()) == ApiSource.COINGECKO) {
            return currency.getId();
        }
        return COINGECKO_IDS_BY_SYMBOL.get(currency.getSymbol().toUpperCase(Locale.US));
    }

    private String tradingPair(CryptoCurrency currency) {
        String symbol = currency != null ? currency.getSymbol() : "BTC";
        String upper = symbol.toUpperCase(Locale.US);
        return upper.endsWith("USDT") ? upper : upper + "USDT";
    }

    private String krakenPair(CryptoCurrency currency) {
        String symbol = currency != null ? currency.getSymbol() : "BTC";
        String upper = symbol.toUpperCase(Locale.US);
        String pair = KRAKEN_PAIRS_BY_SYMBOL.get(upper);
        return pair != null ? pair : upper + "USD";
    }

    private int krakenInterval(ChartRange range) {
        if (range == ChartRange.DAY) {
            return 15;
        }
        if (range == ChartRange.MONTH) {
            return 240;
        }
        return 60;
    }

    private String krakenSymbolFromPairKey(String pairKey) {
        if (pairKey == null) {
            return null;
        }
        String upper = pairKey.toUpperCase(Locale.US);
        if (upper.contains("XBT")) {
            return "BTC";
        }
        if (upper.contains("XDG")) {
            return "DOGE";
        }
        for (String symbol : KRAKEN_PAIRS_BY_SYMBOL.keySet()) {
            if (upper.contains(symbol)) {
                return symbol;
            }
        }
        return null;
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

    private String safePathSegment(String value) throws IOException {
        return encodeQuery(value == null ? "" : value);
    }

    private String encodeQuery(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }

    private <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postError(ApiCallback<T> callback, Exception error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private static class Point {
        final long timestamp;
        final double price;

        Point(long timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }
}
