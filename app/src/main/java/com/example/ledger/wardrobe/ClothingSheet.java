package com.example.ledger.wardrobe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.ledger.R;
import com.example.ledger.data.ClothingItem;
import com.example.ledger.databinding.SheetClothingBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;

public class ClothingSheet extends BottomSheetDialogFragment {
    private static final String EXTRA_ID = "id";
    private static final String EXTRA_WISH = "wish";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_NOTE = "note";
    private static final String[] CATEGORIES = {"外套", "内搭", "裤子", "裙子", "鞋子", "配饰"};
    private static final String[] COLORS = {"黑", "白", "灰", "米", "卡其", "棕", "红", "橙", "黄", "绿", "蓝", "紫", "粉"};
    private static final String[] STYLES = {"休闲", "通勤", "正式", "运动", "约会", "居家"};
    private static final String[] SEASONS = {"春", "夏", "秋", "冬"};
    private static final int[] SEASON_BITS = {
            ClothingItem.SPRING, ClothingItem.SUMMER, ClothingItem.AUTUMN, ClothingItem.WINTER
    };
    private static final int[] STATUSES = {
            ClothingItem.READY, ClothingItem.LAUNDRY, ClothingItem.WASHING, ClothingItem.BACK
    };

    private final Map<Integer, String> textByChip = new HashMap<>();
    private final Map<Integer, Integer> valueByChip = new HashMap<>();
    private SheetClothingBinding binding;
    private WardrobeViewModel viewModel;
    private long id;
    private long wishId;
    private String imagePath;
    private String originalPath;
    private long createdAt;
    private boolean saved;

    private final androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest> picker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null || binding == null) {
                    return;
                }
                try {
                    String previous = imagePath;
                    imagePath = ClothingImages.importUri(requireContext(), uri);
                    if (previous != null && !previous.equals(originalPath)) {
                        ClothingImages.delete(previous);
                    }
                    ClothingImages.load(binding.photo, imagePath, currentColor());
                } catch (Exception ignored) {
                    // Keep the previous photo when the picker result cannot be read.
                }
            });

    public static void open(FragmentManager manager, @Nullable ClothingItem item, long wishId, @Nullable String category, @Nullable String note) {
        ClothingSheet sheet = new ClothingSheet();
        Bundle args = new Bundle();
        if (item != null) {
            args.putLong(EXTRA_ID, item.id);
        }
        args.putLong(EXTRA_WISH, wishId);
        args.putString(EXTRA_CATEGORY, category);
        args.putString(EXTRA_NOTE, note);
        sheet.setArguments(args);
        sheet.show(manager, "clothing");
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Ledger_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetClothingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WardrobeViewModel.class);
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        id = args.getLong(EXTRA_ID);
        wishId = args.getLong(EXTRA_WISH);
        ClothingItem existing = id > 0 ? viewModel.find(id) : null;
        if (existing != null) {
            imagePath = existing.imagePath;
            originalPath = existing.imagePath;
            createdAt = existing.createdAt;
        }
        binding.title.setText(existing == null ? R.string.wardrobe_add : R.string.clothing_edit);
        binding.deleteButton.setVisibility(existing == null ? View.GONE : View.VISIBLE);
        String category = existing == null ? args.getString(EXTRA_CATEGORY) : existing.category;
        String color = existing == null ? "黑" : existing.color;
        String style = existing == null ? "休闲" : existing.style;
        int seasons = existing == null ? ClothingItem.SPRING | ClothingItem.AUTUMN : existing.seasons;
        int status = existing == null ? ClothingItem.READY : existing.status;
        addChoices(binding.categories, CATEGORIES, category, true);
        addChoices(binding.colors, COLORS, color, true);
        addChoices(binding.styles, STYLES, style, true);
        addSeasons(seasons);
        addStatuses(status);
        if (existing != null && existing.note != null) {
            binding.note.setText(existing.note);
        } else if (args.getString(EXTRA_NOTE) != null) {
            binding.note.setText(args.getString(EXTRA_NOTE));
        }
        ClothingImages.load(binding.photo, imagePath, WardrobeViewModel.colorOf(color));
        binding.photo.setOnClickListener(v -> picker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));
        binding.colors.setOnCheckedStateChangeListener((group, checkedIds) ->
                ClothingImages.load(binding.photo, imagePath, currentColor()));
        binding.saveButton.setOnClickListener(v -> save());
        binding.deleteButton.setOnClickListener(v -> confirmDelete());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            FrameLayout bottom = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottom != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottom);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (!saved && imagePath != null && !imagePath.equals(originalPath)) {
            ClothingImages.delete(imagePath);
        }
        binding = null;
        super.onDestroyView();
    }

    private void save() {
        if (binding == null || viewModel == null) {
            return;
        }
        ClothingItem item = new ClothingItem();
        item.id = id;
        item.imagePath = imagePath;
        item.category = selectedText(binding.categories, CATEGORIES[0]);
        item.color = selectedText(binding.colors, COLORS[0]);
        item.style = selectedText(binding.styles, STYLES[0]);
        item.seasons = selectedSeasons();
        item.status = selectedStatus();
        item.note = binding.note.getText().toString().trim();
        item.createdAt = createdAt;
        saved = true;
        viewModel.saveClothing(item, wishId);
        dismiss();
    }

    private void confirmDelete() {
        ClothingItem item = viewModel == null ? null : viewModel.find(id);
        if (item == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clothing_delete)
                .setMessage(R.string.clothing_delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteClothing(item);
                    dismiss();
                })
                .show();
    }

    private void addChoices(com.google.android.material.chip.ChipGroup group, String[] labels, String selected, boolean single) {
        group.removeAllViews();
        group.setSingleSelection(single);
        group.setSelectionRequired(single);
        for (String label : labels) {
            Chip chip = chip(label);
            textByChip.put(chip.getId(), label);
            group.addView(chip);
            if (label.equals(selected)) {
                chip.setChecked(true);
            }
        }
    }

    private void addSeasons(int selected) {
        binding.seasons.removeAllViews();
        for (int i = 0; i < SEASONS.length; i++) {
            Chip chip = chip(SEASONS[i]);
            valueByChip.put(chip.getId(), SEASON_BITS[i]);
            binding.seasons.addView(chip);
            if ((selected & SEASON_BITS[i]) != 0) {
                chip.setChecked(true);
            }
        }
    }

    private void addStatuses(int selected) {
        int[] labels = {R.string.status_ready, R.string.status_laundry, R.string.status_washing, R.string.status_back};
        binding.statuses.removeAllViews();
        for (int i = 0; i < STATUSES.length; i++) {
            Chip chip = chip(getString(labels[i]));
            valueByChip.put(chip.getId(), STATUSES[i]);
            binding.statuses.addView(chip);
            if (STATUSES[i] == selected) {
                chip.setChecked(true);
            }
        }
    }

    private Chip chip(String label) {
        Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        return chip;
    }

    private String selectedText(com.google.android.material.chip.ChipGroup group, String fallback) {
        String value = textByChip.get(group.getCheckedChipId());
        return value == null ? fallback : value;
    }

    private int selectedSeasons() {
        int mask = 0;
        for (int i = 0; i < binding.seasons.getChildCount(); i++) {
            View child = binding.seasons.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                Integer bit = valueByChip.get(child.getId());
                if (bit != null) {
                    mask |= bit;
                }
            }
        }
        return mask;
    }

    private int selectedStatus() {
        Integer value = valueByChip.get(binding.statuses.getCheckedChipId());
        return value == null ? ClothingItem.READY : value;
    }

    private int currentColor() {
        return WardrobeViewModel.colorOf(selectedText(binding.colors, COLORS[0]));
    }
}
