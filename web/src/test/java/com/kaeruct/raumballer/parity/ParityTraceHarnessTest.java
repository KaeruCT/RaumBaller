package com.kaeruct.raumballer.parity;

import com.kaeruct.raumballer.web.ParityScenarios;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jgame.JGObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParityTraceHarnessTest {
    @Before
    public void enableHeadlessMode() {
        System.setProperty("raumballer.headless", "true");
        System.setProperty("raumballer.seed", "2026");
        JGObject.setEngine(null);
    }

    @After
    public void resetEngine() {
        JGObject.setEngine(null);
        System.clearProperty("raumballer.headless");
        System.clearProperty("raumballer.seed");
    }

    @Test
    public void scriptedScenariosProduceDeterministicStateAndAudioTraces() {
        for (String name : ParityScenarios.NAMES) {
            assertScenario(name, ParityScenarios.run(name));
        }
    }

    private void assertScenario(String name, String trace) {
        assertTrue(name + " should include frame", trace.contains("frame="));
        assertTrue(name + " should include state", trace.contains("state="));
        assertTrue(name + " should include score", trace.contains("score="));
        assertTrue(name + " should include objects", trace.contains("objects="));
        assertTrue(name + " should include audio events", trace.contains("audio="));
        assertEquals(name + " deterministic trace checksum", ParityScenarios.expectedChecksum(name), trace.hashCode());
    }
}
