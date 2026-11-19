package com.example.ledger;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.data.DiaryEntry;
import com.example.ledger.data.Transaction;
import com.example.ledger.databinding.ActivityMainBinding;
import com.example.ledger.ui.DiaryAdapter;
import com.example.ledger.ui.DiarySheet;
import com.example.ledger.ui.DiaryUiState;
import com.example.ledger.ui.DiaryViewModel;
import com.example.ledger.ui.EditorSheet;
import com.example.ledger.ui.LedgerAdapter;
import com.example.ledger.ui.LedgerUiState;
import com.example.ledger.ui.LedgerViewModel;
import com.example.ledger.update.UpdateFlow;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private LedgerViewModel viewModel;
    private DiaryViewModel diaryViewModel;
    private final LedgerAdapter adapter = new LedgerAdapter();
    private final DiaryAdapter diaryAdapter = new DiaryAdapter();
    private boolean diaryMode;
    private UpdateFlow updateFlow;
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
        deleteLabel = getString(R.string.delete);
        swipePaint.setColor(ContextCompat.getColor(this, R.color.swipe));
        swipeTextPaint.setColor(ContextCompat.getColor(this, R.color.white));
        swipeTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14,
                getResources().getDisplayMetrics()
        ));

        updateFlow = new UpdateFlow(this, binding.versionEntry);
        updateFlow.attach();
        adapter.setListener(transaction -> EditorSheet.open(getSupportFragmentManager(), transaction));
        diaryAdapter.setListener(entry -> DiarySheet.open(getSupportFragmentManager(), entry, null));
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        attachSwipe();

        binding.prevMonth.setOnClickListener(v -> shiftMonth(-1));
        binding.nextMonth.setOnClickListener(v -> shiftMonth(1));
        binding.monthLabel.setOnClickListener(v -> showRangeDialog());
        binding.rangeHint.setOnClickListener(v -> showRangeDialog());
        binding.todayButton.setOnClickListener(v -> {
            viewModel.goToCurrentMonth();
            diaryViewModel.goToCurrentMonth();
        });
        binding.fab.setOnClickListener(v -> {
            if (diaryMode) {
                DiarySheet.open(getSupportFragmentManager(), null, diaryViewModel.suggestedDate());
            } else {
                EditorSheet.open(getSupportFragmentManager(), null);
            }
        });
        binding.sectionGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            showSection(checkedIds.get(0) == R.id.sectionDiary);
        });
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updateFlow != null) {
            updateFlow.onResume();
        }
    }

    private void shiftMonth(int delta) {
        viewModel.shiftMonth(delta);
        diaryViewModel.shiftMonth(delta);
    }

    private void showSection(boolean diary) {
        diaryMode = diary;
        binding.titleLabel.setText(diary ? R.string.section_diary : R.string.title);
        binding.summary.setVisibility(diary ? View.GONE : View.VISIBLE);
        binding.filterGroup.setVisibility(diary ? View.GONE : View.VISIBLE);
        binding.fab.setText(diary ? R.string.add_diary : R.string.add_entry);
        binding.list.setAdapter(diary ? diaryAdapter : adapter);
    }

    private void render(LedgerUiState state) {
        if (state == null) {
            return;
        }
        binding.monthLabel.setText(state.monthLabel);
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
        binding.todayButton.setVisibility(state.showBackToToday ? View.VISIBLE : View.GONE);
        binding.monthLabel.setContentDescription(getString(R.string.range_description, state.monthLabel));
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
        if (diaryMode) {
            binding.monthLabel.setText(state.monthLabel);
            binding.todayButton.setVisibility(state.showBackToToday ? View.VISIBLE : View.GONE);
            binding.monthLabel.setContentDescription(getString(R.string.range_description, state.monthLabel));
        }
        diaryAdapter.submit(state.rows);
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
                boolean canSwipe = diaryMode ? diaryAdapter.canSwipe(position) : adapter.canSwipe(position);
                if (!canSwipe) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (diaryMode) {
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
