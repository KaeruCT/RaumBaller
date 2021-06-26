package com.kaeruct.raumballer.ship.enemy;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.SoloPlasmaCannon;
import com.kaeruct.raumballer.cannon.TripleFireCannon;
import com.kaeruct.raumballer.ship.EnemyShip;

/**
 * Big menacing red ship with lots of health
 */
public class CibumDestroyer extends EnemyShip {

    public CibumDestroyer(double x, double y, double vel, double angle, AndroidGame game) {
        super(x, y, "cibumdestroyer", 50, game);

        this.velocity = vel;
        this.angle = angle;
        this.cannons.add(new TripleFireCannon());
        this.cannons.add(new SoloPlasmaCannon());
        this.explosionColor = "red";
        this.setBBox(4, 4, 24, 24);
        this.width = 32;
    }

    public void move() {
        super.move();

        if (clock % 60 == 0) {
            double ang = game.atan2(y - game.getPlayer().y, game.getPlayer().x - x);
            shoot(ang);
        }
        // only attempt to stay in screen if not near boundaries
        if (x >= 8 && x + 8 <= game.viewWidth()) {
            if (y + 32 >= game.viewHeight() && game.goingDown(angle) ||
                    y <= 32 && game.goingUp(angle)) {

                if (game.random(0, 1, 1) == 1) {
                    this.angle = -this.angle;
                }
            }
        }
    }
}
