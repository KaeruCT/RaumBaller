package jgame;

/** A generic font specification for fonts on the different
* platforms.  It is based on the JRE platform, other platforms should do their
* best to translate it to something equivalent or use sensible defaults if
* there is nothing comparable. */
public class JGFont {

	public static final int PLAIN=0;
	public static final int BOLD=1;
	public static final int ITALIC=2;

	/** The font name can be a logical font name or a font face name. A
	 * logical name must be either: Dialog, DialogInput, Monospaced, Serif, or
	 * SansSerif. If name is null, the name of the new Font is set to the name
	 * "Default". */
	public String name="Default";

	/** The style is an integer bitmask that may be PLAIN, or a bitwise union
	 * of BOLD and/or ITALIC (for example, ITALIC or BOLD|ITALIC). If the
	 * style argument does not conform to one of the expected integer bitmasks
	 * then the style is set to PLAIN. */
	public int style=PLAIN;

	/** Font size is effectively pixel height. */
	public double size;

	/** Optional object that represents the font on a particular platform.*/
	public Object impl=null;

	/** Creates a new Font from the specified name, style and point size.
	* This is a generic font specification for fonts on the different
	* platforms.  
	* @param name the font name
	* @param style - the style constant for the Font
	* @param size - the point size of the Font
	*/
	public JGFont(String name, int style, double size) {
		if (name!=null) this.name=name;
		this.style=style;
		this.size=size;
	}

	public int getSize() { return (int)size; }
	public double getSize2D() { return size; }
	public int getStyle() { return style; }
}
