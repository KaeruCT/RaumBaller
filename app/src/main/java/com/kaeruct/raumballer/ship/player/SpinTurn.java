package com.kaeruct.raumballer.ship.player;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.Cannon;
import com.kaeruct.raumballer.cannon.CombinedCannon;
import com.kaeruct.raumballer.cannon.FireCannon;
import com.kaeruct.raumballer.cannon.NanoCannon;
import com.kaeruct.raumballer.cannon.PlasmaCannon;
import com.kaeruct.raumballer.cannon.SparkCannon;
import com.kaeruct.raumballer.ship.PlayerShip;

public class SpinTurn extends PlayerShip {

    public SpinTurn(int x, int y, AndroidGame game) {
        super(x, y, "player3", 140, game);
        this.velocity = 2.4;
        this.acc = 0.2;
        this.drag = 0.2;
        this.explosionColor = "blue";
        this.width = 16;
        this.cannonPrototypes[0] = new SparkCannon();
        this.cannonPrototypes[1] = new CombinedCannon(new Cannon[]{
                new NanoCannon(), new FireCannon()
        });
        this.cannonPrototypes[2] = new CombinedCannon(new Cannon[]{
                new SparkCannon(), new FireCannon()
        });
        this.cannonPrototypes[3] = new CombinedCannon(new Cannon[]{
                new SparkCannon(), new PlasmaCannon(), new NanoCannon()
        });

        this.cannons.add(cannonPrototypes[0]);
    }
}
