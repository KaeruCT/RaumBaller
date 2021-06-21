package com.kaeruct.raumballer.cannon;

public class CombinedCannon extends Cannon {

    private final Cannon[] cannons;

    public CombinedCannon(Cannon[] c) {
        super();
        this.cannons = c;
        waitTime = 1;
    }

    public void shoot(double x, double y, double angle, int colid, int t) {

        if (canShoot(t)) {
            for (int i = 0; i < cannons.length; i++) {
                cannons[i].shoot(x, y, angle, colid, t);
            }
        }
    }
}
