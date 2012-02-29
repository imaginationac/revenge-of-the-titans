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
import net.puppygames.applet.effects.BlastEffect;
import net.puppygames.applet.effects.Effect;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.LabelEffect;

import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import worm.AttenuatedColor;
import worm.CauseOfDeath;
import worm.ClickAction;
import worm.Entity;
import worm.GameMap;
import worm.GameStateInterface;
import worm.Hints;
import worm.Layers;
import worm.MapRenderer;
import worm.Medals;
import worm.Res;
import worm.SFX;
import worm.Stats;
import worm.Worm;
import worm.WormGameState;
import worm.brains.SmartBrainFeature;
import worm.effects.BuildingAttackedEffect;
import worm.features.GidrahFeature;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.screens.GameScreen;
import worm.weapons.WeaponFeature.WeaponInstance;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.interpolators.OpenLinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Range;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sound.SoundPlayer;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.Util;


/**
 * $Id: Gidrah.java,v 1.177 2010/11/07 01:26:59 foo Exp $
 * Gidrahs!
 * @author $Author: foo $
 * @version $Revision: 1.177 $
 */
public class Gidrah extends Entity {

	private static final long serialVersionUID = 1L;

	private static final ArrayList<Entity> COLLISIONS = new ArrayList<Entity>();

	private static final double GIDRAH_INITIAL_DISTANCE = 32.0; // Calibrate gidrah hitpoints to 64 tiles distant from base
	private static final float MAX_KNOCKBACK = 5.0f;
	private static final int ATTACK_DURATION = 60;
	private static final int MIN_ATTACK_DURATION = 10;
	private static final int BOSS_ATTACK_DURATION = 10;
	private static final int SPAWN_DURATION = 60;
	private static final int DERES_DURATION = 10;
	private static final int HITPOINTS_FADE = 16;
	private static final int HITPOINTS_DURATION = 120;
	private static final int MAX_SPAWNS = 16;
	private static final float WRAITH_VISIBILITY_DISTANCE = 256.0f;
	private static final int WRAITH_MIN_ALPHA = 10;
	private static final int WRAITH_XRAY_MIN_ALPHA = 128;
	private static final int WRAITHS_VISIBLE_TO_TURRETS_ALPHA = 224;
	private static final int DANGER_RECALC_THRESHOLD = 20;
	private static final int DANGER_DIVISOR = 5;
	public static final int MAX_DANGER = 100;
	private static final float MAX_SURVIVAL_DIFFICULTY_HITPOINTS_MULTIPLIER = 2.0f;
	private static final float MAX_SURVIVAL_DIFFICULTY_BOSS_HITPOINTS_MULTIPLIER = 1.5f;
	private static final float MAX_DIFFICULTY_HITPOINTS_MULTIPLIER = 3.0f;
	private static final float HITPOINTS_PER_SURVIVAL_LEVEL_TICK = 1.0f / 3600.0f; // Every 60 seconds, all gidrahs get an extra hitpoint in survival mode at difficulty 0.0
	private static final float MAX_HITPOINTS_PER_SURVIVAL_LEVEL_TICK = 1.0f / 1200.0f; // Every 20 seconds, all gidrahs get an extra hitpoint in survival mode at difficulty 1.0
	private static final float MAX_DIFFICULTY_BOSS_HITPOINTS_MULTIPLIER = 2.0f;

	// Sanity check timers.
	private static final int GIDLET_SAFETY_TIME = 60;
	private static final int CHECK_MAP_COLLISION_SAFETY = 60;
	private static final int ATTACK_ABORT_TIME = -360;

	private static final Rectangle TEMP_BOUNDS = new Rectangle();

	private static final Color AWARD_COLOR = new Color(255,0,0,200);

	/** The gidrah feature */
	private final GidrahFeature feature;

	/** Xrays researched? */
	private boolean xrays;

	/** Hitpoints */
	private int hitPoints;

	/** Gidrah wounds (0=healthy, increases as damage is taken) */
	private int wounds;

	/** Armoured damage tick */
	private int armouredDamage;

	/** Flash tick */
	private int flashTick;

	/** Hit by an exploding bullet? */
	private ArrayList<Bullet> explodingBullets;

	/** Smartbombs we've hit */
	private ArrayList<Smartbomb> smartbombs;

	/** Current appearance */
	private LayersFeature appearance;

	/** Emitters */
	private transient Emitter[] emitter;

	/** Stun */
	private int stunTick;

	/** Spawner tick */
	private int spawnerTick;

	/** Last attack tick (limits roaring) */
	private int lastAttackTick;

	/** Frozen */
	private static final AttenuatedColor ICE_COLOR = new AttenuatedColor(new Color(0,240,255));

	/** Exotic? */
	private boolean exotic;

	/** Weapon instance */
	private WeaponInstance weaponInstance;

	/** Movement handler */
	private final Movement movement;

	/** Building we're attacking */
	private Building attacking;

	/** Target building */
	private Entity target;

	/** Boss hitpoints */
	private Sprite hitPointsSprite;

	/** Hitpoints alpha */
	private int hitPointsAlpha;

	/** Target hitpoints alpha */
	private int targetHitPointsAlpha;

	/** Hitpoints alpha ticker */
	private int hitPointsAlphaTick;

	/** Parent gidrah */
	private Gidrah parent;

	/** Bomb we're carrying */
	private Bomb bomb;

	/** Gidlet safety tick */
	private int gidletSafetyTick;

	/** Map collision safety tick */
	private int mapSafetyTick;

	/** Appearance lock */
	private boolean locked;

	/** Phase */
	private int phase;
	private static final int PHASE_WAIT = 0;
	private static final int PHASE_SPAWN = 1;
	private static final int PHASE_ALIVE = 2;
	private static final int PHASE_DERES = 3;
	private static final int PHASE_DYING = 4;

	/** Timer for attacks */
	private int attackTick;

	/** How many gidrahs have we spawed from ourselves? */
	private int numSpawned;

	/** Whether frozen */
	private boolean frozen;

	/** Knockback */
	private float kx, ky;

	/** Request to set layers next tick */
	private LayersFeature layersRequest;

	/** Current appearance */
	private int currentAppearance = -1;
	private static final int APPEARANCE_IDLE = 0;
	private static final int APPEARANCE_MOVING = 1;
	private static final int APPEARANCE_FROZEN = 2;
	private static final int APPEARANCE_ATTACKING = 3;
	private static final int APPEARANCE_DEATH = 4;
	private static final int APPEARANCE_STUN = 5;

	private transient Effect attackEffect;

	private int type;
	private int tangled;

	/**
	 * C'tor
	 */
	public Gidrah(GidrahFeature feature, int tileX, int tileY, int type) {
		this.feature = feature;
		this.type = type;

		setLocation(tileX * MapRenderer.TILE_SIZE, tileY * MapRenderer.TILE_SIZE);
		if (feature.isFlying()) {
			movement = new FlyingMovement(this);
		} else {
			movement = new GidrahMovement(this);
		}
		calcHitPoints();
	}

	/**
	 * Gets the gidrah type (0, 1, 2, or 3) - used in Survival mode
	 * @return int
	 */
	public int getType() {
		return type;
	}

	/**
	 * Rethink routes
	 */
	public static void rethinkRoutes(Rectangle bounds) {
		ArrayList<Gidrah> gidrahs = Worm.getGameState().getGidrahs();
		int n = gidrahs.size();
		for (int i = 0; i < n; i ++) {
			Gidrah g = gidrahs.get(i);
			g.movement.maybeRethink(bounds);
		}
	}

	/**
	 * Rethink all targets
	 */
	public static void rethinkTargets() {
		ArrayList<Gidrah> gidrahs = Worm.getGameState().getGidrahs();
		int n = gidrahs.size();
		for (int i = 0; i < n; i ++) {
			Gidrah g = gidrahs.get(i);
			g.findTarget();
		}
	}

	@Override
	public float getBeamXOffset() {
		int bOff = feature.getBeamOffset().getX();
		if (this.isMirrored()) {
			bOff = -bOff;
		}
		return getFinalXOffset() + bOff;
	}

	@Override
	public float getBeamYOffset() {
		return getOffsetY() + getFinalYOffset() + feature.getBeamOffset().getY();
	}

	public int getWidth() {
		return feature.getBounds().getWidth();
	}

	public int getHeight() {
		return feature.getBounds().getHeight();
	}

	/**
	 * @param exotic the exotic to set
	 */
	public void setExotic(boolean exotic) {
		this.exotic = exotic;
	}

	@Override
	public boolean isShootable() {
		return canCollide();
	}

	@Override
	public boolean isLaserProof() {
		return feature.getArmour() >= 8;
	}

	@Override
	public boolean isSolid() {
		return canCollide() && !feature.isWraith();
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
		entity.onCollisionWithGidrah(this);
	}

	@Override
	public void onCollisionWithSmartbomb(Smartbomb smartbomb) {
		if (smartbombs == null) {
			smartbombs = new ArrayList<Smartbomb>(1);
		} else if (smartbombs.contains(smartbomb)) {
			return;
		}
		smartbombs.add(smartbomb);
		damage(100, 100, CauseOfDeath.SMARTBOMB);
	}

	@Override
	public void onCollisionWithBullet(Bullet bullet) {
		if (bullet.getSource() == this) {
			return;
		}

		if (!bullet.isDangerousToGidrahs()) {
			return;
		}

		// Wraiths are unharmed by bullets most of the time
		if (xrays || !feature.isWraith()) {
			if (explodingBullets != null) {
				for (int i = explodingBullets.size(); -- i >= 0; ) {
					Bullet b = explodingBullets.get(i);
					if (bullet == b) {
						return;
					}
					if (!b.isActive()) {
						explodingBullets.remove(i);
					}
				}
			}
			if (explodingBullets == null) {
				explodingBullets = new ArrayList<Bullet>(8);
			}
			explodingBullets.add(bullet);

			int damage = feature.isWraith() ? 1 + bullet.getExtraDamage() + bullet.getArmourPiercing() : bullet.getDamage();
			boolean wasDamaged;
			if (bullet.isExploding()) {
				explosionDamage(damage, false);
				wasDamaged = true;
			} else if (feature.isGidlet() && !bullet.isDangerousToGidlets()) {
				return;
			} else {
				wasDamaged = damage(damage, bullet.getArmourPiercing(), CauseOfDeath.BULLET);
			}
			if (bullet.isPassThrough() || feature.isWraith()) {
				bullet.onPassThrough();
			} else {
				bullet.onHit(wasDamaged, this);
			}
			if (!wasDamaged) {
				doDeflectAppearance(bullet);
			}

			if (isActive() && phase != PHASE_DYING) {
				Emitter e = Res.getGidrahPainEmitter().spawn(GameScreen.getInstance());
				e.setLocation(bullet.getMapX(), bullet.getMapY());
				e.setOffset(GameScreen.getSpriteOffset());
				if (!feature.isBoss()) {
					if (!(feature.getNoStunOnAttack() && attacking != null)) {
						stunTick = Math.min(120, stunTick + bullet.getStun());
						if (stunTick > 0 && isShootable()) {
							if (stunTick > 9) {
								setAppearance(APPEARANCE_STUN, feature.getStunAppearance());
								if (attacking != null || !isFrozen()) {
									attacking = null; // Stop the attack
								}
								movement.dontAttack();
							}
						}
					}
					// Knockback - only do a little knockback if bullet deflected
					float knockbackAmount = wasDamaged ? 1.0f : 0.5f;
					double angle = Math.atan2(bullet.getDY(), bullet.getDX());
					float factor = (bullet.getDamage() * 2.0f) / feature.getHitPoints() * knockbackAmount;
					knockback((float) Math.cos(angle) * factor, (float) Math.sin(angle) * factor);
				}
			}
		}
	}

	@Override
	public void explosionDamage(int damageAmount, boolean friendly) {
		if (!feature.isFlying()) {
			if (feature.isExploding()) {
				damage(100, 100, CauseOfDeath.EXPLOSION);
			} else if (feature.isBoss()) {
				// Bosses get randomized explosion damage to prevent minefields from killing them too easily
				damage(Util.random(1, damageAmount), damageAmount, CauseOfDeath.EXPLOSION);
			} else if (feature.isWraith()) {
				// Wraiths barely damaged by explosives
				damage(1, damageAmount, CauseOfDeath.EXPLOSION);
			} else if (feature.isGidlet() && gidletSafetyTick != 0) {
				// Immune!
				return;
			} else {
				// Ordinary gidrahs armour has no protection against explosions
				damage(damageAmount + feature.getArmour(), damageAmount, CauseOfDeath.EXPLOSION);
			}
		}
	}

	@Override
	public boolean isClickable() {
		return true;
	}

	@Override
	public LayersFeature getMousePointer(boolean clicked) {
		// Hackery! If we're calling getMousePointer it means the mouse is over us...
		if (hitPointsSprite != null) {
			targetHitPointsAlpha = 255;
			hitPointsAlphaTick = HITPOINTS_DURATION;
		}
		WormGameState gameState = Worm.getGameState();
		if (gameState.inRangeOfCapacitor()) {
			if (gameState.isSmartbombMode()) {
				return Res.getMousePointerSmartbomb();
			} else if (gameState.isBezerk()) {
				return Res.getMousePointerBezerkOnTarget();
			} else {
				return Res.getMousePointerOnTarget();
			}
		} else {
			if (gameState.isSmartbombMode()) {
				return Res.getMousePointerSmartbomb();
			} else {
				return Res.getMousePointerOutOfRange();
			}
		}
	}

	@Override
	public int onClicked(int mode) {
		return ClickAction.FIRE;
	}

	@Override
	public int crush() {
		if (feature.isFlying()) {
			// Ignore
			return 0;
		}

		if (feature.isBoss()) {
			assert false;
			return 0;
		}
		kill(CauseOfDeath.CRUSHED, false);
		return feature.isAngry() ? 6 : feature.isGidlet() ? 1 : 2;
	}

	/**
	 * Damage and maybe kill the gidrah
	 * @param amount The amount of damage to inflict
	 * @param armourPiercing TODO
	 * @return true if we damaged the gidrah, false if not
	 */
	protected boolean damage(int amount, int armourPiercing, int source) {

		int armour = Math.max(feature.getArmour() - armourPiercing, 0);
		int newAmount = Math.max(0, amount - armour);
		if (newAmount <= 0) {
			armouredDamage ++;
			if (armouredDamage > armour) {
				newAmount = Math.max(1, amount / 4);
				armouredDamage = 0;
			} else {
				newAmount = 0;
			}
		}
		if (newAmount == 0) {
			Worm.getGameState().flagHint(Hints.ARMOURED);
			return false;
		}
		wounds += newAmount;
		// If we've got hitpoints, make them visible
		if (hitPointsSprite != null) {
			updateHitPoints();
			targetHitPointsAlpha = 255;
			hitPointsAlphaTick = HITPOINTS_DURATION;
		}

		flashTick = 3;
		setFlash(true);
		if (wounds >= getHitPoints()) {
			kill(source, true);
		} else {
			Emitter e = Res.getGidrahPainEmitter().spawn(GameScreen.getInstance());
			e.setOffset(GameScreen.getSpriteOffset());
			e.setLocation(getMapX(), getMapY());
		}
		return true;
	}

	private int getHitPoints() {
		return hitPoints;
	}

	private void calcHitPoints() {
		int gameMode = Worm.getGameState().getGameMode();
		if (feature.isBoss()) {
			float mult;
			switch (gameMode) {
				case WormGameState.GAME_MODE_SURVIVAL:
				case WormGameState.GAME_MODE_XMAS:
					mult = MAX_SURVIVAL_DIFFICULTY_BOSS_HITPOINTS_MULTIPLIER;
					break;
				default:
					mult = MAX_DIFFICULTY_HITPOINTS_MULTIPLIER;
			}
			hitPoints = (int) OpenLinearInterpolator.instance.interpolate(feature.getHitPoints(), feature.getHitPoints() * mult, Worm.getGameState().getDifficulty() );
			Building base = Worm.getGameState().getBase();
			double dx = getTileX() - base.getTileX();
			double dy = getTileY() - base.getTileY();
			double actualDist = Math.sqrt(dx * dx + dy * dy);
			double ratio = actualDist / GIDRAH_INITIAL_DISTANCE;
			if (Game.DEBUG) {
				System.out.println("Boss @ ("+getTileX()+","+getTileY()+") dist "+actualDist+" from base @ ("+base.getTileX()+","+base.getTileY()+") vs "+GIDRAH_INITIAL_DISTANCE+", ratio "+ratio);
				System.out.println("Hitpoints were "+hitPoints);
			}
			hitPoints *= ratio;
			if (Game.DEBUG) {
				System.out.println("... and now "+hitPoints);
			}
		} else {
			if (gameMode == WormGameState.GAME_MODE_SURVIVAL || gameMode == WormGameState.GAME_MODE_XMAS) {
				hitPoints = (int) OpenLinearInterpolator.instance.interpolate
					(
						feature.getHitPoints() + Worm.getGameState().getLevelTick() * HITPOINTS_PER_SURVIVAL_LEVEL_TICK / (type + 1),
						feature.getHitPoints() * MAX_SURVIVAL_DIFFICULTY_HITPOINTS_MULTIPLIER + Worm.getGameState().getLevelTick() * MAX_HITPOINTS_PER_SURVIVAL_LEVEL_TICK / (type + 1),
						Worm.getGameState().getDifficulty()
					);
			} else {
				hitPoints = (int) OpenLinearInterpolator.instance.interpolate(feature.getHitPoints(), feature.getHitPoints() * MAX_DIFFICULTY_HITPOINTS_MULTIPLIER, Worm.getGameState().getDifficulty());
			}
		}
	}

	@Override
	public boolean isDisruptorProof() {
	    return feature.isDisruptorProof();
	}

	@Override
	public void disruptorDamage(int amount, boolean friendly) {
		if (!friendly || feature.isFlying() || isDisruptorProof()) {
			return;
		}
		stunDamage(amount);
		damage(amount, 100, CauseOfDeath.DISRUPTOR); // Penetrates all known armour
	}

	@Override
	public boolean laserDamage(int amount) {
		if (feature.isWraith()) {
			return false;
		}
		damage(amount, amount, CauseOfDeath.LASER); // Varying armour penetration
		return true;
	}

	@Override
	public void capacitorDamage(int amount) {
		damage(amount, 100, CauseOfDeath.CAPACITOR); // Penetrates all known armour
	}

	@Override
	public void stunDamage(int amount) {
		if (feature.isBoss()) {
			return;
		}
		if (feature.isDropAttack() && attacking != null) {
			return;
		}
		if (stunTick == 0) {
			Emitter e = Res.getGidrahPainEmitter().spawn(GameScreen.getInstance());
			e.setOffset(GameScreen.getSpriteOffset());
			e.setLocation(getMapX(), getMapY());
		}

		stunTick += amount;

		// stop any attack
		if (attacking != null) {
			attacking = null;
			movement.dontAttack();
			setAppearance(APPEARANCE_STUN, feature.getStunAppearance());
		} else if (!isFrozen()) {
			setAppearance(APPEARANCE_STUN, feature.getStunAppearance());
		}
		flashTick = 3;
		setFlash(true);
	}

	/**
	 * Kill the gidrah
	 * @param award TODO
	 */
	private void kill(int causeOfDeath, boolean award) {
		if (!isActive()) {
			return;
		}
		movement.dontAttack();
		attacking = null;
		WormGameState gameState = Worm.getGameState();
		if (feature.isBoss()) {
			gameState.addMoney(feature.getPoints());
			LabelEffect effect = new LabelEffect
				(
					net.puppygames.applet.Res.getTinyFont(),
					"$"+String.valueOf(feature.getPoints()),
					ReadableColor.WHITE,
					AWARD_COLOR,
					50,
					20
				);
			effect.spawn(GameScreen.getInstance());
			effect.setLayer(Layers.HUD);
			effect.setLocation(getX(), getMapY() + getHeight() + 10);
			effect.setVelocity(0.0f, 0.5f);
			effect.setAcceleration(0.0f, -0.01f);
			effect.setDelay(0);
		}

		// Increase danger
		if (causeOfDeath != CauseOfDeath.ATTACK) {
			int oldDanger = gameState.getMap().getDanger(getTileX(), getTileY());
			// Tangled gids warn other gids if they're killed whilst tangled
			int newDanger = isTangled() ? MAX_DANGER : Math.min(MAX_DANGER, oldDanger + feature.getPoints() / DANGER_DIVISOR);
			gameState.getMap().setDanger(getTileX(), getTileY(), newDanger);
			if (newDanger / DANGER_RECALC_THRESHOLD > oldDanger / DANGER_RECALC_THRESHOLD || (newDanger == MAX_DANGER && oldDanger < newDanger)) {
				// All gidrahs rethink your routes!
				Gidrah.rethinkRoutes(new Rectangle(getTileX() - 1, getTileY() - 1, 3, 3));
			}
		}


		unfreeze();
		gameState.onGidrahKilled(this, causeOfDeath);

		// Maybe spawn some more gidrahs?
		if (feature.getSpawnType() == GidrahFeature.SPAWN_TYPE_EXPLODE && causeOfDeath != CauseOfDeath.CRUSHED && causeOfDeath != CauseOfDeath.SMARTBOMB && award) {
			int numToSpawn = (int) (feature.getSpawnRate().getValue() + feature.getSpawnRate().getMin() * gameState.getSpawners());
			for (int i = 0; i < numToSpawn; i ++) {
				Gidrah spawned = feature.getSpawn().spawn(GameScreen.getInstance(), getTileX(), getTileY(), 0);
				spawned.phase = PHASE_ALIVE;
				// Randomize gidlet location
				if (feature.getSpawn().isGidlet()) {
					spawned.setLocation(spawned.getMapX() + Util.random(0, MapRenderer.TILE_SIZE - 1), spawned.getMapY() + Util.random(0, MapRenderer.TILE_SIZE - 1));
				}
				if (!spawned.isValidMove()) {
					spawned.remove();
				}
			}
		}

		// Maybe drop a powerup?
		if (feature.isAngry() && award) {
			Saucer saucer = new Saucer(getMapX() + getCollisionX(), getMapY() + getCollisionY());
			saucer.spawn(GameScreen.getInstance());
		}

		// Maybe it was a boss, moments away from killing the player?
		if (feature.isBoss() && target != null && getDistanceTo(target) < 64.0f) {
			gameState.awardMedal(Medals.PHEW_THAT_WAS_CLOSE);
		}

		if (feature.getMedal() != null && !feature.isBoss()) {
			gameState.awardMedal(feature.getMedal());
		}

		// chaz hack! remove hitpoints counter thing - maybe should fade out?

		if (feature.isBoss()) {
			if (hitPointsSprite != null) {
				hitPointsSprite.deallocate();
				hitPointsSprite = null;
			}
			Game.allocateSound(feature.getDeathBuffer(), feature.getDeathBuffer().getGain() * Worm.calcLoudGain(getX(), getY()), feature.getDeathBuffer().getPitch(), Game.class);
		} else {
			Game.allocateSound(feature.getDeathBuffer(), feature.getDeathBuffer().getGain() * Worm.calcGain(getX(), getY()), feature.getDeathBuffer().getPitch() * (feature.isAngry() ? 0.8f : 1.0f), Game.class);
		}

		setFlash(false);


		if (feature.getDeathAppearance() != null) {
			setAppearance(APPEARANCE_DEATH, feature.getDeathAppearance());
			phase = PHASE_DYING;
		} else if (feature.getDeathAppearanceLeft() != null && feature.getDeathAppearanceRight() != null){

			// chaz hack! for boss gidrah death

			if (isMirrored()) {
				setAppearance(APPEARANCE_DEATH, feature.getDeathAppearanceLeft());
			} else {
				setAppearance(APPEARANCE_DEATH, feature.getDeathAppearanceRight());
			}

			phase = PHASE_DYING;
		} else {
			// Just remove us for now
			remove();
		}
	}

	public boolean isFrozen() {
		return frozen;
	}

	/**
	 * Freeze the gidrah for a number of ticks
	 * @param duration
	 */
	public void freeze() {
		if (frozen || attacking != null && feature.isDropAttack()) {
			return;
		}
		frozen = true;
		attacking = null;
		movement.dontAttack();
		if (bomb != null) {
			dropBomb(0.0f, 0.0f);
		}
		setAppearance(APPEARANCE_FROZEN, feature.getFrozenAppearance());
		createIceShards();
	}

	@Override
	public final void addToGameState(GameStateInterface gsi) {
		gsi.addToGidrahs(this);
	}

	@Override
	public final void removeFromGameState(GameStateInterface gsi) {
		gsi.removeFromGidrahs(this);
	}

	@Override
	protected final void doSpawn() {
		emitter = feature.getAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
		if (feature.getWeapon() != null) {
			weaponInstance = feature.getWeapon().spawn(this);
		}
		if (feature.isBoss()) {
			hitPointsSprite = GameScreen.getInstance().allocateSprite(this);
			hitPointsSprite.setAlpha(0);
			hitPointsSprite.setLayer(Layers.HITPOINTS);
			hitPointsSprite.setScale(FPMath.fpValue(0.5));
			updateHitPoints();
		}
		if (feature.isGidlet()) {
			gidletSafetyTick = GIDLET_SAFETY_TIME;
		}
		if (feature.getBomb() != null) {
			spawnBomb();
		}
		xrays = Worm.getGameState().isResearched(ResearchFeature.XRAYS);
		tick();
		update();
	}

	private void updateHitPoints() {
		hitPointsSprite.setAppearance(Res.getBossHitPoints((float) wounds / getHitPoints()));
	}

	@Override
	protected final void doRespawn() {
		emitter = feature.getAppearance().createEmitters(GameScreen.getInstance(), getMapX(), getMapY());
	}

	@Override
	protected final void doRemove() {
		setEmitters(null);

		if (hitPointsSprite != null) {
			hitPointsSprite.deallocate();
			hitPointsSprite = null;
		}

		// Clean up brain and remove our occupation status
		movement.remove();

		if (parent != null) {
			parent.numSpawned --;
		}
	}

	private void setParent(Gidrah parent) {
		this.parent = parent;
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

	private void setAppearance(int appearanceType, LayersFeature newAppearance) {
		if (currentAppearance == appearanceType) {
			return;
		}
		if (currentAppearance == APPEARANCE_DEATH) {
			// Don't allow anything to stop or restart death appearance
			return;
		}
		currentAppearance = appearanceType;
		forceSetAppearance(newAppearance);
	}

	private void forceSetAppearance(LayersFeature newAppearance) {
		if (newAppearance == null) {
			// Only really put this check in here because gidrahs don't have idle anims specified yet
			return;
		}
		appearance = newAppearance;
		layersRequest = null;

		boolean mirrored = isMirrored();
		setEmitters(newAppearance.createEmitters(GameScreen.getInstance(), getMapX(), getMapY()));
		newAppearance.createSprites(GameScreen.getInstance(), getMapX(), getMapY(), this);
		setMirrored(mirrored);
	}

	@Override
	public LayersFeature getAppearance() {
		return appearance;
	}

	@Override
	public void setMirrored(boolean mirrored) {
		super.setMirrored(mirrored);
		if (bomb != null) {
			bomb.setMirrored(mirrored);
		}
	}

	@Override
	public void requestSetAppearance(LayersFeature newAppearance) {
		layersRequest = newAppearance;
	}

	@Override
	protected LayersFeature getCurrentAppearance() {
		return appearance;
	}

	@Override
	public float getZ() {
		return feature.getHeight();
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
		if (hitPointsSprite != null) {
			hitPointsSprite.setLocation(getScreenX()+feature.getHitPointsX(), getScreenY()+feature.getHitPointsY());
		}
	}

	@Override
	protected final void doTick() {
		switch (phase) {
			case PHASE_WAIT:
				doWaitTick();
				break;
			case PHASE_SPAWN:
				doSpawnTick();
				break;
			case PHASE_ALIVE:
				doAliveTick();
				break;
			case PHASE_DERES:
				doDeResTick();
				break;
			case PHASE_DYING:
				doDyingTick();
				break;
			default:
				assert false;
				break;
		}

	}

	private void doWaitTick() {
		// Wait until the location we're spawning at is empty
		maybeAddToCollisionManager();
		checkCollisions(COLLISIONS);
		for (int i = 0; i < COLLISIONS.size(); i ++) {
			Entity e = COLLISIONS.get(i);
			if (e instanceof Gidrah) {
				Gidrah g = (Gidrah) e;
				if (feature.isFlying() && g.isFlying()) {
					// Only worried about other flying gids being in the way
					return;
				} else if (e.isSolid()) {
					return;
				}
			}
		}

		// Ok, spawn now
		phase = PHASE_SPAWN;
		attackTick = 0;

	}

	private void doDyingTick() {
		if (layersRequest != null) {
			forceSetAppearance(layersRequest);
			layersRequest = null;
		}
		// Wait for sprite 0 to flag an event 1
		if (getEvent() == 1) {
			if (feature.getMedal() != null && feature.isBoss()) {
				Worm.getGameState().awardMedal(feature.getMedal());
			}
			remove();
		}
	}

	private void doSpawnTick() {
		attackTick ++;
		if (attackTick > SPAWN_DURATION) {
			phase = PHASE_ALIVE;
			attackTick = 0;

			setAlpha(calcAlpha());
		} else {
			setAlpha((int) CosineInterpolator.instance.interpolate(0.0f, calcAlpha(), (float) attackTick / SPAWN_DURATION));
		}
	}

	public boolean isVisibleToTurrets() {
		if (feature.isWraith()) {
			return xrays || calcAlpha() > WRAITHS_VISIBLE_TO_TURRETS_ALPHA;
		} else if (feature.isGidlet()) {
			return false;
		} else {
			return true;
		}
	}

	private int calcAlpha() {
		if (feature.isWraith()) {
			// Get distance from target and interpolate
			int minAlpha = xrays ? WRAITH_XRAY_MIN_ALPHA : WRAITH_MIN_ALPHA;
			if (target == null) {
				return minAlpha;
			}

			return (int) LinearInterpolator.instance.interpolate(255.0f, minAlpha, getDistanceTo(target) / WRAITH_VISIBILITY_DISTANCE);
		} else {
			return 255;
		}
	}

	private float getWeaponRange() {
		return OpenLinearInterpolator.instance.interpolate(feature.getMinWeaponRange(), feature.getMaxWeaponRange(), Worm.getGameState().getDifficulty());
	}

	private void doDeResTick() {
		attackTick ++;
		if (attackTick > DERES_DURATION) {
			phase = PHASE_SPAWN;
			attackTick = 0;
			setAlpha(calcAlpha());
		} else {
			setAlpha((int) LinearInterpolator.instance.interpolate(calcAlpha(), 0.0f, (float) attackTick / DERES_DURATION));
		}
	}

	private void doAliveTick() {
		if (attackEffect != null && !attackEffect.isActive()) {
			attackEffect = null;
		}

		if (flashTick > 0) {
			flashTick --;
			if (flashTick == 0) {
				setFlash(false);
			}
		}

		if (lastAttackTick > 0) {
			lastAttackTick --;
		}

		if (frozen && Worm.getGameState().getFreezeTick() == 0) {
			unfreeze();
		} else if (!frozen && Worm.getGameState().getFreezeTick() > 0) {
			freeze();
		}

		if (hitPointsAlpha < targetHitPointsAlpha) {
			hitPointsAlpha = Math.min(255, hitPointsAlpha + HITPOINTS_FADE);
			if (hitPointsSprite != null) {
				hitPointsSprite.setAlpha(hitPointsAlpha);
			}
		} else if (hitPointsAlphaTick > 0) {
			hitPointsAlphaTick --;
			if (hitPointsAlphaTick == 0) {
				targetHitPointsAlpha = 0;
			}
		} else if (hitPointsAlpha > targetHitPointsAlpha) {
			hitPointsAlpha = Math.max(0, hitPointsAlpha - HITPOINTS_FADE);
			if (hitPointsSprite != null) {
				hitPointsSprite.setAlpha(hitPointsAlpha);
			}
		}

		if (gidletSafetyTick > 0) {
			gidletSafetyTick --;
		}

		if (tangled > 0) {
			tangled --;
		}

		// Are we frozen?
		if (isFrozen()) {
			doKnockback();
			return;
		}

		if (stunTick > 0) {
			stunTick --;
			if (stunTick == 0) {
				setAppearance(APPEARANCE_IDLE, feature.getAppearance());
			}
			doKnockback();
			return;
		}

		if (isSolid() && !feature.isFlying()) {
			mapSafetyTick ++;
			if (mapSafetyTick > CHECK_MAP_COLLISION_SAFETY) {
				mapSafetyTick = 0;

				if (!Worm.getGameState().getMap().isClearPX(getBounds(TEMP_BOUNDS))) {
					kill(CauseOfDeath.GRID_BUG, false);
					System.out.println("Gidrah "+this+" was killed due to a grid bug");
					return;
				}
			}
		}


		// If we're attacking something, continue attacking:
		if (attacking != null) {
			if (attacking.isAlive()) {
				if (attackTick > 0) {
					attackTick --;
					if (attackTick <= 0) {
						attack(attacking);
					}
				} else {
					// If event=1, do the attack
					if (currentAppearance != APPEARANCE_ATTACKING) {
						// Er. Stop attacking!
						attacking = null;
						movement.dontAttack();
						normalAppearance();
						attackTick = 0;
					} else if (getEvent() == 1) {
						doAttack();
					} else if (getEvent() == 2) {
						finishAttack();
					} else {
						attackTick --;
						if (attackTick < ATTACK_ABORT_TIME) {
							// Hm, something is awry - stop attacking
							attacking = null;
							movement.dontAttack();
							normalAppearance();
							attackTick = 0;
						}
					}
				}

				if (layersRequest != null) {
					forceSetAppearance(layersRequest);
					layersRequest = null;
				}

				// Don't otherwise move or shoot
				return;
			} else {
				attacking = null;
				movement.dontAttack();
				// Return appearance to normal
				normalAppearance();
				attackTick = 0;
			}
		}

		// If we've got no target, find one:
		if (target == null || !target.isAttackableByGidrahs()) {
			findTarget();
		}

		if (target == null) {
			// Don't move or shoot
			setAppearance(APPEARANCE_IDLE, feature.getIdleAppearance());
			doKnockback();
			return;
		}

		// Shoot weapon if it's in range of the target AND we're allowed to
		if (weaponInstance != null) {
			weaponInstance.tick();
			if (weaponInstance.isAutomatic()) {
				// Aim at the nearest building and shoot it.
				fire();
			}
		}

		// Spawner?
		GameMap map = Worm.getGameState().getMap();
		if (numSpawned < MAX_SPAWNS && feature.getSpawnType() == GidrahFeature.SPAWN_TYPE_CONSTANT && Worm.getGameState().isLevelActive() &&
				getTileX() > 2 && getTileY() > 2 && getTileX() <= map.getWidth() - 2 && getTileY() <= map.getHeight() - 2)
		{
			if (spawnerTick > 0) {
				spawnerTick --;
			}
			if (spawnerTick == 0) {
				numSpawned ++;
				spawnerTick = (int) feature.getSpawnRate().getValue();
				Gidrah spawned = feature.getSpawn().spawn(GameScreen.getInstance(), getTileX(), getTileY(), 0);
				spawned.setParent(this);
				// Randomize gidlet location
				if (feature.getSpawn().isGidlet()) {
					Range range = feature.getSpawnDistance();
					if (range == null) {
						spawned.setLocation(spawned.getMapX() + Util.random(0, MapRenderer.TILE_SIZE - 1), spawned.getMapY() + Util.random(0, MapRenderer.TILE_SIZE - 1));
					} else {
						float xx = spawned.getMapX() + MapRenderer.TILE_SIZE / 2;
						float yy = spawned.getMapY() + MapRenderer.TILE_SIZE / 2;
						double angle = Math.random() * Math.PI * 2.0;
						float dist = range.getValue();
						xx += Math.cos(angle) * dist;
						yy += Math.sin(angle) * dist;
						spawned.setLocation(xx, yy);
					}
				}
				if (!spawned.isValidMove()) {
					spawned.remove();
				}
			}
		}

		// Now move if we're not being knocked back
		if (layersRequest != null) {
			forceSetAppearance(layersRequest);
			layersRequest = null;
		}
		if (kx == 0.0f && ky == 0.0f) {
			movement.tick();
			if (movement.isMoving()) {
				if (feature.isFlying() && bomb == null) {
					// Flying gid dropped bomb - wait for event=2 and then go back to normal appearance
					if (getEvent() == 2) {
						normalAppearance();
					}
				} else {
					setAppearance(APPEARANCE_MOVING, feature.getAppearance());
				}
			} else {
				// Only set idle appearance when not "locked"
				if (!locked) {
					setAppearance(APPEARANCE_IDLE, feature.getIdleAppearance());
				}
			}
		} else {
			setAppearance(APPEARANCE_IDLE, feature.getIdleAppearance());
			doKnockback();
		}

		setAlpha(calcAlpha());

	}

	/**
	 * Fire the weapon at a nearby target. If there is one.
	 */
	public void fire() {
		if (!weaponInstance.isReady()) {
			return;
		}
		Building aimAt = (Building) SmartBrainFeature.getInstance().findTarget(this);
		if (aimAt != null && getDistanceTo(aimAt) < getWeaponRange()) {
			weaponInstance.fire((int) (aimAt.getMapX() + aimAt.getCollisionX()), (int) (aimAt.getMapY() + aimAt.getCollisionY()));
		}
	}

	private void doKnockback() {
		if (kx == 0.0f && ky == 0.0f) {
			return;
		}

		float oldX = getMapX();
		float oldY = getMapY();

		mapSafetyTick = 0;
		setLocation(oldX + kx, oldY + ky);
		if (!isValidMove()) {
			setLocation(oldX, oldY);
			kx = 0.0f;
			ky = 0.0f;
		} else {
			// Slow down
			kx *= 0.4f;
			ky *= 0.4f;
			if (Math.abs(kx) < 0.1f) {
				kx = 0.0f;
			}
			if (Math.abs(ky) < 0.1f) {
				ky = 0.0f;
			}
		}
		// When we've stopped, tell the movement about it so it can work out how to get back on track
		if (kx == 0.0f && ky == 0.0f) {
			movement.adjust(getMapX(), getMapY());
		}
	}

	private void knockback(float kx, float ky) {
		if (feature.isWraith() || feature.isBoss() || feature.isGidlet() || feature.isFlying()) {
			return;
		}
		if (attacking != null) {
			attacking = null;
			movement.dontAttack();
		}
		this.kx += kx;
		this.ky += ky;
		float kb = (float) Math.sqrt(kx * kx + ky * ky);
		if (kb > MAX_KNOCKBACK) {
			kx = MAX_KNOCKBACK * kx / kb;
			ky = MAX_KNOCKBACK * ky / kb;
		}
	}

	/**
	 * Determines if our move takes us into another gidrah or solid building or map tile
	 */
	private boolean isValidMove() {
		// Flying gidrahs / ghosts can move anywhere
		if (!isSolid() || feature.isFlying()) {
			return true;
		}

		if (!Worm.getGameState().getMap().isClearPX(getBounds(TEMP_BOUNDS))) {
			return false;
		}

		Entity.getCollisions(TEMP_BOUNDS, COLLISIONS);
		int n = COLLISIONS.size();
		for (int i = 0; i < n; i ++) {
			Entity entity = COLLISIONS.get(i);
			if (entity == this) {
				continue;
			}
			if (entity.isActive() && entity.canCollide() && entity.isTouching(this) && entity.isSolid()) {
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

	private void unfreeze() {
		if (frozen) {
			createIceShards();
			for (int i = 0; i < getNumSprites(); i ++) {
				getSprite(i).setPaused(false);
			}
			// Back to normal appearance... unless we're a flying gid
			if (feature.isFlying() && feature.getAttackAppearance()!=null) {
				attackAppearance();
			} else {
				normalAppearance();
			}
			attacking = null;
			movement.dontAttack();
			frozen = false;
		}
	}

	private void createIceShards() {

		Emitter iceShards;

		if (feature.isBoss()) {
			iceShards = Res.getIceShardsBossEmitter().spawn(GameScreen.getInstance());
		} else if (feature.isAngry()) {
			iceShards = Res.getIceShardsAngryEmitter().spawn(GameScreen.getInstance());
		} else {
			iceShards = Res.getIceShardsSmallEmitter().spawn(GameScreen.getInstance());
		}

		iceShards.setLocation(getX(), getY());
		iceShards.setOffset(GameScreen.getSpriteOffset());
	}

	private void doDeflectAppearance(Bullet bullet) {
		Emitter emitter = feature.getDeflectEmitter().spawn(GameScreen.getInstance());

		emitter.setAngle(bullet.getHitAngle());
		int defXOffset = feature.getDeflectXOffset();
		if (defXOffset!=0 && isMirrored()) {
			defXOffset*=-1;
		}

		emitter.setLocation(getX()+defXOffset, getY()+feature.getDeflectYOffset());
		emitter.setOffset(GameScreen.getSpriteOffset());
	}

	@Override
	public void onCollisionWithBuilding(Building building) {
		if (attacking != null || isFrozen() || feature.isFlying()) {
			// Already attacking, or frozen
			return;
		}

		if (building.isMineField() || building.isCrystal()) {
			// It's a mine or crystal - ignore it
			return;
		}

		// Wraiths and gidlets ignore most barriers
		if (!building.isGidletProof() && (feature.isWraith() || feature.isGidlet())) {
			return;
		}

		if (building.isSlowDown()) {
			if (!feature.isBoss() && !feature.isWraith() && !feature.isFlying() && !feature.isGidlet()) {
				tangled = 2;
			}
			return;
		}

		initAttack(building);
	}

	/**
	 * @return true if the gidrah is tangled
	 */
	public boolean isTangled() {
		return tangled > 0;
	}

	/**
	 * Package private so we can call it from {@link FlyingMovement}
	 */
	void initAttack(Building building) {

		float xx = (getMapX() + building.getMapX()) / 2;
		float yy = (getMapY() + building.getMapY()) / 2;
		if (building.shouldShowAttackWarning() && attackEffect == null) {
			attackEffect = new BuildingAttackedEffect(building, xx, yy);
			attackEffect.setOffset(GameScreen.getSpriteOffset());
			attackEffect.spawn(GameScreen.getInstance());
		}

		ALBuffer roarBuffer = feature.getRoar();
		ALBuffer ambientRoarBuffer = feature.getAmbientRoar();
		if (roarBuffer != null && ambientRoarBuffer != null) {
			if (Game.isSFXEnabled() && lastAttackTick == 0) {
				lastAttackTick = 60;
				SoundPlayer player = Resources.get("gidrah.player");
				SoundEffect roar = player.allocate(roarBuffer, this);
				if (roar != null) {
					roar.setGain(roarBuffer.getGain() * Game.getSFXVolume() * Worm.calcGain(xx, yy), this);
					float twiddle = 1.0f - ((float)Math.random() / 20.0f);
					if (feature.isAngry()) {
						roar.setPitch(0.8f * roarBuffer.getPitch() * twiddle, this);
					} else {
						roar.setPitch(roarBuffer.getPitch() * twiddle, this);
					}
					SoundEffect ambient = player.allocate(ambientRoarBuffer, this);
					if (ambient != null) {
						ambient.setGain(ambientRoarBuffer.getGain() * Game.getSFXVolume() * Worm.calcGain(xx, yy), this);
						if (feature.isAngry()) {
							ambient.setPitch(0.8f * ambientRoarBuffer.getPitch() * twiddle, this);
						} else {
							ambient.setPitch(ambientRoarBuffer.getPitch() * twiddle, this);
						}
					}
				}
			}
		} else {
			if (Game.DEBUG) {
				System.out.println("Gidrah "+feature+" has no roar or ambient roar!");
			}

		}

		attack(building);
	}

	private void attack(Building building) {
		if (building.isWorthAttacking()) {
			Worm.getGameState().flagHint(Hints.TURRETS_IN_PATH);
		}

		// Start attack animation. On event id = 1 we perform the attack.
		attackAppearance();
		attacking = building;
		attackTick = 0;
		movement.attack();
	}

	private LayersFeature getAttackAppearance() {
		if (feature.getAttackAppearance() != null) {
			return feature.getAttackAppearance();
		} else {
			return isMirrored() ? feature.getAttackAppearanceLeft() : feature.getAttackAppearanceRight();
		}
	}

	/**
	 * Called when event=1 and there's a building being attacked
	 */
	private void doAttack() {
		Emitter emitter = Res.getBuildingDamageEmitter().spawn(GameScreen.getInstance());
		float attackLocationX = (getMapX() + getCollisionX() + attacking.getMapX() + attacking.getCollisionX()) / 2;
		float attackLocationY = (getMapY() + getCollisionY() + attacking.getMapY() + attacking.getCollisionY()) / 2;

		float gain;
		if (attacking.isBarricade()) {
			gain = 0.25f;
		} else {
			gain = attacking.getMaxHitPoints() / 40.0f;
		}

		SFX.buildingDamaged(attackLocationX, attackLocationY, gain);
		emitter.setLocation
			(
				attackLocationX,
				attackLocationY
			);

		if (attacking.isWorthAttacking()) {
			Worm.getGameState().addStat(Stats.ALIEN_ATTACKS_ON_BUILDINGS, 1);
		}

		// If gidrah is explody, blow it to bits
		if (feature.isExploding()) {
			BlastEffect effect = new BlastEffect(getMapX() + getCollisionX(), getMapY() + getCollisionY(), 16, 16, feature.getExplosionRadius() * 2.0f, feature.getExplosionRadius() * 2.0f, Res.getExplosionTexture());
			effect.setFadeWhenExpanding(true);
			effect.setOffset(GameScreen.getSpriteOffset());
			effect.spawn(GameScreen.getInstance());
			// Damage nearby buildings
			ArrayList<Building> buildings = new ArrayList<Building>(Worm.getGameState().getBuildings());
			int n = buildings.size();
			for (int i = 0; i < n; i ++) {
				Building b = buildings.get(i);
				if (b.canCollide() && b.isActive()) {
					if (b.isTouching(getMapX() + getCollisionX(), getMapY() + getCollisionY(), feature.getExplosionRadius())) {
						b.explosionDamage(feature.getStrength(), false);
					}
				}
			}
			kill(CauseOfDeath.ATTACK, false);
			setEvent(0);
			return;


		} else {
			attacking.damage(feature.getStrength());
			if (attacking.isBarricade() || feature.isBoss() || feature.isAngry()) {
				// If it's a barricade, or we're a boss, or we're angry, we slowly knock it down. Otherwise we die.
				// Slowly weaken angry gidrahs: they get 4 attacks, ish
				if ((feature.isWraith() || feature.isAngry()) && !attacking.isBarricade()) {
					wounds += Math.max(1, getHitPoints() / 4);
					if (wounds > getHitPoints()) {
						kill(CauseOfDeath.ATTACK, false);
						return;
					}
				}
			} else {
				kill(CauseOfDeath.ATTACK, false);
			}
		}

		setEvent(0);

	}

	/**
	 * Called when event=2 and there's a building being attacked... if the gidrah hasnt been killed attacking
	 */
	private void finishAttack() {
		if (feature.isDropAttack()) {
			return;
		}
		// Back to normal appearance...
		normalAppearance();
		// Then wait a bit before calling attack() again and starting the attack animation
		attackTick = getAttackDuration();

	}

	private void normalAppearance() {
		setAppearance(APPEARANCE_IDLE, feature.getAppearance());
	}

	private void attackAppearance() {
		setAppearance(APPEARANCE_ATTACKING, getAttackAppearance());
	}


	private int getAttackDuration() {
		if (feature.isBoss()) {
			return BOSS_ATTACK_DURATION;
		} else {
			return (int) LinearInterpolator.instance.interpolate(ATTACK_DURATION, MIN_ATTACK_DURATION, Worm.getGameState().getDifficulty());
		}
	}

	/**
	 * Find a new target.
	 */
	public void findTarget() {
		Entity newTarget = feature.getBrain().findTarget(this);
		if (newTarget != target) {
			movement.reset();
			target = newTarget;
		}
	}

	/**
	 * @return the feature
	 */
	public GidrahFeature getFeature() {
		return feature;
	}

	public void dropBomb(float vx, float vy) {
		if (bomb != null) {
			bomb.drop(vx, vy);
			bomb = null;
			if (phase == PHASE_ALIVE) {
				attackAppearance();
			}
		}
	}

	public void spawnBomb() {
		if (feature.getBomb() != null && bomb == null) {
			bomb = new Bomb(feature.getBomb(), this);
			bomb.spawn(GameScreen.getInstance());
			normalAppearance();
		}
	}

	@Override
	public String toString() {
		return "Gidrah["+System.identityHashCode(this)+","+feature+","+getTileX()+","+getTileY()+"]";
	}

	@Override
	public boolean isAttackableByUnits() {
		return !feature.isWraith() && canCollide();
	}

	public static void resetTotalThinkTime() {
		GidrahMovement.resetTotalThinkTime();
	}

	public static void init() {
		GidrahMovement.init();
	}

	/**
	 * Lock or unlock appearance
	 * @param locked
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public void setHitPoints(int hp) {
		this.hitPoints = hp;
	}

	@Override
	public boolean isFlying() {
		return feature.isFlying();
	}

	/**
	 * Called when a gidrah fails to plot a path to its destination several times in succession. We figure it's never going to by
	 * this point, which is a bug, really. So we'll kill the gidrah.
	 */
	public void onMovementFail() {
		System.out.println("Gidrah "+this+" failed to move!");
		kill(CauseOfDeath.GRID_BUG, false);
	}
}
