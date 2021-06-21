package com.kaeruct.raumballer.cannon;

import com.kaeruct.raumballer.bullet.BitBullet;

public class BitWaveCannon extends Cannon {

    public BitWaveCannon() {
        super();
        waitTime = 3;
    }

    public void shoot(double x, double y, double angle, int colid, int t) {

        if (canShoot(t)) {
            double v = (Math.cos(t * 4) / 4);
            new BitBullet(x, y, colid, angle - v);
            new BitBullet(x, y, colid, angle + v);

            //game.playAudio("shoot");
        }
    }
}
