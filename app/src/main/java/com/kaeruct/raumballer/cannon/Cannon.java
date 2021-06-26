package com.kaeruct.raumballer.cannon;

import com.kaeruct.raumballer.AndroidGame;

public abstract class Cannon {

    public static AndroidGame game;
    public int waitTime;
    public int lastShoot;

    public Cannon() {
        waitTime = 5;
        lastShoot = 0;
    }

    public boolean canShoot(int t) {
        if (t < lastShoot)
            lastShoot = 0; // t can be reset externally, so we need to take this into account

        if (t - lastShoot >= waitTime) {
            lastShoot = t;
            return true;
        }
        return false;
    }

    public abstract void shoot(double x, double y, double angle, int colid, int t);
}
