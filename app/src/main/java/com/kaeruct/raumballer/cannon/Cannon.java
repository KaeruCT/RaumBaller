package com.kaeruct.raumballer.cannon;

import com.kaeruct.raumballer.AndroidGame;

public abstract class Cannon {

    public int waitTime;
    public static AndroidGame game;
    public int lastShoot;

    public Cannon() {
        waitTime = 5;
        lastShoot = 0;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setLastShoot(int t) {
        lastShoot = t;
    }

    public int getLastShoot() {
        return lastShoot;
    }

    public boolean canShoot(int t) {
        if (t - getLastShoot() >= getWaitTime()) {
            setLastShoot(t);
            return true;
        }
        return false;
    }

    public abstract void shoot(double x, double y, double angle, int colid, int t);
}
