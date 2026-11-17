package com.example.ledger.ui;

import android.content.Context;

import com.example.ledger.R;
import com.example.ledger.data.CategoryTotal;
import com.example.ledger.data.Money;
import com.example.ledger.data.Transaction;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LedgerUiState {
    public static final int FILTER_ALL = 0;
    public static final int FILTER_EXPENSE = 1;
    public static final int FILTER_INCOME = 2;

    public final String monthLabel;
    public final String balance;
    public final boolean balanceNegative;
    public final String income;
    public final String expense;
    public final long incomeCents;
    public final long expenseCents;
    public final String daily;
    public final int filter;
    public final boolean showBackToToday;
    public final List<LedgerRow> rows;

    private LedgerUiState(
            String monthLabel,
            String balance,
            boolean balanceNegative,
            String income,
            String expense,
            long incomeCents,
            long expenseCents,
            String daily,
            int filter,
            boolean showBackToToday,
            List<LedgerRow> rows
    ) {
        this.monthLabel = monthLabel;
        this.balance = balance;
        this.balanceNegative = balanceNegative;
        this.income = income;
        this.expense = expense;
        this.incomeCents = incomeCents;
        this.expenseCents = expenseCents;
        this.daily = daily;
        this.filter = filter;
        this.showBackToToday = showBackToToday;
        this.rows = rows;
    }

    public static LedgerUiState create(
            LocalDate rangeStart,
            LocalDate rangeEnd,
            int filter,
            List<Transaction> all,
            boolean loaded,
            Context context
    ) {
        long income = 0L;
        long expense = 0L;
        for (int i = 0; i < all.size(); i++) {
            Transaction transaction = all.get(i);
            if (transaction.type == Transaction.INCOME) {
                income += transaction.amountCents;
            } else {
                expense += transaction.amountCents;
            }
        }

        List<Transaction> shown = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Transaction transaction = all.get(i);
            if (filter == FILTER_EXPENSE && transaction.type != Transaction.EXPENSE) {
                continue;
            }
            if (filter == FILTER_INCOME && transaction.type != Transaction.INCOME) {
                continue;
            }
            shown.add(transaction);
        }
        Collections.sort(shown, (left, right) -> {
            int byTime = Long.compare(right.occurredAt, left.occurredAt);
            if (byTime != 0) {
                return byTime;
            }
            return Long.compare(right.id, left.id);
        });

        List<LedgerRow> rows = new ArrayList<>();
        if (loaded && filter == FILTER_EXPENSE) {
            List<CategoryTotal> expenseSlices = totalsFor(all, Transaction.EXPENSE);
            if (!expenseSlices.isEmpty()) {
                rows.add(new LedgerRow.Charts(Transaction.EXPENSE, expenseSlices));
            }
        } else if (loaded && filter == FILTER_INCOME) {
            List<CategoryTotal> incomeSlices = totalsFor(all, Transaction.INCOME);
            if (!incomeSlices.isEmpty()) {
                rows.add(new LedgerRow.Charts(Transaction.INCOME, incomeSlices));
            }
        }
        if (shown.isEmpty()) {
            if (loaded) {
                if (all.isEmpty()) {
                    rows.add(new LedgerRow.Empty(
                            context.getString(R.string.empty_title),
                            context.getString(R.string.empty_subtitle)
                    ));
                } else {
                    rows.add(new LedgerRow.Empty(
                            context.getString(R.string.empty_filter_title),
                            context.getString(R.string.empty_filter_subtitle)
                    ));
                }
            }
        } else {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(zone);
            Map<LocalDate, List<Transaction>> grouped = new LinkedHashMap<>();
            for (int i = 0; i < shown.size(); i++) {
                Transaction transaction = shown.get(i);
                LocalDate date = Instant.ofEpochMilli(transaction.occurredAt).atZone(zone).toLocalDate();
                List<Transaction> bucket = grouped.get(date);
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    grouped.put(date, bucket);
                }
                bucket.add(transaction);
            }
            for (Map.Entry<LocalDate, List<Transaction>> entry : grouped.entrySet()) {
                long dayIncome = 0L;
                long dayExpense = 0L;
                List<Transaction> items = entry.getValue();
                for (int i = 0; i < items.size(); i++) {
                    Transaction transaction = items.get(i);
                    if (transaction.type == Transaction.INCOME) {
                        dayIncome += transaction.amountCents;
                    } else {
                        dayExpense += transaction.amountCents;
                    }
                }
                rows.add(new LedgerRow.Day(
                        dayLabel(entry.getKey(), today, context),
                        dayTotal(dayIncome, dayExpense, context)
                ));
                for (int i = 0; i < items.size(); i++) {
                    rows.add(new LedgerRow.Entry(items.get(i), i < items.size() - 1));
                }
            }
        }

        int days = elapsedDays(rangeStart, rangeEnd);
        String daily = "";
        if (days > 0 && expense > 0L) {
            daily = context.getString(R.string.daily_average, Money.format(expense / days));
        }
        long balance = income - expense;
        YearMonth currentMonth = YearMonth.now();
        boolean currentMonthRange = rangeStart.equals(currentMonth.atDay(1))
                && rangeEnd.equals(currentMonth.atEndOfMonth());
        return new LedgerUiState(
                rangeLabel(rangeStart, rangeEnd, context),
                Money.format(balance),
                balance < 0L,
                Money.format(income),
                Money.format(expense),
                income,
                expense,
                daily,
                filter,
                !currentMonthRange,
                rows
        );
    }

    private static List<CategoryTotal> totalsFor(List<Transaction> all, int type) {
        Map<String, Long> totals = new HashMap<>();
        for (int i = 0; i < all.size(); i++) {
            Transaction transaction = all.get(i);
            if (transaction.type != type) {
                continue;
            }
            Long previous = totals.get(transaction.categoryKey);
            totals.put(transaction.categoryKey, (previous == null ? 0L : previous) + transaction.amountCents);
        }
        List<CategoryTotal> slices = new ArrayList<>();
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            if (entry.getValue() > 0L) {
                slices.add(new CategoryTotal(entry.getKey(), entry.getValue()));
            }
        }
        Collections.sort(slices, (left, right) -> Long.compare(right.cents, left.cents));
        return slices;
    }

    private static int elapsedDays(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        LocalDate countedEnd = end.isAfter(today) ? today : end;
        if (countedEnd.isBefore(start)) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(start, countedEnd) + 1;
        if (days > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) days;
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

    private static String dayTotal(long income, long expense, Context context) {
        if (income > 0L && expense == 0L) {
            return context.getString(R.string.day_income, Money.format(income));
        }
        if (expense > 0L && income == 0L) {
            return context.getString(R.string.day_expense, Money.format(expense));
        }
        return context.getString(R.string.day_balance, Money.format(income - expense));
    }
}
