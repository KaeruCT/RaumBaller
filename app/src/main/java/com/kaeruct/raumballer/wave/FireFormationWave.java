package com.kaeruct.raumballer.wave;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.LevelReader;

public class FireFormationWave extends Wave {

    public FireFormationWave(AndroidGame game, LevelReader r, int maxAmount) {
        super(game, r, maxAmount * 3); // *3 because it spawns in triads
        this.setFreq(50);
        this.setShipClass("FireStriker");
    }

    public void spawn() {

        double x = game.getPlayer().x + game.random(-64, 64);

        super.spawn(x - 8, -24, 2.2, Math.PI * 3 / 2 - 0.2);
        super.spawn(x, -16, 1.8, Math.PI * 3 / 2);
        super.spawn(x + 8, -24, 2.2, Math.PI * 3 / 2 + 0.2);
    }
}
