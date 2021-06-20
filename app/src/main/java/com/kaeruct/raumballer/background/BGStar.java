package com.kaeruct.raumballer.background;

import com.kaeruct.raumballer.AndroidGame;
import jgame.JGObject;

public class BGStar extends JGObject{
	
	private AndroidGame game;
	
	public BGStar(double x, double y, int n, AndroidGame game){
		super("bgstar", true, x, y, 0x99, "bgstar_"+n, 0, n*2);
		this.expiry = -2;
		this.game = game;
	}
	
	public void move(){
		super.move();
		if(y >= this.game.pfHeight()-4){
			y = 0;
		}
	}
}
