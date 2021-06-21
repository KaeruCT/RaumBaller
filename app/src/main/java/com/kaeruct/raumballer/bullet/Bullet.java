package com.kaeruct.raumballer.bullet;

import com.kaeruct.raumballer.Explosion;

import jgame.JGObject;

public abstract class Bullet extends JGObject {

    private final double angle;
    protected int velocity;
    public double damage;
    public int width;

    public Bullet(double x, double y, String anim, int colid, double angle) {
        super("bullet", true, x, y, colid, anim);

        this.velocity = 4;
        this.damage = 1;
        this.width = 8;
        this.angle = angle;
        this.expiry = -2;

        // center
        this.x -= width / 2;

        if (!(angle >= 0 && angle <= Math.PI)) {
            this.setAnim(anim + "u");
        }
    }

    public void move() {
        x += Math.cos(angle) * velocity * gamespeed;
        y -= Math.sin(angle) * velocity * gamespeed;
    }

    public void hit(JGObject obj) {

        if (obj instanceof Bullet) {
            new Explosion(x, y, "blue");
            this.remove();
        }
    }
}
