package com.example.ledger;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.data.ClothingItem;
import com.example.ledger.data.DiaryEntry;
import com.example.ledger.data.Transaction;
import com.example.ledger.data.WishItem;
import com.example.ledger.databinding.ActivityMainBinding;
import com.example.ledger.ui.DiaryAdapter;
import com.example.ledger.ui.DiarySheet;
import com.example.ledger.ui.DiaryUiState;
import com.example.ledger.ui.DiaryViewModel;
import com.example.ledger.ui.EditorSheet;
import com.example.ledger.ui.LedgerAdapter;
import com.example.ledger.ui.LedgerUiState;
import com.example.ledger.ui.LedgerViewModel;
import com.example.ledger.wardrobe.ClothingAdapter;
import com.example.ledger.wardrobe.ClothingSheet;
import com.example.ledger.wardrobe.OutfitAdapter;
import com.example.ledger.wardrobe.OutfitSheet;
import com.example.ledger.wardrobe.WardrobeUi;
import com.example.ledger.wardrobe.WardrobeViewModel;
import com.example.ledger.wardrobe.WishAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private static final int SECTION_LEDGER = 0;
    private static final int SECTION_DIARY = 1;
    private static final int SECTION_WARDROBE = 2;
    private static final int TAB_CLOTHES = 0;
    private static final int TAB_OUTFITS = 1;
    private static final int TAB_WISHES = 2;
    private static final String[] CLOTHING_CATEGORIES = {"外套", "内搭", "裤子", "裙子", "鞋子", "配饰"};
    private static final String[] CLOTHING_COLORS = {"黑", "白", "灰", "米", "卡其", "棕", "红", "橙", "黄", "绿", "蓝", "紫", "粉"};

    private ActivityMainBinding binding;
    private LedgerViewModel viewModel;
    private DiaryViewModel diaryViewModel;
    private WardrobeViewModel wardrobeViewModel;
    private final LedgerAdapter adapter = new LedgerAdapter();
    private final DiaryAdapter diaryAdapter = new DiaryAdapter();
    private final ClothingAdapter clothingAdapter = new ClothingAdapter(this::editClothing);
    private final OutfitAdapter outfitAdapter = new OutfitAdapter(this::openOutfit);
    private final WishAdapter wishAdapter = new WishAdapter(new WishAdapter.Listener() {
        @Override
        public void onBought(WishItem item) {
            ClothingSheet.open(getSupportFragmentManager(), null, item.id, item.category, item.need);
        }

        @Override
        public void onDelete(WishItem item) {
            wardrobeViewModel.deleteWish(item.id);
        }
    });
    private LinearLayoutManager listLayout;
    private GridLayoutManager clothingGrid;
    private int section = SECTION_LEDGER;
    private int wardrobeTab = TAB_CLOTHES;
    private String filterCategory = "";
    private int filterSeason;
    private String filterColor = "";
    private final Paint swipePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swipeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String deleteLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(LedgerViewModel.class);
        diaryViewModel = new ViewModelProvider(this).get(DiaryViewModel.class);
        wardrobeViewModel = new ViewModelProvider(this).get(WardrobeViewModel.class);
        deleteLabel = getString(R.string.delete);
        swipePaint.setColor(ContextCompat.getColor(this, R.color.swipe));
        swipeTextPaint.setColor(ContextCompat.getColor(this, R.color.white));
        swipeTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14,
                getResources().getDisplayMetrics()
        ));

        binding.settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        adapter.setListener(transaction -> EditorSheet.open(getSupportFragmentManager(), transaction));
        diaryAdapter.setListener(entry -> DiarySheet.open(getSupportFragmentManager(), entry, null));
        listLayout = new LinearLayoutManager(this);
        clothingGrid = new GridLayoutManager(this, 2);
        clothingGrid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return clothingAdapter.fullSpan(position) ? 2 : 1;
            }
        });
        binding.list.setLayoutManager(listLayout);
        binding.list.setAdapter(adapter);
        attachSwipe();
        buildWardrobeFilters();

        binding.prevMonth.setOnClickListener(v -> shiftMonth(-1));
        binding.nextMonth.setOnClickListener(v -> shiftMonth(1));
        binding.monthLabel.setOnClickListener(v -> showRangeDialog());
        binding.rangeHint.setOnClickListener(v -> showRangeDialog());
        binding.todayButton.setOnClickListener(v -> {
            viewModel.goToCurrentMonth();
            diaryViewModel.goToCurrentMonth();
        });
        binding.fab.setOnClickListener(v -> {
            if (section == SECTION_DIARY) {
                DiarySheet.open(getSupportFragmentManager(), null, diaryViewModel.suggestedDate());
            } else if (section == SECTION_WARDROBE) {
                openWardrobeAdd();
            } else {
                EditorSheet.open(getSupportFragmentManager(), null);
            }
        });
        binding.sectionGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            int next = SECTION_LEDGER;
            if (checkedId == R.id.sectionDiary) {
                next = SECTION_DIARY;
            } else if (checkedId == R.id.sectionWardrobe) {
                next = SECTION_WARDROBE;
            }
            showSection(next);
        });
        binding.wardrobeTabs.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            wardrobeTab = TAB_CLOTHES;
            if (checkedId == R.id.wardrobeOutfits) {
                wardrobeTab = TAB_OUTFITS;
            } else if (checkedId == R.id.wardrobeWishes) {
                wardrobeTab = TAB_WISHES;
            }
            binding.clothingFilters.setVisibility(wardrobeTab == TAB_CLOTHES ? View.VISIBLE : View.GONE);
            if (section == SECTION_WARDROBE) {
                applyList();
            }
        });
        binding.cityButton.setOnClickListener(v -> showCityDialog());
        binding.filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            int filter = LedgerUiState.FILTER_ALL;
            if (checkedId == R.id.chipExpense) {
                filter = LedgerUiState.FILTER_EXPENSE;
            } else if (checkedId == R.id.chipIncome) {
                filter = LedgerUiState.FILTER_INCOME;
            }
            viewModel.setFilter(filter);
        });
        getSupportFragmentManager().setFragmentResultListener(EditorSheet.REQUEST, this, (requestKey, bundle) -> {
            String action = bundle.getString(EditorSheet.EXTRA_ACTION);
            if (EditorSheet.ACTION_DELETE.equals(action)) {
                Transaction transaction = viewModel.find(bundle.getLong(EditorSheet.EXTRA_ID));
                if (transaction != null) {
                    Transaction snapshot = transaction.copy();
                    viewModel.remove(snapshot);
                    showUndo(snapshot);
                }
                return;
            }
            if (EditorSheet.ACTION_SAVE.equals(action)) {
                viewModel.save(
                        bundle.getLong(EditorSheet.EXTRA_ID),
                        bundle.getInt(EditorSheet.EXTRA_TYPE),
                        bundle.getLong(EditorSheet.EXTRA_AMOUNT),
                        bundle.getString(EditorSheet.EXTRA_CATEGORY),
                        bundle.getString(EditorSheet.EXTRA_NOTE),
                        bundle.getLong(EditorSheet.EXTRA_TIME)
                );
            }
        });
        getSupportFragmentManager().setFragmentResultListener(DiarySheet.REQUEST, this, (requestKey, bundle) -> {
            String action = bundle.getString(DiarySheet.EXTRA_ACTION);
            if (DiarySheet.ACTION_DELETE.equals(action)) {
                DiaryEntry entry = diaryViewModel.find(bundle.getLong(DiarySheet.EXTRA_ID));
                if (entry != null) {
                    DiaryEntry snapshot = entry.copy();
                    diaryViewModel.remove(snapshot);
                    showDiaryUndo(snapshot);
                }
                return;
            }
            if (DiarySheet.ACTION_SAVE.equals(action)) {
                diaryViewModel.save(
                        bundle.getLong(DiarySheet.EXTRA_ID),
                        bundle.getString(DiarySheet.EXTRA_BODY),
                        bundle.getLong(DiarySheet.EXTRA_TIME)
                );
            }
        });
        viewModel.getUi().observe(this, this::render);
        diaryViewModel.getUi().observe(this, this::renderDiary);
        wardrobeViewModel.getUi().observe(this, this::renderWardrobe);
        wardrobeViewModel.getNotice().observe(this, text -> {
            if (text == null || text.isEmpty()) {
                return;
            }
            Snackbar snackbar = Snackbar.make(binding.main, text, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(binding.fab);
            snackbar.show();
            wardrobeViewModel.consumeNotice();
        });
    }

    private void shiftMonth(int delta) {
        viewModel.shiftMonth(delta);
        diaryViewModel.shiftMonth(delta);
    }

    private void showSection(int next) {
        section = next;
        boolean ledger = next == SECTION_LEDGER;
        boolean diary = next == SECTION_DIARY;
        boolean wardrobe = next == SECTION_WARDROBE;
        binding.titleLabel.setText(diary ? R.string.section_diary : wardrobe ? R.string.section_wardrobe : R.string.title);
        binding.monthRow.setVisibility(wardrobe ? View.GONE : View.VISIBLE);
        binding.rangeHint.setVisibility(wardrobe ? View.GONE : View.VISIBLE);
        binding.summary.setVisibility(ledger ? View.VISIBLE : View.GONE);
        binding.filterGroup.setVisibility(ledger ? View.VISIBLE : View.GONE);
        binding.wardrobePanel.setVisibility(wardrobe ? View.VISIBLE : View.GONE);
        LedgerUiState ledgerState = viewModel.getUi().getValue();
        DiaryUiState diaryState = diaryViewModel.getUi().getValue();
        if (ledger && ledgerState != null) {
            binding.monthLabel.setText(ledgerState.monthLabel);
            binding.todayButton.setVisibility(ledgerState.showBackToToday ? View.VISIBLE : View.GONE);
        } else if (diary && diaryState != null) {
            binding.monthLabel.setText(diaryState.monthLabel);
            binding.todayButton.setVisibility(diaryState.showBackToToday ? View.VISIBLE : View.GONE);
        } else {
            binding.todayButton.setVisibility(View.GONE);
        }
        applyList();
    }

    private void applyList() {
        int fab = R.string.add_entry;
        if (section == SECTION_DIARY) {
            binding.list.setLayoutManager(listLayout);
            binding.list.setAdapter(diaryAdapter);
            fab = R.string.add_diary;
        } else if (section == SECTION_WARDROBE) {
            if (wardrobeTab == TAB_OUTFITS) {
                binding.list.setLayoutManager(listLayout);
                binding.list.setAdapter(outfitAdapter);
                fab = R.string.wardrobe_outfit;
            } else if (wardrobeTab == TAB_WISHES) {
                binding.list.setLayoutManager(listLayout);
                binding.list.setAdapter(wishAdapter);
                fab = R.string.wardrobe_wish;
            } else {
                binding.list.setLayoutManager(clothingGrid);
                binding.list.setAdapter(clothingAdapter);
                fab = R.string.wardrobe_add;
            }
        } else {
            binding.list.setLayoutManager(listLayout);
            binding.list.setAdapter(adapter);
        }
        binding.fab.setText(fab);
    }

    private void openWardrobeAdd() {
        if (wardrobeTab == TAB_OUTFITS) {
            OutfitSheet.open(getSupportFragmentManager());
        } else if (wardrobeTab == TAB_WISHES) {
            showWishDialog();
        } else {
            ClothingSheet.open(getSupportFragmentManager(), null, 0, null, null);
        }
    }

    private void render(LedgerUiState state) {
        if (state == null) {
            return;
        }
        binding.balance.setText(state.balance);
        binding.balance.setTextColor(ContextCompat.getColor(
                this,
                state.balanceNegative ? R.color.on_cover_negative : R.color.on_cover
        ));
        binding.income.setText(state.income);
        binding.expense.setText(state.expense);
        binding.summary.setContentDescription(getString(
                R.string.summary_description,
                state.balance,
                state.income,
                state.expense
        ));
        if (state.daily.isEmpty()) {
            binding.dailyLabel.setVisibility(View.GONE);
        } else {
            binding.dailyLabel.setVisibility(View.VISIBLE);
            binding.dailyLabel.setText(state.daily);
        }
        if (section == SECTION_LEDGER) {
            binding.monthLabel.setText(state.monthLabel);
            binding.todayButton.setVisibility(state.showBackToToday ? View.VISIBLE : View.GONE);
            binding.monthLabel.setContentDescription(getString(R.string.range_description, state.monthLabel));
        }
        int expected = R.id.chipAll;
        if (state.filter == LedgerUiState.FILTER_EXPENSE) {
            expected = R.id.chipExpense;
        } else if (state.filter == LedgerUiState.FILTER_INCOME) {
            expected = R.id.chipIncome;
        }
        if (binding.filterGroup.getCheckedChipId() != expected) {
            binding.filterGroup.check(expected);
        }
        adapter.setCategories(viewModel.currentCategories());
        adapter.submit(state.rows);
    }

    private void renderDiary(DiaryUiState state) {
        if (state == null) {
            return;
        }
        if (section == SECTION_DIARY) {
            binding.monthLabel.setText(state.monthLabel);
            binding.todayButton.setVisibility(state.showBackToToday ? View.VISIBLE : View.GONE);
            binding.monthLabel.setContentDescription(getString(R.string.range_description, state.monthLabel));
        }
        diaryAdapter.submit(state.rows);
    }

    private void renderWardrobe(WardrobeUi state) {
        if (state == null) {
            return;
        }
        clothingAdapter.submit(state.clothes, state.loaded);
        outfitAdapter.submit(state.outfits, state.loaded);
        wishAdapter.submit(state.wishes, state.loaded);
        if (state.city == null || state.city.isEmpty()) {
            binding.cityButton.setText(R.string.city_unset);
        } else {
            binding.cityButton.setText(getString(R.string.city_set, state.city));
        }
    }

    private void showRangeDialog() {
        View form = getLayoutInflater().inflate(R.layout.dialog_range, null);
        TextView startValue = form.findViewById(R.id.startValue);
        TextView endValue = form.findViewById(R.id.endValue);
        TextView rangeError = form.findViewById(R.id.rangeError);
        LocalDate[] draftStart = new LocalDate[]{viewModel.getRangeStart()};
        LocalDate[] draftEnd = new LocalDate[]{viewModel.getRangeEnd()};
        startValue.setText(formatFullDate(draftStart[0]));
        endValue.setText(formatFullDate(draftEnd[0]));
        startValue.setOnClickListener(v -> pickRangeDate(draftStart[0], date -> {
            draftStart[0] = date;
            startValue.setText(formatFullDate(date));
            rangeError.setVisibility(View.GONE);
        }));
        endValue.setOnClickListener(v -> pickRangeDate(draftEnd[0], date -> {
            draftEnd[0] = date;
            endValue.setText(formatFullDate(date));
            rangeError.setVisibility(View.GONE);
        }));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pick_range)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (draftEnd[0].isBefore(draftStart[0])) {
                rangeError.setVisibility(View.VISIBLE);
                return;
            }
            viewModel.setRange(draftStart[0], draftEnd[0]);
            diaryViewModel.setRange(draftStart[0], draftEnd[0]);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void pickRangeDate(LocalDate current, java.util.function.Consumer<LocalDate> onPicked) {
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, day) -> onPicked.accept(LocalDate.of(year, month + 1, day)),
                current.getYear(),
                current.getMonthValue() - 1,
                current.getDayOfMonth()
        );
        picker.show();
    }

    private String formatFullDate(LocalDate date) {
        return getString(R.string.year_month_day, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private void editClothing(ClothingItem item) {
        ClothingSheet.open(getSupportFragmentManager(), item, 0, null, null);
    }

    private void openOutfit(WardrobeUi.OutfitCard card) {
        String message = card.weather == null || card.weather.isEmpty()
                ? getString(R.string.outfit_unplanned)
                : card.weather;
        new MaterialAlertDialogBuilder(this)
                .setTitle(card.outfit.name)
                .setMessage(message)
                .setPositiveButton(R.string.outfit_plan, (dialog, which) -> pickPlanDate(card))
                .setNeutralButton(R.string.delete, (dialog, which) -> wardrobeViewModel.deleteOutfit(card.outfit))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void pickPlanDate(WardrobeUi.OutfitCard card) {
        LocalDate current = LocalDate.now();
        if (card.plan != null && card.plan.planDate != null) {
            current = LocalDate.parse(card.plan.planDate);
        }
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, day) -> wardrobeViewModel.plan(card.outfit.id, LocalDate.of(year, month + 1, day)),
                current.getYear(),
                current.getMonthValue() - 1,
                current.getDayOfMonth()
        );
        picker.show();
    }

    private void showCityDialog() {
        EditText input = new EditText(this);
        WardrobeUi state = wardrobeViewModel.getUi().getValue();
        input.setText(state == null || state.city == null ? "" : state.city);
        input.setHint(R.string.city_hint);
        input.setSingleLine(true);
        input.setTextColor(ContextCompat.getColor(this, R.color.ink));
        int pad = Math.round(14 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        input.setBackgroundResource(R.drawable.bg_input);
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        int side = Math.round(24 * getResources().getDisplayMetrics().density);
        frame.setPadding(side, pad, side, 0);
        frame.addView(input);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.city_title)
                .setView(frame)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> wardrobeViewModel.setCity(input.getText().toString()))
                .show();
    }

    private void showWishDialog() {
        View form = getLayoutInflater().inflate(R.layout.dialog_wish, null);
        EditText need = form.findViewById(R.id.wishNeed);
        ChipGroup categories = form.findViewById(R.id.wishCategories);
        for (int i = 0; i < CLOTHING_CATEGORIES.length; i++) {
            Chip chip = filterChip(CLOTHING_CATEGORIES[i], CLOTHING_CATEGORIES[i]);
            categories.addView(chip);
            if (i == 0) {
                chip.setChecked(true);
            }
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.wish_title)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = need.getText().toString().trim();
            if (text.isEmpty()) {
                need.setError(getString(R.string.wish_need_error));
                return;
            }
            Chip checked = categories.findViewById(categories.getCheckedChipId());
            String category = checked == null ? CLOTHING_CATEGORIES[0] : checked.getText().toString();
            wardrobeViewModel.saveWish(text, category);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void buildWardrobeFilters() {
        String[] categories = new String[CLOTHING_CATEGORIES.length + 1];
        categories[0] = getString(R.string.filter_all);
        System.arraycopy(CLOTHING_CATEGORIES, 0, categories, 1, CLOTHING_CATEGORIES.length);
        String[] categoryValues = new String[categories.length];
        categoryValues[0] = "";
        System.arraycopy(CLOTHING_CATEGORIES, 0, categoryValues, 1, CLOTHING_CATEGORIES.length);
        fillFilter(binding.categoryFilter, categories, categoryValues);

        fillFilter(
                binding.seasonFilter,
                new String[]{getString(R.string.filter_all), "春", "夏", "秋", "冬"},
                new Integer[]{0, ClothingItem.SPRING, ClothingItem.SUMMER, ClothingItem.AUTUMN, ClothingItem.WINTER}
        );

        String[] colors = new String[CLOTHING_COLORS.length + 1];
        colors[0] = getString(R.string.filter_all);
        System.arraycopy(CLOTHING_COLORS, 0, colors, 1, CLOTHING_COLORS.length);
        String[] colorValues = new String[colors.length];
        colorValues[0] = "";
        System.arraycopy(CLOTHING_COLORS, 0, colorValues, 1, CLOTHING_COLORS.length);
        fillFilter(binding.colorFilter, colors, colorValues);
    }

    private void fillFilter(ChipGroup group, String[] labels, Object[] values) {
        for (int i = 0; i < labels.length; i++) {
            Chip chip = filterChip(labels[i], values[i]);
            group.addView(chip);
            if (i == 0) {
                chip.setChecked(true);
            }
        }
        group.setOnCheckedStateChangeListener((chips, checkedIds) -> {
            if (checkedIds.isEmpty() || wardrobeViewModel == null) {
                return;
            }
            Chip chip = chips.findViewById(checkedIds.get(0));
            if (chip == null) {
                return;
            }
            if (group.getId() == R.id.seasonFilter && chip.getTag() instanceof Integer) {
                filterSeason = (Integer) chip.getTag();
            } else if (group.getId() == R.id.colorFilter && chip.getTag() instanceof String) {
                filterColor = (String) chip.getTag();
            } else if (chip.getTag() instanceof String) {
                filterCategory = (String) chip.getTag();
            }
            wardrobeViewModel.filter(filterCategory, filterSeason, filterColor);
        });
    }

    private Chip filterChip(String label, Object value) {
        Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setTag(value);
        return chip;
    }

    private void showDiaryUndo(DiaryEntry snapshot) {
        Snackbar snackbar = Snackbar.make(binding.main, R.string.deleted, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(binding.fab);
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.income));
        snackbar.setAction(R.string.undo, v -> diaryViewModel.restore(snapshot));
        snackbar.show();
    }

    private void showUndo(Transaction snapshot) {
        Snackbar snackbar = Snackbar.make(binding.main, R.string.deleted, Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(binding.fab);
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.income));
        snackbar.setAction(R.string.undo, v -> viewModel.restore(snapshot));
        snackbar.show();
    }

    private void attachSwipe() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.START | ItemTouchHelper.END
        ) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getBindingAdapterPosition();
                if (section == SECTION_WARDROBE) {
                    return 0;
                }
                boolean canSwipe = section == SECTION_DIARY ? diaryAdapter.canSwipe(position) : adapter.canSwipe(position);
                if (!canSwipe) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (section == SECTION_DIARY) {
                    DiaryEntry entry = diaryAdapter.entryAt(position);
                    if (entry == null) {
                        if (position != RecyclerView.NO_POSITION) {
                            diaryAdapter.notifyItemChanged(position);
                        }
                        return;
                    }
                    DiaryEntry snapshot = entry.copy();
                    binding.list.post(() -> {
                        diaryViewModel.remove(snapshot);
                        showDiaryUndo(snapshot);
                    });
                    return;
                }
                Transaction transaction = adapter.transactionAt(position);
                if (transaction == null) {
                    if (position != RecyclerView.NO_POSITION) {
                        adapter.notifyItemChanged(position);
                    }
                    return;
                }
                Transaction snapshot = transaction.copy();
                binding.list.post(() -> {
                    viewModel.remove(snapshot);
                    showUndo(snapshot);
                });
            }

            @Override
            public void onChildDraw(
                    Canvas canvas,
                    RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    float dX,
                    float dY,
                    int actionState,
                    boolean isCurrentlyActive
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View item = viewHolder.itemView;
                    canvas.drawRect(item.getLeft(), item.getTop(), item.getRight(), item.getBottom(), swipePaint);
                    if (Math.abs(dX) > 48f * getResources().getDisplayMetrics().density) {
                        swipeTextPaint.setTextAlign(dX > 0 ? Paint.Align.LEFT : Paint.Align.RIGHT);
                        float x = dX > 0
                                ? item.getLeft() + 24f * getResources().getDisplayMetrics().density
                                : item.getRight() - 24f * getResources().getDisplayMetrics().density;
                        float y = item.getTop() + item.getHeight() / 2f
                                - (swipeTextPaint.descent() + swipeTextPaint.ascent()) / 2f;
                        canvas.drawText(deleteLabel, x, y, swipeTextPaint);
                    }
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        helper.attachToRecyclerView(binding.list);
    }
}
