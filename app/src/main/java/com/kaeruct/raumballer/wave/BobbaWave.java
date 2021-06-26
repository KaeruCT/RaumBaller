package com.kaeruct.raumballer.wave;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.LevelReader;
import com.kaeruct.raumballer.ship.Ship;
import com.kaeruct.raumballer.ship.enemy.Asterisk;

public class BobbaWave extends Wave {

    public BobbaWave(AndroidGame game, LevelReader r, int maxAmount) {
        super(game, r, maxAmount); // it spawns 8 enemies per spawn
        this.setFreq(200);
        this.setShipClass("BobbaDestroyer");
    }

    public void spawn() {
        double center = game.viewWidth() / 2;

        Ship parent = super.spawn(
                game.random(center - 44, center + 44),
                -42,
                0.5,
                Math.PI * 3 / 2 - game.random(-0.1, 0.1)
        );

        double num = 16;

        for (int i = 0; i < num; i++) {
            double ang = Math.PI * (2 * (num / 2 - i) / num);

            Asterisk a = (Asterisk) super.spawn(
                    parent.x,
                    parent.y,
                    0.8,
                    ang,
                    "Asterisk",
                    parent
            );
            a.radius = 54;
            a.wavy = true;
            if (i % 2 == 1) a.odd = true;
        }
    }

}