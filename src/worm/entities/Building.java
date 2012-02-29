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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import worm.ClickAction;
import worm.Entity;
import worm.GameStateInterface;
import worm.Hints;
import worm.Mode;
import worm.Res;
import worm.SFX;
import worm.Worm;
import worm.animation.SimpleThingWithLayers;
import worm.buildings.BuildingFeature;
import worm.effects.HitPointsEffect;
import worm.effects.ProximityEffect;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.util.Util;



/**
 * $Id: Building.java,v 1.118 2010/11/02 17:21:33 foo Exp $
 * A building
 * @author $Author: foo $
 * @version $Revision: 1.118 $
 */
public abstract class Building extends Entity {

	private static final long serialVersionUID = 1L;

	private static final int MAX_SHIELDS = 4;
	public static final float PROXIMITY_DISTANCE = 64.0f;
	private static final int BOSS_ATTACK_DAMAGE = 100;
	private static final int SELL_TICK_TIME = 600; // you get 10 seconds to sell a building after it's built and you'll get 100% of your money back
	private static final int CLOAK_MIN_ALPHA = 32;
	private static final int CLOAK_MAX_ALPHA = 128;
	private static final int CLOAK_MIN_CYCLE = 120;
	private static final int CLOAK_MAX_CYCLE = 240;

	/*
	 * Phases
	 */
	private static final int PHASE_NORMAL = 0;
	private static final int PHASE_DYING = 1;
	private static final int PHASE_DEAD = 2;
	private static final int PHASE_GHOST = 3;

	/*
	 * Shield types
	 */
	private static final int SHIELD_NONE = 0;
	private static final int SHIELD_FORCEFIELD = 1;
	private static final int SHIELD_INVULN = 2;


	/** Feature */
	private final BuildingFeature feature;

	/** Phase */
	private int phase = PHASE_NORMAL;

	/** Hitpoints */
	private int hitPoints;

	/** Shielded */
	private boolean shielded;

	/** Flash ticker */
	private int flashTick;

	/** Hovered */
	private boolean hovered;

	/** Proximity */
	private int reactors, scanners, batteries, coolingTowers, shields, factories, bases, capacitors, spawners, autoLoaders, warehouses, collectors, crystals, turrets, cloaks;

	/** More sprites */
	private SimpleThingWithLayers shieldedLayers, hoveredLayers;

	/** Hitpoints effect */
	private transient HitPointsEffect hitPointsEffect;

	/** Emitters */
	private transient Emitter[] emitter, shieldedEmitter, hoveredEmitter;

	/** Ghost effects */
	private transient ArrayList<ProximityEffect> proximityEffects;

	/** Burning & flames */
	private transient Emitter damagedEmitter;

	/** Shield type */
	private int shieldType = SHIELD_NONE;

	/** What buildings we've added */
	private final Set<Building> proximal = new HashSet<Building>();

	/** What this building cost to build */
	private int cost;

	/** Sell tick */
	private int sellTick;

	/** Cloaking tick */
	private int cloakTick, cloakCycle;

	/** Exploding bullets that have hit us */
	private ArrayList<Bullet> explodingBullets;

	/**
	 * C'tor
	 */
	protected Building(BuildingFeature feature, boolean ghost) {
		this.feature = feature;
		if (ghost) {
			phase = PHASE_GHOST;
		}
		hitPoints = getMaxHitPoints();
	}

	/**
	 * @param cost the cost to set
	 */
	public void setCost(int cost) {
		this.cost = cost;
	}

	/**
	 * @return the cost
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Creates the proximity effects for this building
	 * @param color The color to use
	 */
	protected void createProximityEffects(ReadableColor color) {
		finishProximityEffects();

		ArrayList<Building> buildings = getAllAffectedBuildings();
		int n = buildings.size();
		proximityEffects = null;
		proximityEffects = new ArrayList<ProximityEffect>(n);
		for (int i = 0; i < n; i ++) {
			Building target = buildings.get(i);
			ProximityEffect fx = new ProximityEffect(color, PROXIMITY_DISTANCE, target.getMapX() + target.getCollisionX(), target.getMapY() + target.getCollisionY());
			fx.setOffset(GameScreen.getSpriteOffset());
			fx.spawn(GameScreen.getInstance());
			fx.setTarget(getMapX() + getCollisionX(), getMapY() + getCollisionY());
			fx.tick();
			proximityEffects.add(fx);
		}
	}

	@Override
	public void setVisible(boolean visible) {
	    super.setVisible(visible);

	    if (proximityEffects != null) {
	    	for (ProximityEffect pe : proximityEffects) {
	    		pe.setVisible(visible);
	    	}
	    }
	}

	/**
	 * Apply proximity adjustment
	 * @param delta
	 */
	protected final void doProximityAdjustment(int delta) {
		if (phase == PHASE_GHOST) {
			return;
		}
		ArrayList<Building> buildings = getNearbyAffectedBuildings();
		int n = buildings.size();
		for (int i = 0; i < n; i ++) {
			Building target = buildings.get(i);
			boolean tweak = false;
			if (delta == 1) {
				if (!proximal.contains(target)) {
					this.proximal.add(target);
					target.proximal.add(this);
					tweak = true;
				}
			} else {
				if (proximal.contains(target)) {
					this.proximal.remove(target);
					target.proximal.remove(this);
					tweak = true;
				}
			}
			if (tweak) {
				this.adjustProximity(target, delta);
				target.adjustProximity(this, delta);
			}
		}
	}

	protected final ArrayList<Building> getNearbyAffectedBuildings() {
		ArrayList<Building> buildings = getNearbyBuildings();
		return getAffectedBuildings(buildings);
	}

	protected final ArrayList<Building> getAllAffectedBuildings() {
		ArrayList<Building> buildings = Worm.getGameState().getBuildings();
		return getAffectedBuildings(buildings);
	}

	private ArrayList<Building> getAffectedBuildings(ArrayList<Building> buildings) {
		int n = buildings.size();
		ArrayList<Building> ret = new ArrayList<Building>();
		for (int i = 0; i < n; i ++) {
			Building target = buildings.get(i);
			if (target != this && isAffectedBy(target) && target.isAffectedBy(this)) {
				ret.add(target);
			}
		}
		return ret;
	}

	protected final ArrayList<Building> getNearbyBuildings() {
		ArrayList<Building> buildings = Worm.getGameState().getBuildings();
		int n = buildings.size();
		ArrayList<Building> ret = new ArrayList<Building>();
		for (int i = 0; i < n; i ++) {
			Building target = buildings.get(i);
			if (target != this && (target.isAlive() || target.isGhost()) && this.getDistanceTo(target.getMapX() + target.getCollisionX(), target.getMapY() + target.getCollisionY()) <= PROXIMITY_DISTANCE) {
				ret.add(target);
			}
		}
		return ret;
	}

	/**
	 * Apply ghost proximity adjustment - this is called every tick
	 * @param delta
	 */
	protected final void calcGhostProximity() {
		assert phase == PHASE_GHOST;

		// Reset
		reactors = scanners = batteries = coolingTowers = shields = factories = bases = capacitors = spawners = autoLoaders = warehouses = collectors = crystals = turrets = 0;

		ArrayList<Building> buildings = Worm.getGameState().getBuildings();
		int n = buildings.size();
		for (int i = 0; i < n; i ++) {
			Building target = buildings.get(i);
			if (target == this) {
				continue;
			}
			if (target.isAlive() && isAffectedBy(target) && this.getDistanceTo(target.getX(), target.getY()) <= PROXIMITY_DISTANCE) {
				assert target.isAffectedBy(this);
				target.adjustProximity(this, 1);
			}
		}
	}

	protected boolean isAffectedBy(Building building) {
		return feature.isAffectedBy(building.feature);
	}

	/**
	 * Adjust the appropriate proximity value of the target building by delta
	 * @param target
	 * @param delta
	 */
	protected void adjustProximity(Building target, int delta) {
	}

	/**
	 * Finish proximity effects
	 */
	private void finishProximityEffects() {
		if (proximityEffects != null) {
			for (Iterator<ProximityEffect> i = proximityEffects.iterator(); i.hasNext(); ) {
				ProximityEffect e = i.next();
				e.finish();
			}
			proximityEffects = null;
		}
	}

	/**
	 * @return true if this building is a ghost
	 */
	public final boolean isGhost() {
		return phase == PHASE_GHOST;
	}

	/**
	 * @return true if this building is a minefield
	 */
	public boolean isMineField() {
		return false;
	}

	public boolean isCrystal() {
		return false;
	}

	/**
	 * @return true if this is a reactor
	 */
	public boolean isReactor() {
		return false;
	}

	/**
	 * @return true if this building is a barricade
	 */
	public boolean isBarricade() {
		return false;
	}

	/**
	 * @return true if this is the "slowdown" substance ("tangleweb")
	 */
	public boolean isSlowDown() {
		return false;
	}

	/**
	 * @return true if this building blocks gidlets
	 */
	public boolean isGidletProof() {
		return true;
	}

	@Override
	public boolean isShootable() {
		// Player can't shoot buildings
		return false;
	}

	@Override
	public boolean isSolid() {
		return !isMineField();
	}

	@Override
	public final boolean isClickable() {
		return phase == PHASE_NORMAL;
	}

	@Override
	public final boolean isHoverable() {
		return isAlive();
	}

	/**
	 * @return true if the building is active, and solid
	 */
	public boolean isAlive() {
		return isActive() && phase == PHASE_NORMAL;
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
	public boolean canCollide() {
		return isActive() && phase == PHASE_NORMAL;
	}

	/**
	 * Can we build this building on top of the target?
	 * @param target A solid, collidable, active entity
	 * @return true if we can
	 */
	public boolean canBuildOnTopOf(Entity target) {
		if (target instanceof Building) {
			return feature.canBuildOnTopOf(((Building) target).feature);
		} else {
			return
					!(target instanceof Gidrah)
				||	target instanceof Gidrah
				&&	!((Gidrah) target).getFeature().isBoss();
		}
	}

	public boolean canBuild() {
		return true;
	}

	/* (non-Javadoc)
	 * @see worm.Entity#isRound()
	 */
	@Override
	public boolean isRound() {
		return false;
	}

	@Override
	public float getRadius() {
		return 0.0f;
	}

	@Override
	public void onCollision(Entity entity) {
		entity.onCollisionWithBuilding(this);
	}

	/**
	 * @return the feature this building is based on
	 */
	public final BuildingFeature getFeature() {
		return feature;
	}

	/**
	 * Damage the building
	 */
	public final void damage(int amount) {
		if (!isAlive() || phase != PHASE_NORMAL || shielded && amount < BOSS_ATTACK_DAMAGE) {
			return;
		}

		cancelUndo();
		createHitPointsEffect();

		int armour = Math.min(MAX_SHIELDS, shields + (Worm.getGameState().isResearched(ResearchFeature.NANOHARDENING) ? 1 : 0));
		int damage = Math.max(0, amount - armour);
		if (damage == 0) {
			return;
		}
		hitPoints = Math.max(0, hitPoints - damage);
		if (isWorthAttacking()) {
			Worm.getGameState().onBuildingDamaged();
		}
		updateDamageIndicators();
		setFlash(true);
		flashTick += 4;
		if (hitPoints == 0) {
			destroy(false);
		} else {
			onDamaged();
		}
	}

	protected final void updateDamageIndicators() {
		// No emitters for barricades
		if (isBarricade() || isMineField()) {
			return;
		}
		if (hitPointsEffect != null) {
			hitPointsEffect.setHitPoints(hitPoints);
		}
		if (damagedEmitter != null) {
			damagedEmitter.finish();
			damagedEmitter = null;
		}
		if (hitPoints <= getMaxHitPoints() * 0.33f) {
			// Flames!
			damagedEmitter = feature.getFlamesEmitter().spawn(GameScreen.getInstance());
			Point offset = feature.getFlamesOffset();
			if (offset == null) {
				offset = new Point((int) getCollisionX(), (int) (getCollisionY() * 0.33f));
			}
			damagedEmitter.setLocation(getMapX() + offset.getX(), getMapY() + offset.getY());
			damagedEmitter.setFloor(feature.getFloor() + getMapY());
			if (hitPoints > 0) {
				onDestructionImminent();
			}
		} else if (hitPoints <= getMaxHitPoints() * 0.66f) {
			// Smoke!
			damagedEmitter = Res.getBuildingSmokeEmitter().spawn(GameScreen.getInstance());
			damagedEmitter.setLocation(getMapX() + getCollisionX(), getMapY() + getCollisionY() * 0.5f);
			damagedEmitter.setFloor(feature.getFloor() + getMapY());
		}
	}

	/**
	 * Called when the building is down to 1/3rd its hitpoints
	 */
	protected void onDestructionImminent() {}

	/**
	 * Called if a building takes some damage but isn't destroyed
	 */
	protected void onDamaged() {}

	/**
	 * Called if a building gains hitpoints
	 */
	protected void onRepaired() {}

	@Override
	public void disruptorDamage(int amount, boolean friendly) {
		if (isBarricade() || isMineField() || Worm.getGameState().isResearched(ResearchFeature.SHIELDING)) {
			return;
		}

		if (friendly) {
			amount = 1;
			Worm.getGameState().flagHint(Hints.DISRUPTOR);
		}

		damage(amount);
	}

	@Override
	public void explosionDamage(int damageAmount, boolean friendly) {
		if (isMineField()) {
			return;
		}
		// Always just 1 point
		damage(1);
		if (friendly) {
			Worm.getGameState().flagHint(Hints.EXPLOSIVES);
		}
	}

	/**
	 * Destroy the building
	 */
	public void destroy(boolean deliberate) {
		if (!isActive() || phase != PHASE_NORMAL) {
			return;
		}

		setFlash(false);
		doBuildingDestroy();

		LayersFeature appearance = feature.getDeathAppearance();
		if (appearance != null) {
			removeSpecialEffects();

			phase = PHASE_DYING;
			appearance.createSprites(GameScreen.getInstance(), getX(), getY(), this);
			setEmitters(appearance.createEmitters(GameScreen.getInstance(), getMapX(), getMapY()));
			if (damagedEmitter != null) {
				damagedEmitter.remove();
				damagedEmitter = null;
			}
		} else {
			// No death appearance - just destroy and remove
			remove();
		}

		if (!deliberate) {
			float gain;
			if (isBarricade()) {
				gain = 0.25f;
			} else {
				gain = getMaxHitPoints() / 40.0f;
			}
			SFX.buildingDestroyed(getX(), getY(), gain);
		}

		Worm.getGameState().onBuildingDestroyed(this, deliberate);
	}

	private void maybeRemoveEmitters() {
		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null) {
					element.remove();
				}
			}
			emitter = null;
		}
	}

	/**
	 * Removes any existing sprites and emitters if our appearance has changed
	 */
	protected void updateAppearance() {
		if (!isAlive()) {
			return;
		}
		LayersFeature appearance = feature.getAppearance(this);
		appearance.createSprites(GameScreen.getInstance(), getX(), getY(), this);
		setEmitters(appearance.createEmitters(GameScreen.getInstance(), getMapX(), getMapY()));
		updateDamageIndicators();
	}

	public void setAppearance(LayersFeature newAppearance) {
	}

	protected final void setEmitters(Emitter[] newEmitters) {
		maybeRemoveEmitters();
		this.emitter = newEmitters;
		float ey = getMapY() + feature.getFloor();
		if (emitter != null) {
			for (Emitter element : emitter) {
				if (element != null) {
					element.setFloor(ey);
				}
			}
		}
	}

	private void removeSpecialEffects() {
		maybeRemoveEmitters();
		finishProximityEffects();
		removeShieldEffects();
		removeHoveredEffects();
		doRemoveSpecialEffects();
		if (damagedEmitter != null) {
			damagedEmitter.remove();
			damagedEmitter = null;
		}
		if (hitPointsEffect != null) {
			hitPointsEffect.finish();
			hitPointsEffect = null;
		}
	}
	protected void doRemoveSpecialEffects() {}

	@Override
	protected final void doRemove() {
		doProximityAdjustment(-1);
		removeSpecialEffects();
		doBuildingRemove();
	}
	protected void doBuildingRemove() {
	}
	protected void doBuildingDestroy() {
	}
	private void removeShieldEffects() {
		if (shieldedLayers != null) {
			shieldedLayers.remove();
			shieldedLayers = null;
		}
		if (shieldedEmitter != null) {
			for (Emitter element : shieldedEmitter) {
				if (element != null) {
					element.remove();
				}
			}
			shieldedEmitter = null;
		}
		shieldType = SHIELD_NONE;
	}
	private void removeHoveredEffects() {
		if (hoveredLayers != null) {
			hoveredLayers.remove();
			hoveredLayers = null;
		}
		if (hoveredEmitter != null) {
			for (Emitter element : hoveredEmitter) {
				if (element != null) {
					element.remove();
				}
			}
			hoveredEmitter = null;
		}
	}

	public void createGhostProximityEffects() {
		createProximityEffects(ReadableColor.RED);
	}

	@Override
	protected final void doSpawn() {
		if (phase == PHASE_GHOST) {
			createGhostProximityEffects();
			doGhostSpawn();
			return;
		}
		// Create emitters
		setEmitters(feature.getAppearance(this).createEmitters(GameScreen.getInstance(), getMapX(), getMapY()));

		sellTick = SELL_TICK_TIME;
		addProximities();
		doBuildingSpawn();
	}
	protected void doBuildingSpawn() {}
	protected void doGhostSpawn() {}
	public final void onBuild() {
		finishProximityEffects();
		doProximityAdjustment(1);
		Worm.getGameState().addAvailableStock(getFeature(), -1);
		doOnBuild();
	}
	protected void doOnBuild() {}

	private void addProximities() {
		if (isMineField() || isBarricade()) {
			return;
		}

		createProximityEffects(ReadableColor.CYAN);
	}

	@Override
	protected void createSprites(Screen screen) {
		feature.getAppearance(this).createSprites(screen, getX(), getY(), this);
	}

	private void createHitPointsEffect() {
		if (!shouldShowAttackWarning()) {
			return;
		}
		if (hitPointsEffect == null) {
			hitPointsEffect = new HitPointsEffect(this);
			hitPointsEffect.spawn(GameScreen.getInstance());
			hitPointsEffect.setHitPoints(getHitPoints());
		}
		hitPointsEffect.reset();
	}

	@Override
	protected final void doTick() {
		switch (phase) {
			case PHASE_GHOST:
				calcGhostProximity();
				doGhostTick();
				return;

			case PHASE_DEAD:
				// Wait for death to finish. This is an event 2 from sprite 0.
				if (getEvent() == 2) {
					remove();
				}
				return;

			case PHASE_DYING:
				// Wait to explode. This is an event 1 from sprite 0.
				if (getEvent() == 1) {
					phase=PHASE_DEAD;
				}
				return;

			case PHASE_NORMAL:
				if (GameScreen.getInstance().isBlocked()) {
					finishProximityEffects();
				} else if (shouldShowAttackWarning() && !isGhost()) {
					if (hovered) {
						createHitPointsEffect();
					} else {
						if (hitPointsEffect != null) {
							hitPointsEffect.setShow(false);
						}
						finishProximityEffects();
					}
				}

				if (sellTick > 0 && Worm.getGameState().isLevelStarted()) {
					sellTick --;
				}

				if (hitPointsEffect != null && !hitPointsEffect.isActive()) {
					hitPointsEffect = null;
				}

				if (shielded && Worm.getGameState().getShieldTick() == 0) {
					shielded = false;
					updateShieldAppearance();
				} else if (!shielded && Worm.getGameState().getShieldTick() > 0) {
					shielded = true;
					updateShieldAppearance();
				}

				if (flashTick > 0) {
					flashTick --;
					if (flashTick == 0) {
						setFlash(false);
					}
				}

				if (isCloaked()) {
					cloakTick ++;
					if (cloakCycle == 0) {
						cloakCycle = Util.random(CLOAK_MIN_CYCLE, CLOAK_MAX_CYCLE);
					}
					if (cloakTick > cloakCycle) {
						cloakTick = 0;
					}
					double angle = (Math.PI * 2.0 * cloakTick) / cloakCycle;
					setAlpha((int) ((Math.cos(angle) + 1.0) * 0.5 * (CLOAK_MAX_ALPHA - CLOAK_MIN_ALPHA) + CLOAK_MIN_ALPHA));
				}

				doBuildingTick();
				break;

			default:
				assert false;
				break;
		}
	}
	protected void doBuildingTick() {}

	protected void doGhostTick() {}

	/**
	 * Repair this building with a fraction of a shield
	 */
	public void repair() {
		if (phase == PHASE_NORMAL && hitPoints < feature.getHitPoints()) {
			hitPoints = Math.min(getMaxHitPoints(), hitPoints + BuildingFeature.HITPOINTS_DIVISOR);
			updateAppearance();
			spawnRepairEmitter();
			onRepaired();
		}
	}

	/**
	 * @return true if this building has been damaged
	 */
	public final boolean isDamaged() {
		return hitPoints < getMaxHitPoints();
	}

	private void updateShieldAppearance() {
		if (phase != PHASE_NORMAL) {
			return;
		}

		// Invulnerability takes precedence
		if (shielded) {
			if (shieldedLayers == null && feature.getShieldedAppearance() != null && shieldType != SHIELD_INVULN) {
				shieldedLayers = new SimpleThingWithLayers(GameScreen.getInstance());
				feature.getShieldedAppearance().createSprites(GameScreen.getInstance(), shieldedLayers);
				shieldedEmitter = feature.getShieldedAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
				shieldType = SHIELD_INVULN;
			}
		} else if (shields > 0) {
			if (shieldedLayers == null && feature.getForcefieldAppearance() != null && shieldType != SHIELD_FORCEFIELD) {
				shieldedLayers = new SimpleThingWithLayers(GameScreen.getInstance());
				feature.getForcefieldAppearance().createSprites(GameScreen.getInstance(), shieldedLayers);
				shieldedEmitter = feature.getForcefieldAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
				shieldType = SHIELD_FORCEFIELD;
			}
		} else {
			removeShieldEffects();
		}
	}

	@Override
	public final void addToGameState(GameStateInterface gsi) {
		if (phase == PHASE_GHOST) {
			return;
		}
		gsi.addToBuildings(this);
	}

	@Override
	public final void removeFromGameState(GameStateInterface gsi) {
		if (phase == PHASE_GHOST) {
			return;
		}
		gsi.removeFromBuildings(this);
	}

	@Override
	public void onCollisionWithBullet(Bullet bullet) {
		if (bullet.getSource() == this) {
			return;
		}

		if (!bullet.isDangerousToBuildings()) {
			return;
		}

		if (isBarricade() || isMineField()) {
			return;
		}

		if (explodingBullets != null && explodingBullets.contains(bullet)) {
			return;
		}
		if (explodingBullets == null) {
			explodingBullets = new ArrayList<Bullet>(8);
		}
		explodingBullets.add(bullet);

		damage(bullet.getDamage());
		bullet.onHit(true, this);
	}

	/**
	 * @return Returns the hitPoints remaining
	 */
	public int getHitPoints() {
		return hitPoints;
	}

	/**
	 * Repairs the building to full health
	 */
	public void repairFully() {
		if (phase != PHASE_NORMAL || isMineField() || isBarricade() || isGhost()) {
			return;
		}
		spawnRepairEmitter();

		if (hitPoints < getMaxHitPoints()) {
			hitPoints = getMaxHitPoints();
			updateAppearance();
			onRepaired();
		}
	}

	@Override
	public LayersFeature getMousePointer(boolean clicked) {
		if (canSell() && Worm.getGameState().isSelling()) {
			return Res.getMousePointerSellOn();
		} else {
			return super.getMousePointer(clicked);
		}
	}

	public int getMaxHitPoints() {
		return feature.getHitPoints();
	}

	@Override
	protected final void onSetLocation() {
		if (emitter != null) {
			feature.getAppearance(this).updateEmitters(emitter, getMapX(), getMapY());
		}
		if (proximityEffects != null) {
			int n = proximityEffects.size();
			for (int i = 0; i < n; i ++) {
				ProximityEffect fx = proximityEffects.get(i);
				fx.setTarget(getMapX() + getCollisionX(), getMapY() + getCollisionY());
			}
		}

		doBuildingSetLocation();
	}

	protected void doBuildingSetLocation() {}

	@Override
	protected final void doUpdate() {
		if (shieldedLayers != null) {
			for (int i = 0; i < shieldedLayers.getSprites().length; i ++) {
				shieldedLayers.getSprite(i).setLocation(getScreenX(), getScreenY());
			}
		}
		if (hoveredLayers != null) {
			for (int i = 0; i < hoveredLayers.getSprites().length; i ++) {
				hoveredLayers.getSprite(i).setLocation(getScreenX(), getScreenY());
			}
		}
		doBuildingUpdate();
	}
	protected void doBuildingUpdate() {}

	@Override
	protected LayersFeature getCurrentAppearance() {
		switch (phase) {
			case PHASE_NORMAL:
				return feature.getAppearance(this);
			case PHASE_DYING:
				return feature.getDeathAppearance();
			default:
				return null;
		}
	}

	/**
	 * @return true if this is a city
	 */
	public boolean isCity() {
		return false;
	}

	/**
	 * @return true if this is apparently a high-value building (a target for the rank and file)
	 */
	public boolean isApparentlyValuable() {
		return isCity();
	}

	protected boolean dontShowLabel() {
		return false;
	}

	@Override
	public void onHovered(int mode) {
		if (phase != PHASE_NORMAL) {
			return;
		}
		if (proximityEffects == null) {
			createProximityEffects(Res.GREEN);
		}
		net.puppygames.applet.effects.SFX.buttonHover();

		hovered = true;
		if (hoveredLayers == null && feature.getHoveredAppearance() != null) {
			hoveredLayers = new SimpleThingWithLayers(GameScreen.getInstance());
			feature.getHoveredAppearance().createSprites(GameScreen.getInstance(), hoveredLayers);
			hoveredEmitter = feature.getHoveredAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
		}
	}

	@Override
	public int onClicked(int mode) {
		if (mode == Mode.MODE_SELL) {
			if (canSell()) {
				Worm.getGameState().sell(this);
				return ClickAction.CONSUME;
			} else {
				return ClickAction.IGNORE;
			}
		} else {
			return super.onClicked(mode);
		}
	}

	@Override
	public void onLeave(int mode) {
		if (phase != PHASE_NORMAL) {
			return;
		}

		hovered = false;
		removeHoveredEffects();
	}

	@Override
	public boolean isAttackableByGidrahs() {
		return isActive() && phase == PHASE_NORMAL;
	}

	/**
	 * Used by gidrahs to determine if it's worth targeting this building for attack
	 * @return
	 */
	public boolean isWorthAttacking() {
		return true;
	}

	/**
	 * @return true if this building should have the "help I'm being attacked" effect
	 */
	public boolean shouldShowAttackWarning() {
		return isAttackableByGidrahs();
	}

	public int getReactors() {
		return reactors + Worm.getGameState().getReactorBoost();
	}

	public int getCoolingTowers() {
		return coolingTowers + Worm.getGameState().getCoolingBoost();
	}

	public int getBatteries() {
		return batteries + Worm.getGameState().getBatteryBoost();
	}

	public int getShields() {
		return shields + Worm.getGameState().getShieldBoost();
	}

	public int getScanners() {
		return scanners + Worm.getGameState().getScannerBoost();
	}

	public int getCrystals() {
		return crystals;
	}

	public int getCapacitors() {
		return capacitors;
	}

	public int getBases() {
		return bases;
	}

	public int getFactories() {
		return factories;
	}

	public int getSpawners() {
		return spawners;
	}

	public int getAutoLoaders() {
		return autoLoaders;
	}

	public int getCollectors() {
		return collectors;
	}

	public int getWarehouses() {
		return warehouses;
	}

	public int getTurrets() {
		return turrets;
	}

	public int getCloaks() {
	    return cloaks;
    }

	private boolean canAdjustProximity() {
		return phase == PHASE_NORMAL || phase == PHASE_GHOST;
	}

	public void addReactors(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		reactors += n;
		assert reactors >= 0 : this;
	}

	public void addCoolingTowers(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		coolingTowers += n;
		if (n > 0) {
			Worm.getGameState().flagHint(Hints.STACK);
		}
		assert coolingTowers >= 0 : this;
	}

	public void addBatteries(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		batteries += n;
		if (n > 0) {
			Worm.getGameState().flagHint(Hints.STACK);
		}
		assert batteries >= 0 : this;
	}

	public void addFactories(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		factories += n;
		assert factories >= 0 : this;
	}

	public void addBases(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		bases += n;
		assert bases >= 0 : this;
	}

	public void addCrystals(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		crystals += n;
		assert crystals >= 0 : this;
	}

	public void addShields(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		shields += n;
		assert shields >= 0 : this;
		updateShieldAppearance();
	}

	public void addShieldBoost() {
		if (!canAdjustProximity()) {
			return;
		}
		updateShieldAppearance();
	}

	public void addScanners(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		scanners += n;
		if (n > 0) {
			Worm.getGameState().flagHint(Hints.STACK);
		}
		assert scanners >= 0 : this;
	}

	public void addAutoLoaders(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		autoLoaders += n;
		assert autoLoaders >= 0 : this;
	}

	public void addCollectors(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		collectors += n;
		assert collectors >= 0 : this;
	}

	public void addWarehouses(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		warehouses += n;
		assert warehouses >= 0 : this;
	}

	public void addCapacitors(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		capacitors += n;
		assert capacitors >= 0 : this;
	}

	public void addSpawners(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		spawners += n;
		assert spawners >= 0 : this;
	}

	public void addTurrets(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		turrets += n;
		assert turrets >= 0 : this;
	}

	public void addCloaks(int n) {
		if (!canAdjustProximity()) {
			return;
		}
		cloaks += n;
		assert cloaks >= 0 : this;
		if (cloaks == 0) {
			setAlpha(255);
		}
	}

	/**
	 * @return true if the building is cloaked
	 */
	public boolean isCloaked() {
		return cloaks > 0;
	}


	/**
	 * Called at the end of the level on active buildings.
	 */
	public void onEndLevel() {
	}

	@Override
	public int crush() {
		remove();
		return 0;
	}

	/**
	 * @return true if we're being hovered by the mouse
	 */
	public boolean isHovered() {
		return hovered;
	}

	private void spawnRepairEmitter() {
		EmitterFeature ef = feature.getRepairEmitter();
		if (ef != null) {
			Emitter e = ef.spawn(GameScreen.getInstance());
			e.setLocation(getX(), getY()+(float)(getBounds(null).getHeight()*0.25));
			e.setOffset(GameScreen.getSpriteOffset());
		}
	}

	// chaz hack! test if offset will fix yoffset prob

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

	/**
	 * Cancel the grace period after building to get 100% of your money back
	 */
	public final void cancelUndo() {
		sellTick = 0;
	}

	/**
	 * @return the value of this building if it's sold.
	 */
	public int getSalePrice() {
		int value = (int) LinearInterpolator.instance.interpolate(0.0f, cost * (sellTick > 0 ? 1.0f : 0.4f), (float) getHitPoints() / (float) getMaxHitPoints());
		value /= 50;
		value *= 50;
		return value;
	}

	/**
	 * Called when a Bezerk powerup is activated
	 */
	public void onBezerk() {
	}

	@Override
	public String toString() {
		return getClass().getName()+"[phase="+phase+", "+getTileX()+", "+getTileY()+"]";
	}

	@Override
	public boolean laserDamage(int amount) {
		damage(amount);
		return true;
	}

	public boolean canSell() {
		return isActive() && phase == PHASE_NORMAL;
	}

	/**
	 * @return the building's agitation factor
	 */
	public float getAgitation() {
		return feature.getAgitation();
	}
}
