package com.example.ledger.wardrobe;

public final class WeatherFit {
    public static final int OK = 0;
    public static final int CAUTION = 1;
    public static final int UNSUITABLE = 2;

    public static final int REASON_OK = 0;
    public static final int REASON_HOT = 1;
    public static final int REASON_COLD = 2;
    public static final int REASON_RAIN = 3;
    public static final int REASON_HOT_RAIN = 4;
    public static final int REASON_COLD_RAIN = 5;

    public final int level;
    public final int reason;

    private WeatherFit(int level, int reason) {
        this.level = level;
        this.reason = reason;
    }

    public static WeatherFit judge(int seasons, double maxC, boolean wet) {
        boolean hot = seasons != 0 && maxC >= 26 && has(seasons, 8) && !has(seasons, 2);
        boolean cold = seasons != 0 && maxC <= 12 && has(seasons, 2) && !has(seasons, 8);
        boolean warm = seasons != 0 && !hot && maxC >= 22 && has(seasons, 8) && !has(seasons, 2) && !has(seasons, 1) && !has(seasons, 4);
        if (hot && wet) {
            return new WeatherFit(UNSUITABLE, REASON_HOT_RAIN);
        }
        if (cold && wet) {
            return new WeatherFit(UNSUITABLE, REASON_COLD_RAIN);
        }
        if (hot) {
            return new WeatherFit(UNSUITABLE, REASON_HOT);
        }
        if (cold) {
            return new WeatherFit(UNSUITABLE, REASON_COLD);
        }
        if (wet) {
            return new WeatherFit(CAUTION, REASON_RAIN);
        }
        if (warm) {
            return new WeatherFit(CAUTION, REASON_HOT);
        }
        return new WeatherFit(OK, REASON_OK);
    }

    public static boolean wet(int weatherCode) {
        return (weatherCode >= 51 && weatherCode <= 67)
                || (weatherCode >= 71 && weatherCode <= 77)
                || (weatherCode >= 80 && weatherCode <= 82)
                || weatherCode >= 95;
    }

    private static boolean has(int seasons, int bit) {
        return (seasons & bit) != 0;
    }
}
