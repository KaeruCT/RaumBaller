package com.kaeruct.raumballer.ship.player;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.BitWaveCannon;
import com.kaeruct.raumballer.cannon.Cannon;
import com.kaeruct.raumballer.cannon.CombinedCannon;
import com.kaeruct.raumballer.cannon.DiscCannon;
import com.kaeruct.raumballer.cannon.NanoCannon;
import com.kaeruct.raumballer.cannon.SparkCannon;
import com.kaeruct.raumballer.ship.PlayerShip;

public class StenoShot extends PlayerShip {

    public StenoShot(int x, int y, AndroidGame game) {
        super(x, y, "player1", 70, game);
        this.velocity = 3.0;
        this.acc = 0.2;
        this.drag = 0.1;
        this.explosionColor = "green";
        this.width = 16;
        this.cannonPrototypes[0] = new DiscCannon();
        this.cannonPrototypes[1] = new SparkCannon();
        this.cannonPrototypes[2] = new CombinedCannon(new Cannon[]{
                new SparkCannon(), new DiscCannon()
        });
        this.cannonPrototypes[3] = new CombinedCannon(new Cannon[]{
                new NanoCannon(), new BitWaveCannon(), new DiscCannon()
        });

        this.cannons.add(cannonPrototypes[0]);
    }
}
