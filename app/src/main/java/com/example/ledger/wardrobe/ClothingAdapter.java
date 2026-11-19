package com.example.ledger.wardrobe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.R;
import com.example.ledger.data.ClothingItem;
import com.example.ledger.databinding.ItemClothingBinding;
import com.example.ledger.databinding.ItemWardrobeEmptyBinding;

import java.util.ArrayList;
import java.util.List;

public class ClothingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Listener {
        void onClick(ClothingItem item);
    }

    private static final int CLOTHING = 1;
    private static final int EMPTY = 2;

    private final Listener listener;
    private List<ClothingItem> items = new ArrayList<>();
    private boolean loaded;

    public ClothingAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ClothingItem> next, boolean loaded) {
        this.items = next == null ? new ArrayList<>() : next;
        this.loaded = loaded;
        notifyDataSetChanged();
    }

    public boolean fullSpan(int position) {
        return items.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        return items.isEmpty() ? EMPTY : CLOTHING;
    }

    @Override
    public int getItemCount() {
        if (!loaded) {
            return 0;
        }
        return items.isEmpty() ? 1 : items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == EMPTY) {
            return new EmptyHolder(ItemWardrobeEmptyBinding.inflate(inflater, parent, false));
        }
        return new ClothingHolder(ItemClothingBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyHolder) {
            ((EmptyHolder) holder).binding.title.setText(R.string.clothing_empty_title);
            ((EmptyHolder) holder).binding.subtitle.setText(R.string.clothing_empty_subtitle);
            return;
        }
        ClothingItem item = items.get(position);
        ClothingHolder row = (ClothingHolder) holder;
        row.binding.title.setText(item.category + " · " + item.color);
        ClothingImages.load(row.binding.photo, item.imagePath, WardrobeViewModel.colorOf(item.color));
        if (item.status == ClothingItem.READY) {
            row.binding.badge.setVisibility(View.GONE);
        } else {
            row.binding.badge.setVisibility(View.VISIBLE);
            int label = R.string.status_laundry;
            int color = row.itemView.getContext().getColor(R.color.expense);
            if (item.status == ClothingItem.WASHING) {
                label = R.string.status_washing;
            } else if (item.status == ClothingItem.BACK) {
                label = R.string.status_back;
                color = row.itemView.getContext().getColor(R.color.income);
            }
            row.binding.badge.setText(label);
            row.binding.badge.setBackgroundColor(color);
        }
        row.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    private static final class ClothingHolder extends RecyclerView.ViewHolder {
        private final ItemClothingBinding binding;

        private ClothingHolder(ItemClothingBinding binding) {
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
