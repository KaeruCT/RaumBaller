package com.kaeruct.raumballer.ship.enemy;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.SparkCannon;
import com.kaeruct.raumballer.ship.EnemyShip;

/**
 * Basic quick ship which attempts to stay inside the screen as long as possible
 */
public class SparkDefender extends EnemyShip {

    public SparkDefender(double x, double y, double vel, double angle, AndroidGame game) {
        super(x, y, "sparkdefender", 15, game);

        this.velocity = vel;
        this.angle = angle;
        this.cannons.add(new SparkCannon());
        this.explosionColor = "green";
        this.width = 16;
    }

    public void move() {
        super.move();

        if (game.t % 30 == 0) {
            shoot(Math.PI * 3 / 2);
        }
        // only attempt to stay in screen if not near boundaries
        if (x >= 8 && x + 8 <= game.viewWidth()) {
            if (y + 32 >= game.viewHeight() && game.goingDown(angle) ||
                    y <= 32 && game.goingUp(angle)) {

                this.angle = -this.angle;
            }
        }
    }
}
