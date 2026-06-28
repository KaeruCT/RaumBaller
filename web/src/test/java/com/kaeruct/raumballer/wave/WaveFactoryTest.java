package com.kaeruct.raumballer.wave;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WaveFactoryTest {
    @Test
    public void createsEveryLevelReferencedWave() {
        assertTrue(WaveFactory.create("SparkWave", null, null, 1) instanceof SparkWave);
        assertTrue(WaveFactory.create("FireFormationWave", null, null, 1) instanceof FireFormationWave);
        assertTrue(WaveFactory.create("SpaceBallWave", null, null, 1) instanceof SpaceBallWave);
        assertTrue(WaveFactory.create("SparkEyeWave", null, null, 1) instanceof SparkEyeWave);
        assertTrue(WaveFactory.create("CibumWave", null, null, 1) instanceof CibumWave);
        assertTrue(WaveFactory.create("BobbaWave", null, null, 1) instanceof BobbaWave);
    }

    @Test
    public void rejectsUnknownWave() {
        try {
            WaveFactory.create("MissingWave", null, null, 1);
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown wave class: MissingWave", e.getMessage());
            return;
        }
        throw new AssertionError("Expected an IllegalArgumentException");
    }
}
