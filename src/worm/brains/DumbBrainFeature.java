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
package worm.brains;

import java.util.ArrayList;

import worm.Entity;
import worm.Worm;
import worm.entities.Building;

import com.shavenpuppy.jglib.util.Util;

/**
 * The Dumb Brain just heads straight for any bases, then the nearest building if there are no more bases.
 * @author Cas
 */
public class DumbBrainFeature extends BrainFeature {

	private static final long serialVersionUID = 1L;

	/**
	 * C'tor
	 */
	public DumbBrainFeature() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public DumbBrainFeature(String name) {
		super(name);
	}

	@Override
	public Entity findTarget(Entity entity) {
		ArrayList<Building> buildings = Worm.getGameState().getBuildings();
		int n = buildings.size();
		if (n == 0) {
			// No buildings left!
			return null;
		}
		ArrayList<Building> bases = new ArrayList<Building>(4);
		Building closest = null;
		float closestDist = Float.MAX_VALUE;
		float mapY = entity.getY();
		float mapX = entity.getX();
		// Find a random base.
		for (int i = 0; i < n; i ++) {
			Building newTarget = buildings.get(i);
			if (newTarget.isActive() && !newTarget.isCloaked() && newTarget.isAttackableByGidrahs() && newTarget.isApparentlyValuable()) {
				bases.add(newTarget);
			}
		}

		if (bases.size() > 0) {
			return bases.get(Util.random(0, bases.size() - 1));
		}

		// Reset the search and look for nearest building
		closestDist = Float.MAX_VALUE;
		int idx = Util.random(0, n - 1);
		for (int i = idx; i < n; i ++) {
			Building newTarget = buildings.get(i);
			if (newTarget.isActive() && !newTarget.isCloaked() && newTarget.isAttackableByGidrahs() && newTarget.isWorthAttacking()) {
				float dist = newTarget.getDistanceTo(mapX, mapY);
				if (dist < closestDist) {
					closestDist = dist;
					closest = newTarget;
				}
			}
		}
		for (int i = 0; i < idx; i ++) {
			Building newTarget = buildings.get(i);
			if (newTarget.isActive() && !newTarget.isCloaked() && newTarget.isAttackableByGidrahs() && newTarget.isWorthAttacking()) {
				float dist = newTarget.getDistanceTo(mapX, mapY);
				if (dist < closestDist) {
					closestDist = dist;
					closest = newTarget;
				}
			}
		}

		return closest;
	}

}
