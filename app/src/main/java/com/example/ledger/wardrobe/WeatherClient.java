package com.example.ledger.wardrobe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public final class WeatherClient {
    public final double maxC;
    public final double minC;
    public final int code;

    public WeatherClient(double maxC, double minC, int code) {
        this.maxC = maxC;
        this.minC = minC;
        this.code = code;
    }

    public static double[] locate(String city) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=zh&name="
                + URLEncoder.encode(city, StandardCharsets.UTF_8.name());
        JSONObject json = new JSONObject(get(url));
        JSONArray results = json.optJSONArray("results");
        if (results == null || results.length() == 0) {
            return null;
        }
        JSONObject first = results.getJSONObject(0);
        return new double[]{first.getDouble("latitude"), first.getDouble("longitude")};
    }

    public static WeatherClient day(double latitude, double longitude, LocalDate date) throws Exception {
        LocalDate today = LocalDate.now();
        String base = date.isBefore(today)
                ? "https://archive-api.open-meteo.com/v1/archive"
                : "https://api.open-meteo.com/v1/forecast";
        if (!date.isBefore(today) && date.isAfter(today.plusDays(15))) {
            return null;
        }
        String url = base
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=auto"
                + "&start_date=" + date
                + "&end_date=" + date;
        JSONObject daily = new JSONObject(get(url)).optJSONObject("daily");
        if (daily == null) {
            return null;
        }
        JSONArray max = daily.optJSONArray("temperature_2m_max");
        JSONArray min = daily.optJSONArray("temperature_2m_min");
        JSONArray codes = daily.optJSONArray("weather_code");
        if (max == null || min == null || codes == null || max.length() == 0) {
            return null;
        }
        return new WeatherClient(max.getDouble(0), min.getDouble(0), codes.getInt(0));
    }

    private static String get(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "ledger-android");
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new java.io.IOException("http " + connection.getResponseCode());
            }
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
