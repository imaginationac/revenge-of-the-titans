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
import worm.entities.Gidrah;
import worm.features.GidrahFeature;

/**
 * Unit brains
 */
public class UnitBrainFeature extends BrainFeature {

	private static final long serialVersionUID = 1L;

	private float gidletWeight;
	private float gidrahWeight;
	private float angryGidrahWeight;
	private float bossWeight;

	/**
	 * C'tor
	 */
	public UnitBrainFeature() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public UnitBrainFeature(String name) {
		super(name);
	}

	@Override
	public Entity findTarget(Entity entity) {
		ArrayList<Gidrah> gidrahs = Worm.getGameState().getGidrahs();
		int n = gidrahs.size();
		if (n == 0) {
			// No gidrahs left!
			return null;
		}
		Gidrah closest = null;
		float closestDist = Float.MAX_VALUE;
		float mapY = entity.getY();
		float mapX = entity.getX();

		// Find nearest gidlet...
		for (int i = 0; i < n; i ++) {
			Gidrah newTarget = gidrahs.get(i);
			if (newTarget.isActive() && newTarget.isAttackableByUnits()) {
				float dist = newTarget.getDistanceTo(mapX, mapY);
				GidrahFeature feature = newTarget.getFeature();
				if (feature.isGidlet()) {
					dist *= gidletWeight;
				} else if (feature.isAngry()) {
					dist *= angryGidrahWeight;
				} else if (feature.isBoss()) {
					dist *= bossWeight;
				} else {
					dist *= gidrahWeight;
				}
				if (dist < closestDist) {
					if (entity.getIgnore() != null && entity.getIgnore().contains(newTarget)) {
						// Ignore this entity
						continue;
					}
					closestDist = dist;
					closest = newTarget;
				}
			}
		}

		if (closest != null) {
			// We found a gidlet
			return closest;
		}

		return null;
	}

}
