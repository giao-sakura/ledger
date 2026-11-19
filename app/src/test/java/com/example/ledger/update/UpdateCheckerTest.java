package com.example.ledger.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateCheckerTest {
    @Test
    public void comparesNumericSegments() {
        assertEquals(0, UpdateChecker.compareVersions("1.0", "v1.0"));
        assertTrue(UpdateChecker.compareVersions("1.0", "1.1") < 0);
        assertTrue(UpdateChecker.compareVersions("1.10", "1.9") > 0);
        assertTrue(UpdateChecker.compareVersions("1.0", "1.0.1") < 0);
        assertEquals(0, UpdateChecker.compareVersions("V1.2.0", "1.2"));
    }
}
