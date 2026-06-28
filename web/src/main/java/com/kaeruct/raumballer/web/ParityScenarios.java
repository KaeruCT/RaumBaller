package com.kaeruct.raumballer.web;

import com.kaeruct.raumballer.AndroidGame;

import jgame.JGObject;
import jgame.JGPoint;
import jgame.platform.JGEngine;

public final class ParityScenarios {
    public static final String[] NAMES = new String[]{
            "title-idle",
            "select-ship-0",
            "select-ship-1",
            "select-ship-2",
            "level-1-pointer",
            "keyboard-wasd",
            "keyboard-arrows",
            "space-shoot",
            "mixed-pointer-keyboard",
            "enemy-collision-game-over",
            "level-complete",
            "winner"
    };

    private static AndroidGame lastGame;

    private ParityScenarios() {
    }

    public static void paintLastScenarioForTest() {
        if (lastGame != null) lastGame.paintFrameForTest();
    }

    public static String run(String name) {
        if ("title-idle".equals(name)) return runTitleIdle();
        if ("select-ship-0".equals(name)) return runSelectShip(0);
        if ("select-ship-1".equals(name)) return runSelectShip(1);
        if ("select-ship-2".equals(name)) return runSelectShip(2);
        if ("level-1-pointer".equals(name)) return runPointerMovement();
        if ("keyboard-wasd".equals(name)) return runKeyboardMovement('W', 'A');
        if ("keyboard-arrows".equals(name)) return runKeyboardMovement(JGEngine.KeyUp, JGEngine.KeyLeft);
        if ("space-shoot".equals(name)) return runKeyboardMovement(32, 0);
        if ("mixed-pointer-keyboard".equals(name)) return runMixedPointerKeyboard();
        if ("enemy-collision-game-over".equals(name)) return runGameOverTransition();
        if ("level-complete".equals(name)) return runForcedState("LevelDone", 1);
        if ("winner".equals(name)) return runForcedState("EnterHighscore", 4);
        throw new IllegalArgumentException("Unknown parity scenario: " + name);
    }

    public static int expectedChecksum(String name) {
        if ("title-idle".equals(name)) return 616122706;
        if ("select-ship-0".equals(name)) return -244245836;
        if ("select-ship-1".equals(name)) return 1138935041;
        if ("select-ship-2".equals(name)) return 216086947;
        if ("level-1-pointer".equals(name)) return -1682687873;
        if ("keyboard-wasd".equals(name)) return -769052057;
        if ("keyboard-arrows".equals(name)) return -769052057;
        if ("space-shoot".equals(name)) return 251870129;
        if ("mixed-pointer-keyboard".equals(name)) return 2022323162;
        if ("enemy-collision-game-over".equals(name)) return 1751088272;
        if ("level-complete".equals(name)) return -880110737;
        if ("winner".equals(name)) return -649496232;
        throw new IllegalArgumentException("No expected checksum for " + name);
    }

    private static String runTitleIdle() {
        AndroidGame game = newGame();
        step(game, 5);
        return game.traceState();
    }

    private static String runSelectShip(int shipIndex) {
        AndroidGame game = newGame();
        selectShip(game, shipIndex);
        step(game, 5);
        return game.traceState();
    }

    private static String runPointerMovement() {
        AndroidGame game = newGame();
        selectShip(game, 0);
        game.setPointer(144, 420, true, true);
        step(game, 30);
        game.setPointer(144, 420, false, true);
        return game.traceState();
    }

    private static String runKeyboardMovement(int firstKey, int secondKey) {
        AndroidGame game = newGame();
        selectShip(game, 1);
        if (firstKey != 0) game.setBrowserKey(firstKey, true, firstKey);
        if (secondKey != 0) game.setBrowserKey(secondKey, true, secondKey);
        step(game, 30);
        if (firstKey != 0) game.setBrowserKey(firstKey, false, firstKey);
        if (secondKey != 0) game.setBrowserKey(secondKey, false, secondKey);
        return game.traceState();
    }

    private static String runMixedPointerKeyboard() {
        AndroidGame game = newGame();
        selectShip(game, 2);
        game.setPointer(40, 360, true, true);
        game.setBrowserKey('D', true, 'D');
        game.setBrowserKey(JGEngine.KeyUp, true, JGEngine.KeyUp);
        step(game, 30);
        game.setPointer(40, 360, false, true);
        game.setBrowserKey('D', false, 'D');
        game.setBrowserKey(JGEngine.KeyUp, false, JGEngine.KeyUp);
        return game.traceState();
    }

    private static String runGameOverTransition() {
        AndroidGame game = newGame();
        selectShip(game, 0);
        game.getPlayer().setHealth(0);
        step(game, 2);
        return game.traceState();
    }

    private static String runForcedState(String state, int level) {
        AndroidGame game = newGame();
        selectShip(game, 0);
        game.level = level;
        game.setGameState(state);
        step(game, 2);
        return game.traceState();
    }

    private static AndroidGame newGame() {
        JGObject.setEngine(null);
        JGObject.resetObjectIdForTest();
        AndroidGame game = new AndroidGame(new JGPoint(0, 0));
        lastGame = game;
        step(game, 1);
        return game;
    }

    private static void selectShip(AndroidGame game, int shipIndex) {
        int x = 96 + 48 * shipIndex;
        game.setPointer(x + 8, 256, true, true);
        step(game, 2);
        game.setPointer(x + 8, 256, false, true);
        step(game, 2);
    }

    private static void step(AndroidGame game, int frames) {
        for (int i = 0; i < frames; i++) game.stepFrameForTest();
    }
}
