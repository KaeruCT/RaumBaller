package com.kaeruct.raumballer.bullet;

public class FireBullet extends Bullet {

    public FireBullet(double x, double y, int colid, double angle) {
        super(x, y, "bul4", colid, angle);
        this.velocity = 4;
        this.damage = 2;
    }
}
