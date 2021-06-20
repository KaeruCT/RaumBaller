package com.kaeruct.raumballer.cannon;
import com.kaeruct.raumballer.ship.player.SpinTurn;

public class ShipCannon extends Cannon{
	
	public ShipCannon(){
		super();
		waitTime = 20;
	}
	
	public void shoot(double x, double y, double angle, int colid, int t){
		
		if(canShoot(t)){
			SpinTurn a = new SpinTurn((int)x, (int)y, game);
			a.setHealth(0);
			a.cannons.set(0, new CombinedCannon(new Cannon[]{}));
			a.cannonPrototypes[0] = new CombinedCannon(new Cannon[]{});
			a.cannonPrototypes[1] = new CombinedCannon(new Cannon[]{});
			a.cannonPrototypes[2] = new CombinedCannon(new Cannon[]{});
			//game.playAudio("shoot");
		}
	}
}
