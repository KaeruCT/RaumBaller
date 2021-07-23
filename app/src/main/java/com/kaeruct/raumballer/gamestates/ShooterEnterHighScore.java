package com.kaeruct.raumballer.gamestates;

import com.kaeruct.raumballer.AndroidGame;

public class ShooterEnterHighScore extends GameState {
    public ShooterEnterHighScore(AndroidGame game) {
        super(game);
    }

    public void start() {
        game.startGeneral();
        game.removeObjects("bullet", 0);
        game.playAudio("state", "success", false);
    }

    public void doFrame() {
        game.doFrameGeneral();
        if (game.t > 120 && game.isTapping) {
            game.setGameState("Title");
        }
    }

    public void paintFrame() {
        int centerY = game.viewHeight() / 2;
        int centerX = game.viewWidth() / 2;
        game.getPlayer().setPos(centerX, centerY);

        game.drawString("YOU ARE WINNER", centerX, centerY - 48, 0, "blue");
        game.drawString("Y~U~~R~~W~N~E~", centerX, centerY - 48, 0, "yellow");
        game.drawString("Your score was: " + game.score, centerX, centerY - 32, 0, "blue");
        game.drawString("~~~~~~~~~~~~~~~~" + game.score, centerX, centerY - 32, 0, "yellow");
        game.drawString("TAP to restart!", centerX, centerY + 32, 0, "blue");
        game.drawString("TAP~~~~~~~~~~~~", centerX, centerY + 32, 0, "yellow");
        game.drawString("Maybe try another ship?", centerX, centerY + 48, 0, "blue");
    }
}
