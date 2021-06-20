package com.kaeruct.raumballer.cannon;
import com.kaeruct.raumballer.bullet.BitBullet;

public class NanoCannon extends Cannon{
	
	public NanoCannon(){
		super();
		waitTime = 3;
	}
	
	public void shoot(double x, double y, double angle, int colid, int t){
		
		if(canShoot(t)){
			new BitBullet(x, y, colid, angle);
			
			//game.playAudio("shoot");
		}
	}
}
