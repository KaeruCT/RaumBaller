package com.kaeruct.raumballer.bullet;

public class BitBullet extends Bullet{

	public BitBullet(double x, double y, int colid, double angle){
		super(x, y, "bul5", colid, angle);
		this.velocity = 4;
		this.damage = 0.2;
	}
}
