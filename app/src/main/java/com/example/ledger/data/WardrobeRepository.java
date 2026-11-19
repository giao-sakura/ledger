package com.example.ledger.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WardrobeRepository {
    private static final String TAG = "Ledger";

    private final WardrobeDao dao;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "wardrobe-db"));

    public WardrobeRepository(WardrobeDao dao) {
        this.dao = dao;
    }

    public void load(java.util.function.Consumer<Snapshot> callback) {
        io.execute(() -> {
            Snapshot snapshot;
            try {
                snapshot = new Snapshot(dao.clothes(), dao.outfits(), dao.links(), dao.plans(), dao.wishes());
            } catch (RuntimeException error) {
                Log.e(TAG, "wardrobe read failed", error);
                snapshot = Snapshot.empty();
            }
            Snapshot result = snapshot;
            main.post(() -> callback.accept(result));
        });
    }

    public void saveClothing(ClothingItem item, Runnable after) {
        io.execute(() -> {
            try {
                if (item.id == 0) {
                    if (item.createdAt == 0L) {
                        item.createdAt = System.currentTimeMillis();
                    }
                    dao.insert(item);
                } else {
                    dao.update(item);
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "wardrobe save failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void deleteClothing(ClothingItem item, Runnable after) {
        io.execute(() -> {
            try {
                dao.deleteClothing(item);
            } catch (RuntimeException error) {
                Log.e(TAG, "wardrobe delete failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void saveOutfit(String name, java.util.List<Long> itemIds, Runnable after) {
        io.execute(() -> {
            try {
                Outfit outfit = new Outfit();
                outfit.name = name;
                outfit.createdAt = System.currentTimeMillis();
                long id = dao.insert(outfit);
                for (int i = 0; i < itemIds.size(); i++) {
                    OutfitLink link = new OutfitLink();
                    link.outfitId = id;
                    link.itemId = itemIds.get(i);
                    dao.insert(link);
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "outfit save failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void deleteOutfit(Outfit outfit, Runnable after) {
        io.execute(() -> {
            try {
                dao.deleteOutfitFully(outfit);
            } catch (RuntimeException error) {
                Log.e(TAG, "outfit delete failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void plan(long outfitId, String date, Runnable after) {
        io.execute(() -> {
            try {
                OutfitPlan plan = new OutfitPlan();
                plan.outfitId = outfitId;
                plan.planDate = date;
                dao.replacePlan(plan);
            } catch (RuntimeException error) {
                Log.e(TAG, "plan save failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void saveWish(WishItem item, Runnable after) {
        io.execute(() -> {
            try {
                if (item.createdAt == 0L) {
                    item.createdAt = System.currentTimeMillis();
                }
                dao.insert(item);
            } catch (RuntimeException error) {
                Log.e(TAG, "wish save failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void deleteWish(long id, Runnable after) {
        io.execute(() -> {
            try {
                for (WishItem wish : dao.wishes()) {
                    if (wish.id == id) {
                        dao.delete(wish);
                        break;
                    }
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "wish delete failed", error);
            }
            if (after != null) {
                main.post(after);
            }
        });
    }

    public void close() {
        io.shutdown();
    }

    public static final class Snapshot {
        public final java.util.List<ClothingItem> clothes;
        public final java.util.List<Outfit> outfits;
        public final java.util.List<OutfitLink> links;
        public final java.util.List<OutfitPlan> plans;
        public final java.util.List<WishItem> wishes;

        Snapshot(
                java.util.List<ClothingItem> clothes,
                java.util.List<Outfit> outfits,
                java.util.List<OutfitLink> links,
                java.util.List<OutfitPlan> plans,
                java.util.List<WishItem> wishes
        ) {
            this.clothes = clothes;
            this.outfits = outfits;
            this.links = links;
            this.plans = plans;
            this.wishes = wishes;
        }

        static Snapshot empty() {
            return new Snapshot(
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList()
            );
        }
    }
}
