package com.kaeruct.raumballer.bullet;

public class PlasmaBullet extends Bullet{

	public PlasmaBullet(double x, double y, int colid, double angle){
		super(x, y, "bul1", colid, angle);
		this.velocity = 3;
		this.damage = 3;
	}
}
