package com.kaeruct.raumballer.gamestates;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.ship.PlayerShip;
import com.kaeruct.raumballer.ship.player.NimakRunner;
import com.kaeruct.raumballer.ship.player.SpinTurn;
import com.kaeruct.raumballer.ship.player.StenoShot;

import jgame.JGColor;

public class ShooterInGame extends GameState {

    public ShooterInGame(AndroidGame game) {
        super(game);
    }

    public void start() {
        game.startGeneral();
        game.startLevel();

        if (game.getPlayer() == null) {
            switch (game.selectedShip) {
                default:
                case 0:
                    game.setPlayer(new StenoShot(game.pfWidth() / 2 - 16, game.pfHeight() - 32, game));
                    break;
                case 1:
                    game.setPlayer(new NimakRunner(game.pfWidth() / 2 - 16, game.pfHeight() - 32, game));
                    break;
                case 2:
                    game.setPlayer(new SpinTurn(game.pfWidth() / 2 - 16, game.pfHeight() - 32, game));
                    break;
            }
        }
    }

    public void doFrame() {
        game.doFrameGeneral();

        game.checkCollision(AndroidGame.PLAYER_ID, AndroidGame.ENEMY_ID);
        game.checkCollision(AndroidGame.ENEMY_ID, AndroidGame.PLAYER_ID);

        game.setViewOffset(0, (int) game.getPlayer().y, true);

        if (game.getPlayer().isDead()) {
            game.setGameState("GameOver");
        } else {
            game.updateWaves();
            game.updateStars();
            game.updateLevelState();
        }

        game.moveObjects(null, 0);
    }

    public void paintFrame() {
        game.drawString("Score:" + game.score, game.viewWidth() - 8, game.viewHeight() - 20, 1);

        PlayerShip player = game.getPlayer();
        double amt = player.getHealth() / player.getMaxHealth();
        double intensity = 0.6;
        double amtColor = player.getHealth() / player.getMaxHealth() * intensity;
        JGColor c = new JGColor(intensity - amtColor, amtColor, 0);
        game.drawRect(
                8,
                game.viewHeight() - 8.0,
                (game.viewWidth() * amt) - 16,
                4,
                true,
                false,
                1,
                c
        );
    }
}
