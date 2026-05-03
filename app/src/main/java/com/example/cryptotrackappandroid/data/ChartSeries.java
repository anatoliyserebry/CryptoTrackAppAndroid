package com.example.cryptotrackappandroid.data;

public class ChartSeries {
    private final long[] timestamps;
    private final double[] prices;
    private final ChartRange range;
    private final ApiSource source;
    private final String sourceLabel;
    private final boolean estimated;

    public ChartSeries(
            long[] timestamps,
            double[] prices,
            ChartRange range,
            ApiSource source,
            String sourceLabel,
            boolean estimated
    ) {
        this.timestamps = timestamps != null ? timestamps : new long[0];
        this.prices = prices != null ? prices : new double[0];
        this.range = range;
        this.source = source;
        this.sourceLabel = sourceLabel;
        this.estimated = estimated;
    }

    public long[] getTimestamps() {
        return timestamps;
    }

    public double[] getPrices() {
        return prices;
    }

    public ChartRange getRange() {
        return range;
    }

    public ApiSource getSource() {
        return source;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public boolean isEstimated() {
        return estimated;
    }

    public double getFirstPrice() {
        return prices.length > 0 ? prices[0] : 0.0;
    }

    public double getLastPrice() {
        return prices.length > 0 ? prices[prices.length - 1] : 0.0;
    }

    public double getChangePercent() {
        double first = getFirstPrice();
        double last = getLastPrice();
        if (first <= 0.0 || last <= 0.0) {
            return 0.0;
        }
        return ((last - first) / first) * 100.0;
    }

    public boolean isPositive() {
        return getChangePercent() >= 0.0;
    }

    public double getMinPrice() {
        if (prices.length == 0) {
            return 0.0;
        }
        double min = prices[0];
        for (double price : prices) {
            min = Math.min(min, price);
        }
        return min;
    }

    public double getMaxPrice() {
        if (prices.length == 0) {
            return 0.0;
        }
        double max = prices[0];
        for (double price : prices) {
            max = Math.max(max, price);
        }
        return max;
    }
}
