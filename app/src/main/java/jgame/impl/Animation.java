package jgame.impl;

public class Animation {
	/* settings;  the public ones may be manipulated freely */
	String [] frames;
	public double speed;
	public int increment=1;
	public boolean pingpong=false;
	/* state */
	int framenr=0;
	double phase=0.0;
	boolean running=true;
	public Animation (String [] frames, double speed) {
		this.frames=frames;
		this.speed=speed;
	}
	public Animation (String [] frames, double speed, boolean pingpong) {
		this.frames=frames;
		this.speed=speed;
		this.pingpong=pingpong;
	}
	public Animation (String [] frames, double speed, boolean pingpong,
	int increment) {
		this.frames=frames;
		this.speed=speed;
		this.pingpong=pingpong;
		this.increment=increment;
	}
	public void stop() { running=false; }
	public void start() { running=true; }
	public void reset() {
		framenr=0;
		phase=0.0;
	}
	//public Object clone() {
	//	try {
	//		return super.clone();
	//	} catch (CloneNotSupportedException e) { return null; }
	//}
	public Animation copy() {
		Animation cp = new Animation(frames,speed,pingpong);
		cp.framenr = framenr;
		cp.phase = phase;
		cp.running = running;
		return cp;
		//return (Animation) clone();
	}
	public String getCurrentFrame() {
		if (framenr < 0 || framenr >= frames.length) {
			return frames[0];
		} else {
			return frames[framenr];
		}
	}
	/** Does one animation step and returns current image.  Note that the
	 * function returns the frame before the state is being updated. */
	public String animate(double speedmul) {
		String ret = getCurrentFrame();
		if (running) {
			phase += speed*speedmul;
			while (phase >= 1.0) {
				phase -= 1.0;
				framenr += increment;
				if (!pingpong) {
					if (framenr >= frames.length) framenr -= frames.length;
					if (framenr < 0)              framenr += frames.length;
				} else {
					if (framenr >= frames.length) {
						framenr -= 2*increment;
						increment = -increment;
					}
					if (framenr < 0) {
						framenr -= 2*increment;
						increment = -increment;
					}
				}
			}
		}
		return ret;
	}
}


