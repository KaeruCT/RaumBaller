package com.kaeruct.raumballer.ship.enemy;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.ship.EnemyShip;

/**
 * Small ship that orbits a parent ship (SparkEye)
 */
public class Asterisk extends EnemyShip {

    private double offset;
    public double radius;
    public double speed = 0.01;
    public boolean wavy = false;
    public boolean odd = false;

    public Asterisk(double x, double y, double vel, double angle, AndroidGame game) {
        super(x, y, "asterisk", 5, game);

        this.velocity = vel;
        this.angle = angle;
        this.explosionColor = "blue";
        this.width = 16;
        this.radius = 12; // distance from ship to "orbit"
        this.offset = angle;
        this.sturdy = true;
        this.expiry = -1;
    }

    public void move() {
        super.move();

        x = parent.x + parent.getImageBBoxConst().width/2 - Math.cos(-offset) * radius - this.getImageBBox().width/2;
        y = parent.y + parent.getImageBBoxConst().height/2 - Math.sin(offset) * radius - this.getImageBBox().height/2;

        if (wavy) {
            radius += odd ? Math.sin(clock * 0.08) : Math.cos(clock * 0.08);
        }

        offset += speed;

        // die if parent died
        if (!parent.isAlive()) {
            this.kill();
        }
    }
}
