package com.kaeruct.raumballer.gamestates;

import com.kaeruct.raumballer.AndroidGame;

public class ShooterLevelDone extends GameState {
    public ShooterLevelDone(AndroidGame game) {
        super(game);
    }

    public void start() {
        game.startGeneral();
    }

    public void doFrame() {
        game.doFrameGeneral();
        if (game.t > 120 && game.isTapping) {
            game.setGameState("InGame");
        }
    }

    public void paintFrame() {
        int centerY = game.viewHeight() / 2;
        int centerX = game.viewWidth() / 2;

        game.drawString("Level " + game.level + " completed!", centerX, centerY - 16, 0, "blue");
        game.drawString("~~~~~~" + game.level + "~~~~~~~~~~~", centerX, centerY - 16, 0, "yellow");
        game.drawString("TAP to continue", centerX, centerY, 0, "blue");
        game.drawString("TAP~~~~~~~~~~~~", centerX, centerY, 0, "yellow");
    }
}
