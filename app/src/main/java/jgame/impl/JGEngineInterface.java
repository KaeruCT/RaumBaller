package jgame.impl;
import jgame.*;
import jgame.impl.Animation;
import java.util.Vector;

// the dependences we should get rid of
//import java.awt.Point;
//import java.awt.Rectangle;
//import java.awt.Image;
//import java.awt.Font;
//import java.awt.Color;
//import java.awt.Cursor;

/** Interface defining all platform-independent methods in JGEngine.  It is
 * primarily used as an internal interface for JGObjects, JGTimers, etc to the
 * different engine implementations.  */
public interface JGEngineInterface {

	// backend interface
/*
	// XXX should not be public
	public void registerTimer(JGTimer timer);

	public boolean inGameStateNextFrame(String state);

	public void dbgPrint(String source,String msg);


	public Point getTileIndex(double x, double y);

	public Rectangle getTiles(Rectangle r);


	public boolean isXAligned(double x,double margin);
	public boolean isYAligned(double y,double margin);

	public double snapToGridX(double x, double gridsnapx);
	public double snapToGridY(double y, double gridsnapy);

	public void snapToGrid(Point p,int gridsnapx,int gridsnapy);


	// XXX maybe not public
	public double moduloXPos(double x);

	// XXX maybe not public
	public double moduloYPos(double y);


	public int checkBGCollision(Rectangle r);

	public void checkBGCollision(int tilecid,int objcid);

	public int checkCollision(int cidmask, JGObject obj);


	public int viewXOfs();
	public int viewYOfs();

	public int viewWidth();
	public int viewHeight();

	public int pfTileWidth();
	public int pfTileHeight();

	public int pfWidth();
	public int pfHeight();

	public boolean pfWrapX();
	public boolean pfWrapY();

	public int getOffscreenMarginX();
	public int getOffscreenMarginY();




	// XXX should not be public
	public void markAddObject(JGObject obj);

	public void removeObject(JGObject obj);


	// XXX should not be public
	public Animation getAnimation(String id);


	public Rectangle getImageBBox(String imgname);
*/

	public static final String JGameVersionString = "3.6";


	/** Cursor keys for both regular and mobile keyboard. */
	public static final int KeyUp=38,KeyDown=40,KeyLeft=37,KeyRight=39;
	/** On a mobile, the cursor control Fire is the same as shift. */
	public static final int KeyShift=16;
	/** Fire stands for a mobile key, indicating the fire button of the cursor
	 * controls.  It is equivalent to KeyShift. */
	public static final int KeyFire=16;
	public static final int KeyCtrl=17;
	public static final int KeyAlt=18;
	public static final int KeyEsc=27;
	/** On a mobile, pressing "*" also triggers KeyEnter. */
	public static final int KeyEnter=10;
	/** The mobile Star key, equal to '*'. */
	public static final int KeyStar='*';
	/** The mobile Pound key, equal to '#'. */
	public static final int KeyPound='#';
	public static final int KeyBackspace=8; /* is it different sometimes? */
	public static final int KeyTab=9;
	/** Keymap equivalent of mouse button. */
	public static final int KeyMouse1=256, KeyMouse2=257, KeyMouse3=258;



	/** Keycode of cursor key. */
	//public static final int KeyUp=38,KeyDown=40,KeyLeft=37,KeyRight=39;
	//public static final int KeyShift=16;
	//public static final int KeyCtrl=17;
	//public static final int KeyAlt=18;
	//public static final int KeyEsc=27;
	//public static final int KeyEnter=10;
	//public static final int KeyBackspace=127;
	////public static final int KeyBackspace=KeyEvent.VK_BACK_SPACE;
	//public static final int KeyTab=8;
	////public static final int KeyTab=KeyEvent.VK_TAB;
	/** Keymap equivalent of mouse button. */
	//public static final int KeyMouse1=256, KeyMouse2=257, KeyMouse3=258;



	/** Set progress bar position in the load screen.
	* @param pos  a number between 0 and 1 */
	public void setProgressBar(double pos);

	/** Set progress message, default "Loading files..." */
	public void setProgressMessage(String msg);

	/** Set author message, default "JGame [version]"  */
	public void setAuthorMessage(String msg);

	/* images */

	// public but not in interface
	//public ImageUtil imageutil = new ImageUtil(this);


	/** Gets (scaled) image directly. Is usually not necessary. Returns
	* null if image is a null image; throws error if image is not defined.  */
	public JGImage getImage(String imgname);

	/** Gets (non-scaled) image's physical size directly.  The object returned
	 * may be a reference to an internal variable, do not change it! */
	public JGPoint getImageSize(String imgname);

	/** If an image with name already exists, it is removed from memory.
	* @param imgfile  filespec, loaded as resource
	*/
	/** Define new sprite/tile image from a file.  If an image with this 
	* id is already defined, it is removed from any caches, so that the old
	* image is really unloaded.  This can be used to load large (background)
	* images on demand, rather than have them all in memory.  Note that the
	* unloading does not work for images defined from image maps. Defining
	* an image with the same name and filename twice does not cause the image
	* to be reloaded, but keeps the old image.
	*
	* @param name  image id
	* @param tilename  tile id (1-4 characters)
	* @param collisionid  cid to use for tile collision matching
	* @param imgfile  filespec in resource path; "null" means no file
	* @param top  collision bounding box dimensions
	* @param left  collision bounding box dimensions
	* @param width  collision bounding box dimensions
	* @param height  collision bounding box dimensions
	*/
	public void defineImage(String name, String tilename, int collisionid,
	String imgfile, String img_op,
	int top,int left, int width,int height);

	/** Define new image by rotating an already loaded image.  This method
	 * does not yet work for images defined from image maps! The destination
	 * image is always a square which is large enough to fit the source image
	 * at any angle. Its dimension is calculated as: max(width, height,
	 * 0.75*(width+height)).  The source image is rendered to its center.  
	 * <p> If an image with this id is already defined,
	 * it is removed from any caches, so that the old image is really
	 * unloaded.  This can be used to load large (background) images on
	 * demand, rather than have them all in memory.  Note that the unloading
	 * does not work for images defined from image maps.
	* @param imgname  image id
	* @param tilename  tile id (1-4 characters)
	* @param collisionid  cid to use for tile collision matching
	* @param srcimg image id of (already loaded) source image
	* @param angle  the angle in radians by which to rotate
	*/
	public void defineImageRotated(String imgname, String tilename,
	int collisionid, String srcimg, double angle);

	/** Define image map, a large image containing a number of smaller images
	* to use for sprites or fonts.  The images must be in a regularly spaced
	* matrix.  One may define multiple image maps with the same image but
	* different matrix specs. 
	* @param mapname  id of image map
	* @param imgfile  filespec in resource path
	* @param xofs  x offset of first image 
	* @param yofs  y offset of first image
	* @param tilex  width of an image
	* @param tiley  height of an image
	* @param skipx  nr of pixels to skip between successive images
	* @param skipy  nr of pixels to skip between successive images vertically.
	*/
	public void defineImageMap(String mapname, String imgfile,
	int xofs,int yofs, int tilex,int tiley, int skipx,int skipy);

	/** Gets the collision bounding box of an image. */
	public JGRectangle getImageBBox(String imgname);


	/*====== image from engine ======*/

	/** Define new sprite/tile image from a file, with collision bounding box
	* equal to the image's dimensions. If an image with this 
	* id is already defined, it is removed from any caches, so that the old
	* image is really unloaded.  This can be used to load large (background)
	* images on demand, rather than have them all in memory.  Note that the
	* unloading does not work for images defined from image maps. Defining
	* an image with the same name and filename twice does not cause the image
	* to be reloaded, but keeps the old image.
	*
	* @param imgname  image id
	* @param tilename  tile id (1-4 characters)
	* @param collisionid  cid to use for tile collision matching
	* @param imgfile  filespec in resource path; "null" means no file
	*/
	public void defineImage(String imgname, String tilename, int collisionid,
	String imgfile, String img_op);

	/** Define new sprite/tile image from map.
	* @param imgname  image id
	* @param tilename  tile id (1-4 characters)
	* @param collisionid  cid to use for tile collision matching
	* @param imgmap  id of image map
	* @param mapidx  index of image in map, 0=first
	* @param top  collision bounding box dimensions
	* @param left  collision bounding box dimensions
	* @param width  collision bounding box dimensions
	* @param height  collision bounding box dimensions
	*/
	public void defineImage(String imgname, String tilename, int collisionid,
	String imgmap, int mapidx, String img_op,
	int top,int left, int width,int height);

	/** Define new sprite/tile image from map, with collision bounding box
	* equal to the image's dimensions.
	* @param imgname  image id
	* @param tilename  tile id (1-4 characters)
	* @param collisionid  cid to use for tile collision matching
	* @param imgmap  id of image map
	* @param mapidx  index of image in map, 0=first
	*/
	public void defineImage(String imgname, String tilename, int collisionid,
	String imgmap, int mapidx, String img_op);

	/** Load a set of imagemap, image, animation, and audio clip definitions
	* from a file.
	* The file contains one image / imagemap / animation definition / audio
	* clip
	* on each line, with the fields separated by one or more tabs.  Lines not
	* matching the required number of fields are ignored.
	* The fields have the same order as in defineImage, defineImageMap, 
	* defineAnimation, and defineAudioClip. For example:
	* <p>
	* <code>defineImage("mytile", "#", 1,"gfx/myimage.gif", "-");</code>
	* <p>
	* is equivalent to the following line in the table:
	* <p>
	* <code>mytile &nbsp;&nbsp;&nbsp; # &nbsp;&nbsp;&nbsp;  1
	* &nbsp;&nbsp;&nbsp; gfx/myimage.gif &nbsp;&nbsp;&nbsp; -</code>
	* <p>
	* with the whitespace between the fields consisting of one or more tabs.
	* The defineAnimation methods take an array of names as the second
	* argument.  This is represented in table format as the names separated by
	* semicolon ';' characters.  So:
	* <P>
	* <code>defineAnimation("anim",new String[]{"frame0","frame1",...},0.5);
	* </code><p>
	* is equivalent to:
	* <p>
	* <code>anim &nbsp;&nbsp;&nbsp; frame0;frame1;... &nbsp;&nbsp;&nbsp; 0.5
	* </code>
	**/
	public void defineMedia(String filename);



	/*====== BG/tiles ======*/


	/** Set image to display behind transparent tiles.  Image size must be a
	 * multiple of the tile size. Passing null turns off background image; the
	 * background colour will be used instead.
	 * @param bgimg  image name, null=turn off background image */
	public void setBGImage(String bgimg);


	/** Set image to display at a particular parallax scroll level.  Only
	* some platforms support parallax scrolling.  On other platforms, only the
	* level 0 image is displayed, with its offset equal to the view
	* offset.  Level 0 corresponds to the top level; setBGImage(String) is
	* equivalent to setBGImage(String,0,true,true).   The level 0 image
	* follows the view offset by default, higher levels are initialised to
	* offset (0,0) by default.
	* @param bgimg  image name, null=turn off image at this level
	* @param depth  depth level, 0=topmost
	* @param wrapx  image should wrap in x direction
	* @param wrapy  image should wrap in y direction
	*/
	public void setBGImage(int depth, String bgimg,boolean wrapx,boolean wrapy);


	/** Define background tile settings.  Default is setBGCidSettings("",0,0).
	* @param out_of_bounds_tile  tile string to use outside of screen bounds
	* @param out_of_bounds_cid  cid to use outside of screen boundaries
	* @param preserve_cids  cid mask to preserve when setting tiles */
	public void setTileSettings(String out_of_bounds_tile,
	int out_of_bounds_cid,int preserve_cids);

	/** Fill the background with the given tile.
	* @param filltile null means use background colour */
	public void fillBG(String filltile);


	/*====== objects from canvas ======*/

	/** Add new object, will become active next frame, do not call directly.
	* This method is normally called automatically by the JGObject
	* constructor.  You should not need to call this directly.*/
	public void markAddObject(JGObject obj);


	/** Get object if it exists.
	*/
	public boolean existsObject(String index);

	/** Get object if it exists, null if not.
	*/
	public JGObject getObject(String index);

	/** Call the move() methods of those objects matching the given name
	 * prefix and collision id mask.
	* @param cidmask collision id mask, 0 means ignore
	* @param prefix  ID prefix, null means ignore  */
	public void moveObjects(String prefix, int cidmask);

	/** Call the move() methods of all registered objects. */
	public void moveObjects();



	/** Calls all colliders of objects that match dstcid that collide with
	* objects that match srccid.
	*/
	public void checkCollision(int srccid,int dstcid);

	/** Checks collision of objects with given cid mask with given object. 
	* Suspended objects are not counted (same as checkCollision(int,int)).
	* This method should be a more efficient way to check for object overlap
	* than getObjects, though it's still not very efficient.
	* @param cidmask  cid mask of objects to consider, 0 means any
	* @return the OR of the CIDs of all object overlapping r
	*/
	public int checkCollision(int cidmask, JGObject obj);

	/** Check collision of tiles within given rectangle, return the OR of all
	* cids found.
	* @param r  bounding box in pixel coordinates
	*/
	public int checkBGCollision(JGRectangle r);

	/** Calls all bg colliders of objects that match objid that collide with
	* tiles that match tileid.
	*/
	public void checkBGCollision(int tilecid,int objcid);

	/* objects from engine */

	/** Query the object list for objects matching the given name prefix, CID
	* mask, and collide with the given bounding box.  If suspended_obj is
	* true, suspended objects are also included.  The list of objects
	* returned match all the supplied criteria.  This is an inefficient
	* method, use sparingly.
	* @param cidmask collision id mask, 0 means ignore
	* @param prefix  ID prefix, null means ignore  
	* @param suspended_obj  also count suspended objects
	* @param bbox  collision bounding box, null means ignore */
	public Vector getObjects(String prefix,int cidmask,boolean suspended_obj,
	JGRectangle bbox);

	/** Remove one particular object. The actual removal is done after the
	* current moveObjects or check*Collision ends, or immediately if done
	* from within the main doFrame loop.*/
	public void removeObject(JGObject obj);

	/** Remove all objects which have the given name prefix and/or match the
	* given cidmask.  It also removes suspended objects.
	* The actual matching and removal is done after the
	* current moveObjects or check*Collision ends, or immediately if done
	* from within the main doFrame loop.  It also removes any matching
	* objects which are pending to be added the next frame.  
	* @param cidmask collision id mask, 0 means ignore
	* @param prefix  ID prefix, null means ignore  */
	public void removeObjects(String prefix,int cidmask);

	/** Remove all objects which have the given name prefix and/or match the
	* given cidmask.  You can specify whether to remove suspended objects or
	* not.  The actual matching and removal is done after the
	* current moveObjects or check*Collision ends, or immediately if done
	* from within the main doFrame loop.  It also removes any matching
	* objects which are pending to be added the next frame.
	* @param cidmask collision id mask, 0 means ignore
	* @param prefix  ID prefix, null means ignore
	* @param suspended_obj  also count suspended objects */
	public void removeObjects(String prefix,int cidmask,boolean suspended_obj);

	/** Count how many objects there are with both the given name prefix and
	* have colid&amp;cidmask != 0.  Either criterion can be left out. 
	* It also counts suspended objects.  Actually
	* searches the object array, so it may be inefficient to use it a lot of
	* times.   
	* @param cidmask collision id mask, 0 means ignore
	* @param prefix  ID prefix, null means ignore  */
	public int countObjects(String prefix,int cidmask);

	/** Count how many objects there are with both the given name prefix and
	* have colid&amp;cidmask != 0.  Either criterion can be left out.  You can
	* specify whether to count suspended objects or not.  Actually
	* searches the object array, so it may be inefficient to use it a lot of
	* times. 
	* @param cidmask collision id mask, 0 means ignore
	* @param prefix  ID prefix, null means ignore  
	* @param suspended_obj  also count suspended objects */
	public int countObjects(String prefix,int cidmask,boolean suspended_obj);


	/*====== tiles ======*/

	/** Set the cid of a single tile using and and or mask. */
	public void setTileCid(int x,int y,int and_mask,int or_mask);

	/** Set a single tile.
	*/
	public void setTile(int x,int y,String tilestr);

	/** Count number of tiles with given mask. Actually searches all tiles, so
	 * it's inefficient and should be used sparingly (such as, determine
	 * the number of something at the beginning of a game). */
	public int countTiles(int tilecidmask);

	/** Get collision id of tile at given tile index position. Moduloes the
	* given position if wraparound */
	public int getTileCid(int xidx,int yidx);

	/** get string id of tile at given index position.
	* Moduloes the given position if wraparound */
	public String getTileStr(int xidx,int yidx);

	/** Get the OR of the cids at the tile indexes given by tiler */
	public int getTileCid(JGRectangle tiler);

	/** Get tile index range of all tiles overlapping given rectangle of pixel
	* coordinates. 
	* Get tile position range of all tiles overlapping given rectangle.
	* Returns null is rectangle is null.
	* @param r   rectangle in pixel coordinates, null is none
	* @return  tile indices */
	public JGRectangle getTiles(JGRectangle r);

	/** Get tile index range of all tiles overlapping given rectangle of pixel
	* coordinates, version without object creation.
	* Get tile position range of all tiles overlapping given rectangle.
	* Returns false is rectangle is null.
	* @param r   rectangle in pixel coordinates, null is none
	* @param dest  rectangle to copy tile range into
	* @return true if rectangle exists, false if null */
	public boolean getTiles(JGRectangle dest,JGRectangle r);

	/* background methods from engine */

	/** Draw tile directly on background, do not call this
	 * directly, use setTile instead.  */
	public void drawTile(int xi,int yi,int tileid);

	/** Set the cid of a single tile to the given value, leaving the actual
	 * tile. */
	public void setTileCid(int x,int y,int value);

	/** Modify the cid of a single tile by ORing a bit mask, leaving the actual
	 * tile. */
	public void orTileCid(int x,int y,int or_mask);

	/** Modify the cid of a single tile by ANDing a bit mask, leaving the actual
	 * tile. */
	public void andTileCid(int x,int y,int and_mask);

	/** Set a single tile.
	*/
	public void setTile(JGPoint tileidx,String tilename);

	/** Set a block of tiles according to the single-letter tile names in the
	* nxm character array tilemap.  
	*/
	public void setTiles(int xofs,int yofs,String [] tilemap);

	/** Set a block of tiles according to the tile names in the nxm element
	* array tilemap.  The tile names may be multiple characters.  Each String
	* in the tilemap consists of a list of tile names separated by spaces. So:
	* <code> "x aa ab abc"</code> stands for a sequence of four tiles, "x",
	* "aa", "ab", and "abc".
	*/
	public void setTilesMulti(int xofs,int yofs,String [] tilemap);

	/** Get collision id of the tile at given pixel coordinates.
	*/
	public int getTileCidAtCoord(double x,double y);

	/** Get the tile cid of the point that is (xofs,yofs) from the tile index
	 * coordinate center. */
	public int getTileCid(JGPoint center, int xofs, int yofs);

	/** Get string id of the tile at given pixel coordinates.
	*/
	public String getTileStrAtCoord(double x,double y);

	/** Get the tile string of the point that is (xofs,yofs) from the tile
	* index coordinate center. */
	public String getTileStr(JGPoint center, int xofs, int yofs);

	/** Convert tile name to integer ID code (as used internally).  The ID
	* code basically encodes the four characters of the string into the bytes
	* of the four-byte integer.  The ID code is NOT related to the collision
	* ID (CID).
	* @param tilestr tilename, null or empty string -&gt; ID = 0 */
	public int tileStrToID(String tilestr);

	/** Convert tile ID code to tile name (as used internally).  The ID
	 * code basically encodes the four characters of the string into the bytes
	 * of the four-byte integer.  The ID code is NOT related to the collision
	 * ID (CID).
	* @param tileid tile ID, tileid==0 -&gt; tilename = empty string */
	public String tileIDToStr(int tileid);

	/*====== math ======*/


	/** A modulo that moduloes symmetrically, relative to the
	 * middle of the view.  That is, the returned x/ypos falls within
	 * -pfwidth/height_half and pfwidth/height_half of x/yofs_mid */
	public double moduloXPos(double x);

	/** A modulo that moduloes symmetrically, relative to the
	 * middle of the view.  That is, the returned x/ypos falls within
	 * -pfwidth/height_half and pfwidth/height_half of x/yofs_mid */
	public double moduloYPos(double y);


	/** Show bounding boxes around the objects: the image bounding box
	 * (getBBox) , the tile span (getTiles), and the center tiles
	 * (getCenterTiles).  */
	public void dbgShowBoundingBox(boolean enabled);

	/** Show the game state in the bottom right corner of the screen. The
	 * message font and foreground colour are used to draw the text. */
	public void dbgShowGameState(boolean enabled);

	/** Indicates whether to show full exception stack traces or just the
	 * first lines.  Default is false.  */
	public void dbgShowFullStackTrace(boolean enabled);

	/** Output messages on playfield instead of console. Default is true.
	 * Messages printed by an object are displayed close to that object.
	 * Messages printed by the main program are shown at the bottom of the
	 * screen.  The debug message font is used to display the messages.
	 * <p>A message that is generated in this frame is shown in the foreground
	 * colour at the appropriate source.  If the source did not generate a
	 * message, the last printed message remains visible, and is shown in
	 * debug colour 1.  If an object prints a message, and then dies, the
	 * message will remain for a period of time after the object is gone.
	 * These messages are shown in debug colour 2.
	 */
	public void dbgShowMessagesInPf(boolean enabled);

	/** Set the number of frames a debug message of a removed object should
	 * remain on the playfield. */
	public void dbgSetMessageExpiry(int ticks);

	/** Set the font for displaying debug messages. */
	public void dbgSetMessageFont(JGFont font);

	/** Set debug color 1, used for printing debug information. */
	public void dbgSetDebugColor1(JGColor col);

	/** Set debug color 2, used for printing debug information. */
	public void dbgSetDebugColor2(JGColor col);


	/** Print a debug message, with the main program being the source. */
	public void dbgPrint(String msg);

	/** Print a debug message from a specific source, which is either the main
	 * program or a JGObject.
	* @param source  may be object ID or "MAIN" for the main program. */
	public void dbgPrint(String source,String msg);

	/** Print the relevant information of an exception as a debug message.
	* @param source  may be object ID or "MAIN" for the main program. */
	public void dbgShowException(String source, Throwable e);

	/**Convert the relevant information of an exception to a multiline String.*/
	public String dbgExceptionToString(Throwable e);

	/** Exit, optionally reporting an exit message.  The exit message can be
	 * used to report fatal errors.  In case of an application or midlet, the
	 * program exits.  In case of an applet, destroy is called, and the exit
	 * message is displayed on the playfield.
	 * @param msg an exit message, null means none */
	public void exitEngine(String msg);


	/** Init engine as component to be embedded in a frame or panel;
	* call this in your engine constructor.
	 * @param width  canvas width
	 * @param height canvas height */
	public void initEngineComponent(int width,int height);

	/** Init engine as applet; call this in your engine constructor.  Applet
	 * init() will start the game.
	 */
	public void initEngineApplet();

	/** Init engine as application.  Passing (0,0) for width, height will
	 * result in a full-screen window without decoration.  Passing another
	 * value results in a regular window with decoration.
	 * @param width  real screen width, 0 = use screen size
	 * @param height real screen height, 0 = use screen size */
	public void initEngine(int width,int height);

	/** Set canvas dimensions and message colours/fonts.  You must call this
	 * in initCanvas().
	 * @param nrtilesx  nr of tiles horizontally
	 * @param nrtilesy  nr of tiles vertically
	 * @param tilex  width of one tile
	 * @param tiley  height of one tile
	 * @param fgcolor pen/text colour, null for default white
	 * @param bgcolor background colour, null for default black
	 * @param msgfont font for messages and text drawing, null for default */
	public void setCanvasSettings(int nrtilesx,int nrtilesy,int tilex,int tiley,
	JGColor fgcolor, JGColor bgcolor, JGFont msgfont);

	/** Set scaling preferences for translating the virtual playfield to the
	 * actual display. You can only call this in initCanvas().
	 * You can set the allowed aspect ratio and the crop margin here.
	 * Aspect ratio is defined as
	 * the ratio (actual_tile_width / actual_tile_height)
	 * / (virtual_tile_width / virtual_tile_height).  So, if the tile size of
	 * the scaled display is (3,2) pixels, and the original was (4,4) pixels,
	 * the aspect ratio is 1.5.  Default values for min_aspect_ratio and
	 * max_aspect_ratio are resp 0.75 and 1.333. Setting both to 1.0 means
	 * you always get a square aspect ratio.
	 * <P>
	 * Crop margin can be used if you wish to allow the scaling algorithm to
	 * take just a few pixels off your playfield in order to make a wider tile
	 * size fit.  The tile size is always integer,
	 * so even a best-fit scaled tile size may leave an unused border around
	 * the playfield, which may be undesirable for small screens.
	 * Cropping just a few pixels off the playfield may be just
	 * enough to make the tiles 1 pixel larger.  	 
	 * Setting a crop to a value greater than zero means you
	 * allow the playfield to fall off the canvas for the amount of actual
	 * pixels specified, in order to make a larger tile size fit.  
	 * Default crop margin is 0.
	 * <P>
	 *
	 * @param min_aspect_ratio minimum width:height ratio allowed
	 * @param max_aspect_ratio maximum width:height ratio allowed
	 * @param crop_top number of pixels to crop at top
	 * @param crop_left number of pixels to crop at left size
	 * @param crop_bottom number of pixels to crop at bottom
	 * @param crop_right number of pixels to crop at right size
	 */
	public void setScalingPreferences(double min_aspect_ratio, double
	max_aspect_ratio,int crop_top,int crop_left,int crop_bottom,int crop_right);

	/** Magnification can be set to smooth or blocky.  For platforms that
	 * enable smooth magnification (OpenGL), smoothing may look too blurry when
	 * magnifying by a large amount, so blocky magnification may actually look
	 * more charming.
	 * @param smooth_magnify smooth images when magnifying
	 */
	public void setSmoothing(boolean smooth_magnify);

	/** Call this to get focus. */
	public void requestGameFocus();

	/** Are we running as an applet or as an application? */
	public boolean isApplet();

	/** Are we running as a midlet? */
	public boolean isMidlet();

	/** Are we running with an OpenGL backend? */
	public boolean isOpenGL();

	/** Are we running on Android? */
	public boolean isAndroid();

	/** Get the virtual width in pixels (not the scaled screen width) */
	public int viewWidth();
	/** Get the virtual height in pixels (not the scaled screen height) */
	public int viewHeight();

	/** Get the number of tiles of view window in X direction */
	public int viewTilesX();
	/** Get the number of tiles of view window in Y direction */
	public int viewTilesY();

	/** Get view offset as it will be at the next frame draw, in case we are
	 * not inside a frame draw, or the view offset as it is, when we are. */
	public int viewXOfs();
	/** Get view offset as it will be at the next frame draw, in case we are
	 * not inside a frame draw, or the view offset as it is, when we are. */
	public int viewYOfs();

	//public int viewTileXOfs();
	//public int viewTileYOfs();

	/** Get the virtual width in pixels (not the scaled screen width) */
	public int pfWidth();
	/** Get the virtual height in pixels (not the scaled screen height) */
	public int pfHeight();

	/** Get the number of tiles in X direction */
	public int pfTilesX();
	/** Get the number of tiles in Y direction */
	public int pfTilesY();

	/** Is playfield X wrap enabled? */
	public boolean pfWrapX();
	/** Is playfield Y wrap enabled? */
	public boolean pfWrapY();

	/** Get the tile width in (virtual) pixels. */
	public int tileWidth();
	/** Get the tile height in (virtual) pixels. */
	public int tileHeight();

	/** Get the real display width on this device. */
	public int displayWidth();
	/** Get the real display height on this device. */
	public int displayHeight();


	/** Override to define your own initialisations before the engine
	* initialises.  This method is meant for doing initialisations after the
	* applet has been initialised (in case we're an applet) but before the
	* engine initialises.  This can be considered a replacement of the
	* regular constructor, making it independent of whether we're an applet
	* or application.  Typically you only need to call setCanvasSettings here,
	* and, optionally, setScalingPreferences().
	* This is the place where you can read applet parameters and initialise
	* accordingly.  In case you want to adapt to the real display dimensions,
	* you can get them using displayWidth/Height at this point.
	*/
	abstract public void initCanvas();

	/** Override to define your own initialisations after the engine
	* initialised.  This method is called by the game thread after
	* initEngine(), initEngineApplet(), or initEngineComponent() was called. */
	abstract public void initGame();

	/** Signal that the engine should start running. May be called by the web
	 * browser. */
	public void start();
	/** signal that the engine should stop running and wait. May be called by
	* the web browser.*/
	public void stop();

	/** Called when midlet is first initialised, or unpaused.  Midlet version
	 * of init() when called for the first time, and start() for subsequent
	 * calls. */
	public void startApp();

	/** Called by the application manager to pause app. Basically
	* the midlet version of stop(), behaves the same as stop(). */
	public void pauseApp();

	/** Called by the application manager to exit app. Basically the
	* midlet version of destroy(), behaves the same as destroy(). */
	public void destroyApp(boolean unconditional);

	/** True if engine is running, false if paused. */
	public boolean isRunning();

	/** Make engine call start() when a key is pressed.  This can be used to
	* determine a start criterion when halting the engine from within using
	* stop().
	* @param key  keycode to wake up on, -1=any key or mouse, 0=none */
	public void wakeUpOnKey(int key);

	/** Destroy function for deinitialising the engine properly.  This is
	* called by the applet viewer to dispose the applet.  Use exitEngine to
	* destroy the applet and exit the system.  */
	public void destroy();

	/** 	 
	* @return  frame rate in frames per second */
	public double getFrameRate();

	/** 	 
	* @return  max successive frames to skip */
	public double getFrameSkip();

	/**
	* @return true = video synced mode enabled
	*/
	public boolean getVideoSyncedUpdate();

	/** Change offset of playfield view.  The offset will become active 
	 * at the next frame draw.  If the view would be out of the
	 * playfield's bounds, the offset is corrected so that it is inside them.
	 * The offset of the parallax level 0 background image is set to the
	 * offset as well, the other levels remain unchanged.
	 * @param centered  center view on (xofs, yofs), topleft otherwise
	 */
	public void setViewOffset(int xofs,int yofs,boolean centered);

	/** Change (absolute) offset of BG image independently of view offset.
	 * Only supported by parallax scrolling platforms.  Note that parallax
	 * level 0 follows the view offset, so a call to this method to set
	 * level 0 should be done after calling setViewOffset.
	 * @param depth  depth level of image to set
	 * @param centered  center view on (xofs, yofs), topleft otherwise
	 */
	public void setBGImgOffset(int depth, double xofs, double yofs,
	boolean centered);

	/** Zoom/rotate view.  Can be used to create special effects, like
	 * speed-dependent zoom, explosion shake, etc.  Only works in OpenGL.  If
	 * you zoom out too far, parts that are beyond the borders of the
	 * defined view may get exposed, the appearance of which is undefined.
	 * This also happens when you rotate, so you will also need to zoom in 
	 * to ensure this
	 * does not happen.  Everything that is drawn relative to the playfield is
	 * zoomed/rotated, everything that is not is unaffected.  Game logic is
	 * unaffected.  
	 * <P> Mouse coordinates are inverse projected through
	 * the last set zoom/rotate setting, so that a playfield relative
	 * pixel drawn at the logical mouse coordinates coincides with the 
	 * physical position of the mouse pointer. If you don't want this, you can
	 * set zoom/rotate to (1,0), read the mouse position, then set zoom/rotate
	 * to the desired value.  
	 * <P> The zoom/rotate setting used for actual drawing
	 * is the last value set at the end of the doFrame phase.  Should not be
	 * called during the paintFrame phase.
	 * @param zoom  zoom factor, 1.0 is normal size
	 * @param rotate  angle in radians
	 */
	public void setViewZoomRotate(double zoom, double rotate);

	/** Set the playfield size to be any size larger or equal to the view
	 * size. 
	 * @param nrtilesx number of tiles, &gt;= viewTilesX()
	 * @param nrtilesy number of tiles, &gt;= viewTilesY() */
	public void setPFSize(int nrtilesx,int nrtilesy);

	/** Set playfield wraparound setting.  When wraparound is enabled, the
	 * playfield theoretically behaves as if it is infinitely long or high,
	 * with tiles and objects repeating periodically, with playfield size
	 * being the period.  Tile coordinates in a wrapped playfield are
	 * effectively modulo the playfield size.  Object coordinates are wrapped
	 * symmetrically to the view offset, that is, they are kept within
	 * -playfieldsize, +playfieldsize of the center of the view offset. This
	 * ensures that regular coordinate comparison and collision usually work
	 * as expected, without having to actually model the infinite repetition
	 * of the objects in the wrap direction.  The shiftx and shifty can be
	 * used to shift the object's wrap center by some pixels, to ensure they
	 * enter and leave the sides / top-bottom of the screen neatly when the
	 * playfield is not larger than the view.  For this case, use the
	 * following formula: ensure the playfield is slightly larger than the
	 * view (namely, one sprite length), and set the shift to sprite length/2.
	 */
	public void setPFWrap(boolean wrapx,boolean wrapy,int shiftx,int shifty);

	/** Set frame rate in frames per second, and maximum number of frames that
	 * may be skipped before displaying a frame again. Default is 35 frames
	 * per second, with a maxframeskip of 4.
	 * @param fps  frames per second, useful range 2...80
	 * @param maxframeskip  max successive frames to skip, useful range 0..10*/
	public void setFrameRate(double fps, double maxframeskip);

	/** Enable/disable video synced update (jogl only).
	 * This method has no effect on non-jogl platforms,
	 * where it is always disabled.  The game state update becomes synced with
	 * the screen refresh rate.  Frame rate is no longer fixed, but depends on
	 * the machine the game is running on.  Gamespeed is set at the beginning
	 * of each frame to compensate for this.  Gamespeed is 1 when actual frame
	 * rate == frame rate set with setFrameRate, less than 1 if frame rate
	 * greater than setFrameRate, more than 1 if frame rate less than
	 * setFrameRate.  There is a hard upper and lower bound for gamespeed, to
	 * ensure it does not attain wild values under rare conditions.  Lower
	 * bound for game speed is determined by a fixed upper bound for the
	 * expected screen refresh rate, 95 hz.  Upper bound for game speed is
	 * determined by the frameskip setting.  */
	public void setVideoSyncedUpdate(boolean value);

	/** Set game speed variable, default is 1.0.  Game speed affects certain
	 * parts of the game engine automatically to offload some of the work
	 * involved of adapting a game to different speeds.  These are the
	 * following: it is used as JGTimer tick increment, animation increment,
	 * JGObject expiry increment, and is used as multiplier for object
	 * x/yspeed, and for the default margins of is...Aligned and snapToGrid. */
	public void setGameSpeed(double speed);

	/** Get game speed variable.  This can be used if you have other stuff in
	 * your game that is affected by game speed, besides the standard game
	 * speed adaptation done by the engine. */
	public double getGameSpeed();

	/** Configure image rendering.  alpha_thresh is used to determine how a
	 * translucent image is converted to a bitmask image.  Alpha values below
	 * the threshold are set to 0, the others to 255. render_bg_col is used to
	 * render transparency for scaled images; it is the background colour that
	 * interpolations between transparent and non-transparent pixels are
	 * rendered to.  Currently, this has an effect in Jdk1.2 only. The default
	 * render_bg_col is null, meaning the global background colour is used.
	 * @param alpha_thresh  bitmask threshold, 0...255, default=128
	 * @param render_bg_col bg colour for render, null=use background colour
	 */
	public void setRenderSettings(int alpha_thresh,JGColor render_bg_col);

	/** Set margin used for testing if object should expire or suspend when
	* off-view or off-playfield. Default is 16,16. */
	public void setOffscreenMargin(int xmargin,int ymargin);

	/** Get offscreen X margin.
	* @see #setOffscreenMargin(int,int) */
	public int getOffscreenMarginX();
	/** Get offscreen Y margin.
	* @see #setOffscreenMargin(int,int) */
	public int getOffscreenMarginY();


	/** Set global background colour, which is displayed in borders, and behind
	* transparent tiles if no BGImage is defined. */
	public void setBGColor(JGColor bgcolor);

	/** Set global foreground colour, used for printing text and status
	 * messages.  It is also the default colour for painting */
	public void setFGColor(JGColor fgcolor);

	/** Set the (unscaled) message font, used for displaying status messages.
	* It is also the default font for painting.  */
	public void setMsgFont(JGFont msgfont);

	/** Set foreground and background colour, and message font in one go;
	* passing a null means ignore that argument. */
	public void setColorsFont(JGColor fgcolor,JGColor bgcolor,JGFont msgfont);

	/** Set parameters of outline surrounding text (for example, used to
	 *  increase contrast).
	 * @param thickness 0 = turn off outline */
	public void setTextOutline(int thickness,JGColor colour);

	/** Platform-independent cursor. */
	public static int NO_CURSOR        = -1;
	/** Platform-independent cursor. */
	public static int DEFAULT_CURSOR   = 0;
	/** Platform-independent cursor. */
	public static int CROSSHAIR_CURSOR = 1;
	/** Platform-independent cursor. */
	public static int HAND_CURSOR      = 2;
	/** Platform-independent cursor. */
	public static int WAIT_CURSOR      = 3;

	/** Set mouse cursor to a platform-independent standard cursor. */
	public void setMouseCursor(int cursor);

	/** Set mouse cursor, platform dependent. null (platform-independent)
	* means hide cursor. */
	public void setMouseCursor(Object cursor);

	/** Remove all JGTimers still ticking in the system. */
	public void removeAllTimers();

	/** Register a timer with the engine, don't call directly.  This is called
	 * automatically by the JGTimer constructor. */
	public void registerTimer(JGTimer timer);


	/** Set the game's main state on the next frame.  Methods with the names
	 * doFrame&lt;state&gt; and paintFrame&lt;state&gt; will be called in
	 * addition to doFrame() and paintFrame(). Before the next frame,
	 * start&lt;state&gt; is called once.  Note that setGameState may actually
	 * set a state that's already set, in which case start&lt;state&gt; is not
	 * called. Also, if the setGameState is superseded by another setGameState
	 * within the same frame, the first setGameState is ignored. */
	public void setGameState(String state);

	/** Add the given state to the game's existing state on the next frame.
	* The methods
	* doFrame&lt;state&gt; and paintFrame&lt;state&gt; will be called <i>in
	* addition to</i> the methods of any states already set. 
	* Before the next frame,
	* start&lt;state&gt; is called once.  Note that addGameState may actually
	* set a state that's already set, in which case start&lt;state&gt; is not
	* called.  
	*/
	public void addGameState(String state);

	/** Remove the given state from the game's existing state on the next
	* frame. */
	public void removeGameState(String state);

	/** Set the game's main state to none, on the next frame.
	* Only doFrame() and paintFrame() will be called each frame. */
	public void clearGameState();


	/** Check if game is in given state. */
	public boolean inGameState(String state);

	/** Check if game will be in given state the next frame. */
	public boolean inGameStateNextFrame(String state);

	/** Is called every frame.  Override to define frame action.  Default is
	 * do nothing. */
	public void doFrame();

	/** Is called when the engine's default screen painting is finished,
	* and custom painting actions may be carried out. Can be used to display
	* status information or special graphics.  Default is do nothing. */
	public void paintFrame();

	/** Get the graphics context for drawing on the buffer during a
	 * paintFrame().  Returns null when not inside paintFrame. */
	//public Graphics getBufferGraphics();

	/** Get scale factor of real screen width wrt virtual screen width */
	public double getXScaleFactor();
	/** Get scale factor of real screen height wrt virtual screen height */
	public double getYScaleFactor();
	/** Get minimum of the x and y scale factors */
	public double getMinScaleFactor();

	/* some convenience functions for drawing during repaint and paintFrame()*/

	/** Set current drawing colour. */
	public void setColor(JGColor col);

	/** Set current font, scale the font to screen size. */
	public void setFont(JGFont font);

	/** Set current font, scale the font to screen size. */
	//public void setFont(Graphics g,JGFont font);

	/** Set the line thickness */
	public void setStroke(double thickness);

	/** Set blend mode, for platforms that support blending.
	* Blend functions supported are based on the alpha of the object
	* being drawn (the drawing source).  Source and destination alpha
	* multiplier are specified separately. These are one of:
	* always 1 (encoded as 0), alpha (encoded as 1), and 1-alpha (encoded as
	* -1).   Default is (1,-1).
	* @param src_func  source multiply factor, 0=one, 1=alpha, -1 = one - alpha
	* @param dst_func  destination multiply factor, 0=one, 1=alpha, -1 = one - alpha
	*/
	public void setBlendMode(int src_func, int dst_func);

	/** Get height of given font in pixels. */
	public double getFontHeight(JGFont font);
	/** Get height of given font or current font in pixels. */
	//double getFontHeight(Graphics g,JGFont font);


	/* convert all coordinates to double?? */

	/** DrawLine combined with thickness/colour setting. The line is drawn
	 * relative to the playfield coordinates. */
	public void drawLine(double x1,double y1,double x2,double y2,
	double thickness, JGColor color);
	/** Draw a line with current thickness and colour. The line is drawn
	 * relative to the playfield coordinates. */
	public void drawLine(double x1,double y1,double x2,double y2);

	/** Draw a line with current thickness and colour.
	* @param pf_relative coordinates are relative to playfield, otherwise view*/
	public void drawLine(double x1,double y1,double x2,double y2,
	boolean pf_relative);

	/** Draw convex polygon.  For filled polygons, it is possible to draw a
	 * colour gradient, namely a "fan" of colours (specified by col),
	 * spreading from the first point to each successive point in the polygon.
	 * For line polygons, a gradient is drawn between successive line
	 * segments.
	 * On non-OpenGL platforms, the fill gradient is drawn as a fan of plain
	 * colours, with colour 0 and 1 ignored, the line gradient as plain
	 * coloured lines.
	 *
	* @param x  x coordinates of the points
	* @param y  y coordinates of the points
	* @param col  colour of each point, null means use default colour
	* @param len number of points
	* @param pf_relative coordinates are relative to playfield, otherwise view
	*/
	public void drawPolygon(double [] x,double [] y, JGColor [] col, int len,
	boolean filled, boolean pf_relative);

	/** Set colour/thickness and draw rectangle.  Coordinates are relative to
	* playfield.
	* @param centered indicates (x,y) is center instead of topleft.
	*/
	public void drawRect(double x,double y,double width,double height, boolean filled,
	boolean centered, double thickness, JGColor color);

	/** Draw rectangle in default colour and thickness.  Coordinates are
	* relative to playfield.
	* @param centered indicates (x,y) is center instead of topleft.
	*/
	public void drawRect(double x,double y,double width,double height, boolean filled,
	boolean centered);

	/** Draw rectangle in default colour and thickness.
	* @param centered indicates (x,y) is center instead of topleft.
	* @param pf_relative coordinates are relative to playfield, otherwise view
	*/
	public void drawRect(double x,double y,double width,double height, boolean filled,
	boolean centered, boolean pf_relative);

	/** Draw shaded rectangle.  On non-opengl platforms, rectangle is drawn in
	 * default colour.
	 * @param shadecol colors topleft,topright,botright,botleft corners
	*/
	public void drawRect(double x,double y,double width,double height,
	boolean filled, boolean centered,boolean pf_relative,
	JGColor [] shadecol);

	/** Draw shaded or patterned rectangle.  On non-opengl platforms,
	* rectangle is drawn in default colour.
	* @param shadecol colors topleft,topright,botright,botleft corners
	*/
	public void drawRect(double x,double y,double width,double height,
	boolean filled, boolean centered,boolean pf_relative,
	JGColor [] shadecol,String tileimage);

	/** Set thickness/colour and draw oval.  Coordinates are relative to
	* playfield.
	* @param centered indicates (x,y) is center instead of topleft.
	*/
	public void drawOval(double x,double y,double width,double height, boolean filled,
	boolean centered, double thickness, JGColor color);

	/** Draw oval with default thickness and colour. Coordinates are relative
	* to playfield.
	* @param centered indicates (x,y) is center instead of topleft.
	*/
	public void drawOval(double x,double y, double width,double height,boolean filled,
	boolean centered);

	/** Draw oval with default thickness and colour. 
	* @param centered indicates (x,y) is center instead of topleft.
	* @param pf_relative coordinates are relative to playfield, otherwise view
	*/
	public void drawOval(double x,double y, double width,double height,boolean filled,
	boolean centered, boolean pf_relative);

	/** Draw image with given ID. Coordinates are relative to playfield. */
	public void drawImage(double x,double y,String imgname);

	/** Draw image with given ID. 
	* @param pf_relative coordinates are relative to playfield, otherwise view*/
	public void drawImage(double x,double y,String imgname,boolean pf_relative);

	/** Extended version of drawImage for OpenGL or Android.
	 * On platforms without support for accelerated blending, rotation,
	 * scaling, this call is equivalent to drawImage(x,y,imgname,pf_relative).
	 *
	 * rotation and scaling are centered around the image center.
	 *
	 * @param blend_col colour to blend with image, null=(alpha,alpha,alpha)
	 * @param alpha  alpha (blending) value, 0=transparent, 1=opaque
	 * @param rot  rotation of object in degrees (radians)
	 * @param scale  scaling of object (1 = normal size).
	 */
	public void drawImage(double x,double y,String imgname, JGColor blend_col,
	double alpha, double rot, double scale, boolean pf_relative);


	/** Draw image with given ID, new version.
	* Coordinates are relative to playfield. */
	public void drawImage(String imgname,double x,double y);

	/** Draw image with given ID, new version. 
	* @param pf_relative coordinates are relative to playfield, otherwise view*/
	public void drawImage(String imgname,double x,double y,boolean pf_relative);

	/** Extended version of drawImage for OpenGL or Android, new version.
	 * On platforms without support for accelerated blending, rotation,
	 * scaling, this call is equivalent to drawImage(x,y,imgname,pf_relative).
	 *
	 * rotation and scaling are centered around the image center.
	 *
	 * @param blend_col colour to blend with image, null=(alpha,alpha,alpha)
	 * @param alpha  alpha (blending) value, 0=transparent, 1=opaque
	 * @param rot  rotation of object in degrees (radians)
	 * @param scale  scaling of object (1 = normal size).
	 */
	public void drawImage(String imgname, double x,double y,
	boolean pf_relative,JGColor blend_col,
	double alpha, double rot, double scale);


	/** Draws string so that (x,y) is the topleft coordinate (align=-1), the
	 * top middle coordinate (align=0), or the top right coordinate (align=1).
	 * Use given font and colour; filling in null for either means ignore.
	 * Unlike the other draw functions, for strings, coordinates are relative
	 * to view by default.  An outline is drawn around the text when defined
	 * by setTextOutline.
	 * @param align  text alignment, -1=left, 0=center, 1=right */
	public void drawString(String str, double x, double y, int align,
	JGFont font, JGColor color);

	/** Draws string so that (x,y) is the topleft coordinate (align=-1), the
	 * top middle coordinate (align=0), or the top right coordinate (align=1).
	 * Use current font and colour.
	 * Unlike the other draw functions, for strings, coordinates are relative
	 * to view by default. An outline is drawn around the text when defined
	 * by setTextOutline.
	 * @param align  text alignment, -1=left, 0=center, 1=right */
	public void drawString(String str, double x, double y, int align);

	/** Draws string so that (x,y) is the topleft coordinate (align=-1), the
	* top middle coordinate (align=0), or the top right coordinate (align=1).
	* Use current font and colour. An outline is drawn around the text when
	* defined by setTextOutline.
	* @param align  text alignment, -1=left, 0=center, 1=right
	* @param pf_relative coordinates are relative to playfield, otherwise view*/
	public void drawString(String str, double x, double y, int align,
	boolean pf_relative);

	/** Internal function for writing on both buffer and screen.  Coordinates
	 * are always relative to view. */
	//void drawString(Graphics g, String str, double x, double y, int align,
	//boolean pf_relative);

	/** Draws a single line of text using an image map as font;
	* text alignment is same as drawString.  Typically, an image font only
	* defines the ASCII character range 32-96.  In this case, set char_offset
	* to 32, and use only the uppercase letters.  Coordinates are relative to
	* view.
	* @param align  text alignment, -1=left, 0=center, 1=right
	* @param imgmap  name of image map
	* @param char_offset  ASCII code of first image of image map
	* @param spacing  number of pixels to skip between letters
	*/
	public void drawImageString(String string, double x, double y, int align,
	String imgmap, int char_offset, int spacing);

	/** Draws a single line of text using an image map as font;
	* text alignment is same as drawString.  Typically, an image font only
	* defines the ASCII character range 32-96.  In this case, set char_offset
	* to 32, and use only the uppercase letters.
	* @param align  text alignment, -1=left, 0=center, 1=right
	* @param imgmap  name of image map
	* @param char_offset  ASCII code of first image of image map
	* @param spacing  number of pixels to skip between letters
	* @param pf_relative coordinates are relative to playfield, otherwise view
	*/
	public void drawImageString(String string, double x, double y, int align,
	String imgmap, int char_offset, int spacing,boolean pf_relative);

	/** Get current mouse position in logical coordinates, inverse projected
	 * through last set view zoom/rotate setting.  */
	public JGPoint getMousePos();
	/** Get current mouse X position */
	public int getMouseX();
	/** Get current mouse Y position */
	public int getMouseY();

	/** Get state of button.
	* @param nr  1=button 1 ... 3 = button 3
	* @return true=pressed, false=released */
	public boolean getMouseButton(int nr);
	/** Set state of button to released.
	* @param nr  1=button 1 ... 3 = button 3 */
	public void clearMouseButton(int nr);
	/** Set state of button to pressed.
	* @param nr  1=button 1 ... 3 = button 3 */
	public void setMouseButton(int nr);
	/** Check if mouse is inside game window */
	public boolean getMouseInside();

	/** Get the key status of the given key. */
	public boolean getKey(int key);
	/** Set the key status of a key to released. */
	public void clearKey(int key);
	/** Set the key status of a key to pressed. */
	public void setKey(int key);

	/** Get the keycode of the key that was pressed last, 0=none. */
	public int getLastKey();
	/** Get the keycode of the key that was pressed last, 0=none. */
	public char getLastKeyChar();
	/** Clear the lastkey status. */
	public void clearLastKey();

	/** Get a printable string describing the key. */
	public String getKeyDesc(int key);

	/** Obtain key code from printable string describing the key, the inverse
	 * of getKeyDesc. The string is trimmed and lowercased. */
	public int getKeyCode(String keydesc);

	/** returns true if device has accelerometer (currently only android) */
	public boolean hasAccelerometer();

	/** get accelerometer vector X coordinate */
	public double getAccelX();
	/** get accelerometer vector Y coordinate */
	public double getAccelY();
	/** get accelerometer vector Z coordinate (positive = towards user) */
	public double getAccelZ();

	/** get double[3] vector representing acceleration */
	public double [] getAccelVec();

	/*====== animation ======*/

	/** Define new animation sequence. Speed must be &gt;= 0.
	* @param id  the name by which the animation is known
	* @param frames  an array of image names that should be played in sequence
	* @param speed the sequence speed: the number of animation steps per frame
	*/
	public void defineAnimation (String id,
	String [] frames, double speed);

	/** Define new animation sequence. Speed must be &gt;= 0.
	* @param id  the name by which the animation is known
	* @param frames  an array of image names that should be played in sequence
	* @param speed the sequence speed: the number of animation steps per frame
	* @param pingpong  true=play the images in forward order, then in reverse
	*/
	public void defineAnimation (String id,
	String [] frames, double speed, boolean pingpong);

	/** Get animation definition, don't call directly.  This is used by
	 * JGObjects to get animations. */
	public Animation getAnimation(String id);


	/*== file op ==*/

	/** Returns path to writable location for a file with the given name.
	 * Basically it uses [user.home] / .jgame / [filename], with "/" being the
	 * system path separator.  In case [user.home] / .jgame does not exist, it
	 * is created.  In case .jgame is not a directory, null is returned.  In
	 * case the file does not exist yet, an empty file is created.
	 * @return path to writable file, or null if not possible
	 */
	public String getConfigPath(String filename);

	/** Execute or go to URL (action depends on file type). 
	* @return  0 if fail; 1 if success; -1 if the status is unknown
	*/
	public int invokeUrl(String url,String target);

	/* computation */

	/** A Boolean AND shorthand to use for collision;
	* returns (value&amp;mask) != 0. */
	public boolean and(int value, int mask);

	/** A floating-point random number between min and max */
	public double random(double min, double max);

	/** Generates discrete random number between min and max inclusive, with
	 * steps of interval.  Epsilon is added to max to ensure there are no
	 * rounding error problems with the interval.  So, random(0.0, 4.2, 2.1)
	 * generates either 0.0, 2.1, or 4.2 with uniform probabilities.  If max
	 * is halfway between interval steps, max is treated as exclusive.  So,
	 * random(0.0,5.0,2.1) generates 0.0, 2.1, 4.2 with uniform probabilities.
	 * If you need integer ranges, be sure to use the integer version to avoid
	 * rounding problems. */
	public double random(double min, double max, double interval);

	/** Generates discrete random number between min and max inclusive, with
	 * steps of interval, integer version.  If max is halfway between two
	 * interval steps, it is treated as exclusive. */
	public int random(int min, int max, int interval);

	/** Replacement for Math.atan2 for the sake of MIDP portability.  The JRE
	 * implementation simply uses Math.atan2, for MIDP a short and fast
	 * Math.atan2 replacement is used, with average numerical error less than
	 * 0.001 radians, maximum error 0.005 radians. */
	public double atan2(double y,double x);


	/** Get tile index of the tile the coordinate is on.*/
	public JGPoint getTileIndex(double x, double y);

	/** Get pixel coordinate corresponding to the top left of the tile at the
	* given index */
	public JGPoint getTileCoord(int tilex, int tiley);

	/** Get pixel coordinate corresponding to the top left of the tile at the
	* given index */
	public JGPoint getTileCoord(JGPoint tileidx);

	/** Snap to grid, double version. Epsilon is added to the gridsnap value,
	* so that isXAligned(x,margin) always implies that snapToGridX(x,margin)
	* will snap.
	* @param x  position to snap
	* @param gridsnapx  snap margin, 0.0 means no snap   */
	public double snapToGridX(double x, double gridsnapx);

	/** Snap to grid, double version. Epsilon is added to the gridsnap value,
	* so that isYAligned(y,margin) always implies that snapToGridY(y,margin)
	* will snap.
	* @param y  position to snap
	* @param gridsnapy  snap margin, 0.0 means no snap   */
	public double snapToGridY(double y, double gridsnapy);

	/** Snap p to grid in case p is close enough to the grid lines. Note: this
	 * function only handles integers so it should not be used to snap an
	 * object position.  */
	public void snapToGrid(JGPoint p,int gridsnapx,int gridsnapy);

	/** Returns true if x falls within margin of the tile snap grid. Epsilon
	 * is added to the margin, so that isXAligned(1.0000, 1.0000)
	 * always returns true. */
	public boolean isXAligned(double x,double margin);

	/** Returns true if y falls within margin of the tile snap grid. Epsilon
	 * is added to the margin, so that isYAligned(1.0000, 1.0000)
	 * always returns true. */
	public boolean isYAligned(double y,double margin);

	/** Returns the difference between position and the closest tile-aligned
	 * position. */
	public double getXAlignOfs(double x);

	/** Returns the difference between position and the closest tile-aligned
	 * position. */
	public double getYAlignOfs(double y);

	// XXX please test these two methods

	/** Calculates length of the shortest path between x1 and x2,
	* with x1, x2 being playfield coordinates,
	* taking into account the wrap setting. */
	public double getXDist(double x1, double x2);

	/** Calculates length of the shortest path between y1 and y2,
	* with y1, y2 being playfield coordinates,
	* taking into account the wrap setting. */
	public double getYDist(double y1, double y2);



	/*===== audio =====*/


	/** Enable audio, restart any audio loops. */
	public void enableAudio();

	/** Disable audio, stop all currently playing audio.  Audio commands will
	* be ignored, except that audio loops (music, ambient sounds) are
	* remembered and will be restarted once audio is enabled again. */
	public void disableAudio();

	/** Associate given clipid with a filename.  Files are loaded from the
	* resource path.  Java 1.2+ supports at least: midi and wav files. */
	public void defineAudioClip(String clipid,String filename);

	/** Returns the audioclip that was last played, null if audio was stopped
	* with stopAudio.	Note the clip does not actually have to be playing; it
	* might have finished playing already. */
	public String lastPlayedAudio(String channel);

	/** Play audio clip on unnamed channel, which means it will not replace
	* another clip, and cannot be stopped.  The clip is not looped.  When
	* this method is called multiple times with the same sample within the
	* same frame, it is played only once. */
	public void playAudio(String clipid);

	/** Play clip on channel with given name.  Will replace any other clip
	 * already playing on the channel.  Will restart if the clip is already
	 * playing <i>and</i> either this call or the already playing one are
	 * <i>not</i> specified as looping.  If both are looping, the looped sound
	 * will continue without restarting.  If you want the looping sound to be
	 * restarted, call stopAudio first.  Note the channel "music" is reserved
	 * for enabling/disabling music separately in future versions.  */
	public void playAudio(String channel,String clipid,boolean loop);

	/** Stop one audio channel. */
	public void stopAudio(String channel);

	/** Stop all audio channels. */
	public void stopAudio();


	/*===== store =====*/

	/** Writes integer to store under given ID */
	public void storeWriteInt(String id,int value);

	/** Writes double to store under given ID */
	public void storeWriteDouble(String id,double value);

	/** Writes string to store under given ID */
	public void storeWriteString(String id,String value);

	/** Remove record if it exists */
	public void storeRemove(String id);

	/** Checks if item exists in store */
	public boolean storeExists(String id);

	/** Reads int from store, use undef if ID not found */
	public int storeReadInt(String id,int undef);

	/** Reads double from store, use undef if ID not found */
	public double storeReadDouble(String id,double undef);

	/** Reads String from store, use undef if ID not found */
	public String storeReadString(String id,String undef);

	/*====== options ======*/

	/** Adds title to be displayed above subsequent items.  Default title of
	 * initial items is "Preferences".  Call this before defining any items to
	 * override the default title. */
	public void optsAddTitle(String title);

	/** adds (int or float) number that can be configured with a slider.
	* Type is double if decimals!=0, int otherwise.
	* @param decimals number is int if 0
	**/
	public void optsAddNumber(String varname,String title,String desc,
	int decimals, double lower,double upper,double step, double initial);

	/** Adds boolean that can be configured with a checkbox. Actual type is
	 * int (0 or 1) */
	public void optsAddBoolean(String varname,String title,String desc,
	boolean initial);

	/** Adds enum that can be configured with radio buttons. Actual type is
	 * int, 0 for first item, 1 for second item, etc. */
	public void optsAddEnum(String varname,String title,String desc,
	String [] values, int initial);

	/** Adds "key" option that can be configured by selecting a key or button
	* on the device.  Actual type is int. */
	public void optsAddKey(String varname,String title,String desc,int initial);

	/** Adds String that can be configured by typing text. */
	public void optsAddString(String varname,String title,String desc,
	int maxlen, boolean isPassword, String initial);

	/** Clear all previous option definitions. */
	public void optsClear();

}


