package jgame.platform;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import jgame.JGColor;
import jgame.JGFont;
import jgame.JGImage;
import jgame.JGObject;
import jgame.JGPoint;
import jgame.JGRectangle;
import jgame.JGTimer;
import jgame.impl.Animation;
import jgame.impl.EngineLogic;
import jgame.impl.ImageMap;
import jgame.impl.JGEngineInterface;
import jgame.impl.JGameError;

/** Browser Canvas/WebAudio implementation of the JGame platform layer. */
public abstract class JGEngine implements JGEngineInterface {
    static final String STORE_PREFIX = "JG_";

    private final int[] xpos = new int[3];
    private final int[] ypos = new int[3];
    public double progress_bar = 0.0;
    BrowserImage imageutil = new BrowserImage();
    EngineLogic el = new EngineLogic(imageutil, true, true);

    JSObject canvas;
    JSObject context;
    JSObject background;
    JSObject backgroundContext;
    JSObject currentContext;

    String fillStyle = "rgba(255,255,255,1)";
    String strokeStyle = "rgba(255,255,255,1)";
    double lineWidth = 1;
    String fontStyle = "16px sans-serif";

    boolean running = true;
    boolean is_initialised = false;
    double lastFrameTimestamp = -1;
    double frameAccumulator = 0;
    boolean[] keymap = new boolean[256 + 3];
    int lastkey = 0;
    char lastkeychar = 0;
    int wakeup_key = 0;
    String progress_message = "Loading files ...";
    String author_message = "JGame " + JGameVersionString;
    JGPoint mousepos = new JGPoint(0, 0);
    double mouseposd_x = 0, mouseposd_y = 0;
    boolean[] mousebutton = new boolean[]{false, false, false, false};
    boolean mouseinside = false;
    double[] accelvec = new double[]{0, 0, 1};
    boolean audioenabled = true;
    Hashtable audioclips = new Hashtable();
    Hashtable channeltoclip = new Hashtable();
    Hashtable store = new Hashtable();
    StringBuffer audioTrace = new StringBuffer();

    String[] stateprefixes = new String[]{"start", "doFrame", "paintFrame"};

    @JSFunctor
    interface FrameHandler extends JSObject {
        void run(double timestamp);
    }

    @JSFunctor
    interface PointerHandler extends JSObject {
        void run(double x, double y, boolean down, boolean inside);
    }

    @JSFunctor
    interface KeyHandler extends JSObject {
        void run(int key, boolean state, int ch);
    }

    String[] statesuffixes = new String[]{
            "Loader", "Title", "SelectLevel", "Highscores", "InGame", "StartLevel",
            "StartGame", "LevelDone", "LifeLost", "GameOver", "EnterHighscore", "Paused"
    };

    public static String getKeyDescStatic(int key) {
        if (key == 32) return "#";
        if (key == 0) return "(none)";
        if (key == KeyEnter || key == KeyStar || key == '*') return "*";
        if (key == KeyPound || key == '#') return "#";
        if (key == KeyUp) return "cursor up";
        if (key == KeyDown) return "cursor down";
        if (key == KeyLeft) return "cursor left";
        if (key == KeyRight) return "cursor right";
        if (key == KeyShift) return "fire";
        if (key >= 33 && key <= 95) return new String(new char[]{(char) key});
        return "keycode " + key;
    }

    public static int getKeyCodeStatic(String keydesc) {
        keydesc = keydesc.toLowerCase().trim();
        if (keydesc.equals("(none)")) return 0;
        if (keydesc.equals("cursor up")) return KeyUp;
        if (keydesc.equals("cursor down")) return KeyDown;
        if (keydesc.equals("cursor left")) return KeyLeft;
        if (keydesc.equals("cursor right")) return KeyRight;
        if (keydesc.equals("fire")) return KeyFire;
        if (keydesc.equals("star")) return '*';
        if (keydesc.equals("pound")) return '#';
        if (keydesc.startsWith("keycode")) return Integer.parseInt(keydesc.substring(7));
        if (keydesc.length() == 1) return keydesc.charAt(0);
        return 0;
    }

    public abstract void initCanvas();
    public abstract void initGame();

    static boolean isHeadless() {
        return Boolean.getBoolean("raumballer.headless");
    }

    private void applyConfiguredRandomSeed() {
        if (isHeadless()) {
            String seed = System.getProperty("raumballer.seed");
            if (seed != null && seed.length() > 0) el.setRandomSeed(Long.parseLong(seed));
            return;
        }
        double seed = getConfiguredSeed();
        if (!Double.isNaN(seed)) el.setRandomSeed((long) seed);
    }

    public void initEngineApplet() {
        initEngine(0, 0);
    }

    public void initEngineComponent(int width, int height) {
        initEngine(width, height);
    }

    public void initEngine(int width, int height) {
        if (isHeadless()) {
            initHeadlessEngine(width, height);
            return;
        }
        canvas = getCanvas();
        if (canvas == null) {
            canvas = createCanvasElement();
        }
        context = get2dContext(canvas);
        setImageSmoothing(context, false);
        applyConfiguredRandomSeed();
        installInputHandlers(canvas, new PointerHandler() {
            public void run(double x, double y, boolean down, boolean inside) {
                setPointer(x, y, down, inside);
            }
        }, new KeyHandler() {
            public void run(int key, boolean state, int ch) {
                setBrowserKey(key, state, ch);
            }
        });
        init();
        try {
            initGame();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new JGameError("Exception during initGame(): " + e);
        }
        is_initialised = true;
        if (!isBrowserTestMode()) {
            startAnimationLoop(new FrameHandler() {
                public void run(double timestamp) {
                    animationFrame(timestamp);
                }
            });
        }
    }

    private void initHeadlessEngine(int width, int height) {
        applyConfiguredRandomSeed();
        initCanvas();
        if (!el.view_initialised) exitEngine("Canvas settings not initialised, use setCanvasSettings().");
        el.winwidth = viewWidth();
        el.winheight = viewHeight();
        el.initPF();
        clearKeymap();
        if (!JGObject.setEngine(this)) exitEngine("Another JGEngine is already running!");
        el.is_inited = true;
        try {
            initGame();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new JGameError("Exception during initGame(): " + e);
        }
        is_initialised = true;
    }

    public void stepFrameForTest() {
        synchronized (el.objects) {
            doFrameAll();
            el.updateViewOffset();
        }
    }

    public void paintFrameForTest() {
        if (!isHeadless() && context != null) {
            synchronized (el.objects) {
                drawAll();
            }
        }
    }

    private void init() {
        boolean do_full_init = !el.is_inited;
        el.winwidth = getCanvasWidth(canvas);
        el.winheight = getCanvasHeight(canvas);
        if (do_full_init) {
            initCanvas();
            if (!el.view_initialised) exitEngine("Canvas settings not initialised, use setCanvasSettings().");
            el.winwidth = viewWidth();
            el.winheight = viewHeight();
            setCanvasSize(canvas, el.winwidth, el.winheight);
        }
        el.initPF();
        if (do_full_init) {
            clearKeymap();
            el.msg_font = new JGFont("Helvetica", 0, (int) (25.0 / (640.0 / (el.tilex * el.nrtilesx))));
            if (!JGObject.setEngine(this)) exitEngine("Another JGEngine is already running!");
            el.is_inited = true;
        }
        background = BrowserImage.createCanvas(el.width + 3 * el.scaledtilex, el.height + 3 * el.scaledtiley);
        backgroundContext = get2dContext(background);
        setImageSmoothing(backgroundContext, false);
        el.invalidateBGTiles();
    }

    private void clearKeymap() {
        for (int i = 0; i < keymap.length; i++) keymap[i] = false;
    }

    public void animationFrame(double timestamp) {
        if (el.is_exited) return;
        if (!running) {
            lastFrameTimestamp = timestamp;
            return;
        }
        if (lastFrameTimestamp < 0) lastFrameTimestamp = timestamp;
        frameAccumulator += timestamp - lastFrameTimestamp;
        lastFrameTimestamp = timestamp;

        double step = 1000.0 / el.fps;
        int updates = 0;
        int maxUpdates = Math.max(1, (int) el.maxframeskip + 1);
        synchronized (el.objects) {
            while (frameAccumulator >= step && updates < maxUpdates) {
                doFrameAll();
                el.updateViewOffset();
                frameAccumulator -= step;
                updates++;
            }
            if (updates == maxUpdates) frameAccumulator = 0;
            drawAll();
        }
    }

    private void doFrameAll() {
        el.flushRemoveList();
        el.flushAddList();
        el.tickTimers();
        el.flushRemoveList();
        el.flushAddList();
        el.gamestate.removeAllElements();
        for (int i = 0; i < el.gamestate_nextframe.size(); i++) {
            el.gamestate.addElement(el.gamestate_nextframe.elementAt(i));
        }
        invokeGameStateMethods("start", el.gamestate_new);
        el.gamestate_new.removeAllElements();
        el.flushRemoveList();
        el.flushAddList();
        try {
            doFrame();
        } catch (JGameError ex) {
            ex.printStackTrace();
            exitEngine(dbgExceptionToString(ex));
        } catch (Exception ex) {
            dbgShowException("MAIN", ex);
        }
        invokeGameStateMethods("doFrame", el.gamestate);
        el.frameFinished();
    }

    private void drawAll() {
        clearCanvas(context, cssColor(el.bg_color));
        el.repaintBG(this);
        currentContext = context;
        int tilexshift = el.moduloFloor(el.tilexofs + 1, el.viewnrtilesx + 3);
        int tileyshift = el.moduloFloor(el.tileyofs + 1, el.viewnrtilesy + 3);
        int sx1 = tilexshift + 1;
        int sy1 = tileyshift + 1;
        int sx2 = el.viewnrtilesx + 3;
        int sy2 = el.viewnrtilesy + 3;
        if (sx2 - sx1 > el.viewnrtilesx) sx2 = sx1 + el.viewnrtilesx;
        if (sy2 - sy1 > el.viewnrtilesy) sy2 = sy1 + el.viewnrtilesy;
        int bufmidx = sx2 - sx1;
        int bufmidy = sy2 - sy1;
        copyBGToBuf(context, sx1, sy1, sx2, sy2, 0, 0);
        copyBGToBuf(context, 0, 0, tilexshift - 1, tileyshift - 1, bufmidx, bufmidy);
        sx1 = 0; sy1 = tileyshift + 1; sx2 = tilexshift - 1; sy2 = el.viewnrtilesy + 3;
        if (sy2 - sy1 > el.viewnrtilesy) sy2 = sy1 + el.viewnrtilesy;
        copyBGToBuf(context, sx1, sy1, sx2, sy2, bufmidx, 0);
        sx1 = tilexshift + 1; sy1 = 0; sx2 = el.viewnrtilesx + 3; sy2 = tileyshift - 1;
        if (sx2 - sx1 > el.viewnrtilesx) sx2 = sx1 + el.viewnrtilesx;
        copyBGToBuf(context, sx1, sy1, sx2, sy2, 0, bufmidy);
        for (int i = 0; i < el.objects.size; i++) {
            drawObject((JGObject) el.objects.values[i]);
        }
        paintFrameInternal();
        currentContext = null;
    }

    private void drawObject(JGObject o) {
        if (!o.is_suspended) {
            drawImage((int) o.x, (int) o.y, o.getImageName(), true);
            try {
                o.paint();
            } catch (JGameError ex) {
                ex.printStackTrace();
                exitEngine(dbgExceptionToString(ex));
            } catch (Exception e) {
                dbgShowException(o.getName(), e);
            }
        }
    }

    private void paintFrameInternal() {
        setColor(el.fg_color);
        setFont(el.msg_font);
        try {
            paintFrame();
        } catch (JGameError ex) {
            ex.printStackTrace();
            exitEngine(dbgExceptionToString(ex));
        } catch (Exception ex) {
            dbgShowException("MAIN", ex);
        }
        invokeGameStateMethods("paintFrame", el.gamestate);
    }

    void copyBGToBuf(JSObject target, int sx1, int sy1, int sx2, int sy2, int dx1, int dy1) {
        if (sx2 <= sx1 || sy2 <= sy1) return;
        int barrelx = el.scaleXPos(el.moduloFloor(el.xofs, el.tilex), false);
        int barrely = el.scaleYPos(el.moduloFloor(el.yofs, el.tiley), false);
        int barreldx = (sx1 == 0) ? barrelx : 0;
        int barreldy = (sy1 == 0) ? barrely : 0;
        barrelx = (sx1 == 0) ? 0 : barrelx;
        barrely = (sy1 == 0) ? 0 : barrely;
        int sx1e = barrelx + sx1 * el.scaledtilex;
        int sy1e = barrely + sy1 * el.scaledtiley;
        int sx2e = barrelx + sx2 * el.scaledtilex;
        int sx2max = (el.viewnrtilesx + 3) * el.scaledtilex;
        if (sx2e > sx2max) sx2e = sx2max;
        int sy2e = barrely + sy2 * el.scaledtiley;
        int sy2max = (el.viewnrtilesy + 3) * el.scaledtiley;
        if (sy2e > sy2max) sy2e = sy2max;
        drawCanvasRegion(target, background, sx1e, sy1e, sx2e - sx1e, sy2e - sy1e,
                dx1 * el.scaledtilex - barreldx + el.canvas_xofs,
                dy1 * el.scaledtiley - barreldy + el.canvas_yofs,
                sx2e - sx1e, sy2e - sy1e);
    }

    public InputStream openAsset(String path) throws UnsupportedEncodingException {
        if (isHeadless()) {
            try {
                return new FileInputStream(headlessAssetPath(path));
            } catch (IOException e) {
                throw new Error("Asset '" + path + "' could not be read", e);
            }
        }
        String text = getTextAsset(path);
        if (text == null) throw new Error("Text asset '" + path + "' was not preloaded");
        return new ByteArrayInputStream(text.getBytes("UTF-8"));
    }

    public void defineMedia(String filename) {
        int lnr = 1;
        int nr_lines = 0;
        try {
            InputStream instr = openAsset(filename);
            InputStreamReader in = new InputStreamReader(instr, "UTF-8");
            while (EngineLogic.readline(in) != null) nr_lines++;
            if (nr_lines == 0) exitEngine("Cannot open `" + filename + "'.");
            in = new InputStreamReader(openAsset(filename), "UTF-8");
            String line;
            String[] fields = new String[14];
            while ((line = EngineLogic.readline(in)) != null) {
                setProgressBar((double) lnr / (double) nr_lines);
                int i = 0;
                Vector tokens = EngineLogic.tokenizeString(line, '\t');
                for (Enumeration e = tokens.elements(); e.hasMoreElements(); ) fields[i++] = (String) e.nextElement();
                if (i == 8) defineImageMap(this, fields[0], fields[1], Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]), Integer.parseInt(fields[6]), Integer.parseInt(fields[7]));
                else if (i == 9) defineImage(fields[0], fields[1], Integer.parseInt(fields[2]), fields[3], fields[4], Integer.parseInt(fields[5]), Integer.parseInt(fields[6]), Integer.parseInt(fields[7]), Integer.parseInt(fields[8]));
                else if (i == 5) defineImage(fields[0], fields[1], Integer.parseInt(fields[2]), fields[3], fields[4], -1, -1, -1, -1);
                else if (i == 10) el.defineImage(fields[0], fields[1], Integer.parseInt(fields[2]), el.getSubImage(fields[3], Integer.parseInt(fields[4])), fields[5], Integer.parseInt(fields[6]), Integer.parseInt(fields[7]), Integer.parseInt(fields[8]), Integer.parseInt(fields[9]));
                else if (i == 6) el.defineImage(fields[0], fields[1], Integer.parseInt(fields[2]), el.getSubImage(fields[3], Integer.parseInt(fields[4])), fields[5], -1, -1, -1, -1);
                else if (i == 3) defineAnimation(fields[0], EngineLogic.splitList(fields[1]), Double.parseDouble(fields[2]));
                else if (i == 4) defineAnimation(fields[0], EngineLogic.splitList(fields[1]), Double.parseDouble(fields[2]), fields[3].equals("true"));
                else if (i == 2) defineAudioClip(fields[0], fields[1]);
                lnr++;
            }
        } catch (JGameError e) {
            exitEngine("Error in " + filename + " line " + lnr + ": " + e);
        } catch (Exception e) {
            exitEngine("Error in " + filename + " line " + lnr + ":\n" + dbgExceptionToString(e));
        }
    }

    public void defineImage(String name, String tilename, int collisionid, String imgfile, String img_op, int top, int left, int width, int height) { defineImage(this, name, tilename, collisionid, imgfile, img_op, top, left, width, height); }
    public void defineImage(String imgname, String tilename, int collisionid, String imgfile, String img_op) { defineImage(this, imgname, tilename, collisionid, imgfile, img_op, -1, -1, -1, -1); }
    public void defineImage(String imgname, String tilename, int collisionid, String imgmap, int mapidx, String img_op, int top, int left, int width, int height) { defineImage(imgname, tilename, collisionid, imgmap, mapidx, img_op, top, left, width, height); }
    public void defineImage(String imgname, String tilename, int collisionid, String imgmap, int mapidx, String img_op) { el.defineImage(imgname, tilename, collisionid, imgmap, mapidx, img_op); }
    public void defineImageRotated(String name, String tilename, int collisionid, String srcname, double angle) { el.defineImageRotated(this, name, tilename, collisionid, srcname, angle); }
    public void defineImageMap(String mapname, String imgfile, int xofs, int yofs, int tilex, int tiley, int skipx, int skipy) { defineImageMap(this, mapname, imgfile, xofs, yofs, tilex, tiley, skipx, skipy); }
    public void defineImage(Object pkg_obj, String name, String tilename, int collisionid, String imgfile, String img_op, int top, int left, int width, int height) {
        if (el.images_loaded.containsKey(name) && !el.images_loaded.get(name).equals(imgfile)) el.undefineImage(name);
        JGImage img = null;
        if (!imgfile.equals("null")) {
            img = el.imageutil.loadImage(imgfile);
            el.images_loaded.put(name, imgfile);
        }
        el.defineImage(name, tilename, collisionid, img, img_op, top, left, width, height);
    }
    public void defineImageMap(Object pkg_obj, String mapname, String imgfile, int xofs, int yofs, int tilex, int tiley, int skipx, int skipy) { el.imagemaps.put(mapname, new ImageMap(el.imageutil, imgfile, xofs, yofs, tilex, tiley, skipx, skipy)); }

    public void drawTile(int xi, int yi, int tileid) {
        if (backgroundContext == null) return;
        int x = el.moduloFloor(xi + 1, el.viewnrtilesx + 3) * el.scaledtilex;
        int y = el.moduloFloor(yi + 1, el.viewnrtilesy + 3) * el.scaledtiley;
        Integer tileid_obj = new Integer(tileid);
        BrowserImage img = (BrowserImage) el.getTileImage(tileid_obj);
        if (img == null || el.images_transp.containsKey(tileid_obj)) {
            EngineLogic.BGImage bg_image = (EngineLogic.BGImage) el.bg_images.elementAt(0);
            if (bg_image == null) {
                fillRect(backgroundContext, x, y, el.scaledtilex, el.scaledtiley, cssColor(el.bg_color));
            } else {
                int xtile = el.moduloFloor(xi, bg_image.tiles.x);
                int ytile = el.moduloFloor(yi, bg_image.tiles.y);
                BrowserImage bg = (BrowserImage) el.getImage(bg_image.imgname);
                drawCanvasRegion(backgroundContext, bg.image, xtile * el.scaledtilex, ytile * el.scaledtiley, el.scaledtilex, el.scaledtiley, x, y, el.scaledtilex, el.scaledtiley);
            }
        }
        if (img != null) drawCanvas(backgroundContext, img.image, x, y);
    }

    public void drawImage(double x, double y, String imgname, boolean pf_relative) {
        if (currentContext == null || imgname == null) return;
        x = el.scaleXPos(x, pf_relative) + el.canvas_xofs;
        y = el.scaleYPos(y, pf_relative) + el.canvas_yofs;
        BrowserImage img = (BrowserImage) el.getImage(imgname);
        if (img != null) drawCanvas(currentContext, img.image, x, y);
    }

    public void drawImage(double x, double y, String imgname, JGColor blend_col, double alpha, double rot, double scale, boolean pf_relative) {
        if (currentContext == null || imgname == null) return;
        if (blend_col == null && alpha == 1.0 && rot == 0.0 && scale == 1.0) { drawImage(x, y, imgname, pf_relative); return; }
        x = el.scaleXPos(x, pf_relative) + el.canvas_xofs;
        y = el.scaleYPos(y, pf_relative) + el.canvas_yofs;
        BrowserImage img = (BrowserImage) el.getImageOrig(imgname);
        if (img != null) drawCanvasTransformed(currentContext, img.image, x, y, el.x_scale_fac, el.y_scale_fac, alpha, -rot, scale);
    }

    public void drawLine(double x1, double y1, double x2, double y2, double thickness, JGColor color) { if (color != null) setColor(color); setStroke(thickness); drawLine(x1, y1, x2, y2, true); }
    public void drawLine(double x1, double y1, double x2, double y2) { drawLine(x1, y1, x2, y2, true); }
    public void drawLine(double x1, double y1, double x2, double y2, boolean pf_relative) {
        if (currentContext == null) return;
        drawLineJs(currentContext, el.scaleXPos(x1, pf_relative) + el.canvas_xofs, el.scaleYPos(y1, pf_relative) + el.canvas_yofs, el.scaleXPos(x2, pf_relative) + el.canvas_xofs, el.scaleYPos(y2, pf_relative) + el.canvas_yofs, strokeStyle, lineWidth);
    }
    public void drawPolygon(double[] x, double[] y, JGColor[] col, int len, boolean filled, boolean pf_relative) {
        for (int i = 1; i < len; i++) { if (col != null) setColor(col[i]); drawLine(x[i - 1], y[i - 1], x[i], y[i], pf_relative); }
        if (len > 1) { if (col != null) setColor(col[0]); drawLine(x[len - 1], y[len - 1], x[0], y[0], pf_relative); }
    }
    public void drawRect(double x, double y, double width, double height, boolean filled, boolean centered, double thickness, JGColor color) { if (color != null) setColor(color); setStroke(thickness); drawRect(x, y, width, height, filled, centered, true); }
    public void drawRect(double x, double y, double width, double height, boolean filled, boolean centered) { drawRect(x, y, width, height, filled, centered, true); }
    public void drawRect(double x, double y, double width, double height, boolean filled, boolean centered, boolean pf_relative) { drawRect(x, y, width, height, filled, centered, pf_relative, null, null); }
    public void drawRect(double x, double y, double width, double height, boolean filled, boolean centered, boolean pf_relative, JGColor[] shadecol) { drawRect(x, y, width, height, filled, centered, pf_relative, shadecol, null); }
    public void drawRect(double x, double y, double width, double height, boolean filled, boolean centered, boolean pf_relative, JGColor[] shadecol, String tileimage) {
        if (currentContext == null) return;
        if (centered) { x -= width / 2; y -= height / 2; }
        JGRectangle r = el.scalePos(x, y, width, height, pf_relative); r.x += el.canvas_xofs; r.y += el.canvas_yofs;
        if (filled) fillRect(currentContext, r.x, r.y, r.width, r.height, fillStyle); else strokeRect(currentContext, r.x, r.y, r.width, r.height, strokeStyle, lineWidth);
    }
    public void drawOval(double x, double y, double width, double height, boolean filled, boolean centered, double thickness, JGColor color) { if (color != null) setColor(color); setStroke(thickness); drawOval(x, y, width, height, filled, centered); }
    public void drawOval(double x, double y, double width, double height, boolean filled, boolean centered) { drawOval(x, y, width, height, filled, centered, true); }
    public void drawOval(double x, double y, double width, double height, boolean filled, boolean centered, boolean pf_relative) {
        if (centered) { x -= width / 2; y -= height / 2; }
        JGRectangle r = el.scalePos(x, y, width, height, pf_relative); r.x += el.canvas_xofs; r.y += el.canvas_yofs;
        oval(currentContext, r.x, r.y, r.width, r.height, filled, filled ? fillStyle : strokeStyle, lineWidth);
    }
    public void drawImage(String imgname, double x, double y) { drawImage(x, y, imgname); }
    public void drawImage(String imgname, double x, double y, boolean pf_relative) { drawImage(x, y, imgname, pf_relative); }
    public void drawImage(String imgname, double x, double y, boolean pf_relative, JGColor blend_col, double alpha, double rot, double scale) { drawImage(x, y, imgname, blend_col, alpha, rot, scale, pf_relative); }
    public void drawImage(double x, double y, String imgname) { drawImage(x, y, imgname, true); }

    public void drawString(String str, double x, double y, int align, JGFont font, JGColor color) { if (font != null) setFont(font); if (color != null) setColor(color); drawString(str, x, y, align, false); }
    public void drawString(String str, double x, double y, int align) { drawString(str, x, y, align, false); }
    public void drawString(String str, double x, double y, int align, boolean pf_relative) {
        if (currentContext == null || str.equals("")) return;
        x = el.scaleXPos(x, pf_relative) + el.canvas_xofs;
        y = el.scaleYPos(y, pf_relative) + el.canvas_yofs + getFontHeight(el.msg_font);
        drawText(currentContext, str, x, y, align, fillStyle, fontStyle);
    }
    public void drawImageString(String string, double x, double y, int align, String imgmap, int char_offset, int spacing) { el.drawImageString(this, string, x, y, align, imgmap, char_offset, spacing, false); }
    public void drawImageString(String string, double x, double y, int align, String imgmap, int char_offset, int spacing, boolean pf_relative) { el.drawImageString(this, string, x, y, align, imgmap, char_offset, spacing, pf_relative); }

    public void invokeGameStateMethods(String prefix, Vector states) { for (int i = 0; i < states.size(); i++) tryMethod(prefix, (String) states.elementAt(i)); }
    public void tryMethod(String prefix, String suffix) {
        int prefidx, sufidx;
        for (prefidx = 0; prefidx < stateprefixes.length; prefidx++) if (stateprefixes[prefidx].equals(prefix)) break;
        for (sufidx = 0; sufidx < statesuffixes.length; sufidx++) if (statesuffixes[sufidx].equals(suffix)) break;
        if (sufidx >= statesuffixes.length) exitEngine("Game state " + suffix + " not supported!");
        switch (statesuffixes.length * prefidx + sufidx) {
            case 0: startLoader(); break; case 1: startTitle(); break; case 2: startSelectLevel(); break; case 3: startHighscores(); break; case 4: startInGame(); break; case 5: startStartLevel(); break; case 6: startStartGame(); break; case 7: startLevelDone(); break; case 8: startLifeLost(); break; case 9: startGameOver(); break; case 10: startEnterHighscore(); break; case 11: startPaused(); break;
            case 12: doFrameLoader(); break; case 13: doFrameTitle(); break; case 14: doFrameSelectLevel(); break; case 15: doFrameHighscores(); break; case 16: doFrameInGame(); break; case 17: doFrameStartLevel(); break; case 18: doFrameStartGame(); break; case 19: doFrameLevelDone(); break; case 20: doFrameLifeLost(); break; case 21: doFrameGameOver(); break; case 22: doFrameEnterHighscore(); break; case 23: doFramePaused(); break;
            case 24: paintFrameLoader(); break; case 25: paintFrameTitle(); break; case 26: paintFrameSelectLevel(); break; case 27: paintFrameHighscores(); break; case 28: paintFrameInGame(); break; case 29: paintFrameStartLevel(); break; case 30: paintFrameStartGame(); break; case 31: paintFrameLevelDone(); break; case 32: paintFrameLifeLost(); break; case 33: paintFrameGameOver(); break; case 34: paintFrameEnterHighscore(); break; case 35: paintFramePaused(); break;
            default: exitEngine("Game state method " + prefix + suffix + " not supported");
        }
    }

    public void startLoader() {} public void startTitle() {} public void startSelectLevel() {} public void startHighscores() {} public void startInGame() {} public void startStartLevel() {} public void startStartGame() {} public void startLevelDone() {} public void startLifeLost() {} public void startGameOver() {} public void startEnterHighscore() {} public void startPaused() {}
    public void doFrameLoader() {} public void doFrameTitle() {} public void doFrameSelectLevel() {} public void doFrameHighscores() {} public void doFrameInGame() {} public void doFrameStartLevel() {} public void doFrameStartGame() {} public void doFrameLevelDone() {} public void doFrameLifeLost() {} public void doFrameGameOver() {} public void doFrameEnterHighscore() {} public void doFramePaused() {}
    public void paintFrameLoader() {} public void paintFrameTitle() {} public void paintFrameSelectLevel() {} public void paintFrameHighscores() {} public void paintFrameInGame() {} public void paintFrameStartLevel() {} public void paintFrameStartGame() {} public void paintFrameLevelDone() {} public void paintFrameLifeLost() {} public void paintFrameGameOver() {} public void paintFrameEnterHighscore() {} public void paintFramePaused() {}
    public void doFrame() {} public void paintFrame() {}

    public void setProgressBar(double pos) { progress_bar = pos; }
    public void setProgressMessage(String msg) { progress_message = msg; }
    public void setAuthorMessage(String msg) { author_message = msg; }
    public void setCanvasSettings(int nrtilesx, int nrtilesy, int tilex, int tiley, JGColor fgcolor, JGColor bgcolor, JGFont msgfont) { el.nrtilesx = nrtilesx; el.nrtilesy = nrtilesy; el.viewnrtilesx = nrtilesx; el.viewnrtilesy = nrtilesy; el.tilex = tilex; el.tiley = tiley; setColorsFont(fgcolor, bgcolor, msgfont); el.view_initialised = true; }
    public void setScalingPreferences(double min_aspect_ratio, double max_aspect_ratio, int crop_top, int crop_left, int crop_bottom, int crop_right) { el.min_aspect = min_aspect_ratio; el.max_aspect = max_aspect_ratio; el.crop_top = crop_top; el.crop_left = crop_left; el.crop_bottom = crop_bottom; el.crop_right = crop_right; }
    public void setSmoothing(boolean smooth_magnify) { el.smooth_magnify = smooth_magnify; if (!isHeadless()) { setImageSmoothing(context, smooth_magnify); setImageSmoothing(backgroundContext, smooth_magnify); } }
    public JGImage getImage(String imgname) { return el.getImage(imgname); }
    public JGPoint getImageSize(String imgname) { return el.getImageSize(imgname); }
    public JGRectangle getImageBBox(String imgname) { return el.getImageBBox(imgname); }
    public void markAddObject(JGObject obj) { el.markAddObject(obj); }
    public boolean existsObject(String index) { return el.existsObject(index); }
    public JGObject getObject(String index) { return el.getObject(index); }
    public void moveObjects(String prefix, int cidmask) { el.moveObjects(this, prefix, cidmask); }
    public void moveObjects() { el.moveObjects(this); }
    public void checkCollision(int srccid, int dstcid) { el.checkCollision(this, srccid, dstcid); }
    public int checkCollision(int cidmask, JGObject obj) { return el.checkCollision(cidmask, obj); }
    public int checkBGCollision(JGRectangle r) { return el.checkBGCollision(r); }
    public void checkBGCollision(int tilecid, int objcid) { el.checkBGCollision(this, tilecid, objcid); }
    public Vector getObjects(String prefix, int cidmask, boolean suspended_obj, JGRectangle bbox) { return el.getObjects(prefix, cidmask, suspended_obj, bbox); }
    public void removeObject(JGObject obj) { el.removeObject(obj); }
    public void removeObjects(String prefix, int cidmask) { el.removeObjects(prefix, cidmask); }
    public void removeObjects(String prefix, int cidmask, boolean suspended_obj) { el.removeObjects(prefix, cidmask, suspended_obj); }
    public int countObjects(String prefix, int cidmask) { return el.countObjects(prefix, cidmask); }
    public int countObjects(String prefix, int cidmask, boolean suspended_obj) { return el.countObjects(prefix, cidmask, suspended_obj); }
    public void setBGImage(String bgimg) { el.setBGImage(bgimg, 0, true, true); }
    public void setBGImage(int depth, String bgimg, boolean wrapx, boolean wrapy) { el.setBGImage(bgimg, depth, wrapx, wrapy); }
    public void setTileSettings(String out_of_bounds_tile, int out_of_bounds_cid, int preserve_cids) { el.setTileSettings(out_of_bounds_tile, out_of_bounds_cid, preserve_cids); }
    public void fillBG(String filltile) { el.fillBG(filltile); }
    public void setTileCid(int x, int y, int and_mask, int or_mask) { el.setTileCid(x, y, and_mask, or_mask); }
    public void setTile(int x, int y, String tilestr) { el.setTile(x, y, tilestr); }
    public int countTiles(int tilecidmask) { return el.countTiles(tilecidmask); }
    public int getTileCid(int xidx, int yidx) { return el.getTileCid(xidx, yidx); }
    public String getTileStr(int xidx, int yidx) { return el.getTileStr(xidx, yidx); }
    public int getTileCid(JGRectangle tiler) { return el.getTileCid(tiler); }
    public JGRectangle getTiles(JGRectangle r) { return el.getTiles(r); }
    public boolean getTiles(JGRectangle dest, JGRectangle r) { return el.getTiles(dest, r); }
    public void setTileCid(int x, int y, int value) { el.setTileCid(x, y, value); }
    public void orTileCid(int x, int y, int or_mask) { el.orTileCid(x, y, or_mask); }
    public void andTileCid(int x, int y, int and_mask) { el.andTileCid(x, y, and_mask); }
    public void setTile(JGPoint tileidx, String tilename) { el.setTile(tileidx, tilename); }
    public void setTiles(int xofs, int yofs, String[] tilemap) { el.setTiles(xofs, yofs, tilemap); }
    public void setTilesMulti(int xofs, int yofs, String[] tilemap) { el.setTilesMulti(xofs, yofs, tilemap); }
    public int getTileCidAtCoord(double x, double y) { return el.getTileCidAtCoord(x, y); }
    public int getTileCid(JGPoint center, int xofs, int yofs) { return el.getTileCid(center, xofs, yofs); }
    public String getTileStrAtCoord(double x, double y) { return el.getTileStrAtCoord(x, y); }
    public String getTileStr(JGPoint center, int xofs, int yofs) { return el.getTileStr(center, xofs, yofs); }
    public int tileStrToID(String tilestr) { return el.tileStrToID(tilestr); }
    public String tileIDToStr(int tileid) { return el.tileIDToStr(tileid); }
    public double moduloXPos(double x) { return el.moduloXPos(x); }
    public double moduloYPos(double y) { return el.moduloYPos(y); }
    public void dbgShowBoundingBox(boolean enabled) {} public void dbgShowGameState(boolean enabled) {} public void dbgShowFullStackTrace(boolean enabled) {} public void dbgShowMessagesInPf(boolean enabled) {} public void dbgSetMessageExpiry(int ticks) {} public void dbgSetMessageFont(JGFont font) {} public void dbgSetDebugColor1(JGColor col) {} public void dbgSetDebugColor2(JGColor col) {}
    public void dbgPrint(String msg) { dbgPrint("MAIN", msg); }
    public void dbgPrint(String source, String msg) { consoleLog(source + ": " + msg); }
    public void dbgShowException(String source, Throwable e) { e.printStackTrace(); consoleError(source + ": " + e.toString()); }
    public String dbgExceptionToString(Throwable e) { return e.toString(); }
    public void exitEngine(String msg) { stopAudio(); if (msg != null) { consoleError(msg); el.exit_message = msg; } el.is_exited = true; }
    public void requestGameFocus() { focusCanvas(canvas); }
    public boolean isApplet() { return false; } public boolean isMidlet() { return false; } public boolean isAndroid() { return false; } public boolean isOpenGL() { return false; }
    public int viewWidth() { return el.viewnrtilesx * el.tilex; } public int viewHeight() { return el.viewnrtilesy * el.tiley; } public int viewTilesX() { return el.viewnrtilesx; } public int viewTilesY() { return el.viewnrtilesy; } public int viewXOfs() { return el.pendingxofs; } public int viewYOfs() { return el.pendingyofs; } public int pfWidth() { return el.nrtilesx * el.tilex; } public int pfHeight() { return el.nrtilesy * el.tiley; } public int pfTilesX() { return el.nrtilesx; } public int pfTilesY() { return el.nrtilesy; } public boolean pfWrapX() { return el.pf_wrapx; } public boolean pfWrapY() { return el.pf_wrapy; } public int tileWidth() { return el.tilex; } public int tileHeight() { return el.tiley; } public int displayWidth() { return el.winwidth; } public int displayHeight() { return el.winheight; }
    public double getFrameRate() { return el.fps; } public double getGameSpeed() { return el.gamespeed; } public void setGameSpeed(double gamespeed) { el.setGameSpeed(gamespeed); } public double getFrameSkip() { return el.maxframeskip; } public boolean getVideoSyncedUpdate() { return false; } public void setVideoSyncedUpdate(boolean value) {} public int getOffscreenMarginX() { return el.offscreen_margin_x; } public int getOffscreenMarginY() { return el.offscreen_margin_y; } public double getXScaleFactor() { return el.x_scale_fac; } public double getYScaleFactor() { return el.y_scale_fac; } public double getMinScaleFactor() { return el.min_scale_fac; }
    public void setViewOffset(int xofs, int yofs, boolean centered) { el.setViewOffset(xofs, yofs, centered); } public void setBGImgOffset(int depth, double xofs, double yofs, boolean centered) {} public void setViewZoomRotate(double zoom, double rotate) {} public void setPFSize(int nrtilesx, int nrtilesy) { el.setPFSize(nrtilesx, nrtilesy); } public void setPFWrap(boolean wrapx, boolean wrapy, int shiftx, int shifty) { el.setPFWrap(wrapx, wrapy, shiftx, shifty); } public void setFrameRate(double fps, double maxframeskip) { el.setFrameRate(fps, maxframeskip); } public void setRenderSettings(int alpha_thresh, JGColor render_bg_col) { el.setRenderSettings(alpha_thresh, render_bg_col); } public void setOffscreenMargin(int xmargin, int ymargin) { el.setOffscreenMargin(xmargin, ymargin); }
    public void setBGColor(JGColor bgcolor) { el.bg_color = bgcolor; } public void setFGColor(JGColor fgcolor) { el.fg_color = fgcolor; } public void setMsgFont(JGFont msgfont) { el.msg_font = msgfont; } public void setColorsFont(JGColor fgcolor, JGColor bgcolor, JGFont msgfont) { if (msgfont != null) el.msg_font = msgfont; if (fgcolor != null) el.fg_color = fgcolor; if (bgcolor != null) setBGColor(bgcolor); } public void setTextOutline(int thickness, JGColor colour) { el.outline_colour = colour; el.outline_thickness = thickness; } public void setMouseCursor(int cursor) {} public void setMouseCursor(Object cursor) {}
    public void removeAllTimers() { el.removeAllTimers(); } public void registerTimer(JGTimer timer) { el.registerTimer(timer); } public void setGameState(String state) { el.setGameState(state); } public void addGameState(String state) { el.addGameState(state); } public void removeGameState(String state) { el.removeGameState(state); } public void clearGameState() { el.clearGameState(); } public boolean inGameState(String state) { return el.inGameState(state); } public boolean inGameStateNextFrame(String state) { return el.inGameStateNextFrame(state); }
    public void setColor(JGColor col) { fillStyle = cssColor(col); strokeStyle = fillStyle; } public void setFont(JGFont font) { if (font != null) fontStyle = ((int) Math.max(1, font.size * el.min_scale_fac)) + "px sans-serif"; } public void setStroke(double thickness) { lineWidth = thickness * el.min_scale_fac; } public void setBlendMode(int src_func, int dst_func) {} public double getFontHeight(JGFont jgfont) { return jgfont == null ? 8 : jgfont.size * el.min_scale_fac; }
    public JGPoint getMousePos() { return mousepos; } public int getMouseX() { return mousepos.x; } public int getMouseY() { return mousepos.y; } public double getMouseXD() { return mouseposd_x; } public double getMouseYD() { return mouseposd_y; } public boolean getMouseButton(int nr) { return mousebutton[nr]; } public void clearMouseButton(int nr) { mousebutton[nr] = false; } public void setMouseButton(int nr) { mousebutton[nr] = true; } public boolean getMouseInside() { return mouseinside; } public boolean getKey(int key) { return key >= 0 && key < keymap.length && keymap[key]; } public void clearKey(int key) { if (key >= 0 && key < keymap.length) keymap[key] = false; } public void setKey(int key) { if (key >= 0 && key < keymap.length) keymap[key] = true; } public int getLastKey() { return lastkey; } public char getLastKeyChar() { return lastkeychar; } public void clearLastKey() { lastkey = 0; lastkeychar = 0; } public String getKeyDesc(int key) { return getKeyDescStatic(key); } public int getKeyCode(String keydesc) { return getKeyCodeStatic(keydesc); }
    public boolean hasAccelerometer() { return false; } public double getAccelX() { return accelvec[0]; } public double getAccelY() { return accelvec[1]; } public double getAccelZ() { return accelvec[2]; } public double[] getAccelVec() { return new double[]{accelvec[0], accelvec[1], accelvec[2]}; } public void defineAnimation(String id, String[] frames, double speed) { el.defineAnimation(id, frames, speed); } public void defineAnimation(String id, String[] frames, double speed, boolean pingpong) { el.defineAnimation(id, frames, speed, pingpong); } public Animation getAnimation(String id) { return el.getAnimation(id); } public String getConfigPath(String filename) { return null; } public int invokeUrl(String url, String target) { openUrl(url, target); return -1; } public boolean and(int value, int mask) { return el.and(value, mask); } public int timerCount() { return el.getTimerCount(); } public void setRandomSeed(long seed) { el.setRandomSeed(seed); } public String audioTrace() { return audioTrace.toString(); } public double random(double min, double max) { return el.random(min, max); } public double random(double min, double max, double interval) { return el.random(min, max, interval); } public int random(int min, int max, int interval) { return el.random(min, max, interval); } public double atan2(double y, double x) { return Math.atan2(y, x); } public JGPoint getTileIndex(double x, double y) { return el.getTileIndex(x, y); } public JGPoint getTileCoord(int tilex, int tiley) { return el.getTileCoord(tilex, tiley); } public JGPoint getTileCoord(JGPoint tileidx) { return el.getTileCoord(tileidx); } public double snapToGridX(double x, double gridsnapx) { return el.snapToGridX(x, gridsnapx); } public double snapToGridY(double y, double gridsnapy) { return el.snapToGridY(y, gridsnapy); } public void snapToGrid(JGPoint p, int gridsnapx, int gridsnapy) { el.snapToGrid(p, gridsnapx, gridsnapy); } public boolean isXAligned(double x, double margin) { return el.isXAligned(x, margin); } public boolean isYAligned(double y, double margin) { return el.isYAligned(y, margin); } public double getXAlignOfs(double x) { return el.getXAlignOfs(x); } public double getYAlignOfs(double y) { return el.getYAlignOfs(y); } public double getXDist(double x1, double x2) { return el.getXDist(x1, x2); } public double getYDist(double y1, double y2) { return el.getYDist(y1, y2); }
    public void enableAudio() { audioenabled = true; } public void disableAudio() { audioenabled = false; stopAudio(); } public void defineAudioClip(String clipid, String filename) { audioclips.put(clipid, filename); } public String lastPlayedAudio(String channel) { return (String) channeltoclip.get(channel); } public void playAudio(String clipid) { playAudio(null, clipid, false); } public void playAudio(String channel, String clipid, boolean loop) { if (!audioenabled) return; String filename = (String) audioclips.get(clipid); if (filename == null) return; if (audioTrace.length() > 0) audioTrace.append('|'); audioTrace.append("play ").append(channel).append(' ').append(clipid).append(' ').append(loop); if (channel != null) channeltoclip.put(channel, clipid); if (!isHeadless()) playAudioJs(channel, filename, loop); } public void stopAudio(String channel) { if (audioTrace.length() > 0) audioTrace.append('|'); audioTrace.append("stop ").append(channel); if (!isHeadless()) stopAudioJs(channel); channeltoclip.remove(channel); } public void stopAudio() { if (audioTrace.length() > 0) audioTrace.append('|'); audioTrace.append("stop all"); if (!isHeadless()) stopAllAudioJs(); channeltoclip.clear(); }
    public void storeWriteInt(String id, int value) { store.put(id, new Integer(value)); } public void storeWriteDouble(String id, double value) { store.put(id, new Double(value)); } public void storeWriteString(String id, String value) { store.put(id, value); } void storeWriteBoolean(String id, boolean value) { store.put(id, new Boolean(value)); } public void storeRemove(String id) { store.remove(id); } public boolean storeExists(String id) { return store.containsKey(id); } public int storeReadInt(String id, int undef) { Object v = store.get(id); return v instanceof Number ? ((Number) v).intValue() : undef; } public double storeReadDouble(String id, double undef) { Object v = store.get(id); return v instanceof Number ? ((Number) v).doubleValue() : undef; } public String storeReadString(String id, String undef) { Object v = store.get(id); return v == null ? undef : v.toString(); } public void optsAddTitle(String title) {} public void optsAddNumber(String varname, String title, String desc, int decimals, double lower, double upper, double step, double initial) {} public void optsAddBoolean(String varname, String title, String desc, boolean initial) {} public void optsAddEnum(String varname, String title, String desc, String[] values, int initial) {} public void optsAddKey(String varname, String title, String desc, int initial) {} public void optsAddString(String varname, String title, String desc, int maxlen, boolean isPassword, String initial) {} public void optsClear() {}
    public void start() { running = true; } public void stop() { stopAudio(); running = false; } public void startApp() {} public void pauseApp() {} public void destroyApp(boolean unconditional) { destroy(); } public boolean isRunning() { return running; } public void wakeUpOnKey(int key) { wakeup_key = key; } public void destroy() { stopAudio(); el.is_exited = true; if (el.is_inited) JGObject.setEngine(null); }

    public void setPointer(double cssX, double cssY, boolean down, boolean inside) { mouseposd_x = cssX; mouseposd_y = cssY; mousepos.x = (int) cssX; mousepos.y = (int) cssY; mousebutton[1] = down; keymap[KeyMouse1] = down; mouseinside = inside; }
    public void setBrowserKey(int key, boolean state, int ch) { if (key >= 0 && key < keymap.length) keymap[key] = state; if (key == 32) keymap[KeyFire] = state; if (state) { lastkey = key; lastkeychar = (char) ch; } }

    private static String cssColor(JGColor c) { return "rgba(" + c.r + "," + c.g + "," + c.b + "," + (c.alpha / 255.0) + ")"; }

    private static String headlessAssetPath(String path) {
    String userDir = System.getProperty("user.dir");
    String prefix = userDir != null && userDir.endsWith("web") ? "../app/src/main/assets/" : "app/src/main/assets/";
    return prefix + path;
}

@JSBody(params = {}, script = "return document.getElementById('raumballer');") private static native JSObject getCanvas();
@JSBody(params = {}, script = "var p=new URLSearchParams(window.location.search); if (p.has('seed')) return Number(p.get('seed')); return typeof window.RaumBallerSeed === 'number' ? window.RaumBallerSeed : NaN;") private static native double getConfiguredSeed();
@JSBody(params = {}, script = "return new URLSearchParams(window.location.search).get('test') === '1' || window.RaumBallerTestMode === true;") private static native boolean isBrowserTestMode();
    @JSBody(params = {}, script = "var c=document.createElement('canvas'); c.id='raumballer'; c.width=288; c.height=512; document.body.appendChild(c); return c;") private static native JSObject createCanvasElement();
    @JSBody(params = {"canvas"}, script = "return canvas.getContext('2d');") private static native JSObject get2dContext(JSObject canvas);
    @JSBody(params = {"canvas"}, script = "return canvas.width;") private static native int getCanvasWidth(JSObject canvas);
    @JSBody(params = {"canvas"}, script = "return canvas.height;") private static native int getCanvasHeight(JSObject canvas);
    @JSBody(params = {"canvas", "w", "h"}, script = "canvas.width=w; canvas.height=h;") private static native void setCanvasSize(JSObject canvas, int w, int h);
    @JSBody(params = {"ctx", "enabled"}, script = "if(ctx){ctx.imageSmoothingEnabled=enabled;}") private static native void setImageSmoothing(JSObject ctx, boolean enabled);
    @JSBody(params = {"handler"}, script = "function loop(ts){ handler(ts); window.requestAnimationFrame(loop); } window.requestAnimationFrame(loop);") private static native void startAnimationLoop(FrameHandler handler);
    @JSBody(params = {"canvas", "pointer", "key"}, script = "function pos(e){var r=canvas.getBoundingClientRect(); return {x:(e.clientX-r.left)*canvas.width/r.width, y:(e.clientY-r.top)*canvas.height/r.height};} canvas.addEventListener('pointerdown',function(e){var p=pos(e); canvas.focus(); canvas.setPointerCapture && canvas.setPointerCapture(e.pointerId); pointer(p.x,p.y,true,true); e.preventDefault();}); canvas.addEventListener('pointermove',function(e){var p=pos(e); pointer(p.x,p.y,e.buttons!==0,true); e.preventDefault();}); function up(e){var p=pos(e); pointer(p.x,p.y,false,false); e.preventDefault();} canvas.addEventListener('pointerup',up); canvas.addEventListener('pointercancel',up); window.addEventListener('keydown',function(e){var k=keyCode(e); if(k>=0){key(k,true,e.key && e.key.length===1 ? e.key.charCodeAt(0):0); if(prevent(e)) e.preventDefault();}}); window.addEventListener('keyup',function(e){var k=keyCode(e); if(k>=0){key(k,false,e.key && e.key.length===1 ? e.key.charCodeAt(0):0); if(prevent(e)) e.preventDefault();}}); function keyCode(e){if(e.key==='ArrowUp')return 38;if(e.key==='ArrowDown')return 40;if(e.key==='ArrowLeft')return 37;if(e.key==='ArrowRight')return 39;if(e.key===' '||e.code==='Space')return 32;if(e.key&&e.key.length===1)return e.key.toUpperCase().charCodeAt(0);return -1;} function prevent(e){return ['ArrowUp','ArrowDown','ArrowLeft','ArrowRight',' '].indexOf(e.key)>=0 || ['KeyW','KeyA','KeyS','KeyD','Space'].indexOf(e.code)>=0;}") private static native void installInputHandlers(JSObject canvas, PointerHandler pointer, KeyHandler key);
    @JSBody(params = {"path"}, script = "return window.RaumBallerAssets && window.RaumBallerAssets.text[path] || null;") private static native String getTextAsset(String path);
    @JSBody(params = {"ctx", "color"}, script = "ctx.save(); ctx.fillStyle=color; ctx.fillRect(0,0,ctx.canvas.width,ctx.canvas.height); ctx.restore();") private static native void clearCanvas(JSObject ctx, String color);
    @JSBody(params = {"ctx", "image", "x", "y"}, script = "ctx.drawImage(image,x,y);") private static native void drawCanvas(JSObject ctx, JSObject image, double x, double y);
    @JSBody(params = {"ctx", "image", "sx", "sy", "sw", "sh", "dx", "dy", "dw", "dh"}, script = "ctx.drawImage(image,sx,sy,sw,sh,dx,dy,dw,dh);") private static native void drawCanvasRegion(JSObject ctx, JSObject image, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh);
    @JSBody(params = {"ctx", "image", "x", "y", "sx", "sy", "alpha", "rot", "scale"}, script = "var w=image.width||image.naturalWidth,h=image.height||image.naturalHeight; ctx.save(); ctx.globalAlpha=alpha; ctx.translate(x+w*sx/2,y+h*sy/2); ctx.rotate(rot); ctx.scale(sx*scale,sy*scale); ctx.drawImage(image,-w/2,-h/2); ctx.restore();") private static native void drawCanvasTransformed(JSObject ctx, JSObject image, double x, double y, double sx, double sy, double alpha, double rot, double scale);
    @JSBody(params = {"ctx", "x", "y", "w", "h", "color"}, script = "ctx.save(); ctx.fillStyle=color; ctx.fillRect(x,y,w,h); ctx.restore();") private static native void fillRect(JSObject ctx, double x, double y, double w, double h, String color);
    @JSBody(params = {"ctx", "x", "y", "w", "h", "color", "lineWidth"}, script = "ctx.save(); ctx.strokeStyle=color; ctx.lineWidth=lineWidth; ctx.strokeRect(x,y,w,h); ctx.restore();") private static native void strokeRect(JSObject ctx, double x, double y, double w, double h, String color, double lineWidth);
    @JSBody(params = {"ctx", "x1", "y1", "x2", "y2", "color", "lineWidth"}, script = "ctx.save(); ctx.strokeStyle=color; ctx.lineWidth=lineWidth; ctx.beginPath(); ctx.moveTo(x1,y1); ctx.lineTo(x2,y2); ctx.stroke(); ctx.restore();") private static native void drawLineJs(JSObject ctx, double x1, double y1, double x2, double y2, String color, double lineWidth);
    @JSBody(params = {"ctx", "x", "y", "w", "h", "filled", "color", "lineWidth"}, script = "ctx.save(); ctx.beginPath(); ctx.ellipse(x+w/2,y+h/2,Math.abs(w/2),Math.abs(h/2),0,0,Math.PI*2); if(filled){ctx.fillStyle=color;ctx.fill();}else{ctx.strokeStyle=color;ctx.lineWidth=lineWidth;ctx.stroke();} ctx.restore();") private static native void oval(JSObject ctx, double x, double y, double w, double h, boolean filled, String color, double lineWidth);
    @JSBody(params = {"ctx", "text", "x", "y", "align", "color", "font"}, script = "ctx.save(); ctx.fillStyle=color; ctx.font=font; ctx.textBaseline='top'; ctx.textAlign=align<0?'left':(align>0?'right':'center'); ctx.fillText(text,x,y); ctx.restore();") private static native void drawText(JSObject ctx, String text, double x, double y, int align, String color, String font);
    @JSBody(params = {"channel", "filename", "loop"}, script = "window.RaumBallerChannels=window.RaumBallerChannels||Object.create(null); var base=window.RaumBallerAssets.audio[filename]; if(!base)return; if(channel&&window.RaumBallerChannels[channel]){window.RaumBallerChannels[channel].pause(); window.RaumBallerChannels[channel].currentTime=0;} var a=base.cloneNode(true); a.loop=loop; a.volume=0.9; if(channel)window.RaumBallerChannels[channel]=a; var p=a.play(); if(p)p.catch(function(){});") private static native void playAudioJs(String channel, String filename, boolean loop);
    @JSBody(params = {"channel"}, script = "if(window.RaumBallerChannels&&window.RaumBallerChannels[channel]){window.RaumBallerChannels[channel].pause(); window.RaumBallerChannels[channel].currentTime=0; delete window.RaumBallerChannels[channel];}") private static native void stopAudioJs(String channel);
    @JSBody(params = {}, script = "if(window.RaumBallerChannels){for(var k in window.RaumBallerChannels){window.RaumBallerChannels[k].pause(); window.RaumBallerChannels[k].currentTime=0;} window.RaumBallerChannels=Object.create(null);}") private static native void stopAllAudioJs();
    @JSBody(params = {"message"}, script = "console.log(message);") private static native void consoleLog(String message);
    @JSBody(params = {"message"}, script = "console.error(message);") private static native void consoleError(String message);
    @JSBody(params = {"canvas"}, script = "canvas && canvas.focus && canvas.focus();") private static native void focusCanvas(JSObject canvas);
    @JSBody(params = {"url", "target"}, script = "window.open(url,target||'_blank');") private static native void openUrl(String url, String target);
}
