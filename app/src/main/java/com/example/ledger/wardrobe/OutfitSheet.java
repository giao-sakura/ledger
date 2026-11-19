package com.example.ledger.wardrobe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.R;
import com.example.ledger.data.ClothingItem;
import com.example.ledger.databinding.ItemClothingBinding;
import com.example.ledger.databinding.SheetOutfitBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OutfitSheet extends BottomSheetDialogFragment {
    private SheetOutfitBinding binding;
    private WardrobeViewModel viewModel;
    private final Set<Long> selected = new HashSet<>();
    private final PickerAdapter adapter = new PickerAdapter();

    public static void open(FragmentManager manager) {
        new OutfitSheet().show(manager, "outfit");
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Ledger_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetOutfitBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WardrobeViewModel.class);
        binding.grid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.grid.setAdapter(adapter);
        binding.showWashing.setOnCheckedChangeListener((button, checked) -> {
            if (!checked) {
                List<ClothingItem> all = viewModel.allClothes();
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).status == ClothingItem.WASHING) {
                        selected.remove(all.get(i).id);
                    }
                }
            }
            adapter.show(visibleItems());
        });
        binding.saveButton.setOnClickListener(v -> save());
        adapter.show(visibleItems());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!(getDialog() instanceof BottomSheetDialog)) {
            return;
        }
        FrameLayout bottom = ((BottomSheetDialog) getDialog()).findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottom == null) {
            return;
        }
        bottom.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottom);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private List<ClothingItem> visibleItems() {
        List<ClothingItem> all = viewModel.allClothes();
        boolean includeWashing = binding != null && binding.showWashing.isChecked();
        List<ClothingItem> shown = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            ClothingItem item = all.get(i);
            if (!includeWashing && item.status == ClothingItem.WASHING) {
                continue;
            }
            shown.add(item);
        }
        return shown;
    }

    private void save() {
        if (binding == null || viewModel == null) {
            return;
        }
        String name = binding.name.getText().toString().trim();
        if (name.isEmpty()) {
            binding.name.setError(getString(R.string.outfit_name_error));
            return;
        }
        if (selected.isEmpty()) {
            binding.name.setError(getString(R.string.outfit_pick_error));
            return;
        }
        viewModel.saveOutfit(name, new ArrayList<>(selected));
        dismiss();
    }

    private final class PickerAdapter extends RecyclerView.Adapter<PickerAdapter.Holder> {
        private List<ClothingItem> items = new ArrayList<>();

        void show(List<ClothingItem> next) {
            items = next;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemClothingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            ClothingItem item = items.get(position);
            boolean on = selected.contains(item.id);
            holder.binding.title.setText(item.category);
            ClothingImages.load(holder.binding.photo, item.imagePath, WardrobeViewModel.colorOf(item.color));
            holder.binding.photo.setAlpha(item.status == ClothingItem.WASHING ? 0.35f : 1f);
            holder.binding.badge.setVisibility(View.VISIBLE);
            if (on) {
                holder.binding.badge.setText(R.string.outfit_selected);
                holder.binding.badge.setBackgroundColor(holder.itemView.getContext().getColor(R.color.income));
            } else if (item.status == ClothingItem.WASHING) {
                holder.binding.badge.setText(R.string.status_washing);
                holder.binding.badge.setBackgroundColor(holder.itemView.getContext().getColor(R.color.expense));
            } else if (item.status == ClothingItem.LAUNDRY) {
                holder.binding.badge.setText(R.string.status_laundry);
                holder.binding.badge.setBackgroundColor(holder.itemView.getContext().getColor(R.color.expense));
            } else {
                holder.binding.badge.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> {
                if (selected.contains(item.id)) {
                    selected.remove(item.id);
                } else {
                    selected.add(item.id);
                }
                notifyItemChanged(holder.getBindingAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            private final ItemClothingBinding binding;

            private Holder(ItemClothingBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
