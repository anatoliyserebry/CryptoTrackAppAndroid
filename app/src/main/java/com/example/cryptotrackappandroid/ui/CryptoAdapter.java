package com.example.cryptotrackappandroid.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptotrackappandroid.R;
import com.example.cryptotrackappandroid.data.CryptoCurrency;

import java.util.ArrayList;
import java.util.List;

public class CryptoAdapter extends RecyclerView.Adapter<CryptoAdapter.CryptoViewHolder> {
    public interface Listener {
        void onCryptoClick(CryptoCurrency currency);

        void onFavoriteClick(CryptoCurrency currency);
    }

    private final List<CryptoCurrency> items = new ArrayList<>();
    private final Listener listener;

    public CryptoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<CryptoCurrency> currencies) {
        items.clear();
        items.addAll(currencies);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CryptoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crypto, parent, false);
        return new CryptoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CryptoViewHolder holder, int position) {
        CryptoCurrency currency = items.get(position);
        Context context = holder.itemView.getContext();
        boolean positive = currency.getChangePercent24h() >= 0;

        holder.avatarText.setText(Formatters.initials(currency.getSymbol()));
        holder.nameText.setText(currency.getName());
        holder.symbolText.setText(currency.getSymbol() + " / " + currency.getSource());
        holder.priceText.setText(Formatters.price(currency.getPriceUsd()));
        holder.changeText.setText(Formatters.change(currency.getChangePercent24h()));
        holder.changeText.setTextColor(ContextCompat.getColor(context, positive ? R.color.positive : R.color.negative));
        holder.changeText.setBackgroundResource(positive ? R.drawable.bg_change_positive : R.drawable.bg_change_negative);
        holder.favoriteButton.setImageResource(currency.isFavorite() ? R.drawable.ic_star_24 : R.drawable.ic_star_border_24);
        holder.sparklineView.setValues(currency.getHistory(), positive);

        holder.itemView.setOnClickListener(v -> listener.onCryptoClick(currency));
        holder.favoriteButton.setOnClickListener(v -> listener.onFavoriteClick(currency));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CryptoViewHolder extends RecyclerView.ViewHolder {
        final TextView avatarText;
        final TextView nameText;
        final TextView symbolText;
        final TextView priceText;
        final TextView changeText;
        final ImageButton favoriteButton;
        final SparklineView sparklineView;

        CryptoViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarText = itemView.findViewById(R.id.coinAvatarText);
            nameText = itemView.findViewById(R.id.nameText);
            symbolText = itemView.findViewById(R.id.symbolText);
            priceText = itemView.findViewById(R.id.priceText);
            changeText = itemView.findViewById(R.id.changeText);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            sparklineView = itemView.findViewById(R.id.sparklineView);
        }
    }
}
