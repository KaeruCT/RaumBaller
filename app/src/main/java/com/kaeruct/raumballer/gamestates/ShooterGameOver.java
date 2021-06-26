package com.kaeruct.raumballer.gamestates;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.ship.PlayerShip;

import jgame.JGObject;

public class ShooterGameOver extends GameState {

    private int centerX;
    private int centerY;

    public ShooterGameOver(AndroidGame game) {
        super(game);
        centerY = game.viewHeight() / 2;
        centerX = game.viewWidth() / 2;
    }

    public void start() {
        game.startGeneral();

        game.removeObjects("", 0);
        // show selected ship in game over screen
        PlayerShip player = game.getPlayer();
        if (player != null) {
            new JGObject("game-over-player", true, centerX - 16, centerY, 0, player.getGraphic());
        }
    }

    public void doFrame() {
        game.doFrameGeneral();
        if (game.t > 120 && game.isTapping) {
            game.setGameState("Title");
            game.setPlayer(null);
        }
    }

    public void paintFrame() {
        game.drawString("Your score was: " + game.score, centerX, centerY - 32, 0, "blue");
        game.drawString("~~~~~~~~~~~~~~~~" + game.score, centerX, centerY - 32, 0, "yellow");
        game.drawString("TAP to restart!", centerX, centerY + 32, 0, "blue");
        game.drawString("TAP~~~~~~~~~~~~", centerX, centerY + 32, 0, "yellow");
        game.drawString("Maybe try another ship?", centerX, centerY + 48, 0, "blue");
    }
}
