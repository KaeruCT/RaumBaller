package com.kaeruct.raumballer;

import java.util.ArrayList;
import java.util.List;

import jgame.JGObject;
import jgame.JGRectangle;

public class ShooterTitle {

    private final AndroidGame game;
    private final int centerX;
    private final int centerY;
    private List<JGObject> ships;

    public ShooterTitle(AndroidGame game) {
        this.game = game;

        centerY = this.game.viewHeight() / 2;
        centerX = this.game.viewWidth() / 2;
    }

    public void start() {
        game.addStars(128);
        ships = new ArrayList<>();
        ships.add(new JGObject("select-steno-shot", true, centerX - 48, centerY - 8, 0, "player1"));
        ships.add(new JGObject("select-nimak-runner", true, centerX, centerY - 8, 0, "player2"));
        ships.add(new JGObject("select-spin-turn", true, centerX + 48, centerY - 8, 0, "player3"));
    }

    public void doFrame() {
        game.moveObjects(null, 0);

        if (game.getMouseButton(1)) {
            JGRectangle mouseRect = new JGRectangle(game.getMouseX(), game.getMouseY(), 1, 1);
            for (int i = 0; i < ships.size(); i++) {
                if (ships.get(i).getBBox().intersects(mouseRect)) {
                    game.selectedShip = i;
                    game.removeObjects("select-", 0);
                    game.setGameState("InGame");
                    break;
                }
            }
        }
    }

    public void paintFrame() {
        game.drawString("-= RAUM BALLER =-", centerX, centerY - 48, 0, "blue");
        game.drawString("TAP a ship to begin", centerX, centerY - 32, 0);
        game.drawString("move by TAPPING where to go", centerX, centerY + 32, 0);
        game.drawString("get stronger by destroying", centerX, centerY + 48, 0);
        game.drawString("enemy ships", centerX, centerY + 64, 0);
    }
}
