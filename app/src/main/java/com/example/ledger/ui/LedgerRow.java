package com.example.ledger.ui;

import com.example.ledger.data.CategoryTotal;
import com.example.ledger.data.Transaction;

import java.util.List;

public abstract class LedgerRow {
    public static final int CATEGORIES = 1;
    public static final int DAY = 2;
    public static final int TRANSACTION = 3;
    public static final int EMPTY = 4;
    public static final int CHARTS = 5;

    public abstract int viewType();

    public static final class Categories extends LedgerRow {
        public final String title;
        public final List<CategoryTotal> totals;

        public Categories(String title, List<CategoryTotal> totals) {
            this.title = title;
            this.totals = totals;
        }

        @Override
        public int viewType() {
            return CATEGORIES;
        }
    }

    public static final class Charts extends LedgerRow {
        public final int type;
        public final List<CategoryTotal> slices;

        public Charts(int type, List<CategoryTotal> slices) {
            this.type = type;
            this.slices = slices;
        }

        @Override
        public int viewType() {
            return CHARTS;
        }
    }

    public static final class Day extends LedgerRow {
        public final String label;
        public final String total;

        public Day(String label, String total) {
            this.label = label;
            this.total = total;
        }

        @Override
        public int viewType() {
            return DAY;
        }
    }

    public static final class Entry extends LedgerRow {
        public final Transaction transaction;
        public final boolean showDivider;

        public Entry(Transaction transaction, boolean showDivider) {
            this.transaction = transaction;
            this.showDivider = showDivider;
        }

        @Override
        public int viewType() {
            return TRANSACTION;
        }
    }

    public static final class Empty extends LedgerRow {
        public final String title;
        public final String subtitle;

        public Empty(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        @Override
        public int viewType() {
            return EMPTY;
        }
    }
}
