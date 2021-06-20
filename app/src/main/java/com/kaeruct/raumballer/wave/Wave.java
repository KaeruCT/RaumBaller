package com.kaeruct.raumballer.wave;

import java.lang.reflect.Constructor;

import com.kaeruct.raumballer.LevelReader;
import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.ship.*;

public abstract class Wave {
	
	private int freq;
	private int spawned;
	private int maxAmount;
	private LevelReader reader;
	public boolean stopped;
	private String shipClass;
	protected AndroidGame game;
	
	public Wave(AndroidGame game, LevelReader reader, int maxAmount) {
		this.setFreq(20);
		this.game = game;
		this.reader = reader;
		this.setSpawned(0);
		this.stopped = false;
		this.setMaxAmount(maxAmount);
	}
	
	public abstract void spawn();
	
	// spawn straight down
	public Ship spawn(double x, double y, double vel){
		return this.spawn(x, y, vel, Math.PI*3/2);
	}
	
	// spawn specifying a direction
	public Ship spawn(double x, double y, double vel, double angle){
		return this.spawn(x, y, vel, angle, shipClass);
	}
	
	// spawn specifying a direction and a different class
	public Ship spawn(double x, double y, double vel, double angle , String shipClass){
		return this.spawn(x, y, vel, angle, shipClass, null);
	}	
	
	public Ship spawn(double x, double y, double vel, double angle, String shipClass, Ship parent){
		Ship s = null;
		
		try {
			// spawn ship
			Class<Ship> ship = (Class<Ship>) Class.forName("com.kaeruct.raumballer.ship.enemy."+shipClass);
			
			Constructor<Ship> c = ship.getConstructor(double.class, double.class,
					double.class, double.class, AndroidGame.class);
			
			s = c.newInstance(x+game.viewXOfs(), y+game.viewYOfs(), vel, angle, this.game);
			this.setSpawned(this.getSpawned()+1);
			
			if(parent != null){
				s.setParent(parent);
			}

		} catch (Exception e) {
			this.game.dbgPrint(e.toString());
		}
		
		return s;
	
	}
	
	public void update(){
		if(!stopped){
			if(getSpawned() >= getMaxAmount()){
				this.stop();
			}else if(game.t%getFreq() == 0){
				spawn();
			}
		}
		
	}
	
	public void stop(){
		// tell the level reader this wave is done
		if(!this.stopped) this.reader.waveDone();
		this.stopped = true;
	}

	public int getFreq() {
		return freq;
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}

	public int getSpawned() {
		return spawned;
	}

	public void setSpawned(int spawned) {
		this.spawned = spawned;
	}

	public int getMaxAmount() {
		return maxAmount;
	}

	public void setMaxAmount(int maxAmount) {
		this.maxAmount = maxAmount;
	}
	
	public String getShipClass() {
		return shipClass;
	}

	public void setShipClass(String shipClass) {
		this.shipClass = shipClass;
	}
}
