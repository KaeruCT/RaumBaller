package jgame.platform;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

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

// XXX is end coord of drawRect inclusive or exclusive?

public abstract class JGEngine extends Application
        implements JGEngineInterface {

    /*=== main objects ===*/

    static JGEngine current_engine = null;

    static AssetManager assets;

    static int displayformat;

    AndroidImage imageutil = new AndroidImage();

    EngineLogic el = new EngineLogic(imageutil, true, true);

    Thread gamethread = null;

    Display display;

    public void setProgressBar(double pos) {
        // XXX check out if the load screen gets updated properly if we don't
        // use defineMedia
        progress_bar = pos;
        canvas.drawAll();
    }

    public void setProgressMessage(String msg) {
        progress_message = msg;
        canvas.drawAll();
    }

    public void setAuthorMessage(String msg) {
        author_message = msg;
        canvas.drawAll();
    }




    /*=== android specific stuff ===*/

    JGView canvas;

    Canvas bufg = null;
    Paint bufpainter = new Paint();
    Paint imagepainter = new Paint();
    Paint simpleimagepainter = new Paint();
    Bitmap background = null;
    Canvas bgg = null;
    Paint bgpainter = new Paint();


    SensorManager sensormanager;
    Sensor accelerometer;

    // android application

    @Override
    public void onCreate() {
        super.onCreate();
        current_engine = this;
    }

    JGActivity currentact = null;

    // called by JGActivity when created
    void initActivity(JGActivity act) {
        currentact = act;
        //setContentView(R.layout.main);
        assets = act.getAssets();
        canvas = new JGView(act);
        act.setContentView(canvas);
        canvas.requestFocus();
        //renderer = new GLAccelRenderer(glsview,false);
        //glsview.setRenderer(renderer);
        //setContentView(glsview);
        sensormanager = (SensorManager) act.getSystemService(SENSOR_SERVICE);
        accelerometer = sensormanager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        WindowManager mWindowManager = (WindowManager)
                act.getSystemService(WINDOW_SERVICE);

        display = mWindowManager.getDefaultDisplay();
        // does not work as advertised, always returns -1
        //displayformat = getWindow().getAttributes().format;
        displayformat = display.getPixelFormat();
        System.err.println("Display format = " + displayformat);
        System.err.println("RGB_565 = " + PixelFormat.RGB_565);
        System.err.println("RGB_888 = " + PixelFormat.RGB_888);
    }

    // android activity

    /**
     * for displaying progress info
     */
    String progress_message = "Loading files ...";
    /**
     * for displaying progress bar, value between 0.0 - 1.0
     */
    public double progress_bar = 0.0;

    String author_message = "JGame " + JGameVersionString;

    boolean is_initialised = false;


    class JGView extends View implements SensorEventListener {
        public JGView(Activity c) {
            super(c);
            bufpainter.setAntiAlias(true);
            bgpainter.setAntiAlias(true);
            bufpainter.setARGB(255, 255, 255, 255);
            bgpainter.setARGB(255, 255, 255, 255);
            setKeepScreenOn(true);
            setFocusable(true); // receive key events
        }

        /* standard view methods */

        /**
         * Canvas size is known: the cue to init the engine.
         */
        @Override
        protected void onSizeChanged(int w, int h, int oldw,
                                     int oldh) {
            init();
        }

        @Override
        protected void onDraw(Canvas g) {
            try {
                if (el.is_exited) {
                    //paintExitMessage(g);
                    return;
                }
                bufg = g;
                int clipx = 0;
                if (el.canvas_xofs > 0) clipx = el.canvas_xofs;
                int clipy = 0;
                if (el.canvas_yofs > 0) clipy = el.canvas_yofs;
                int clipwidth = getWidth();
                if (el.width < clipwidth) clipwidth = el.width;
                int clipheight = getHeight();
                if (el.height < clipheight) clipheight = el.height;
                g.clipRect(clipx, clipy, clipx + clipwidth, clipy + clipheight);
                if (!is_initialised) {
                    setFont(bufpainter, el.msg_font);
                    setColor(bufpainter, el.fg_color);
                    JGImage splash = el.existsImage("splash_image") ?
                            el.getImage("splash_image") : null;
                    if (splash != null) {
                        JGPoint splash_size = getImageSize("splash_image");
                        drawImage(bufg, viewWidth() / 2 - splash_size.x / 2,
                                Math.max(0, viewHeight() / 4 - splash_size.y / 2),
                                "splash_image",
                                false);
                    }
                    drawString(bufg, progress_message,
                            viewWidth() / 2, 3 * viewHeight() / 5, 0, false);
                    //if (canvas.progress_message!=null) {
                    //drawString(bufg,canvas.progress_message,
                    //		viewWidth()/2,2*viewHeight()/3,0);
                    //}
                    // paint the right hand side black in case the bar decreases
                    setColor(bufpainter, el.bg_color);
                    drawRect(bufg, (int) (viewWidth() * (0.1 + 0.8 * progress_bar)),
                            (int) (viewHeight() * 0.75),
                            (int) (viewWidth() * 0.8 * (1.0 - progress_bar)),
                            (int) (viewHeight() * 0.05), true, false, false, null, null);
                    // left hand side of bar
                    setColor(bufpainter, el.fg_color);
                    drawRect(bufg, (int) (viewWidth() * 0.1), (int) (viewHeight() * 0.75),
                            (int) (viewWidth() * 0.8 * progress_bar),
                            (int) (viewHeight() * 0.05), true, false, false, null, null);
                    // length stripes
				/*drawRect(bufg,(int)(viewWidth()*0.1), (int)(viewHeight()*0.6),
						(int)(viewWidth()*0.8),
						(int)(viewHeight()*0.008), true,false, false);
				drawRect(bufg,(int)(viewWidth()*0.1),
						(int)(viewHeight()*(0.6+0.046)),
						(int)(viewWidth()*0.8),
						(int)(viewHeight()*0.008), true,false, false);*/
                    drawString(bufg, author_message,
                            viewWidth() - 16, viewHeight() - getFontHeight(el.msg_font) - 10,
                            1, false);
                    return;
                }
                //bufg.setClip(Math.max(0,el.canvas_xofs),Math.max(0,el.canvas_yofs),
                //	Math.min(el.width,el.winwidth),
                //	Math.min(el.height,el.winheight) );
                // block update thread
                synchronized (el.objects) {
                    // paint any part of bg which is not yet defined
                    el.repaintBG(JGEngine.this);
                    /* clear buffer */
                    buf_gfx = bufg; // enable objects to draw on buffer gfx.
                    // Draw background to buffer.
                    // this part is the same across jre and midp.  Move it to
                    // EngineLogic?
                    //bufg.drawImage(background,-scaledtilex,-scaledtiley,this);
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
                    copyBGToBuf(bufg, sx1, sy1, sx2, sy2, 0, 0);
                    sx1 = 0;
                    sy1 = 0;
                    sx2 = tilexshift - 1;
                    sy2 = tileyshift - 1;
                    copyBGToBuf(bufg, sx1, sy1, sx2, sy2, bufmidx, bufmidy);
                    sx1 = 0;
                    sy1 = tileyshift + 1;
                    sx2 = tilexshift - 1;
                    sy2 = el.viewnrtilesy + 3;
                    if (sy2 - sy1 > el.viewnrtilesy) sy2 = sy1 + el.viewnrtilesy;
                    copyBGToBuf(bufg, sx1, sy1, sx2, sy2, bufmidx, 0);
                    sx1 = tilexshift + 1;
                    sy1 = 0;
                    sx2 = el.viewnrtilesx + 3;
                    sy2 = tileyshift - 1;
                    if (sx2 - sx1 > el.viewnrtilesx) sx2 = sx1 + el.viewnrtilesx;
                    copyBGToBuf(bufg, sx1, sy1, sx2, sy2, 0, bufmidy);
                    for (int i = 0; i < el.objects.size; i++) {
                        drawObject(bufg, (JGObject) el.objects.values[i]);
                    }
                    buf_gfx = null; // we're finished with the object drawing
                    /* draw status */
                    paintFrame(bufg);
                }
            } catch (JGameError e) {
                e.printStackTrace();
                exitEngine("Error during paint:\n"
                        + dbgExceptionToString(e));
            }
        }


        @Override
        public boolean onTouchEvent(MotionEvent me) {
            int action = me.getAction();
            double x = me.getX();
            double y = me.getY();
            // on a multitouch, touches after the first touch are also
            // considered mouse-down flanks.
            boolean press = action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_POINTER_DOWN;
            boolean down = press || action == MotionEvent.ACTION_MOVE;
            mouseposd_x = (x - el.canvas_xofs) / el.x_scale_fac;
            mouseposd_y = (y - el.canvas_yofs) / el.y_scale_fac;
            mousepos.x = (int) mouseposd_x;
            mousepos.y = (int) mouseposd_y;
            if (press) { // || !mouseinside) { // if press does not register
                mousebutton[1] = true;
                keymap[256] = true;
            }
            if (action == MotionEvent.ACTION_UP) {
                mouseinside = false;
                mousebutton[1] = false;
                keymap[256] = false;
            } else {
                mouseinside = true;
            }
            return true;
        }

        /**
         * Code taken from TouchPaint example. Can't test this yet.
         * Sensitivity should be a setting. Unclear how to test button press.
         */
        @Override
        public boolean onTrackballEvent(MotionEvent event) {
            int N = event.getHistorySize();
            final float scaleX = event.getXPrecision();
            final float scaleY = event.getYPrecision();
			/*for (int i=0; i<N; i++) {
				drawPoint(baseX+event.getHistoricalX(i)*scaleX,
						baseY+event.getHistoricalY(i)*scaleY,
						event.getHistoricalPressure(i),
						event.getHistoricalSize(i));
			}
			drawPoint(baseX+event.getX()*scaleX, baseY+event.getY()*scaleY,
					event.getPressure(), event.getSize());
			*/
            return true;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent msg) {
            // not sure how to handle shift key
            handleKey(keyCode, msg, true);
            return true;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent msg) {
            handleKey(keyCode, msg, false);
            return true;
        }

        private void handleKey(int key, KeyEvent msg, boolean state) {
            int keychar = msg.getUnicodeChar(0);
            int modkeychar = msg.getUnicodeChar();
            if (state) lastkeychar = (char) -1; // not defined
            char key_to_set = 0;
            // it appears not all KEYCODE_* are defined in older versions of
            // Android, so we just use the numeric constants
            switch (key) {
                case 19: //KeyEvent.KEYCODE_DPAD_UP :
                    key_to_set = KeyUp;
                    break;
                case 20: //KeyEvent.KEYCODE_DPAD_DOWN :
                    key_to_set = KeyDown;
                    break;
                case 21: //KeyEvent.KEYCODE_DPAD_LEFT :
                    key_to_set = KeyLeft;
                    break;
                case 22: //KeyEvent.KEYCODE_DPAD_RIGHT :
                    key_to_set = KeyRight;
                    break;
                case 59: //KeyEvent.KEYCODE_SHIFT_LEFT :
                case 60: //KeyEvent.KEYCODE_SHIFT_RIGHT :
                    key_to_set = KeyShift;
                    break;
                case 57: //KeyEvent.KEYCODE_ALT_LEFT :
                case 58: //KeyEvent.KEYCODE_ALT_RIGHT :
                    key_to_set = KeyAlt;
                    break;
                case 113: //KeyEvent.KEYCODE_CTRL_LEFT :
                case 114: //KeyEvent.KEYCODE_CTRL_RIGHT :
                    key_to_set = KeyCtrl;
                    break;
                case 111: //KeyEvent.KEYCODE_ESCAPE :
                    key_to_set = KeyEsc;
                    if (state) lastkeychar = 27;
                    break;
                case 66: //KeyEvent.KEYCODE_ENTER :
                case 160: //KeyEvent.KEYCODE_NUMPAD_ENTER :
                    key_to_set = KeyEnter;
                    if (state) lastkeychar = 10;
                    break;
                case 67: //KeyEvent.KEYCODE_DEL :
                    key_to_set = KeyBackspace;
                    if (state) lastkeychar = 8;
                    break;
                case 61: //KeyEvent.KEYCODE_TAB :
                    key_to_set = KeyTab;
                    if (state) lastkeychar = 9;
                    break;
                default:
                    if (keychar >= 'a' && keychar <= 'z') {
                        key_to_set = (char) (keychar - 'a' + 'A');
                        if (state) lastkeychar = (char) modkeychar;
                    } else if (keychar >= 32 && keychar <= 127) {
                        key_to_set = (char) keychar;
                        if (state) lastkeychar = (char) modkeychar;
                    }
            }
            if (key_to_set != 0) {
                keymap[key_to_set] = state;
                if (state) lastkey = key_to_set;
            }
            if (wakeup_key == -1 || keymap[wakeup_key]) {
                if (!running) {
                    start();
                    // key is cleared when it is used as wakeup key
                    if (wakeup_key != -1) keymap[wakeup_key] = false;
                }
            }

        }

        /* sensorlistener */

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            float x = 0, y = 0, z = 1;
            switch (display.getOrientation()) {
                case Surface.ROTATION_0:
                    x = event.values[0];
                    y = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    x = -event.values[1];
                    y = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    x = -event.values[0];
                    y = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    x = event.values[1];
                    y = -event.values[0];
                    break;
            }
            z = event.values[2];
            // handle values
            accelvec[0] = -x;
            accelvec[1] = y;
            accelvec[2] = z;
        }


        void setInitialised() {
            is_initialised = true;
            //initpainter=null;
        }


        public void clearCanvas() {
            // fill canvas with black
            //if (bufg==null) bufg = getGraphics();
            //bufg.setClip(0,0,getWidth(),getHeight());
            //setColor(bufg,JGColor.black);
            //bufg.fillRect(0,0,getWidth(),getHeight());
        }

        public void drawAll() {
            postInvalidate();
        }
    }

    /*=== jre applet emulation ===*/

    /**
     * Is there an android equivalent?
     */
    public String getParameter(String name) {
        return null;
    }


    /**
     * The current android entry point; size is determined by environment.
     */
    public void initEngineApplet() {
    }

    /**
     * Not implemented yet.
     */
    public void initEngine(int width, int height) {
        exitEngine("Use initEngineApplet");
    }

    /**
     * Not implemented yet.
     */
    public void initEngineComponent(int width, int height) {
        exitEngine("Use initEngineApplet");
    }

    /**
     * Call setCanvasSettings here.
     */
    public abstract void initCanvas();


    /**
     * initialise engine, or resize view is el.is_inited is true
     */
    void init() {
        boolean do_full_init = !el.is_inited;
        if (do_full_init) {
            storeInit();
        }
        // canvas size might change, we don't support this yet
        el.winwidth = canvas.getWidth();
        el.winheight = canvas.getHeight();
        // get all the dimensions
        if (do_full_init) {
            initCanvas();
            if (!el.view_initialised) {
                exitEngine("Canvas settings not initialised, use setCanvasSettings().");
            }
        }
        // init vars
        //canvas = new JGCanvas(el.winwidth,el.winheight);
        el.initPF();
        if (do_full_init) {
            clearKeymap();
            // set canvas padding color  (probably we need to draw an el.bg_color
            // rectangle)
            // determine default font size (unscaled)
            el.msg_font = new JGFont("Helvetica", 0,
                    (int) (25.0 / (640.0 / (el.tilex * el.nrtilesx))));
            if (!JGObject.setEngine(this)) {
                exitEngine("Another JGEngine is already running!");
            }
            el.is_inited = true;
        }
        // do something like setInitPainter here to init the loading screen
        // create background
        // XXX find display compatible bit depth
        background = Bitmap.createBitmap(el.width + 3 * el.scaledtilex,
                el.height + 3 * el.scaledtiley,
                AndroidImage.getPreferredBitmapFormat(displayformat));
        bgg = new Canvas();
        bgg.setBitmap(background);
        imagepainter.setAntiAlias(el.smooth_magnify);
        imagepainter.setFilterBitmap(el.smooth_magnify);
        simpleimagepainter.setAntiAlias(el.smooth_magnify);
        simpleimagepainter.setFilterBitmap(el.smooth_magnify);
        el.invalidateBGTiles();
        if (do_full_init) {
            gamethread = new Thread(new JGEngineThread());
            gamethread.start();
        }
    }

    private void clearKeymap() {
        for (int i = 0; i < 256 + 3; i++) keymap[i] = false;
    }


    abstract public void initGame();

    public void start() {
        // restart audio if possible?
        running = true;
    }

    public void stop() {
        // more rigorous than disableAudio but we can't be sure if the stop is
        // permanent (likely it is).
        stopAudio();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void wakeUpOnKey(int key) {
        wakeup_key = key;
    }

    public void destroyApp(boolean unconditional) {
        destroy();
    }

    public void startApp() {
    }

    public void pauseApp() {
    }

    /**
     * We should not call destroy in android because the app should keep
     * running all the time.
     */
    public void destroy() {
        // stop all samples (audio not implemented yet)
        stopAudio();
        // kill game thread
        el.is_exited = true;
        if (gamethread != null) {
            gamethread.interrupt();
            //try {
            //	gamethread.join(2000); // give up after 2 sec
            //} catch (InterruptedException e) {
            //	e.printStackTrace();
            //	// give up
            //}
        }
        // close files?? that appears to be unnecessary
        // reset global variables
        if (el.is_inited) {
            JGObject.setEngine(null);
        }
        System.out.println("JGame engine disposed.");
    }


    /**
     * exitEngine with error means fatal error. exitEngine without error
     * means quit to desktop. In Android, this means the app will continue,
     * only the the current activity will finish.
     */
    public void exitEngine(String msg) {
        // stop all samples (audio not implemented yet)
        stopAudio();
        if (msg != null) {
            System.err.println(msg);
            el.exit_message = msg;
            // display error to user
        } else {
            //System.err.println("Exiting JGEngine.");
            //destroy();
            currentact.finish();
        }
    }


    public void setCanvasSettings(int nrtilesx, int nrtilesy, int tilex, int tiley,
                                  JGColor fgcolor, JGColor bgcolor, JGFont msgfont) {
        el.nrtilesx = nrtilesx;
        el.nrtilesy = nrtilesy;
        el.viewnrtilesx = nrtilesx;
        el.viewnrtilesy = nrtilesy;
        el.tilex = tilex;
        el.tiley = tiley;
        setColorsFont(fgcolor, bgcolor, msgfont);
        el.view_initialised = true;
    }

    public void setScalingPreferences(double min_aspect_ratio, double
            max_aspect_ratio, int crop_top, int crop_left, int crop_bottom, int crop_right) {
        el.min_aspect = min_aspect_ratio;
        el.max_aspect = max_aspect_ratio;
        el.crop_top = crop_top;
        el.crop_left = crop_left;
        el.crop_bottom = crop_bottom;
        el.crop_right = crop_right;
    }

    public void setSmoothing(boolean smooth_magnify) {
        el.smooth_magnify = smooth_magnify;
        imagepainter.setAntiAlias(smooth_magnify);
        imagepainter.setFilterBitmap(smooth_magnify);
        simpleimagepainter.setAntiAlias(smooth_magnify);
        simpleimagepainter.setFilterBitmap(smooth_magnify);

    }


    /**
     * Engine thread, executing game action.
     */
    class JGEngineThread implements Runnable {
        private long target_time = 0; /* time at which next frame should start */
        private int frames_skipped = 0;

        public JGEngineThread() {
        }

        public void run() {
            try {
                try {
                    initGame();
                    Thread.sleep(500);
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new JGameError("Exception during initGame(): " + e);
                }
                canvas.setInitialised();
                canvas.clearCanvas();
                target_time = System.currentTimeMillis() + (long) (1000.0 / el.fps);
                while (!el.is_exited) {
                    //canvas.updateKeyState();
                    //if ((debugflags&MSGSINPF_DEBUG)!=0) refreshDbgFrameLogs();
                    long cur_time = System.currentTimeMillis();
                    if (!running) {
                        // wait in portions of 1/2 sec until running is set;
                        // reset target time
                        Thread.sleep(500);
                        target_time = cur_time + (long) (1000.0 / el.fps);
                    } else if (cur_time < target_time + 900.0 / el.fps) {
                        // we lag behind less than 0.9 frame
                        // -> do full frame.
                        synchronized (el.objects) {
                            doFrameAll();
                            el.updateViewOffset();
                        }
                        canvas.drawAll();
                        frames_skipped = 0;
                        if (cur_time + 3 < target_time) {
                            //we even have some time left -> sleep it away
                            Thread.sleep(target_time - cur_time);
                        } else {
                            // we don't, just yield to give input handler and
                            // painter some time
                            Thread.yield();
                        }
                        target_time += (1000.0 / el.fps);
                        //} else if (cur_time >
                        //target_time + (long)(1000.0*el.maxframeskip/el.fps)) {
                        //	// we lag behind more than the max # frames ->
                        //	// draw full frame and reset target time
                        //	synchronized (el.objects) {
                        //		doFrameAll();
                        //		el.updateViewOffset();
                        //	}
                        //	canvas.drawAll();
                        //	frames_skipped=0;
                        //	// yield to give input handler + painter some time
                        //	Thread.yield();
                        //	target_time=cur_time + (long)(1000.0/el.fps);
                    } else {
                        // we lag behind a little -> frame skip
                        synchronized (el.objects) {
                            doFrameAll();
                            el.updateViewOffset();
                        }
                        // if we skip too many frames in succession, draw a frame
                        if ((++frames_skipped) > el.maxframeskip) {
                            //canvas.repaint();
                            canvas.drawAll();
                            frames_skipped = 0;
                            target_time = cur_time + (long) (1000.0 / el.fps);
                        } else {
                            target_time += (long) (1000.0 / el.fps);
                        }
                        // yield to give input handler some time
                        Thread.yield();
                    }
                }
            } catch (InterruptedException e) {
                /* exit thread when interrupted */
                System.out.println("JGame thread exited.");
            } catch (Exception e) {
                dbgShowException("MAIN", e);
            } catch (JGameError e) {
                e.printStackTrace();
                exitEngine("Error in main:\n" + dbgExceptionToString(e));
            }
        }
    }




    /*====== variables from engine ======*/


    /**
     * Should engine thread run or halt? Set by start() / stop()
     */
    boolean running = true;


    Canvas buf_gfx = null;


    /*====== platform-dependent variables ======*/


    /* keyboard */

    /**
     * The codes 256-258 are the mouse buttons
     */
    boolean[] keymap = new boolean[256 + 3];
    int lastkey = 0;
    char lastkeychar = 0;
    int wakeup_key = 0;



    /*====== images ======*/


    public JGImage getImage(String imgname) {
        return el.getImage(imgname);
    }


    public JGPoint getImageSize(String imgname) {
        return el.getImageSize(imgname);
    }

    public void defineMedia(String filename) {
        //	el.defineMedia(this,filename);
        //}
        //public void defineMedia(JGEngineInterface eng,String filename) {
        // getResourceAsStream does not work in android
        int lnr = 1;
        int nr_lines = 0;
        try {
            InputStream instr = getAssets().open(filename);
            //InputStream instr = getClass().getResourceAsStream(filename);
            if (instr == null) exitEngine("Cannot open `" + filename + "'.");
            InputStreamReader in = new InputStreamReader(instr);
            if (in == null) exitEngine("Cannot open `" + filename + "'.");
            // count nr of lines in file first
            while (EngineLogic.readline(in) != null) nr_lines++;
            if (nr_lines == 0) exitEngine("Cannot open `" + filename + "'.");
            // now, read the file
            in = new InputStreamReader(getAssets().open(filename));
            String line;
            String[] fields = new String[14];
            while ((line = EngineLogic.readline(in)) != null) {
                setProgressBar((double) lnr / (double) nr_lines);
                int i = 0;
                Vector tokens = EngineLogic.tokenizeString(line, '\t');
                for (Enumeration e = tokens.elements(); e.hasMoreElements(); ) {
                    fields[i++] = (String) e.nextElement();
                }
                if (i == 8) {
                    defineImageMap(this,
                            fields[0], fields[1],
                            Integer.parseInt(fields[2]),
                            Integer.parseInt(fields[3]),
                            Integer.parseInt(fields[4]),
                            Integer.parseInt(fields[5]),
                            Integer.parseInt(fields[6]),
                            Integer.parseInt(fields[7]));
                } else if (i == 9) {
                    defineImage(fields[0], fields[1],
                            Integer.parseInt(fields[2]),
                            fields[3],
                            fields[4],
                            Integer.parseInt(fields[5]),
                            Integer.parseInt(fields[6]),
                            Integer.parseInt(fields[7]),
                            Integer.parseInt(fields[8]));
                } else if (i == 5) {
                    defineImage(fields[0], fields[1],
                            Integer.parseInt(fields[2]),
                            fields[3],
                            fields[4], -1, -1, -1, -1);
                } else if (i == 10) {
                    el.defineImage(fields[0], fields[1],
                            Integer.parseInt(fields[2]),
                            el.getSubImage(fields[3],
                                    Integer.parseInt(fields[4])),
                            fields[5],
                            Integer.parseInt(fields[6]),
                            Integer.parseInt(fields[7]),
                            Integer.parseInt(fields[8]),
                            Integer.parseInt(fields[9]));
                } else if (i == 6) {
                    el.defineImage(fields[0], fields[1],
                            Integer.parseInt(fields[2]),
                            el.getSubImage(fields[3],
                                    Integer.parseInt(fields[4])),
                            fields[5], -1, -1, -1, -1);
                } else if (i == 3) {
                    defineAnimation(fields[0], EngineLogic.splitList(fields[1]),
                            Double.parseDouble(fields[2]));
                } else if (i == 4) {
                    defineAnimation(fields[0], EngineLogic.splitList(fields[1]),
                            Double.parseDouble(fields[2]),
                            fields[3].equals("true"));
                } else if (i == 2) {
                    defineAudioClip(fields[0], fields[1]);
                }
                lnr++;
            }
        } catch (JGameError e) {
            exitEngine("Error in " + filename + " line " + lnr + ": " + e);
        } catch (Exception e) {
            exitEngine("Error in " + filename + " line " + lnr + ":\n"
                    + dbgExceptionToString(e));
        }

    }

    public void defineImage(String name, String tilename, int collisionid,
                            String imgfile, String img_op,
                            int top, int left, int width, int height) {
        //el.
        defineImage(this, name, tilename, collisionid, imgfile, img_op,
                top, left, width, height);
    }

    public void defineImage(String imgname, String tilename, int collisionid,
                            String imgfile, String img_op) {
        //el.
        defineImage(this, imgname, tilename, collisionid, imgfile, img_op,
                -1, -1, -1, -1);
    }

    public void defineImage(String imgname, String tilename, int collisionid,
                            String imgmap, int mapidx, String img_op,
                            int top, int left, int width, int height) {
        //el.
        defineImage(imgname, tilename, collisionid, imgmap, mapidx,
                img_op, top, left, width, height);
    }

    public void defineImage(String imgname, String tilename, int collisionid,
                            String imgmap, int mapidx, String img_op) {
        el.defineImage(imgname, tilename, collisionid, imgmap, mapidx, img_op);
    }

    public void defineImageRotated(String name, String tilename,
                                   int collisionid, String srcname, double angle) {
        el.defineImageRotated(this, name, tilename, collisionid, srcname, angle);
    }


    public void defineImageMap(String mapname, String imgfile,
                               int xofs, int yofs, int tilex, int tiley, int skipx, int skipy) {
        //el.
        defineImageMap(this, mapname, imgfile, xofs, yofs, tilex, tiley,
                skipx, skipy);
    }

    // replaces enginelogic.defineImage because path should not be prepended
    // XXX defineImageRotated also affected
    public void defineImage(Object pkg_obj, String name, String tilename,
                            int collisionid, String imgfile, String img_op,
                            int top, int left, int width, int height) {
        if (el.images_loaded.containsKey(name)
                && !el.images_loaded.get(name).equals(imgfile)) {
            // if associated file is not the same, undefine old image
            el.undefineImage(name);
        }
        JGImage img = null;
        if (!imgfile.equals("null")) {
            //imgfile = getAbsolutePath(pkg_obj,imgfile);
            img = el.imageutil.loadImage(imgfile);
            el.images_loaded.put(name, imgfile);
        }
        el.defineImage(name, tilename, collisionid, img,
                img_op, top, left, width, height);
    }

    // again the path problem
    public void defineImageMap(Object pkg_obj, String mapname, String imgfile,
                               int xofs, int yofs, int tilex, int tiley, int skipx, int skipy) {
        //imgfile = getAbsolutePath(pkg_obj,imgfile);
        el.imagemaps.put(mapname, new ImageMap(el.imageutil, imgfile, xofs, yofs,
                tilex, tiley, skipx, skipy));
    }


    public JGRectangle getImageBBox(String imgname) {
        return el.getImageBBox(imgname);
    }


    /*====== PF/view ======*/


    /*====== objects from canvas ======*/

    public void markAddObject(JGObject obj) {
        el.markAddObject(obj);
    }

    public boolean existsObject(String index) {
        return el.existsObject(index);
    }

    public JGObject getObject(String index) {
        return el.getObject(index);
    }

    public void moveObjects(String prefix, int cidmask) {
        el.moveObjects(this, prefix, cidmask);
    }

    public void moveObjects() {
        el.moveObjects(this);
    }

    public void checkCollision(int srccid, int dstcid) {
        el.checkCollision(this, srccid, dstcid);
    }

    public int checkCollision(int cidmask, JGObject obj) {
        return el.checkCollision(cidmask, obj);
    }

    public int checkBGCollision(JGRectangle r) {
        return el.checkBGCollision(r);
    }

    public void checkBGCollision(int tilecid, int objcid) {
        el.checkBGCollision(this, tilecid, objcid);
    }

    /* objects from engine */

    public Vector getObjects(String prefix, int cidmask, boolean suspended_obj,
                             JGRectangle bbox) {
        return el.getObjects(prefix, cidmask, suspended_obj,
                bbox);
    }

    public void removeObject(JGObject obj) {
        el.removeObject(obj);
    }

    public void removeObjects(String prefix, int cidmask) {
        el.removeObjects(prefix, cidmask);
    }

    public void removeObjects(String prefix, int cidmask, boolean suspended_obj) {
        el.removeObjects(prefix, cidmask, suspended_obj);
    }

    public int countObjects(String prefix, int cidmask) {
        return el.countObjects(prefix, cidmask);
    }

    public int countObjects(String prefix, int cidmask, boolean suspended_obj) {
        return el.countObjects(prefix, cidmask, suspended_obj);
    }


    void drawObject(Canvas g, JGObject o) {
        if (!o.is_suspended) {
            drawImage(g, (int) o.x, (int) o.y, o.getImageName(), true);
            try {
                o.paint();
            } catch (JGameError ex) {
                ex.printStackTrace();
                exitEngine(dbgExceptionToString(ex));
            } catch (Exception e) {
                dbgShowException(o.getName(), e);
            }
        }
        // debug functionality not implemented in midp
    }



    /*====== BG/tiles ======*/

    public void setBGImage(String bgimg) {
        el.setBGImage(bgimg, 0, true, true);
    }

    public void setBGImage(int depth, String bgimg, boolean wrapx, boolean wrapy) {
        el.setBGImage(bgimg, depth, wrapx, wrapy);
    }


    public void setTileSettings(String out_of_bounds_tile,
                                int out_of_bounds_cid, int preserve_cids) {
        el.setTileSettings(out_of_bounds_tile, out_of_bounds_cid, preserve_cids);
    }

    public void fillBG(String filltile) {
        el.fillBG(filltile);
    }

    public void setTileCid(int x, int y, int and_mask, int or_mask) {
        el.setTileCid(x, y, and_mask, or_mask);
    }

    public void setTile(int x, int y, String tilestr) {
        el.setTile(x, y, tilestr);
    }

    void setColor(Paint p, JGColor col) {
        p.setColor((col.alpha << 24) | (col.r << 16) | (col.g << 8) | col.b);
    }


    /**
     * xi,yi are tile indexes relative to the tileofs, that is, the top left
     * of the bg, + 1. They must be within both the tilemap and the view.
     */
    public void drawTile(int xi, int yi, int tileid) {
        if (background == null || bgg == null) return;
        // determine position within bg
        int x = el.moduloFloor(xi + 1, el.viewnrtilesx + 3) * el.scaledtilex;
        int y = el.moduloFloor(yi + 1, el.viewnrtilesy + 3) * el.scaledtiley;
        // draw
        Integer tileid_obj = new Integer(tileid);
        AndroidImage img = (AndroidImage) el.getTileImage(tileid_obj);
        // define background behind tile in case the tile is null or
        // transparent.
        if (img == null || el.images_transp.containsKey(tileid_obj)) {
            EngineLogic.BGImage bg_image = (EngineLogic.BGImage)
                    el.bg_images.elementAt(0);
            if (bg_image == null) {
                setColor(bgpainter, el.bg_color);
                bgg.drawRect(new Rect(x, y, x + el.scaledtilex, y + el.scaledtiley),
                        bgpainter);
            } else {
                int xtile = el.moduloFloor(xi, bg_image.tiles.x);
                int ytile = el.moduloFloor(yi, bg_image.tiles.y);
                bgg.drawBitmap(
                        ((AndroidImage) el.getImage(bg_image.imgname)).img,
                        new Rect(xtile * el.scaledtilex, ytile * el.scaledtiley,
                                (xtile + 1) * el.scaledtilex, (ytile + 1) * el.scaledtiley),
                        new Rect(x, y, x + el.scaledtilex, y + el.scaledtiley),
                        bgpainter);
            }
        }
        if (img != null) {
            bgg.drawBitmap(img.img, x, y, bgpainter);
        }
        //System.out.println("Drawn tile"+tileid);
    }


    public int countTiles(int tilecidmask) {
        return el.countTiles(tilecidmask);
    }

    public int getTileCid(int xidx, int yidx) {
        return el.getTileCid(xidx, yidx);
    }

    public String getTileStr(int xidx, int yidx) {
        return el.getTileStr(xidx, yidx);
    }

    public int getTileCid(JGRectangle tiler) {
        return el.getTileCid(tiler);
    }

    public JGRectangle getTiles(JGRectangle r) {
        return el.getTiles(r);
    }

    public boolean getTiles(JGRectangle dest, JGRectangle r) {
        return el.getTiles(dest, r);
    }


    public void setTileCid(int x, int y, int value) {
        el.setTileCid(x, y, value);
    }

    public void orTileCid(int x, int y, int or_mask) {
        el.orTileCid(x, y, or_mask);
    }

    public void andTileCid(int x, int y, int and_mask) {
        el.andTileCid(x, y, and_mask);
    }

    public void setTile(JGPoint tileidx, String tilename) {
        el.setTile(tileidx, tilename);
    }

    public void setTiles(int xofs, int yofs, String[] tilemap) {
        el.setTiles(xofs, yofs, tilemap);
    }

    public void setTilesMulti(int xofs, int yofs, String[] tilemap) {
        el.setTilesMulti(xofs, yofs, tilemap);
    }

    public int getTileCidAtCoord(double x, double y) {
        return el.getTileCidAtCoord(x, y);
    }

    public int getTileCid(JGPoint center, int xofs, int yofs) {
        return el.getTileCid(center, xofs, yofs);
    }

    public String getTileStrAtCoord(double x, double y) {
        return el.getTileStrAtCoord(x, y);
    }

    public String getTileStr(JGPoint center, int xofs, int yofs) {
        return el.getTileStr(center, xofs, yofs);
    }

    public int tileStrToID(String tilestr) {
        return el.tileStrToID(tilestr);
    }

    public String tileIDToStr(int tileid) {
        return el.tileIDToStr(tileid);
    }


    /**
     * dx1 and dy1 are coordinates on canvas buffer, without canvas_ofs.
     */
    void copyBGToBuf(Canvas bufg, int sx1, int sy1, int sx2, int sy2,
                     int dx1, int dy1) {
        //dx1 += el.canvas_xofs;
        //dy1 += el.canvas_yofs;
        //System.out.println("("+sx1+","+sy1+")-("+sx2+","+sy2+")");
        if (sx2 <= sx1 || sy2 <= sy1) return;
        int barrelx = el.scaleXPos(el.moduloFloor(el.xofs, el.tilex), false);
        int barrely = el.scaleYPos(el.moduloFloor(el.yofs, el.tiley), false);
        int barreldx = (sx1 == 0) ? barrelx : 0;
        int barreldy = (sy1 == 0) ? barrely : 0;
        barrelx = (sx1 == 0) ? 0 : barrelx;
        barrely = (sy1 == 0) ? 0 : barrely;
        int dx2 = dx1 + sx2 - sx1;
        int dy2 = dy1 + sy2 - sy1;
        // ensure source coordinates are not out of the bounds of the source
        // image
        int sx1e = barrelx + sx1 * el.scaledtilex;
        int sy1e = barrely + sy1 * el.scaledtiley;
        int sx2e = barrelx + sx2 * el.scaledtilex;
        int sx2max = (el.viewnrtilesx + 3) * el.scaledtilex;
        if (sx2e > sx2max) sx2e = sx2max;
        int sy2e = barrely + sy2 * el.scaledtiley;
        int sy2max = (el.viewnrtilesy + 3) * el.scaledtiley;
        if (sy2e > sy2max) sy2e = sy2max;
        //void drawRegion(Image src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int anchor)
        bufg.drawBitmap(background,
                new Rect(sx1e, sy1e, sx2e, sy2e),
                //barrelx+sx1*el.scaledtilex, barrely+sy1*el.scaledtiley,
                //(sx2-sx1)*el.scaledtilex, (sy2-sy1)*el.scaledtiley,
                new Rect(dx1 * el.scaledtilex - barreldx + el.canvas_xofs,
                        dy1 * el.scaledtiley - barreldy + el.canvas_yofs,
                        dx1 * el.scaledtilex - barreldx + el.canvas_xofs + sx2e - sx1e,
                        dy1 * el.scaledtiley - barreldy + el.canvas_yofs + sy2e - sy1e),
                simpleimagepainter);
        //bufg.drawImage(background,
        //	dx1*el.scaledtilex-barreldx, dy1*el.scaledtiley-barreldy,
        //	dx2*el.scaledtilex-barreldx, dy2*el.scaledtiley-barreldy,
        //	barrelx+sx1*el.scaledtilex, barrely+sy1*el.scaledtiley,
        //	barrelx+sx2*el.scaledtilex, barrely+sy2*el.scaledtiley,
        //	this);
    }



    /*====== math ======*/


    public double moduloXPos(double x) {
        return el.moduloXPos(x);
    }

    public double moduloYPos(double y) {
        return el.moduloYPos(y);
    }


    /*====== debug ======*/

    public void dbgShowBoundingBox(boolean enabled) {
    }

    public void dbgShowGameState(boolean enabled) {
    }

    public void dbgShowFullStackTrace(boolean enabled) {
    }

    public void dbgShowMessagesInPf(boolean enabled) {
    }

    public void dbgSetMessageExpiry(int ticks) {
    }

    public void dbgSetMessageFont(JGFont font) {
    }

    public void dbgSetDebugColor1(JGColor col) {
    }

    public void dbgSetDebugColor2(JGColor col) {
    }

    public void dbgPrint(String msg) {
        dbgPrint("MAIN", msg);
    }

    public void dbgPrint(String source, String msg) {
        System.out.println(source + ": " + msg);
    }

    public void dbgShowException(String source, Throwable e) {
        e.printStackTrace();
        //dbgPrint(source,st.toString());
    }

    public String dbgExceptionToString(Throwable e) {
        return e.toString();
    }


    //public void setCanvasSettings(int nrtilesx,int nrtilesy,int tilex,int tiley,
    //Color fgcolor, Color bgcolor, Font msgfont) {
    //	el.setCanvasSettings(nrtilesx,nrtilesy,tilex,tiley,
    //		fgcolor, bgcolor, msgfont);
    //}

    public void requestGameFocus() {
    }

    // note: these get and set methods do not delegate calls

    public boolean isApplet() {
        return false;
    }

    public boolean isMidlet() {
        return false;
    }

    public boolean isAndroid() {
        return true;
    }

    public boolean isOpenGL() {
        return false;
    }

    public int viewWidth() {
        return el.viewnrtilesx * el.tilex;
    }

    public int viewHeight() {
        return el.viewnrtilesy * el.tiley;
    }

    public int viewTilesX() {
        return el.viewnrtilesx;
    }

    public int viewTilesY() {
        return el.viewnrtilesy;
    }

    public int viewXOfs() {
        return el.pendingxofs;
    }

    public int viewYOfs() {
        return el.pendingyofs;
    }

    //public int viewTileXOfs() { return canvas.tilexofs; }
    //public int viewTileYOfs() { return canvas.tileyofs; }

    public int pfWidth() {
        return el.nrtilesx * el.tilex;
    }

    public int pfHeight() {
        return el.nrtilesy * el.tiley;
    }

    public int pfTilesX() {
        return el.nrtilesx;
    }

    public int pfTilesY() {
        return el.nrtilesy;
    }

    public boolean pfWrapX() {
        return el.pf_wrapx;
    }

    public boolean pfWrapY() {
        return el.pf_wrapy;
    }

    public int tileWidth() {
        return el.tilex;
    }

    public int tileHeight() {
        return el.tiley;
    }

    public int displayWidth() {
        return el.winwidth;
    }

    public int displayHeight() {
        return el.winheight;
    }

    public double getFrameRate() {
        return el.fps;
    }

    public double getGameSpeed() {
        return el.gamespeed;
    }

    public double getFrameSkip() {
        return el.maxframeskip;
    }

    public boolean getVideoSyncedUpdate() {
        return false;
    }

    public int getOffscreenMarginX() {
        return el.offscreen_margin_x;
    }

    public int getOffscreenMarginY() {
        return el.offscreen_margin_y;
    }

    public double getXScaleFactor() {
        return el.x_scale_fac;
    }

    public double getYScaleFactor() {
        return el.y_scale_fac;
    }

    public double getMinScaleFactor() {
        return el.min_scale_fac;
    }


    public void setViewOffset(int xofs, int yofs, boolean centered) {
        el.setViewOffset(xofs, yofs, centered);
    }

    public void setBGImgOffset(int depth, double xofs, double yofs,
                               boolean centered) {
    }

    public void setViewZoomRotate(double zoom, double rotate) {
    }

    public void setPFSize(int nrtilesx, int nrtilesy) {
        el.setPFSize(nrtilesx, nrtilesy);
    }

    public void setPFWrap(boolean wrapx, boolean wrapy, int shiftx, int shifty) {
        el.setPFWrap(wrapx, wrapy, shiftx, shifty);
    }


    public void setFrameRate(double fps, double maxframeskip) {
        el.setFrameRate(fps, maxframeskip);
    }

    public void setVideoSyncedUpdate(boolean value) {
    }

    public void setGameSpeed(double gamespeed) {
        el.setGameSpeed(gamespeed);
    }

    public void setRenderSettings(int alpha_thresh, JGColor render_bg_col) {
        el.setRenderSettings(alpha_thresh, render_bg_col);
    }

    public void setOffscreenMargin(int xmargin, int ymargin) {
        el.setOffscreenMargin(xmargin, ymargin);
    }


    /**
     * Set global background colour, which is displayed in borders, and behind
     * transparent tiles if no BGImage is defined.
     */
    public void setBGColor(JGColor bgcolor) {
        el.bg_color = bgcolor;
    }

    /**
     * Set global foreground colour, used for printing text and status
     * messages.  It is also the default colour for painting
     */
    public void setFGColor(JGColor fgcolor) {
        el.fg_color = fgcolor;
    }

    /**
     * Set the (unscaled) message font, used for displaying status messages.
     * It is also the default font for painting.
     */
    public void setMsgFont(JGFont msgfont) {
        el.msg_font = msgfont;
    }

    /**
     * Set foreground and background colour, and message font in one go;
     * passing a null means ignore that argument.
     */
    public void setColorsFont(JGColor fgcolor, JGColor bgcolor, JGFont msgfont) {
        if (msgfont != null) el.msg_font = msgfont;
        if (fgcolor != null) el.fg_color = fgcolor;
        if (bgcolor != null) setBGColor(bgcolor);
    }

    /**
     * Set parameters of outline surrounding text (for example, used to
     * increase contrast).
     *
     * @param thickness 0 = turn off outline
     */
    public void setTextOutline(int thickness, JGColor colour) {
        // curiously, I've seen the init screen draw in-between these two
        // statements.  Check of if that's what really happened
        el.outline_colour = colour;
        el.outline_thickness = thickness;
    }

    /**
     * Unimplemented, does nothing.
     */
    public void setMouseCursor(int cursor) {
    }

    /**
     * Unimplemented, does nothing.
     */
    public void setMouseCursor(Object cursor) {
    }


    /* timers */

    public void removeAllTimers() {
        el.removeAllTimers();
    }

    public void registerTimer(JGTimer timer) {
        el.registerTimer(timer);
    }

    /* game state */

    public void setGameState(String state) {
        el.setGameState(state);
    }

    public void addGameState(String state) {
        el.addGameState(state);
    }

    public void removeGameState(String state) {
        el.removeGameState(state);
    }

    public void clearGameState() {
        el.clearGameState();
    }


    public boolean inGameState(String state) {
        return el.inGameState(state);
    }

    public boolean inGameStateNextFrame(String state) {
        return el.inGameStateNextFrame(state);
    }

    /**
     * Do some administration, call doFrame.
     */
    private void doFrameAll() {
        //audioNewFrame();
        // the first flush is needed to remove any objects that were created
        // in the main routine after the last moveObjects or checkCollision
        el.flushRemoveList();
        el.flushAddList();
        // tick timers before doing state transitions, because timers may
        // initiate new transitions.
        el.tickTimers();
        el.flushRemoveList();
        el.flushAddList();
        // the game state transition starts here
        el.gamestate.removeAllElements();
        int maxi = el.gamestate_nextframe.size();
        for (int i = 0; i < maxi; i++) {
            el.gamestate.addElement(el.gamestate_nextframe.elementAt(i));
        }
        // we assume that state transitions will not initiate new state
        // transitions!
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

    private void invokeGameStateMethods(String prefix, Vector states) {
        int maxi = states.size();
        for (int i = 0; i < maxi; i++) {
            String state = (String) states.elementAt(i);
            tryMethod(prefix, state);
        }
    }

    Hashtable methodidx = null;

    String[] stateprefixes = new String[]{
            "start", "doFrame", "paintFrame"
    };

    String[] statesuffixes = new String[]{
            "Loader",
            "Title",
            "SelectLevel",
            "Highscores",
            "InGame",
            "StartLevel",
            "StartGame",
            "LevelDone",
            "LifeLost",
            "GameOver",
            "EnterHighscore",
            "Paused",
    };


    /**
     * Try to invoke parameterless method in this object, used for game state
     * methods.  In MIDP, we don't have
     * reflection, so we only support a fixed number of methods.  Override
     * this to add new methods.
     */
    public void tryMethod(String prefix, String suffix) {
        int prefidx, sufidx;
        for (prefidx = 0; prefidx < stateprefixes.length; prefidx++) {
            if (stateprefixes[prefidx].equals(prefix)) break;
        }
        for (sufidx = 0; sufidx < statesuffixes.length; sufidx++) {
            if (statesuffixes[sufidx].equals(suffix)) break;
        }
        if (sufidx >= statesuffixes.length)
            exitEngine("Game state " + suffix + " not supported!");
        int idx = statesuffixes.length * prefidx + sufidx;
        //if (methodidx==null) {
        //	methodidx = new Hashtable();
        //	for (int i=0; i<statemethods.length; i++) {
        //		methodidx.put(statemethods[i],new Integer(i));
        //	}
        //}
        //Integer idx_int = (Integer)methodidx.get(name);
        //if (idx_int==null)
        //	exitEngine("Game state method "+name+" not supported!");
        switch (idx) {
            case 0:
                startLoader();
                break;
            case 1:
                startTitle();
                break;
            case 2:
                startSelectLevel();
                break;
            case 3:
                startHighscores();
                break;
            case 4:
                startInGame();
                break;
            case 5:
                startStartLevel();
                break;
            case 6:
                startStartGame();
                break;
            case 7:
                startLevelDone();
                break;
            case 8:
                startLifeLost();
                break;
            case 9:
                startGameOver();
                break;
            case 10:
                startEnterHighscore();
                break;
            case 11:
                startEnterHighscore();
                break;
            case 12:
                doFrameLoader();
                break;
            case 13:
                doFrameTitle();
                break;
            case 14:
                doFrameSelectLevel();
                break;
            case 15:
                doFrameHighscores();
                break;
            case 16:
                doFrameInGame();
                break;
            case 17:
                doFrameStartLevel();
                break;
            case 18:
                doFrameStartGame();
                break;
            case 19:
                doFrameLevelDone();
                break;
            case 20:
                doFrameLifeLost();
                break;
            case 21:
                doFrameGameOver();
                break;
            case 22:
                doFrameEnterHighscore();
                break;
            case 23:
                doFramePaused();
                break;
            case 24:
                paintFrameLoader();
                break;
            case 25:
                paintFrameTitle();
                break;
            case 26:
                paintFrameSelectLevel();
                break;
            case 27:
                paintFrameHighscores();
                break;
            case 28:
                paintFrameInGame();
                break;
            case 29:
                paintFrameStartLevel();
                break;
            case 30:
                paintFrameStartGame();
                break;
            case 31:
                paintFrameLevelDone();
                break;
            case 32:
                paintFrameLifeLost();
                break;
            case 33:
                paintFrameGameOver();
                break;
            case 34:
                paintFrameEnterHighscore();
                break;
            case 35:
                paintFramePaused();
                break;
            default:
                exitEngine("Game state method " + prefix + suffix + " not supported");
        }
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startLoader() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startTitle() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startSelectLevel() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startHighscores() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startInGame() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startStartLevel() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startStartGame() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startLevelDone() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startLifeLost() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startGameOver() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startEnterHighscore() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void startPaused() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameLoader() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameTitle() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameSelectLevel() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameHighscores() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameInGame() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameStartLevel() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameStartGame() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameLevelDone() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameLifeLost() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameGameOver() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFrameEnterHighscore() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void doFramePaused() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameLoader() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameTitle() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameSelectLevel() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameHighscores() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameInGame() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameStartLevel() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameStartGame() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameLevelDone() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameLifeLost() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameGameOver() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFrameEnterHighscore() {
    }

    /**
     * Predefined game state method, implementation is empty.
     */
    public void paintFramePaused() {
    }


    public void doFrame() {
    }

    void paintFrame(Canvas g) {
        buf_gfx = g;
        setColor(bufpainter, el.fg_color);
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
        //if ((debugflags&GAMESTATE_DEBUG)!=0) {
        //	String state="{";
        //	for (Enumeration e=el.gamestate.elements(); e.hasMoreElements(); ) {
        //		state += (String)e.nextElement();
        //		if (e.hasMoreElements()) state +=",";
        //	}
        //	state += "}";
        //	setFont(el.msg_font);
        //	setColor(g,el.fg_color);
        //	drawString(state,el.viewWidth(),
        //			el.viewHeight()-(int)getFontHeight(g,el.msg_font), 1);
        //}
        //if ((debugflags&MSGSINPF_DEBUG)!=0) paintDbgFrameLogs(buf_gfx);
        buf_gfx = null;
    }

    public void paintFrame() {
    }

    public Canvas getBufferGraphics() {
        return buf_gfx;
    }

    /* some convenience functions for drawing during repaint and paintFrame()*/

    public void setColor(JGColor col) {
        // XXX setColor cannot check if graphics is available
        //if (buf_gfx!=null)
        setColor(bufpainter, col);
    }

    public void setFont(JGFont font) {
        setFont(bufpainter, font);
    }

    public void setFont(Paint p, JGFont jgfont) {
        double fontsize = jgfont.size * el.min_scale_fac;
        p.setTextSize((int) fontsize);
        //Font font = new Font(jgfont.name,jgfont.style,(int)jgfont.size);
        //if (g!=null) {
        //	double origsize = font.getSize2D();
        //	font=font.deriveFont((float)(origsize*el.min_scale_fac));
        //	g.setFont(font);
        //}
    }

    public void setStroke(double thickness) {
        bufpainter.setStrokeWidth((float) (thickness * el.min_scale_fac));
    }

    public void setBlendMode(int src_func, int dst_func) {
    }

    public double getFontHeight(JGFont jgfont) {
        // TODO get actual size?
        return jgfont.size * el.min_scale_fac;
    }

    void drawImage(Canvas g, double x, double y, String imgname,
                   boolean pf_relative) {
        //drawImage(x,y,imgname,null,1.0,0.0,1.0,pf_relative);
        if (g == null) return;
        if (imgname == null) return;
        x = el.scaleXPos(x, pf_relative) + el.canvas_xofs;
        y = el.scaleYPos(y, pf_relative) + el.canvas_yofs;
        AndroidImage img = (AndroidImage) el.getImage(imgname);
        if (img != null)
            g.drawBitmap(img.img, (int) x, (int) y, simpleimagepainter);
    }


    public void drawLine(double x1, double y1, double x2, double y2,
                         double thickness, JGColor color) {
        if (color != null) setColor(color);
        setStroke(thickness);
        drawLine(x1, y1, x2, y2, true);
    }

    public void drawLine(double x1, double y1, double x2, double y2) {
        drawLine(x1, y1, x2, y2, true);
    }

    public void drawLine(double x1, double y1, double x2, double y2,
                         boolean pf_relative) {
        if (buf_gfx == null) return;
        buf_gfx.drawLine(
                el.scaleXPos(x1, pf_relative) + el.canvas_xofs,
                el.scaleYPos(y1, pf_relative) + el.canvas_yofs,
                el.scaleXPos(x2, pf_relative) + el.canvas_xofs,
                el.scaleYPos(y2, pf_relative) + el.canvas_yofs, bufpainter);
    }

    private final int[] xpos = new int[3];
    private final int[] ypos = new int[3];

    public void drawPolygon(double[] x, double[] y, JGColor[] col, int len,
                            boolean filled, boolean pf_relative) {
        if (buf_gfx == null) return;
        xpos[0] = el.scaleXPos(x[0], pf_relative) + el.canvas_xofs;
        ypos[0] = el.scaleYPos(y[0], pf_relative) + el.canvas_yofs;
        xpos[1] = el.scaleXPos(x[1], pf_relative) + el.canvas_xofs;
        ypos[1] = el.scaleYPos(y[1], pf_relative) + el.canvas_yofs;
        if (!filled) {
            // draw first and last line segment
            xpos[2] = el.scaleXPos(x[len - 1], pf_relative) + el.canvas_xofs;
            ypos[2] = el.scaleYPos(y[len - 1], pf_relative) + el.canvas_yofs;
            if (col != null) setColor(bufpainter, col[1]);
            buf_gfx.drawLine(xpos[0], ypos[0], xpos[1], ypos[1], bufpainter);
            if (col != null) setColor(bufpainter, col[0]);
            buf_gfx.drawLine(xpos[2], ypos[2], xpos[0], ypos[0], bufpainter);
        }
        if (filled) {
            bufpainter.setStyle(Paint.Style.FILL);
        } else {
            bufpainter.setStyle(Paint.Style.STROKE);
        }
        for (int i = 2; i < len; i++) {
            xpos[2] = el.scaleXPos(x[i], pf_relative) + el.canvas_xofs;
            ypos[2] = el.scaleYPos(y[i], pf_relative) + el.canvas_yofs;
            if (col != null) setColor(bufpainter, col[i]);
            if (filled) {
                bufpainter.setStyle(Paint.Style.FILL);
                fillTriangle(buf_gfx, xpos[0], ypos[0], xpos[1], ypos[1], xpos[2], ypos[2]);
            } else {
                buf_gfx.drawLine(xpos[1], ypos[1], xpos[2], ypos[2], bufpainter);
            }
            xpos[1] = xpos[2];
            ypos[1] = ypos[2];
        }
        bufpainter.setStyle(Paint.Style.STROKE);
    }

    private void fillTriangle(Canvas g, int x1, int y1, int x2, int y2,
                              int x3, int y3) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        g.drawPath(p, bufpainter);
    }

    public void drawRect(double x, double y, double width, double height, boolean filled,
                         boolean centered, double thickness, JGColor color) {
        if (color != null) setColor(color);
        setStroke(thickness);
        drawRect(buf_gfx, x, y, width, height, filled, centered, true, null, null);
    }

    public void drawRect(double x, double y, double width, double height, boolean filled,
                         boolean centered) {
        drawRect(buf_gfx, x, y, width, height, filled, centered, true, null, null);
    }

    public void drawRect(double x, double y, double width, double height, boolean filled,
                         boolean centered, boolean pf_relative) {
        drawRect(buf_gfx, x, y, width, height, filled, centered, pf_relative, null, null);
    }

    public void drawRect(double x, double y, double width, double height,
                         boolean filled, boolean centered, boolean pf_relative,
                         JGColor[] shadecol) {
        drawRect(buf_gfx, x, y, width, height, filled, centered, pf_relative, shadecol, null);
    }

    public void drawRect(double x, double y, double width, double height,
                         boolean filled, boolean centered, boolean pf_relative,
                         JGColor[] shadecol, String tileimage) {
        drawRect(buf_gfx, x, y, width, height, filled, centered, pf_relative, shadecol, tileimage);
    }

    void drawRect(Canvas g, double x, double y, double width, double height,
                  boolean filled, boolean centered, boolean pf_relative,
                  JGColor[] shadecol, String tileimage) {
        if (centered) {
            x -= (width / 2);
            y -= (height / 2);
        }
        JGRectangle r = el.scalePos(x, y, width, height, pf_relative);
        r.x += el.canvas_xofs;
        r.y += el.canvas_yofs;
        if (tileimage != null) {
            Bitmap b = ((AndroidImage) el.getImage(tileimage)).img;
            Matrix m = new Matrix();
            m.setTranslate(r.x, r.y);
            Shader sh = new BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            sh.setLocalMatrix(m);
            bufpainter.setShader(sh);
        }
        if (filled || tileimage != null) {
            bufpainter.setStyle(Paint.Style.FILL);
        } else {
            bufpainter.setStyle(Paint.Style.STROKE);
        }
        g.drawRect(new Rect(r.x, r.y, r.x + r.width, r.y + r.height), bufpainter);
        // reset state
        bufpainter.setShader(null);
        bufpainter.setStyle(Paint.Style.STROKE);
    }

    public void drawOval(double x, double y, double width, double height, boolean filled,
                         boolean centered, double thickness, JGColor color) {
        if (color != null) setColor(color);
        setStroke(thickness);
        drawOval(x, y, width, height, filled, centered, true);
    }

    public void drawOval(double x, double y, double width, double height, boolean filled,
                         boolean centered) {
        drawOval(x, y, width, height, filled, centered, true);
    }

    public void drawOval(double x, double y, double width, double height, boolean filled,
                         boolean centered, boolean pf_relative) {
        if (buf_gfx == null) return;
        x = el.scaleXPos(x, pf_relative) + el.canvas_xofs + 0.5;
        y = el.scaleYPos(y, pf_relative) + el.canvas_yofs + 0.5;
        width = el.scaleXPos(width, false);
        height = el.scaleYPos(height, false);
        if (centered) {
            x -= (width / 2.0);
            y -= (height / 2.0);
        }
        if (filled) {
            bufpainter.setStyle(Paint.Style.FILL);
        } else {
            bufpainter.setStyle(Paint.Style.STROKE);
        }
        buf_gfx.drawOval(new RectF(
                        (int) x, (int) y, (int) x + (int) width, (int) y + (int) height),
                bufpainter);
        // reset state
        bufpainter.setStyle(Paint.Style.STROKE);
    }

    /* new versions of drawImage */

    public void drawImage(String imgname, double x, double y) {
        drawImage(x, y, imgname);
    }

    public void drawImage(String imgname, double x, double y, boolean pf_relative) {
        drawImage(x, y, imgname, pf_relative);
    }

    public void drawImage(String imgname, double x, double y,
                          boolean pf_relative, JGColor blend_col,
                          double alpha, double rot, double scale) {
        drawImage(x, y, imgname, blend_col, alpha, rot, scale, pf_relative);
    }


    public void drawImage(double x, double y, String imgname) {
        if (buf_gfx == null) return;
        drawImage(buf_gfx, x, y, imgname, true);
    }

    public void drawImage(double x, double y, String imgname, boolean pf_relative) {
        if (buf_gfx == null) return;
        drawImage(buf_gfx, x, y, imgname, pf_relative);
    }

    // static variables to avoid object creations
    JGPoint drawImage_size = new JGPoint(0, 0);
    Matrix drawImage_m = new Matrix();

    public void drawImage(double x, double y, String imgname, JGColor blend_col,
                          double alpha, double rot, double scale, boolean pf_relative) {
        if (buf_gfx == null) return;
        if (blend_col == null && alpha == 1.0 && rot == 0.0 && scale == 1.0) {
            drawImage(buf_gfx, x, y, imgname, pf_relative);
            return;
        }
        if (imgname == null) return;
        int alpha_int = (int) Math.floor(255.95 * alpha);
        imagepainter.setColor((alpha_int << 24) | 0xffffff);
        if (blend_col != null) {
            imagepainter.setColorFilter(
                    new LightingColorFilter(
                            (blend_col.r << 16) | (blend_col.g << 8) | blend_col.b, 0));
            //imagepainter.setColor( (alpha_int<<24)
            //	| (blend_col.r<<16) | (blend_col.g<<8)  | blend_col.b);
        } else {
            imagepainter.setColorFilter(null);
        }
        x = el.scaleXPos(x, pf_relative) + el.canvas_xofs;
        y = el.scaleYPos(y, pf_relative) + el.canvas_yofs;
        AndroidImage img = (AndroidImage) el.getImageOrig(imgname);
        JGPoint origsize = img.getSize();
        //System.err.println(" "+origsize.x+" "+origsize.y);
        drawImage_size.x = el.scaleXPos(origsize.x, false);
        drawImage_size.y = el.scaleYPos(origsize.y, false);
        // size of scaled image
        double txsize_s = drawImage_size.x * scale;
        double tysize_s = drawImage_size.y * scale;
        // determine topleft of scaled image
        //System.err.println(""+drawImage_size.x+" "+drawImage_size.y+" "+txsize_s+" "+tysize_s);
        //x -= (txsize_s - drawImage_size.x)/2;
        //y -= (tysize_s - drawImage_size.y)/2;
        float pivotx = (float) (origsize.x / 2.0);
        float pivoty = (float) (origsize.y / 2.0);
        drawImage_m.reset();
        drawImage_m.setRotate((float) ((-rot * 180) / Math.PI), pivotx, pivoty);
        drawImage_m.postScale((float) scale, (float) scale, pivotx, pivoty);
        drawImage_m.postScale((float) el.x_scale_fac, (float) el.y_scale_fac, 0, 0);
        drawImage_m.postTranslate((float) x,
                (float) y);
        if (img != null)
            buf_gfx.drawBitmap(img.img, drawImage_m, imagepainter);
    }

    public void drawString(String str, double x, double y, int align,
                           JGFont font, JGColor color) {
        if (font != null) setFont(font);
        if (color != null) setColor(color);
        drawString(buf_gfx, str, x, y, align, false);
    }

    public void drawString(String str, double x, double y, int align) {
        drawString(buf_gfx, str, x, y, align, false);
    }

    public void drawString(String str, double x, double y, int align,
                           boolean pf_relative) {
        drawString(buf_gfx, str, x, y, align, pf_relative);
    }

    /**
     * Internal function for writing on both buffer and screen.  Coordinates
     * are always relative to view.
     */
    void drawString(Canvas g, String str, double x, double y, int align,
                    boolean pf_relative) {
        if (g == null) return;
        if (str.equals("")) return;
        x = el.scaleXPos(x, pf_relative) + el.canvas_xofs;
        y = el.scaleYPos(y, pf_relative) + el.canvas_yofs;
        y -= bufpainter.ascent();
        Paint.Align palign = Paint.Align.CENTER;
        if (align == -1) palign = Paint.Align.LEFT;
        if (align == 1) palign = Paint.Align.RIGHT;
        bufpainter.setTextAlign(palign);
        if (el.outline_thickness > 0) {
            int origcol = bufpainter.getColor();
            setColor(el.outline_colour);
            int real_thickness = Math.max(
                    el.scaleXPos(el.outline_thickness, false), 1);
            for (int i = -real_thickness; i <= real_thickness; i++) {
                if (i == 0) continue;
                g.drawText(str, 0, str.length(), (int) x + i, (int) y, bufpainter);
            }
            for (int i = -real_thickness; i <= real_thickness; i++) {
                if (i == 0) continue;
                g.drawText(str, 0, str.length(), (int) x, (int) y + i, bufpainter);
            }
            bufpainter.setColor(origcol);
        }
        // stroke width make text bolder so we set it to 0
        float origwidth = bufpainter.getStrokeWidth();
        bufpainter.setStrokeWidth(0);
        g.drawText(str, 0, str.length(), (int) x, (int) y, bufpainter);
        bufpainter.setStrokeWidth(origwidth);
    }

    public void drawImageString(String string, double x, double y, int align,
                                String imgmap, int char_offset, int spacing) {
        el.drawImageString(this, string, x, y, align, imgmap, char_offset, spacing,
                false);
    }

    public void drawImageString(String string, double x, double y, int align,
                                String imgmap, int char_offset, int spacing, boolean pf_relative) {
        el.drawImageString(this, string, x, y, align, imgmap, char_offset, spacing,
                pf_relative);
    }

    /* input */

    /**
     * Android has mouse semantics based on touch screen. They are the same
     * as MIDP. Touch screens have only press, drag, and release (no mouse
     * move without button press).  There is only one "button", button 1.
     * Mouseinside is set to false when finger/stylus is not on the touch
     * screen.
     */

    JGPoint mousepos = new JGPoint(0, 0);
    double mouseposd_x = 0, mouseposd_y = 0;
    boolean[] mousebutton = new boolean[]{false, false, false, false};
    boolean mouseinside = false;

    double[] accelvec = new double[]{0, 0, 1};

    /**
     * XXX: does not produce a clone of mousepos, for efficiency reasons!
     */
    public JGPoint getMousePos() {
        return mousepos;
    }

    public int getMouseX() {
        return mousepos.x;
    }

    public int getMouseY() {
        return mousepos.y;
    }

    // XXX android only as yet
    public double getMouseXD() {
        return mouseposd_x;
    }

    public double getMouseYD() {
        return mouseposd_y;
    }

    public boolean getMouseButton(int nr) {
        return mousebutton[nr];
    }

    public void clearMouseButton(int nr) {
        mousebutton[nr] = false;
    }

    public void setMouseButton(int nr) {
        mousebutton[nr] = true;
    }

    public boolean getMouseInside() {
        return mouseinside;
    }

    public boolean getKey(int key) {
        return keymap[key];
    }

    public void clearKey(int key) {
        keymap[key] = false;
    }

    public void setKey(int key) {
        keymap[key] = true;
    }

    public int getLastKey() {
        return lastkey;
    }

    public char getLastKeyChar() {
        return lastkeychar;
    }

    public void clearLastKey() {
        lastkey = 0;
        lastkeychar = 0;
    }

    /* maybe move the translations? Should they be system dependent? */

    /**
     * Get a printable string describing the key. Non-static version for the
     * sake of the interface.
     */
    public String getKeyDesc(int key) {
        return getKeyDescStatic(key);
    }

    /**
     * Get a printable string describing the key.
     */
    public static String getKeyDescStatic(int key) {
        if (key == 32) return "#";
        if (key == 0) return "(none)";
        if (key == KeyEnter) return "*";
        if (key == KeyStar) return "*";
        if (key == ' ') return "#";
        if (key == KeyPound) return "#";
        //if (key==KeyEsc) return "escape";
        if (key == KeyUp) return "cursor up";
        if (key == KeyDown) return "cursor down";
        if (key == KeyLeft) return "cursor left";
        if (key == KeyRight) return "cursor right";
        if (key == KeyShift) return "fire";
        //if (key==KeyAlt) return "alt";
        //if (key==KeyCtrl) return "control";
        //if (key==KeyMouse1) return "left mouse button";
        //if (key==KeyMouse2) return "middle mouse button";
        //if (key==KeyMouse3) return "right mouse button";
        //if (key==27) return "escape";
        if (key >= 33 && key <= 95)
            return new String(new char[]{(char) key});
        return "keycode " + key;
    }

    /**
     * Obtain key code from printable string describing the key, the inverse
     * of getKeyDesc. The string is trimmed and lowercased. Non-static version
     * for the sake of the interface.
     */
    public int getKeyCode(String keydesc) {
        return getKeyCodeStatic(keydesc);
    }

    /**
     * Obtain key code from printable string describing the key, the inverse
     * of getKeyDesc. The string is trimmed and lowercased.
     */
    public static int getKeyCodeStatic(String keydesc) {
        // tab, enter, backspace, insert, delete, home, end, pageup, pagedown
        // escape
        keydesc = keydesc.toLowerCase().trim();
        //if (keydesc.equals("space")) {
        //	return 32;
        //} else if (keydesc.equals("escape")) {
        //	return KeyEsc;
        //} else
        if (keydesc.equals("(none)")) {
            return 0;
            //} else if (keydesc.equals("enter")) {
            //	return KeyEnter;
        } else if (keydesc.equals("cursor up")) {
            return KeyUp;
        } else if (keydesc.equals("cursor down")) {
            return KeyDown;
        } else if (keydesc.equals("cursor left")) {
            return KeyLeft;
        } else if (keydesc.equals("cursor right")) {
            return KeyRight;
            //} else if (keydesc.equals("shift")) {
            //	return KeyShift;
        } else if (keydesc.equals("fire")) {
            return KeyFire;
        } else if (keydesc.equals("star")) {
            return '*';
        } else if (keydesc.equals("pound")) {
            return '#';
            //} else if (keydesc.equals("alt")) {
            //	return KeyAlt;
            //} else if (keydesc.equals("control")) {
            //	return KeyCtrl;
            //} else if (keydesc.equals("left mouse button")) {
            //	return KeyMouse1;
            //} else if (keydesc.equals("middle mouse button")) {
            //	return KeyMouse2;
            //} else if (keydesc.equals("right mouse button")) {
            //	return KeyMouse3;
        } else if (keydesc.startsWith("keycode")) {
            return Integer.parseInt(keydesc.substring(7));
        } else if (keydesc.length() == 1) {
            return keydesc.charAt(0);
        }
        return 0;
    }

    public boolean hasAccelerometer() {
        return true;
    }

    public double getAccelX() {
        return accelvec[0];
    }

    public double getAccelY() {
        return accelvec[1];
    }

    public double getAccelZ() {
        return accelvec[2];
    }

    public double[] getAccelVec() {
        return new double[]{accelvec[0], accelvec[1], accelvec[2]};
    }


    /*====== animation ======*/

    public void defineAnimation(String id,
                                String[] frames, double speed) {
        el.defineAnimation(id, frames, speed);
    }

    public void defineAnimation(String id,
                                String[] frames, double speed, boolean pingpong) {
        el.defineAnimation(id, frames, speed, pingpong);
    }

    public Animation getAnimation(String id) {
        return el.getAnimation(id);
    }

    public String getConfigPath(String filename) {
        return null;
        // not implemented yet.  midp should use the special configuration
        // features for this.
    }

    public int invokeUrl(String url, String target) {
        // XXX application state is reset
        canvas.post(currentact.new UrlInvoker(url));
        return -1;
    }

//	void paintExitMessage(Canvas g) { try {
//		setFont(g,debugmessage_font);
//		int height = (int) (getFontHeight(g,null) / el.y_scale_fac);
//		// clear background
//		setColor(g,el.bg_color);
//		drawRect(g, el.viewWidth()/2, el.viewHeight()/2,
//			9*el.viewWidth()/10, height*5, true,true, false);
//		setColor(g,debug_auxcolor2);
//		// draw colour bars
//		drawRect(g, el.viewWidth()/2, el.viewHeight()/2 - 5*height/2,
//			9*viewWidth()/10, 5, true,true, false);
//		drawRect(g, el.viewWidth()/2, el.viewHeight()/2 + 5*height/2,
//			9*viewWidth()/10, 5, true,true, false);
//		setColor(g,el.fg_color);
//		int ypos = el.viewHeight()/2 - 3*height/2;
//		StringTokenizer toker = new StringTokenizer(el.exit_message,"\n");
//		while (toker.hasMoreTokens()) {
//			drawString(g,toker.nextToken(),el.viewWidth()/2,ypos,0, false);
//			ypos += height+1;
//		}
//	} catch(java.lang.NullPointerException e) {
//		// this sometimes happens during drawString when the applet is exiting
//		// but calls repaint while the graphics surface is already disposed.
//		// See also bug 4791314:
//		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4791314
//	} }




    /* computation */

    public boolean and(int value, int mask) {
        return el.and(value, mask);
    }

    public double random(double min, double max) {
        return el.random(min, max);
    }

    public double random(double min, double max, double interval) {
        return el.random(min, max, interval);
    }

    public int random(int min, int max, int interval) {
        return el.random(min, max, interval);
    }

    public double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public JGPoint getTileIndex(double x, double y) {
        return el.getTileIndex(x, y);
    }

    public JGPoint getTileCoord(int tilex, int tiley) {
        return el.getTileCoord(tilex, tiley);
    }

    public JGPoint getTileCoord(JGPoint tileidx) {
        return el.getTileCoord(tileidx);
    }

    public double snapToGridX(double x, double gridsnapx) {
        return el.snapToGridX(x, gridsnapx);
    }

    public double snapToGridY(double y, double gridsnapy) {
        return el.snapToGridY(y, gridsnapy);
    }

    public void snapToGrid(JGPoint p, int gridsnapx, int gridsnapy) {
        el.snapToGrid(p, gridsnapx, gridsnapy);
    }

    public boolean isXAligned(double x, double margin) {
        return el.isXAligned(x, margin);
    }

    public boolean isYAligned(double y, double margin) {
        return el.isYAligned(y, margin);
    }

    public double getXAlignOfs(double x) {
        return el.getXAlignOfs(x);
    }

    public double getYAlignOfs(double y) {
        return el.getYAlignOfs(y);
    }

    // XXX please test these two methods

    public double getXDist(double x1, double x2) {
        return el.getXDist(x1, x2);
    }

    public double getYDist(double y1, double y2) {
        return el.getYDist(y1, y2);
    }

    // android sound is handled through soundpool which matches the jgame
    // system closely. We make sure the audio api is not touched until a sound
    // is actually played.

    boolean audioenabled = true;

    // create on demand
    SoundPool soundpool = null;

    int cursound = -1; // -1 indicates no sound

    Hashtable clipidtosoundid = new Hashtable(); /* String->Int */

    Hashtable channelidtostreamid = new Hashtable(); /* String->Int */

    Hashtable streamidtoclipid = new Hashtable(); /* Int->String */

    //MediaPlayer player=null;

    private void ensureSoundpoolExists() {
        if (soundpool == null) {
            soundpool = new SoundPool(8, AudioManager.STREAM_MUSIC, 0);
        }
    }

    public void enableAudio() {
        if (audioenabled) return;
        audioenabled = true;
        resumeAudio();
    }

    void resumeAudio() {
        ensureSoundpoolExists();
        for (Enumeration e = channelidtostreamid.elements(); e.hasMoreElements(); ) {
            Integer streamid = (Integer) e.nextElement();
            soundpool.resume(streamid.intValue());
        }
        // android 2.2 only
        //soundpool.autoResume();
    }

    public void disableAudio() {
        if (!audioenabled) return;
        audioenabled = false;
        pauseAudio();
    }

    void pauseAudio() {
        ensureSoundpoolExists();
        for (Enumeration e = channelidtostreamid.elements(); e.hasMoreElements(); ) {
            Integer streamid = (Integer) e.nextElement();
            soundpool.pause(streamid.intValue());
        }
        // android 2.2 only
        //soundpool.autoPause();
    }

    public void defineAudioClip(String clipid, String filename) {
        ensureSoundpoolExists();
        // getAbsolutePath should not prepend package path
        el.audioclips.put(clipid, filename);
        //el.defineAudioClip(this,clipid,filename);
        try {
            AssetFileDescriptor fd = assets.openFd(filename);
            int soundid = soundpool.load(fd, 1/*priority*/);
            clipidtosoundid.put(clipid, new Integer(soundid));
        } catch (IOException e) {
            throw new Error("Sound asset '" + filename + "' could not be loaded");
        }
    }

    public String lastPlayedAudio(String channel) {
        Integer streamid = (Integer) channelidtostreamid.get(channel);
        return (String) streamidtoclipid.get(streamid);
    }

    public void playAudio(String clipid) {
        playAudio(null, clipid, false);
    }

    public void playAudio(String channel, String clipid, boolean loop) {
        ensureSoundpoolExists();
        if (!audioenabled) return;
        // stop previous sound on channel
        if (channel != null) {
            Integer prevstreamid = (Integer) channelidtostreamid.get(channel);
            if (prevstreamid != null) {
                soundpool.stop(prevstreamid.intValue());
            }
        }
        int soundid = ((Integer) clipidtosoundid.get(clipid)).intValue();
        int streamid = soundpool.play(soundid, 0.9f, 0.9f,
                loop ? 1 : 0,
                loop ? -1 : 0,
                1.0f);
        if (channel != null) {
            channelidtostreamid.put(channel, new Integer(streamid));
        }
        // this code opens asset using mediaplayer. current problems:
        // sound cut off too early
        //String filename = (String)el.audioclips.get(clipid);
        //try {
        //AssetFileDescriptor fd = assets.openFd(filename);
        //player = new MediaPlayer();
        //player.setDataSource(fd.getFileDescriptor(),
        //	fd.getStartOffset(), fd.getLength() );
        //fd.close();
        //player.prepare();
        //player.start();
        //} catch (IOException e) {
        //	throw new Error("Asset '"+filename+"' not found");
        //}

    }

    public void stopAudio() {
        for (Enumeration e = channelidtostreamid.keys(); e.hasMoreElements(); ) {
            String channelid = (String) e.nextElement();
            stopAudio(channelid);
        }
        // stop mediaplayer
        //if (player!=null) {
        //	player.stop();
        //	player.release();
        //	player=null;
        //}
    }

    public void stopAudio(String channel) {
        ensureSoundpoolExists();
        Integer streamid = (Integer) channelidtostreamid.get(channel);
        if (streamid == null) return;
        soundpool.stop(streamid.intValue());
    }

    /**
     * I found SharedPreferences already contains some predefined values
     * (such as "volume") so we prefix all keys with a special prefix.
     */
    final static String STORE_PREFIX = "JG_";


    private void storeInit() {
        // nothing needs to be done
    }

    void storeWriteBoolean(String id, boolean value) {
        SharedPreferences.Editor spe =
                PreferenceManager.getDefaultSharedPreferences(currentact).edit();
        spe.putBoolean(STORE_PREFIX + id, value);
        spe.commit();
    }

    public void storeWriteInt(String id, int value) {
        SharedPreferences.Editor spe =
                PreferenceManager.getDefaultSharedPreferences(currentact).edit();
        spe.putInt(STORE_PREFIX + id, value);
        spe.commit();
    }

    public void storeWriteDouble(String id, double value) {
        SharedPreferences.Editor spe =
                PreferenceManager.getDefaultSharedPreferences(currentact).edit();
        spe.putFloat(STORE_PREFIX + id, (float) value);
        spe.commit();
    }

    public void storeWriteString(String id, String value) {
        SharedPreferences.Editor spe =
                PreferenceManager.getDefaultSharedPreferences(currentact).edit();
        spe.putString(STORE_PREFIX + id, value);
        spe.commit();
    }

    public void storeRemove(String id) {
        SharedPreferences.Editor spe =
                PreferenceManager.getDefaultSharedPreferences(currentact).edit();
        spe.remove(STORE_PREFIX + id);
        spe.commit();
    }

    public boolean storeExists(String id) {
        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(currentact);
        return sp.contains(STORE_PREFIX + id);
    }

    public int storeReadInt(String id, int undef) {
        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(currentact);
        try {
            return sp.getInt(STORE_PREFIX + id, undef);
        } catch (ClassCastException e1) {
            try {
                return sp.getBoolean(STORE_PREFIX + id, false) ? 1 : 0;
            } catch (ClassCastException e2) {
                try {
                    return (int) sp.getFloat(STORE_PREFIX + id, (float) undef);
                } catch (ClassCastException e3) {
                    return (int) Float.parseFloat(sp.getString(STORE_PREFIX + id,
                            "0"));
                }
            }
        }
    }

    public double storeReadDouble(String id, double undef) {
        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(currentact);
        try {
            return sp.getFloat(STORE_PREFIX + id, (float) undef);
        } catch (ClassCastException e1) {
            try {
                return sp.getBoolean(STORE_PREFIX + id, false) ? 1 : 0;
            } catch (ClassCastException e2) {
                try {
                    return sp.getInt(STORE_PREFIX + id, (int) undef);
                } catch (ClassCastException e3) {
                    return (int) Float.parseFloat(sp.getString(STORE_PREFIX + id,
                            "0"));
                }
            }
        }
    }

    public String storeReadString(String id, String undef) {
        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(currentact);
        try {
            return sp.getString(STORE_PREFIX + id, undef);
        } catch (ClassCastException e1) {
            try {
                return sp.getBoolean(STORE_PREFIX + id, false) ? "1" : "0";
            } catch (ClassCastException e2) {
                try {
                    return "" + sp.getInt(STORE_PREFIX + id, 0);
                } catch (ClassCastException e3) {
                    return "" + sp.getFloat(STORE_PREFIX + id, 0);
                }
            }
        }
    }

    /*====== options ======*/

    /** The Android implementation uses the standard settings objects, which
     * use types that do not quite match the JGame types.  This is solved by
     * using automatic type coercion when reading settings and a typecast when
     * the settings menu is created. */

    /**
     * Integer (setting ID) - Setting
     */
    Hashtable settings_hash = new Hashtable();
    Vector settings_array = new Vector(20, 20);

    private void addSetting(Object key, Setting s) {
        settings_hash.put(key, s);
        settings_array.add(s);
    }

    public void optsAddTitle(String title) {
        settings_array.addElement(new SettingsTitle(title));
    }

    public void optsAddNumber(String varname, String title, String desc,
                              int decimals, double lower, double upper, double step, double initial) {
        //if (decimals==0) {
        //	if (!storeExists(varname)) storeWriteInt(varname,(int)initial);
        //} else {
        if (!storeExists(varname)) storeWriteDouble(varname, initial);
        //}
        addSetting(new Integer(Setting.next_id),
                new NumberSetting(varname, title, desc, decimals, lower, upper, step));
    }

    public void optsAddBoolean(String varname, String title, String desc,
                               boolean initial) {
        if (!storeExists(varname)) storeWriteBoolean(varname, initial);
        addSetting(new Integer(Setting.next_id),
                new BooleanSetting(varname, title, desc));
    }

    public void optsAddEnum(String varname, String title, String desc,
                            String[] values, int initial) {
        if (!storeExists(varname)) storeWriteString(varname, "" + initial);
        addSetting(new Integer(Setting.next_id),
                new EnumSetting(varname, title, desc, values));
    }

    public void optsAddKey(String varname, String title, String desc, int initial) {
        if (!storeExists(varname)) storeWriteInt(varname, initial);
        addSetting(new Integer(Setting.next_id),
                new KeySetting(varname, title, desc));
    }

    public void optsAddString(String varname, String title, String desc,
                              int maxlen, boolean isPassword, String initial) {
        if (!storeExists(varname)) storeWriteString(varname, initial);
        addSetting(new Integer(Setting.next_id),
                new StringSetting(varname, title, desc, maxlen, isPassword));
    }

    public void optsClear() {
        settings_hash = new Hashtable();
        settings_array = new Vector(20, 20);
    }

}

abstract class Setting {
    public static int next_id = JGActivity.SETTINGS_SUBMENU;
    public String varname;
    public String title, desc;
    public int id = 0;

    public Setting(String varname, String title, String desc) {
        this.varname = varname;
        this.title = title;
        this.desc = desc;
        id = next_id;
    }

    abstract void addMenuItem(JGEngine eng, PreferenceActivity context,
                              PreferenceCategory cat);
}

class SettingsTitle {
    public String titledesc;

    public SettingsTitle(String titledesc) {
        this.titledesc = titledesc;
    }
}

class NumberSetting extends Setting {
    public int decimals;
    public double lower, upper, step;

    public NumberSetting(String varname, String title, String desc, int decimals,
                         double lower, double upper, double step) {
        super(varname, title, desc);
        this.decimals = decimals;
        this.lower = lower;
        this.upper = upper;
        this.step = step;
        next_id++;
    }

    void addMenuItem(JGEngine eng, PreferenceActivity context, PreferenceCategory cat) {
        SeekBarPreference sbpref = new SeekBarPreference(context, this);
        sbpref.setKey(JGEngine.STORE_PREFIX + varname);
        sbpref.setTitle(title);
        sbpref.setSummary(desc);
        //sbpref.setMax((int)upper);
        cat.addPreference(sbpref);
    }
}

class BooleanSetting extends Setting {
    public BooleanSetting(String varname, String title, String desc) {
        super(varname, title, desc);
        next_id++;
    }

    void addMenuItem(JGEngine eng, PreferenceActivity context,
                     PreferenceCategory cat) {
        // make sure key is of right type
        // XXX is a race condition possible?
        eng.storeWriteBoolean(JGEngine.STORE_PREFIX + varname,
                eng.storeReadInt(JGEngine.STORE_PREFIX + varname, 0) != 0);
        // Toggle preference
        CheckBoxPreference togglePref = new CheckBoxPreference(context);
        togglePref.setKey(JGEngine.STORE_PREFIX + varname);
        togglePref.setTitle(title);
        togglePref.setSummary(desc);
        cat.addPreference(togglePref);
    }
}

class EnumSetting extends Setting {
    public String[] values;

    public EnumSetting(String varname, String title, String desc, String[] values) {
        super(varname, title, desc);
        this.values = values;
        next_id += values.length;
    }

    void addMenuItem(JGEngine eng, PreferenceActivity context,
                     PreferenceCategory cat) {
        String[] valueidxes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            valueidxes[i] = "" + i;
        }
        // make sure key is of right type
        // XXX is a race condition possible?
        eng.storeWriteString(JGEngine.STORE_PREFIX + varname,
                eng.storeReadString(JGEngine.STORE_PREFIX + varname, ""));
        // List preference
        ListPreference listPref = new ListPreference(context);
        listPref.setEntries(values);
        listPref.setEntryValues(valueidxes);
        listPref.setDialogTitle(title);
        listPref.setKey(JGEngine.STORE_PREFIX + varname);
        listPref.setTitle(title);
        listPref.setSummary(desc);
        cat.addPreference(listPref);
    }
}

class KeySetting extends Setting {
    public KeySetting(String varname, String title, String desc) {
        super(varname, title, desc);
        next_id++;
    }

    void addMenuItem(JGEngine eng, PreferenceActivity context,
                     PreferenceCategory cat) {
        // XXX not implemented yet!
    }
}

class StringSetting extends Setting {
    public int maxlen;
    public boolean isPassword;

    public StringSetting(String varname, String title, String desc, int maxlen,
                         boolean isPassword) {
        super(varname, title, desc);
        this.maxlen = maxlen;
        this.isPassword = isPassword;
        next_id++;
    }

    void addMenuItem(JGEngine eng, PreferenceActivity context,
                     PreferenceCategory cat) {
        // Edit text preference
        EditTextPreference editTextPref = new EditTextPreference(context);
        editTextPref.setDialogTitle(title);
        editTextPref.setKey(JGEngine.STORE_PREFIX + varname);
        editTextPref.setTitle(title);
        editTextPref.setSummary(desc);
        cat.addPreference(editTextPref);
    }
}

