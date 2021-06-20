package com.kaeruct.raumballer.ship.player;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.*;
import com.kaeruct.raumballer.ship.PlayerShip;

public class SpinTurn extends PlayerShip {
	
	public SpinTurn(int x, int y, AndroidGame game) {
		super(x, y, "player3", 100, game);
		this.velocity = 2.6;
		this.acc = 0.2;
		this.explosionColor = "blue";
		this.width = 16;
		this.cannonPrototypes[0] = new SparkCannon();
		this.cannonPrototypes[1] = new FireCannon();
		this.cannonPrototypes[2] = new CombinedCannon(new Cannon[]{
				new SparkCannon(), new PlasmaCannon(), new NanoCannon()
		});
		
		this.cannons.add(cannonPrototypes[0]);
	}
}
