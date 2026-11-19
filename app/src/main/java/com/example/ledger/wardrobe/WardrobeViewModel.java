package com.example.ledger.wardrobe;

import android.app.Application;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ledger.R;
import com.example.ledger.data.AppDatabase;
import com.example.ledger.data.ClothingItem;
import com.example.ledger.data.Outfit;
import com.example.ledger.data.OutfitLink;
import com.example.ledger.data.OutfitPlan;
import com.example.ledger.data.WardrobeRepository;
import com.example.ledger.data.WishItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WardrobeViewModel extends AndroidViewModel {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WardrobeRepository repository;
    private final CityStore cities;
    private final MutableLiveData<WardrobeUi> ui = new MutableLiveData<>();
    private final MutableLiveData<String> notice = new MutableLiveData<>();
    private final ExecutorService weatherIo = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "wardrobe-weather"));
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, WeatherClient> weatherCache = new HashMap<>();
    private List<ClothingItem> clothes = new ArrayList<>();
    private List<Outfit> outfits = new ArrayList<>();
    private List<OutfitLink> links = new ArrayList<>();
    private List<OutfitPlan> plans = new ArrayList<>();
    private List<WishItem> wishes = new ArrayList<>();
    private String category = "";
    private int season;
    private String color = "";

    private boolean loaded;

    public WardrobeViewModel(Application application) {
        super(application);
        repository = new WardrobeRepository(AppDatabase.get(application).wardrobeDao());
        cities = new CityStore(application);
        publish();
        reload();
    }

    public LiveData<WardrobeUi> getUi() {
        return ui;
    }

    public LiveData<String> getNotice() {
        return notice;
    }

    public void consumeNotice() {
        notice.setValue(null);
    }

    public void filter(String category, int season, String color) {
        this.category = category == null ? "" : category;
        this.season = season;
        this.color = color == null ? "" : color;
        publish();
    }

    public ClothingItem find(long id) {
        for (int i = 0; i < clothes.size(); i++) {
            if (clothes.get(i).id == id) {
                return clothes.get(i);
            }
        }
        return null;
    }

    public List<ClothingItem> allClothes() {
        return clothes;
    }

    public void saveClothing(ClothingItem item, long wishId) {
        String previous = item.id == 0 ? null : imageOf(item.id);
        repository.saveClothing(item, () -> {
            if (previous != null && item.imagePath != null && !previous.equals(item.imagePath)) {
                ClothingImages.delete(previous);
            }
            if (wishId > 0) {
                repository.deleteWish(wishId, this::reload);
            } else {
                reload();
            }
        });
    }

    public void deleteClothing(ClothingItem item) {
        ClothingImages.delete(item.imagePath);
        repository.deleteClothing(item, this::reload);
    }

    public void saveOutfit(String name, List<Long> itemIds) {
        repository.saveOutfit(name, itemIds, this::reload);
    }

    public void deleteOutfit(Outfit outfit) {
        repository.deleteOutfit(outfit, this::reload);
    }

    public void plan(long outfitId, LocalDate date) {
        repository.plan(outfitId, date.format(DAY), () -> {
            reload();
            fetchWeather(date);
        });
    }

    public void saveWish(String need, String category) {
        WishItem item = new WishItem();
        item.need = need;
        item.category = category;
        repository.saveWish(item, this::reload);
    }

    public void deleteWish(long id) {
        repository.deleteWish(id, this::reload);
    }

    public void setCity(String city) {
        String trimmed = city == null ? "" : city.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        weatherIo.execute(() -> {
            try {
                double[] point = WeatherClient.locate(trimmed);
                if (point == null) {
                    main.post(() -> notice.setValue(getApplication().getString(R.string.city_failed)));
                    return;
                }
                main.post(() -> {
                    cities.save(trimmed, point[0], point[1]);
                    weatherCache.clear();
                    reload();
                });
            } catch (Exception ignored) {
                // The city label stays unchanged when lookup fails.
            }
        });
    }

    @Override
    protected void onCleared() {
        repository.close();
        weatherIo.shutdown();
    }

    private void reload() {
        repository.load(snapshot -> {
            clothes = snapshot.clothes;
            outfits = snapshot.outfits;
            links = snapshot.links;
            plans = snapshot.plans;
            wishes = snapshot.wishes;
            loaded = true;
            publish();
            refreshWeather();
        });
    }

    private void refreshWeather() {
        if (!cities.located()) {
            return;
        }
        for (int i = 0; i < plans.size(); i++) {
            LocalDate date = LocalDate.parse(plans.get(i).planDate);
            if (!weatherCache.containsKey(plans.get(i).planDate)) {
                fetchWeather(date);
            }
        }
    }

    private void fetchWeather(LocalDate date) {
        if (!cities.located()) {
            return;
        }
        String key = date.format(DAY);
        double lat = cities.latitude();
        double lon = cities.longitude();
        weatherIo.execute(() -> {
            try {
                WeatherClient day = WeatherClient.day(lat, lon, date);
                if (day != null) {
                    main.post(() -> {
                        weatherCache.put(key, day);
                        publish();
                    });
                }
            } catch (Exception ignored) {
                // Leave the date without a forecast instead of caching the failure.
            }
        });
    }

    private void publish() {
        ui.postValue(new WardrobeUi(filteredClothes(), outfitCards(), wishes, cities.city(), loaded));
    }

    private List<ClothingItem> filteredClothes() {
        List<ClothingItem> shown = new ArrayList<>();
        for (int i = 0; i < clothes.size(); i++) {
            ClothingItem item = clothes.get(i);
            if (!category.isEmpty() && !category.equals(item.category)) {
                continue;
            }
            if (season != 0 && (item.seasons & season) == 0) {
                continue;
            }
            if (!color.isEmpty() && !color.equals(item.color)) {
                continue;
            }
            shown.add(item);
        }
        return shown;
    }

    private List<WardrobeUi.OutfitCard> outfitCards() {
        List<WardrobeUi.OutfitCard> cards = new ArrayList<>();
        for (int i = 0; i < outfits.size(); i++) {
            Outfit outfit = outfits.get(i);
            List<ClothingItem> items = itemsFor(outfit.id);
            OutfitPlan plan = nearestPlan(outfit.id);
            String weather = "";
            int level = -1;
            if (plan != null && weatherCache.containsKey(plan.planDate) && weatherCache.get(plan.planDate) != null) {
                WeatherClient day = weatherCache.get(plan.planDate);
                WeatherFit fit = WeatherFit.judge(seasonsOf(items), day.maxC, WeatherFit.wet(day.code));
                level = fit.level;
                weather = weatherLine(plan.planDate, day, fit);
            } else if (plan != null) {
                weather = planLabel(plan.planDate);
            }
            cards.add(new WardrobeUi.OutfitCard(outfit, items, plan, weather, level));
        }
        return cards;
    }

    private List<ClothingItem> itemsFor(long outfitId) {
        List<ClothingItem> items = new ArrayList<>();
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).outfitId != outfitId) {
                continue;
            }
            ClothingItem item = find(links.get(i).itemId);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private OutfitPlan nearestPlan(long outfitId) {
        String today = LocalDate.now().format(DAY);
        OutfitPlan future = null;
        OutfitPlan past = null;
        for (int i = 0; i < plans.size(); i++) {
            OutfitPlan plan = plans.get(i);
            if (plan.outfitId != outfitId) {
                continue;
            }
            if (plan.planDate.compareTo(today) >= 0) {
                if (future == null || plan.planDate.compareTo(future.planDate) < 0) {
                    future = plan;
                }
            } else if (past == null || plan.planDate.compareTo(past.planDate) > 0) {
                past = plan;
            }
        }
        return future != null ? future : past;
    }

    private static int seasonsOf(List<ClothingItem> items) {
        int mask = 0;
        for (int i = 0; i < items.size(); i++) {
            mask |= items.get(i).seasons;
        }
        return mask;
    }

    private String weatherLine(String date, WeatherClient day, WeatherFit fit) {
        LocalDate local = LocalDate.parse(date);
        String when = getApplication().getString(R.string.month_day, local.getMonthValue(), local.getDayOfMonth());
        String range = Math.round(day.minC) + "–" + Math.round(day.maxC) + "°";
        return when + "  " + range + "  " + reason(fit.reason);
    }

    private String planLabel(String date) {
        LocalDate local = LocalDate.parse(date);
        return getApplication().getString(R.string.month_day, local.getMonthValue(), local.getDayOfMonth());
    }

    private String reason(int code) {
        int res = R.string.fit_ok;
        if (code == WeatherFit.REASON_HOT) {
            res = R.string.fit_hot;
        } else if (code == WeatherFit.REASON_COLD) {
            res = R.string.fit_cold;
        } else if (code == WeatherFit.REASON_RAIN) {
            res = R.string.fit_rain;
        } else if (code == WeatherFit.REASON_HOT_RAIN) {
            res = R.string.fit_hot_rain;
        } else if (code == WeatherFit.REASON_COLD_RAIN) {
            res = R.string.fit_cold_rain;
        }
        return getApplication().getString(res);
    }

    private String imageOf(long id) {
        ClothingItem item = find(id);
        return item == null ? null : item.imagePath;
    }

    public static int colorOf(String name) {
        if ("白".equals(name)) return Color.parseColor("#F4F1EA");
        if ("灰".equals(name)) return Color.parseColor("#8E877C");
        if ("米".equals(name)) return Color.parseColor("#E4D2B0");
        if ("卡其".equals(name)) return Color.parseColor("#C2A36B");
        if ("棕".equals(name)) return Color.parseColor("#7A4E2D");
        if ("红".equals(name)) return Color.parseColor("#A33B32");
        if ("橙".equals(name)) return Color.parseColor("#D06A2B");
        if ("黄".equals(name)) return Color.parseColor("#D6B13A");
        if ("绿".equals(name)) return Color.parseColor("#3E7A52");
        if ("蓝".equals(name)) return Color.parseColor("#3E6296");
        if ("紫".equals(name)) return Color.parseColor("#6E538A");
        if ("粉".equals(name)) return Color.parseColor("#D9899A");
        return Color.parseColor("#1C1915");
    }
}
