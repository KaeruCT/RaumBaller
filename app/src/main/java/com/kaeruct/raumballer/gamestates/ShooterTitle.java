package com.kaeruct.raumballer.gamestates;

import com.kaeruct.raumballer.AndroidGame;

import java.util.ArrayList;
import java.util.List;

import jgame.JGObject;
import jgame.JGRectangle;

public class ShooterTitle extends GameState {
    public List<JGObject> ships;
    private int centerY;
    private int centerX;

    public ShooterTitle(AndroidGame game) {
        super(game);
        centerY = this.game.viewHeight() / 2;
        centerX = this.game.viewWidth() / 2;
    }

    public void start() {
        game.startGeneral();
        game.playAudio("state", "start", true);
        game.score = 0;
        game.level = 0;
        game.removeObjects("", 0);
        game.addStars(128);
        game.setPlayer(null);
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
                // find out which ship was tapped and use it as the player ship
                if (ships.get(i).getBBox().intersects(mouseRect)) {
                    game.stopAudio("state");
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
        game.drawString("-~~~~~~~~~~~~~~~-", centerX, centerY - 48, 0, "yellow");
        game.drawString("TAP a ship to begin", centerX, centerY - 32, 0, "blue");
        game.drawString("TAP~~~~~~~~~~~~~~~~", centerX, centerY - 32, 0, "yellow");
        game.drawString("move by TAPPING where to go", centerX, centerY + 32, 0, "blue");
        game.drawString("~~~~~~~~TAPPING~~~~~~~~~~~~", centerX, centerY + 32, 0, "yellow");
        game.drawString("get STRONGER by DESTROYING", centerX, centerY + 48, 0, "blue");
        game.drawString("~~~~STRONGER~~~~DESTROYING", centerX, centerY + 48, 0, "yellow");
        game.drawString("enemy ships", centerX, centerY + 64, 0, "blue");
        game.drawString("© KaeruCT 2012 - 2021", centerX, this.game.viewHeight() - 32, 0, "white");
    }
}
