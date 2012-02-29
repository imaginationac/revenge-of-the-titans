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
package worm.buildings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.puppygames.applet.Game;

import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import worm.ClickAction;
import worm.Entity;
import worm.GameMap;
import worm.Hints;
import worm.Layers;
import worm.MapRenderer;
import worm.Medals;
import worm.Mode;
import worm.Res;
import worm.SFX;
import worm.Worm;
import worm.effects.RangeEffect;
import worm.entities.Building;
import worm.entities.Gidrah;
import worm.entities.Turret;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.screens.GameScreen;
import worm.weapons.WeaponFeature;

import com.shavenpuppy.jglib.resources.PointParser;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.Util;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: TurretBuildingFeature.java,v 1.96 2010/11/08 02:03:11 foo Exp $
 * A defence turret, which shoots autonomously
 * @author $Author: foo $
 * @version $Revision: 1.96 $
 */
public class TurretBuildingFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	protected static final ReadableColor RANGE_COLOR = new Color(255, 10, 10, 48);
	protected static final ReadableColor MIN_RANGE_COLOR = new Color(255, 255, 10, 48);
	protected static final ReadableColor BLAST_RANGE_COLOR = new Color(255, 10, 255, 48);

	private static final int LIGHTS_OFF_TIME = 120;
	private static final int ALPHA_ADJUST = 8;
	private static final float ATTENUATION = 256.0f;
	private static final int FIND_TARGET_INTERVAL = 8;
	private static final int USELESS_WARNING_TIME = 1800;
	private static final int RETARGET_TIME = 120;

	/** Target acquisistion */
	private static final ArrayList<Entity> ENTITIES = new ArrayList<Entity>();
	private static final ArrayList<Gid> TARGETS = new ArrayList<Gid>();
	private static class Gid {
		Gidrah gidrah;
		float distance;
	}
	private static final Rectangle TEMP = new Rectangle();


	private int maxReactors;
	private int maxScanners;

	/** Weapon */
	private String weapon;

	/** Ignore deflections */
	private boolean ignoreDeflection;

	/** Turret position offset */
	private int beamOffsetX, beamOffsetY;

	/** Light movement speed */
	private float lightSpeed;

	/** Base range, defaults to AIM_RANGE */
	private float baseRange;

	/** Range increment per scanner, defaults to RANGE_PER_SCANNER */
	private float rangeIncrement;

	/** Minimum range */
	private float minimumRange;

	/** Light */
	private String light;

	/** Light layer */
	private int lightLayer;

	/** Beam */
	private String beam;

	/** Beam layer */
	private int beamLayer;

	/** Reload appearance */
	private LayersFeature reloadAppearance, reloadingAppearance;

	/** heavy weapons use different dits */
	private boolean heavyWeapon;

	/** Bullet offsets, in sequence */
	private List<Point> barrels;

	/** Don't target flying targets */
	private boolean dontTargetFlyingTargets;

	/** Allow targeting in to mountains */
	private boolean targetIntoMountains;

	/*
	 * Transient
	 */

	private transient WeaponFeature weaponFeature;
	private transient Appearance beamResource;

	/**
	 * Building instances
	 */
	protected class TurretBuildingInstance extends Turret {

		private static final long serialVersionUID = 1L;

		private WeaponFeature.WeaponInstance weaponInstance;

		/** Find target tick */
		private int findTick;

		/** Useless tick */
		private int uselessTick;

		/** Sight check tick */
		private int sightTick;

		/** Retarget tick */
		private int retargetTick;

		/** Reload warning */
		private Sprite reloadSprite;

		/** Lighting */
		private Sprite beamSprite;

		/** Current target */
		private Entity target;

		/** Light position */
		private float lightX, lightY;

		/** Target light alpha */
		private int targetLightAlpha, currentLightAlpha;

		/** Lights off tick */
		private int lightsOffTick;

		/** Light attenuation */
		private float attenuation;

		/** Range indicator */
		private transient RangeEffect rangeEffect, minRangeEffect, blastRangeEffect;

		/** Danger radius added */
		private float dangerAdded;

		/** Extra damage (xenobiology research) */
		private int extraDamage;

		/** Extra scanning range (optics research) */
		private float extraScan;

		/** Extra minimum range (rockets with plastic charge pattern) */
		private float extraMinimum;

		/** Barrel */
		private int barrel;

		/** Ignore list */
		private ArrayList<Entity> ignore = new ArrayList<Entity>();

		/**
		 * C'tor
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected TurretBuildingInstance(boolean ghost) {
			super(TurretBuildingFeature.this, ghost);

		}

		@Override
		public void disruptorDamage(int amount, boolean friendly) {
			if (weaponFeature != null && weaponFeature.confersImmunityFromDisruptors()) {
				return;
			}
		    super.disruptorDamage(amount, friendly);
		}

		@Override
		public boolean isFiringAtAerialTargets() {
		    return target != null && target.isActive() && target.isFlying();
		}

		@Override
		public void onBezerk() {
			if (weaponInstance != null) {
				weaponInstance.instantReload();
				updateAppearance();
			}
		}

		@Override
		protected void doOnBuild() {
			if (weaponFeature != null) {
				weaponInstance = weaponFeature.spawn(this);
				addDanger(weaponFeature.getDanger());
			} else {
				addDanger(0); // Decoys!
			}

			// Create lights
			if (beamResource != null) {
				beamSprite = GameScreen.getInstance().allocateSprite(this);
				beamSprite.setLayer(beamLayer);
				beamSprite.setAppearance(beamResource);
				beamSprite.setAlpha(0);
			}
		}

		private void updateLight() {
			if (beamSprite != null) {
				beamSprite.setLocation(getScreenX() + getBeamOffsetX(), getScreenY() + getBeamOffsetY());
			}
		}

		@Override
		public WeaponFeature.WeaponInstance getWeapon() {
			return weaponInstance;
		}

		@Override
		public boolean usesAmmo() {
			return true;
		}

		@Override
		public boolean isApparentlyValuable() {
			return weaponFeature == null; // Decoy!
		}

		@Override
		public int getExtraDamage() {
			return extraDamage + Math.min(maxReactors, getReactors());
		}

		@Override
		public void setWeapon(WeaponFeature.WeaponInstance weapon) {
			this.weaponInstance = weapon;
		}

		@Override
		public float getBeamXOffset() {
			return 0.0f;
		}

		@Override
		public float getBeamYOffset() {
			return 0.0f;
		}

		@Override
		public void onBulletDeflected(Entity target) {
			if (ignoreDeflection || target != this.target) {
				return;
			}
			ignore.remove(target);
			ignore.add(target);
			this.target = null;
		}

		/**
		 * Find a new target
		 */
		protected void findTarget() {
			if (findTick > 0) {
				findTick --;
				return;
			}

			// Find a target and kill it
			target = null;
			TARGETS.clear();
			float minDist = getScanRadius();
			ENTITIES.clear();
			TEMP.setBounds((int) (getX() - minDist), (int) (getY() - minDist), (int) (minDist * 2.0f), (int) (minDist * 2.0f));
			Entity.getCollisions(TEMP, ENTITIES);
			int n = ENTITIES.size();
			if (n == 0) {
				// No gidrahs left!
				findTick = FIND_TARGET_INTERVAL;
				return;
			}

			for (int j = ignore.size(); -- j >= 0; ) {
				Entity e = ignore.get(j);
				if (!e.isActive()) {
					// Clean up the ignore list while we're at it...
					ignore.remove(j);
				}
			}

			// Get all the gidrahs in scanning range which we can potentially aim at
			outer: for (int i = 0; i < n; i ++) {
				Entity ent = ENTITIES.get(i);
				if (!(ent instanceof Gidrah)) {
					continue;
				}
				Gidrah g = (Gidrah) ent;
				// Ignore gidlets: need units to combat these! And wraiths: need capacitors for these!
				if (!g.isShootable() || !g.isVisibleToTurrets()) {
					continue;
				}

				// Rocket turrets ignore flying targets
				if (dontTargetFlyingTargets && g.getFeature().isFlying()) {
					continue;
				}

				float dist = g.getDistanceTo(getX(), getY());
				if (dist >= minDist || dist <= getMinimumRange()) {
					continue;
				}
				if (!canSee(g.getX(), g.getY(), g, targetIntoMountains)) {
					continue;
				}

				// If the gidrah is in the ignore list... ignore it
				for (int j = ignore.size(); -- j >= 0; ) {
					if (g == ignore.get(j)) {
						// Ignore!
						continue outer;
					}
				}
				Gid gid = new Gid();
				gid.gidrah = g;
				gid.distance = dist;

				TARGETS.add(gid);
			}

			if (TARGETS.size() == 0) {
				findTick = FIND_TARGET_INTERVAL;
				return;
			}

			// Sort the targets in order of distance, with a twiddle for flying gids so that they are preferred over ground based
			// gids if this is a laser ("targetIntoMountains" is true)
			Collections.sort(TARGETS, new Comparator<Gid>() {
				@Override
				public int compare(Gid g0, Gid g1) {
					if (targetIntoMountains) {
						if (g0.gidrah.isFlying() && !g1.gidrah.isFlying()) {
							return -1;
						} else if (!g0.gidrah.isFlying() && g1.gidrah.isFlying()) {
							return 1;
						}
					}
					if (g0.distance < g1.distance) {
						return -1;
					} else if (g0.distance > g1.distance) {
						return 1;
					} else {
						return 0;
					}
				}
			});

			target = TARGETS.get(0).gidrah;
			retargetTick = RETARGET_TIME;
			findTick = FIND_TARGET_INTERVAL;
		}

		@Override
		protected void doBuildingSetLocation() {
			lightX = getX() + getBeamOffsetX();
			lightY = getY() + getBeamOffsetY();

			if (rangeEffect != null) {
				rangeEffect.setLocation(getX(), getY());
			}
			if (minRangeEffect != null) {
				minRangeEffect.setLocation(getX(), getY());
			}
			if (blastRangeEffect != null) {
				blastRangeEffect.setLocation(getX(), getY());
			}
		}

		/**
		 * @return true if we have a current valid target that's in range and visible
		 */
		private boolean isTargetValid() {
			if (target == null || !target.isActive() || !target.isShootable()) {
				return false;
			}

			// Ensure target still in range
			float distanceTo = target.getDistanceTo(getX(), getY());
			if (distanceTo > getScanRadius() || distanceTo <= getMinimumRange()) {
				return false;
			}

			// Retarget every so often
			if (retargetTick > 0) {
				retargetTick --;
				if (retargetTick == 0) {
					return false;
				}
			}

			// Ensure we can still see the target every now and again
			if (sightTick > 0) {
				sightTick --;
				return true;
			}
			sightTick = Util.random(FIND_TARGET_INTERVAL / 2, FIND_TARGET_INTERVAL * 2);
			return canSee(target.getX(), target.getY(), target, targetIntoMountains);
		}

		@Override
		public float getScanRadius() {
			return baseRange + Math.min(maxScanners, getScanners()) * rangeIncrement + extraScan;
		}

		@Override
		public LayersFeature getMousePointer(boolean clicked) {
			if (Worm.getGameState().isSelling()) {
				return Res.getMousePointerSellOn();
			}

			if (Worm.getGameState().isBuilding() || weaponInstance == null || !weaponInstance.canReload()) {
				return super.getMousePointer(clicked);
			}

			return Res.getMousePointerReload();
		}

		@Override
		public void onReloaded() {
			updateAppearance();
		}

		@Override
		public int onClicked(int mode) {
			if (mode == Mode.MODE_SELL || mode == Mode.MODE_BUILD || weaponInstance == null || !weaponInstance.canReload()) {
				return super.onClicked(mode);
			}

			reload();

			return ClickAction.DRAG;
		}

		private void reload() {
			weaponInstance.reload(reloadSprite);
			updateAppearance();
			targetLightAlpha = 0;
			SFX.reload();
		}

		@Override
		public void createGhostProximityEffects() {
			super.createGhostProximityEffects();

			// Show the turret range effect for ghosts
			rangeEffect = new RangeEffect(RANGE_COLOR);
			rangeEffect.setLocation(getX(), getY());
			rangeEffect.spawn(GameScreen.getInstance());
			rangeEffect.setShow(true);
			rangeEffect.setRadius(getScanRadius());

			if (getMinimumRange() > 0.0f) {
				minRangeEffect = new RangeEffect(MIN_RANGE_COLOR);
				minRangeEffect.setLocation(getX(), getY());
				minRangeEffect.spawn(GameScreen.getInstance());
				minRangeEffect.setShow(true);
				minRangeEffect.setRadius(getMinimumRange());
			}

			if (weaponFeature != null) {
				weaponInstance = weaponFeature.spawn(this);
				if (weaponInstance.getBlastRange() > 0.0f) {
					blastRangeEffect = new RangeEffect(BLAST_RANGE_COLOR);
					blastRangeEffect.setLocation(getX(), getY());
					blastRangeEffect.spawn(GameScreen.getInstance());
					blastRangeEffect.setShow(true);
					blastRangeEffect.setRadius(weaponInstance.getBlastRange());
				}
			}
		}

		@Override
		protected void doBuildingSpawn() {
			if (weaponFeature != null) {
				reloadSprite = GameScreen.getInstance().allocateSprite(this);
				reloadSprite.setLayer(Layers.BUILDING_INFO);

				if (heavyWeapon) {
					reloadSprite.setAppearance(Res.getAmmoLarge(1, 1));
				} else {
					reloadSprite.setAppearance(Res.getAmmo(1, 1));
				}

				reloadSprite.setScale(FPMath.fpValue(TurretBuildingFeature.this.getAppearance(this).getScale()));

				extraDamage = Worm.getGameState().isResearched(ResearchFeature.BIOLOGY) ? 1 : 0;
				calcExtraScan();
			}
		}

		@Override
		protected void doGhostSpawn() {
			calcExtraScan();
		}

		private void calcExtraScan() {
			int optics = Worm.getGameState().isResearched(ResearchFeature.OPTICS) ? 1 : 0;
			int xrays = Worm.getGameState().isResearched(ResearchFeature.XRAYS) ? -1 : 0;
			extraScan = (optics + xrays) * rangeIncrement / 2.0f;
			boolean bigBlast = Worm.getGameState().isResearched(ResearchFeature.PLASTIC) && minimumRange > 0.0f;
			extraScan += bigBlast ? 32.0f : 0.0f;
			extraMinimum = bigBlast ? 32.0f : 0.0f;

		}

		private void addDanger(int amount) {
			if (amount == 0) {
				// It's a decoy
				Gidrah.rethinkTargets();
				return;
			}

			// Increase danger level nearby
			float scanRadius = getScanRadius()+ MapRenderer.TILE_SIZE;
			GameMap map = Worm.getGameState().getMap();
			int bottomLeftY = (int) (getY() - scanRadius - 1.0f) / MapRenderer.TILE_SIZE;
			int height = (int) (getY() + scanRadius + 1.0f)  / MapRenderer.TILE_SIZE;
			int bottomLeftX = (int) (getX() - scanRadius - 1.0f) / MapRenderer.TILE_SIZE;
			int width = (int) (getX()  + scanRadius + 1.0f)  / MapRenderer.TILE_SIZE;
			for (int yy = bottomLeftY; yy <= height; yy ++) {
				float dy = getY() - yy * MapRenderer.TILE_SIZE - MapRenderer.TILE_SIZE * 0.5f;
				for (int xx = bottomLeftX; xx <= width; xx ++) {
					float dx = getX() - xx * MapRenderer.TILE_SIZE - MapRenderer.TILE_SIZE * 0.5f;
					double dist = Math.sqrt(dx * dx + dy * dy);
					if (dist <= scanRadius + 1.0f && dist > getMinimumRange()) {
						map.setDanger(xx, yy, map.getDanger(xx, yy) + amount);
					}
				}
			}

			// All gidrahs rethink your routes!
			Gidrah.rethinkRoutes(new Rectangle(bottomLeftX, bottomLeftY, width + 1, height + 1));
		}

		@Override
		public void addScanners(int n) {
			if (weaponFeature != null && !isGhost()) {
				addDanger(-weaponFeature.getDanger());
			}
			super.addScanners(n);
			if (weaponFeature != null && !isGhost()) {
				addDanger(weaponFeature.getDanger());
			}

			checkAwesome();
		}

		@Override
		public void addBatteries(int n) {
			super.addBatteries(n);

			if (weaponInstance != null) {
				weaponInstance.addBatteries(n);
			}

			checkAwesome();
		}

		@Override
		protected void adjustProximity(Building target, int delta) {
			target.addTurrets(delta);
		}

		@Override
		protected void doBuildingUpdate() {
			if (weaponInstance != null && reloadSprite != null) {
				reloadSprite.setLocation(getScreenX(), getScreenY());
			}

			updateLight();
		}

		@Override
		protected void doBuildingDestroy() {
			if (weaponInstance != null) {
				weaponInstance.remove();
				weaponInstance = null;
			}
		}

		@Override
		protected void doBuildingRemove() {
			if (weaponFeature != null && !isGhost()) {
				addDanger(-weaponFeature.getDanger());
			}
		}

		@Override
		protected void doGhostTick() {
			if (rangeEffect != null) {
				rangeEffect.setLocation(getX(), getY());
				rangeEffect.setRadius(getScanRadius());
				rangeEffect.setVisible(isVisible());
			}
			if (minRangeEffect != null) {
				minRangeEffect.setLocation(getX(), getY());
				minRangeEffect.setRadius(getMinimumRange());
				minRangeEffect.setVisible(isVisible());
			}
			if (blastRangeEffect != null) {
				blastRangeEffect.setLocation(getX(), getY());
				blastRangeEffect.setRadius(weaponInstance.getBlastRange());
				blastRangeEffect.setVisible(isVisible());
			}
		}

		@Override
		protected void doBuildingTick() {
			if (weaponInstance == null) {
				return;
			}
			if (!isTargetValid()) {
				findTarget();
			}

			// Turn light to meet target
			float dist = 0.0f;
			float aimX = 0.0f, aimY = 0.0f;
			if (target != null) {
				uselessTick = 0;
				aimX = (int) target.getX();
				aimY = (int) target.getY();
			} else {
				uselessTick ++;
				if (uselessTick > USELESS_WARNING_TIME && Worm.getGameState().isLevelActive()) {
					uselessTick = 0;
					Worm.getGameState().flagHint(Hints.SELL_TURRETS);
				}
			}
			if (target != null && weaponInstance.getAmmo() > 0) {
				targetLightAlpha = 255;
				lightsOffTick = 0;
				float tx = aimX;
				float ty = aimY;
				float dx = tx - lightX;
				float dy = ty - lightY;

				dist = (float) Math.sqrt(dx * dx + dy * dy);
				if (dist > 0.0f) {
					float move = lightSpeed / dist;
					if (Math.abs(dx) <= lightSpeed) {
						lightX = tx;
					} else {
						lightX += dx * move;
					}
					if (Math.abs(dy) <= lightSpeed) {
						lightY = ty;
					} else {
						lightY += dy * move;
					}
				}

				// What's the new distance?
				dx = lightX - (getX() + getBeamOffsetX());
				dy = lightY - (getY() + getBeamOffsetY());

				if (beamSprite != null) {
					beamSprite.setAngle(FPMath.fpYaklyDegrees(Math.atan2(dy, dx)));
				}
			} else {
				if (lightsOffTick < LIGHTS_OFF_TIME) {
					lightsOffTick ++;
					if (lightsOffTick == LIGHTS_OFF_TIME) {
						targetLightAlpha = 0;
					}
				}
			}
			if (currentLightAlpha < targetLightAlpha) {
				currentLightAlpha = Math.min(targetLightAlpha, currentLightAlpha + ALPHA_ADJUST);
			} else if (currentLightAlpha > targetLightAlpha) {
				currentLightAlpha = Math.max(targetLightAlpha, currentLightAlpha - ALPHA_ADJUST);
			}
			if (beamSprite != null) {
				beamSprite.setAlpha(currentLightAlpha);
			}

			weaponInstance.tick();
			if (weaponInstance.isReady()) {
				if (target != null) {
					if (!weaponInstance.fire((int) aimX, (int) aimY)) {
						return;
					}
					cancelUndo();
					if (barrels != null) {
						barrel ++;
						if (barrel == barrels.size()) {
							barrel = 0;
						}
					}
					if (weaponInstance.getAmmo() == 0) {
						SFX.noAmmo();
						targetLightAlpha = 0;
						updateAppearance();
						reloadSprite.setAnimation(null);

						reload();

					} else {
						reloadSprite.setAnimation(null);

						// Reset color - animation changes alpha
						reloadSprite.setColors(ReadableColor.WHITE);

						if (heavyWeapon) {
							reloadSprite.setAppearance(Res.getAmmoLarge(weaponInstance.getAmmo(), weaponInstance.getMaxAmmo()));
						} else {
							reloadSprite.setAppearance(Res.getAmmo(weaponInstance.getAmmo(), weaponInstance.getMaxAmmo()));
						}

					}
				}
			}

			float mx = GameScreen.getInstance().getMouseX() - GameScreen.getSpriteOffset().getX();
			float my = GameScreen.getInstance().getMouseY() - GameScreen.getSpriteOffset().getY();
			if (isTouching(mx, my)) {
				if (rangeEffect == null) {
					rangeEffect = new RangeEffect(RANGE_COLOR);
					rangeEffect.setLocation(getX(), getY());
					rangeEffect.spawn(GameScreen.getInstance());
				}
				rangeEffect.setShow(true);
				if (getMinimumRange() > 0.0f) {
					if (minRangeEffect == null) {
						minRangeEffect = new RangeEffect(MIN_RANGE_COLOR);
						minRangeEffect.setLocation(getX(), getY());
						minRangeEffect.spawn(GameScreen.getInstance());
					}
					minRangeEffect.setShow(true);
				}
				if (weaponInstance.getBlastRange() > 0.0f) {
					if (blastRangeEffect == null) {
						blastRangeEffect = new RangeEffect(BLAST_RANGE_COLOR);
						blastRangeEffect.setLocation(getX(), getY());
						blastRangeEffect.spawn(GameScreen.getInstance());
					}
					blastRangeEffect.setShow(true);
				}
			} else {
				if (rangeEffect != null) {
					rangeEffect.setShow(false);
				}
				if (minRangeEffect != null) {
					minRangeEffect.setShow(false);
				}
				if (blastRangeEffect != null) {
					blastRangeEffect.setShow(false);
				}
			}
			if (rangeEffect != null) {
				rangeEffect.setRadius(getScanRadius());
			}
			if (minRangeEffect != null) {
				minRangeEffect.setRadius(getMinimumRange());
			}
			if (blastRangeEffect != null) {
				blastRangeEffect.setRadius(weaponInstance.getBlastRange());
			}
		}

		@Override
		protected void doRemoveSpecialEffects() {
			if (reloadSprite != null) {
				reloadSprite.deallocate();
				reloadSprite = null;
			}
			if (beamSprite != null) {
				beamSprite.deallocate();
				beamSprite = null;
			}
			if (rangeEffect != null) {
				rangeEffect.finish();
				rangeEffect = null;
			}
			if (minRangeEffect != null) {
				minRangeEffect.finish();
				minRangeEffect = null;
			}
			if (blastRangeEffect != null) {
				blastRangeEffect.finish();
				blastRangeEffect = null;
			}
		}

		public float getBeamOffsetX() {
			return beamOffsetX;
		}

		public float getBeamOffsetY() {
			return beamOffsetY;
		}

		@Override
		public float getOffsetX() {
			if (barrels == null) {
				return getBeamOffsetX();
			} else {
				return barrels.get(barrel).getX();
			}
		}

		@Override
		public float getOffsetY() {
			if (barrels == null) {
				return getBeamOffsetY();
			} else {
				return barrels.get(barrel).getY();
			}
		}

		@Override
		public void addCoolingTowers(int n) {
			super.addCoolingTowers(n);
			checkAwesome();
		}

		@Override
		public void addReactors(int n) {
			super.addReactors(n);
			checkAwesome();
		}

		@Override
		public void addAutoLoaders(int n) {
		    super.addAutoLoaders(n);
			checkAwesome();
		}

		private void checkAwesome() {
			if (isDecoy() || isGhost()) {
				return;
			}
			if (!Worm.getGameState().isAwesome() && getReactors() >= 4 && getScanners() >= 4 && getBatteries() >= 4 && getCoolingTowers() >= 4 && getAutoLoaders() >= 4) {
				Worm.getGameState().awardMedal(Medals.AWESOME);
				Worm.getGameState().setAwesome();
			}
		}

		private float getMinimumRange() {
			return minimumRange + extraMinimum;
	    }

	}

	/**
	 * @param name
	 */
	public TurretBuildingFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new TurretBuildingInstance(ghost);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		if (isDecoy()) {
			return feature instanceof ShieldGeneratorBuildingFeature;
		} else {
			return
					feature instanceof ReactorBuildingFeature
				||	feature instanceof ShieldGeneratorBuildingFeature
				||	feature instanceof BatteryBuildingFeature
				||	feature instanceof CoolingTowerBuildingFeature
				||	feature instanceof ScannerBuildingFeature
				||	feature instanceof AutoLoaderBuildingFeature
				|| 	feature instanceof CloakBuildingFeature
				;
		}
	}

	public boolean isDecoy() {
		return weaponFeature == null;
	}

	@Override
	public void getResearchStats(StringBuilder stats_1_text, StringBuilder stats_2_text) {
		super.getResearchStats(stats_1_text, stats_2_text);

		if (!isDecoy()) {
			weaponFeature.getResearchStats(stats_1_text, stats_2_text);
		}

	}

	@Override
	protected String getBuildingType() {
		if (isDecoy()) {
			return super.getBuildingType();
		} else {
			return Game.getMessage("ultraworm.researchstats.building_type_turret");
		}
	}

	@Override
	public void appendFullStats(StringBuilder dest) {
		super.appendFullStats(dest);

		if (weaponFeature != null) {
			weaponFeature.appendFullStats(dest);
		}
	}

	@Override
	public LayersFeature getAppearance(Building building) {
		TurretBuildingInstance turret = (TurretBuildingInstance) building;
		if (turret.weaponInstance == null || turret.weaponInstance.getAmmo() > 0) {
			return super.getAppearance(building);
		} else if (turret.weaponInstance.isReloading()) {
			return reloadingAppearance;
		} else {
			return reloadAppearance;
		}
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);
		List<Element> children = XMLUtil.getChildren(element, "barrel");
		if (children != null && children.size() > 0) {
			barrels = new ArrayList<Point>(children.size());
			for (Element child : children) {
				barrels.add(PointParser.parse(XMLUtil.getText(child, "0,0")));
			}
		}
	}

}
