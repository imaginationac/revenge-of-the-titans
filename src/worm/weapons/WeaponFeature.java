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
package worm.weapons;

import java.io.Serializable;
import java.text.DecimalFormat;

import net.puppygames.applet.Game;

import org.lwjgl.util.ReadableColor;

import worm.Entity;
import worm.Res;
import worm.Statistics;
import worm.Worm;
import worm.entities.Building;
import worm.features.BulletFeature;
import worm.features.ResearchFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.Range;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.Util;

/**
 * Weaponry
 * @author Cas
 */
public class WeaponFeature extends Feature implements Statistics {

	private static DecimalFormat RATE_FORMAT = new DecimalFormat("##.#");

	/** Minimum fire rate */
	private int minFireRate;

	/** Minimum reload speed */
	private int minReloadSpeed;

	protected int maxCoolingTowers;
	protected int maxBatteries;
	protected int maxReactors;
	protected int maxAutoloaders;

	/** Firing rate (either in ticks, when expressed as a range for units/gidrahs, or in rounds / minute (3600 ticks)) */
	private Range fireRate;

	/** Sound when fired */
	private String sound;

	/** Magazine */
	private int magazine;

	/** Ammo per battery */
	private int ammoPerBattery;

	/** Fire rate adjustment per cooling tower, rounds / minute */
	private int fireRatePerCoolingTower;

	/** Reload per reactor */
	private int reloadPerReloader;

	/** Reload speed */
	private int reloadSpeed;

	/** Heavy weapon ? */
	private boolean heavyWeapon;

	/** Bullets */
	protected BulletFeature bullet;

	/** Constant reload */
	private boolean constantReload;

	/** Danger */
	private int danger;

	/** Multiple sounds */
	private ResourceArray sounds;

	private transient ALBuffer soundResource;

	public class WeaponInstance implements Serializable {
		private static final long serialVersionUID = 1L;

		protected final Entity entity;

		/** Ammo remaining */
		private int ammo;

		/** Extra ammo */
		private int extraAmmo;

		/** Extra cooling */
		private boolean extraCooling;

		/** Faster reloading */
		private int faster;

		/** Reloading? */
		private boolean reloading;


		/** Fire down? */
		private boolean latched;

		private int tick;
		private int targetX, targetY;

		private Sprite reloadSprite;
		private transient SoundEffect soundInstance;

		public WeaponInstance(Entity entity) {
			this.entity = entity;
			tick = Util.random(0, (int) fireRate.getValue());
			extraAmmo = Worm.getGameState().isResearched(ResearchFeature.LITHIUM) ? ammoPerBattery / 2 : 0;
			extraCooling = Worm.getGameState().isResearched(ResearchFeature.SODIUM);
			faster = Worm.getGameState().isResearched(ResearchFeature.PRECISION) ? reloadPerReloader / 2 : 0;
			ammo = getMaxAmmo();
		}

		public void addBatteries(int n) {
			if (n > 0 && ((Building) entity).getBatteries() < maxBatteries && ammo != 0 && !reloading) {
				ammo += ammoPerBattery * n;
			}
		}

		public int getMaxAmmo() {
			if (entity.usesAmmo()) {
				return magazine + ammoPerBattery * Math.min(maxBatteries, ((Building) entity).getBatteries()) + extraAmmo;
			} else {
				return magazine;
			}
		}

		public WeaponFeature getFeature() {
			return WeaponFeature.this;
		}

		public void reload(Sprite reloadSprite) {
			if (ammo == getMaxAmmo() || reloading) {
				return;
			}
			ammo = 0;
			reloading = true;
			tick = getReloadTime();
			this.reloadSprite = reloadSprite;
			reloadSprite.setAnimation(null);
			reloadSprite.setColors(ReadableColor.WHITE);

			if (heavyWeapon) {
				reloadSprite.setImage(Res.getReloadLarge(1.0f));
			} else {
				reloadSprite.setImage(Res.getReload(1.0f));
			}
		}

		/**
		 * @return true if the weapon is ready to fire
		 */
		public boolean isReady() {
			return tick == 0 &&
				(
					!reloading && ammo > 0
				||  magazine == 0
				||	!entity.usesAmmo()
				);
		}

		/**
		 * Gidrah weapons: is this weapon automatically fired at a nearby target when ready?
		 * @return true by default
		 */
		public boolean isAutomatic() {
			return true;
		}

		public boolean isReloading() {
			return reloading;
		}

		public int getAmmo() {
			return ammo;
		}

		public boolean isLatched() {
			return latched;
		}

		/**
		 * Fire the weapon! The weapon may first delay and will then start shooting.
		 * @param newTargetX
		 * @param newTargetY
		 * @return true if a round was expended
		 */
		public boolean fire(int newTargetX, int newTargetY) {
			this.targetX = newTargetX;
			this.targetY = newTargetY;

			latched = true;

			// Reloading etc?
			if (!isReady()) {
				return false;
			}

			// Allowed to fire?
			if (entity.usesAmmo()) {
				boolean bezerk = Worm.getGameState().isBezerk();
				if (constantReload) {
					tick = 1;
				} else {
					int towers = bezerk ? maxCoolingTowers : Math.min(maxCoolingTowers, ((Building) entity).getCoolingTowers());
					int extraRounds = fireRatePerCoolingTower * towers + (extraCooling ? Math.max(1, fireRatePerCoolingTower / 2) : 0);
					tick = (int) Math.max(minFireRate, (int) 3600.0f / (fireRate.getMin() + extraRounds));
				}
				if (!bezerk && magazine > 0) {
					ammo --;
				}
			} else {
				tick = (int) fireRate.getValue();
			}

			doFire(newTargetX, newTargetY);

			// Shooty noise
			if (soundResource != null) {
				soundInstance = Game.allocateSound(soundResource, Worm.calcGain(entity.getMapX() + entity.getOffsetX(), entity.getMapY() + entity.getOffsetY()), 1.0f, this);
			} else if (sounds != null) {
				ALBuffer s = (ALBuffer) sounds.getResource(Util.random(0, sounds.getNumResources() - 1));
				soundInstance = Game.allocateSound(s, Worm.calcGain(entity.getMapX() + entity.getOffsetX(), entity.getMapY() + entity.getOffsetY()), 1.0f, this);
			}

			return true;
		}

		/**
		 * @return Returns the targetX.
		 */
		protected int getTargetX() {
			return targetX;
		}
		/**
		 * @return Returns the targetY.
		 */
		protected int getTargetY() {
			return targetY;
		}


		/**
		 * @return Returns the entity.
		 */
		protected Entity getEntity() {
			return entity;
		}

		protected void doFire(float newTargetX, float newTargetY) {
			bullet.spawn(GameScreen.getInstance(), entity, (int) (entity.getMapX() + entity.getOffsetX()), (int) (entity.getMapY() + entity.getOffsetY()), (int) newTargetX, (int) newTargetY, entity.getExtraDamage());
		}

		public void remove() {
		}

		public float getBlastRange() {
			return 0.0f;
		}

		public void instantReload() {
			ammo = getMaxAmmo();

			tick = 0;
			reloading = false;
			if (reloadSprite != null) {
				reloadSprite.setAnimation(null);

				if (heavyWeapon) {
					reloadSprite.setAppearance(Res.getAmmoLarge(ammo, ammo));
				} else {
					reloadSprite.setAppearance(Res.getAmmo(ammo, ammo));
				}
			}

			if (entity.usesAmmo()) {
				entity.onReloaded();
			}
		}

		public void tick() {
			if (constantReload) {

				if (!latched) {
					if (getAmmo() < getMaxAmmo()) {
						if (tick == 0) {
							tick = getReloadTime();
						} else {
							tick --;
							if (tick == 0) {
								ammo ++;
							}
						}
					}

				} else {
					latched = false;
					if (tick > 0) {
						tick --;
					}
				}

			} else if (tick > 0) {
				tick --;
				if (reloading) {
					if (tick == 0) {
						instantReload();
					} else {
						reloadSprite.setAnimation(null);

						if (heavyWeapon) {
							reloadSprite.setImage(Res.getReloadLarge((float) tick / getReloadTime()));
						} else {
							reloadSprite.setImage(Res.getReload((float) tick / getReloadTime()));
						}
					}
				}
			}
		}

		private int getReloadTime() {
			if (reloadSpeed < minReloadSpeed) {
				return Math.max(1, reloadSpeed - Math.min(maxAutoloaders, ((Building) entity).getAutoLoaders()) * reloadPerReloader - faster);
			} else {
				return Math.max(minReloadSpeed, reloadSpeed - Math.min(maxAutoloaders, ((Building) entity).getAutoLoaders()) * reloadPerReloader - faster);
			}
		}

		public boolean canReload() {
			// Don't allow reloading if only one shot's been fired
			return ammo < getMaxAmmo() - 1 && !reloading && magazine > 0 && !constantReload;
		}

	}

	/**
	 * C'tor
	 * @param name
	 */
	public WeaponFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return Returns the soundResource.
	 */
	public final ALBuffer getSound() {
		return soundResource;
	}

	/**
	 * @return Returns the bullet feature to use.
	 */
	public BulletFeature getBulletFeature() {
		return bullet;
	}

	/**
	 * Spawn a weapon to be used by the entity
	 * @param entity
	 * @return the weapon instance
	 */
	public WeaponInstance spawn(Entity entity) {
		return new WeaponInstance(entity);
	}

	public boolean isLaser() {
		return false;
	}

	public boolean isHeavyWeapon() {
	    return heavyWeapon;
    }

	public boolean isDisruptor() {
		return false;
	}

	public int getMagazine() {
		return magazine;
	}

	protected final int getAmmoPerBattery() {
		return ammoPerBattery;
	}

	public int getDanger() {
		return danger;
	}

	protected final String getFireRateDescription() {
		StringBuilder sb = new StringBuilder(10);

		float rate = fireRate.getMin();
		sb.append(RATE_FORMAT.format(rate));
		if (Worm.getGameState().isResearched(ResearchFeature.COOLINGTOWER)) {
			float rate2 = fireRate.getMin() + fireRatePerCoolingTower * maxCoolingTowers;
			sb.append("-");
			sb.append(RATE_FORMAT.format(rate2));
		}
		sb.append("/min");
		return sb.toString();
	}

	/**
	 * @return the reloadPerReactor
	 */
	protected final int getReloadPerReactor() {
		return reloadPerReloader;
	}

	/**
	 * @return the reloadSpeed
	 */
	protected final int getReloadSpeed() {
		return reloadSpeed;
	}

	public void getResearchStats(StringBuilder stats_1_text, StringBuilder stats_2_text) {
		stats_1_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.weaponstats.damage")+": {color:text-bold}");
		stats_1_text.append(getDamageStats());

		stats_2_text.append("\n{color:text}"+Game.getMessage("ultraworm.weaponstats.firerate")+": {color:text-bold}");
		stats_2_text.append(getFireRateDescription());

		stats_2_text.append("\n{color:text}"+Game.getMessage("ultraworm.weaponstats.ammo")+": {color:text-bold}");
		stats_2_text.append(getMagazine());
		if (Worm.getGameState().isResearched(ResearchFeature.BATTERY)) {
			stats_2_text.append("+");
			stats_2_text.append(getAmmoPerBattery());
			stats_2_text.append("/");
			stats_2_text.append(Game.getMessage("ultraworm.weaponstats.battery"));
		}
		stats_2_text.append("\n{color:text}"+Game.getMessage("ultraworm.weaponstats.reload")+": {color:text-bold}");
		stats_2_text.append(getReloadSpeed() / 60);

		if (Worm.getGameState().isResearched(ResearchFeature.AUTOLOADER)) {
			stats_2_text.append("-");
			stats_2_text.append(getReloadPerReactor() / 60);
			stats_2_text.append("s/"+Game.getMessage("ultraworm.weaponstats.reloader"));
		} else {
			stats_2_text.append("s");
		}
	}

	protected String getDamageStats() {
		StringBuilder sb = new StringBuilder();
		sb.append(bullet.getDamage());
		return sb.toString();
	}

	protected String getArmourPiercingStats() {
		StringBuilder sb = new StringBuilder();
		if (bullet != null && bullet.getArmourPiercing() > 0) {
			sb.append("\n "+Game.getMessage("ultraworm.weaponstats.armour_piercing")+": ");
			sb.append(bullet.getArmourPiercing());
		}
		return sb.toString();
	}

	protected String getStunStats() {
		StringBuilder sb = new StringBuilder();
		if (bullet != null && bullet.getStun() > 0) {
			sb.append("\n "+Game.getMessage("ultraworm.weaponstats.stun")+": ");
			DecimalFormat df = new DecimalFormat("#.#");
			sb.append(df.format(bullet.getStun() * .016666666667));
			sb.append(" "+Game.getMessage("ultraworm.weaponstats.time_unit"));
		}
		return sb.toString();
	}

	@Override
	public void appendFullStats(StringBuilder dest) {
		dest.append("\n "+Game.getMessage("ultraworm.weaponstats.firerate_lowercase")+": ");
		dest.append(getFireRateDescription().toUpperCase());
		dest.append("\n "+Game.getMessage("ultraworm.weaponstats.damage_lowercase")+": ");
		dest.append(getDamageStats());
		dest.append(getArmourPiercingStats());
		dest.append(getStunStats());
		dest.append("\n "+Game.getMessage("ultraworm.weaponstats.ammo_lowercase")+": ");
		dest.append(getMagazine());
		dest.append("\n "+Game.getMessage("ultraworm.weaponstats.reload_time")+": ");
		dest.append(getReloadSpeed() / 60);
		dest.append(" "+Game.getMessage("ultraworm.weaponstats.time_units"));
	}

	@Override
	public void appendBasicStats(StringBuilder dest) {
	}

	@Override
	public void appendTitle(StringBuilder dest) {
	}

	/**
	 * @return true if this weapon makes the wielder immune to disruptors
	 */
	public boolean confersImmunityFromDisruptors() {
		return false;
	}
}
