package com.kaeruct.raumballer.cannon;

import com.kaeruct.raumballer.bullet.PlasmaBullet;

public class SoloPlasmaCannon extends Cannon {

    public SoloPlasmaCannon() {
        super();
        waitTime = 5;
    }

    public void shoot(double x, double y, double angle, int colid, int t) {

        if (canShoot(t)) {

            new PlasmaBullet(x, y, colid, angle);
            //game.playAudio("shoot");
        }
    }
}
