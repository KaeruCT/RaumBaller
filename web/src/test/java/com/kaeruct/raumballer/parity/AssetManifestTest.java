package com.kaeruct.raumballer.parity;

import static org.junit.Assert.assertTrue;

import com.kaeruct.raumballer.wave.WaveFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class AssetManifestTest {
    private final File assetsDir = new File("../app/src/main/assets");

    @Test
    public void shooterManifestReferencesExistingAssets() throws Exception {
        List<String> missing = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(assetsDir, "shooter.tbl")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t+");
                if (fields.length == 2 || fields.length == 8) {
                    String path = fields[1].trim();
                    if (!"-".equals(path) && !new File(assetsDir, path).exists()) {
                        missing.add(path);
                    }
                }
            }
        }

        assertTrue("Missing manifest assets: " + missing, missing.isEmpty());
    }

    @Test
    public void levelsOnlyReferenceExplicitWaveFactories() throws Exception {
        List<String> missing = new ArrayList<>();
        for (int level = 1; level <= 4; level++) {
            try (BufferedReader reader = new BufferedReader(new FileReader(new File(assetsDir, "level" + level + ".lvl")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] waveSpecs = line.split("\\+");
                    for (String waveSpec : waveSpecs) {
                        String[] fields = waveSpec.trim().split("[ \\t]");
                        String waveName = fields[0].trim();
                        try {
                            WaveFactory.create(waveName, null, null, 1);
                        } catch (IllegalArgumentException e) {
                            missing.add(waveName);
                        }
                    }
                }
            }
        }

        assertTrue("Missing wave factories: " + missing, missing.isEmpty());
    }
}
