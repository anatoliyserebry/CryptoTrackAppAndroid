package com.example.cryptotrackappandroid.data;

import java.util.ArrayList;
import java.util.List;

public final class DemoData {
    private DemoData() {
    }

    public static List<CryptoCurrency> currencies() {
        List<CryptoCurrency> items = new ArrayList<>();
        items.add(new CryptoCurrency("demo-bitcoin", "BTC", "Bitcoin", 64250.45, 2.84, 1260000000000.0, 32100000000.0, history(59400, 60220, 61580, 60740, 62200, 63120, 64250), false, "Demo"));
        items.add(new CryptoCurrency("demo-ethereum", "ETH", "Ethereum", 3184.72, -1.16, 382000000000.0, 16400000000.0, history(3310, 3285, 3248, 3220, 3198, 3160, 3184), false, "Demo"));
        items.add(new CryptoCurrency("demo-solana", "SOL", "Solana", 146.38, 4.92, 65300000000.0, 4200000000.0, history(132, 137, 141, 139, 144, 145, 146), false, "Demo"));
        items.add(new CryptoCurrency("demo-bnb", "BNB", "BNB", 592.14, 1.86, 87500000000.0, 1900000000.0, history(570, 576, 583, 579, 586, 590, 592), false, "Demo"));
        items.add(new CryptoCurrency("demo-cardano", "ADA", "Cardano", 0.58, -2.38, 20500000000.0, 735000000.0, history(0.62, 0.61, 0.60, 0.59, 0.585, 0.575, 0.58), false, "Demo"));
        items.add(new CryptoCurrency("demo-ripple", "XRP", "XRP", 0.62, 1.47, 34100000000.0, 1400000000.0, history(0.59, 0.60, 0.61, 0.605, 0.615, 0.618, 0.62), false, "Demo"));
        items.add(new CryptoCurrency("demo-dogecoin", "DOGE", "Dogecoin", 0.17, 7.25, 24500000000.0, 2800000000.0, history(0.145, 0.151, 0.158, 0.160, 0.166, 0.171, 0.17), false, "Demo"));
        items.add(new CryptoCurrency("demo-avalanche", "AVAX", "Avalanche", 34.84, -0.91, 13900000000.0, 610000000.0, history(35.9, 35.4, 35.1, 34.7, 34.3, 34.6, 34.8), false, "Demo"));
        items.add(new CryptoCurrency("demo-polkadot", "DOT", "Polkadot", 7.24, -0.64, 9700000000.0, 310000000.0, history(7.5, 7.41, 7.35, 7.28, 7.21, 7.18, 7.24), false, "Demo"));
        items.add(new CryptoCurrency("demo-chainlink", "LINK", "Chainlink", 15.92, 3.31, 9400000000.0, 520000000.0, history(14.9, 15.1, 15.45, 15.3, 15.72, 15.84, 15.92), false, "Demo"));
        return items;
    }

    private static double[] history(double... values) {
        return values;
    }
}
