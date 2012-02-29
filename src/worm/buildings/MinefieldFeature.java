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

import net.puppygames.applet.Game;
import net.puppygames.applet.effects.BlastEffect;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;
import worm.ClickAction;
import worm.Entity;
import worm.Mode;
import worm.Res;
import worm.SFX;
import worm.Worm;
import worm.entities.Building;
import worm.entities.Gidrah;
import worm.features.GidrahFeature;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.resources.ResourceArray;

/**
 * $Id: MinefieldFeature.java,v 1.32 2010/09/03 01:43:49 foo Exp $
 * A minefield.
 * @author $Author: foo $
 * @version $Revision: 1.32 $
 */
public class MinefieldFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	/** Mine damage */
	private int damage;

	/** Explosion radius */
	private float explosionRadius;

	/** Number of uses */
	private int uses;

	/** Requires arming */
	private boolean requiresArming;

	/** Appearances for different number of uses */
	private ResourceArray appearances;

	/** Armed appearance */
	private LayersFeature armedAppearance;

	/** min explsion emitter */
	private String mineExplosionEmitter;

	/*
	 * Transient data
	 */

	private transient EmitterFeature mineExplosionEmitterResource;



	/**
	 * Building instances
	 */
	private class MinefieldInstance extends Building {

		private static final long serialVersionUID = 1L;

		/** How many times mine has gone off */
		private int used;

		/** Last gidrah to set the mine off */
		private Gidrah lastHit;

		/** Ticking blast mine */
		private boolean ticking;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected MinefieldInstance(boolean ghost) {
			super(MinefieldFeature.this, ghost);
		}

		@Override
		public int getSalePrice() {
			return 0;
		}

		@Override
		public boolean isMineField() {
			return true;
		}

		@Override
		public boolean isAttackableByGidrahs() {
			return false;
		}

		@Override
		public boolean isWorthAttacking() {
			return false;
		}

		@Override
		protected boolean dontShowLabel() {
			return true;
		}

		@Override
		public int onClicked(int mode) {
			if (mode == Mode.MODE_SELL) {
				return super.onClicked(mode);
			}
			if (!ticking && requiresArming && mode != Mode.MODE_BUILD) {
				arm();
				return ClickAction.CONSUME;
			} else {
				return super.onClicked(mode);
			}
		}

		@Override
		public LayersFeature getMousePointer(boolean clicked) {
			if (Worm.getGameState().isSelling()) {
				return Res.getMousePointerCantBuild();
			}
			if (ticking || !requiresArming) {
				return super.getMousePointer(clicked);
			}

			return Res.getMousePointerBlastmine();
		}

		/**
		 * Arms mines that require arming
		 */
		private void arm() {
			ticking = true;
			updateAppearance();
		}

		/**
		 * @return true if this mine requires arming, and is armed
		 */
		public boolean isArmed() {
			return ticking;
		}

		@Override
		public void onCollisionWithGidrah(Gidrah gidrah) {
			// Requires arming? Ignore
			if (requiresArming) {
				return;
			}

			// Wraiths and flying gidrahs don't trigger mines
			GidrahFeature gidrahFeature = gidrah.getFeature();
			if (gidrahFeature.isWraith() || gidrahFeature.isFlying()) {
				return;
			}

			// Gidlets only trigger multi-use mines
			if (gidrahFeature.isGidlet() && uses <= 1) {
				return;
			}

			if (gidrah == lastHit) {
				// Don't repeatedly go off on one gidrah (multi-use mines)
				return;
			}

			lastHit = gidrah;
			explode();
		}

		/* (non-Javadoc)
		 * @see worm.entities.Building#doBuildingTick()
		 */
		@Override
		protected void doBuildingTick() {
			if (ticking) {
				int event = getSprite(2).getEvent();
				getSprite(2).setEvent(0);
				switch (event) {
					case 0:
						return;
					case 1:
						SFX.blastMinePip(getX(), getY());
						return;
					case 2:
						SFX.blastMineReady(getX(), getY());
						return;
					case 3:
						explode();
						return;
					default:
						assert false : "Blast mine event "+event;
				}
			}
		}

		private float getExplosionRadius() {
			return Worm.getGameState().isResearched(ResearchFeature.PLASTIC) ? explosionRadius * 2.0f : explosionRadius;
		}

		private int getDamage() {
			return Worm.getGameState().isResearched(ResearchFeature.ADVANCEDEXPLOSIVES) ? (int)(damage * 2.0f) : damage;
		}

		/**
		 * Explode the mine
		 */
		private void explode() {
			float expRad = getExplosionRadius();
			BlastEffect effect = new BlastEffect(getMapX(), getMapY(), 16, 16, expRad * 2.0f, expRad * 2.0f, Res.getExplosionTexture());
			effect.setFadeWhenExpanding(true);
			effect.setOffset(GameScreen.getSpriteOffset());
			effect.spawn(GameScreen.getInstance());

			// chaz hack! moved from res to xml

			if (getMineExplosionEmitter()!=null) {
				Emitter emitter = getMineExplosionEmitter().spawn(GameScreen.getInstance());
				emitter.setLocation(getMapX(), getMapY());
				emitter.setFloor(getFloor() + getMapY());
				emitter.setOffset(GameScreen.getSpriteOffset());
			}

			// chaz hack! and added shake

			GameScreen.shake(damage);

			// Damage nearby gidrahs
			ArrayList<Entity> entities = new ArrayList<Entity>(Worm.getGameState().getEntities());
			int n = entities.size();
			int dam = getDamage();
			for (int i = 0; i < n; i ++) {
				Entity e = entities.get(i);
				if (e.canCollide() && e.isActive()) {
					if (e.isTouching(getX(), getY(), expRad)) {
						e.explosionDamage(dam, true);
					}
				}
			}

			used ++;
			if (used >= uses) {
				remove();
			} else {
				// Update appearance
				updateAppearance();
			}
		}

		/*
		 * No repairs
		 */
		@Override
		public void repairFully() {
		}

		/*
		 * No repairs
		 */
		@Override
		public void repair() {
		}

		@Override
		public boolean canBuildOnTopOf(Entity target) {
			return false;
		}

		@Override
		public boolean canSell() {
			return false;
		}
	}

	/**
	 * @param name
	 */
	public MinefieldFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new MinefieldInstance(ghost);
	}

	@Override
	public LayersFeature getAppearance(Building building) {
		if (building == null || uses == 1) {
			if (building != null && ((MinefieldInstance) building).isArmed()) {
				return armedAppearance;
			} else {
				return super.getAppearance(null);
			}
		} else {
			return (LayersFeature) appearances.getResource(((MinefieldInstance) building).used);
		}
	}

	@Override
	public boolean canBuildOnTopOf(BuildingFeature target) {
		return false;
	}

	public EmitterFeature getMineExplosionEmitter() {
		return mineExplosionEmitterResource;
	}

	@Override
	public int getNumAvailable() {
		if (Worm.getGameState().isResearched(ResearchFeature.EXTRAMINES)) {
			return (int) (super.getNumAvailable() * 1.5f);
		} else {
			return super.getNumAvailable();
		}
	}


	@Override
	public void getResearchStats(StringBuilder stats_1_text, StringBuilder stats_2_text) {
		super.getResearchStats(stats_1_text, stats_2_text);

		stats_1_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.uses")+": {font:tinyfont.glfont color:text-bold}");
		stats_1_text.append(uses);

		stats_2_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.blast_radius")+": {font:tinyfont.glfont color:text-bold}");
		stats_2_text.append((int) explosionRadius);
		stats_2_text.append("m");

		stats_2_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.damage")+": {font:tinyfont.glfont color:text-bold}");
		stats_2_text.append(damage);
		stats_2_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.production")+": {font:tinyfont.glfont color:text-bold}");
		stats_2_text.append(getNumAvailable());
		stats_2_text.append("/"+Game.getMessage("ultraworm.researchstats.level"));
	}

	@Override
	protected String getBuildingType() {
		return Game.getMessage("ultraworm.researchstats.building_type_explosive");
	}

	@Override
	public void appendBasicStats(StringBuilder dest) {
		dest.append(Game.getMessage("ultraworm.researchstats.damage_lowercase")+": ");
		dest.append(damage);
		dest.append("\n "+Game.getMessage("ultraworm.researchstats.blast_radius_lowercase")+": ");
		dest.append((int) (explosionRadius * 6.25f));
		dest.append(" M \n "+Game.getMessage("ultraworm.researchstats.uses_lowercase")+": ");
		dest.append(uses);
		dest.append("\n "+Game.getMessage("ultraworm.researchstats.production_lowercase")+": ");
		dest.append(getNumAvailable());
		dest.append("/"+Game.getMessage("ultraworm.researchstats.level"));
	}
}
