package com.kaeruct.raumballer.cannon;
import com.kaeruct.raumballer.bullet.FireBullet;

public class FireCannon extends Cannon{
	
	public FireCannon(){
		super();
		waitTime = 12;
	}
	
	public void shoot(double x, double y, double angle, int colid, int t){
		
		if(canShoot(t)){
			new FireBullet(x, y, colid, angle);
			//game.playAudio("shoot");
		}
	}
}
