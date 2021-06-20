package com.kaeruct.raumballer.ship.player;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.*;
import com.kaeruct.raumballer.ship.PlayerShip;

public class NimakRunner extends PlayerShip {
	
	public NimakRunner(int x, int y, AndroidGame game) {
		super(x, y, "player2", 80, game);
		this.velocity = 2.2;
		this.acc = 0.1;
		this.explosionColor = "red";
		this.width = 16;
		this.cannonPrototypes[0] = new DiscCannon();
		this.cannonPrototypes[1] = new FireCannon();
		this.cannonPrototypes[2] = new CombinedCannon(new Cannon[]{
				new TripleFireCannon(), new NanoCannon(), new FireCannon()
		});
		
		this.cannons.add(cannonPrototypes[0]);
	}
}
