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

import org.lwjgl.util.Rectangle;

import worm.GameMap;
import worm.MapRenderer;
import worm.Worm;
import worm.entities.Building;
import worm.entities.Gidrah;

/**
 * Scarecrow
 */
public class ScarecrowBuildingFeature extends BuildingFeature {

	private static final long serialVersionUID = 1L;

	private float radius;
	private int danger;

	/**
	 * Building instances
	 */
	private class ScarecrowBuildingInstance extends Building {

		private static final long serialVersionUID = 1L;

		protected ScarecrowBuildingInstance(boolean ghost) {
			super(ScarecrowBuildingFeature.this, ghost);
		}

		private void addDanger(int amount) {
			// Increase danger level nearby
			float scanRadius = radius + MapRenderer.TILE_SIZE;
			GameMap map = Worm.getGameState().getMap();
			int bottomLeftY = (int) (getY() - scanRadius - 1.0f) / MapRenderer.TILE_SIZE;
			int height = (int) (getY() + scanRadius + 1.0f)  / MapRenderer.TILE_SIZE;
			int bottomLeftX = (int) (getX() - scanRadius - 1.0f) / MapRenderer.TILE_SIZE;
			int width = (int) (getX()  + scanRadius + 1.0f)  / MapRenderer.TILE_SIZE;
			for (int yy = bottomLeftY; yy <= height; yy ++) {
				float dy = getY() - yy * MapRenderer.TILE_SIZE - MapRenderer.TILE_SIZE * 0.5f;
				for (int xx = bottomLeftX; xx <= width; xx ++) {
					float dx = getX() - xx * MapRenderer.TILE_SIZE - MapRenderer.TILE_SIZE * 0.5f;
					double dist = Math.sqrt(dx * dx + dy * dy);
					if (dist <= scanRadius + 1.0f) {
						map.setDanger(xx, yy, map.getDanger(xx, yy) + amount);
					}
				}
			}

			// All gidrahs rethink your routes!
			Gidrah.rethinkRoutes(new Rectangle(bottomLeftX, bottomLeftY, width + 1, height + 1));
		}
		@Override
		protected void doOnBuild() {
		    addDanger(danger);
		}

		@Override
		protected void doBuildingDestroy() {
		    addDanger(-danger);
		}
	}

	/**
	 * @param name
	 */
	public ScarecrowBuildingFeature(String name) {
		super(name);
	}

	@Override
	public Building doSpawn(boolean ghost) {
		return new ScarecrowBuildingInstance(ghost);
	}

	@Override
	public boolean isAffectedBy(BuildingFeature feature) {
		return
				feature instanceof ShieldGeneratorBuildingFeature
			||	feature instanceof CloakBuildingFeature
			;
	}

}
