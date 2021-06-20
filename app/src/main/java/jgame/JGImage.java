package jgame;
/** Image functionality */
public interface JGImage {

	/* static in spirit*/

	/** Load image from resource path (using getResource).  Note that GIFs are
	 * loaded as _translucent_ indexed images.   Images are cached: loading
	 * an image with the same name twice will get the cached image the second
	 * time.  If you want to remove an image from the cache, use purgeImage.
	* Throws JGError when there was an error. */
	public JGImage loadImage(String imgfile);

	/** Behaves like loadImage(String).  Returns null if there was an error. */
	// midp has no url
	//public JGImage loadImage(URL imgurl);

	/** Purge image with the given resourcename from the cache. */
	public void purgeImage(String imgfile);


	/* object-related methods */

	/** Get image size.  The object returned may be a reference to an internal
	 * variable, so do not change it! */
	public JGPoint getSize();

	/** True means the image may have some transparent pixels below the given
	 * alpha threshold, false means image is completely opaque.
	 * Implementations may choose to return false when they are not sure, but
	 * should return true whenever applicable to enable maximum performance
	 * benefits.  Current implementations actually check the alpha channel
	 * pixel for pixel */
	public boolean isOpaque(int alpha_thresh);

	/** for angle, only increments of 90 are allowed */
	public JGImage rotate(int angle);

	/** Image is rotated by rot (radians). Resulting image is square with
	* dimension max(width, height, 0.75*(width+height))*/
	public JGImage rotateAny(double angle);

	public JGImage flip(boolean horiz,boolean vert);

	/** Returns a smoothly scaled image using getScaledInstance.  This method
	 * has interesting behaviour.  The scaled image retains its type
	 * (indexed/rgb and bitmask/translucent), and the algorithm tries to scale
	 * smoothly within these constraints.  For indexed, interpolated pixels
	 * are rounded to the existing indexed colours.  For bitmask, the
	 * behaviour depends on the platform.  On WinXP/J1.2 I found that the
	 * colour _behind_ each transparent pixel is used to interpolate between
	 * nontransparent and transparent pixels.  On BSD/J1.4 I found that the
	 * colours of transparent pixels are never used, and only the
	 * nontransparent pixels are used when interpolating a region with mixed
	 * transparent/nontransparent pixels.
	 */
	public JGImage scale(int width, int height);

	//public void ensureLoaded() throws Exception;

	public JGImage crop(int x,int y, int width,int height);

	/** Turn a (possibly) translucent or indexed image into a
	* display-compatible bitmask image using the given alpha threshold and
	* render-to-background colour, or to display-compatible translucent image.
	* In bitmask mode, the alpha values in the
	* image are set to either 0 (below threshold) or 255 (above threshold).
	* The render-to-background colour bg_col is used to determine how the
	* pixels overlapping transparent pixels should be rendered.  The fast
	* algorithm just sets the colour behind the transparent pixels in the
	* image (for bitmask source images); the slow algorithm actually
	* renders the image to a background of bg_col (for translucent sources).
	*
	* @param thresh alpha threshold between 0 and 255
	* @param fast use fast algorithm (only set bg_col behind transp. pixels)
	* @param bitmask true=make bitmask, false=make translucent */
	public JGImage toDisplayCompatible(int thresh,JGColor bg_col,
	boolean fast, boolean bitmask);

	/** Create empty image with given alpha mode that should be efficient on
	* this display */
	//public BufferedImage createCompatibleImage(int width,int height,
	//int transparency);

	// the following is ugly version-conditional execution.  What we really
	// want is to have a 1.4 and a <1.4 version of the following two methods.
	// Drawbacks are now that IMAGE_INCOMPATIBLE is coded as a hard number and 
	// applets cannot do reflection on volatile images for some reason and
	// hence do not have acceleration.


}
