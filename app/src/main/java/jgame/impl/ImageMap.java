package jgame.impl;
import jgame.*;
public class ImageMap {
	JGImage imageutil;
	public JGImage img;
	//public Image scaled_img=null;
	public int xofs,yofs;
	public int tilex,tiley;
	public int skipx,skipy;
	public ImageMap (JGImage imageutil,
	String imgfile, int xofs,int yofs,
	int tilex,int tiley, int skipx,int skipy) {
		this.imageutil=imageutil;
		img = imageutil.loadImage(imgfile);
		this.xofs=xofs;
		this.yofs=yofs;
		this.tilex=tilex;
		this.tiley=tiley;
		this.skipx=skipx;
		this.skipy=skipy;
	}
	//public int getHeight() { return img.getHeight(this); }
	/** returns null when image was not loaded */
	public JGPoint getImageCoord(int imgnr) {
		if (img==null) return null;
		JGPoint size = img.getSize();
		int imgs_per_line = (size.x - xofs + skipx) / (tilex+skipx);
		int ynr = imgnr / imgs_per_line;
		int xnr = imgnr % imgs_per_line;
		return new JGPoint(
			xofs + xnr*(tilex+skipx),
			yofs + ynr*(tiley+skipy) );
	}
}

