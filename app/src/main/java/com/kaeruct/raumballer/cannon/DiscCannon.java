package com.kaeruct.raumballer.cannon;

import com.kaeruct.raumballer.bullet.DiscBullet;

public class DiscCannon extends Cannon {

    public DiscCannon() {
        super();
        waitTime = 4;
    }

    public void shoot(double x, double y, double angle, int colid, int t) {

        if (canShoot(t)) {
            new DiscBullet(x, y, colid, angle);
            new DiscBullet(x, y - 8, colid, angle);

        }
    }
}
