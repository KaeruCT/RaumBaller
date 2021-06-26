package com.kaeruct.raumballer;

import com.kaeruct.raumballer.background.BGImage;
import com.kaeruct.raumballer.background.BGStar;
import com.kaeruct.raumballer.cannon.Cannon;
import com.kaeruct.raumballer.gamestates.GameState;
import com.kaeruct.raumballer.gamestates.ShooterEnterHighScore;
import com.kaeruct.raumballer.gamestates.ShooterGameOver;
import com.kaeruct.raumballer.gamestates.ShooterInGame;
import com.kaeruct.raumballer.gamestates.ShooterLevelDone;
import com.kaeruct.raumballer.gamestates.ShooterTitle;
import com.kaeruct.raumballer.ship.PlayerShip;
import com.kaeruct.raumballer.wave.Wave;

import java.util.ArrayList;

import jgame.JGColor;
import jgame.JGPoint;
import jgame.JGTimer;
import jgame.platform.JGEngine;

public class AndroidGame extends JGEngine {

    private PlayerShip player;
    private GameState titleState;
    private GameState gameOverState;
    private GameState levelDoneState;
    private GameState enterHighscoreState;
    private GameState inGameState;
    public int selectedShip = 0;
    public static final byte PLAYER_ID = 1;
    public static final byte ENEMY_ID = 2;
    public int score;
    public int starCount;
    private boolean levelDone = false;
    public final int WIDTH = 48 / 3;
    public final int HEIGHT = 68 / 3;
    public LevelReader levelReader;
    public ArrayList<Wave> waves;
    public int t;
    public boolean isTapping;
    public boolean prevIsTapping;
    public boolean isTouchDown;
    public int lastDown = -1;
    public int lastUp;
    public int level = 0;
    private final int lastLevel = 4;
    public int starMax = 128;
    public int starFreq = 80;

    public static void main(String[] args) {
        new AndroidGame(new JGPoint(0, 0));
    }

    /**
     * Application constructor.
     */
    public AndroidGame(JGPoint size) {
        initEngine(size.x, size.y);
    }

    /**
     * Applet constructor.
     */
    public AndroidGame() {
        initEngineApplet();
    }

    public void initCanvas() {
        setCanvasSettings(WIDTH, HEIGHT, 16, 16, JGColor.cyan, JGColor.black, null);
    }

    public void initGame() {
        setAuthorMessage("");
        defineMedia("shooter.tbl");
        setPFSize(WIDTH, HEIGHT);

        // init background
        new BGImage("pipe", 2, this);
        new BGImage("pipe", 6, this);
        new BGImage("bg1", 4, this);

        setGameState("Title");

        dbgShowFullStackTrace(true);
        dbgShowMessagesInPf(false);

        titleState = new ShooterTitle(this);
        inGameState = new ShooterInGame(this);
        gameOverState = new ShooterGameOver(this);
        levelDoneState = new ShooterLevelDone(this);
        enterHighscoreState = new ShooterEnterHighScore(this);
        Cannon.game = this;

        setFrameRate(60, 2);
        setGameSpeed(1);
    }

    public void updateLevelState() {
        if (levelReader != null && levelReader.isComplete()) {
            levelDone = getObjects("", ENEMY_ID, false, null).size() == 0;
        }
        if (levelDone) {
            levelDone = false;
            new JGTimer(60, true,"InGame") {
                public void alarm() {
                    if (level == lastLevel) {
                        setGameState("EnterHighscore");
                    } else {
                        setGameState("LevelDone");
                    }
                }
            };
        }
    }

    public void updateWaves() {
        for (int i = 0; i < waves.size(); i++) {
            waves.get(i).update();
        }

        for (int i = waves.size() - 1; i > 0; i--) {
            if (waves.get(i).stopped) waves.remove(i);
        }
    }

    public void updateStars() {
        if (t % starFreq == 0 && starCount < starMax) {
            addStars(16);
        }
    }

    public void loadLevel(String lvlFile) {
        waves = new ArrayList<>();
        starCount = 0;
        try {
            levelReader = new LevelReader(this, getAssets().open(lvlFile));
        } catch (Exception e) {
            dbgPrint(e.toString());
            return;
        }

        removeObjects("bgstar", -1);
        addStars(16);
    }

    public void addStars(int n) {
        for (int i = 0; i < n; i++) {
            new BGStar(
                    random(0, pfWidth()) - 8,
                    random(4, pfHeight()) - 4,
                    (int) Math.ceil(random(1, 8)),
                    this);
        }
        starCount += n;
    }

    public void addScore(double n) {
        score += n;
        player.onScore(score);
    }

    public void startTitle() {
        titleState.start();
    }

    public void doFrameTitle() {
        titleState.doFrame();
    }

    public void paintFrameTitle() {
        titleState.paintFrame();
    }

    public void startInGame() {
        inGameState.start();
    }

    public void doFrameInGame() {
        inGameState.doFrame();
    }

    public void paintFrameInGame() {
        inGameState.paintFrame();
    }

    public void startLevel() {
        level += 1;
        loadLevel("level" + level + ".lvl");
    }

    public void startGeneral() {
        t = 0;
    }

    public void doFrameGeneral() {
        if (t < lastDown) lastDown = -1;
        boolean isDown = getMouseButton(1);
        isTouchDown = isDown;
        if (isDown) lastDown = t;
        isTapping = lastDown > 0 && (t - lastDown < 10); // expire taps after 10 frames

        if (!isTapping && prevIsTapping) {
            lastUp = t;
        }

        prevIsTapping = isTapping;
        t++;
    }

    public void startLevelDone() {
        levelDoneState.start();
    }

    public void doFrameLevelDone() {
        levelDoneState.doFrame();
    }
    public void paintFrameLevelDone() {
        levelDoneState.paintFrame();
    }

    public void startEnterHighscore() {
        enterHighscoreState.start();
    }

    public void doFrameEnterHighscore() {
        enterHighscoreState.doFrame();
    }

    public void paintFrameEnterHighscore() {
        enterHighscoreState.paintFrame();
    }

    public void startGameOver() {
        gameOverState.start();
    }

    public void doFrameGameOver() {
        gameOverState.doFrame();
    }

    public void paintFrameGameOver() {
        gameOverState.paintFrame();
    }

    public PlayerShip getPlayer() {
        return player;
    }

    public void setPlayer(PlayerShip p) {
        this.player = p;
    }

    public boolean goingUp(double angle) {
        angle = (angle * 180 / Math.PI) % 360;
        while (angle < 0) angle += 360;

        return (angle >= 0 && angle <= 180);
    }

    public boolean goingDown(double angle) {
        return !goingUp(angle);
    }

    public void drawString(String s, double x, double y, int align) {
        drawImageString(s, x, y, align, "font_white", 32, 0);
    }

    public void drawString(String s, double x, double y, int align, String color) {
        drawImageString(s, x, y, align, "font_" + color, 32, 0);
    }
}
