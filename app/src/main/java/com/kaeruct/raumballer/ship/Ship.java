package com.kaeruct.raumballer.ship;

import com.kaeruct.raumballer.AndroidGame;
import com.kaeruct.raumballer.Explosion;
import com.kaeruct.raumballer.bullet.Bullet;
import com.kaeruct.raumballer.cannon.Cannon;

import java.util.ArrayList;

import jgame.JGObject;
import jgame.JGTimer;

public abstract class Ship extends JGObject {

    protected AndroidGame game;
    protected double velocity;
    public ArrayList<Cannon> cannons;
    private static final int flashingTime = 32;
    private final String baseAnimation;
    protected double angle;
    private double health;
    private final double maxHealth;
    protected int flashStart;
    protected String explosionColor;
    protected int clock;
    public int width;
    protected Ship parent;
    protected boolean sturdy; // don't get hurt at all if hit by ships

    public Ship(double x, double y, String name, int cid, String graphic, double maxHealth, AndroidGame game) {
        super(name, true, x, y, cid, graphic);
        this.game = game;
        this.velocity = 1; // max speed
        this.maxHealth = maxHealth;
        this.angle = Math.PI * 2;
        this.expiry = -2;
        this.baseAnimation = graphic;
        this.explosionColor = "red";
        this.setBBox(4, 4, 8, 8);
        this.clock = 0;
        this.width = 16;
        this.parent = null;
        this.sturdy = false;
        this.setHealth(maxHealth);

        // initialize cannons
        this.cannons = new ArrayList<>();
    }

    public boolean flashing() {

        if (this.flashStart == 0)
            return false;
        else
            return this.flashStart > game.t - flashingTime;
    }

    // begin flashing
    private void flash() {
        if (!flashing()) {
            this.flashStart = this.game.t;
            this.setAnim(baseAnimation + "_flash");
        }
    }

    public String getMode() {
        String mode = this.cannons.get(0).getClass().getName();
        return mode.replaceAll("com\\.kaeruct\\.raumballer\\.cannon\\.", "");
    }

    // shoot in the same direction the ship is looking
    public void shoot() {
        shoot(this.angle);
    }

    // shoot at an specific direction
    public void shoot(double angle) {
        for (Cannon currentCannon : this.cannons) {
            currentCannon.shoot(x + this.width / 2, y, angle, this.colid, game.t);
        }

    }

    // update stuff not related to movement
    public void update() {
        clock++;
        if (!flashing()) {
            this.setAnim(baseAnimation);
        }
    }

    public void move() {
        // movement
        this.update();
        x += Math.cos(angle) * velocity * gamespeed;
        y -= Math.sin(angle) * velocity * gamespeed;
    }

    public void hurt(double n) {
        this.setHealth(this.getHealth() - n);

        if (this.getHealth() <= 0) {
            this.setHealth(0);
            kill();
        }
    }

    public void kill() {

        final int offset = this.width / 2 - 8; // 16/2 is explosion width
        final int r = this.width / 2;

        new Explosion(x + offset, y + offset, explosionColor);

        for (int i = 0; i < this.width; i += 8) {
            new JGTimer(game.random(i, i + 20, 1), true, "InGame") {
                public void alarm() {
                    new Explosion(x + offset, y + offset + game.random(-r, r), explosionColor);
                    new Explosion(x + offset + game.random(-r, r), y, explosionColor);
                    new Explosion(x + offset, y + offset, explosionColor);
                }
            };
        }

        game.playAudio("shooting", "explode", false);

        remove();
    }

    public void hit(JGObject obj) {
        if (obj instanceof Bullet) {
            Bullet b = ((Bullet) obj);
            hurt(b.damage);
            b.remove();

            flash();

            // increase score if it was an enemy ship
            if (this.isDead() && this instanceof EnemyShip) {
                this.game.addScore(this.maxHealth * 100);
            }

        } else if (obj instanceof Ship && !sturdy) {
            Ship s = ((Ship) obj);
            hurt(2 * s.getHealth() / this.getHealth());
            flash();

            // temporary invincibility to avoid getting hurt too much when hitting a ship with another ship
            if (this instanceof PlayerShip) {
                this.sturdy = true;
                PlayerShip ship = (PlayerShip) this;
                new JGTimer(10, true, "InGame") {
                    @Override
                    public void alarm() {
                        ship.sturdy = false;
                    }
                };
            }
        }
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public boolean isDead() {
        return this.getHealth() == 0;
    }

    public void setParent(Ship parent) {
        this.parent = parent;
    }

    public void onScore(int score) { }
}
