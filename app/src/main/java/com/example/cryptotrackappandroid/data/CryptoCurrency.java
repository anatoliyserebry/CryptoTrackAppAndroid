package com.example.cryptotrackappandroid.data;

import java.io.Serializable;

public class CryptoCurrency implements Serializable {
    private final String id;
    private final String symbol;
    private final String name;
    private final double priceUsd;
    private final double changePercent24h;
    private final double marketCapUsd;
    private final double volume24hUsd;
    private final double[] history;
    private final String source;
    private boolean favorite;

    public CryptoCurrency(
            String id,
            String symbol,
            String name,
            double priceUsd,
            double changePercent24h,
            double marketCapUsd,
            double volume24hUsd,
            double[] history,
            boolean favorite
    ) {
        this(id, symbol, name, priceUsd, changePercent24h, marketCapUsd, volume24hUsd, history, favorite, "FastAPI");
    }

    public CryptoCurrency(
            String id,
            String symbol,
            String name,
            double priceUsd,
            double changePercent24h,
            double marketCapUsd,
            double volume24hUsd,
            double[] history,
            boolean favorite,
            String source
    ) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.priceUsd = priceUsd;
        this.changePercent24h = changePercent24h;
        this.marketCapUsd = marketCapUsd;
        this.volume24hUsd = volume24hUsd;
        this.history = history;
        this.favorite = favorite;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getPriceUsd() {
        return priceUsd;
    }

    public double getChangePercent24h() {
        return changePercent24h;
    }

    public double getMarketCapUsd() {
        return marketCapUsd;
    }

    public double getVolume24hUsd() {
        return volume24hUsd;
    }

    public double[] getHistory() {
        return history;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getSource() {
        return source;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
