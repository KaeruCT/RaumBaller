package jgame.impl;
import jgame.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Random;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/** Contains the platform-independent game logic. */
public class EngineLogic {

	public JGImage imageutil;

	Random random;

	/** Platform implementation decides if window is resizeable.  Resizeable
	* requires the implementation to keep the original unscaled images in
	* memory.  Turn it off if you are short on memory. */
	public boolean is_resizeable=true;

	/** make_bitmask indicates what to do with transparent images.
	* true = make bitmask false=make translucent */
	boolean make_bitmask;

	/** prescale indicates if: (1) images should be prescaled to screen
	* resolution or kept at their original size; (2) tiles should be drawn
	* prescaled. It affects:
	* scaled_tile*: scaled tile size, or original size
	* width/height: rounded to whole tile sizes or actual window size
	* *ofs_scaled: either scaled, or same as *ofs.
	* Indirectly affected: *_scale_fac canvas_*ofs */
	boolean prescale;

	public EngineLogic (JGImage imageutil,
	boolean make_bitmask,boolean prescale) {
		this.imageutil=imageutil;
		this.make_bitmask=make_bitmask;
		this.prescale=prescale;
		random = new Random();
		bg_images.addElement(null);
	}


	/** indicates if setCanvasSettings was called. */
	public boolean view_initialised=false;

	public JGColor fg_color = JGColor.white;
	public JGColor bg_color = JGColor.black;
	public JGFont msg_font=null;

	public int outline_thickness=0;
	public JGColor outline_colour=JGColor.black;


	public double fps = 35;
	public double maxframeskip = 4.0; /* max # of frames to skip  */

	public double gamespeed=1.00000000001;

	/* game state. These vectors are always reused and not reconstructed. */

	/** Engine game state */
	public Vector gamestate=new Vector(10,20);
	/** New engine game state to be assigned at the next frame */
	public Vector gamestate_nextframe=new Vector(10,20);
	/** New game states which the game has to transition to, and for which
	 * start[state] have to be called. */
	public Vector gamestate_new=new Vector(10,20);
	/** indicates when engine is inside a parallel object update (moveObjects,
	 * check*Collision) */


	boolean in_parallel_upd=false;


	private Vector timers = new Vector(20,40);


	/** signals that JGame globals are set, and exit code should null globals in
	* JGObject */
	public boolean is_inited=false;
	/** signals that thread should die and canvas should paint exit message */
	public boolean is_exited=false;
	public String exit_message="JGEngine exited successfully";


	Hashtable animations = new Hashtable();


	/* images */

	/** Strings -&gt; JGImages, original size,
	* nonexistence means there is no image */
	public Hashtable images_orig = new Hashtable();
	/** JGPoint sizes of original images */
	public Hashtable image_orig_size = new Hashtable();
	/** Strings -&gt; JGImages, screen size, nonexistence indicates image
	* is not cached and needs to be generated from images_orig */
	public Hashtable images = new Hashtable();

	/** indicates that image is defined even if it has no Image */
	public Hashtable images_exists= new Hashtable(); 
	public Hashtable images_transp = new Hashtable(); 
	/** Hashtable: name to filename. Indicates that image with given name
	* is loaded from given filename */
	public Hashtable images_loaded= new Hashtable(); 
		/* Integers -> Objects, existence indicates transparency */
	public Hashtable images_tile = new Hashtable(); /* Integers -> Strings */
	public Hashtable images_bbox = new Hashtable(); /* Strings -> Rectangles */
	public Hashtable images_tilecid = new Hashtable(); /* Integers -> Integers */

	public Hashtable imagemaps = new Hashtable(); /* Strings->ImageMaps*/

	public int alpha_thresh=128;
	public JGColor render_bg_color=null; // null means use bg_color


	/* objects */

	/** Note: objects lock is used to synchronise object updating between
	 * repaint thread and game thread.  The synchronize functions are found in
	 * Engine.doFrameAll and Canvas.paint */
	public SortedArray objects=new SortedArray(80);    /* String->JGObject */
	SortedArray obj_to_remove = new SortedArray(40); /* String */
	Vector obj_spec_to_remove = new Vector(20,40); /* (String,Int) */
	SortedArray obj_to_add = new SortedArray(40); /* JGObject */


	/* shared playfield dimensions */

	/** Total number of tiles on the playfield. Initially is the same as the
	* nr of tiles on the visible window (viewnrtilesx/y), but can be
	* changed to define a playfield larger than the visible window. */
	public int nrtilesx, nrtilesy;
	/** Size of one tile */
	public int tilex,tiley;
	/** Number of tiles in view (visible window). */
	public int viewnrtilesx, viewnrtilesy;

	/* scaling preferences */

	public double min_aspect=3.0/4.0, max_aspect=4.0/3.0;
	public int crop_top=0,crop_left=0,crop_bottom=0,crop_right=0;
	public boolean smooth_magnify=true;

	/* playfield dimensions from canvas*/

	/** Actual scaled canvas size; that is, the size of the playfield view,
	 * which may be smaller than the desired size of the game window
	 * to accommodate for integer-sized scaled tiles. */
	public int width,height;
	/** Derived info, used for modulo calculation */
	public int pfwidth_half,pfheight_half;
	/** Derived info (playfield size in logical pixels) */
	public int pfwidth,pfheight;

	/** offset of playfield wrt canvas (may be negative if we crop the
	 * playfield). */
	public int canvas_xofs=0,canvas_yofs=0;

	/** Size of one tile, scaled */
	public int scaledtilex,scaledtiley;

	/** Pending pixel offset of visible view on playfield, to be handled at the
	 * next frame draw. */
	public int pendingxofs=0,pendingyofs=0;
	/** Pixel offset of visible view on playfield. */
	public int xofs=0,yofs=0;
	/** Tile offset of top left tile on background image;
	* is derived from x/yofs */
	public int tilexofs=-1,tileyofs=-1;
	/** Derived offset information, useful for scaling. */
	public int xofs_scaled=0,yofs_scaled=0;
	/** Derived offset information, useful for modulo. */
	public int xofs_mid, yofs_mid;

	/** min_scale_fac is min (scalex,scaley). These are 1.0 until width,
	* height are defined */
	public double x_scale_fac=1.0, y_scale_fac=1.0, min_scale_fac=1.0;


	/* playfield dimensions from engine */

	/** Desired width/height of game window; 0 is not initialised yet. Note
	 * that the width/height of the canvas may be a little smaller to
	 * accommodate integer-sized scaled tiles. */
	public int winwidth=0,winheight=0;


	/* background */

	public int [] [] tilemap=null;
	public int [] [] tilecidmap=null;
	//public boolean [] [] tilechangedmap=null;

	/** Wrap-around playfield */
	public boolean pf_wrapx=false,pf_wrapy=false;
	public int pf_wrapshiftx=0,pf_wrapshifty=0;

	public class BGImage {
		/** Image name (not tile name) of image to use behind transparent
		tiles. */
		public String imgname;
		public boolean wrapx,wrapy;
		public JGPoint tiles;
		public double xofs=0,yofs=0;
		public BGImage(String imgname, boolean wrapx, boolean wrapy) {
			this.imgname = imgname;
			this.wrapx = wrapx;
			this.wrapy = wrapy;
			tiles = new JGPoint( (JGPoint)image_orig_size.get(imgname) );
			tiles.x /= tilex;
			tiles.y /= tiley;
		}
	}

	/** BGImages: images to use behind transparent tiles.  Element 0 is always
	 * defined.  Null indicates empty image. */
	public Vector bg_images = new Vector(8,20);

	//public String bg_image=null;
	//public JGPoint bg_image_tiles=null;

	String out_of_bounds_tile="";
	int out_of_bounds_cid=0;
	int preserve_cids=0;

	public int offscreen_margin_x=16,offscreen_margin_y=16;


	/** the defined state of the physical cells of background, i.e. (0,0) is
	 * the top left of background. */
	public boolean [] [] bg_defined=null;


	public int viewWidth() { return viewnrtilesx*tilex; }

	public int viewHeight() { return viewnrtilesy*tiley; }

	public int tileWidth()  { return tilex; }

	public int tileHeight() { return tiley; }


	/** Replacement for stringTokenizer.   str will be split into a Vector of
	 * tokens. Empty tokens are skipped.  splitchar is a single character
	 * indicating a token boundary (multiple characters are not used here).
	 * The split characters are not included.
	*/
	public static Vector tokenizeString(String str,char splitchar) {
		Vector tok = new Vector(20,50);
		int curidx=0,nextidx;
		while ( (nextidx=str.indexOf(splitchar,curidx)) >= 0) {
			if (nextidx > curidx) tok.addElement(str.substring(curidx,nextidx));
			curidx = nextidx+1;
		}
		if (curidx < str.length())
			tok.addElement(str.substring(curidx));
		return tok;
	}

	/** Readline as in BufferedReader.  Skips empty lines!
	*/
	public static String readline(InputStreamReader in) {
		int ch;
		StringBuffer line=null;
		try {
			while (true) {
				ch = in.read();
				if (ch==-1) {
					if (line==null) return null;
					return line.toString();
				}
				if (ch==10 || ch==13) {
					// eol if we read other characters, ignore if not
					if (line!=null) return line.toString();
				} else {
					if (line==null) line=new StringBuffer();
					line.append((char)ch);
				}
			}
		} catch (IOException e) {
			if (line==null) return null;
			return line.toString();
		}
	}


	/** Generate absolute path from relative path by prepending the package
	 * name of this class (and converting the "." to "/".  A
	 * relative path is a path without "/" or an URL protocol at the beginning.
	 * Absolute paths are not changed. */
	public String getAbsolutePath(Object pkg_obj,String filename) {
		if (filename.indexOf("/")==0
		||   (filename.indexOf("://")>=0 && filename.indexOf("://")<=5) ) {
			// path starts with "/" or protocol: do not change
			return filename;
		} else {
			// path does not start with "/": prepend package name
			// with "." in package replaced by fileseparator
			// this doesn't work as getPackage may return null for applets
			// String pkgname = getClass().getPackage().getName();
			String pkgname = pkg_obj.getClass().getName();
			String pkgname_path="";
			Vector tokens = tokenizeString(pkgname,'.');
			//StringTokenizer toker = new StringTokenizer(pkgname,".");
			for (Enumeration e=tokens.elements();e.hasMoreElements();) {
				String tok = (String)e.nextElement();
				if (e.hasMoreElements()) {
					pkgname_path += tok + "/";
				}
			}
			return "/" + pkgname_path + filename;
		}
	}




	/* images */

	/** protected */
	public boolean existsImage(String imgname) {
		return images_exists.containsKey(imgname);
	}

	/** Protected.
	* @param tileid  tile id number as Integer object (note: 0 is undefined)
	* @return the image object, null means not defined */
	public Object getTileImage(Integer tileid) {
		// we assume that images_tile will not contain id for tileid==0 
		//if (tileid==0) return null;
		//Integer tileid_obj = new Integer(tileid);
		String imgid = (String)images_tile.get(tileid);
		if (imgid==null) return null;
		if (!is_resizeable) return (JGImage)images.get(imgid);
		return getImage(imgid);
	}

	/** Gets (non-scaled) image's physical size directly. */
	public JGPoint getImageSize(String imgname) {
		return (JGPoint)image_orig_size.get(imgname);
	}

	/** Quick version does not scale image on demand, and does not
	* give an error when image is not defined.  DEPRECATED. */
	public JGImage getImageQuick(String imgname) {
		return (JGImage)images.get(imgname);
	}

	/** Slow version, (re)scales image on demand if original image is
	* present. */
	public JGImage getImage(String imgname) {
		if (!existsImage(imgname)) throw new JGameError(
				"Image '"+imgname+"' not defined.",true );
		JGImage img = (JGImage)images.get(imgname);
		if (img==null) {
			img = (JGImage)images_orig.get(imgname);
			if (img==null) return null;
			// convert indexed to display-compatible image
			JGColor render_bg_col = render_bg_color;
			if (render_bg_col==null) render_bg_col = bg_color;
			img = img.toDisplayCompatible(alpha_thresh,
					render_bg_col, true, make_bitmask);
			JGPoint size = img.getSize();
			//BufferedImage img2 = JREImage.createCompatibleImage(
			//		size.width,size.height, Transparency.TRANSLUCENT );
			//img2.getGraphics().drawImage(img,0,0,null);
			//img=img2;
			if (width>0 && height>0) {
				if (prescale) {
					JGPoint scaledpos = scalePos(size.x,size.y,false);
					img = img.scale(scaledpos.x,scaledpos.y);
					// convert translucent image to bitmask
					// not necessary?
					//img = imageutil.toCompatibleBitmask(img,alpha_thresh,
					//		render_bg_col,false);
				} // else skip this part for efficiency, even though scalefac
				// is 1.0.
				images.put(imgname,img);
			} else {
				throw new JGameError("Image width, height <= 0 !",true);
			}
		}
		return img;
	}

	public JGImage getImageOrig(String imgname) {
		return (JGImage)images_orig.get(imgname);
	}

	/** protected */
	public JGImage getSubImage(String mapname,int imgnr) {
		ImageMap imgmap = (ImageMap)imagemaps.get(mapname);
		if (imgmap == null) throw new JGameError(
				"Image map '"+mapname+"' not found.",true );
		JGPoint subcoord = imgmap.getImageCoord(imgnr);
		if (subcoord!=null) {
			return imgmap.img.crop(subcoord.x,subcoord.y,
					imgmap.tilex,imgmap.tiley);
		} else {
			return null;
		}
	}


	public void defineMedia(JGEngineInterface eng,String filename) {
		int lnr=1;
		int nr_lines=0;
		filename = getAbsolutePath(eng,filename);
		try {
			InputStream instr = getClass().getResourceAsStream(filename);
			if (instr==null) eng.exitEngine("Cannot open `"+filename+"'.");
			InputStreamReader in = new InputStreamReader(instr);
			if (in==null) eng.exitEngine("Cannot open `"+filename+"'.");
			// count nr of lines in file first
			while (readline(in) != null) nr_lines++;
			if (nr_lines==0) eng.exitEngine("Cannot open `"+filename+"'.");
			// now, read the file
			in = new InputStreamReader(
				getClass().getResourceAsStream( filename ) );
			String line;
			String [] fields = new String [14];
			while ( (line = readline(in)) != null) {
				eng.setProgressBar((double)lnr / (double)nr_lines);
				int i=0;
				Vector tokens = tokenizeString(line,'\t');
				for (Enumeration e=tokens.elements(); e.hasMoreElements(); ) {
					fields[i++] = (String)e.nextElement();
				}
				if (i==8) {
					defineImageMap(eng,
						fields[0], fields[1],
						Integer.parseInt(fields[2]),
						Integer.parseInt(fields[3]),
						Integer.parseInt(fields[4]),
						Integer.parseInt(fields[5]),
						Integer.parseInt(fields[6]),
						Integer.parseInt(fields[7]) );
				} else if (i==9) {
					defineImage(eng, fields[0],fields[1],
						Integer.parseInt(fields[2]),
						fields[3],
						fields[4],
						Integer.parseInt(fields[5]),
						Integer.parseInt(fields[6]),
						Integer.parseInt(fields[7]),
						Integer.parseInt(fields[8])  );
				} else if (i==5) {
					defineImage(eng, fields[0],fields[1],
						Integer.parseInt(fields[2]),
						fields[3],
						fields[4], -1,-1,-1,-1  );
				} else if (i==10) {
					defineImage(fields[0],fields[1],
						Integer.parseInt(fields[2]),
						getSubImage(fields[3],
							Integer.parseInt(fields[4]) ),
						fields[5],
						Integer.parseInt(fields[6]),
						Integer.parseInt(fields[7]),
						Integer.parseInt(fields[8]),
						Integer.parseInt(fields[9])  );
				} else if (i==6) {
					defineImage(fields[0],fields[1],
						Integer.parseInt(fields[2]),
						getSubImage(fields[3],
							Integer.parseInt(fields[4]) ),
						fields[5], -1,-1,-1,-1  );
				} else if (i==3) {
					defineAnimation(fields[0], splitList(fields[1]), 
						Double.parseDouble(fields[2])  );
				} else if (i==4) {
					defineAnimation(fields[0], splitList(fields[1]),
						Double.parseDouble(fields[2]),
						fields[3].equals("true"));
				} else if (i==2) {
					defineAudioClip(eng,fields[0], fields[1]);
				}
				lnr++;
			}
		} catch (JGameError e) {
			eng.exitEngine("Error in "+filename+" line "+lnr+": "+e);
		} catch (Exception e) {
			eng.exitEngine("Error in "+filename+" line "+lnr+":\n"
				+ eng.dbgExceptionToString(e));
		}
		
	}

	/** Split a ';' separated list of words */
	public static String [] splitList(String liststr) {
		Vector list = tokenizeString(liststr,';');
		String [] list_arr = new String [list.size()];
		int i=0;
		for (Enumeration e=list.elements(); e.hasMoreElements(); ) {
			list_arr[i] = (String) e.nextElement();
			i++;
		}
		return list_arr;
	}




	/** Remove all information associated with image, including any cached
	* image data. Does not unload any image maps.  XXX not quite finished;
	* publish this method when finished. */
	public void undefineImage(String name) {
		imageutil.purgeImage((String)images_loaded.get(name));
		images_orig.remove(name);
		image_orig_size.remove(name);
		images.remove(name);
		images_exists.remove(name);
		images_transp.remove(name);
		images_loaded.remove(name);
		images_bbox.remove(name);
		for (int i=bg_images.size()-1; i>=0; i--) {
			BGImage bg_image = (BGImage) bg_images.elementAt(i);
			if (bg_image!=null && bg_image.imgname.equals(name)) {
				bg_images.setElementAt(null,i);
			}
		}
		// XXX association with tile is not yet removed; as we cannot obtain
		// the tile name from the image name
		//Integer tileid = new Integer(tileStrToID(tilename));
		//Hashtable images_tile = new Hashtable(); /* Integers -> Strings */
		//Hashtable images_tilecid = new Hashtable(); /* Integers -> Integers */
	}

	public void defineImage(Object pkg_obj,String name, String tilename,
	int collisionid, String imgfile, String img_op,
	int top,int left, int width,int height) {
		if ( images_loaded.containsKey(name)
		&&  !images_loaded.get(name).equals(imgfile)) {
			// if associated file is not the same, undefine old image
			undefineImage(name);
		}
		JGImage img=null;
		if (!imgfile.equals("null")) {
			imgfile = getAbsolutePath(pkg_obj,imgfile);
			img = imageutil.loadImage(imgfile);
			images_loaded.put(name,imgfile);
		}
		defineImage(name,tilename, collisionid, img,
			img_op, top,left, width,height);
	}

	/** passing -1 to top,left,width,height indicates these have to be taken
	* from the image dimensions.
	*/
	public void defineImage(String name, String tilename, int collisionid,
	JGImage img, String img_op,
	int top,int left, int width,int height) {
		if (img!=null) {
			/* do image ops */
			img_op = img_op.toLowerCase();
			boolean flipx = img_op.indexOf("x") >= 0;
			boolean flipy = img_op.indexOf("y") >= 0;
			boolean rot90  = img_op.indexOf("r") >= 0;
			boolean rot180 = img_op.indexOf("u") >= 0;
			boolean rot270 = img_op.indexOf("l") >= 0;
			//System.out.println("img_op "+img_op+ " "+flipx+" "+flipy);
			if (flipx || flipy) img = img.flip(flipx,flipy);
			if (rot90) { img = img.rotate(90); }
			else if (rot180) { img = img.rotate(180); }
			else if (rot270) { img = img.rotate(270); }
			images_orig.put(name,img);
			image_orig_size.put(name,img.getSize());
		}
		images_exists.put(name, "yes");
		Integer tileid = new Integer(tileStrToID(tilename));
		if (img==null || !img.isOpaque(alpha_thresh))
			images_transp.put(tileid, "yes");
		images_tile.put(tileid, name);
		images_tilecid.put(tileid, new Integer(collisionid));
		// width/height < 0 indicate take bounding box from image dims
		if (width >= 0) {
			images_bbox.put(name,new JGRectangle(top,left,width,height));
		} else {
			JGPoint size;
			if (img==null) size = new JGPoint(0,0);
			else           size = img.getSize();
			images_bbox.put(name,new JGRectangle(0,0,size.x,size.y));
		}
		/* pre-load scaled image to prevent hiccups during gameplay*/
		getImage(name); 
		if (!is_resizeable) {
			// throw away unscaled image to save memory
			images_orig.remove(name);
		}
	}

	public void defineImageRotated(Object pkg_obj,String name, String tilename,
	int collisionid, String srcname, double angle) {
		//JGImage src = (JGImage)images_orig.get(srcname);
		//if (src == null) throw new JGameError(
		//		"Source image '"+srcname+"' not found.",true );
		if ( images_loaded.containsKey(name) ) {
			undefineImage(name);
		}
		String imgfile = (String)images_loaded.get(srcname);
		if (imgfile.equals("null")) throw new JGameError(
				"Source image '"+srcname+"' does not have a filename.",true );

		imgfile = getAbsolutePath(pkg_obj,imgfile);
		JGImage img = imageutil.loadImage(imgfile);
		defineImage(name,tilename, collisionid, img.rotateAny(angle),
			"-", 0,0, -1,-1);
	}


	public void defineImageMap(Object pkg_obj,String mapname, String imgfile,
		int xofs,int yofs, int tilex,int tiley, int skipx,int skipy) {
		imgfile = getAbsolutePath(pkg_obj,imgfile);
		imagemaps.put(mapname, new ImageMap (imageutil, imgfile, xofs,yofs,
			tilex,tiley, skipx,skipy) );
	}

	public JGRectangle getImageBBox(String imgname) {
		return (JGRectangle)images_bbox.get(imgname);
	}


	/*====== image from engine ======*/

	public void defineImage(Object pkg_obj,String imgname, String tilename,
	int collisionid,String imgfile, String img_op) {
		defineImage(pkg_obj,imgname,tilename,collisionid,imgfile, img_op,
		-1,-1,-1,-1);
	}

	public void defineImage(String imgname, String tilename, int collisionid,
	String imgmap, int mapidx, String img_op,
	int top,int left, int width,int height) {
		defineImage(imgname,tilename,collisionid,
			getSubImage(imgmap, mapidx),
			img_op, top,left,width,height);
	}

	public void defineImage(String imgname, String tilename, int collisionid,
	String imgmap, int mapidx, String img_op) {
		defineImage(imgname,tilename,collisionid,
			getSubImage(imgmap, mapidx), img_op, 0,0,-1,-1);
	}




	/*====== PF/view ======*/

	/** Offset that should be set on next frame draw. Offset is clipped so the
	* view always fits within playfield.*/
	void setPendingViewOffset(int xofs,int yofs) {
		if (!pf_wrapx) {
			if (xofs < 0) xofs=0;
			if (xofs > tilex*(nrtilesx-viewnrtilesx))
				xofs = tilex*(nrtilesx-viewnrtilesx);
		}
		if (!pf_wrapy) {
			if (yofs < 0) yofs=0;
			if (yofs > tiley*(nrtilesy-viewnrtilesy))
				yofs = tiley*(nrtilesy-viewnrtilesy);
		}
		pendingxofs = xofs;
		pendingyofs = yofs;
		// update parallax level 0
		if (bg_images.size()>=1) {
			BGImage bgimg = (BGImage) bg_images.elementAt(0);
			if (bgimg!=null) {
				bgimg.xofs = xofs;
				bgimg.yofs = yofs;
			}
		}
	}

	/** Update offset according to pending offset.  Protected. */
	public void updateViewOffset() {
		if (pendingxofs!=xofs || pendingyofs!=yofs) {
			setViewOffset(pendingxofs,pendingyofs);
		}
	}

	/** Update all offset-related variables according to given offset.  */
	void setViewOffset(int xofs,int yofs) {
		if (bg_defined==null) return;
		this.xofs = xofs;
		this.yofs = yofs;
		int oldtilexofs = tilexofs;
		int oldtileyofs = tileyofs;
		tilexofs = divFloor(xofs,tilex) - 1;
		tileyofs = divFloor(yofs,tiley) - 1;
		// XXX does scalePos handle negative numbers properly?
		xofs_scaled = scaleXPos(xofs,false);
		yofs_scaled = scaleYPos(yofs,false);
		calcPFWrapCenter();
		int maxtilexofs = Math.max(tilexofs,oldtilexofs);
		int maxtileyofs = Math.max(tileyofs,oldtileyofs);
		int mintilexofs = Math.min(tilexofs,oldtilexofs);
		int mintileyofs = Math.min(tileyofs,oldtileyofs);
		//bg_defined = new boolean[viewnrtilesx+3][viewnrtilesy+3];
		// wipe all rows and columns that the vector
		// oldtileofs -> tileofs crosses
		for (int yi=mintileyofs; yi<maxtileyofs; yi++) {
			for (int xi=0; xi<viewnrtilesx+3; xi++) {
				bg_defined[xi][moduloFloor(yi,viewnrtilesy+3)]=false;
			}
		}
		for (int xi=mintilexofs; xi<maxtilexofs; xi++) {
			for (int yi=0; yi<viewnrtilesy+3; yi++) {
				bg_defined[moduloFloor(xi,viewnrtilesx+3)][yi]=false;
			}
		}
		//bg_defined[xi%(viewnrtilesx+3)][yi%(viewnrtilesy+3)]=true;
	}

	void calcPFWrapCenter() {
		xofs_mid = xofs + viewnrtilesx*tilex/2 + pf_wrapshiftx;
		yofs_mid = yofs + viewnrtilesy*tiley/2 + pf_wrapshifty;
	}



	/*====== objects from canvas ======*/

	public void markAddObject(JGObject obj) {
		obj_to_add.put(obj.getName(),obj);
	}

	/** Add new object now.  Old object with the same name is replaced
	 * immediately, and its remove() method called.  Skips calling objects.put
	 * when skip_actual_add=true.  This is useful if the caller optimises the
	 * objects.add by adding an entire array at once */
	void addObject(JGObject obj, boolean skip_actual_add) {
		int idx = objects.get(obj.getName());
		if (idx >= 0) {
			JGObject old_obj = (JGObject)objects.values[idx];
			// disable object so it doesn't call engine on removal
			old_obj.removeDone();
			// ensure any dispose stuff in the object is called
			old_obj.remove();
		}
		if (!skip_actual_add)
			objects.put(obj.getName(),obj);
	}

	/** Mark object for removal. */
	void markRemoveObject(String index) {
		int idx = objects.get(index);
		if (idx<0) return;
		obj_to_remove.put(index,(JGObject)objects.values[idx]);
	}

	/** Mark object for removal. */
	void markRemoveObject(JGObject obj) {
		obj_to_remove.put(obj.getName(),obj);
	}

	/** Actually remove object now */
	void doRemoveObject(JGObject obj) {
		obj.removeDone();
		objects.remove(obj.getName());
	}

	/** Mark all objects with given spec for removal. */
	void markRemoveObjects(String prefix,int cidmask,boolean suspended_obj) {
		obj_spec_to_remove.addElement(prefix);
		obj_spec_to_remove.addElement(new Integer(cidmask));
		obj_spec_to_remove.addElement(new Boolean(suspended_obj));
	}
	/** Actually remove objects with given spec, including those in obj_to_add
	 * list.  Uses obj_to_remove as a temp variable.  If anything is already
	 * in obj_to_remove, it is left there. If do_remove_list is true, the
	 * objects are removed and obj_to_remove is cleared.  Otherwise, the
	 * objects to remove are just added to obj_to_remove. */
	void doRemoveObjects(String prefix,int cidmask,boolean suspended_obj,
	boolean do_remove_list) {
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject o = (JGObject) objects.values[i];
			if (cidmask==0 || (o.colid&cidmask)!=0) {
				if (suspended_obj || !o.is_suspended) {
					obj_to_remove.put(objects.keys[i],o);
				}
			}
		}
		if (do_remove_list) doRemoveList();
		// if we enumerate backwards, we can remove elements inline without
		// consistency problems
		for (int i=obj_to_add.size-1; i>=0; i--) {
			JGObject o = (JGObject) obj_to_add.values[i];
			if (prefix==null || obj_to_add.keys[i].startsWith(prefix)) {
				if (cidmask==0 || (o.colid&cidmask)!=0) {
					if (suspended_obj || !o.is_suspended) {
						// Note: remove element inside element enumeration
						obj_to_add.remove(obj_to_add.keys[i]);
						o.removeDone();
					}
				}
			}
		}
	}


	/** protected, remove objects marked for removal. */
	public void flushRemoveList() {
		//for (Enumeration e=obj_to_remove.elements(); e.hasMoreElements();) {
		//	String name = (String)e.nextElement();
		//	JGObject o = (JGObject)objects.get(name);
		//	if (o!=null) { // object might have been removed already
		//		doRemoveObject(o);
		//	}
		//}
		// add all query results from object specs to obj_to_remove
		// don't enumerate when no elements (which is about 90% of the time)
		if (obj_spec_to_remove.size()!=0) {
			for (Enumeration e=obj_spec_to_remove.elements();
			e.hasMoreElements(); ) {
				String prefix = (String) e.nextElement();
				int cid = ((Integer)e.nextElement()).intValue();
				boolean suspended_obj=((Boolean)e.nextElement()).booleanValue();
				doRemoveObjects(prefix,cid,suspended_obj,false);
			}
			obj_spec_to_remove.removeAllElements();
		}
		// remove everything in one go
		doRemoveList();
	}

	/** Actually remove objects in obj_to_remove. */
	void doRemoveList() {
		for (int i=0; i<obj_to_remove.size; i++) {
			((JGObject)obj_to_remove.values[i]).removeDone();
		}
		objects.remove(obj_to_remove);
		obj_to_remove.clear();
	}

	/** Add objects marked for addition. Protected.
	*/
	public void flushAddList() {
		// XXX we have to add one by one because we have to call the dispose
		// method of the objects that are replaced
		for (int i=0; i<obj_to_add.size; i++) {
			addObject((JGObject)obj_to_add.values[i],true);
		}
		// actually add objects to array in one go for faster performance
		objects.put(obj_to_add);
		obj_to_add.clear();
	}

	///** Remove objects marked for addition before they can be added.
	// * Protected.
	//*/
	//public void clearAddList() {
	//	for (int i=0; i<obj_to_add.size; i++) {
	//		// be sure to mark the object as removed
	//		((JGObject)obj_to_add.values[i]).removeDone();
	//	}
	//	obj_to_add.clear();
	//}


	/* public */

	public boolean existsObject(String index) {
		return objects.get(index) >= 0;
	}

	public JGObject getObject(String index) {
		int idx = objects.get(index);
		if (idx<0) return null;
		return (JGObject)objects.values[idx];
	}

	///** Remove all objects.  All objects are marked for removal, the add
	//* list is cleared.  */
	//void clearObjects() {
	//	for (Enumeration e=objects.keys(); e.hasMoreElements(); ) {
	//		markRemoveObject((String)e.nextElement());
	//	}
	//	clearAddList();
	//	//clear_objects=true;
	//}

	public void moveObjects(JGEngineInterface eng,String prefix, int cidmask) {
		if (in_parallel_upd) throw new JGameError("Recursive call",true);
		in_parallel_upd=true;
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject o = (JGObject) objects.values[i];
			if (cidmask!=0 && (o.colid&cidmask)==0) continue;
			// first, update suspend mode
			if (o.is_suspended) {
				if (o.resume_in_view
				&&o.isInView(offscreen_margin_x,offscreen_margin_y)) o.resume();
			} else {
				if (o.expiry==JGObject.suspend_off_view
				||  o.expiry==JGObject.suspend_off_view_expire_off_pf) {
					if (!o.isInView(offscreen_margin_x,offscreen_margin_y))
						o.suspend();
				}
			}
			// move object
			// we must ensure modulo is correct when object is suspended so
			// that it will unsuspend properly
			//o.moduloPos(); // is inlined below
			if (pf_wrapx) o.x = moduloXPos(o.x);
			if (pf_wrapy) o.y = moduloYPos(o.y);
			if (!o.is_suspended) {
				//o.moduloPos(); // is inlined below
				if (pf_wrapx) o.x = moduloXPos(o.x);
				if (pf_wrapy) o.y = moduloYPos(o.y);
				try {
					o.move();
				} catch (JGameError ex) {
					eng.exitEngine(eng.dbgExceptionToString(ex));
				} catch (Exception ex) {
					eng.dbgShowException(o.getName(),ex);
				}
				o.updateAnimation(gamespeed);
				o.x += o.xdir*o.xspeed*gamespeed;
				o.y += o.ydir*o.yspeed*gamespeed;
				//o.moduloPos(); // is inlined below
				if (pf_wrapx) o.x = moduloXPos(o.x);
				if (pf_wrapy) o.y = moduloYPos(o.y);
			}
			// check expiry; object should not expire when suspended
			if (!o.is_suspended) {
				int expiry = (int) o.expiry;
				if (expiry >= 0) {
					o.expiry -= gamespeed;
					if (o.expiry < 0) o.remove();
				} else {
					if (expiry==JGObject.expire_off_pf
					||  expiry==JGObject.suspend_off_view_expire_off_pf) {
						if (!o.isOnPF(offscreen_margin_x,offscreen_margin_y))
							o.remove();
					}
					if (expiry==JGObject.expire_off_view
					&& !o.isInView(offscreen_margin_x,offscreen_margin_y))
						o.remove();
				}
			}
		}
		flushRemoveList();
		in_parallel_upd=false;
	}


	public void moveObjects(JGEngineInterface eng) {
		moveObjects(eng,null,0); 
	}

	/* reused rectangles, used within collision methods */

	JGRectangle tmprect1 = new JGRectangle();
	JGRectangle tmprect2 = new JGRectangle();

	JGObject [] srcobj = new JGObject[50];
	JGObject [] dstobj = new JGObject[50];
	public void checkCollision(JGEngineInterface eng,int srccid,int dstcid) {
		if (in_parallel_upd) throw new JGameError("Recursive call",true);
		in_parallel_upd=true;
		if (objects.size > srcobj.length) {
			// grow arrays to make objects fit
			srcobj = new JGObject[objects.size+50];
			dstobj = new JGObject[objects.size+50];
		}
		int srcsize = 0;
		int dstsize = 0;
		/* get all matching objects */
		JGRectangle sr = tmprect1;
		JGRectangle dr = tmprect2;
		for (int i=0; i<objects.size; i++) {
			JGObject o  = (JGObject)objects.values[i];
			if (o.is_suspended) continue;
			if (!o.getBBox(sr)) continue;
			if ((o.colid & srccid) != 0) {
				srcobj[srcsize++] = o;
			}
			if ((o.colid & dstcid) != 0) {
				dstobj[dstsize++] = o;
			}
		}
		/* check collision */
		for (int si=0; si<srcsize; si++) {
			JGObject srco = srcobj[si];
			if (!srco.getBBox(sr)) continue;
			for (int di=0; di<dstsize; di++) {
				JGObject dsto = dstobj[di];
				if (dsto == srco) continue;
				if (!dsto.getBBox(dr)) continue;
				if (sr.intersects(dr)) {
					try {
						dsto.hit(srco);
					} catch (JGameError ex) {
						eng.exitEngine(eng.dbgExceptionToString(ex));
					} catch (Exception ex) {
						eng.dbgShowException(dsto.getName(),ex);
					}
				}
			}
		}
		flushRemoveList();
		in_parallel_upd=false;
	}

	public int checkCollision(int cidmask, JGObject obj) {
		JGRectangle bbox = obj.getBBox();
		if (bbox==null) return 0;
		int retcid=0;
		JGRectangle obj_bbox = tmprect1;
		for (int i=0; i<objects.size; i++) {
			JGObject o  = (JGObject)objects.values[i];
			if (o==obj) continue;
			if (!o.is_suspended) {
				if (cidmask==0 || (o.colid&cidmask)!=0) {
					if (!o.getBBox(obj_bbox)) continue;
					if (bbox.intersects(obj_bbox)) {
						retcid |= o.colid;
					}
				}
			}
		}
		return retcid;
	}



	public int checkBGCollision(JGRectangle r) {
		return getTileCid(getTiles(r));
	}

	public void checkBGCollision(JGEngineInterface eng,int tilecid,int objcid) {
		if (in_parallel_upd) throw new JGameError("Recursive call",true);
		in_parallel_upd=true;
		if (objects.size > srcobj.length) {
			// grow arrays to make objects fit
			srcobj = new JGObject[objects.size+50];
		}
		int srcsize = 0;
		JGRectangle r = tmprect1;
		/* get all matching objects */
		for (int i=0; i<objects.size; i++) {
			JGObject o  = (JGObject)objects.values[i];
			if (o.is_suspended) continue;
			if (!o.getTileBBox(r)) continue;
			if ((o.colid & objcid) != 0) {
				srcobj[srcsize++] = o;
			}
		}
		/* check collision */
		JGRectangle tiler = tmprect2;
		for (int i=0; i<srcsize; i++) {
			JGObject o = srcobj[i];
			// tile bbox is always defined
			o.getTileBBox(r);
			// fast equivalent of cid=checkBGCollision(r)
			getTiles(tiler,r);
			int cid=getTileCid(tiler);
			if ((cid & tilecid) != 0) {
				try {
					o.hit_bg(cid);
					o.hit_bg(cid,tiler.x,tiler.y,tiler.width,tiler.height);
					// XXX this might be slow, check its performance
					for (int y=0; y<tiler.height; y++) {
						for (int x=0; x<tiler.width; x++) {
							int thiscid = getTileCid(tiler.x+x, tiler.y+y);
							if ( (thiscid&tilecid) != 0)
								o.hit_bg(thiscid, tiler.x+x, tiler.y+y);
						}
					}
				} catch (JGameError ex) {
					eng.exitEngine(eng.dbgExceptionToString(ex));
				} catch (Exception ex) {
					eng.dbgShowException(o.getName(),ex);
				}
			}
		}
		flushRemoveList();
		in_parallel_upd=false;
	}


	/* objects from engine */


	public Vector getObjects(String prefix,int cidmask,boolean suspended_obj,
	JGRectangle bbox) {
		Vector objects_v = new Vector(50,100);
		int nr_obj=0;
		JGRectangle obj_bbox = tmprect1;
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject obj  = (JGObject)objects.values[i];
			if (cidmask==0 || (obj.colid&cidmask)!=0) {
				if (suspended_obj || !obj.is_suspended) {
					if (bbox!=null) {
						if (!obj.getBBox(obj_bbox)) continue;
						if (bbox.intersects(obj_bbox)) {
							objects_v.addElement(obj);
						}
					} else {
						objects_v.addElement(obj);
					}
				}
			}
		}
		return objects_v;
	}


	public void removeObject(JGObject obj) {
		if (in_parallel_upd) { // queue remove
			markRemoveObject(obj);
		} else { // do remove immediately
			doRemoveObject(obj);
		}
	}

	public void removeObjects(String prefix,int cidmask) {
		removeObjects(prefix,cidmask,true);
	}

	public void removeObjects(String prefix,int cidmask,boolean suspended_obj) {
		if (in_parallel_upd) {
			markRemoveObjects(prefix,cidmask,suspended_obj);
		} else {
			doRemoveObjects(prefix,cidmask,suspended_obj,true);
		}
	}


	public int countObjects(String prefix,int cidmask) {
		return countObjects(prefix,cidmask,true);
	}

	public int countObjects(String prefix,int cidmask,boolean suspended_obj) {
		int nr_obj=0;
		int firstidx=getFirstObjectIndex(prefix);
		int lastidx=getLastObjectIndex(prefix);
		for (int i=firstidx; i<lastidx; i++) {
			JGObject obj = (JGObject) objects.values[i];
			if (cidmask==0 || (obj.colid&cidmask)!=0) {
				if (suspended_obj || !obj.is_suspended) {
					nr_obj++;
				}
			}
		}
		return nr_obj;
	}


	int getFirstObjectIndex(String prefix) {
		if (prefix==null) return 0;
		int firstidx = objects.get(prefix);
		if (firstidx<0) firstidx = -1-firstidx;
		return firstidx;
	}

	int getLastObjectIndex(String prefix) {
		if (prefix==null) return objects.size;
		// XXX theoretically there may be strings with prefix
		// lexicographically below this one
		return -1-objects.get(prefix+'\uffff');
	}



	/** Do final update actions on objects after all frame updates finished.
	* Protected. */
	public void frameFinished() {
		for (int i=0; i<objects.size; i++) {
			((JGObject)objects.values[i]).frameFinished();
		}
	}

	/*====== BG/tiles ======*/

	/** Repaint those parts of BG which are undefined according to bg_defined.
	* Handles wraparound if applicable.  If wraparound is on, xofs and yofs
	* may be any value. */
	public void repaintBG(JGEngineInterface eng) {
		if (bg_defined==null) return;
		// first, convert tilechangedmap entries to bg_defined entries. Note
		// that one tilechanged may result in multiple bg undefineds, in case
		// the playfield is smaller than viewnrtiles+3.
//		for (int x=0; x<viewnrtilesx+3; x++) {
//			int xi = x + tilexofs;
//			// check if out of playfield bounds, only needed if not wrapping.
//			if (!pf_wrapx && (xi<0 || xi>=nrtilesx)) continue;
//			int xi_mod = moduloFloor(xi,viewnrtilesx+3);
//			int xi_modpf = xi;
//			if (pf_wrapx) xi_modpf = moduloFloor(xi,nrtilesx);
//			// determine loop bounds for tight loop
//			int ylower = tileyofs;
//			int yupper = tileyofs + viewnrtilesy+3;
//			if (!pf_wrapy) {
//				if (ylower<0) ylower=0;
//				if (yupper<0) continue;
//				if (ylower>nrtilesy) continue;
//				if (yupper>nrtilesy) yupper=nrtilesy; // exclusive bound
//			}
//			for (int yi=ylower; yi<yupper; yi++) {
//				// check if out of playfield bounds: already done
//				//if (!pf_wrapy && (yi<0 || yi>=nrtilesy)) continue;
//				// check if already defined
//				int yi_modpf = yi;
//				if (pf_wrapy)
//					yi_modpf = moduloFloor(yi,nrtilesy);
//				if (!tilechangedmap[xi_modpf][yi_modpf]) continue;
//				int yi_mod = moduloFloor(yi,viewnrtilesy+3);
//				bg_defined[xi_mod][yi_mod]=false;
//			}
//		}
		int nrtilesdrawn=0;
		// The pre-draw area is the 1-tile thick space surrounding the visible
		// area, which does not really need to be drawn.  It is used to
		// pre-draw tiles before they become visible, so that the system can
		// draw tiles a few at a time when scrolling slowly.  Otherwise it
		// would have to wait for the view offset to cross a tile boundary, at
		// which point it would have to draw a whole row/column of tiles at a
		// time.
		//int nrpredrawtilesdrawn=0;
		// The pre-draw quota is the number of tiles that the system should
		// draw in the pre-draw area when it has nothing to do.
		// Upper bound for pre-draw quota is a whole row plus column divided
		// by 2 (so that the work of a diagonal scroll is split into at least
		// 2 equal batches).  A better estimate for the optimal quota may be
		// achieved by estimating the expected scroll speed.  For now we
		// choose a default of (rows+columns)/5.
		int maxpredrawtiles = (viewnrtilesx+viewnrtilesy)/5;
		for (int x=0; x<viewnrtilesx+3; x++) {
			int xi = x + tilexofs;
			// check if out of playfield bounds, only needed if not wrapping.
			if (!pf_wrapx && (xi<0 || xi>=nrtilesx)) continue;
			int xi_mod = moduloFloor(xi,viewnrtilesx+3);
			int xi_modpf = xi;
			if (pf_wrapx) xi_modpf = moduloFloor(xi,nrtilesx);
			// determine loop bounds for tight loop
			int ylower = tileyofs;
			int yupper = tileyofs + viewnrtilesy+3;
			if (!pf_wrapy) {
				if (yupper<0) continue;
				if (ylower<0) ylower=0;
				if (ylower>nrtilesy) continue;
				if (yupper>nrtilesy) yupper=nrtilesy; // exclusive bound
			}
			int divfactor = viewnrtilesy+3;
			for (int yi=ylower; yi<yupper; yi++) {
				// check if out of playfield bounds: already done
				//if (!pf_wrapy && (yi<0 || yi>=nrtilesy)) continue;
				// check if already defined
				//int yi_mod = moduloFloor(yi,divfactor);
				// version with moduloFloor inlined
				int yi_mod = (yi>=0) ? yi%divfactor
								     : divfactor-1 - ((-1-yi)%divfactor);
				if (bg_defined[xi_mod][yi_mod]) continue;
				int yi_modpf = yi;
				if (pf_wrapy) yi_modpf = moduloFloor(yi,nrtilesy);
				//&&  !tilechangedmap[xi_modpf][yi_modpf]) continue;
				int y = yi - tileyofs;
				if (x==0||y==0 || x==viewnrtilesx+2 || y==viewnrtilesy+2){
					// tile is inside the pre-draw area.
					// don't draw if we already reached quota.
					if (nrtilesdrawn > maxpredrawtiles) continue;
					//nrpredrawtilesdrawn++;
				}
				eng.drawTile(xi,yi,tilemap[xi_modpf][yi_modpf]);
				nrtilesdrawn++;
				bg_defined[xi_mod][yi_mod]=true;
				//tilechangedmap[xi_modpf][yi_modpf]=false;
			}
		}
		//System.out.println("Nr. tiles updated: "+nrtilesdrawn);
	}



	public void setBGImage(String bgimg, int depth, boolean wrapx,
	boolean wrapy) {
		while (bg_images.size() < depth+1) bg_images.addElement(null);
		if (bgimg!=null) {
			if (!images.containsKey(bgimg) 
			&&  !images_orig.containsKey(bgimg))
				throw new JGameError("unknown BG image "+bgimg);
			BGImage newimg = new BGImage(bgimg,wrapx,wrapy);
			bg_images.setElementAt(newimg,depth);
			if (depth==0) {
				newimg.xofs = xofs;
				newimg.yofs = yofs;
			}
		} else {
			bg_images.setElementAt(null,depth);
		}
		invalidateBGTiles();
	}

	/** protected */
	public void initBGTiles(int nrtilesx,int nrtilesy,String filltile) {
		this.nrtilesx=nrtilesx;
		this.nrtilesy=nrtilesy;
		pfwidth = nrtilesx*tilex;
		pfheight = nrtilesy*tiley;
		pfwidth_half = pfwidth/2;
		pfheight_half = pfheight/2;
		tilemap = new int [nrtilesx][nrtilesy];
		tilecidmap = new int [nrtilesx][nrtilesy];
		//tilechangedmap = new boolean [nrtilesx][nrtilesy];
		bg_defined = new boolean[viewnrtilesx+3][viewnrtilesy+3];
		fillBG(filltile);
		setViewOffset(0,0);
	}

	/** protected */
	public void invalidateBGTiles() {
		if (bg_defined==null) return;
		for (int x=0; x<viewnrtilesx+3; x++) {
			for (int y=0; y<viewnrtilesy+3; y++) {
				bg_defined[x][y]=false;
			}
		}
	}


	public void setTileSettings(String out_of_bounds_tile,
	int out_of_bounds_cid,int preserve_cids) {
		this.out_of_bounds_tile=out_of_bounds_tile;
		this.out_of_bounds_cid=out_of_bounds_cid;
		this.preserve_cids=preserve_cids;
	}


	public void fillBG(String filltile) {
		for (int y=0; y<nrtilesy; y++) {
			for (int x=0; x<nrtilesx; x++) {
				setTile(x,y,filltile);
			}
		}
	}

	public void setTileCid(int x,int y,int and_mask,int or_mask) {
		// inlined tile out of bounds handling
		if (pf_wrapx) {
			x = moduloFloor(x,nrtilesx);
		} else {
			if (x<0 || x>=nrtilesx) return;
		}
		if (pf_wrapy) {
			y = moduloFloor(y,nrtilesy);
		} else {
			if (y<0 || y>=nrtilesy) return;
		}
		tilecidmap[x][y] &= and_mask;
		tilecidmap[x][y] |= or_mask;
	}

	public void setTile(int x,int y,String tilestr) {
		// inlined tile out of bounds handling
		int x_mod=x,y_mod=y;
		if (pf_wrapx) {
			x_mod = moduloFloor(x,nrtilesx);
		} else {
			if (x<0 || x>=nrtilesx) return;
		}
		if (pf_wrapy) {
			y_mod = moduloFloor(y,nrtilesy);
		} else {
			if (y<0 || y>=nrtilesy) return;
		}
		int tileid = tileStrToID(tilestr);
		tilemap[x_mod][y_mod] = tileid;
		tilecidmap[x_mod][y_mod] &= preserve_cids;
		tilecidmap[x_mod][y_mod] |= tileintToCid(tileid);
		// invalidate tile, to be redrawn later
		// new method of invalidating tile; the tilechanged flag is picked
		// up by repaintBG.
		//tilechangedmap[x_mod][y_mod] = true;

		// update bg_defined
		// check if we are in view
		// XXX for pf_wrap we also need to modulo tilex/yofs and reverse
		// bounds if necessary.  For now we just invalidate too many tiles.
		if (!pf_wrapx) 
			if (x_mod < tilexofs || x_mod >= tilexofs + viewnrtilesx+3) return;
		if (!pf_wrapy) 
			if (y_mod < tileyofs || y_mod >= tileyofs + viewnrtilesy+3) return;
		// find offset in bg
		//int x_mod_bg = moduloFloor(x,viewnrtilesx+3);
		//int y_mod_bg = moduloFloor(y,viewnrtilesy+3);
		// version with moduloFloor inlined
		int x_mod_bg = (x >= 0) ? x%(viewnrtilesx+3)
								: viewnrtilesx+2 - ((-1-x)%(viewnrtilesx+3));
		int y_mod_bg = (y >= 0) ? y%(viewnrtilesy+3)
								: viewnrtilesy+2 - ((-1-y)%(viewnrtilesy+3));
		// in case we wrap and bg is larger than pf, ensure we are within
		// 0 < x_mod_bg < nrtilesx
		if (x_mod_bg >= nrtilesx) x_mod_bg -= nrtilesx;
		if (y_mod_bg >= nrtilesy) y_mod_bg -= nrtilesy;
		// invalidate tile
		bg_defined[x_mod_bg][y_mod_bg]=false;
		// see if we need to undefine multiple tiles, in case of wrap-around
		// and playfield size smaller than bg size
		boolean do_doubley=false;
		if (pf_wrapx) {
			if (viewnrtilesx+3 - nrtilesx > x_mod_bg) {
				bg_defined[x_mod_bg+nrtilesx][y_mod_bg]=false;
				if (pf_wrapy) {
					if (viewnrtilesy+3 - nrtilesy > y_mod_bg) {
						bg_defined[x_mod_bg+nrtilesx][y_mod_bg+nrtilesy]=false;
						do_doubley=true;
					}
				}
			}
		}
		if (do_doubley) {
			bg_defined[x_mod_bg][y_mod_bg+nrtilesy]=false;
		}
		// old routine, which finds whether it's in the viewport but fails to
		// easily find the appropriate bg_defined entry
		/*if (pf_wrapx) {
			int bg_x1_mod = moduloFloor(tilexofs-1,nrtilesx);
			int bg_x2_mod = moduloFloor(tilexofs+viewnrtilesx+2,nrtilesx);
			if (bg_x1_mod < bg_x2_mod) {
				if (x_mod < bg_x1_mod || x_mod >= bg_x2_mod) return;
			} else {
				if (x_mod < bg_x1_mod && x_mod >= bg_x2_mod) return;
			}
		} else {
			if ((x-tilexofs < -1)
			||  (x-tilexofs >= viewnrtilesx+2) ) return;
		}
		if (pf_wrapy) {
			int bg_y1_mod = moduloFloor(tileyofs-1,nrtilesy);
			int bg_y2_mod = moduloFloor(tileyofs+viewnrtilesy+2,nrtilesy);
			if (bg_y1_mod < bg_y2_mod) {
				if (y_mod < bg_y1_mod || y_mod >= bg_y2_mod) return;
			} else {
				if (y_mod < bg_y1_mod && y_mod >= bg_y2_mod) return;
			}
		} else {
			if ((y-tileyofs < -1)
			||  (y-tileyofs >= viewnrtilesy+2)) return;
		}*/
		//System.out.print("/x"+x+"y"+y);
		// XXX modulo is wrong!
		//x = moduloFloor(x+1, viewnrtilesx+3);
		//y = moduloFloor(y+1, viewnrtilesy+3);
		//System.out.print(" x"+x+"y"+y);
		//bg_defined[x][y] = false;
		// XXX these are not yet adapted to wraparound
		// XXX this routine does not work for scrolling!
		//if ((x-tilexofs < 0)
		//||  (x-tilexofs >= viewnrtilesx+3) ) return;
		//if ((y-tileyofs < 0)
		//||  (y-tileyofs >= viewnrtilesy+3)) return;
		//drawTile(x,y,tileid);
	}



	public int countTiles(int tilecidmask) {
		int count=0;
		for (int x=0; x<nrtilesx; x++) {
			for (int y=0; y<nrtilesy; y++) {
				if ( (tilecidmap[x][y]&tilecidmask) != 0) count++;
			}
		}
		return count;
	}

	public int getTileCid(int xidx,int yidx) {
		if (pf_wrapx) {
			xidx = moduloFloor(xidx,nrtilesx);
		} else {
			if (xidx<0 || xidx>=nrtilesx) return out_of_bounds_cid;
		}
		if (pf_wrapy) {
			yidx = moduloFloor(yidx,nrtilesy);
		} else {
			if (yidx<0 || yidx>=nrtilesy) return out_of_bounds_cid;
		}
		return tilecidmap[xidx][yidx];
	}

	public String getTileStr(int xidx,int yidx) {
		if (pf_wrapx) {
			xidx = moduloFloor(xidx,nrtilesx);
		} else {
			if (xidx<0 || xidx>=nrtilesx) return out_of_bounds_tile;
		}
		if (pf_wrapy) {
			yidx = moduloFloor(yidx,nrtilesy);
		} else {
			if (yidx<0 || yidx>=nrtilesy) return out_of_bounds_tile;
		}
		return tileIDToStr(tilemap[xidx][yidx]);
	}

	public int getTileCid(JGRectangle tiler) {
		int cid=0;
		for (int x=tiler.x; x<tiler.x+tiler.width; x++)
			for (int y=tiler.y; y<tiler.y+tiler.height; y++)
				cid |= getTileCid(x,y);
		return cid;
	}

	private int tilestrToCid(String tilestr) {
		if (tilestr==null || tilestr.length()==0) return 0;
		Integer tileid = (Integer)
			images_tilecid.get( new Integer(tileStrToID(tilestr)) );
		if (tileid==null) {
			System.out.println("Warning: unknown tile '"+tilestr+"'.");
			// XXX no reference to dbg
			//dbgPrint("MAIN","Warning: unknown tile '"+tilestr+"'.");
			return 0;
		}
		return tileid.intValue();
	}

	private int tileintToCid(int tileint) {
		if (tileint==0) return 0;
		Integer tileid = (Integer)
			images_tilecid.get( new Integer(tileint) );
		if (tileid==null) {
			System.out.println("Warning: unknown tile '"+tileint+"'.");
			// XXX no reference to dbg
			//dbgPrint("MAIN","Warning: unknown tile '"+tileint+"'.");
			return 0;
		}
		return tileid.intValue();
	}

	/** Convert tile name to integer ID code (as used internally).  The ID
	 * code basically encodes the four characters of the string into the bytes
	 * of the four-byte integer.  The ID code is NOT related to the collision
	 * ID (CID).
	* @param tilestr tilename, null or empty string -&gt; ID = 0 */
	public int tileStrToID(String tilestr) {
		if (tilestr==null) return 0;
		switch (tilestr.length()) {
			case 0: return 0;
			case 1: return (int)tilestr.charAt(0);
			case 2: return (int)tilestr.charAt(0)
			             + (int)tilestr.charAt(1)*256;
			case 3: return (int)tilestr.charAt(0)
			             + (int)tilestr.charAt(1)*256
			             + (int)tilestr.charAt(2)*256*256;
			case 4: return (int)tilestr.charAt(0)
			             + (int)tilestr.charAt(1)*256
			             + (int)tilestr.charAt(2)*256*256
			             + (int)tilestr.charAt(3)*256*256*256;
			default:
				System.out.println(
					"Warning: string '"+tilestr+" has wrong size.");
				// XXX no reference to dbg
				//dbgPrint("Warning: string '"+tilestr+" has wrong size.");
				return 0;
		}
	}

	/** Convert tile ID code to tile name (as used internally).  The ID
	 * code basically encodes the four characters of the string into the bytes
	 * of the four-byte integer.  The ID code is NOT related to the collision
	 * ID (CID).
	* @param tilestr tile ID, tileid==0 -&gt; tilename = empty string */
	public String tileIDToStr(int tileid) {
		if (tileid==0) return "";
		StringBuffer tilestr = new StringBuffer(""+(char)(tileid&255));
		if (tileid >= 0x100) tilestr.append( (char)((tileid/0x100)&255));
		if (tileid >= 0x10000) tilestr.append( (char)((tileid/0x10000)&255));
		if (tileid>=0x1000000) tilestr.append( (char)((tileid/0x1000000)&255));
		return tilestr.toString();
	}

	public boolean getTiles(JGRectangle dest,JGRectangle r) {
		if (r==null) return false;
		dest.copyFrom(r);
		convertToTiles(dest,r);
		return true;
	}

	void convertToTiles(JGRectangle dest,JGRectangle r) {
		if (dest.x >= 0) {
			dest.x /= tilex;
			dest.width  = 1 - dest.x + (r.x + r.width  - 1) / tilex;
		} else {
			dest.x = (dest.x-tilex+1)/tilex;
			dest.width  = 1 - dest.x + (r.x + r.width  - 1 - tilex+1) / tilex;
		}
		if (dest.y >= 0) {
			dest.y /= tiley;
			dest.height = 1 - dest.y + (r.y + r.height - 1) / tiley;
		} else {
			dest.y = (dest.y-tiley+1)/tiley;
			dest.height = 1 - dest.y + (r.y + r.height - 1 - tilex+1) / tiley;
		}
	}

	public JGRectangle getTiles(JGRectangle r) {
		if (r==null) return null;
		JGRectangle tiler = new JGRectangle(r);
		convertToTiles(tiler,r);
		return tiler;
		/*if (tiler.x >= 0) {
			tiler.x /= tilex;
		} else {
			tiler.x = (tiler.x-tilex+1)/tilex;
		}
		if (tiler.y >= 0) {
			tiler.y /= tiley;
		} else {
			tiler.y = (tiler.y-tiley+1)/tiley;
		}
		tiler.width  = 1 - tiler.x + (r.x + r.width  - 1) / tilex;
		tiler.height = 1 - tiler.y + (r.y + r.height - 1) / tiley;
		return tiler;*/
	}




	/* background methods from engine */


	public void setTileCid(int x,int y,int value) {
		setTileCid(x,y,0,value);
	}

	public void orTileCid(int x,int y,int or_mask) {
		setTileCid(x,y,-1,or_mask);
	}

	public void andTileCid(int x,int y,int and_mask) {
		setTileCid(x,y,and_mask,0);
	}

	public void setTile(JGPoint tileidx,String tilename) {
		setTile(tileidx.x,tileidx.y,tilename);
	}

	public void setTiles(int xofs,int yofs,String [] tilemap) {
		for (int y=0; y<tilemap.length; y++) {
			for (int x=0; x<tilemap[y].length(); x++) {
				setTile(x+xofs,y+yofs,
					new String(tilemap[y].substring(x,x+1)) );
			}
		}
	}

	public void setTilesMulti(int xofs,int yofs,String [] tilemap) {
		for (int y=0; y<tilemap.length; y++) {
			Vector tokens = tokenizeString(tilemap[y],' ');
			//StringTokenizer toker = new StringTokenizer(tilemap[y]," ");
			int x=0;
			for (Enumeration e=tokens.elements(); e.hasMoreElements(); ) {
				setTile(x+xofs,y+yofs, (String)e.nextElement());
				x++;
			}
		}
	}

	public int getTileCidAtCoord(double x,double y) {
		int xidx = (int)x / tilex;
		int yidx = (int)y / tiley;
		return getTileCid(xidx,yidx);
	}
	public int getTileCid(JGPoint center, int xofs, int yofs) {
		return getTileCid(center.x+xofs, center.y+yofs);
	}

	public String getTileStrAtCoord(double x,double y) {
		int xidx = (int)x / tilex;
		int yidx = (int)y / tiley;
		return getTileStr(xidx,yidx);
	}

	public String getTileStr(JGPoint center, int xofs, int yofs) {
		return getTileStr(center.x+xofs, center.y+yofs);
	}




	public void drawImageString(JGEngineInterface eng,
	String string, double x, double y, int align,
	String imgmap, int char_offset, int spacing,boolean pf_relative) {
		ImageMap map = (ImageMap) imagemaps.get(imgmap);
		if (map==null) throw new JGameError(
				"Font image map '"+imgmap+"' not found.",true );
		if (align==0) {
			x -= (map.tilex+spacing) * string.length()/2;
		} else if (align==1) {
			x -= (map.tilex+spacing) * string.length();
		}
		//Image img = map.getScaledImage();
		StringBuffer lettername_buf = new StringBuffer(imgmap+"# ");
		int lastchar = lettername_buf.length()-1;
		String lettername=null;
		for (int i=0; i<string.length(); i++) {
			int imgnr = -char_offset+string.charAt(i);
			//String lettername = imgmap+"#"+string.charAt(i);
			lettername_buf.setCharAt(lastchar,string.charAt(i));
			lettername=lettername_buf.toString();
			if (!existsImage(lettername)) {
				defineImage(lettername, "FONT", 0,
					getSubImage(imgmap,imgnr),
					"-", 0,0,0,0);
			}
			JGImage letter = getImage(lettername);
			eng.drawImage(x,y,lettername,pf_relative);
			//eng.drawImage(buf_gfx, x,y,lettername,pf_relative);
			x += map.tilex + spacing;
		}
	}




	/*====== math ======*/

	// XXX replace moduloX/YPos by versions without complexity!

	public double moduloXPos(double x) {
		while (x - xofs_mid > pfwidth_half)  { x -= pfwidth_half*2;  }
		while (x - xofs_mid < -pfwidth_half) { x += pfwidth_half*2;  }
		return x;
	}

	public double moduloYPos(double y) {
		while (y - yofs_mid > pfheight_half)  { y -= pfheight_half*2;  }
		while (y - yofs_mid < -pfheight_half) { y += pfheight_half*2;  }
		return y;
	}

	/** Div which always rounds downwards, also for negative numbers. It is
	 * the counterpart of moduloFloor. */
	public int divFloor(int pos,int div) {
		return (int)Math.floor((double)pos / (double)div);
	}

	/** Modulo according to a regular modulo space, resulting in a value
	 * that is always greater than 0, float version. It is the counterpart
	 * of divFloor. */
	public double moduloFloor(double pos,int div) {
		return pos - div*Math.floor(pos/div);
	}

	/** Modulo according to a regular modulo space, resulting in a value
	 * that is always greater than 0. */
	public int moduloFloor(int pos,int modulo) {
		//int result;
		if (pos >= 0) return pos%modulo;
		return modulo-1 - ((-1-pos)%modulo);
		//return (modulo - ((-pos)%modulo))%modulo;
		//System.out.println(pos+" modulo "+modulo+" = "+result);
		//return result;
	}


	/** The scale methods also take care of wraparound modulo calculations.
	 * This is only applicable to pf_relative positions.  The modulo semantics
	 * is that of moduloPos. */

	public int scaleXPos(double x,boolean pf_relative) {
		if (!pf_relative) {
			return(int)
				Math.floor(x_scale_fac * x);
		} else {
			if (pf_wrapx) x = moduloXPos(x);
			return(int)
				Math.floor(x_scale_fac * x)
				- xofs_scaled;
		}
	}

	public int scaleYPos(double y,boolean pf_relative) {
		if (!pf_relative) {
			return(int)
				Math.floor(y_scale_fac * y);
		} else {
			if (pf_wrapy) y = moduloYPos(y);
			return(int)
				Math.floor(y_scale_fac * y)
				- yofs_scaled;
		}
	}

	public JGPoint scalePos(double x, double y, boolean pf_relative) {
		//XXX inline scaleXPos/scaleYPos
		return new JGPoint(scaleXPos(x,pf_relative),scaleYPos(y,pf_relative));
	}

	/** returns null if r is null */
	public JGRectangle scalePos(double x,double y,double width,double height,
	boolean pf_relative) {
		// XXX that's a lot of object creation
		JGPoint topleft = scalePos(x,y,pf_relative);
		JGPoint botright = scalePos(x+width, y+height, pf_relative);
		// XXX a rectangle on the wrap border is a singular case
		// XXX rounding is not pixel-perfect for this case
		if (botright.x < topleft.x) 
			botright.x = topleft.x + scaleXPos(width,pf_relative);
		if (botright.y < topleft.y) 
			botright.y = topleft.y + scaleYPos(height,pf_relative);
		return new JGRectangle(topleft.x, topleft.y,
			botright.x-topleft.x - 1, botright.y-topleft.y - 1);
	}

	/** returns null if r is null */
	public JGRectangle scalePos(JGRectangle r, boolean pf_relative) {
		if (r==null) return null;
		return scalePos(r.x, r.y, r.width, r.height, pf_relative);
	}


	/* misc */



	/** Initialise some derived pf dimension variables. Also clears the tile
	* map if !is_inited.  Clears the resized image cache if is_inited and
	* is_resizeable.
	*/
	public void initPF() {
		int allowed_width = winwidth + crop_left + crop_right;
		int allowed_height = winheight + crop_top + crop_bottom;
		if (!prescale) {
			// canvas_x/yofs are zero for !prescale, so should x/y_excess be
			allowed_width = winwidth;
			allowed_height = winheight;
		}
		// calculate scaledtilex/y and width/height according to aspect ratio
		// constraints
		scaledtilex = allowed_width / viewnrtilesx;
		scaledtiley = allowed_height / viewnrtilesy;
		double aspectratio = (scaledtilex / (double)scaledtiley)
				/ (tilex / (double)tiley);
		if (aspectratio < min_aspect) {
			// y is too large
			scaledtiley = (int)(scaledtilex / min_aspect);
		} else if (aspectratio > max_aspect) {
			// x is too large
			scaledtilex = (int)(max_aspect * scaledtiley);
		}
		width = scaledtilex*viewnrtilesx;
		height = scaledtiley*viewnrtilesy;
		if (!prescale) {
			// for !prescale, set scaledtilex/y to tilex/y
			scaledtilex = tilex;
			scaledtiley = tiley;
		}
		x_scale_fac = width  / (double)(tilex*viewnrtilesx);
		y_scale_fac = height / (double)(tiley*viewnrtilesy);
		min_scale_fac = Math.min( x_scale_fac, y_scale_fac );
		// now, calculate the offsets. 
		//if (prescale) {
			int x_excess = width-winwidth;
			int y_excess = height-winheight;
			// balance is a number between -1 (shift left) and 1 (shift right)
			double xbalance=0,ybalance=0;
			if (crop_left+crop_right>0 && x_excess>0)
				xbalance = (crop_right-crop_left)/(crop_left+crop_right);
			if (crop_top+crop_bottom>0 && y_excess>0)
				ybalance = (crop_bottom-crop_top)/(crop_top+crop_bottom);
			canvas_xofs = (int)(-x_excess*(0.5 - 0.5*xbalance));
			canvas_yofs = (int)(-y_excess*(0.5 - 0.5*ybalance));
		//} // else use default offsets of 0.0
		if (!is_inited) {
			initBGTiles(nrtilesx, nrtilesy, "");
		} else {
			if (is_resizeable) {
				// clear resized images so that they are reconstructed
				// from images_orig
				// XXX maybe pre-load images to prevent hiccups?
				images = new Hashtable();
			}
		}
	}



	public void setViewOffset(int xofs,int yofs,boolean centered) {
		if (centered) {
			xofs -= viewWidth()/2;
			yofs -= viewHeight()/2;
		}
		setPendingViewOffset(xofs,yofs);
		JGObject.updateEngineSettings();
	}

	public void setBGImgOffset(int depth, double xofs, double yofs,
	boolean centered) { 
		if (centered) {
			xofs -= viewWidth()/2;
			yofs -= viewHeight()/2;
		}
		if (bg_images.size() < depth) 
			throw new JGameError("Parallax depth "+depth+" not defined.");
		BGImage bgimg = (BGImage)bg_images.elementAt(depth);
		bgimg.xofs = xofs;
		bgimg.yofs = yofs;
	}


	public void setPFSize(int nrtilesx,int nrtilesy) {
		initBGTiles(nrtilesx,nrtilesy,"");
		JGObject.updateEngineSettings();
	}

	public void setPFWrap(boolean wrapx,boolean wrapy,int shiftx,int shifty) {
		pf_wrapx = wrapx;
		pf_wrapy = wrapy;
		pf_wrapshiftx = shiftx;
		pf_wrapshifty = shifty;
		// ensure offset is properly clipped
		setViewOffset(pendingxofs,pendingyofs,false);
		calcPFWrapCenter();	
		JGObject.updateEngineSettings();
	}


	public void setFrameRate(double fps, double maxframeskip) {
		this.fps = fps;
		this.maxframeskip = maxframeskip;
	}

	public void setRenderSettings(int alpha_thresh,JGColor render_bg_col) {
		this.alpha_thresh=alpha_thresh;
		this.render_bg_color=render_bg_col;
	}


	public void setOffscreenMargin(int xmargin,int ymargin) {
		offscreen_margin_x = xmargin;
		offscreen_margin_y = ymargin;
	}

	public void setGameSpeed(double speed) {
		gamespeed = speed + 0.00000000001;
		JGObject.updateEngineSettings();
	}



	/* timers */

	public void removeAllTimers() { timers.removeAllElements(); }

	public void registerTimer(JGTimer timer) { timers.addElement(timer); }

	/** protected */
	public void tickTimers() {
		for (int i=timers.size()-1; i>=0; i--) {
			JGTimer timer = (JGTimer)timers.elementAt(i);
			if (timer.tick(gamespeed)) {
				timers.removeElement(timer);
			}
		}
	}



	/* game state */

	public void setGameState(String state) {
		boolean already_in_state = inGameStateNextFrame(state);
		gamestate_nextframe.removeAllElements();
		gamestate_nextframe.addElement(state);
		gamestate_new.removeAllElements();
		if (!already_in_state) gamestate_new.addElement(state);
	}

	public void addGameState(String state) {
		if (!inGameStateNextFrame(state)) {
			gamestate_nextframe.addElement(state);
			gamestate_new.addElement(state);
		}
	}

	public void removeGameState(String state) {
		gamestate_nextframe.removeElement(state);
		gamestate_new.removeElement(state);
	}

	public void clearGameState() { gamestate_nextframe.removeAllElements(); }


	public boolean inGameState(String state) {
		for (int i=gamestate.size()-1; i>=0; i--) {
			if ( ((String)gamestate.elementAt(i)).equals(state) ) return true;
		}
		return false;
	}

	public boolean inGameStateNextFrame(String state) {
		for (int i=gamestate_nextframe.size()-1; i>=0; i--) {
			if ( ((String)gamestate_nextframe.elementAt(i)).equals(state) )
				return true;
		}
		return false;
	}


	/*====== animation ======*/

	public void defineAnimation (String id,
	String [] frames, double speed) {
		animations.put(id, new Animation(frames,speed));
	}

	public void defineAnimation (String id,
	String [] frames, double speed, boolean pingpong) {
		animations.put(id, new Animation(frames,speed,pingpong));
	}

	public Animation getAnimation(String id) {
		return (Animation) animations.get(id);
	}


	/* computation */

	public boolean and(int value, int mask) {
		return (value&mask) != 0;
	}

	public double random(double min, double max) {
		return min + random.nextDouble()*(max-min);
	}

	public double random(double min, double max, double interval) {
		int steps = (int)Math.floor(0.00001 + (max-min)/interval);
		return min + ( (int)(random.nextDouble()*(steps+0.99)) )*interval;
	}

	public int random(int min, int max, int interval) {
		int steps = (max-min)/interval;
		return min + ( (int)(random.nextDouble()*(steps+0.99)) )*interval;
	}

	public JGPoint getTileIndex(double x, double y) {
		return new JGPoint(
			(int)Math.floor( x / (double)tilex ),
			(int)Math.floor( y / (double)tiley )  );
	}

	public JGPoint getTileCoord(int tilex, int tiley) {
		return new JGPoint( tilex*tileWidth(), tiley*tileHeight() );
	}

	public JGPoint getTileCoord(JGPoint tileidx) {
		return new JGPoint( tileidx.x*tileWidth(), tileidx.y*tileHeight() );
	}

	public double snapToGridX(double x, double gridsnapx) {
		if (gridsnapx <= 0.0) return x;
		int xaligned = tilex*(int)
			Math.floor(((x + tilex/2.0) / (double)tilex));
		double gridofsx = Math.abs(x - xaligned);
		if (gridofsx <= gridsnapx+0.0002) return xaligned;
		return x;
	}

	public double snapToGridY(double y, double gridsnapy) {
		if (gridsnapy <= 0.0) return y;
		int yaligned = tiley*(int)
			Math.floor(((y + tiley/2.0) / (double)tiley));
		double gridofsy = Math.abs(y - yaligned);
		if (gridofsy <= gridsnapy+0.0002) return yaligned;
		return y;
	}

	public void snapToGrid(JGPoint p,int gridsnapx,int gridsnapy) {
		if (gridsnapx==0 && gridsnapy==0) return;
		int xaligned = tilex*(int)
			Math.floor(((p.x + tilex/2.0) / (double)tilex));
		int yaligned = tiley*(int)
			Math.floor(((p.y + tiley/2.0) / (double)tiley));
		int gridofsx = Math.abs(p.x - xaligned);
		int gridofsy = Math.abs(p.y - yaligned);
		if (gridofsx <= gridsnapx) p.x = xaligned;
		if (gridofsy <= gridsnapy) p.y = yaligned;
	}

	public boolean isXAligned(double x,double margin) {
		if (margin<0) margin=0.0;
		int xaligned = tilex*(int)(((int)x + tilex/2)
			/ tilex);
		return Math.abs(x - xaligned) <= margin+0.00005;
	}

	public boolean isYAligned(double y,double margin) {
		if (margin<0) margin=0.0;
		int yaligned = tiley*(int)(((int)y + tiley/2)
			/ tiley);
		return Math.abs(y - yaligned) <= margin+0.00005;
	}

	public double getXAlignOfs(double x) {
		int xaligned = tilex*(int)(((int)x + tilex/2)
			/ tilex);
		return x - xaligned;
	}

	public double getYAlignOfs(double y) {
		int yaligned = tiley*(int)(((int)y + tiley/2)
			/ tiley);
		return y - yaligned;
	}

	// XXX please test these two methods

	public double getXDist(double x1, double x2) {
		if (pf_wrapx) {
			double x1mod = moduloFloor(x1,pfwidth);
			double x2mod = moduloFloor(x2,pfwidth);
			return Math.min(Math.abs(x1mod-x2mod),
				Math.abs(x1mod+pfwidth-x2mod) );
		} else {
			return Math.abs(x1-x2);
		}
	}

	public double getYDist(double y1, double y2) {
		if (pf_wrapy) {
			int pfheight = pfheight_half*2;
			double y1mod = moduloFloor(y1,pfheight);
			double y2mod = moduloFloor(y2,pfheight);
			return Math.min(Math.abs(y1mod-y2mod),
				Math.abs(y1mod+pfheight-y2mod) );
		} else {
			return Math.abs(y1-y2);
		}
	}


	/*=== audio ===*/

	/** clipid -} filename */
	public Hashtable audioclips = new Hashtable();
	/** Associate given clipid with a filename.  Files are loaded from the
	* resource path.  Java 1.2+ supports at least: midi and wav files. */

	public void defineAudioClip(Object pkg_obj,String clipid,String filename) {
		filename = getAbsolutePath(pkg_obj,filename);
		audioclips.put(clipid,filename);
		// XXX we should replace the old clip.
		//replace requires all old audioclip instances to be deleted.
	}


}
