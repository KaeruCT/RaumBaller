package com.kaeruct.raumballer.wave;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.LevelReader;

public class SpaceBallWave extends Wave {

    public SpaceBallWave(AndroidGame game, LevelReader r, int maxAmount) {
        super(game, r, maxAmount);
        this.setFreq(300);
        this.setShipClass("SpaceBall");
    }

    public void spawn() {

        super.spawn(
                game.random(0, game.viewWidth()),
                game.viewHeight() - 8,
                game.random(0.7, 1.6),
                Math.PI / 2 - game.random(-0.5, 0.5)
        );
    }

}