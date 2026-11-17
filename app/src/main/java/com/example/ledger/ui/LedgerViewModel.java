package com.example.ledger.ui;

import android.app.Application;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ledger.data.AppDatabase;
import com.example.ledger.data.Category;
import com.example.ledger.data.CategoryStore;
import com.example.ledger.data.LedgerRepository;
import com.example.ledger.data.Transaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class LedgerViewModel extends AndroidViewModel {
    private final LedgerRepository repository;
    private final CategoryStore categoryStore;
    private final MutableLiveData<LedgerUiState> ui = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final List<Transaction> cache = new ArrayList<>();
    private LocalDate rangeStartDate;
    private LocalDate rangeEndDate;
    private int filter = LedgerUiState.FILTER_ALL;
    private boolean loaded;
    private int loadGeneration;
    private long rangeStart;
    private long rangeEnd;

    public LedgerViewModel(Application application) {
        super(application);
        repository = new LedgerRepository(AppDatabase.get(application).transactionDao());
        categoryStore = new CategoryStore(application);
        categories.setValue(categoryStore.all());
        YearMonth current = YearMonth.now();
        rangeStartDate = current.atDay(1);
        rangeEndDate = current.atEndOfMonth();
        updateRange();
        publish();
        reload();
    }

    public LiveData<LedgerUiState> getUi() {
        return ui;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public List<Category> currentCategories() {
        List<Category> value = categories.getValue();
        return value == null ? categoryStore.all() : value;
    }

    public LocalDate getRangeStart() {
        return rangeStartDate;
    }

    public LocalDate getRangeEnd() {
        return rangeEndDate;
    }

    @Nullable
    public Transaction find(long id) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id == id) {
                return cache.get(i);
            }
        }
        return null;
    }

    @Nullable
    public Category addCategory(int type, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > 8) {
            return null;
        }
        List<Category> current = currentCategories();
        int customCount = 0;
        for (int i = 0; i < current.size(); i++) {
            Category category = current.get(i);
            if (category.type == type && trimmed.equals(category.name)) {
                return null;
            }
            if (category.custom && category.type == type) {
                customCount++;
            }
        }
        Category created = Category.custom(type, trimmed, customCount);
        categoryStore.add(created);
        categories.setValue(categoryStore.all());
        publish();
        return created;
    }

    public void shiftMonth(int delta) {
        if (isFullMonth(rangeStartDate, rangeEndDate)) {
            YearMonth target = YearMonth.from(rangeStartDate).plusMonths(delta);
            setRange(target.atDay(1), target.atEndOfMonth());
            return;
        }
        setRange(shiftDate(rangeStartDate, delta), shiftDate(rangeEndDate, delta));
    }

    public void goToCurrentMonth() {
        YearMonth now = YearMonth.now();
        setRange(now.atDay(1), now.atEndOfMonth());
    }

    public void setRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return;
        }
        if (start.equals(rangeStartDate) && end.equals(rangeEndDate)) {
            return;
        }
        rangeStartDate = start;
        rangeEndDate = end;
        updateRange();
        cache.clear();
        loaded = false;
        publish();
        reload();
    }

    public void setFilter(int filter) {
        if (this.filter == filter) {
            return;
        }
        this.filter = filter;
        publish();
    }

    public void save(long id, int type, long amountCents, String categoryKey, String note, long occurredAt) {
        Transaction transaction = new Transaction();
        transaction.id = id;
        transaction.type = type;
        transaction.amountCents = amountCents;
        transaction.categoryKey = categoryKey;
        transaction.note = note == null ? "" : note.trim();
        transaction.occurredAt = occurredAt;
        loadGeneration++;
        repository.save(transaction, this::reload);
    }

    public void remove(Transaction transaction) {
        Transaction snapshot = transaction.copy();
        loadGeneration++;
        cache.removeIf(item -> item.id == snapshot.id);
        publish();
        repository.delete(snapshot, this::reload);
    }

    public void restore(Transaction transaction) {
        loadGeneration++;
        if (transaction.occurredAt >= rangeStart && transaction.occurredAt < rangeEnd) {
            cache.add(transaction);
            publish();
        }
        repository.insert(transaction, this::reload);
    }

    @Override
    protected void onCleared() {
        repository.close();
    }

    private static boolean isFullMonth(LocalDate start, LocalDate end) {
        return start.getDayOfMonth() == 1
                && start.getYear() == end.getYear()
                && start.getMonthValue() == end.getMonthValue()
                && end.equals(YearMonth.from(start).atEndOfMonth());
    }

    private static LocalDate shiftDate(LocalDate date, int delta) {
        YearMonth target = YearMonth.from(date).plusMonths(delta);
        int day = Math.min(date.getDayOfMonth(), target.lengthOfMonth());
        return target.atDay(day);
    }

    private void updateRange() {
        ZoneId zone = ZoneId.systemDefault();
        rangeStart = rangeStartDate.atStartOfDay(zone).toInstant().toEpochMilli();
        rangeEnd = rangeEndDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    private void reload() {
        final int generation = ++loadGeneration;
        final long start = rangeStart;
        final long end = rangeEnd;
        repository.listBetween(start, end, list -> {
            if (generation != loadGeneration) {
                return;
            }
            cache.clear();
            cache.addAll(list);
            loaded = true;
            publish();
        });
    }

    private void publish() {
        ui.setValue(LedgerUiState.create(
                rangeStartDate,
                rangeEndDate,
                filter,
                cache,
                loaded,
                getApplication()
        ));
    }
}
