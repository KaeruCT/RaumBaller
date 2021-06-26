package com.kaeruct.raumballer.ship.enemy;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.BitWaveCannon;
import com.kaeruct.raumballer.cannon.PlasmaCannon;
import com.kaeruct.raumballer.cannon.SoloPlasmaCannon;
import com.kaeruct.raumballer.cannon.TripleFireCannon;
import com.kaeruct.raumballer.ship.EnemyShip;

/**
 * Big menacing "boss"?? ship with lots of health
 */
public class BobbaDestroyer extends EnemyShip {

    public BobbaDestroyer(double x, double y, double vel, double angle, AndroidGame game) {
        super(x, y, "bobbadestroyer", 400, game);

        this.velocity = vel;
        this.angle = angle;
        this.cannons.add(new TripleFireCannon());
        this.cannons.add(new PlasmaCannon());
        this.cannons.add(new BitWaveCannon());
        this.explosionColor = "green";
        this.setBBox(4, 4, 60, 58);
        this.width = 64;
    }

    public void move() {
        super.move();

        double ang = game.atan2(y - game.getPlayer().y, game.getPlayer().x - x);
        if (clock % 60 == 0) {
            shoot(ang);
        }

        if (clock % 25 == 0 && game.random(0, 2, 1) == 0) {
            shoot(ang + game.random(-0.3, 0.3));
            shoot(ang - game.random(-0.3, 0.3));
        }

        // only attempt to stay in screen if not near boundaries
        if (x >= 40 && x + 40 <= game.viewWidth()) {
            if (y + 40 >= game.viewHeight() && game.goingDown(angle) ||
                    y <= 40 && game.goingUp(angle)) {

                if (game.random(0, 1, 1) == 1) {
                    this.angle = -this.angle;
                }
            }
        }
    }
}
