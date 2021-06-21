package com.kaeruct.raumballer.wave;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.LevelReader;

public class CibumWave extends Wave {

    public CibumWave(AndroidGame game, LevelReader r, int maxAmount) {
        super(game, r, maxAmount);
        this.setFreq(200);
        this.setShipClass("CibumDestroyer");
    }

    public void spawn() {

        double center = game.viewWidth() / 2;

        super.spawn(
                game.random(center - 64, center + 64),
                -32,
                0.5,
                Math.PI * 3 / 2 - game.random(-0.1, 0.1)
        );
    }
}
