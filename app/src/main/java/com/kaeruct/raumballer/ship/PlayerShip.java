package com.kaeruct.raumballer.ship;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.cannon.*;

import jgame.platform.JGEngine;

public abstract class PlayerShip extends Ship {

	public Cannon[] cannonPrototypes;
	protected double acc;
	
	public PlayerShip(int x, int y, String anim, int health, AndroidGame game) {
		super(x, y, "player", game.PLAYER_ID, anim, health, game);
		this.velocity = 2;
		this.acc = 0.1;
		this.angle = Math.PI/2;
		this.explosionColor = "blue";
		this.width = 16;
		this.cannonPrototypes = new Cannon[3];
	}
	
	public void move(){
		super.update();

		String type = "1";
		
		if(type.equals("1")){
			this.cannons.clear();
			this.cannons.add(this.cannonPrototypes[0]);
		}else if(type.equals("2")){
			this.cannons.clear();
			this.cannons.add(this.cannonPrototypes[1]);
		}else if(type.equals("3")){
			this.cannons.clear();
			this.cannons.add(this.cannonPrototypes[2]);
		}
		// shooting
		boolean shooting = true;
		if(shooting) {
			
			shoot();
			
			// bounce
			// bounce is annoying, never uncomment
			//if(yspeed < velocity)
			//	this.yspeed += acc*0.1;
			
			if (clock%5==0) {
				game.playAudio("laser");
			}
		}

		double xacc = 0;
		double yacc = 0;
		if (game.isTapping) {
			double angle = Math.atan2(getLastY(), getLastX()) - Math.atan2(game.getMouseY(), game.getMouseX());
			xacc = Math.cos(angle) * 0.05;
			yacc = Math.sin(angle) * 0.05;
			System.out.println(angle);
		}

		this.xspeed += xacc;
		this.yspeed += yacc;

		if (yspeed != 0) {
			yspeed += 0.1*(yspeed < 0 ? acc : -acc);
		}
		if (xspeed != 0) {
			xspeed += 0.1*(xspeed < 0 ? acc : -acc);
		}
		
		// correcting bounds (wrap horizontally)
		if(x <= -8)
			x = game.pfWidth()-9;
		if(y <= -8)
			y = -8;
		if(x+8 >= game.pfWidth())
			x = -8;
		if(y+8 >= game.pfHeight())
			y = game.pfHeight()-8;
	}
}
