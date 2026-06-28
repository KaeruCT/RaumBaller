package jgame.platform;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import jgame.JGColor;
import jgame.JGImage;
import jgame.JGPoint;

/** Browser Canvas-backed JGImage implementation. */
class BrowserImage implements JGImage {
    final JSObject image;
    private JGPoint size;

    BrowserImage() {
        this.image = null;
    }

    BrowserImage(JSObject image) {
        this.image = image;
    }

    BrowserImage(int width, int height) {
        this.image = null;
        this.size = new JGPoint(width, height);
    }

    @Override
    public JGImage loadImage(String imgfile) {
        if (JGEngine.isHeadless()) {
            return loadHeadlessImage(imgfile);
        }
        JSObject loaded = getPreloadedImage(imgfile);
        if (loaded == null) {
            throw new Error("Image asset '" + imgfile + "' was not preloaded");
        }
        return new BrowserImage(loaded);
    }

    @Override
    public void purgeImage(String imgfile) {
    }

    @Override
    public JGPoint getSize() {
        if (size == null) {
            size = new JGPoint(getWidth(image), getHeight(image));
        }
        return size;
    }

    @Override
    public boolean isOpaque(int alpha_thresh) {
        if (image == null) return false;
        return isOpaque(image, alpha_thresh);
    }

    @Override
    public JGImage rotate(int angle) {
        int normalized = ((angle / 90) % 4 + 4) % 4;
        if (normalized == 0) return this;
        if (image == null) {
            JGPoint s = getSize();
            return normalized == 2 ? new BrowserImage(s.x, s.y) : new BrowserImage(s.y, s.x);
        }
        return new BrowserImage(rotate(image, normalized));
    }

    @Override
    public JGImage rotateAny(double angle) {
        if (image == null) {
            JGPoint s = getSize();
            int d = Math.max(Math.max(s.x, s.y), (int) (0.75 * (s.x + s.y)));
            return new BrowserImage(d, d);
        }
        return new BrowserImage(rotateAny(image, angle));
    }

    @Override
    public JGImage flip(boolean horiz, boolean vert) {
        if (!horiz && !vert) return this;
        if (image == null) {
            JGPoint s = getSize();
            return new BrowserImage(s.x, s.y);
        }
        return new BrowserImage(flip(image, horiz, vert));
    }

    @Override
    public JGImage scale(int width, int height) {
        if (image == null) return new BrowserImage(width, height);
        return new BrowserImage(scale(image, width, height));
    }

    @Override
    public JGImage crop(int x, int y, int width, int height) {
        if (image == null) return new BrowserImage(width, height);
        return new BrowserImage(crop(image, x, y, width, height));
    }

    @Override
    public JGImage toDisplayCompatible(int thresh, JGColor bg_col, boolean fast, boolean bitmask) {
        return this;
    }

    private static BrowserImage loadHeadlessImage(String imgfile) {
        InputStream in = null;
        try {
            in = new FileInputStream(headlessAssetPath(imgfile));
            byte[] header = new byte[24];
            int read = 0;
            while (read < header.length) {
                int count = in.read(header, read, header.length - read);
                if (count < 0) break;
                read += count;
            }
            if (read < 24 || header[0] != (byte) 0x89 || header[1] != 0x50 || header[2] != 0x4e || header[3] != 0x47) {
                throw new Error("Image asset '" + imgfile + "' is not a PNG");
            }
            int width = readPngInt(header, 16);
            int height = readPngInt(header, 20);
            return new BrowserImage(width, height);
        } catch (IOException e) {
            throw new Error("Image asset '" + imgfile + "' could not be read", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String headlessAssetPath(String path) {
        String userDir = System.getProperty("user.dir");
        String prefix = userDir != null && userDir.endsWith("web") ? "../app/src/main/assets/" : "app/src/main/assets/";
        return prefix + path;
    }

    private static int readPngInt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24)
                | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8)
                | (data[offset + 3] & 0xff);
    }

    @JSBody(params = {"path"}, script = "return window.RaumBallerAssets && window.RaumBallerAssets.images[path] || null;")
    private static native JSObject getPreloadedImage(String path);

    @JSBody(params = {"image"}, script = "return image.width || image.naturalWidth || 0;")
    static native int getWidth(JSObject image);

    @JSBody(params = {"image"}, script = "return image.height || image.naturalHeight || 0;")
    static native int getHeight(JSObject image);

    @JSBody(params = {"width", "height"}, script = "var c=document.createElement('canvas'); c.width=width; c.height=height; return c;")
    static native JSObject createCanvas(int width, int height);

    @JSBody(params = {"image", "x", "y", "width", "height"}, script =
            "var c=document.createElement('canvas'); c.width=width; c.height=height;" +
            "var g=c.getContext('2d'); g.imageSmoothingEnabled=false;" +
            "g.drawImage(image, x, y, width, height, 0, 0, width, height); return c;")
    private static native JSObject crop(JSObject image, int x, int y, int width, int height);

    @JSBody(params = {"image", "width", "height"}, script =
            "var c=document.createElement('canvas'); c.width=width; c.height=height;" +
            "var g=c.getContext('2d'); g.imageSmoothingEnabled=false;" +
            "g.drawImage(image, 0, 0, width, height); return c;")
    private static native JSObject scale(JSObject image, int width, int height);

    @JSBody(params = {"image", "horiz", "vert"}, script =
            "var w=image.width||image.naturalWidth, h=image.height||image.naturalHeight;" +
            "var c=document.createElement('canvas'); c.width=w; c.height=h; var g=c.getContext('2d'); g.imageSmoothingEnabled=false;" +
            "g.translate(horiz ? w : 0, vert ? h : 0); g.scale(horiz ? -1 : 1, vert ? -1 : 1);" +
            "g.drawImage(image, 0, 0); return c;")
    private static native JSObject flip(JSObject image, boolean horiz, boolean vert);

    @JSBody(params = {"image", "turns"}, script =
            "var w=image.width||image.naturalWidth, h=image.height||image.naturalHeight;" +
            "var c=document.createElement('canvas'); if (turns === 2) { c.width=w; c.height=h; } else { c.width=h; c.height=w; }" +
            "var g=c.getContext('2d'); g.imageSmoothingEnabled=false;" +
            "if (turns === 1) { g.translate(h, 0); g.rotate(Math.PI/2); }" +
            "else if (turns === 2) { g.translate(w, h); g.rotate(Math.PI); }" +
            "else { g.translate(0, w); g.rotate(3*Math.PI/2); }" +
            "g.drawImage(image, 0, 0); return c;")
    private static native JSObject rotate(JSObject image, int turns);

    @JSBody(params = {"image", "angle"}, script =
            "var w=image.width||image.naturalWidth, h=image.height||image.naturalHeight; var d=Math.max(Math.max(w,h),0.75*(w+h));" +
            "var c=document.createElement('canvas'); c.width=d; c.height=d; var g=c.getContext('2d'); g.imageSmoothingEnabled=false;" +
            "g.translate(d/2,d/2); g.rotate(-angle); g.drawImage(image, -w/2, -h/2); return c;")
    private static native JSObject rotateAny(JSObject image, double angle);

    @JSBody(params = {"image", "alphaThresh"}, script =
            "var w=image.width||image.naturalWidth, h=image.height||image.naturalHeight;" +
            "var c=document.createElement('canvas'); c.width=w; c.height=h; var g=c.getContext('2d'); g.drawImage(image,0,0);" +
            "var data=g.getImageData(0,0,w,h).data; for (var i=3;i<data.length;i+=4) if (data[i] < alphaThresh) return false; return true;")
    private static native boolean isOpaque(JSObject image, int alphaThresh);
}
