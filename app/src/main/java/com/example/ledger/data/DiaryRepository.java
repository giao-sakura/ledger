package com.example.ledger.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class DiaryRepository {
    private static final String TAG = "Ledger";

    private final DiaryDao dao;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "diary-db"));

    public DiaryRepository(DiaryDao dao) {
        this.dao = dao;
    }

    public void listBetween(long start, long end, Consumer<List<DiaryEntry>> callback) {
        io.execute(() -> {
            List<DiaryEntry> list;
            try {
                list = dao.listBetween(start, end);
            } catch (RuntimeException error) {
                Log.e(TAG, "diary read failed", error);
                list = new ArrayList<>();
            }
            List<DiaryEntry> result = list;
            main.post(() -> callback.accept(result));
        });
    }

    public void save(DiaryEntry entry, Runnable after) {
        io.execute(() -> {
            try {
                if (entry.id == 0) {
                    dao.insert(entry);
                } else {
                    dao.update(entry);
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "diary save failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void insert(DiaryEntry entry, Runnable after) {
        io.execute(() -> {
            try {
                dao.insert(entry);
            } catch (RuntimeException error) {
                Log.e(TAG, "diary insert failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void delete(DiaryEntry entry, Runnable after) {
        io.execute(() -> {
            try {
                dao.delete(entry);
            } catch (RuntimeException error) {
                Log.e(TAG, "diary delete failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void close() {
        io.shutdown();
    }
}
