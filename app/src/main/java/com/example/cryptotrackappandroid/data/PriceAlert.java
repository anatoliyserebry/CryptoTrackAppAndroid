package com.example.cryptotrackappandroid.data;

public class PriceAlert {
    private final String symbol;
    private final double targetPriceUsd;
    private final boolean triggerAbove;
    private final boolean triggered;

    public PriceAlert(String symbol, double targetPriceUsd, boolean triggerAbove, boolean triggered) {
        this.symbol = symbol;
        this.targetPriceUsd = targetPriceUsd;
        this.triggerAbove = triggerAbove;
        this.triggered = triggered;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getTargetPriceUsd() {
        return targetPriceUsd;
    }

    public boolean isTriggerAbove() {
        return triggerAbove;
    }

    public boolean isTriggered() {
        return triggered;
    }
}
