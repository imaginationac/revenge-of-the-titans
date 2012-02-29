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

import worm.Hints;
import worm.entities.Building;


/**
 * $Id: CollectorBuildingFeature.java,v 1.9 2010/09/29 01:21:52 foo Exp $
 * Auto loader
 * @author $Author: foo $
 * @version $Revision: 1.9 $
 */
public class CollectorBuildingFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	/**
	 * Building instances
	 */
	private class CollectorBuildingInstance extends AddonBuilding {

		private static final long serialVersionUID = 1L;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected CollectorBuildingInstance(boolean ghost) {
			super(CollectorBuildingFeature.this, ghost);
		}

		@Override
		protected boolean isUseful() {
			return getFactories() > 0;
		}

		@Override
		protected String getSellHint() {
			return Hints.SELL_MISC;
		}

		@Override
		protected void adjustProximity(Building target, int delta) {
			target.addCollectors(delta);
		}

	}

	/**
	 * @param name
	 */
	public CollectorBuildingFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new CollectorBuildingInstance(ghost);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		return feature instanceof FactoryBuildingFeature || feature instanceof ShieldGeneratorBuildingFeature || feature instanceof CloakBuildingFeature;
	}

}
