package com.kaeruct.raumballer.bullet;

public class DiscBullet extends Bullet{

	public DiscBullet(double x, double y, int colid, double angle){
		super(x, y, "bul3", colid, angle);
		this.velocity = 5;
		this.damage = 0.5;
	}
}
