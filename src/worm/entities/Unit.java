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

import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import worm.Barracks;
import worm.Entity;
import worm.GameConfiguration;
import worm.GameStateInterface;
import worm.Layers;
import worm.Res;
import worm.Worm;
import worm.effects.ElectronZapEffect;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.features.UnitFeature;
import worm.screens.GameScreen;
import worm.weapons.WeaponFeature.WeaponInstance;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.resources.MappedColor;


/**
 * $Id: Unit.java,v 1.18 2010/10/09 01:11:37 foo Exp $
 * Player units
 * @author $Author: foo $
 * @version $Revision: 1.18 $
 */
public class Unit extends Entity implements PlayerWeaponInstallation {

	private static final long serialVersionUID = 1L;

	private static final ArrayList<Entity> COLLISIONS = new ArrayList<Entity>();

	private static final int SPAWN_DURATION = 20;
	private static final int RETARGET_TIME = 300;
	private static final int SHOOT_INTERVAL = 180;
	private static final float MAX_WIDTH = 0.25f;
	private static final float MAX_WOBBLE = 8.0f;
	private static final float WOBBLE_FACTOR = 0.25f;
	private static final float WIDTH_FACTOR = 0.025f;
	private static final int BEAM_X_OFFSET = 0;
	private static final int BEAM_Y_OFFSET = 4;

	private static final Rectangle TEMP_BOUNDS = new Rectangle();

	/** The gidrah feature */
	private final UnitFeature feature;

	/** The owner barracks */
	private final Barracks barracks;

	/** Hitpoints */
	private int hitPoints;

	/** Gidrah wounds (0=healthy, increases as damage is taken) */
	private int wounds;

	/** Flash tick */
	private int flashTick;

	/** Hit by an exploding bullet? */
	private ArrayList<Bullet> explodingBullets;

	/** Current appearance */
	private LayersFeature appearance;

	/** Emitters */
	private transient Emitter[] emitter;

	/** Weapon instance */
	private WeaponInstance weaponInstance;

	/** Movement handler */
	private final Movement movement;

	/** Target gidrah */
	private Entity target;

	/** Retargeting */
	private int targetTick;

	/** Shooting */
	private int shootTick;

	/** Repair interval */
	private final int repairInterval;

	/** Ignore list */
	private final ArrayList<Entity> ignore = new ArrayList<Entity>();

	/** Phase */
	private int phase;
	private int tick;
	private static final int PHASE_SPAWN = 0;
	private static final int PHASE_ALIVE = 1;
	private static final int PHASE_DYING = 2;

	/** Zap effect for repair drones */
	private transient ElectronZapEffect zapEffect;

	/**
	 * C'tor
	 */
	public Unit(Barracks barracks, UnitFeature feature, float mapX, float mapY) {
		this.barracks = barracks;
		this.feature = feature;

		setLocation(mapX, mapY);
		movement = new UnitMovement(this);
		hitPoints = feature.getHitPoints();
		repairInterval = Worm.getGameState().isResearched(ResearchFeature.DROIDBUFF) ? feature.getBuffedRepairInterval() : feature.getRepairInterval();
	}

	public int getWidth() {
		return feature.getBounds().getWidth();
	}

	public int getHeight() {
		return feature.getBounds().getHeight();
	}

	@Override
	public boolean isShootable() {
		return false;
	}

	@Override
	public boolean isSolid() {
		return canCollide();
	}

	@Override
	public boolean canCollide() {
		return isActive() && phase == PHASE_SPAWN || phase == PHASE_ALIVE;
	}

	@Override
	public final Rectangle getBounds(Rectangle bounds) {
		if (bounds == null) {
			bounds = new Rectangle();
		}
		Rectangle featureBounds = feature.getBounds();
		bounds.setBounds((int) getMapX() + featureBounds.getX(), (int) getMapY() + featureBounds.getY(), featureBounds.getWidth(), featureBounds.getHeight());
		return bounds;
	}

	@Override
	public float getOffsetX() {
		return feature.getBounds().getX() + feature.getBounds().getWidth() / 2;
	}

	@Override
	public float getOffsetY() {
		return feature.getBounds().getY() + feature.getBounds().getHeight() / 2;
	}

	@Override
	public boolean isRound() {
		return false;
	}

	@Override
	public float getRadius() {
		return 0.0f;
	}

	@Override
	protected void createSprites(Screen screen) {
		appearance = feature.getAppearance(); // A bit hacky, this - would be better to call normalAppearance()...
		feature.getAppearance().createSprites(screen, this);
	}

	@Override
	public void onCollision(Entity entity) {
		entity.onCollisionWithUnit(this);
	}

	@Override
	public void onCollisionWithBullet(Bullet bullet) {
		if (bullet.isExploding() || bullet.isPassThrough()) {
			if (explodingBullets != null && explodingBullets.contains(bullet)) {
				return;
			}
			if (explodingBullets == null) {
				explodingBullets = new ArrayList<Bullet>(8);
			}
			explodingBullets.add(bullet);
		}

		if (bullet.getSource() == this) {
			return;
		}

		if (!bullet.isDangerousToBuildings()) {
			return;
		}

		int damage = bullet.getDamage();
		damage(damage);
		bullet.onHit(true, this);
		if (isActive()) {
			Emitter e = Res.getGidrahPainEmitter().spawn(GameScreen.getInstance()); // TODO: unit pain emitter
			e.setLocation(bullet.getMapX(), bullet.getMapY());
			e.setOffset(GameScreen.getSpriteOffset());
		}
	}

	@Override
	public void onCollisionWithGidrah(Gidrah gidrah) {
		kill();
	}

	@Override
	public void explosionDamage(int damageAmount, boolean friendly) {
		damage(damageAmount);
	}

	@Override
	public int crush() {
		kill();
		return 0;
	}

	/**
	 * Damage and maybe kill the unit
	 * @param amount The amount of damage to inflict
	 */
	protected void damage(int amount) {
		wounds += amount;
		flashTick = 3;
		setFlash(true);
		if (wounds >= getHitPoints()) {
			kill();
		} else {
			Emitter e = Res.getGidrahPainEmitter().spawn(GameScreen.getInstance());
			e.setOffset(GameScreen.getSpriteOffset());
			e.setLocation(getMapX(), getMapY());
		}
	}

	private int getHitPoints() {
		return hitPoints;
	}

	/**
	 * Kill the unit
	 */
	private void kill() {
		if (!isActive()) {
			return;
		}
		if (feature.getDeathAppearance() != null) {
			setAppearance(feature.getDeathAppearance());
			phase = PHASE_DYING;
		} else {
			// Just remove us for now
			remove();
		}
	}

	@Override
	public final void addToGameState(GameStateInterface gsi) {
		gsi.addToUnits(this);
	}

	@Override
	public final void removeFromGameState(GameStateInterface gsi) {
		gsi.removeFromUnits(this);
	}

	@Override
	protected final void doSpawn() {
		emitter = feature.getAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
		if (feature.getWeapon() != null) {
			weaponInstance = feature.getWeapon().spawn(this);
		}
		tick();
		update();
	}

	@Override
	protected final void doRespawn() {
		emitter = feature.getAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
	}

	@Override
	protected final void doRemove() {
		setEmitters(null);

		// Clean up brain and remove our occupation status
		movement.remove();

		// Inform barracks
		barracks.onUnitRemoved(this);

		if (zapEffect != null) {
			zapEffect.finish();
			zapEffect = null;
		}
	}

	private void setEmitters(Emitter[] newEmitter) {
		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null) {
					element.remove();
				}
			}
		}

		emitter = newEmitter;
	}

	public void setAppearance(LayersFeature newAppearance) {
		this.appearance = newAppearance;
		boolean mirrored = isMirrored();
		newAppearance.createSprites(GameScreen.getInstance(), this);
		setEmitters(newAppearance.createEmitters(GameScreen.getInstance(), getMapX(), getMapY()));
		setMirrored(mirrored);
	}

	@Override
	protected LayersFeature getCurrentAppearance() {
		return appearance;
	}

	@Override
	public int getExtraDamage() {
	    return Worm.getGameState().isResearched(ResearchFeature.BIOLOGY) ? 1 : 0;
	}

	@Override
	protected void calculateScreenPosition() {
		super.calculateScreenPosition();

		// Add feature offsets too (so angry gidrahs are better lookin')
		Point offset = feature.getOffset();
		if (offset != null) {
			setScreenX(getScreenX() + offset.getX());
			setScreenY(getScreenY() + offset.getY());
		}
	}

	@Override
	protected final void doUpdate() {
		if (emitter != null && appearance != null) {
			appearance.updateEmitters(emitter, getMapX(), getMapY());
		}
	}

	@Override
	protected final void doTick() {

		switch (phase) {
			case PHASE_SPAWN:
				doSpawnTick();
				break;
			case PHASE_ALIVE:
				doAliveTick();
				break;
			case PHASE_DYING:
				doDyingTick();
				break;
			default:
				assert false;
				break;
		}

	}

	private void doDyingTick() {
		// Wait for sprite 0 to flag an event 1
		if (getEvent() == 1) {
			remove();
		}
	}

	private void doSpawnTick() {
		tick ++;
		if (tick > SPAWN_DURATION) {
			phase = PHASE_ALIVE;
			tick = 0;

			setAlpha(255);
		} else {
			setAlpha((int) LinearInterpolator.instance.interpolate(0.0f, 255.0f, (float) tick / SPAWN_DURATION));
		}
	}

	@Override
	public void onBulletDeflected(Entity target) {
		ignore.remove(target);
		ignore.add(target);
	}

	private void doAliveTick() {
		if (flashTick > 0) {
			flashTick --;
			if (flashTick == 0) {
				setFlash(false);
			}
		}

		// Retarget every couple of seconds
		Entity oldTarget = target;
		if (targetTick > 0) {
			targetTick --;
			if (targetTick == 0) {
				target = null;
			}
		}
		// If we've got no target, find one:
		if (target == null || !target.isActive()) {
			findTarget();
		} else if (feature.isRepairDrone() && !((Building) target).isDamaged()) {
			findTarget();
		} else if (!feature.isRepairDrone() && !target.isAttackableByUnits()) {
			findTarget();
		}

		if (oldTarget != target && zapEffect != null) {
			if (zapEffect != null) {
				zapEffect.finish();
				zapEffect = null;
			}
		}

		if (target == null) {
			// Don't move or shoot
			return;
		}

		// Shoot weapon if it's in range of the target AND we're allowed to
		if (weaponInstance != null) {
			weaponInstance.tick();
			if (shootTick > 0) {
				shootTick --;
			}
		}

		float range = getDistanceTo(target);
		if (weaponInstance != null && weaponInstance.isReady() && range < feature.getRange()) {
			weaponInstance.fire((int) (target.getX()), (int) (target.getY()));
		} else if (range >= feature.getRange()) {
			// Move. Maybe find a new target.
			movement.tick();
			if (shootTick == 0 && weaponInstance != null && weaponInstance.isReady()) {
				// Take a potshot at anything in range. Only check every few frames
				TEMP_BOUNDS.setBounds((int) (getX() - feature.getRange()), (int) (getY() - feature.getRange()), (int) (feature.getRange() * 2.0f), (int) (feature.getRange() * 2.0f));
				Entity.getCollisions(TEMP_BOUNDS, COLLISIONS);
				int n = COLLISIONS.size();
				float distance = Float.MAX_VALUE;
				Entity closest = null;
				for (int i = n; --i >= 0; ) {
					Entity e = COLLISIONS.get(i);
					if (e.isActive() && e.isAttackableByUnits()) {
						float dist = getDistanceTo(e);
						if (dist < distance && dist <= feature.getRange()) {
							if (!ignore.contains(e)) {
								distance = dist;
								closest = e;
							}
						}
					}
				}
				if (closest != null) {
					weaponInstance.fire((int) (closest.getX()), (int) (closest.getY()));
				} else {
					shootTick = SHOOT_INTERVAL;
				}
			}
		} else if (range < feature.getRange() && feature.isRepairDrone()) {
			// In range of the building we are supposed to be repairing
			Building building = (Building) target;
			int repairCost = (int) (GameConfiguration.getInstance().getRepairCost() * building.getFeature().getInitialValue());
			if (building.isAlive() && building.isDamaged() && Worm.getGameState().getMoney() >= repairCost) {
				if (zapEffect == null) {
					zapEffect = new ElectronZapEffect
						(
							false,
							Res.getRepairZapSound(),
							new MappedColor("repairzap.background"),
							new MappedColor("repairzap.foreground"),
							128,
							feature.getBeamStartEmitter(),
							feature.getBeamEndEmitter(),
							getX() + BEAM_X_OFFSET,
							getY() + BEAM_Y_OFFSET,
							MAX_WIDTH,
							MAX_WOBBLE,
							WOBBLE_FACTOR,
							WIDTH_FACTOR
						);
					zapEffect.setTarget(building.getX(), building.getY());
					zapEffect.spawn(GameScreen.getInstance());
					Game.allocateSound(Res.getCapacitorStartBuffer(), Worm.calcGain(getX(), getY()) * 0.25f, 1.0f);
				}
				shootTick ++;
				if (shootTick > getRepairInterval()) {
					building.repair();
					shootTick = 0;
					LabelEffect effect = new LabelEffect
						(
							net.puppygames.applet.Res.getTinyFont(),
							"-$"+String.valueOf(repairCost),
							ReadableColor.WHITE,
							new MappedColor("repairzap.background"),
							50,
							20
						);
					effect.spawn(GameScreen.getInstance());
					effect.setLayer(Layers.HUD);
					effect.setLocation(building.getX(), building.getY());
					effect.setVelocity(0.0f, 0.5f);
					effect.setAcceleration(0.0f, -0.01f);
					effect.setDelay(0);
					Worm.getGameState().addMoney(-repairCost);
				}
			} else {
				if (zapEffect != null) {
					zapEffect.finish();
					zapEffect = null;
				}
			}
		} else {
			if (zapEffect != null) {
				zapEffect.finish();
				zapEffect = null;
			}
		}
	}

	private int getRepairInterval() {
		return repairInterval;
	}

	/**
	 * Determines if our move takes us into another gidrah or solid building or map tile
	 */
	private boolean isValidMove() {
		if (!Worm.getGameState().getMap().isClearPX(getBounds(TEMP_BOUNDS))) {
			return false;
		}

		ArrayList<Entity> entities = Worm.getGameState().getEntities();
		int n = entities.size();
		for (int i = 0; i < n; i ++) {
			Entity test = entities.get(i);
			if (test == this) {
				continue;
			}
			if (test.isSolid() && test.isTouching(this)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the target
	 */
	public Entity getTarget() {
		return target;
	}

	/**
	 * Find a new target.
	 */
	private void findTarget() {
		if (targetTick > 0) {
			return;
		}
		targetTick = RETARGET_TIME;
		Entity newTarget = feature.getBrain().findTarget(this);
		if (newTarget != target) {
			movement.reset();
			target = newTarget;
		}
	}

	@Override
    public ArrayList<Entity> getIgnore() {
	    return ignore;
    }

	/**
	 * @return the feature
	 */
	public UnitFeature getFeature() {
		return feature;
	}

	@Override
	public boolean isFiringAtAerialTargets() {
	    return feature.isAerialTargets() && target.isFlying();
	}

	@Override
	public String toString() {
		return "Unit["+System.identityHashCode(this)+","+feature+","+getTileX()+","+getTileY()+"]";
	}

	public static void resetTotalThinkTime() {
		UnitMovement.resetTotalThinkTime();
	}


}
