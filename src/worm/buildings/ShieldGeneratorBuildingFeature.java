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

import worm.Worm;
import worm.entities.Building;

/**
 * $Id: ShieldGeneratorBuildingFeature.java,v 1.24 2010/10/22 02:30:35 foo Exp $
 * Shield generator
 * @author $Author: foo $
 * @version $Revision: 1.24 $
 */
public class ShieldGeneratorBuildingFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	/**
	 * Building instances
	 */
	private class ShieldGeneratorBuildingInstance extends Building {

		private static final long serialVersionUID = 1L;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected ShieldGeneratorBuildingInstance(boolean ghost) {
			super(ShieldGeneratorBuildingFeature.this, ghost);
		}


		@Override
		protected void adjustProximity(Building target, int delta) {
			target.addShields(delta);
		}

		@Override
		protected void doOnBuild() {
			Worm.getGameState().addShieldGenerators(1);
		}

		@Override
		protected void doBuildingDestroy() {
			Worm.getGameState().addShieldGenerators(-1);
		}

	}

	/**
	 * @param name
	 */
	public ShieldGeneratorBuildingFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new ShieldGeneratorBuildingInstance(ghost);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		return
				feature instanceof BaseBuildingFeature
			||	feature instanceof BatteryBuildingFeature
			||	feature instanceof CapacitorBuildingFeature
			||	feature	instanceof CoolingTowerBuildingFeature
			||	feature instanceof FactoryBuildingFeature
			||	feature	instanceof ReactorBuildingFeature
			||	feature instanceof ScannerBuildingFeature
			||	feature instanceof TurretBuildingFeature
			||	feature instanceof WarehouseBuildingFeature
			||	feature instanceof BarracksBuildingFeature
			||	feature instanceof CollectorBuildingFeature
			||	feature instanceof AutoLoaderBuildingFeature
			|| 	feature instanceof CloakBuildingFeature
			;
	}

}
