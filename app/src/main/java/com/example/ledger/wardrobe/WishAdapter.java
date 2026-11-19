package com.example.ledger.wardrobe;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.R;
import com.example.ledger.data.WishItem;
import com.example.ledger.databinding.ItemWardrobeEmptyBinding;
import com.example.ledger.databinding.ItemWishBinding;

import java.util.ArrayList;
import java.util.List;

public class WishAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Listener {
        void onBought(WishItem item);

        void onDelete(WishItem item);
    }

    private final Listener listener;
    private List<WishItem> items = new ArrayList<>();
    private boolean loaded;

    public WishAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<WishItem> next, boolean loaded) {
        this.items = next == null ? new ArrayList<>() : next;
        this.loaded = loaded;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (!loaded) {
            return 0;
        }
        return items.isEmpty() ? 1 : items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.isEmpty() ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new EmptyHolder(ItemWardrobeEmptyBinding.inflate(inflater, parent, false));
        }
        return new WishHolder(ItemWishBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyHolder) {
            ((EmptyHolder) holder).binding.title.setText(R.string.wish_empty_title);
            ((EmptyHolder) holder).binding.subtitle.setText(R.string.wish_empty_subtitle);
            return;
        }
        WishItem item = items.get(position);
        WishHolder row = (WishHolder) holder;
        row.binding.need.setText(item.need);
        row.binding.category.setText(item.category == null ? "" : item.category);
        row.binding.bought.setOnClickListener(v -> listener.onBought(item));
        row.binding.remove.setOnClickListener(v -> listener.onDelete(item));
    }

    private static final class WishHolder extends RecyclerView.ViewHolder {
        private final ItemWishBinding binding;

        private WishHolder(ItemWishBinding binding) {
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
