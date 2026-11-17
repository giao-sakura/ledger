package com.example.ledger.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class CategoryStore {
    private static final String PREF = "ledger_categories";
    private static final String KEY = "custom";

    private final SharedPreferences preferences;
    private final List<Category> custom = new ArrayList<>();

    public CategoryStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        load();
    }

    public List<Category> all() {
        List<Category> all = Category.builtins();
        all.addAll(custom);
        return all;
    }

    public void add(Category category) {
        custom.add(category);
        save();
    }

    private void load() {
        custom.clear();
        String raw = preferences.getString(KEY, "");
        if (raw == null || raw.isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                custom.add(new Category(
                        object.getString("key"),
                        object.getString("name"),
                        object.getInt("type"),
                        object.getInt("color"),
                        object.getString("mark"),
                        true
                ));
            }
        } catch (JSONException error) {
            custom.clear();
        }
    }

    private void save() {
        JSONArray array = new JSONArray();
        try {
            for (int i = 0; i < custom.size(); i++) {
                Category category = custom.get(i);
                JSONObject object = new JSONObject();
                object.put("key", category.key);
                object.put("name", category.name);
                object.put("type", category.type);
                object.put("color", category.color);
                object.put("mark", category.mark);
                array.put(object);
            }
        } catch (JSONException error) {
            return;
        }
        preferences.edit().putString(KEY, array.toString()).apply();
    }
}
