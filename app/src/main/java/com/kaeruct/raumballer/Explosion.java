package com.kaeruct.raumballer;

import jgame.JGObject;

public class Explosion extends JGObject{
	public Explosion(double x, double y, String color){
		super("explosion", true, x, y, 0x99, "explosion_"+color);
		this.expiry = 16;
	}
}
