package jgame;
import jgame.impl.JGEngineInterface;
import jgame.impl.Animation;
//import java.awt.*;
//import java.awt.image.ImageObserver;
//import java.io.Serializable;
//import java.net.*;

/** Superclass for game objects, override to define animated game objects.
 * When an object is created, it is automatically registered with the
 * currently running engine.  The object will become active only after the
 * frame ends.  The object is managed by the engine, which will display it and
 * call the move and hit methods when appropriate.  Call remove() to remove
 * the object.  It will be removed after the frame ends.  Use isAlive()
 * to see if the object has been removed or not.

 * <p> Each object corresponds to one image.  The object's appearance can be
 * changed using setImage or any of the animation functions.  If you want
 * multi-image objects, use multiple objects and co-reference them using
 * regular references or using JGEngine's getObject().  You can also define
 * your own paint() method to generate any appearance you want.

 * <p>
 * Objects have a pointer to the engine by which they are managed (eng).  This
 * can be used to call the various useful methods in the engine.
 * Alternatively, the objects can be made inner classes of your JGEngine
 * class, so that they have direct access to all JGEngine methods. 

 * <p> The object remembers some of the state of the previous frame (in
 * particular the previous position and bounding boxes), so that corrective
 * action can be taken after something special happened (such as bumping into
 * a wall).  

 * <P> Objects have a direction and speed built in.  After their move() method
 * finishes, their x and y position are incremented with the given
 * speed/direction.  Speed may be used as absolute value (leave the direction
 * at 1), or as a value relative to the direction (the movement is speed*dir).
 * The object speed is automatically multiplied by the game speed (which has
 * default 1.0).

 * <P> Objects can be suspended, which may be useful for having them sleep
 * until they come into view.  When suspended, they are invisible, and their
 * move, hit, and paint methods are not called.  Also, they will not expire.
 * They can still be counted by countObjects and removed by removeObjects, if
 * you choose to do so.  By default, objects resume operation when they enter
 * the view.  This can be disabled by means of the resume_in_view setting.  An
 * object that has suspend_off_view enabled will be suspended immediately at
 * creation when it is off view.

 */
public class JGObject {
	static int next_id = 0; 
	/** global which might be accessed concurrently */
	static JGEngineInterface default_engine=null;

	/** The engine's viewWidth and viewHeight, stored in a local variable
	* for extra speed.*/
	public static int viewwidth,viewheight;
	/** The engine's tileWidth and tileHeight, stored in a local variable
	* for extra speed.*/
	public static int tilewidth,tileheight;
	/** The engine's pfWrap settings, stored in a local variable
	* for extra speed. */
	public static boolean pfwrapx,pfwrapy;
	/** The engine's gamespeed setting, stored in a local variable
	* for extra speed. */
	public static double gamespeed;
	/** The engine's pfWidth/pfHeight, stored in a local variable
	* for extra speed. */
	public static int pfwidth,pfheight;
	/** The engine's viewX/YOfs, stored in a local variable
	* for extra speed. */
	public static int viewxofs,viewyofs;

	/** Set the engine to connect to when a new object is created.  This
	* should be called only by the JGEngine implementation.
	* @param engine  pass null to indicate the engine has exited.
	* @return true if success; false if other engine already running. */
	public static boolean setEngine(JGEngineInterface engine) {
		if (engine==null) {
			default_engine=null;
			return true;
		}
		if (default_engine!=null) return false;
		default_engine=engine;
		// copy some constants from engine for fast access
		viewwidth=engine.viewWidth();
		viewheight=engine.viewHeight();
		tilewidth=engine.tileWidth();
		tileheight=engine.tileHeight();
		updateEngineSettings();
		return true;
	}
	/** Called automatically by the engine to signal changes to pfWrap,
	* gameSpeed, pfWidth/Height, viewX/YOfs.  The current values of these
	* settings are stored in the JGObject local variables. */
	public static void updateEngineSettings() {
		pfwrapx = default_engine.pfWrapX();
		pfwrapy = default_engine.pfWrapY();
		gamespeed = default_engine.getGameSpeed();
		pfwidth = default_engine.pfWidth();
		pfheight = default_engine.pfHeight();
		viewxofs = default_engine.viewXOfs();
		viewyofs = default_engine.viewYOfs();
	}

	// remove object creation when changing anim

	/** Print a message to the debug channel, using the object ID as source */
	public void dbgPrint(String msg) { eng.dbgPrint(name,msg); }

	/** Expiry value: never expire. */
	public static final int expire_never=-1;
	/** Expiry value: expire when off playfield. */
	public static final int expire_off_pf=-2;
	/** Expiry value: expire when out of view. */
	public static final int expire_off_view=-3;
	/** Expiry value: suspend when out of view. */
	public static final int suspend_off_view=-4;
	/** Expiry value: suspend when out of view and expire when out of
	* playfield.*/
	public static final int suspend_off_view_expire_off_pf=-5;

	/** Object position */
	public double x=0, y=0;
	/** Object speed; default=0 */
	public double xspeed=0, yspeed=0;
	/** Object direction, is multiplied with speed; default=1 */
	public int xdir=1,ydir=1;
	/** Collision ID */
	public int colid;
	/** Object's global identifier; may not change during the lifetime of the
	 * object. */
	String name;
	/** Number of move() steps before object removes itself, -1 (default)
	 * is never; -2 means expire when off-playfield, -3 means expire when
	 * off-view, -4 means suspend when off-view, -5 means suspend when
	 * off-view and expire when off-playfield.  See also the expire_ and
	 * suspend_ constants. */
	public double expiry=-1;
	/** If true, object will automatically start() when it is suspended and in
	 * view.  Default is true. */
	public boolean resume_in_view=true;

	private boolean is_alive=true;
	/** Indicates if object is suspended. */
	public boolean is_suspended=false;

	/** Get object's ID */
	public String getName() { return name; }

	/** Get name of current image. */
	public String getImageName() { return imgname; }

	String imgname=null;
	Animation anim=null; /* will update imgname if set */
	String animid=null;
	/** cached value: has to be recomputed when image changes; simply set to
	 * null to do this. */
	JGRectangle imgbbox=null;
	/** tile bbox is the bbox with offset 0; we have to add the current
	 * coordinate to obtain the actual tile bbox.  Set to null to use regular
	 * bbox instead. */
	JGRectangle tilebbox=null;
	/** The bbox that should override the image bbox if not null. */
	JGRectangle bbox=null;

	/** You can use this to call methods in the object's engine.  Even handier
	 * is to have the objects as inner class of the engine. */
	public JGEngineInterface eng;

	/* dimensions of last time drawn  */
	double lastx=0, lasty=0;
	/* bbox/tilebbox is copied into these variables each time */
	JGRectangle lastbbox_copy=new JGRectangle();
	JGRectangle lasttilebbox_copy=new JGRectangle();
	/* These are equal to lastbbox_copy if bbox was defined, or null
	 * otherwise. */
	JGRectangle lastbbox=null;
	JGRectangle lasttilebbox=null; /* actual coordinates */

	private void initObject(JGEngineInterface engine,
	String name,int collisionid) {
		this.eng=engine;
		this.name=name;
		colid=collisionid;
		// XXX the test on suspend should really be done after the
		// constructor of the subclass is finished, in case the position is
		// changed later in the constructor.
		if ((int)expiry==suspend_off_view
		||  (int)expiry==suspend_off_view_expire_off_pf) {
			if (!isInView(eng.getOffscreenMarginX(),eng.getOffscreenMarginY()))
				suspend();
		}
		eng.markAddObject(this);
	}

	/** Clear tile bbox definition so that we use the regular bbox again. */
	public void clearTileBBox() { tilebbox=null; }

	public void setTileBBox(int x,int y, int width,int height) {
		tilebbox = new JGRectangle(x,y,width,height);
	}

	/** Set bbox definition to override the image bbox.  */
	public void setBBox(int x,int y, int width,int height) {
		bbox = new JGRectangle(x,y,width,height);
	}
	/** Clear bbox definition so that we use the image bbox again. */
	public void clearBBox() { bbox=null; }


	///** Set object's tile span by defining the number of tiles and the margin
	// * by which the object's position is snapped for determining the object's
	// * top left position.  */
	//public void setTiles(int xtiles,int ytiles,int gridsnapx,int gridsnapy){
	//	this.xtiles=xtiles;
	//	this.ytiles=ytiles;
	//	this.gridsnapx=gridsnapx;
	//	this.gridsnapy=gridsnapy;
	//}

	public void setPos(double x,double y) {
		this.x=x;
		this.y=y;
	}

	/** Set absolute speed.  Set xdir, ydir to the sign of the supplied speed,
	 * and xspeed and yspeed to the absolute value of the supplied speed.
	 * Passing a value of exactly 0.0 sets the dir to 0. */
	public void setSpeedAbs(double xspeed, double yspeed) {
		if (xspeed < 0.0) {
			xdir = -1;
			this.xspeed = -xspeed;
		} else if (xspeed == 0.0) {
			xdir = 0;
			this.xspeed = 0;
		} else {
			xdir = 1;
			this.xspeed = xspeed;
		}
		if (yspeed < 0.0) {
			ydir = -1;
			this.yspeed = -yspeed;
		} else if (yspeed == 0.0) {
			ydir = 0;
			this.yspeed = 0;
		} else {
			ydir = 1;
			this.yspeed = yspeed;
		}
	}

	/** Set speed and direction in one go. */
	public void setDirSpeed(int xdir,int ydir, double xspeed, double yspeed) {
		this.xdir=xdir;
		this.ydir=ydir;
		this.xspeed=xspeed;
		this.yspeed=yspeed;
	}

	/** Set speed and direction in one go. */
	public void setDirSpeed(int xdir,int ydir, double speed) {
		this.xdir=xdir;
		this.ydir=ydir;
		this.xspeed=speed;
		this.yspeed=speed;
	}

	/** Set relative speed; the values are copied into xspeed,yspeed. */
	public void setSpeed(double xspeed, double yspeed) {
		this.xspeed=xspeed;
		this.yspeed=yspeed;
	}

	/** Set relative speed; the value is copied into xspeed,yspeed. */
	public void setSpeed(double speed) {
		this.xspeed=speed;
		this.yspeed=speed;
	}

	/** Set direction. */
	public void setDir(int xdir, int ydir) {
		this.xdir=xdir;
		this.ydir=ydir;
	}

	/** Set ID of animation or image to display.  First, look for an animation
	 * with the given ID, and setAnim if found.  Otherwise, look for an image
	 * with the given ID, and setImage if found.  Passing null clears the
	 * image and stops the animation.  */
	public void setGraphic(String gfxname) {
		if (gfxname==null) {
			setImage(gfxname);
		} else {
			Animation newanim = eng.getAnimation(gfxname);
			if (newanim!=null) {
				setAnim(gfxname);
			} else {
				setImage(gfxname);
			}
		}
	}

	/** Set ID of image to display; clear animation. Passing null clears
	* the image. */
	public void setImage(String imgname) {
		this.imgname=imgname;
		imgbbox=null;
		anim=null;
		animid=null;
		//stopAnim();
	}
	/** Get object's current animation ID, or image ID if not defined. */
	public String getGraphic() {
		if (animid!=null) return animid;
		return imgname; 
	}

	/* animation */

	/** Set the animation to the given default animation definition, or leave
	 * it as it was if the anim_id is unchanged.  Subsequent changes made in
	 * the animation's parameters do not change the default animation
	 * definition. The changes will be preserved if another call to
	 * setAnimation is made with the same anim_id.  If you want to reset the
	 * animation to the original settings, use resetAnimation().
	 */
	public void setAnim(String anim_id) {
		if (animid==null || !animid.equals(anim_id)) {
			anim = eng.getAnimation(anim_id);
			if (anim==null) {
				eng.dbgPrint(name,"Warning: animation "+anim_id+" not found.");
				return;
			}
			anim = anim.copy();
			animid = anim_id;
			imgname = anim.getCurrentFrame();
		}
	}

	/** Always set the animation to the given default animation definition,
	 * resetting any changes or updates made to the animation. Subsequent
	 * changes made in the animation's parameters do not change the default
	 * animation definition.
	 */
	public void resetAnim(String anim_id) {
		anim = eng.getAnimation(anim_id).copy();
		animid = anim_id;
	}

	/** Clear the animation, the object's current image will remain. */
	public void clearAnim() { anim=null; animid=null; }

	/** Get the ID of the currently running animation. */
	public String getAnimId() { return animid; }

	//public void setAnimIncrement(int increment) {
	//	if (anim!=null) anim.increment=increment;
	//}
	/** Set animation speed; speed may be less than 0, indicating that
	 * animation should go backwards. */
	public void setAnimSpeed(double speed) {
		if (anim!=null) {
			if (speed >= 0) {
				anim.speed=speed;
				anim.increment=1;
			} else {
				anim.speed=-speed;
				anim.increment=-1;
			}
		}
	}
	public void setAnimPingpong(boolean pingpong) {
		if (anim!=null) anim.pingpong=pingpong;
	}
	public void startAnim() { if (anim!=null) anim.start(); }
	public void stopAnim() { if (anim!=null) anim.stop(); }

	/** Reset the animation's state to the start state. */
	public void resetAnim() { if (anim!=null) anim.reset(); }


	/** Create object.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname) {
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setPos(x,y);
		setGraphic(gfxname);
	}

	/** Create object with given expiry.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,int expiry) {
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setPos(x,y);
		setGraphic(gfxname);
		this.expiry=expiry;
	}

	/** Create object with given tile bbox, old style.  Old style constructors
	 * are not compatible with the JGame Flash parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int tilebbox_x,int tilebbox_y, int tilebbox_width,int tilebbox_height) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setTileBBox(tilebbox_x,tilebbox_y,tilebbox_width,tilebbox_height);
	}

	/** Create object with given tile bbox and expiry, old style.
	* Old style constructors are not compatible with the JGame Flash
	* parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int tilebbox_x,int tilebbox_y, int tilebbox_width,int tilebbox_height,
	int expiry) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setTileBBox(tilebbox_x,tilebbox_y,tilebbox_width,tilebbox_height);
		this.expiry=expiry;
	}

	/** Create object with given absolute speed, old style.
	* Old style constructors are not compatible with the JGame Flash
	* parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	double xspeed, double yspeed) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setSpeedAbs(xspeed,yspeed);
	}

	/** Create object with given absolute speed and expiry, old style.
	* Old style constructors are not compatible with the JGame Flash
	* parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	double xspeed, double yspeed, int expiry) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setSpeedAbs(xspeed,yspeed);
		this.expiry = expiry;
	}

	/** Create object with given tile bbox and absolute speed, old style.
	* Old style constructors are not compatible with the JGame Flash
	* parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int tilebbox_x,int tilebbox_y, int tilebbox_width,int tilebbox_height,
	double xspeed, double yspeed) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setTileBBox(tilebbox_x,tilebbox_y,tilebbox_width,tilebbox_height);
		setSpeedAbs(xspeed,yspeed);
	}

	/** Create object with given tile bbox, absolute speed, expiry, old style.
	* Old style constructors are not compatible with the JGame Flash
	* parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int tilebbox_x,int tilebbox_y, int tilebbox_width,int tilebbox_height,
	double xspeed, double yspeed, int expiry) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setTileBBox(tilebbox_x,tilebbox_y,tilebbox_width,tilebbox_height);
		setSpeedAbs(xspeed,yspeed);
		this.expiry = expiry;
	}

	/** Create object with given direction/speed, expiry, old style.
	* Old style constructors are not compatible with the JGame Flash
	* parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int xdir, int ydir, double xspeed, double yspeed, int expiry) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setDirSpeed(xdir,ydir,xspeed,yspeed);
		this.expiry = expiry;
	}

	/** Create object with given tile bbox, direction/speed, expiry, old
	 * style.
	* Old style constructors are not compatible with the JGame Flash
	* parameter order.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int tilebbox_x,int tilebbox_y, int tilebbox_width,int tilebbox_height,
	int xdir, int ydir, double xspeed, double yspeed, int expiry) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setTileBBox(tilebbox_x,tilebbox_y,tilebbox_width,tilebbox_height);
		setDirSpeed(xdir,ydir,xspeed,yspeed);
		this.expiry = expiry;
	}


	/** Flash style constructors */

	/** Create object with given absolute speed, expiry, new
	 * style.  New-style constructors enable easier porting to JGame Flash.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int expiry,
	double xspeed,double yspeed) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setTileBBox(tilebbox.x,tilebbox.y,tilebbox.width,tilebbox.height);
		setSpeedAbs(xspeed,yspeed);
		this.expiry = expiry;
	}

	/** Create object with given direction/speed, expiry, new
	 * style.  New-style constructors enable easier porting to JGame Flash.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int expiry,
	double xspeed,double yspeed,
	int xdir,int ydir) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setDirSpeed(xdir,ydir,xspeed,yspeed);
		this.expiry = expiry;
	}

	/** Create object with given tile bbox, direction/speed, expiry, new
	 * style.  New-style constructors enable easier porting to JGame Flash.
	* @param unique_id  append name with unique ID if unique_id set
	* @param gfxname  id of animation or image, null = no image */
	public JGObject (String name, boolean unique_id,
	double x,double y,int collisionid,String gfxname,
	int expiry,
	double xspeed,double yspeed,
	int xdir,int ydir,
	JGRectangle tilebbox) {
		setPos(x,y);
		initObject(default_engine,
				name + (unique_id ? ""+(next_id++) : "" ), collisionid );
		setGraphic(gfxname);
		setTileBBox(tilebbox.x,tilebbox.y,tilebbox.width,tilebbox.height);
		setDirSpeed(xdir,ydir,xspeed,yspeed);
		this.expiry = expiry;
	}


	/* Bounding box functions.  Return copies. May return null if
	 * image is not defined. */

	/** Copy object collision bounding box in pixels into bbox_copy. Has actual
	* coordinate offset.  If bounding box is not defined, bbox_copy is
	* unchanged.
	* @return false if bbox is null, true if not null  */
	public boolean getBBox(JGRectangle bbox_copy) {
		if (bbox!=null) {
			bbox_copy.x = bbox.x+(int)x;
			bbox_copy.y = bbox.y+(int)y;
			bbox_copy.width = bbox.width;
			bbox_copy.height= bbox.height;
			return true;
		}
		//updateImageBBox();
		// inlined updateImageBBox
		if (imgbbox==null && imgname!=null) {
			imgbbox = eng.getImageBBox(imgname);
		}
		if (imgbbox!=null) {
			bbox_copy.x = imgbbox.x+(int)x;
			bbox_copy.y = imgbbox.y+(int)y;
			bbox_copy.width=imgbbox.width;
			bbox_copy.height=imgbbox.height;
			return true;
		}
		return false;
	}

	/** Get object collision bounding box in pixels.
	* Has actual coordinate offset.
	* @return copy of bbox in pixel coordinates, null if no bbox
	*/
	public JGRectangle getBBox() {
		if (bbox!=null) return new JGRectangle(bbox.x+(int)x, bbox.y+(int)y,
				bbox.width, bbox.height);
		updateImageBBox();
		if (imgbbox!=null) {
			return new JGRectangle(imgbbox.x+(int)x, imgbbox.y+(int)y,
				imgbbox.width, imgbbox.height );
		}
		return null;
	}

	//JGRectangle bbox_const = new JGRectangle(0,0,0,0);

	/** Get object collision bounding box in pixels.
	* Has actual coordinate offset.  Uses a fixed temp variable to store the
	* bbox, so no object is created.  The returned object may not be modified,
	* nor its contents used after the method is re-entered.
	* @return bbox in pixel coordinates, null if no bbox
	*/
	//public JGRectangle getBBoxConst() {
	//	if (!getBBox(bbox_const)) return null;
	//	return bbox_const;
	//}

	/** Get object collision bounding box in pixels of previous frame.
	* @return pixel coordinates, null if no bbox */
//	public JGRectangle getLastBBox() {
//		if (lastbbox==null) return null;
//		return new JGRectangle(lastbbox);
//	}

	/** Get tile collision bounding box in pixels and store it in bbox_copy.
	* Bounding box has actual coordinate offset.  If bounding box is not
	* defined, bbox_copy is unchanged.
	* @return  false when bounding box is not defined, true otherwise */
	public boolean getTileBBox(JGRectangle bbox_copy) {
		if (tilebbox==null) {
			return getBBox(bbox_copy);
		}
		bbox_copy.x = (int)x+tilebbox.x;
		bbox_copy.y = (int)y+tilebbox.y;
		bbox_copy.width = tilebbox.width;
		bbox_copy.height= tilebbox.height;
		return true;
	}

	/** Get tile collision bounding box in pixels.
	* Bounding box has actual coordinate offset.
	* @return  copy of bbox in pixel coordinates, null if no bbox */
	public JGRectangle getTileBBox() {
		if (tilebbox==null) return getBBox();
		return new JGRectangle((int)x+tilebbox.x,(int)y+tilebbox.y,
			tilebbox.width,tilebbox.height);
	}

	//JGRectangle tilebbox_const = new JGRectangle(0,0,0,0);
	/** Get tile collision bounding box in pixels.
	* Bounding box has actual coordinate offset.   Returns reference to a
	* fixed internal variable.  Do not change the object returned, avoid
	* re-entering this method and then using the previous value returned.
	* @return  pixel coordinates, null if no bbox */
	//public JGRectangle getTileBBoxConst() {
	//	if (tilebbox==null) {
	//		JGRectangle bbox = getBBoxConst();
	//		if (bbox==null) return null;
	//		tilebbox_const.copyFrom(bbox);
	//		return tilebbox_const;
	//	}
	//	tilebbox_const.x = (int)x+tilebbox.x;
	//	tilebbox_const.y = (int)y+tilebbox.y;
	//	tilebbox_const.width = tilebbox.width;
	//	tilebbox_const.height= tilebbox.height;
	//	return tilebbox_const;
	//}


	/** Get tile collision bounding box of previous frame.
	* @return  pixel coordinates, null if no bbox */
//	public JGRectangle getLastTileBBox() {
//		if (lasttilebbox==null) return null;
//		return new JGRectangle(lasttilebbox);
//	}

	/** Get collision bounding box of object's image (same as object's default
	* bbox, note that the offset is (0,0) here).
	* @return  copy of bbox's pixel coordinates, null if no bbox */
	public JGRectangle getImageBBox() {
		updateImageBBox();
		if (imgbbox==null) return null;
		return new JGRectangle(imgbbox);
	}

	/** Get collision bounding box of object's image (same as object's default
	* bbox, note that the offset is (0,0) here). Optimised version of
	* getImageBBox().  Do not change the value of the object!
	* @return  *original* bbox's pixel coordinates, null if no bbox */
	public JGRectangle getImageBBoxConst() {
		updateImageBBox();
		return imgbbox;
	}

	/** Update the imgbbox variable. */
	void updateImageBBox() {
		if (imgbbox==null && imgname!=null) {
			imgbbox = eng.getImageBBox(imgname);
		}
	}

	/** Get x position of previous frame. Returns 0 if first frame. */
	public double getLastX() { return lastx; }
	/** Get y position of previous frame. Returns 0 if first frame. */
	public double getLastY() { return lasty; }


	/* snap functions */

	/** Snap object to grid using the default gridsnap margin of
	 * (xspeed*gamespeed-epsilon, yspeed*gamespeed-epsilon),
	 * corresponding to the default is...Aligned margin. */
	public void snapToGrid() {
		x = eng.snapToGridX(x,Math.abs(xspeed*gamespeed-0.001));
		y = eng.snapToGridY(y,Math.abs(yspeed*gamespeed-0.001));
	}

	/** Snap object to grid.
	* @param gridsnapx margin below which to snap, 0.0 is no snap
	* @param gridsnapy margin below which to snap, 0.0 is no snap */
	public void snapToGrid(double gridsnapx,double gridsnapy) {
		x = eng.snapToGridX(x,gridsnapx);
		y = eng.snapToGridY(y,gridsnapy);
	}
	///** Snap object to grid. */
	//public void snapToGrid(int gridsnapx, int gridsnapy) {
	//	JGPoint p = new JGPoint((int)x,(int)y);
	//	eng.snapToGrid(p,gridsnapx,gridsnapy);
	//	x = p.x;
	//	y = p.y;
	//}
	/** Snap an object's tile bbox corner to grid; floats are rounded down.
	* Snaps to bottom or right of object instead of top and left if the resp.
	* flags are set. Note that bottom and right alignment means that the
	* object's bounding box is one pixel away from crossing the tile border.
	* @param snap_right snap the right hand side of the tile bbox
	* @param snap_bottom snap the bottom of the tile bbox */
	public void snapBBoxToGrid(double gridsnapx,double gridsnapy,
	boolean snap_right, boolean snap_bottom) {
		JGRectangle bbox = getTileBBox();
		double bx = x + bbox.x;
		double by = y + bbox.y;
		if (snap_right)  bx += bbox.width;
		if (snap_bottom) by += bbox.height;
		double bxs = eng.snapToGridX(bx,gridsnapx);
		double bys = eng.snapToGridY(by,gridsnapy);
		bxs -= bbox.x;
		bys -= bbox.y;
		if (snap_right)  bxs -= bbox.width;
		if (snap_bottom) bys -= bbox.height;
		x = bxs;
		y = bys;
	}

	/* bg interaction */

	// temp variables used by get*Tiles and isOnScreen/PF
	JGRectangle temp_bbox_copy = new JGRectangle();

	// variables returned by get*Tiles
	JGRectangle tiles_copy = null;
	JGRectangle last_tiles_copy = null;
	JGRectangle center_tiles_copy = new JGRectangle();
	JGRectangle last_center_tiles_copy = null;
	JGPoint center_tile_copy = null;
	JGPoint tl_tile_copy = null;
	/** Get the tile index coordinates of all the tiles that the object's
	* tile bbox overlaps with.  Always returns the same temp object, so no
	* object creation is necessary.
	* @return tile index coordinates, null if no bbox */
	public JGRectangle getTiles() {
		//orig: return eng.getTiles(getTileBBox());
		if (!getTileBBox(temp_bbox_copy)) return null;
		if (tiles_copy==null) tiles_copy = new JGRectangle();
		if (eng.getTiles(tiles_copy,temp_bbox_copy))
			return tiles_copy;
		return null;
	}

	/** Get the tile index coordinates of the object's previous tile bbox.
	* Always returns the same temp object, so no object creation is necessary.
	* @return tile index coordinates, null if no tile bbox */
//	public JGRectangle getLastTiles() {
//		//orig: return eng.getTiles(getLastTileBBox());
//		// XXX object creation in getLastTileBBox
//		JGRectangle bbox = getLastTileBBox();
//		if (bbox==null) return null;
//		if (last_tiles_copy==null) last_tiles_copy = new JGRectangle();
//		if (eng.getTiles(last_tiles_copy,bbox))
//			return last_tiles_copy;
//		return null;
//	}

	/** get center tiles from bbox and store them in tiles_copy */
	private JGRectangle getCenterTiles(JGRectangle bbox,JGRectangle tiles_copy){
		// XXX create a getTileIndexX and Y?
		//JGPoint p =
		//	eng.getTileIndex(bbox.x+(bbox.width/2),bbox.y+(bbox.height/2) );
		int px,py;
		if (bbox.x >= 0) {
			px=((bbox.x+(bbox.width/2))
							/ tilewidth)*tilewidth + tilewidth/2;
			bbox.x = tilewidth * ( (px-(bbox.width /2)) / tilewidth);
		} else {
			px=((bbox.x+(bbox.width/2) - tilewidth+1)
							/ tilewidth)*tilewidth + tilewidth/2;
			bbox.x = tilewidth * ( (px-(bbox.width /2) - tilewidth+1)
														/ tilewidth);
		}
		if (bbox.y >= 0) {
			py=((bbox.y+(bbox.height/2))
							/ tileheight)*tileheight+ tileheight/2;
			bbox.y = tileheight* ( (py-(bbox.height/2)) / tileheight);
		} else {
			py=((bbox.y+(bbox.height/2) - tileheight+1)
							/ tileheight)*tileheight + tileheight/2;
			bbox.y = tileheight * ( (py-(bbox.height /2) - tileheight+1)
														/ tileheight);
		}
		eng.getTiles(tiles_copy, bbox);
		return tiles_copy;
	}

	/** Get the tile indices spanning the tiles that the object has the
	* most overlap with.  The size of the span is
	* always the same as size of the tile bbox in tiles. For example, if the
	* tile bbox is 48x32 pixels and the tile size is 32x32 pixels, the size
	* in tiles is always 1.5x1 tiles, which is rounded up to 2x1 tiles.
	* Always returns the same temp object, so no object creation is necessary.
	*
	* @return  tile index coordinates, null if no tile bbox is defined */
	public JGRectangle getCenterTiles() {
		if (!getTileBBox(temp_bbox_copy)) return null;
		/* find the tile on which the center of the bounding box is located,
		 * that will be the center of our tile span. */
		return getCenterTiles(temp_bbox_copy,center_tiles_copy);
	}

	/** Get the tile indices spanning the tiles that the object's last
	 * bounding box had the most overlap with.
	* Always returns the same temp object, so no object creation is necessary.
	*@return tile index coordinates, null if no tile bbox */
//	public JGRectangle getLastCenterTiles() {
//		// XXX object creation in getLastTileBBox
//		JGRectangle bbox=getLastTileBBox();
//		if (bbox==null) return null;
//		if (last_center_tiles_copy==null)
//			last_center_tiles_copy = new JGRectangle();
//		return getCenterTiles(bbox,last_center_tiles_copy);
//	}

	/** Get the top left center tile of the object (that is, the x and y of
	* getCenterTiles()).  If the object is 1x1 tile in size, you get the
	* center tile.  If the object is larger, you get the top left tile of the
	* center tiles.
	* Always returns the same temp object, so no object creation is necessary.
	* @return tile index coordinate, null if no tile bbox */
	public JGPoint getCenterTile() {
		if (!getTileBBox(temp_bbox_copy)) return null;
		if (center_tile_copy==null) center_tile_copy = new JGPoint();
		// XXX center_tiles_copy is used also
		getCenterTiles(temp_bbox_copy,center_tiles_copy);
		center_tile_copy.x = center_tiles_copy.x;
		center_tile_copy.y = center_tiles_copy.y;
		return center_tile_copy;
	}

	/** Get the topleftmost tile of the object.
	* Always returns the same temp object, so no object creation is necessary.
	* @return tile index coordinate, null if no bbox */
	public JGPoint getTopLeftTile() {
		// XXX this global is used also
		JGRectangle r = getTiles();
		if (r==null) return null;
		if (tl_tile_copy==null) tl_tile_copy = new JGPoint();
		tl_tile_copy.x = r.x;
		tl_tile_copy.y = r.y;
		return tl_tile_copy;
	}

	/** Returns true if both isXAligned() and isYAligned() are true.
	* @see #isXAligned()
	* @see #isYAligned() */
	public boolean isAligned() {
		return isXAligned() && isYAligned();
	}

	/** Returns true if x is distance xspeed-epsilon away from being grid
	* aligned. If an object moves at its current xspeed, this method will
	* always return true when the object crosses the tile alignment line, and
	* return false when the object is snapped to grid, and then
	* moves xspeed*gamespeed away from its aligned position. */
	public boolean isXAligned() {
		return eng.isXAligned(x,Math.abs(xspeed*gamespeed-0.001));
	}

	/** Returns true if y is distance yspeed-epsilon away from being grid
	* aligned. If an object moves at its current yspeed, this method will
	* always return true when the object crosses the tile alignment line, and
	* return false when the object is snapped to grid, and then
	* moves yspeed*gamespeed away from its aligned position. */
	public boolean isYAligned() {
		return eng.isYAligned(y,Math.abs(yspeed*gamespeed-0.001));
	}

	/** Returns true if x is within margin of being tile grid aligned. Epsilon
	 * is added to the margin, so that isXAligned(1.0000, 1.0000)
	 * always returns true. */
	public boolean isXAligned(double margin) {
		return eng.isXAligned(x,margin);
	}

	/** Returns true if y is within margin of being tile grid aligned. Epsilon
	 * is added to the margin, so that isXAligned(1.0000, 1.0000)
	 * always returns true. */
	public boolean isYAligned(double margin) {
		return eng.isYAligned(y,margin);
	}

	/** Returns true if the left of the object's tile bbox is within margin of
	* being tile grid aligned. */
	public boolean isLeftAligned(double margin) {
		JGRectangle bbox = getTileBBox();
		if (bbox!=null) {
			return eng.isXAligned(bbox.x,margin);
		} else {
			return eng.isXAligned(x,margin);
		}
	}

	/** Returns true if the top of the object's tile bbox is within margin of
	* being tile grid aligned. */
	public boolean isTopAligned(double margin) {
		JGRectangle bbox = getTileBBox();
		if (bbox!=null) {
			return eng.isYAligned(bbox.y,margin);
		} else {
			return eng.isYAligned(y,margin);
		}
	}

	/** Returns true if the right of the object's tile bbox is within margin of
	* being tile grid aligned. Note that right aligned means that the bbox is
	* one pixel away from crossing the tile border. */
	public boolean isRightAligned(double margin) {
		JGRectangle bbox = getTileBBox();
		if (bbox!=null) {
			return eng.isXAligned(bbox.x+bbox.width,margin);
		} else {
			return eng.isXAligned(x,margin);
		}
	}

	/** Returns true if the bottom of the object's tile bbox is within margin of
	* being tile grid aligned. Note that right aligned means that the bbox is
	* one pixel away from crossing the tile border. */
	public boolean isBottomAligned(double margin) {
		JGRectangle bbox = getTileBBox();
		if (bbox!=null) {
			return eng.isYAligned(bbox.y+bbox.height,margin);
		} else {
			return eng.isYAligned(y,margin);
		}
	}

	/** Check collision of this object with other objects, when the object
	 * position would be offset by xofs,yofs.  Returns 0 when object's bbox is
	 * not defined.
	 * @param cid  cid mask of objects to consider, 0=any */
	public int checkCollision(int cid, double xofs, double yofs) {
		double oldx=x, oldy=y;
		x += xofs;
		y += yofs;
		int retcid =  eng.checkCollision(cid,this);
		x = oldx;
		y = oldy;
		return retcid;
	}

	/* something odd going on here: it fails to find the checkBGCollision in
	 * Engine when I define this one in Object
	public int checkBGCollision() {
		return eng.checkBGCollision(getTileBBox());
	}*/
	
	/** Check collision of tiles within given rectangle, return the OR of all
	* cids found.  This method just calls eng.checkBGCollision; it's here
	* because JGEngine.checkBGCollision(JGRectangle) is masked when the object
	* is an inner class of JGEngine.
	*/
	public int checkBGCollision(JGRectangle r) {
		return eng.checkBGCollision(r);
	}


	/** Get OR of Tile Cids of the object's current tile bbox at the current
	* position, when offset by the given offset. Returns 0 if tile bbox is not
	* defined. */
	public int checkBGCollision(double xofs,double yofs) {
		double oldx=x, oldy=y;
		x += xofs;
		y += yofs;
		JGRectangle bbox = getTileBBox();
		x = oldx;
		y = oldy;
		if (bbox==null) return 0;
		return checkBGCollision(bbox);
	}

	/** Margin is the margin beyond which the object is considered offscreen.
	* The tile bounding box is used as the object's size, if there is none, we
	* use a zero-size bounding box.  isOnScreen is equal to isOnPF, but is
	* deprecated because the name is ambiguous.
	* @deprecated Use isOnPF and isInView according to your situation.
	* @param marginx pixel margin, may be negative
	* @param marginy pixel margin, may be negative
	*/
	public boolean isOnScreen(int marginx,int marginy) {
		return isOnPF(marginx,marginy);
	}

	/** Margin is the margin beyond which the object is considered off-view.
	* The tile bounding box is used as the object's size, if there is none, we
	* use a zero-size bounding box.
	* @param marginx pixel margin, may be negative
	* @param marginy pixel margin, may be negative
	*/
	public boolean isInView(int marginx,int marginy) {
		if (!getTileBBox(temp_bbox_copy)) {
			temp_bbox_copy.x = (int)x;
			temp_bbox_copy.y = (int)y;
			temp_bbox_copy.width = 0;
			temp_bbox_copy.height = 0;
		}
		if (temp_bbox_copy.x + temp_bbox_copy.width  < viewxofs-marginx)
			return false;
		if (temp_bbox_copy.y + temp_bbox_copy.height < viewyofs-marginy)
			return false;
		if (temp_bbox_copy.x > viewxofs+viewwidth + marginx)
			return false;
		if (temp_bbox_copy.y > viewyofs+viewheight + marginy)
			return false;
		return true;
	}

	/** Margin is the margin beyond which the object is considered off the
	* playfield.
	* The tile bounding box is used as the object's size, if there is none, we
	* use a zero-size bounding box.
	* @param marginx pixel margin, may be negative
	* @param marginy pixel margin, may be negative
	*/
	public boolean isOnPF(int marginx,int marginy) {
		if (!getTileBBox(temp_bbox_copy)) {
			temp_bbox_copy.x = (int)x;
			temp_bbox_copy.y = (int)y;
			temp_bbox_copy.width = 0;
			temp_bbox_copy.height = 0;
		}
		if (!pfwrapx) {
			if (temp_bbox_copy.x+temp_bbox_copy.width  < -marginx) return false;
			if (temp_bbox_copy.x >pfwidth + marginx) return false;
		}
		if (!pfwrapy) {
			if (temp_bbox_copy.y+temp_bbox_copy.height < -marginy) return false;
			if (temp_bbox_copy.y > pfheight + marginy) return false;
		}
		return true;
	}

	/* computation */

	/** A Boolean AND shorthand to use for collision;
	* returns (value&amp;mask) != 0. */
	public static boolean and(int value, int mask) {
		return (value&mask) != 0;
	}

	public double random(double min, double max) {
		return eng.random(min,max);
	}

	public double random(double min, double max, double interval) {
		return eng.random(min,max,interval);
	}

	public int random(int min, int max, int interval) {
		return eng.random(min,max,interval);
	}



	/** Do automatic animation.  Is automatically called by the JGEngine
	 * implementation once for every
	 * moveObjects; normally it is not necessary to call this.  */
	public void updateAnimation(double gamespeed) {
		if (anim!=null) {// && eng.canvas.anim_running) {
			imgname = anim.animate(gamespeed);
			imgbbox=null;
		}
	}
	/** Signal that a new frame has just been updated; make
	* snapshot of object state.  Should only be called by the JGEngine
	* implementation. */
	public void frameFinished() {
		lastx=x;
		lasty=y;
//		if (getBBox(lastbbox_copy)) {
//			lastbbox = lastbbox_copy;
//		} else {
//			lastbbox=null;
//		}
//		if (getTileBBox(lasttilebbox_copy)) {
//			lasttilebbox = lasttilebbox_copy;
//		} else {
//			lasttilebbox=null;
//		}
	}

	/** Modulo x/y position according to wrap settings. Is automatically 
	* performed by the JGEngine implementation
	* after each moveObjects.  Normally it is not necessary to call this, but
	* it may be in special cases.  */
	public void moduloPos() {
		// note: there is an inlined version of this code in EngineLogic
		if (pfwrapx) x = eng.moduloXPos(x);
		if (pfwrapy) y = eng.moduloYPos(y);
	}

	/** Suspend object until either resume is called or, if
	 * resume_in_view is true, when it comes into view.  A suspended object
	 * does not participate in any move, hit, or paint. */
	public void suspend() { is_suspended=true; }
	/** Resume from suspended state, if suspended. */
	public void resume() { is_suspended=false; }

	/** Check if object is suspended. */
	public boolean isSuspended() { return is_suspended; }

	/** Set resume condition.
	* @param resume_in_view resume when in view */
	public void setResumeMode(boolean resume_in_view) {
		this.resume_in_view=resume_in_view;
	}

	/** Check if object is still active, or has already been removed.  Also
	 * returns false if the object is still pending to be removed.  */
	public boolean isAlive() { return is_alive; }

	/** Signal to object that remove is done, don't call directly.  This is
	 * used by the engine to signal that the object should be finalised. */
	public final void removeDone() {
		destroy();
		is_alive=false; 
	}

	/** Mark object for removal, ignore if already removed. 
	* The object will be removed at the end of this
	* moveObjects, checkCollision, or checkBGCollision action, or immediately
	* if not inside either of these actions. 
	*/
	public void remove() {
		if (isAlive()) eng.removeObject(this); 
		is_alive=false; 
	}

	/** Override to implement object disposal code.  This method is called at
	 * the actual moment of removal. */
	public void destroy() {}

	/** Override to implement automatic move; default is do nothing.
	*/
	public void move() {}

	/** Override to handle collision; default is do nothing.
	*/
	public void hit(JGObject obj) {}

	/** Override to handle tile collision; default is do nothing.
	*/
	public void hit_bg(int tilecid) {}
	
	/** Override to handle tile collision; default is do nothing.
	* This method is always called when hit_bg(int) is also called, only, extra
	* information is passed that may be useful.
	*/
	public void hit_bg(int tilecid,int tx,int ty, int txsize,int tysize) {}

	/** Override to handle tile collision; default is do nothing.
	* This method is always called when hit_bg(int) is also called, only it may
	* be called multiple times, namely once for each tile the object collides
	* with.  The arguments pass the tile cid and position of that tile.  It is
	* always called after hit_bg(int) and hit_bg(int,int,int,int,int) have
	* been called. */
	public void hit_bg(int tilecid,int tx,int ty) {}

	/** Override to define custom paint actions. */
	public void paint() {}
}

