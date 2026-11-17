package com.example.ledger.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class LedgerRepository {
    private static final String TAG = "Ledger";

    private final TransactionDao dao;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "ledger-db"));

    public LedgerRepository(TransactionDao dao) {
        this.dao = dao;
    }

    public void listBetween(long start, long end, Consumer<List<Transaction>> callback) {
        io.execute(() -> {
            List<Transaction> list;
            try {
                list = dao.listBetween(start, end);
            } catch (RuntimeException error) {
                Log.e(TAG, "read failed", error);
                list = new ArrayList<>();
            }
            List<Transaction> result = list;
            main.post(() -> callback.accept(result));
        });
    }

    public void save(Transaction transaction, Runnable after) {
        io.execute(() -> {
            try {
                if (transaction.id == 0) {
                    dao.insert(transaction);
                } else {
                    dao.update(transaction);
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "save failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void insert(Transaction transaction, Runnable after) {
        io.execute(() -> {
            try {
                dao.insert(transaction);
            } catch (RuntimeException error) {
                Log.e(TAG, "insert failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void delete(Transaction transaction, Runnable after) {
        io.execute(() -> {
            try {
                dao.delete(transaction);
            } catch (RuntimeException error) {
                Log.e(TAG, "delete failed", error);
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
