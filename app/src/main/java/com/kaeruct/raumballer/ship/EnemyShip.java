package com.kaeruct.raumballer.ship;

import com.kaeruct.raumballer.AndroidGame;


public abstract class EnemyShip extends Ship {

    public EnemyShip(double x, double y, String anim, double maxHealth, AndroidGame game) {
        super(x, y, "enemy", game.ENEMY_ID, anim, maxHealth, game);

        this.velocity = 1;
        this.angle = Math.PI * 3 / 2;
        this.width = 16;
    }
}
