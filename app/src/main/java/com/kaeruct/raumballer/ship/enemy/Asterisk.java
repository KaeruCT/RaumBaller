package com.kaeruct.raumballer.ship.enemy;
import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.ship.EnemyShip;

public class Asterisk extends EnemyShip {

	private double offset;
	private double radius;
	
	public Asterisk(double x, double y, double vel, double angle, AndroidGame game) {
		super(x, y, "asterisk", 5, game);

		this.velocity = vel;
		this.angle = angle;
		this.explosionColor = "blue";
		this.width = 16;
		this.radius = 12; // distance from ship to "orbit"
		this.offset = angle;
		this.sturdy = true;
		this.expiry = -1;
	}
	
	public void move(){
		super.move();
		
		x = parent.x - Math.cos(-offset)*radius;
		y = parent.y + Math.sin(offset)*radius;
		
		offset += 1;
		
		// removing if out of range
		if(x <= -32 || y <= -32 || x-32 >= game.pfWidth() || y-32 >= game.pfHeight())
			remove();
		
		// die if parent died
		if(!parent.isAlive()){
			this.kill();
		}
	}
}
