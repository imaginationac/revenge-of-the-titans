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

import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.BlastEffect;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.GameMap;
import worm.GameStateInterface;
import worm.MapRenderer;
import worm.SFX;
import worm.Tile;
import worm.Worm;
import worm.features.BulletFeature;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.SineInterpolator;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.Util;


/**
 * Bullets
 * @author Cas
 */
public class Bullet extends Entity {

	private static final long serialVersionUID = 1L;

	private static final int MIN_REMAINING_RANGE = 5;
	private static final int MAX_REMAINING_RANGE = 30;
	private static final double JITTER = Math.PI / 32.0;

	private static final ArrayList<Entity> SHOOTABLE_ENTITIES = new ArrayList<Entity>();


	/** Range, in ticks */
	private int tick = 1;

	/** Direction */
	private float dx, dy;

	/** Start */
	private float sx, sy;

	/** Target */
	private float tx, ty;

	/** Angle in Yakly degrees */
	private int angle;

	/** Speed */
	private float speed;

	/** Leftover movement */
	private float leftoverMovement;

	/** Emitters */
	private transient Emitter[] emitter;

	/** Bullet feature */
	private final BulletFeature feature;

	/** Source of the bullet */
	private final Entity source;

	/** Exploding? */
	private boolean exploding;

	/** Blast effect */
	private transient BlastEffect blastEffect;

	/** Range to target */
	private int rangeToTarget;

	/** Damage */
	private int damage;

	/** Armour piercing */
	private int ap;

	/** Dangerous to buildings? */
	private boolean dangerousToBuildings;

	/** Dangerous to gidrahs? */
	private boolean dangerousToGidrahs;

	/** Dangerous to gidlets? */
	private boolean dangerousToGidlets;

	/** Total distance moved */
	private int moved;

	/** Last entity we bounced off */
	private Entity lastBounce;

	/** Bounce tick */
	private int lastBounceTick;

	/** angle of last hit */
	private double hitAngle;

	/** Remaining range after bounce */
	private int remainingRange;

	/** Spawned in a wall? */
	private int wallThruState;
	private static final int WALLTHRU_UNKNOWN = 0;
	private static final int WALLTHRU_IN_WALL = 1;
	private static final int WALLTHRU_NORMAL = 2;

	/**
	 * C'tor
	 */
	public Bullet(Entity source, float sx, float sy, float tx, float ty, BulletFeature feature, int extraDamage) {
		this.source = source;
		this.dangerousToBuildings = feature.isExploding() || source instanceof Gidrah;
		this.dangerousToGidrahs = !(source instanceof Gidrah);
		this.dangerousToGidlets = dangerousToGidrahs && (feature.isMini() || feature.isExploding());
		this.feature = feature;
		this.speed = feature.getSpeed();
		this.damage = feature.getDamage() + extraDamage;
		if (dangerousToGidrahs && feature.isExploding() && Worm.getGameState().isResearched(ResearchFeature.ADVANCEDEXPLOSIVES)) {
			damage += feature.getDamage();
		}
		this.ap = feature.getArmourPiercing() + (feature.isBlaster() ? (Worm.getGameState().isResearched(ResearchFeature.ANATOMY) ? 1 : 0) : 0);

		setLocation(sx, sy);
		this.sx = sx;
		this.sy = sy;
		this.tx = tx;
		this.ty = ty;

		// If the bullet has a max range, we'll use that and just move it for as far as it's allowed;
		// otherwise it will fly to its target location and beyond, unless it's "targeted"

		if (speed > 0) {
			float ddx = tx - sx;
			float ddy = ty - sy;
			rangeToTarget = (int) Math.sqrt(ddx * ddx + ddy * ddy);
			dx = ddx / rangeToTarget;
			dy = ddy / rangeToTarget;
			calcAngle();
		}
	}

	public void setRemainingRange(int remainingRange) {
	    this.remainingRange = remainingRange;
    }

	/**
	 * @return the armour piercing factor
	 */
	public int getArmourPiercing() {
		return ap;
	}

	/**
	 * @return the stun for this bullet
	 */
	public int getStun() {
		return feature.getStun();
	}

	private void calcAngle() {
		angle = FPMath.fpYaklyDegrees(Math.atan2(dy, dx));
		for (int i = 0; i < getNumSprites(); i ++) {
			getSprite(i).setAngle(angle);
		}
		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null) {
					element.setAngle((float) Math.toDegrees(Math.atan2(dy, dx)));
				}
			}
		}
	}

	@Override
	public final boolean canCollide() {
		return exploding || !feature.isExploding();
	}

	@Override
	public void onCollision(Entity entity) {
		entity.onCollisionWithBullet(this);
	}

	@Override
	public void onCollisionWithBullet(Bullet bullet) {
		if (bullet.dangerousToBuildings != dangerousToBuildings) {
			onHit(true, bullet);
		}
	}

	/**
	 * Bounce!
	 * @param target The target we're bouncing off of
	 */
	public void bounce(Entity target) {

		// remember angle so can set deflect emitter
		hitAngle=getAngle();

		// Stop daft bouncing
		if (lastBounce == target) {
			lastBounceTick ++;
			if (lastBounceTick <= 8) {
				remove();
				return;
			}
		} else {
			lastBounceTick = 0;
			lastBounce = target;
		}

		spawnRicochetEmitter();
		SFX.ricochet(getMapX(), getMapY(), 1.0f);
		remainingRange = Util.random(MIN_REMAINING_RANGE, MAX_REMAINING_RANGE);

		// At what angle have we hit the entity?
		double ddx = target.getMapX() - getMapX();
		double ddy = target.getMapY() - getMapY();
		double angleOfCollision = Math.atan2(ddy, ddx);

		// Now reflect about this angle
		double diff = Math.atan2(dy, dx) - angleOfCollision;
		double reflection = angleOfCollision + Math.PI - diff + Math.random() * JITTER - JITTER * 0.5;
		dx = (float) Math.cos(reflection);
		dy = (float) Math.sin(reflection);
		calcAngle();
//		// Bullet is now dangerous to the player's buildings
//		dangerous = true;
		move();
	}

	/**
	 * @return true if this bullet is dangerous to buildings
	 */
	public boolean isDangerousToBuildings() {
		return dangerousToBuildings;
	}

	public boolean isDangerousToGidrahs() {
		return dangerousToGidrahs;
	}

	public boolean isDangerousToGidlets() {
	    return dangerousToGidlets;
    }

	/* (non-Javadoc)
	 * @see worm.Entity#isHoverable()
	 */
	@Override
	public boolean isHoverable() {
		return false;
	}

	/**
	 * @return true if this bullet passes through aliens
	 */
	public boolean isPassThrough() {
		return feature.isPassThrough();
	}

	/**
	 * Called when the bullet has struck a target
	 * @param wasDamaged Whether the target took damage
	 * @param target The target that was hit
	 */
	public void onHit(boolean wasDamaged, Entity target) {
		if (exploding) {
			return;
		}
		if (target != null && !wasDamaged) {
			bounce(target);
			source.onBulletDeflected(target);
		} else {
			ricochet();
		}
	}

	@Override
	protected final void doSpawn() {
		init();
		for (int i = 0; i < getNumSprites(); i ++) {
			getSprite(i).setAngle(angle);
		}
	}

	@Override
	protected void createSprites(Screen screen) {
		feature.getAppearance().createSprites(screen, this);
	}

	private void init() {
		emitter = feature.getAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null) {
					element.setLocation(getMapX(), getMapY());
					GameScreen.getInstance().detachTickable(element);
				}
			}
		}
		calcAngle();

		spawnFlashEmitter();

	}

	@Override
	protected void doRemove() {
		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null) {
					element.remove();
				}
			}
			emitter = null;
		}
	}

	@Override
	protected void doRespawn() {
		init();
	}

	@Override
	protected void doTick() {
		if (exploding) {
			addToCollisionManager();
			tick ++;
			if (tick > feature.getExplosionDuration()) {
				remove();
				return;
			}
		} else if (speed > 0.0f) {

			if (remainingRange > 0) {
				remainingRange --;
				if (remainingRange == 0) {
					ricochet();
					SFX.ricochet(getX(), getY(), 1.0f);
					return;
				}
			}

			if (feature.isTargeted()) {
				if (getDistanceTo(sx, sy) >= rangeToTarget) {
					ricochet();
					return;
				}
			}

			int ms = (int) speed;
			float rem = speed - ms;
			leftoverMovement += rem;
			if (leftoverMovement >= 1.0f) {
				leftoverMovement -= 1.0f;
				ms ++;
			}
			moved += ms;

			int tileX = (int) (getX() / MapRenderer.TILE_SIZE);
			int tileY = (int) (getY() / MapRenderer.TILE_SIZE);
			for (int i = 0; i < ms && isActive(); i ++) {
				tick ++;
				move();

				if 	(
						getX() < 2.0f
					|| 	getY() < 2.0f
					|| 	getX() >= Worm.getGameState().getMap().getWidth() * MapRenderer.TILE_SIZE - 2.0f
					|| 	getY() >= Worm.getGameState().getMap().getHeight() * MapRenderer.TILE_SIZE - 2.0f
					)
				{
					// Missed
					if (exploding) {
						ricochet();
					} else {
						remove();
					}
					return;
				}

				// Are we at target?
				if (feature.isTargeted()) {
					if (getDistanceTo(sx, sy) >= rangeToTarget) {
						ricochet();
						return;
					}
				}

				if (canCollide()) {
					checkCollisions(SHOOTABLE_ENTITIES);

					for (int j = 0; j < SHOOTABLE_ENTITIES.size(); j ++) {
						Entity target = SHOOTABLE_ENTITIES.get(j);
						this.onCollision(target);
						target.onCollision(this);
						if (!isActive()) {
							return;
						}
					}
				}

				// Check for collision with solid things
				int newTileX = (int) (getMapX() / MapRenderer.TILE_SIZE);
				int newTileY = (int) (getMapY() / MapRenderer.TILE_SIZE);
				if (newTileX != tileX || newTileY != tileY) {
					tileX = newTileX;
					tileY = newTileY;
					boolean inWall = false;
					outer: for (int z = 0; z < GameMap.LAYERS; z ++) {
						Tile t = Worm.getGameState().getMap().getTile(tileX, tileY, z);
						if (t != null && !t.isBulletThrough()) {
							switch (wallThruState) {
								case WALLTHRU_UNKNOWN:
									// We're in a wall
									wallThruState = WALLTHRU_IN_WALL;
									inWall = true;
									break outer;
								case WALLTHRU_IN_WALL:
									// Still in a wall
									inWall = true;
									break outer;
								case WALLTHRU_NORMAL:
									// Hit something
									SFX.ricochet(getMapX(), getMapY(), 1.0f);
									ricochet();
									return;
								default:
									assert false : wallThruState;
							}
						}
					}

					if (!inWall) {
						wallThruState = WALLTHRU_NORMAL;
					}
				}




			}
			speed -= feature.getDeceleration();
			if (speed < 1.0f) {
				speed = 0.0f;
				ricochet();
				SFX.ricochet(getMapX(), getMapY(), 1.0f);
				return;
			}
		}

		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null && element.isActive()) {
					element.tick();
				}
			}
		}
	}

	private void move() {
		setLocation(getMapX() + dx, getMapY() + dy);
	}

	@Override
	protected void onSetLocation() {
		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null) {
					element.setLocation(getMapX(), getMapY());
				}
			}
		}
	}

	/**
	 * Do a ricochet and remove the bullet
	 */
	protected void ricochet() {
		if (!isActive()) {
			return;
		}
		spawnRicochetEmitter();
		if (feature.isExploding()) {
			if (!exploding) {
				exploding = true;
				dangerousToBuildings = true;
				dangerousToGidrahs = true;
				tick = 0;
				blastEffect = feature.createBlastEffect(dangerousToGidrahs, (int) getMapX(), (int) getMapY());
				setVisible(false);
			}
		} else {
			remove();
		}
	}

	private void spawnRicochetEmitter() {
		EmitterFeature ef = feature.getRicochetEmitter();
		if (ef != null) {
			Emitter e = ef.spawn(GameScreen.getInstance());
			// add dx and dy * speed - so ricochet emitter appears more into gidrah
			e.setLocation(getMapX() + dx*speed*0.5f, getMapY() + dy*speed*0.5f);
			e.setAngle(getAngle());
			e.setOffset(GameScreen.getSpriteOffset());
		}
	}

	private void spawnFlashEmitter() {
		EmitterFeature ef = feature.getFlashEmitter();
		if (ef != null) {
			Emitter e = ef.spawn(GameScreen.getInstance());

			e.setLocation(getMapX(), getMapY());
			e.setAngle(getAngle());
			e.setOffset(GameScreen.getSpriteOffset());
		}
	}

	/* (non-Javadoc)
	 * @see storm.Entity#addToGameState(storm.GameStateInterface)
	 */
	@Override
	public void addToGameState(GameStateInterface gsi) {
		// No need to do anything
	}

	@Override
	public void removeFromGameState(GameStateInterface gsi) {
		// No need to do anything
	}

	@Override
	public boolean isShootable() {
		return dangerousToBuildings;
	}

	@Override
	public Rectangle getBounds(Rectangle bounds) {
		return null;
	}

	@Override
	public float getRadius() {
		if (blastEffect != null) {
			return blastEffect.getRadius();
		} else {
			return 3.0f;
		}
	}

	@Override
	public boolean isRound() {
		return true;
	}

	/**
	 * Called when this bullet passes through an alien
	 */
	public void onPassThrough() {
		damage --;
		if (damage == 0) {
			ricochet();
		}
	}

	/**
	 * @return bullet damage
	 */
	public int getDamage() {
		if (feature.getBaseSpeed() > 0.0f) {
			return (int) SineInterpolator.instance.interpolate(0.0f, damage, speed / feature.getBaseSpeed());
		} else {
			return damage;
		}
	}

	/**
	 * @return Returns the source.
	 */
	public Entity getSource() {
		return source;
	}

	/**
	 * @return Returns the exploding.
	 */
	public boolean isExploding() {
		return exploding;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Bullet["+feature+" from "+source+"]";
	}

	public float getDX() {
		return dx;
	}

	public float getDY() {
		return dy;
	}

	/**
	 * @return Returns the anlge (so can set ricochet emitter)
	 */
	public float getAngle() {
		return (float) Math.toDegrees(Math.atan2(dy, dx));
	}

	/**
	 * @return Returns the hitAngle - angle before last bounce (so can set deflect emitter)
	 */
	public float getHitAngle() {
		return (float) hitAngle;
	}

	public void setAppearance(LayersFeature newAppearance) {
	}

}
