package com.kaeruct.raumballer.ship.enemy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kaeruct.raumballer.AndroidGame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jgame.JGObject;
import jgame.JGPoint;

public class EnemyShipFactoryTest {
    private AndroidGame game;

    @Before
    public void setUpGame() {
        System.setProperty("raumballer.headless", "true");
        System.setProperty("raumballer.seed", "2026");
        JGObject.setEngine(null);
        JGObject.resetObjectIdForTest();
        game = new AndroidGame(new JGPoint(0, 0));
    }

    @After
    public void tearDownGame() {
        JGObject.setEngine(null);
        System.clearProperty("raumballer.headless");
        System.clearProperty("raumballer.seed");
    }

    @Test
    public void createsEveryLevelReferencedEnemy() {
        assertTrue(EnemyShipFactory.create("SparkDefender", 0, 0, 0, 0, game) instanceof SparkDefender);
        assertTrue(EnemyShipFactory.create("FireStriker", 0, 0, 0, 0, game) instanceof FireStriker);
        assertTrue(EnemyShipFactory.create("SpaceBall", 0, 0, 0, 0, game) instanceof SpaceBall);
        assertTrue(EnemyShipFactory.create("SparkEye", 0, 0, 0, 0, game) instanceof SparkEye);
        assertTrue(EnemyShipFactory.create("CibumDestroyer", 0, 0, 0, 0, game) instanceof CibumDestroyer);
        assertTrue(EnemyShipFactory.create("BobbaDestroyer", 0, 0, 0, 0, game) instanceof BobbaDestroyer);
        assertTrue(EnemyShipFactory.create("Asterisk", 0, 0, 0, 0, game) instanceof Asterisk);
    }

    @Test
    public void rejectsUnknownEnemy() {
        try {
            EnemyShipFactory.create("MissingEnemy", 0, 0, 0, 0, game);
        } catch (IllegalArgumentException e) {
            assertEquals("Unknown enemy ship class: MissingEnemy", e.getMessage());
            return;
        }
        throw new AssertionError("Expected an IllegalArgumentException");
    }
}
