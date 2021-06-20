package jgame;

/** Minimal replacement of java.awt.Color. */
public class JGColor {

	public static final JGColor black = new JGColor(0,0,0);
	public static final JGColor white = new JGColor(255,255,255);
	public static final JGColor yellow= new JGColor(255,255,0);
	public static final JGColor green = new JGColor(0,255,0);
	public static final JGColor cyan  = new JGColor(0,255,255);
	public static final JGColor blue  = new JGColor(0,0,255);
	public static final JGColor magenta= new JGColor(255,0,255);
	public static final JGColor red   = new JGColor(255,0,0);

	public static final JGColor pink  = new JGColor(255,140,140);
	public static final JGColor orange= new JGColor(255,140,0);

	public static final JGColor grey= new JGColor(128,128,128);
	public static final JGColor gray= new JGColor(128,128,128);

	/** a value between 0 and 255 */
	public int r,g,b;
	/** a value between 0 and 255, default is 255 (opaque) */
	public int alpha=255;

	public Object impl;

	public JGColor (int r,int g,int b) {
		this.r=r;
		this.g=g;
		this.b=b;
	}

	public JGColor (int r,int g,int b,int alpha) {
		this.r=r;
		this.g=g;
		this.b=b;
		this.alpha=alpha;
	}

	public JGColor (double r,double g,double b) {
		this.r = (int)(r*255.95);
		this.g = (int)(g*255.95);
		this.b = (int)(b*255.95);
	}

	public JGColor (double r,double g,double b,double alpha) {
		this.r = (int)(r*255.95);
		this.g = (int)(g*255.95);
		this.b = (int)(b*255.95);
		this.alpha = (int)(alpha*255.95);
	}

}
