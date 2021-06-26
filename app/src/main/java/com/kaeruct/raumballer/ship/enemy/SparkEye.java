package com.kaeruct.raumballer.ship.enemy;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.NanoCannon;
import com.kaeruct.raumballer.ship.EnemyShip;

/**
 * Purple eye that follows the player ship (surrounded by Asterisk)
 */
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
            shoot(ang + game.random(-0.5, 0.5));
            shoot(ang + game.random(-0.5, 0.5));
        }

        if (game.t % 30 == 0 && game.random(0, 1, 1) == 1) {
            this.angle = game.atan2(y - game.viewHeight() / 2, game.viewWidth() / 2 - x);
        }

        // reverse angle if going out of the game area
        if (x >= 32 && x + 32 <= game.viewWidth()) {
            if (y + 32 >= game.viewHeight() && game.goingDown(angle) ||
                    y <= 32 && game.goingUp(angle)) {

                this.angle = -this.angle;
            }
        }
    }
}
