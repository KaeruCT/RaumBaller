package com.kaeruct.raumballer.web;

import com.kaeruct.raumballer.AndroidGame;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import jgame.JGPoint;

public final class WebLauncher {
    private static AndroidGame game;

    @JSFunctor
    interface TraceSupplier extends JSObject {
        String get();
    }

    @JSFunctor
    interface Stepper extends JSObject {
        void step(int frames);
    }

    @JSFunctor
    interface PointerInput extends JSObject {
        void set(double x, double y, boolean down, boolean inside);
    }

    @JSFunctor
    interface KeyInput extends JSObject {
        void set(int key, boolean down, int ch);
    }

    @JSFunctor
    interface ScenarioRunner extends JSObject {
        String run(String name);
    }

    @JSFunctor
    interface Painter extends JSObject {
        void paint();
    }

    private WebLauncher() {
    }

    public static void main(String[] args) {
        game = new AndroidGame(new JGPoint(0, 0));
        installTestHooks(new TraceSupplier() {
            public String get() {
                return game.traceState();
            }
        }, new Stepper() {
            public void step(int frames) {
                for (int i = 0; i < frames; i++) game.stepFrameForTest();
            }
        }, new PointerInput() {
            public void set(double x, double y, boolean down, boolean inside) {
                game.setPointer(x, y, down, inside);
            }
        }, new KeyInput() {
            public void set(int key, boolean down, int ch) {
                game.setBrowserKey(key, down, ch);
            }
        }, new ScenarioRunner() {
            public String run(String name) {
                return ParityScenarios.run(name);
            }
        }, new Painter() {
            public void paint() {
                ParityScenarios.paintLastScenarioForTest();
            }
        });
    }

    @JSBody(params = {"trace", "stepper", "pointer", "key", "scenario", "painter"}, script =
            "window.RaumBallerTrace = function() { return trace(); };" +
            "window.RaumBallerStep = function(frames) { stepper(frames || 1); };" +
            "window.RaumBallerPointer = function(x, y, down, inside) { pointer(x, y, !!down, inside !== false); };" +
            "window.RaumBallerKey = function(code, down, ch) { key(code, !!down, ch || code); };" +
            "window.RaumBallerRunScenario = function(name) { return scenario(name); };" +
            "window.RaumBallerPaintScenario = function() { painter(); };" +
            "window.RaumBallerTraceHash = function(text) { var h = 0; for (var i = 0; i < text.length; i++) { h = ((31 * h + text.charCodeAt(i)) | 0); } return h; };")
    private static native void installTestHooks(TraceSupplier trace, Stepper stepper, PointerInput pointer, KeyInput key, ScenarioRunner scenario, Painter painter);
}
