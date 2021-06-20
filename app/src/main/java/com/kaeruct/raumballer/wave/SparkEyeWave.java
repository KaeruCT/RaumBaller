package com.kaeruct.raumballer.wave;

import com.kaeruct.raumballer.LevelReader;
import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.ship.Ship;

public class SparkEyeWave extends Wave {
	
	public SparkEyeWave(AndroidGame game, LevelReader r, int maxAmount) {
		super(game, r, maxAmount*8); // it spawns 8 enemies per spawn
		this.setFreq(200); 
		this.setShipClass("SparkEye");
	}
	
	public void spawn(){
		double ypos = game.getPlayer().y;
		double xpos, dir;
	
		if(game.random(0, 1, 1)==1){
			xpos = -8;
			dir = 0;
		}else{
			xpos = game.viewWidth()+8;
			dir = Math.PI;
		}
		
		Ship parent = super.spawn(
			xpos,
			ypos,
			game.random(1, 1.5),
			dir+game.random(-0.2, 0.2)
		);
		
		double num = 7; // number of asterisks around the eye
		
		for(int i = 0; i < 7; i++){
			double ang = Math.PI*(2*(num/2-i)/num);
			
			super.spawn(
				xpos,
				ypos,
				6,
				ang,
				"Asterisk",
				parent
			);
		}
	}

}