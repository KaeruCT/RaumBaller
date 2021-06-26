package com.kaeruct.raumballer.ship.player;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.Cannon;
import com.kaeruct.raumballer.cannon.CombinedCannon;
import com.kaeruct.raumballer.cannon.DiscCannon;
import com.kaeruct.raumballer.cannon.FireCannon;
import com.kaeruct.raumballer.cannon.NanoCannon;
import com.kaeruct.raumballer.cannon.TripleFireCannon;
import com.kaeruct.raumballer.ship.PlayerShip;

public class NimakRunner extends PlayerShip {

    public NimakRunner(int x, int y, AndroidGame game) {
        super(x, y, "player2", 80, game);
        this.velocity = 1.8;
        this.drag = 0.3;
        this.acc = 0.1;
        this.explosionColor = "red";
        this.width = 16;
        this.cannonPrototypes[0] = new DiscCannon();
        this.cannonPrototypes[1] = new CombinedCannon(new Cannon[]{
            new DiscCannon(), new FireCannon()
        });
        this.cannonPrototypes[2] = new TripleFireCannon();
        this.cannonPrototypes[3] = new CombinedCannon(new Cannon[]{
                new TripleFireCannon(), new NanoCannon(), new FireCannon()
        });

        this.cannons.add(cannonPrototypes[0]);
    }
}
