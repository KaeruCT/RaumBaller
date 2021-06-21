package com.kaeruct.raumballer.ship.enemy;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.NanoCannon;
import com.kaeruct.raumballer.ship.EnemyShip;

public class SparkEye extends EnemyShip {

    public SparkEye(double x, double y, double vel, double angle, AndroidGame game) {
        super(x, y, "sparkeye", 20, game);

        this.velocity = vel;
        this.angle = angle;
        this.cannons.add(new NanoCannon());
        this.explosionColor = "red";
        this.width = 16;
        this.sturdy = true;
    }

    public void move() {
        super.move();

        if (game.t % 50 == 0) {
            double ang = game.atan2(y - game.getPlayer().y, game.getPlayer().x - x);
            shoot(ang + game.random(-0.5, 0.5));

            if (game.random(0, 1, 1) == 1) {
                this.angle = game.atan2(y - game.viewHeight() / 2, game.viewWidth() / 2 - x);
            }
        }
    }
}
