package com.kaeruct.raumballer;

import com.kaeruct.raumballer.background.BGImage;
import com.kaeruct.raumballer.background.BGStar;
import com.kaeruct.raumballer.cannon.Cannon;
import com.kaeruct.raumballer.ship.PlayerShip;
import com.kaeruct.raumballer.ship.player.NimakRunner;
import com.kaeruct.raumballer.ship.player.SpinTurn;
import com.kaeruct.raumballer.ship.player.StenoShot;
import com.kaeruct.raumballer.wave.Wave;

import java.util.ArrayList;

import jgame.JGColor;
import jgame.JGPoint;
import jgame.platform.JGEngine;

public class AndroidGame extends JGEngine {

    private PlayerShip p;
    private ShooterTitle titleState;
    private int gameOverStart = 0;
    public int selectedShip = 0;
    public final byte PLAYER_ID = 1;
    public final byte ENEMY_ID = 2;
    public int score;
    public int starCount;
    public int starMax = 128;
    public int starFreq = 80;
    private boolean levelFinished = false;
    public final int WIDTH = 48 / 3;
    public final int HEIGHT = 68 / 3;
    public LevelReader levelReader;
    public ArrayList<Wave> waves;
    public int t;
    public boolean isTapping;
    public boolean prevIsTapping;
    public boolean isTouchDown;
    public int lastDown;
    public int lastUp;

    public static void main(String[] args) {
        new AndroidGame(new JGPoint(480, 720));
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
        // we set the background colour to same colour as the splash background
        setCanvasSettings(WIDTH, HEIGHT, 16, 16, JGColor.black, new JGColor(0, 0, 0), null);

    }

    public void initGame() {
        this.setGameState("Title");

        this.dbgShowFullStackTrace(true);
        this.dbgShowMessagesInPf(false);

        this.titleState = new ShooterTitle(this);
        Cannon.game = this;

        setFrameRate(60, 2);
        setGameSpeed(1);

        defineMedia("shooter.tbl");
    }

    public void loadLevel(String lvlFile) {
        this.starCount = 0;

        try {
            this.levelReader = new LevelReader(this, getAssets().open(lvlFile));
        } catch (Exception e) {
            this.dbgPrint(e.toString());
        }

        // init background
        new BGImage("pipe", 2, this);
        new BGImage("pipe", 6, this);
        new BGImage("bg1", 4, this);

        removeObjects("bgstar", -1);
        addStars(16);

        this.levelReader.init();
    }

    public void levelFinished() {
        levelFinished = true;
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
        p.onScore(score);
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
        setPFSize(WIDTH, HEIGHT);

        this.waves = new ArrayList<>();
        this.score = 0;
        this.t = 0;

        switch (this.selectedShip) {
            default:
            case 0:
                p = new StenoShot(pfWidth() / 2 - 16, pfHeight() - 32, this);
                break;
            case 1:
                p = new NimakRunner(pfWidth() / 2 - 16, pfHeight() - 32, this);
                break;
            case 2:
                p = new SpinTurn(pfWidth() / 2 - 16, pfHeight() - 32, this);

                break;
        }

        loadLevel("level1.lvl");
    }

    public void doFrameInGame() {
        boolean isDown = getMouseButton(1);
        isTouchDown = isDown;
        if (isDown) lastDown = t;
        isTapping = (t - lastDown < 10); // expire taps after 10 frames

        if (!isTapping && prevIsTapping) {
            lastUp = t;
        }

        prevIsTapping = isTapping;

        checkCollision(PLAYER_ID, ENEMY_ID);
        checkCollision(ENEMY_ID, PLAYER_ID);

        setViewOffset(0, (int) p.y, true);

        for (int i = 0; i < this.waves.size(); i++) {
            waves.get(i).update();
        }

        for (int i = this.waves.size() - 1; i > 0; i--) {
            if (waves.get(i).stopped) {
                waves.remove(i);
            }
        }

        moveObjects(null, 0);

        if (t % starFreq == 0 && starCount < starMax) {
            addStars(16);
        }

        t++;

        if (gameOverStart > 0 && (t - gameOverStart < 30) && isTapping) {
            this.removeObjects("", 0);
            gameOverStart = 0;
            this.setGameState("Title");
        }

        if (this.waves.size() == 0) {
            levelFinished = true;
        }

        if (levelFinished || p.isDead()) {
            gameOverStart = t;
        }
    }

    public void paintFrameInGame() {
        setColor(JGColor.white);

        drawString("Mode:" + p.getMode(), 4, viewHeight() - 10, -1);
        drawString("Score:" + this.score, viewWidth() - 8, viewHeight() - 20, 1);
        drawString("Health:" + ((int) p.getHealth()), viewWidth() - 10, viewHeight() - 8, 1);

        if (levelFinished) {
            int centerY = viewHeight() / 2;
            int centerX = viewWidth() / 2;

            drawString("Congratulations! Level over!", centerX, centerY - 4, 0, "yellow");
            drawString("TAP to restart!", centerX, centerY + 4, 0, "blue");
            drawString("Try another ship!", centerX, centerY + 12, 0, "blue");
        } else if (p.isDead()) {
            int centerY = viewHeight() / 2;
            int centerX = viewWidth() / 2;

            drawString("TAP to restart!", centerX - 4, centerY - 4, 0, "yellow");
            drawString("Try another ship!", centerX + 4, centerY + 12, 0, "blue");
        }
    }

    public PlayerShip getPlayer() {
        return p;
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
