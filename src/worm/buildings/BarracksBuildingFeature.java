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


import org.lwjgl.util.Point;

import worm.Barracks;
import worm.Worm;
import worm.WormGameState;
import worm.entities.Building;
import worm.entities.Unit;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.features.UnitFeature;
import worm.screens.GameScreen;

/**
 * $Id: BarracksBuildingFeature.java,v 1.7 2010/11/02 17:21:32 foo Exp $
 * Spawner
 * @author $Author: foo $
 * @version $Revision: 1.7 $
 */
public class BarracksBuildingFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	private int baseProductionRate;
	private int buffedProductionRate;
	private int maxReactors;
	private int productionRatePerReactor;
	private int unitsPerReactor;
	private int maxUnits;

	/** Unit to spawn */
	private String unit;

	/** Buffed unit */
	private String buffedUnit;

	/** Sergeant */
	private String sergeant;

	/** Buffed cost */
	private int buffedCost;

	/** Spawn appearance */
	private LayersFeature spawnAppearance;

	/** Spawn offset */
	private Point spawnOffset;

	/*
	 * Transient data
	 */

	private transient UnitFeature unitFeature, buffedUnitFeature, sergeantFeature;

	/**
	 * Building instances
	 */
	private class BarracksBuildingInstance extends Building implements Barracks {

		private static final long serialVersionUID = 1L;

		private final boolean buffed;
		private final int productionRate;

		private int tick;
		private int counter;
		private int numUnits;
		private int phase;

		private static final int PHASE_NORMAL = 0;
		private static final int PHASE_SPAWN = 1;
		private static final int PHASE_WAIT = 2;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected BarracksBuildingInstance(boolean ghost) {
			super(BarracksBuildingFeature.this, ghost);

			buffed = buffedCost > getCost() && Worm.getGameState().isResearched(ResearchFeature.DROIDBUFF);
			productionRate = buffed ? buffedProductionRate : baseProductionRate;
		}

		@Override
		public void onUnitRemoved(Unit unit) {
			numUnits --;
		}

		private int getMaxUnits() {
			return maxUnits + Math.min(maxReactors, getReactors()) * unitsPerReactor;
		}

		@Override
		protected void doBuildingTick() {
			WormGameState gameState = Worm.getGameState();
			// If units is over max or game over, wait
			if (!gameState.isPlaying() || numUnits >= getMaxUnits()) {
				return;
			}

			switch (phase) {
				case PHASE_NORMAL:
					tickNormal();
					break;
				case PHASE_SPAWN:
					tickSpawn();
					break;
				case PHASE_WAIT:
					tickWait();
					break;
				default:
					assert false : "Unknown phase "+phase;
			}
		}

		private void tickNormal() {
			tick ++;
			if (tick >= getProductionRate()) {
				cancelUndo();

				if (spawnAppearance != null) {
					// Start spawn animation
					phase = PHASE_SPAWN;
					updateAppearance();
				} else {
					tick = 0;
					spawnUnit();
				}

			}
		}

		private void tickSpawn() {
			// Wait for event 1
			if (getEvent() == 1) {
				// Spawn!
				spawnUnit();
				phase = PHASE_WAIT;
			}
		}

		private void tickWait() {
			// Wait for event 2
			if (getEvent() == 2) {
				// Back to normal
				tick = 0;
				phase = PHASE_NORMAL;
				updateAppearance();
			}
		}

		private void spawnUnit() {
			UnitFeature uf;
			if (buffed) {
				counter ++;
				if (counter >= getMaxUnits()) {
					counter = 0;
					uf = sergeantFeature;
				} else {
					uf = buffedUnitFeature;
				}
			} else {
				uf = unitFeature;
			}
			numUnits ++;
			uf.spawn(this, GameScreen.getInstance(), getX() + spawnOffset.getX(), getY() + spawnOffset.getY());
		}

		boolean isSpawning() {
			return phase == PHASE_SPAWN;
		}

		private int getProductionRate() {
			return productionRate - Math.min(maxReactors, getReactors()) * productionRatePerReactor;
		}

		@Override
		protected void doOnBuild() {
			Worm.getGameState().addSpawners(1);
		}

		@Override
		protected void doBuildingDestroy() {
			Worm.getGameState().addSpawners(-1);
		}

		@Override
		protected void adjustProximity(Building target, int delta) {
			target.addSpawners(delta);
		}
	}

	/**
	 * @param name
	 */
	public BarracksBuildingFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new BarracksBuildingInstance(ghost);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		return feature instanceof ReactorBuildingFeature || feature instanceof ShieldGeneratorBuildingFeature || feature instanceof CloakBuildingFeature;
	}

	@Override
	public int getNumAvailable() {
		return 1;
	}

	@Override
	public int getShopValue() {
		if (buffedCost > 0 && Worm.getGameState().isResearched(ResearchFeature.DROIDBUFF)) {
			return buffedCost;
		} else {
			return super.getShopValue();
		}
	}

	@Override
	public LayersFeature getAppearance(Building building) {
		BarracksBuildingInstance bbi = (BarracksBuildingInstance) building;
		if (bbi.isSpawning() && spawnAppearance != null) {
			return spawnAppearance;
		} else {
		    return super.getAppearance(building);
		}
	}
}
