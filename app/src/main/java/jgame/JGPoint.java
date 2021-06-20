package jgame;

/** Minimal replacement of java.awt.Point. */
public class JGPoint {

	public int x=0,y=0;

	public JGPoint () {}

	public JGPoint (JGPoint p) {
		x = p.x;
		y = p.y;
	}

	public JGPoint (int x,int y) {
		this.x=x;
		this.y=y;
	}
	public String toString() {
		return "JGPoint("+x+","+y+")";
	}
}
