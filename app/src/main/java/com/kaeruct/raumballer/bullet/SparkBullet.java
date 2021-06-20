package com.kaeruct.raumballer.bullet;

public class SparkBullet extends Bullet{

	public SparkBullet(double x, double y, int colid, double angle){
		super(x, y, "bul2", colid, angle);
		this.velocity = 6;
		this.damage = 1;
	}
}
