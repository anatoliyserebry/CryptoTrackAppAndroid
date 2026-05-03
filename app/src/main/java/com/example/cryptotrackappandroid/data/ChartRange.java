package com.example.cryptotrackappandroid.data;

public enum ChartRange {
    DAY("1D", "1 day", 1, 15L * 60L * 1000L, "15", "15m", 100),
    WEEK("7D", "7 days", 7, 60L * 60L * 1000L, "60", "60m", 170),
    MONTH("30D", "30 days", 30, 4L * 60L * 60L * 1000L, "240", "4h", 190);

    private final String key;
    private final String label;
    private final int days;
    private final long candleMillis;
    private final String bybitInterval;
    private final String mexcInterval;
    private final int limit;

    ChartRange(
            String key,
            String label,
            int days,
            long candleMillis,
            String bybitInterval,
            String mexcInterval,
            int limit
    ) {
        this.key = key;
        this.label = label;
        this.days = days;
        this.candleMillis = candleMillis;
        this.bybitInterval = bybitInterval;
        this.mexcInterval = mexcInterval;
        this.limit = limit;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public int getDays() {
        return days;
    }

    public long getCandleMillis() {
        return candleMillis;
    }

    public String getBybitInterval() {
        return bybitInterval;
    }

    public String getMexcInterval() {
        return mexcInterval;
    }

    public int getLimit() {
        return limit;
    }

    public long durationMillis() {
        return days * 24L * 60L * 60L * 1000L;
    }
}
