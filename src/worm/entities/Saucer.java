/*
 * Copyright (c) 2003-onwards Shaven Puppy Ltd
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'Shaven Puppy' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package worm.entities;

import java.util.ArrayList;

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.LabelEffect;

import org.lwjgl.util.Color;
import org.lwjgl.util.Rectangle;

import worm.ClickAction;
import worm.Entity;
import worm.GameMap;
import worm.GameStateInterface;
import worm.Layers;
import worm.MapRenderer;
import worm.Res;
import worm.Tile;
import worm.Worm;
import worm.WormGameState;
import worm.effects.SaucerEffect;
import worm.features.LayersFeature;
import worm.powerups.PowerupFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: Saucer.java,v 1.53 2010/10/13 23:04:54 foo Exp $
 * The bonus saucer!
 *
 * @author $Author: foo $
 * @version $Revision: 1.53 $
 */
public class Saucer extends Entity {

	private static final long serialVersionUID = 1L;

	private static final int MIN_SAUCER_DURATION = 200;
	private static final int SAUCER_DURATION = 900;
	private static final int SAUCER_DURATION_PER_LEVEL = 10;
	private static final int MIN_DROPPED_SAUCER_DURATION = 100;
	private static final int DROPPED_SAUCER_DURATION = 600;
	private static final int DROPPED_SAUCER_DURATION_PER_LEVEL = 6;

	private static final int POWERUP_LABEL_OFFSET = 6;

	private transient SoundEffect soundEffect;

	/** Phase */
	private int phase;
	private static final int PHASE_APPEAR = 0;
	private static final int PHASE_NORMAL = 1;
	private static final int PHASE_DEAD = 2;

	/** Tick */
	private int tick;

	/** Emitters */
	private transient Emitter[] emitter;

	/** Powerup */
	private PowerupFeature powerup;

	/** Dropped by a gidrah */
	private boolean dropped;

	/**
	 * C'tor
	 */
	public Saucer() {
		powerup = Worm.getGameState().getRandomPowerup();
	}

	public Saucer(float x, float y) {
		dropped = true;
		setLocation(x, y);
		powerup = Worm.getGameState().getCrappyPowerup();
	}

	@Override
	public boolean isShootable() {
		return false;
	}

	@Override
	public boolean isClickable() {
		return phase == PHASE_NORMAL;
	}

	@Override
	protected void doSpawn() {
		// Hint about usage
		Worm.getGameState().flagHint(powerup.getHint());

		// Choose the powerup to use
		if (!dropped) {
			// Find an empty spot on the map;
			int count = 0;
			boolean ok = false;
			ArrayList<Entity> entities = Worm.getGameState().getEntities();
			int n = entities.size();
			int x, y;
			GameMap map = Worm.getGameState().getMap();
			outer: while (++count < 1000 && !ok) {
				x = Util.random(MapRenderer.FADE_SIZE, map.getWidth() - MapRenderer.FADE_SIZE);
				y = Util.random(MapRenderer.FADE_SIZE, map.getHeight() - MapRenderer.FADE_SIZE);
				for (int z = 0; z < GameMap.LAYERS; z ++) {
					Tile t = map.getTile(x, y, z);
					if (t != null && (t.isSolid() || t.isImpassable())) {
						continue outer;
					}
				}

				setLocation(x * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE / 2, y * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE / 2);

				// Check no solid entity in the way
				// TODO: use quadtree
				for (int i = 0; i < n; i ++) {
					Entity entity = entities.get(i);
					if (entity != this && entity.isActive() && entity.isSolid() && entity.canCollide() && entity.isTouching(this)) {
						continue outer;
					}
				}

				ok = true;
			}
			if (!ok) {
				// Didn't find one
				remove();
				return;
			}
		}

		init();
	}

	@Override
	protected void createSprites(Screen screen) {
		if (powerup.getAppearance() == null) {
			System.out.println(powerup+" has no appearance");
		} else {
			powerup.getAppearance().createSprites(screen, this);
		}
	}

	@Override
	protected LayersFeature getCurrentAppearance() {
		return powerup.getAppearance();
	}

	/**
	 * Initialise sound effects and emitters
	 */
	private void init() {
		if (!dropped) {
			// Start the sound effect
			soundEffect = Game.allocateSound(Res.getSaucerSound(), Worm.calcLoudGain(getMapX() + getCollisionX(), getMapY() + getCollisionY()), 1.0f, this);

			// And special effect
			SaucerEffect fx = new SaucerEffect(this);
			fx.spawn(GameScreen.getInstance());
		}

		// Create emitters
		emitter = powerup.getAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
	}

	/* (non-Javadoc)
	 * @see invaders.Entity#doRespawn()
	 */
	@Override
	protected void doRespawn() {
		init();
	}

	/* (non-Javadoc)
	 * @see invaders.Entity#canCollide()
	 */
	@Override
	public boolean canCollide() {
		return phase == PHASE_NORMAL;
	}

	/* (non-Javadoc)
	 * @see invaders.Entity#onCollision(invaders.Entity)
	 */
	@Override
	public void onCollision(Entity entity) {
		entity.onCollisionWithSaucer(this);
	}

	private void kill() {
		WormGameState gameState = Worm.getGameState();


		String msg = powerup.getTitle();

		Color labelColorStart = powerup.getLabelColorStart();
		Color labelColorEnd = powerup.getLabelColorEnd();

		LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getSmallFont(), msg, labelColorStart, labelColorEnd, 60, 55);
		le.setLocation(getMapX(), getMapY() + POWERUP_LABEL_OFFSET );
		le.setVelocity(0.0f, 0.8f);
		le.setAcceleration(0.0f, -0.0075f);
		le.setLayer(Layers.HUD);
		le.spawn(GameScreen.getInstance());

		if (powerup.isCollectable()) {
			// Save it for later
			gameState.addPowerup(powerup, true);
		} else {
			// Immediate activation (money)
			gameState.activatePowerup(powerup);
			Game.allocateSound(powerup.getCollectSound());
		}

		stopSound();

		if (powerup.getCollectAppearance() == null) {
			// Just remove - no death appearance yet
			remove();
		} else {
			powerup.getCollectAppearance().createSprites(GameScreen.getInstance(), this);
			if (emitter != null) {
				for (Emitter element : emitter) {
					if (element != null) {
						element.remove();
					}
				}
				emitter = null;
			}
			emitter = powerup.getCollectAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
			phase = PHASE_DEAD;
		}
	}

	public boolean isDead() {
		return phase == PHASE_DEAD;
	}

	private boolean isAlive() {
		return phase == PHASE_NORMAL && isActive();
	}


	@Override
	public void onCollisionWithSmartbomb(Smartbomb smartBomb) {
		if (!isAlive()) {
			return;
		}
		kill();
	}

	@Override
	public LayersFeature getMousePointer(boolean clicked) {
		return Worm.getGameState().isBezerk() ? Res.getMousePointerBezerkOnTarget() :
			Worm.getGameState().isSmartbombMode() ? Res.getMousePointerSmartbomb() : Res.getMousePointerOnTarget();
	}

	@Override
	public int onClicked(int mode) {
		kill();
		return ClickAction.CONSUME;
	}

	@Override
	protected float getRoundCollisionY() {
		return 10.0f;
	}

	@Override
	public float getRadius() {
		return 12.0f;
	}

	@Override
	public boolean isRound() {
		return true;
	}

	@Override
	protected void doTick() {
		switch (phase) {
			case PHASE_APPEAR:
				if (getEvent() == 1) {
					phase = PHASE_NORMAL;
					// Intentional fallthrough
				} else {
					break;
				}
			case PHASE_NORMAL:
				if (GameScreen.getInstance().isFastForward()) {
					return;
				}
				tick ++;
				if (tick > getDuration()) {
					phase = PHASE_DEAD;
					powerup.getVanishAppearance().createSprites(GameScreen.getInstance(), this);
				}
				break;
			case PHASE_DEAD:
				// Wait for event 2
				if (getEvent() == 2) {
					remove();
				}
				break;
			default:
				assert false;
		}
	}

	/* (non-Javadoc)
	 * @see worm.Entity#doUpdate()
	 */
	@Override
	protected void doUpdate() {
		if (soundEffect != null) {
			soundEffect.setGain(Res.getSaucerSound().getGain() * Game.getSFXVolume() * Worm.calcGain(getMapX() + getOffsetX(), getMapY() + getOffsetY()), this);
		}
	}

	/**
	 * @return the duration, in ticks, that the saucer should remain in normal state
	 */
	private int getDuration() {
		if (Worm.getGameState().getGameMode() == WormGameState.GAME_MODE_SURVIVAL) {
			return dropped ? DROPPED_SAUCER_DURATION : SAUCER_DURATION;
		} else {
			return dropped ?
						Math.max(MIN_DROPPED_SAUCER_DURATION, DROPPED_SAUCER_DURATION - DROPPED_SAUCER_DURATION_PER_LEVEL * Worm.getGameState().getLevel())
					:
						Math.max(MIN_SAUCER_DURATION, SAUCER_DURATION - SAUCER_DURATION_PER_LEVEL * Worm.getGameState().getLevel());
		}
	}

	/* (non-Javadoc)
	 * @see invaders.Entity#doRemove()
	 */
	@Override
	protected void doRemove() {
		stopSound();
		if (emitter != null) {
			for (Emitter element : emitter) {
				element.remove();
			}
			emitter = null;
		}
	}

	private void stopSound() {
		if (soundEffect != null) {
			soundEffect.stop(this);
			soundEffect = null;
		}
	}

	@Override
	public final void addToGameState(GameStateInterface gsi) {
		gsi.addToSaucers(this);
	}

	@Override
	public final void removeFromGameState(GameStateInterface gsi) {
		gsi.removeFromSaucers(this);
	}

	@Override
	public Rectangle getBounds(Rectangle bounds) {
		return null;
	}
}
