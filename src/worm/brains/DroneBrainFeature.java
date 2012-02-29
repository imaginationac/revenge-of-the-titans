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
import worm.entities.Factory;

/**
 * The Drone brain tries to repair the most expensive damage it can by weighting the cost of a building versus the number of
 * hitpoints damage it has taken.
 */
public class DroneBrainFeature extends BrainFeature {

	private static final long serialVersionUID = 1L;

	private float distanceFactor;
	private float baseValue;
	private float valuableFactor;

	/**
	 * C'tor
	 */
	public DroneBrainFeature() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public DroneBrainFeature(String name) {
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
		Building best = null;
		float bestRating = 0.0f;
		for (int i = 0; i < n; i ++) {
			Building newTarget = buildings.get(i);
			if (newTarget.isActive() && newTarget.isAttackableByGidrahs() && newTarget.isWorthAttacking()) {
				float damage = newTarget.getMaxHitPoints() - newTarget.getHitPoints();
				if (damage == 0.0f) {
					continue;
				}
				float cost, factor = 1.0f;
				if (newTarget.isCity()) {
					cost = baseValue;
				} else {
					if (newTarget instanceof Factory) {
						if (((Factory) newTarget).isShutdown()) {
							continue;
						}
					} else if (newTarget.getFeature().getMaxAvailable() > 0) {
						// Rare building; more valuable
						factor = valuableFactor;
					}

					cost = newTarget.getCost();
				}

				float distance = entity.getDistanceTo(newTarget) / distanceFactor;
				float distanceModifier;
				if (distance == 0.0f) {
					distanceModifier = 1.0f;
				} else {
					distanceModifier = 1.0f / distance;
				}
				float rating = cost * (damage / newTarget.getMaxHitPoints()) * factor * distanceModifier;
				if (rating > bestRating) {
					bestRating = rating;
					best = newTarget;
				}
			}
		}

		return best;
	}
}
