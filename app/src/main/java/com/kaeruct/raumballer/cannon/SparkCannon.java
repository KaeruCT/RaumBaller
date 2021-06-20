package com.kaeruct.raumballer.cannon;
import com.kaeruct.raumballer.bullet.SparkBullet;

public class SparkCannon extends Cannon{
	
	public SparkCannon(){
		super();
		waitTime = 4;
	}
	
	public void shoot(double x, double y, double angle, int colid, int t){
		if(canShoot(t)){
			new SparkBullet(x, y, colid, angle);
			
		}
	}
}
