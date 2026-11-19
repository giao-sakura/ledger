package com.example.ledger.wardrobe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.R;
import com.example.ledger.data.ClothingItem;
import com.example.ledger.databinding.ItemOutfitBinding;
import com.example.ledger.databinding.ItemWardrobeEmptyBinding;

import java.util.ArrayList;
import java.util.List;

public class OutfitAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Listener {
        void onClick(WardrobeUi.OutfitCard card);
    }

    private final Listener listener;
    private List<WardrobeUi.OutfitCard> cards = new ArrayList<>();
    private boolean loaded;

    public OutfitAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<WardrobeUi.OutfitCard> next, boolean loaded) {
        this.cards = next == null ? new ArrayList<>() : next;
        this.loaded = loaded;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (!loaded) {
            return 0;
        }
        return cards.isEmpty() ? 1 : cards.size();
    }

    @Override
    public int getItemViewType(int position) {
        return cards.isEmpty() ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new EmptyHolder(ItemWardrobeEmptyBinding.inflate(inflater, parent, false));
        }
        return new OutfitHolder(ItemOutfitBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyHolder) {
            ((EmptyHolder) holder).binding.title.setText(R.string.outfit_empty_title);
            ((EmptyHolder) holder).binding.subtitle.setText(R.string.outfit_empty_subtitle);
            return;
        }
        WardrobeUi.OutfitCard card = cards.get(position);
        OutfitHolder row = (OutfitHolder) holder;
        row.binding.name.setText(card.outfit.name);
        row.binding.washing.setVisibility(card.hasWashing() ? View.VISIBLE : View.GONE);
        if (card.weather == null || card.weather.isEmpty()) {
            row.binding.weather.setVisibility(View.GONE);
        } else {
            row.binding.weather.setVisibility(View.VISIBLE);
            row.binding.weather.setText(card.weather);
            int color = R.color.ink_soft;
            if (card.fitLevel == WeatherFit.UNSUITABLE) {
                color = R.color.expense;
            } else if (card.fitLevel == WeatherFit.OK) {
                color = R.color.income;
            }
            row.binding.weather.setTextColor(ContextCompat.getColor(row.itemView.getContext(), color));
        }
        row.binding.photos.removeAllViews();
        int size = Math.round(72 * row.itemView.getResources().getDisplayMetrics().density);
        int gap = Math.round(8 * row.itemView.getResources().getDisplayMetrics().density);
        for (int i = 0; i < card.items.size(); i++) {
            ClothingItem item = card.items.get(i);
            ImageView image = new ImageView(row.itemView.getContext());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearParams(image, size, i == 0 ? 0 : gap);
            ClothingImages.load(image, item.imagePath, WardrobeViewModel.colorOf(item.color));
            row.binding.photos.addView(image);
        }
        row.itemView.setOnClickListener(v -> listener.onClick(card));
    }

    private static void LinearParams(ImageView image, int size, int marginStart) {
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(size, size);
        params.setMarginStart(marginStart);
        image.setLayoutParams(params);
    }

    private static final class OutfitHolder extends RecyclerView.ViewHolder {
        private final ItemOutfitBinding binding;

        private OutfitHolder(ItemOutfitBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final class EmptyHolder extends RecyclerView.ViewHolder {
        private final ItemWardrobeEmptyBinding binding;

        private EmptyHolder(ItemWardrobeEmptyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
