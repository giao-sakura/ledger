package com.example.ledger.wardrobe;

import android.content.Context;
import android.content.SharedPreferences;

public final class CityStore {
    private final SharedPreferences prefs;

    public CityStore(Context context) {
        prefs = context.getSharedPreferences("wardrobe_city", Context.MODE_PRIVATE);
    }

    public String city() {
        return prefs.getString("city", "");
    }

    public boolean located() {
        return prefs.contains("lat") && city() != null && !city().isEmpty();
    }

    public double latitude() {
        return Double.longBitsToDouble(prefs.getLong("lat", 0L));
    }

    public double longitude() {
        return Double.longBitsToDouble(prefs.getLong("lon", 0L));
    }

    public void save(String city, double latitude, double longitude) {
        prefs.edit()
                .putString("city", city)
                .putLong("lat", Double.doubleToRawLongBits(latitude))
                .putLong("lon", Double.doubleToRawLongBits(longitude))
                .apply();
    }
}
