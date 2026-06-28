package com.kaeruct.raumballer.ship.enemy;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.ship.Ship;

public final class EnemyShipFactory {
    private EnemyShipFactory() {
    }

    public static Ship create(String name, double x, double y, double velocity, double angle, AndroidGame game) {
        if ("SparkDefender".equals(name)) return new SparkDefender(x, y, velocity, angle, game);
        if ("FireStriker".equals(name)) return new FireStriker(x, y, velocity, angle, game);
        if ("SpaceBall".equals(name)) return new SpaceBall(x, y, velocity, angle, game);
        if ("SparkEye".equals(name)) return new SparkEye(x, y, velocity, angle, game);
        if ("CibumDestroyer".equals(name)) return new CibumDestroyer(x, y, velocity, angle, game);
        if ("BobbaDestroyer".equals(name)) return new BobbaDestroyer(x, y, velocity, angle, game);
        if ("Asterisk".equals(name)) return new Asterisk(x, y, velocity, angle, game);
        throw new IllegalArgumentException("Unknown enemy ship class: " + name);
    }
}
