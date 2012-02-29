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

import net.puppygames.applet.effects.EmitterFeature;
import worm.CrystalResource;
import worm.Hints;
import worm.Res;
import worm.SFX;
import worm.Worm;
import worm.WormGameState;
import worm.effects.ElectronZapEffect;
import worm.entities.Building;
import worm.entities.Factory;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.resources.MappedColor;

/**
 * $Id: FactoryBuildingFeature.java,v 1.83 2010/10/16 02:17:16 foo Exp $
 * Factories
 * @author $Author: foo $
 * @version $Revision: 1.83 $
 */
public class FactoryBuildingFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	private static FactoryBuildingFeature instance;

	private int baseProductionRate;
	private int minProductionRate;
	private int productionRatePerCollector;
	private int productionRatePerWarehouse;

	private int amountToExtract;
	private int amountToExtractPerWarehouse;

	private int maxCollectors;
	private int maxWarehouses;

	private static final int WAIT_TIME = 8;

	private static final float MAX_WIDTH = 0.5f;
	private static final float MAX_WOBBLE = 16.0f;
	private static final float WOBBLE_FACTOR = 0.5f;
	private static final float WIDTH_FACTOR = 0.025f;

	private static final int BEAM_X_OFFSET = 5;
	private static final int BEAM_Y_OFFSET = 2;

	/** Shutdown appearance, for end of level */
	private LayersFeature shutdownAppearance;

	private EmitterFeature beamStartEmitter, beamEndEmitter;

	/**
	 * Building instances
	 */
	private class FactoryBuildingInstance extends Building implements Factory {

		private static final long serialVersionUID = 1L;

		private int waitTick, tick;
		private boolean shutdown, mining;
		private int extraCollector, extraWarehouse;
		private CrystalResource crystal;
		private transient ElectronZapEffect zapEffect;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected FactoryBuildingInstance(boolean ghost) {
			super(FactoryBuildingFeature.this, ghost);
		}

		@Override
		protected void doBuildingSpawn() {
			createSpecialEffects();
			extraCollector = Worm.getGameState().isResearched(ResearchFeature.EXTRACTION) ? productionRatePerCollector / 2 : 0;
			extraWarehouse = Worm.getGameState().isResearched(ResearchFeature.FINETUNING) ? amountToExtractPerWarehouse / 2 : 0;
		}

		private void createSpecialEffects() {
		}

		@Override
		protected void doRemoveSpecialEffects() {
			removeCrystalEffects();
		}

		@Override
		public void onEndLevel() {
			doRemoveSpecialEffects();
			shutdown = true;
			updateAppearance();
		}

		@Override
		protected void doBuildingTick() {
			WormGameState gameState = Worm.getGameState();
			if (!gameState.isPlaying() || shutdown) {
				return;
			}

			if (waitTick > 0) {
				waitTick --;
				return;
			}

			if (crystal == null || !crystal.hasRemaining() || !mining) {
				// If we've got no crystal, or the crystal we had is finished, find another one
				findCrystal();
				if (crystal == null) {
					SFX.factoryShutdown();
					doRemoveSpecialEffects();
					shutdown = true;
					updateAppearance();
					return;
				} else if (!mining) {
					// Wait
					waitTick = WAIT_TIME;
					return;
				}
			}

			if (++tick >= getProductionRate()) {
				tick = 0;
				gameState.addMoney(getAmount());
				crystal.consume(amountToExtract);
			}
		}

		private int getAmount() {
			return Math.min(maxWarehouses, getWarehouses()) * amountToExtractPerWarehouse + amountToExtract + extraWarehouse;
		}

		private int getProductionRate() {
			int ret =
				Math.max
					(
						minProductionRate,
						baseProductionRate - extraCollector - Math.min(maxCollectors, getCollectors()) * productionRatePerCollector
							+ Math.min(maxWarehouses, getWarehouses()) * productionRatePerWarehouse
					);
			return ret;
		}

		@Override
		public void addCrystals(int n) {
			super.addCrystals(n);

			// Maybe wake the factory up
			if (shutdown && isActive()) {
				shutdown = false;
				createSpecialEffects();
				updateAppearance();
			}
		}

		@Override
		protected void doOnBuild() {
			Worm.getGameState().addFactories(1);

			findCrystal();

			Worm.getGameState().flagHint(Hints.TIMERSTARTS);
		}

		protected void onDestroy() {
			Worm.getGameState().addFactories(-1);
		}

		@Override
		protected void adjustProximity(Building target, int delta) {
			target.addFactories(delta);
		}

		@Override
		public boolean canBuild() {
			calcGhostProximity();
			return getCrystals() > 0;
		}

		private void removeCrystalEffects() {
			if (mining && crystal != null) {
				crystal.addBeams(-1);
			}
			mining = false;
			crystal = null;
			if (zapEffect != null) {
				zapEffect.remove();
				zapEffect = null;
			}
		}

		private void findCrystal() {
			removeCrystalEffects();

			// Get nearby crystals
			ArrayList<Building> buildings = new ArrayList<Building>(getNearbyAffectedBuildings());
			Collections.shuffle(buildings);
			int n = buildings.size();
			for (int i = 0; i < n; i ++) {
				Building target = buildings.get(i);
				if (target instanceof CrystalResource) {
					CrystalResource cr = (CrystalResource) target;
					if (cr.hasRemaining()) {
						waitTick = 0;
						crystal = cr;
						if (cr.getBeams() < 4) {
							cr.addBeams(1);
							mining = true;
							zapEffect = new ElectronZapEffect
								(
									false,
									Res.getFactoryMiningBuffer(),
									new MappedColor("factoryzap.background"),
									new MappedColor("factoryzap.foreground"),
									128,
									beamStartEmitter,
									beamEndEmitter,
									getX() + BEAM_X_OFFSET,
									getY() + BEAM_Y_OFFSET,
									MAX_WIDTH,
									MAX_WOBBLE,
									WOBBLE_FACTOR,
									WIDTH_FACTOR
								);
							zapEffect.setTarget(crystal.getMapX() + crystal.getCollisionX(), crystal.getMapY() + crystal.getCollisionY());
							zapEffect.spawn(GameScreen.getInstance());
						}
						cancelUndo();
						return;
					}
				}
			}
		}

		@Override
		protected void doRespawn() {
			super.doRespawn();

			findCrystal();
		}

		@Override
		public boolean isShutdown() {
			return shutdown;
		}

		@Override
		public boolean isMining() {
			return !shutdown && crystal != null;
		}

		@Override
		protected boolean isAffectedBy(Building building) {
			if (building instanceof CrystalResource) {
				CrystalResource crystalResource = (CrystalResource) building;
				if (!crystalResource.hasRemaining()) {
					return false;
				}
			}

			return super.isAffectedBy(building);
		}

	}

	public FactoryBuildingFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new FactoryBuildingInstance(ghost);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		return
				feature instanceof WarehouseBuildingFeature
			|| 	feature instanceof CollectorBuildingFeature
			|| 	feature instanceof ShieldGeneratorBuildingFeature
			|| 	feature instanceof CloakBuildingFeature
			||	feature instanceof CrystalFeature;
	}

	@Override
	public boolean isFactory() {
		return true;
	}

	@Override
	public LayersFeature getAppearance(Building building) {
		FactoryBuildingInstance factory = (FactoryBuildingInstance) building;
		if (factory.shutdown) {
			return shutdownAppearance;
		} else {
			return super.getAppearance(factory);
		}
	}

	@Override
	public int getShopValue() {
		// If player has less than the cost of a factory and has no factories left, always allow them to build something
		if (Worm.getGameState().getFactories() == 0) {
			return Math.min(super.getShopValue(), Worm.getGameState().getMoney());
		} else {
			return super.getShopValue();
		}
	}

	@Override
	protected void doRegister() {
		super.doRegister();
		instance = this;
	}

	public static FactoryBuildingFeature getInstance() {
		return instance;
	}

	public int getRate() {
		return baseProductionRate;
	}

}
