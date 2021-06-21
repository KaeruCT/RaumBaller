package com.kaeruct.raumballer.cannon;

import com.kaeruct.raumballer.bullet.PlasmaBullet;

public class PlasmaCannon extends Cannon {

    public PlasmaCannon() {
        super();
        waitTime = 16;
    }

    public void shoot(double x, double y, double angle, int colid, int t) {

        if (canShoot(t)) {
            new PlasmaBullet(x, y, colid, angle);
            new PlasmaBullet(x - 2, y - 4, colid, angle);
            new PlasmaBullet(x + 2, y - 4, colid, angle);
            //game.playAudio("shoot");
        }
    }
}
