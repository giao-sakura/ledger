package com.example.ledger.ui;

import com.example.ledger.data.DiaryEntry;

public abstract class DiaryRow {
    public static final int DAY = 1;
    public static final int ENTRY = 2;
    public static final int EMPTY = 3;

    public abstract int viewType();

    public static final class Day extends DiaryRow {
        public final String label;

        public Day(String label) {
            this.label = label;
        }

        @Override
        public int viewType() {
            return DAY;
        }
    }

    public static final class Entry extends DiaryRow {
        public final DiaryEntry entry;
        public final boolean showDivider;

        public Entry(DiaryEntry entry, boolean showDivider) {
            this.entry = entry;
            this.showDivider = showDivider;
        }

        @Override
        public int viewType() {
            return ENTRY;
        }
    }

    public static final class Empty extends DiaryRow {
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
