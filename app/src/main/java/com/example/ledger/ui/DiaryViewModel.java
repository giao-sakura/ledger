package com.example.ledger.ui;

import android.app.Application;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ledger.data.AppDatabase;
import com.example.ledger.data.DiaryEntry;
import com.example.ledger.data.DiaryRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class DiaryViewModel extends AndroidViewModel {
    private final DiaryRepository repository;
    private final MutableLiveData<DiaryUiState> ui = new MutableLiveData<>();
    private final List<DiaryEntry> cache = new ArrayList<>();
    private LocalDate rangeStartDate;
    private LocalDate rangeEndDate;
    private boolean loaded;
    private int loadGeneration;
    private long rangeStart;
    private long rangeEnd;

    public DiaryViewModel(Application application) {
        super(application);
        repository = new DiaryRepository(AppDatabase.get(application).diaryDao());
        YearMonth current = YearMonth.now();
        rangeStartDate = current.atDay(1);
        rangeEndDate = current.atEndOfMonth();
        updateRange();
        publish();
        reload();
    }

    public LiveData<DiaryUiState> getUi() {
        return ui;
    }

    public LocalDate suggestedDate() {
        LocalDate today = LocalDate.now();
        if (!today.isBefore(rangeStartDate) && !today.isAfter(rangeEndDate)) {
            return today;
        }
        return rangeEndDate;
    }

    @Nullable
    public DiaryEntry find(long id) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id == id) {
                return cache.get(i);
            }
        }
        return null;
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

    public void save(long id, String body, long occurredAt) {
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            return;
        }
        DiaryEntry entry = new DiaryEntry();
        entry.id = id;
        entry.body = text;
        entry.occurredAt = occurredAt;
        loadGeneration++;
        repository.save(entry, this::reload);
    }

    public void remove(DiaryEntry entry) {
        DiaryEntry snapshot = entry.copy();
        loadGeneration++;
        cache.removeIf(item -> item.id == snapshot.id);
        publish();
        repository.delete(snapshot, this::reload);
    }

    public void restore(DiaryEntry entry) {
        loadGeneration++;
        if (entry.occurredAt >= rangeStart && entry.occurredAt < rangeEnd) {
            cache.add(entry);
            publish();
        }
        repository.insert(entry, this::reload);
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
        ui.setValue(DiaryUiState.create(
                rangeStartDate,
                rangeEndDate,
                cache,
                loaded,
                getApplication()
        ));
    }
}
