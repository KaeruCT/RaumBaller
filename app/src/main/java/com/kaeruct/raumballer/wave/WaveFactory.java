package com.kaeruct.raumballer.wave;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.LevelReader;

public final class WaveFactory {
    private WaveFactory() {
    }

    public static Wave create(String name, AndroidGame game, LevelReader reader, int maxAmount) {
        if ("SparkWave".equals(name)) return new SparkWave(game, reader, maxAmount);
        if ("FireFormationWave".equals(name)) return new FireFormationWave(game, reader, maxAmount);
        if ("SpaceBallWave".equals(name)) return new SpaceBallWave(game, reader, maxAmount);
        if ("SparkEyeWave".equals(name)) return new SparkEyeWave(game, reader, maxAmount);
        if ("CibumWave".equals(name)) return new CibumWave(game, reader, maxAmount);
        if ("BobbaWave".equals(name)) return new BobbaWave(game, reader, maxAmount);
        throw new IllegalArgumentException("Unknown wave class: " + name);
    }
}
