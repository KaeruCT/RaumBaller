package com.kaeruct.raumballer.ship;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.Cannon;

public abstract class PlayerShip extends Ship {

    public Cannon[] cannonPrototypes;
    protected double acc;

    public PlayerShip(int x, int y, String anim, int health, AndroidGame game) {
        super(x, y, "player", game.PLAYER_ID, anim, health, game);
        this.velocity = 2;
        this.acc = 0.1;
        this.angle = Math.PI / 2;
        this.explosionColor = "blue";
        this.width = 16;
        this.cannonPrototypes = new Cannon[4];
    }

    public void move() {
        super.update();

        // shooting
        boolean shooting = true;
        if (shooting) {

            shoot();

            // bounce
            // bounce is annoying, never uncomment
            //if(yspeed < velocity)
            //	this.yspeed += acc*0.1;

            if (clock % 10 == 0) {
                game.playAudio("laser");
            }
        }

        double xacc = 0;
        double yacc = 0;
        if (game.isTapping) {
            double rad = Math.atan2(eng.getMouseY() - getLastY(), eng.getMouseX() - getLastX());
            xacc = Math.cos(rad) * 0.1;
            yacc = Math.sin(rad) * 0.1;
        }

        this.xspeed += xacc;
        this.yspeed += yacc;

        if (yspeed != 0) {
            yspeed += 0.3 * (yspeed < 0 ? acc : -acc);
        }
        if (xspeed != 0) {
            xspeed += 0.3 * (xspeed < 0 ? acc : -acc);
        }

        // correcting bounds (wrap horizontally)
        if (x <= -8)
            x = game.pfWidth() - 9;
        if (y <= -8)
            y = -8;
        if (x + 8 >= game.pfWidth())
            x = -8;
        if (y + 8 >= game.pfHeight())
            y = game.pfHeight() - 8;
    }

    @Override
    public void onScore(int score) {
        if (score >= 50000) {
            this.cannons.clear();
            this.cannons.add(this.cannonPrototypes[3]);
            return;
        }
        if (score >= 25000) {
            this.cannons.clear();
            this.cannons.add(this.cannonPrototypes[2]);
            return;
        }
        if (score >= 10000) {
            this.cannons.clear();
            this.cannons.add(this.cannonPrototypes[1]);
            return;
        }
    }
}
