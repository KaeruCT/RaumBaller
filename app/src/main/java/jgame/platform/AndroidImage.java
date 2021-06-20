package jgame.platform;
import jgame.*;
import jgame.impl.JGameError;
//import android.graphics.Bitmap;
import android.graphics.*;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;


/** Image functionality */
class AndroidImage implements JGImage {

	//static Hashtable loadedimages = new Hashtable(); /* filenames => Images */

	Bitmap img=null;

	//BitmapFactory.Options opaquebitmapopts = null;

	// colour which semi transparent pixels in scaled image should render to
	static JGColor bg_col=JGColor.black;

	// true means image is certainly opaque, false means image may be
	// transparent
	boolean is_opaque=false;

	/** Create new image and define any known settings. */
	public AndroidImage (Bitmap img,JGColor bg_color,boolean is_opaque) {
		this.img=img; 
		bg_col=bg_color;
		this.is_opaque=is_opaque;
	}

	/** Create new image */
	public AndroidImage (Bitmap img) { this.img=img; }

	/** Create handle to image functions. */
	public AndroidImage () {
		//opaquebitmapopts = new BitmapFactory.Options();
		//opaquebitmapopts.inPreferredConfig =
		//	getPreferredBitmapFormat(JGEngine.displayformat);
	}

	/* static in spirit*/

	public JGImage loadImage(String imgfile) {
		return loadImage(JGEngine.assets,imgfile);
	}

	/** load image from assets directory
	 * @param assets the assets manager used to load the file
	 */
	private AndroidImage loadImage(AssetManager assets, String filename) {
		Bitmap bitmap = null;
		if (assets!= null) {
			InputStream is;
			try {
				is = assets.open(filename);
				bitmap = BitmapFactory.decodeStream(is);
				is.close();
			} catch (IOException e) {
				throw new Error("Asset '"+filename+"' not found");
			}
		}
		return new AndroidImage(bitmap);
	}

	public void purgeImage(String imgfile) {
		//if (loadedimages.containsKey(imgfile)) loadedimages.remove(imgfile);
	}


	/* object-related methods */

	JGPoint size=null;
	public JGPoint getSize() {
		if (size!=null) return size;
		size=new JGPoint(img.getWidth(),img.getHeight());
		return size;
	}

	public boolean isOpaque(int alpha_thresh) {
		return is_opaque; 
	}

	public JGImage rotate(int angle) {
		JGPoint size = getSize();
		int [] buffer = new int [size.x * size.y];
		img.getPixels(buffer, 0,size.x, 0,0, size.x,size.y);
		int [] rotate = new int [size.x * size.y];
		int angletype = (angle/90) & 3;
		if (angletype==0) return this;
		if (angletype==1) {
			/* 1 2 3 4    9 5 1
			 * 5 6 7 8 => a 6 2
			 * 9 a b c    b 7 3 
			 *            c 8 4 */
			for(int y = 0; y < size.y; y++) {
				for(int x = 0; x < size.x; x++) {
					rotate[x*size.y + (size.y-1-y) ] =
							buffer[(y*size.x)+x];
				}
			}
			return new AndroidImage(
			Bitmap.createBitmap(rotate,size.y,size.x,Bitmap.Config.ARGB_8888)
			);
		}
		if (angletype==3) {
			/* 1 2 3 4    4 8 c
			 * 5 6 7 8 => 3 7 b
			 * 9 a b c    2 6 a 
			 *            1 5 9 */
			for(int y = 0; y < size.y; y++) {
				for(int x = 0; x < size.x; x++) {
					rotate[(size.x-x-1)*size.y + y] =
							buffer[(y*size.x)+x];
				}
			}
			return new AndroidImage(
			Bitmap.createBitmap(rotate,size.y,size.x,Bitmap.Config.ARGB_8888)
			);
		}
		if (angletype==2) {
			/* 1 2 3 4    c b a 9
			 * 5 6 7 8 => 8 7 6 5
			 * 9 a b c    4 3 2 1 */
			for(int y = 0; y < size.y; y++) {
				for(int x = 0; x < size.x; x++) {
					rotate[((size.y-y-1)*size.x)+(size.x-x-1)] =
							buffer[(y*size.x)+x];
				}
			}
		}
		return new AndroidImage(
			Bitmap.createBitmap(rotate,size.x,size.y,Bitmap.Config.ARGB_8888)
		);
	}

	public JGImage flip(boolean horiz,boolean vert) {
		if (!horiz && !vert) return this;
		JGPoint size = getSize();
		int [] buffer = new int [size.x * size.y];
		img.getPixels(buffer, 0,size.x, 0,0, size.x,size.y);
		int [] flipbuf = new int [size.x * size.y];
		if (vert && !horiz) {
			for(int y = 0; y < size.y; y++) {
				for(int x = 0; x < size.x; x++) {
					flipbuf[(size.y-y-1)*size.x + x] =
							buffer[(y*size.x)+x];
				}
			}
		} else if (horiz && !vert) {
			for(int y = 0; y < size.y; y++) {
				for(int x = 0; x < size.x; x++) {
					flipbuf[y*size.x + (size.x-x-1)] =
							buffer[(y*size.x)+x];
				}
			}
		} else if (horiz && vert) {
			for(int y = 0; y < size.y; y++) {
				for(int x = 0; x < size.x; x++) {
					flipbuf[(size.y-y-1)*size.x + (size.x-x-1)] =
							buffer[(y*size.x)+x];
				}
			}
		}
		return new AndroidImage(
			Bitmap.createBitmap(flipbuf,size.x,size.y,Bitmap.Config.ARGB_8888)
		);
	}

	public JGImage scale(int width, int height) {
		return new
			AndroidImage(Bitmap.createScaledBitmap(img,width,height,true));
//		int [] pix = new int [img.getWidth()*img.getHeight()];
//		int srcwidth = img.getWidth();
//		int srcheight = img.getHeight();
//		img.getRGB(pix,0,srcwidth,0,0,srcwidth,srcheight);
//		int [] dstpix = new int[width*height];
//		int dstidx=0;
//		int bg_pix=(((bg_col.r<<16) + (bg_col.g<<8) + bg_col.b)>>2)&0x3f3f3f3f;
//		double srcxinc = img.getWidth()/(double)width;
//		double srcyinc = img.getHeight()/(double)height;
//		double srcx,srcy=srcyinc*0.25;
//		double srcxround=srcxinc*0.5,srcyround=srcyinc*0.5;
//		for (int y=0; y<height; y++) {
//			srcx=srcxinc*0.25;
//			for (int x=0; x<width; x++) {
//				int pix1 = (pix[(int)srcx             + srcwidth*(int)srcy]
//						    >> 2) & 0x3f3f3f3f;
//				int pix2 = (pix[(int)(srcx+srcxround) + srcwidth*(int)srcy]
//						    >> 2) & 0x3f3f3f3f;
//				int pix3 = (pix[(int)srcx     + srcwidth*(int)(srcy+srcyround)]
//						    >> 2) & 0x3f3f3f3f;
//				int pix4 = (pix[(int)(srcx+srcxround) +
//											    srcwidth*(int)(srcy+srcyround)]
//						    >> 2) & 0x3f3f3f3f;
//				// you might think that transparent pixels remember the
//				// colour you assigned to them, but on some phones
//				// (Sony Ericsson) they don't, and
//				// transparent pixels are always white.  So we assign the bg
//				// color to them here.
//				if (pix1 <= 0xffffff) pix1 = bg_pix;
//				if (pix2 <= 0xffffff) pix2 = bg_pix;
//				if (pix3 <= 0xffffff) pix3 = bg_pix;
//				if (pix4 <= 0xffffff) pix4 = bg_pix;
//				int dp = pix1+pix2+pix3+pix4;
//				//if ((dp&0xff000000) != 0) dp |= 0xff000000;
//				if (((dp>>24)&0xff) > 0x60) dp |= 0xff000000; else dp = 0;
//				dstpix[dstidx++] = dp;
//				srcx += srcxinc;
//			}
//			srcy += srcyinc;
//		}
//		// clean up temp data before creating image to avoid peak memory use
//		pix = null;
//		return new AndroidImage(
//			Bitmap.createRGBImage(dstpix,width,height,!is_opaque),
//			bg_col,is_opaque);
	}

	public JGImage rotateAny(double angle) {
		return new AndroidImage(img);
//		int sw = img.getWidth();
//		int sh = img.getHeight();
//		int bg_pix=(((bg_col.r<<16) + (bg_col.g<<8) + bg_col.b)>>2)&0x3f3f3f3f;
//		// destination size is upper bound size. Upper bound is the max
//		// of the longest dimension and the figure's dimension at 45 degrees
//		// = sw*sin(45)+sh*cos(45) ~= 1.5*(sw+sh)
//		int dw = (int)Math.max( Math.max(sw,sh), 0.75*(sw+sh));
//		int dh = dw;
//	
//		int[] srcData = new int[sw * sh];
//		img.getRGB(srcData, 0, sw, 0, 0, sw, sh);
//		int[] dstData = new int[dw * dh];
//
//		float sa = (float) Math.sin(angle);
//		float ca = (float) Math.cos(angle);
//		int isa = (int) (256 * sa);
//		int ica = (int) (256 * ca);
//
//		int my = - (dh >> 1);
//		for(int i = 0; i < dh; i++) {
//			int wpos = i * dw;
//
//			int xacc = my * isa - (dw >> 1) * ica + ((sw >> 1) << 8);
//			int yacc = my * ica + (dw >> 1) * isa + ((sh >> 1) << 8);
//
//			for(int j = 0; j < dw; j++) {
//				do {
//					int srcx1 = (xacc >> 8);
//					int srcy1 = (yacc >> 8);
//					int srcx2 = ((xacc+0x80) >> 8);
//					int srcy2 = ((yacc+0x80) >> 8);
//
//					if (srcx1 < 0 || srcx1 >= sw) {
//						if (srcx2 < 0 || srcx2 >= sw) break;
//						srcx1 = srcx2;
//					}
//					if (srcy1 < 0 || srcy1 >= sh) {
//						if (srcy2 < 0 || srcy2 >= sh) break;
//						srcy1 = srcy2;
//					}
//					if (srcx2 < 0 || srcx2 >= sw) {
//						if (srcx1 < 0 || srcx1 >= sw) break;
//						srcx2 = srcx1;
//					}
//					if (srcy2 < 0 || srcy2 >= sh) {
//						if (srcy1 < 0 || srcy1 >= sh) break;
//						srcy2 = srcy1;
//					}
//					int pix1 = (srcData[srcx1 + srcy1 * sw]>>2)&0x3f3f3f3f;
//					int pix2 = (srcData[srcx2 + srcy1 * sw]>>2)&0x3f3f3f3f;
//					int pix3 = (srcData[srcx1 + srcy2 * sw]>>2)&0x3f3f3f3f;
//					int pix4 = (srcData[srcx2 + srcy2 * sw]>>2)&0x3f3f3f3f;
//					// see also scale()
//					/*int pixnr=0;
//					int dp=0;
//					if (pix1 > 0xffffff) {
//						dp += pix1;
//						pixnr++;
//					}
//					if (pix2 > 0xffffff) {
//						dp += pix2;
//						pixnr++;
//					}
//					if (pix3 > 0xffffff) {
//						dp += pix3;
//						pixnr++;
//					}
//					if (pix4 > 0xffffff) {
//						dp += pix4;
//						pixnr++;
//					}
//					if (pixnr<4) break;
//					if ((dp&0xff000000) != 0) dp |= 0xff000000;
//					dstData[wpos] = (dp/pixnr)<<2;*/
//					if (pix1 <= 0xffffff) pix1 = bg_pix;
//					if (pix2 <= 0xffffff) pix2 = bg_pix;
//					if (pix3 <= 0xffffff) pix3 = bg_pix;
//					if (pix4 <= 0xffffff) pix4 = bg_pix;
//					int dp = pix1+pix2+pix3+pix4;
//					if (((dp>>24)&0xff) > 0x60) dp |= 0xff000000; else dp = 0;
//					dstData[wpos] = dp;
//				} while (false);
//				wpos++;
//				xacc += ica;
//				yacc -= isa;
//			}
//			my++;
//		}
//		// clean up temp data before creating image to avoid peak memory use
//		srcData = null;
//		return new AndroidImage(Bitmap.createRGBImage(dstData, dw, dh, !is_opaque));
	}

	public JGImage crop(int x,int y, int width,int height) {
		return new AndroidImage(Bitmap.createBitmap(img, x, y, width, height,
			null, true) );
//		return new AndroidImage(Bitmap.createImage(img,
//			x,y, width,height, Sprite.TRANS_NONE),bg_col,is_opaque);
	}

	public JGImage toDisplayCompatible(int thresh,JGColor bg_col,
	boolean fast,boolean bitmask) {
		return new AndroidImage(img);
//		this.bg_col=bg_col;
//		int [] pix = new int [img.getWidth()*img.getHeight()];
//		int srcwidth = img.getWidth();
//		int srcheight = img.getHeight();
//		img.getRGB(pix,0,srcwidth,0,0,srcwidth,srcheight);
//		int srcidx=0;
//		int bg_pix = (bg_col.r<<16) + (bg_col.g<<8) + bg_col.b;
//		boolean is_transparent=false;
//		for (int i=0; i<srcwidth*srcheight; i++) {
//			int alpha = (pix[srcidx]>>24)&0xff;
//			if (alpha > thresh) {
//				pix[srcidx++] |= 0xff000000;
//			} else {
//				pix[srcidx++] = bg_pix;
//				is_transparent=true;
//			}
//		}
//		return new AndroidImage(Bitmap.createRGBImage(pix,srcwidth,srcheight,
//				is_transparent),bg_col,!is_transparent);
	}
	// not useful: cannot retrieve display format
	public static Bitmap.Config getPreferredBitmapFormat(int displayformat) {
		if (displayformat == PixelFormat.RGB_565)
			return Bitmap.Config.RGB_565;
		else
			return Bitmap.Config.ARGB_8888;
	}

}
