package com.example.ledger.ui;

import android.content.Context;

import com.example.ledger.R;
import com.example.ledger.data.DiaryEntry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DiaryUiState {
    public final String monthLabel;
    public final boolean showBackToToday;
    public final List<DiaryRow> rows;

    private DiaryUiState(String monthLabel, boolean showBackToToday, List<DiaryRow> rows) {
        this.monthLabel = monthLabel;
        this.showBackToToday = showBackToToday;
        this.rows = rows;
    }

    public static DiaryUiState create(
            LocalDate rangeStart,
            LocalDate rangeEnd,
            List<DiaryEntry> entries,
            boolean loaded,
            Context context
    ) {
        List<DiaryRow> rows = new ArrayList<>();
        if (entries.isEmpty()) {
            if (loaded) {
                rows.add(new DiaryRow.Empty(
                        context.getString(R.string.diary_empty_title),
                        context.getString(R.string.diary_empty_subtitle)
                ));
            }
        } else {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(zone);
            Map<LocalDate, List<DiaryEntry>> grouped = new LinkedHashMap<>();
            for (int i = 0; i < entries.size(); i++) {
                DiaryEntry entry = entries.get(i);
                LocalDate date = Instant.ofEpochMilli(entry.occurredAt).atZone(zone).toLocalDate();
                List<DiaryEntry> bucket = grouped.get(date);
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    grouped.put(date, bucket);
                }
                bucket.add(entry);
            }
            for (Map.Entry<LocalDate, List<DiaryEntry>> group : grouped.entrySet()) {
                rows.add(new DiaryRow.Day(dayLabel(group.getKey(), today, context)));
                List<DiaryEntry> items = group.getValue();
                for (int i = 0; i < items.size(); i++) {
                    rows.add(new DiaryRow.Entry(items.get(i), i < items.size() - 1));
                }
            }
        }
        YearMonth currentMonth = YearMonth.now();
        boolean currentMonthRange = rangeStart.equals(currentMonth.atDay(1))
                && rangeEnd.equals(currentMonth.atEndOfMonth());
        return new DiaryUiState(rangeLabel(rangeStart, rangeEnd, context), !currentMonthRange, rows);
    }

    private static String rangeLabel(LocalDate start, LocalDate end, Context context) {
        if (start.getDayOfMonth() == 1
                && start.getYear() == end.getYear()
                && start.getMonthValue() == end.getMonthValue()
                && end.equals(YearMonth.from(start).atEndOfMonth())) {
            return context.getString(R.string.year_month, start.getYear(), start.getMonthValue());
        }
        return context.getString(R.string.range_separator, formatDay(start, context), formatDay(end, context));
    }

    private static String formatDay(LocalDate date, Context context) {
        if (date.getYear() == LocalDate.now().getYear()) {
            return context.getString(R.string.month_day, date.getMonthValue(), date.getDayOfMonth());
        }
        return context.getString(R.string.year_month_day, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static String dayLabel(LocalDate date, LocalDate today, Context context) {
        String prefix;
        if (date.equals(today)) {
            prefix = context.getString(R.string.today);
        } else if (date.equals(today.minusDays(1))) {
            prefix = context.getString(R.string.yesterday);
        } else if (date.getYear() == today.getYear()) {
            prefix = context.getString(R.string.month_day, date.getMonthValue(), date.getDayOfMonth());
        } else {
            prefix = context.getString(
                    R.string.year_month_day,
                    date.getYear(),
                    date.getMonthValue(),
                    date.getDayOfMonth()
            );
        }
        String week = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINA);
        return context.getString(R.string.day_with_week, prefix, week);
    }
}
