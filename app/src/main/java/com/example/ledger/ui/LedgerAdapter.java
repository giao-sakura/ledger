package com.example.ledger.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.R;
import com.example.ledger.data.Category;
import com.example.ledger.data.CategoryTotal;
import com.example.ledger.data.Money;
import com.example.ledger.data.Transaction;
import com.example.ledger.databinding.ItemCategoriesBinding;
import com.example.ledger.databinding.ItemCategoryBarBinding;
import com.example.ledger.databinding.ItemChartsBinding;
import com.example.ledger.databinding.ItemDayHeaderBinding;
import com.example.ledger.databinding.ItemEmptyBinding;
import com.example.ledger.databinding.ItemSliceBinding;
import com.example.ledger.databinding.ItemTransactionBinding;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class LedgerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Listener {
        void onTransactionClick(Transaction transaction);
    }

    private List<LedgerRow> rows = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private Listener listener;

    public void setCategories(List<Category> categories) {
        this.categories = categories == null ? new ArrayList<>() : categories;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<LedgerRow> next) {
        List<LedgerRow> old = this.rows;
        List<LedgerRow> latest = next == null ? new ArrayList<>() : next;
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return old.size();
            }

            @Override
            public int getNewListSize() {
                return latest.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return identity(old.get(oldPosition)).equals(identity(latest.get(newPosition)));
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return contentsSame(old.get(oldPosition), latest.get(newPosition));
            }
        });
        this.rows = latest;
        result.dispatchUpdatesTo(this);
    }

    public boolean canSwipe(int position) {
        return position != RecyclerView.NO_POSITION
                && position < rows.size()
                && rows.get(position) instanceof LedgerRow.Entry;
    }

    public Transaction transactionAt(int position) {
        if (!canSwipe(position)) {
            return null;
        }
        return ((LedgerRow.Entry) rows.get(position)).transaction;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).viewType();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == LedgerRow.CHARTS) {
            return new ChartsHolder(ItemChartsBinding.inflate(inflater, parent, false));
        }
        if (viewType == LedgerRow.CATEGORIES) {
            return new CategoriesHolder(ItemCategoriesBinding.inflate(inflater, parent, false));
        }
        if (viewType == LedgerRow.DAY) {
            return new DayHolder(ItemDayHeaderBinding.inflate(inflater, parent, false));
        }
        if (viewType == LedgerRow.TRANSACTION) {
            return new TransactionHolder(ItemTransactionBinding.inflate(inflater, parent, false));
        }
        return new EmptyHolder(ItemEmptyBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LedgerRow row = rows.get(position);
        if (holder instanceof ChartsHolder && row instanceof LedgerRow.Charts) {
            ((ChartsHolder) holder).bind((LedgerRow.Charts) row);
        } else if (holder instanceof CategoriesHolder && row instanceof LedgerRow.Categories) {
            ((CategoriesHolder) holder).bind((LedgerRow.Categories) row);
        } else if (holder instanceof DayHolder && row instanceof LedgerRow.Day) {
            ((DayHolder) holder).bind((LedgerRow.Day) row);
        } else if (holder instanceof TransactionHolder && row instanceof LedgerRow.Entry) {
            ((TransactionHolder) holder).bind((LedgerRow.Entry) row);
        } else if (holder instanceof EmptyHolder && row instanceof LedgerRow.Empty) {
            ((EmptyHolder) holder).bind((LedgerRow.Empty) row);
        }
    }

    private Category category(String key) {
        return Category.find(categories, key);
    }

    private static String identity(LedgerRow row) {
        if (row instanceof LedgerRow.Entry) {
            return "tx:" + ((LedgerRow.Entry) row).transaction.id;
        }
        if (row instanceof LedgerRow.Day) {
            return "day:" + ((LedgerRow.Day) row).label;
        }
        if (row instanceof LedgerRow.Charts) {
            return "charts";
        }
        if (row instanceof LedgerRow.Categories) {
            return "categories";
        }
        return "empty";
    }

    private static boolean contentsSame(LedgerRow left, LedgerRow right) {
        if (left instanceof LedgerRow.Entry && right instanceof LedgerRow.Entry) {
            Transaction a = ((LedgerRow.Entry) left).transaction;
            Transaction b = ((LedgerRow.Entry) right).transaction;
            return a.amountCents == b.amountCents
                    && a.type == b.type
                    && a.occurredAt == b.occurredAt
                    && Objects.equals(a.categoryKey, b.categoryKey)
                    && Objects.equals(a.note, b.note)
                    && ((LedgerRow.Entry) left).showDivider == ((LedgerRow.Entry) right).showDivider;
        }
        if (left instanceof LedgerRow.Day && right instanceof LedgerRow.Day) {
            return ((LedgerRow.Day) left).label.equals(((LedgerRow.Day) right).label)
                    && ((LedgerRow.Day) left).total.equals(((LedgerRow.Day) right).total);
        }
        if (left instanceof LedgerRow.Empty && right instanceof LedgerRow.Empty) {
            return ((LedgerRow.Empty) left).title.equals(((LedgerRow.Empty) right).title)
                    && ((LedgerRow.Empty) left).subtitle.equals(((LedgerRow.Empty) right).subtitle);
        }
        if (left instanceof LedgerRow.Charts && right instanceof LedgerRow.Charts) {
            return ((LedgerRow.Charts) left).type == ((LedgerRow.Charts) right).type
                    && totalsSame(((LedgerRow.Charts) left).slices, ((LedgerRow.Charts) right).slices);
        }
        if (left instanceof LedgerRow.Categories && right instanceof LedgerRow.Categories) {
            if (!((LedgerRow.Categories) left).title.equals(((LedgerRow.Categories) right).title)) {
                return false;
            }
            List<CategoryTotal> a = ((LedgerRow.Categories) left).totals;
            List<CategoryTotal> b = ((LedgerRow.Categories) right).totals;
            return totalsSame(a, b);
        }
        return false;
    }

    private static boolean totalsSame(List<CategoryTotal> a, List<CategoryTotal> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).cents != b.get(i).cents || !a.get(i).key.equals(b.get(i).key)) {
                return false;
            }
        }
        return true;
    }

    private final class ChartsHolder extends RecyclerView.ViewHolder {
        private final ItemChartsBinding binding;

        private ChartsHolder(ItemChartsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(LedgerRow.Charts row) {
            int titleRes = row.type == Transaction.INCOME
                    ? R.string.chart_income
                    : R.string.chart_expense;
            bindSide(binding.pie, binding.title, binding.legend, titleRes, row.slices);
        }

        private void bindSide(
                RatioPieView pie,
                android.widget.TextView title,
                android.widget.LinearLayout legend,
                int titleRes,
                List<CategoryTotal> slices
        ) {
            long total = 0L;
            for (int i = 0; i < slices.size(); i++) {
                total += slices.get(i).cents;
            }
            if (total <= 0L) {
                title.setText(itemView.getContext().getString(titleRes, itemView.getContext().getString(R.string.chart_empty)));
                pie.setSlices(new int[0], new long[0]);
                legend.removeAllViews();
                return;
            }
            title.setText(itemView.getContext().getString(titleRes, Money.format(total)));
            int[] colors = new int[slices.size()];
            long[] amounts = new long[slices.size()];
            int[] percents = new int[slices.size()];
            int largest = 0;
            int percentSum = 0;
            for (int i = 0; i < slices.size(); i++) {
                CategoryTotal slice = slices.get(i);
                colors[i] = category(slice.key).color;
                amounts[i] = slice.cents;
                percents[i] = (int) Math.round(slice.cents * 100.0 / total);
                percentSum += percents[i];
                if (slice.cents > slices.get(largest).cents) {
                    largest = i;
                }
            }
            percents[largest] += 100 - percentSum;
            pie.setSlices(colors, amounts);
            legend.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(legend.getContext());
            for (int i = 0; i < slices.size(); i++) {
                ItemSliceBinding sliceBinding = ItemSliceBinding.inflate(inflater, legend, true);
                Category category = category(slices.get(i).key);
                sliceBinding.swatch.setBackground(Drawables.oval(category.color));
                sliceBinding.name.setText(category.name);
                sliceBinding.percent.setText(legend.getContext().getString(R.string.slice_percent, percents[i]));
            }
            StringBuilder description = new StringBuilder(title.getText());
            for (int i = 0; i < slices.size(); i++) {
                description.append('，').append(category(slices.get(i).key).name).append(' ').append(percents[i]).append('%');
            }
            pie.setContentDescription(description.toString());
        }
    }

    private final class CategoriesHolder extends RecyclerView.ViewHolder {
        private final ItemCategoriesBinding binding;

        private CategoriesHolder(ItemCategoriesBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(LedgerRow.Categories row) {
            binding.sectionTitle.setText(row.title);
            binding.bars.removeAllViews();
            long max = 0L;
            for (int i = 0; i < row.totals.size(); i++) {
                max = Math.max(max, row.totals.get(i).cents);
            }
            LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
            for (int i = 0; i < row.totals.size(); i++) {
                bindBar(inflater, row.totals.get(i), max);
            }
        }

        private void bindBar(LayoutInflater inflater, CategoryTotal total, long max) {
            ItemCategoryBarBinding barBinding = ItemCategoryBarBinding.inflate(inflater, binding.bars, true);
            final String key = total.key;
            final long cents = total.cents;
            final float fraction = max == 0L ? 0f : cents / (float) max;
            Category category = category(key);
            barBinding.mark.setText(category.mark);
            barBinding.mark.setBackground(Drawables.oval(category.color));
            barBinding.name.setText(category.name);
            barBinding.amount.setText(Money.format(cents));
            barBinding.fill.setTag(fraction);
            barBinding.track.post(() -> {
                Object tag = barBinding.fill.getTag();
                if (!(tag instanceof Float) || Float.compare((Float) tag, fraction) != 0) {
                    return;
                }
                int trackWidth = barBinding.track.getWidth();
                int trackHeight = barBinding.track.getHeight();
                float radius = trackHeight / 2f;
                int line = ContextCompat.getColor(barBinding.track.getContext(), R.color.line);
                barBinding.track.setBackground(Drawables.rounded(line, radius));
                barBinding.fill.setBackground(Drawables.rounded(category(key).color, radius));
                int fillWidth = trackWidth == 0 ? 0 : Math.round(trackWidth * fraction);
                if (fraction > 0f) {
                    fillWidth = Math.max(fillWidth, trackHeight);
                }
                fillWidth = Math.min(fillWidth, trackWidth);
                ViewGroup.LayoutParams params = barBinding.fill.getLayoutParams();
                params.width = fillWidth;
                barBinding.fill.setLayoutParams(params);
            });
        }
    }

    private static final class DayHolder extends RecyclerView.ViewHolder {
        private final ItemDayHeaderBinding binding;

        private DayHolder(ItemDayHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(LedgerRow.Day row) {
            binding.label.setText(row.label);
            binding.total.setText(row.total);
        }
    }

    private final class TransactionHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        private TransactionHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(LedgerRow.Entry row) {
            Transaction transaction = row.transaction;
            Category category = category(transaction.categoryKey);
            binding.mark.setText(category.mark);
            binding.mark.setBackground(Drawables.oval(category.color));
            binding.category.setText(category.name);
            String note = transaction.note == null ? "" : transaction.note.trim();
            binding.note.setText(note);
            binding.note.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
            binding.amount.setText(Money.formatChange(transaction.type, transaction.amountCents));
            int amountColor = ContextCompat.getColor(
                    binding.amount.getContext(),
                    transaction.type == Transaction.INCOME ? R.color.income : R.color.expense
            );
            binding.amount.setTextColor(amountColor);
            ZonedDateTime time = Instant.ofEpochMilli(transaction.occurredAt).atZone(ZoneId.systemDefault());
            binding.time.setText(String.format(Locale.CHINA, "%02d:%02d", time.getHour(), time.getMinute()));
            binding.divider.setVisibility(row.showDivider ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });
            itemView.setContentDescription(category.name + " " + binding.amount.getText());
        }
    }

    private static final class EmptyHolder extends RecyclerView.ViewHolder {
        private final ItemEmptyBinding binding;

        private EmptyHolder(ItemEmptyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(LedgerRow.Empty row) {
            binding.title.setText(row.title);
            binding.subtitle.setText(row.subtitle);
            itemView.post(() -> stretch(1));
        }

        private void stretch(int attemptsLeft) {
            if (!(itemView.getParent() instanceof RecyclerView)) {
                return;
            }
            RecyclerView recyclerView = (RecyclerView) itemView.getParent();
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            if (adapter == null || adapter.getItemCount() != 1 || getBindingAdapterPosition() != 0) {
                return;
            }
            int target = recyclerView.getHeight() - recyclerView.getPaddingTop() - recyclerView.getPaddingBottom();
            if (target <= 0) {
                if (attemptsLeft > 0) {
                    itemView.post(() -> stretch(attemptsLeft - 1));
                }
                return;
            }
            if (itemView.getLayoutParams().height != target) {
                itemView.getLayoutParams().height = target;
                itemView.requestLayout();
            }
        }
    }
}
