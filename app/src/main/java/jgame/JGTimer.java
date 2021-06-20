package jgame;
import jgame.impl.JGEngineInterface;

/** A timer that generates a callback after a certain number of frames.  It
 * can conveniently be used as a "one liner class", i.e. as an inner class
 * within a context where it can set a variable or call a method. i.e.
 *<code> new JGTimer (10,true) { alarm() { doSomething(); } };</code>

 * <P>Timers are updated just before the beginning of a frame.  Any objects
 * they add or delete are immediately updated before the frame starts.

* <p>Timers can be made dependent on certain game states or objects.  If the
* particular game state is exited or the object is removed at the beginning of
* the frame, the timer removes itself without invoking the alarm.
*/
public abstract class JGTimer {
	int frames=0;
	double frames_left=0;
	public boolean running=true;
	public boolean one_shot;
	public JGObject parent_obj=null;
	public String parent_state=null;
	JGEngineInterface eng;

	/** Create timer; the timer may be one-shot (it runs only once, then
	 * triggers the alarm and removes itself), or continuous (it continues
	 * running and triggering the alarm)
	* @param frames_to_alarm  0 = callback just before next frame.
	* @param one_shot  true = run only once, false = run repeatedly
	*/
	public JGTimer(int frames_to_alarm, boolean one_shot) {
		set(frames_to_alarm,one_shot);
		eng = JGObject.default_engine;
		eng.registerTimer(this);
	}

	/** Create timer which has an object as parent.
	* @param frames_to_alarm  0 = callback just before next frame.
	* @param one_shot  true = run only once, false = run repeatedly
	*/
	public JGTimer(int frames_to_alarm, boolean one_shot, JGObject parent) {
		set(frames_to_alarm,one_shot);
		eng = JGObject.default_engine;
		eng.registerTimer(this);
		parent_obj = parent;
	}

	/** Create timer which has a specific gamestate as parent.
	* @param frames_to_alarm  0 = callback just before next frame.
	* @param one_shot  true = run only once, false = run repeatedly
	*/
	public JGTimer(int frames_to_alarm, boolean one_shot, String parent) {
		set(frames_to_alarm,one_shot);
		eng = JGObject.default_engine;
		eng.registerTimer(this);
		parent_state = parent;
	}

	public void set(int frames_to_alarm, boolean one_shot) {
		frames=frames_to_alarm;
		frames_left=frames;
		this.one_shot = one_shot;
	}

	/** Tick function, as called by the engine implementation.
	* Returns true when the timer has to be removed. */
	public boolean tick(double speed) {
		// XXX note that we use inGameStateNextFrame here, because this is an
		// indication of whether the timer should exist for the upcoming
		// frame.  However, another timer in the timer set may change the
		// state, so there's actually a race condition here.
		if (parent_state!=null
		&& !eng.inGameStateNextFrame(parent_state)) return true;
		if (parent_obj!=null && !parent_obj.isAlive()) return true;
		if (running) {
			if (frames_left <= 0) {
				frames_left=frames;
				alarm();
				if (one_shot) return true;
			} else {
				frames_left -= speed;
			}
		}
		return false;
	}

	/** define your alarm action here. */
	abstract public void alarm();
}

