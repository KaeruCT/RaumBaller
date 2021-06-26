package com.kaeruct.raumballer.gamestates;

import com.kaeruct.raumballer.AndroidGame;

public abstract class GameState {
    protected AndroidGame game;
    public GameState(AndroidGame game) {
        this.game = game;
    }

    public abstract void start();

    public abstract void doFrame();

    public abstract void paintFrame();
}
