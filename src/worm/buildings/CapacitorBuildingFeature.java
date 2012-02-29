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

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

import worm.Layers;
import worm.Res;
import worm.Worm;
import worm.effects.RangeEffect;
import worm.entities.Building;
import worm.entities.Capacitor;
import worm.screens.GameScreen;
import worm.weapons.WeaponFeature;
import worm.weapons.WeaponFeature.WeaponInstance;

import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;

/**
 * Capacitors
 */
public class CapacitorBuildingFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	private static final ReadableColor RANGE_COLOR = new Color(255, 224, 255, 48);

	private static final int LIGHTS_OFF_TIME = 20;
	private static final int ALPHA_ADJUST = 16;
	private static final float ATTENUATION = 256.0f;

	private float baseRange;
	private float rangePerReactor;
	private int maxReactors;
	private String weapon;
	private int beamOffsetX;
	private int beamOffsetY;

	/** Light */
	private String light;

	/** Light layer */
	private int lightLayer;

	/** Beam */
	private String beam;

	/** Beam layer */
	private int beamLayer;

	/** Min. light radius */
	private float lightRadius;

	/** Light scaling factor */
	private float lightScale;

	/** Light movement speed */
	private float lightSpeed;

	private transient WeaponFeature weaponFeature;
	private transient Appearance lightResource, beamResource;

	/**
	 * Building instances
	 */
	private class CapacitorBuildingInstance extends Building implements Capacitor {

		private static final long serialVersionUID = 1L;

		private WeaponInstance weaponInstance;

		/** Lighting */
		private Sprite lightSprite, beamSprite;

		/** Light position */
		private float lightX, lightY;

		/** Target light alpha */
		private int targetLightAlpha, currentLightAlpha;

		/** Lights off tick */
		private int lightsOffTick;

		/** Light attenuation */
		private float attenuation;

		/** Energy level */
		private Sprite reloadSprite;

		/** Range effect */
		private transient RangeEffect rangeEffect;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected CapacitorBuildingInstance(boolean ghost) {
			super(CapacitorBuildingFeature.this, ghost);
		}

		@Override
		public boolean zap(float mx, float my) {
			cancelUndo();
			// Check we're in range. Range is increased by nearby reactors.
			if (getDistanceTo(mx, my) > getZapRadius()) {
				// Scale to maximum range
				double dx = mx - (getMapX() + getBeamOffsetX());
				double dy = my - (getMapY() + getBeamOffsetY());
				double len = Math.sqrt(dx * dx + dy * dy);
				dx *= getZapRadius();
				dy *= getZapRadius();
				dx /= len;
				dy /= len;
				mx = getMapX() + getBeamOffsetX() + (float) dx;
				my = getMapY() + getBeamOffsetY() + (float) dy;
			}

			return weaponInstance.fire((int) mx, (int) my);
		}

		@Override
		protected void doOnBuild() {
			// Create lights
			if (lightResource != null) {
				lightSprite = GameScreen.getInstance().allocateSprite(this);
				lightSprite.setLayer(lightLayer);
				lightSprite.setAppearance(lightResource);
				lightSprite.setAlpha(0);
			}
			if (beamResource != null) {
				beamSprite = GameScreen.getInstance().allocateSprite(this);
				beamSprite.setLayer(beamLayer);
				beamSprite.setAppearance(beamResource);
				beamSprite.setAlpha(0);
			}
		}

		private void updateLight() {
			if (lightSprite != null) {
				float lx = GameScreen.getSpriteOffset().getX() + lightX;
				float ly = GameScreen.getSpriteOffset().getY() + lightY;
				lightSprite.setLocation(lx, ly);
			}
			if (beamSprite != null) {
				beamSprite.setLocation(getScreenX() + getBeamOffsetX(), getScreenY() + getBeamOffsetY());
			}
		}

		@Override
		protected void doBuildingSpawn() {
			weaponInstance = weaponFeature.spawn(this);

			reloadSprite = GameScreen.getInstance().allocateSprite(this);
			reloadSprite.setLayer(Layers.BUILDING_INFO);
			reloadSprite.setAppearance(Res.getEnergyAmmo(1, 1));
			reloadSprite.setScale(FPMath.fpValue(CapacitorBuildingFeature.this.getAppearance(this).getScale()));

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
			if (weaponInstance != null) {
				weaponInstance.remove();
				weaponInstance = null;
			}
		}

		@Override
		public boolean usesAmmo() {
			return true;
		}


		@Override
		protected void doBuildingUpdate() {
			if (weaponInstance != null && reloadSprite != null) {
				reloadSprite.setLocation(getScreenX(), getScreenY());
			}

			updateLight();
		}

		@Override
		protected void doRemoveSpecialEffects() {
			if (reloadSprite != null) {
				reloadSprite.deallocate();
				reloadSprite = null;
			}
			if (lightSprite != null) {
				lightSprite.deallocate();
				lightSprite = null;
			}
			if (beamSprite != null) {
				beamSprite.deallocate();
				beamSprite = null;
			}
			if (rangeEffect != null) {
				rangeEffect.finish();
				rangeEffect = null;
			}
		}

		@Override
		protected void doBuildingSetLocation() {
			lightX = getMapX() + getBeamOffsetX();
			lightY = getMapY() + getBeamOffsetY();

			if (rangeEffect != null) {
				rangeEffect.setLocation(getMapX() + getCollisionX(), getMapY() + getCollisionY());
			}
		}

		@Override
		public void createGhostProximityEffects() {
			super.createGhostProximityEffects();

			// Show the turret range effect for ghosts
			rangeEffect = new RangeEffect(RANGE_COLOR);
			rangeEffect.setLocation(getMapX() + getCollisionX(), getMapY() + getCollisionY());
			rangeEffect.spawn(GameScreen.getInstance());
			rangeEffect.setShow(true);
			rangeEffect.setRadius(getZapRadius());
		}

		@Override
		protected void doGhostTick() {
			rangeEffect.setLocation(getMapX() + getCollisionX(), getMapY() + getCollisionY());
			rangeEffect.setRadius(getZapRadius());
		}

		@Override
		protected void doBuildingTick() {
			// Turn light to meet mouse
			float mx = GameScreen.getInstance().getMouseX() - GameScreen.getSpriteOffset().getX();
			float my = GameScreen.getInstance().getMouseY() - GameScreen.getSpriteOffset().getY();
			if (getDistanceTo(mx, my) <= getZapRadius()) {
				if (rangeEffect == null) {
					rangeEffect = new RangeEffect(RANGE_COLOR);
					rangeEffect.setLocation(getX(), getY());
					rangeEffect.spawn(GameScreen.getInstance());
				}
				rangeEffect.setShow(true);
				targetLightAlpha = 255;
				lightsOffTick = 0;
				float dx = mx - lightX;
				float dy = my - lightY;
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				if (dist > 0.0f) {
					float move = lightSpeed / dist;
					if (Math.abs(dx) <= lightSpeed) {
						lightX = mx;
					} else {
						lightX += dx * move;
					}
					if (Math.abs(dy) <= lightSpeed) {
						lightY = my;
					} else {
						lightY += dy * move;
					}
				}

				// What's the new distance?
				dx = lightX - (getX() + getBeamOffsetX());
				dy = lightY - (getY() + getBeamOffsetY());

				// Scale the light according to current light distance from us
				if (lightSprite != null) {
					dist = (float) Math.sqrt(dx * dx + dy * dy);
					attenuation = Math.max(0.0f, 1.0f - dist / ATTENUATION);
					lightSprite.setScale(FPMath.fpValue(dist * lightScale / lightRadius));
				}
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
				if (rangeEffect != null) {
					rangeEffect.setShow(false);
				}
			}
			if (rangeEffect != null) {
				rangeEffect.setRadius(getZapRadius());
			}
			if (currentLightAlpha < targetLightAlpha) {
				currentLightAlpha = Math.min(targetLightAlpha, currentLightAlpha + ALPHA_ADJUST);
			} else if (currentLightAlpha > targetLightAlpha) {
				currentLightAlpha = Math.max(targetLightAlpha, currentLightAlpha - ALPHA_ADJUST);
			}
			if (lightSprite != null) {
				lightSprite.setAlpha((int) (currentLightAlpha * attenuation));
			}
			if (beamSprite != null) {
				beamSprite.setAlpha(currentLightAlpha);
			}
			reloadSprite.setAppearance(Res.getEnergyAmmo(weaponInstance.getAmmo(), weaponInstance.getMaxAmmo()));
			weaponInstance.tick();
		}

		@Override
		public float getZapRadius() {
			return baseRange + (Worm.getGameState().getCapacitorBoost() + Math.min(maxReactors, getReactors())) * rangePerReactor;
		}

		public float getBeamOffsetX() {
			return beamOffsetX;
		}

		public float getBeamOffsetY() {
			return beamOffsetY;
		}

	}

	/**
	 * @param name
	 */
	public CapacitorBuildingFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new CapacitorBuildingInstance(ghost);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		return
				feature instanceof ReactorBuildingFeature
			|| 	feature instanceof ShieldGeneratorBuildingFeature
			||	feature instanceof BatteryBuildingFeature
			|| 	feature instanceof CloakBuildingFeature
			;
	}

}
