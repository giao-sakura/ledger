package com.example.ledger.wardrobe;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WeatherFitTest {
    @Test
    public void flagsThickClothesOnAHotDay() {
        WeatherFit fit = WeatherFit.judge(8, 30, false);
        assertEquals(WeatherFit.UNSUITABLE, fit.level);
        assertEquals(WeatherFit.REASON_HOT, fit.reason);
    }

    @Test
    public void flagsThinClothesOnAColdDay() {
        WeatherFit fit = WeatherFit.judge(2, 8, true);
        assertEquals(WeatherFit.UNSUITABLE, fit.level);
        assertEquals(WeatherFit.REASON_COLD_RAIN, fit.reason);
    }

    @Test
    public void acceptsAMixedOutfit() {
        WeatherFit fit = WeatherFit.judge(2 | 8, 30, false);
        assertEquals(WeatherFit.OK, fit.level);
    }

    @Test
    public void warnsOnRain() {
        assertEquals(true, WeatherFit.wet(61));
        assertEquals(false, WeatherFit.wet(1));
        WeatherFit fit = WeatherFit.judge(2 | 4, 20, true);
        assertEquals(WeatherFit.CAUTION, fit.level);
        assertEquals(WeatherFit.REASON_RAIN, fit.reason);
    }
}
