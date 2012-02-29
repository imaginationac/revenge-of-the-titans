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

import net.puppygames.applet.Game;

import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.GameMap;
import worm.MapRenderer;
import worm.Worm;
import worm.entities.Building;
import worm.entities.Gidrah;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;

import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: BarricadeFeature.java,v 1.30 2010/09/05 21:25:25 foo Exp $
 * A minefield.
 * @author $Author: foo $
 * @version $Revision: 1.30 $
 */
public class BarricadeFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	/** Hitpoint divisor */
	private static final int HITPOINT_DIVISOR = 12;
	private static final int HITPOINT_MIDPOINT = HITPOINT_DIVISOR / 2;

	/** Appearances for different number of hitpoints */
	private ResourceArray appearances;

	/** Terrain difficulty for gidrahs - on same scale as weapon danger */
	private int difficulty;

	/** Gidlet proof? */
	private boolean gidletProof;

	/** Terrain speed modifier */
	private boolean slowdown;

	/**
	 * Building instances
	 */
	private class BarricadeInstance extends Building {

		private static final long serialVersionUID = 1L;

		/** Appearance index for tangleweb */
		private int idx;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected BarricadeInstance(boolean ghost) {
			super(BarricadeFeature.this, ghost);
		}

		@Override
		public int getSalePrice() {
			return 0;
		}

		@Override
		public boolean canSell() {
			return false;
		}

		@Override
		protected boolean dontShowLabel() {
			return true;
		}

		@Override
		public void repair() {
			// Don't repair barricades
		}

		@Override
		public boolean isBarricade() {
			return true;
		}

		@Override
		public boolean isSlowDown() {
			return slowdown;
		}

		@Override
		public boolean isGidletProof() {
			return gidletProof;
		}

		@Override
		public boolean isWorthAttacking() {
			return false;
		}

		@Override
		public boolean shouldShowAttackWarning() {
			return false;
		}

		@Override
		protected void onDamaged() {
			updateAppearance();
		}

		@Override
		protected void onRepaired() {
			updateAppearance();
		}

		@Override
		protected void doOnBuild() {
			if (difficulty > 0) {
				addDifficulty(difficulty);
			}
		}


		@Override
		protected void doBuildingSpawn() {
			if (slowdown) {
				// Pick a random appearance for tangleweb
				idx = Util.random(0, appearances.getNumResources() - 1);
				updateAppearance();
			}
		}

		@Override
		protected void doBuildingRemove() {
			if (difficulty > 0) {
				addDifficulty(-difficulty);
			}
		}

		private void addDifficulty(int amount) {
			// Increase cost level to traverse the area
			float scanRadius = MapRenderer.TILE_SIZE * 0.5f;
			GameMap map = Worm.getGameState().getMap();
			int bottomLeftY = (int) (getY() - scanRadius) / MapRenderer.TILE_SIZE;
			int height = (int) (getY() + scanRadius)  / MapRenderer.TILE_SIZE;
			int bottomLeftX = (int) (getX() - scanRadius) / MapRenderer.TILE_SIZE;
			int width = (int) (getX() + scanRadius)  / MapRenderer.TILE_SIZE;
			for (int yy = bottomLeftY; yy <= height; yy ++) {
				for (int xx = bottomLeftX; xx <= width; xx ++) {
					map.setDifficulty(xx, yy, map.getDifficulty(xx, yy) + amount);
				}
			}

			// All gidrahs rethink your routes!
			Gidrah.rethinkRoutes(new Rectangle(bottomLeftX, bottomLeftY, width, height));
		}

		@Override
		public boolean canBuildOnTopOf(Entity target) {
			if (target instanceof Building) {
				return getFeature().canBuildOnTopOf(((Building) target).getFeature());
			} else {
				return false;
			}
		}
	}

	/**
	 * @param name
	 */
	public BarricadeFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new BarricadeInstance(ghost);
	}

	@Override
	public LayersFeature getAppearance(Building building) {

		int i = -1;
		if (building != null) {
			BarricadeInstance barricade = (BarricadeInstance) building;
			if (slowdown) {
				i = barricade.idx;
			} else {
				i = (barricade.getHitPoints() + HITPOINT_MIDPOINT) / HITPOINT_DIVISOR - 1;
			}
		}

		if (building == null || i < 0) {
			return super.getAppearance(null);
		} else {
			return (LayersFeature) appearances.getResource(i);
		}
	}

	@Override
	public boolean canBuildOnTopOf(BuildingFeature target) {
		if (target instanceof BarricadeFeature) {
			if (target.getHitPoints() < this.getHitPoints()) {
				return true;
			} else {
				return false;
			}
		} else {
			return target instanceof ObstacleFeature;
		}
	}

	@Override
	public int getNumAvailable() {
		if (Worm.getGameState().isResearched(ResearchFeature.EXTRABARRICADES)) {
			return (int) (super.getNumAvailable() * 1.5f);
		} else {
			return super.getNumAvailable();
		}
	}

	@Override
	public void getResearchStats(StringBuilder stats_1_text, StringBuilder stats_2_text) {
		stats_1_text.append("{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.cost")+": {font:tinyfont.glfont color:text-bold}$");
		stats_1_text.append(getInitialValue());
		if (getHitPoints() > 0) {
			stats_1_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.strength")+": {font:tinyfont.glfont color:text-bold}");
			stats_1_text.append(getHitPoints() / 2);
		}
		stats_2_text.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.researchstats.production")+": {font:tinyfont.glfont color:text-bold}");
		stats_2_text.append(getNumAvailable());
		stats_2_text.append(" "+Game.getMessage("ultraworm.researchstats.per_level"));
	}

	@Override
	protected String getBuildingType() {
		return "barricade";
	}

}
