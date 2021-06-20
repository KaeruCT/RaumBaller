package jgame;

/** Minimal replacement of java.awt.Rectangle. */
public class JGRectangle {

	public int x=0,y=0,width=0,height=0;

	public JGRectangle () {}

	public JGRectangle (int x,int y,int width,int height) {
		this.x=x;
		this.y=y;
		this.width=width;
		this.height=height;
	}

	public JGRectangle(JGRectangle rec) {
		this.x = rec.x;
		this.y = rec.y;
		this.width = rec.width;
		this.height= rec.height;
	}

	/** Copy contents of source rectangle into this rectangle. */
	public void copyFrom(JGRectangle src) {
		x=src.x;
		y=src.y;
		width=src.width;
		height=src.height;
	}

	public boolean intersects(JGRectangle rec) {
		return    rec.x            < x+width
		       && rec.x+rec.width  > x
		       && rec.y            < y+height
		       && rec.y+rec.height > y;
	}
	public String toString() {
		return "JGRectangle("+x+","+y+","+width+","+height+")";
	}
}
