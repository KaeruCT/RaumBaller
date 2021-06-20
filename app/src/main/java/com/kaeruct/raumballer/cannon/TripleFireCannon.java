package com.kaeruct.raumballer.cannon;
import com.kaeruct.raumballer.bullet.FireBullet;

public class TripleFireCannon extends Cannon{
	
	public TripleFireCannon(){
		super();
		waitTime = 12;
	}
	
	public void shoot(double x, double y, double angle, int colid, int t){
		
		if(canShoot(t)){
			new FireBullet(x, y, colid, angle);
			new FireBullet(x, y, colid, angle+0.1);
			new FireBullet(x, y, colid, angle-0.1);
			//game.playAudio("shoot");
		}
	}
}
