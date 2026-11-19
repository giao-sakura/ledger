package com.example.ledger.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ledger.data.DiaryEntry;
import com.example.ledger.databinding.ItemDayHeaderBinding;
import com.example.ledger.databinding.ItemDiaryBinding;
import com.example.ledger.databinding.ItemDiaryEmptyBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiaryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Listener {
        void onDiaryClick(DiaryEntry entry);
    }

    private List<DiaryRow> rows = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<DiaryRow> next) {
        List<DiaryRow> old = this.rows;
        List<DiaryRow> latest = next == null ? new ArrayList<>() : next;
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
                && rows.get(position) instanceof DiaryRow.Entry;
    }

    public DiaryEntry entryAt(int position) {
        if (!canSwipe(position)) {
            return null;
        }
        return ((DiaryRow.Entry) rows.get(position)).entry;
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
        if (viewType == DiaryRow.DAY) {
            return new DayHolder(ItemDayHeaderBinding.inflate(inflater, parent, false));
        }
        if (viewType == DiaryRow.ENTRY) {
            return new EntryHolder(ItemDiaryBinding.inflate(inflater, parent, false));
        }
        return new EmptyHolder(ItemDiaryEmptyBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DiaryRow row = rows.get(position);
        if (holder instanceof DayHolder && row instanceof DiaryRow.Day) {
            ((DayHolder) holder).bind((DiaryRow.Day) row);
        } else if (holder instanceof EntryHolder && row instanceof DiaryRow.Entry) {
            ((EntryHolder) holder).bind((DiaryRow.Entry) row);
        } else if (holder instanceof EmptyHolder && row instanceof DiaryRow.Empty) {
            ((EmptyHolder) holder).bind((DiaryRow.Empty) row);
        }
    }

    private static String identity(DiaryRow row) {
        if (row instanceof DiaryRow.Entry) {
            return "entry:" + ((DiaryRow.Entry) row).entry.id;
        }
        if (row instanceof DiaryRow.Day) {
            return "day:" + ((DiaryRow.Day) row).label;
        }
        return "empty";
    }

    private static boolean contentsSame(DiaryRow left, DiaryRow right) {
        if (left instanceof DiaryRow.Entry && right instanceof DiaryRow.Entry) {
            DiaryEntry a = ((DiaryRow.Entry) left).entry;
            DiaryEntry b = ((DiaryRow.Entry) right).entry;
            return a.occurredAt == b.occurredAt
                    && Objects.equals(a.body, b.body)
                    && ((DiaryRow.Entry) left).showDivider == ((DiaryRow.Entry) right).showDivider;
        }
        if (left instanceof DiaryRow.Day && right instanceof DiaryRow.Day) {
            return ((DiaryRow.Day) left).label.equals(((DiaryRow.Day) right).label);
        }
        if (left instanceof DiaryRow.Empty && right instanceof DiaryRow.Empty) {
            return ((DiaryRow.Empty) left).title.equals(((DiaryRow.Empty) right).title)
                    && ((DiaryRow.Empty) left).subtitle.equals(((DiaryRow.Empty) right).subtitle);
        }
        return false;
    }

    private static final class DayHolder extends RecyclerView.ViewHolder {
        private final ItemDayHeaderBinding binding;

        private DayHolder(ItemDayHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(DiaryRow.Day row) {
            binding.label.setText(row.label);
            binding.total.setText("");
        }
    }

    private final class EntryHolder extends RecyclerView.ViewHolder {
        private final ItemDiaryBinding binding;

        private EntryHolder(ItemDiaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(DiaryRow.Entry row) {
            binding.body.setText(row.entry.body);
            binding.divider.setVisibility(row.showDivider ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDiaryClick(row.entry);
                }
            });
        }
    }

    private static final class EmptyHolder extends RecyclerView.ViewHolder {
        private final ItemDiaryEmptyBinding binding;

        private EmptyHolder(ItemDiaryEmptyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(DiaryRow.Empty row) {
            binding.title.setText(row.title);
            binding.subtitle.setText(row.subtitle);
        }
    }
}
