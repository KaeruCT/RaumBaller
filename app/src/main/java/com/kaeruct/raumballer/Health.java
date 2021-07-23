package com.kaeruct.raumballer;

import com.kaeruct.raumballer.ship.PlayerShip;

import jgame.JGObject;

public class Health extends JGObject {
    private final static int HEALTH_AMOUNT = 20;
    private AndroidGame game;
    public Health(double x, double y, AndroidGame game) {
        super("health", true, x - 4, y - 4, AndroidGame.HEALTH_ID, "health");
        this.expiry = 360;
        this.game = game;
    }
    public void hit(JGObject obj) {
        if (obj instanceof PlayerShip) {
            ((PlayerShip) obj).addHealth(HEALTH_AMOUNT);
            game.playAudio("health", "health", false);
            this.remove();
        }
    }
}
