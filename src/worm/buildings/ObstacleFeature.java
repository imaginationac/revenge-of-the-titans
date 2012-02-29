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

import worm.Entity;
import worm.EntitySpawningFeature;
import worm.MapRenderer;
import worm.entities.Building;

import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: ObstacleFeature.java,v 1.8 2010/08/24 23:18:45 foo Exp $
 * An obstacle.
 * @author $Author: foo $
 * @version $Revision: 1.8 $
 */
public class ObstacleFeature extends BuildingFeature implements EntitySpawningFeature {

	private static final long serialVersionUID = 1L;

	/**
	 * Obstacle instances
	 */
	private class ObstacleInstance extends Building {

		private static final long serialVersionUID = 1L;

		/**
		 * @param feature
		 * @param x
		 * @param y
		 */
		protected ObstacleInstance(boolean ghost) {
			super(ObstacleFeature.this, ghost);
		}

		/* (non-Javadoc)
		 * @see storm.entities.Building#dontShowLabel()
		 */
		@Override
		protected boolean dontShowLabel() {
			return true;
		}

		/* (non-Javadoc)
		 * @see storm.entities.Building#doRepair()
		 */
		@Override
		public void repair() {
			// Don't repair obstacles
		}

		@Override
		public void repairFully() {
			// Don't repair obstacles
		}

		@Override
		public boolean isBarricade() {
			return true;
		}

		@Override
		public boolean isLaserThrough() {
			return true;
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
		public boolean isWorthAttacking() {
			return false;
		}

		@Override
		public boolean shouldShowAttackWarning() {
			return false;
		}

		@Override
		public void onHovered(int mode) {
			// Ignore
		}

	}

	/**
	 * @param name
	 */
	public ObstacleFeature(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see storm.BuildingFeature#doSpawn()
	 */
	@Override
	public Building doSpawn(boolean ghost) {
		return new ObstacleInstance(ghost);
	}

	/* (non-Javadoc)
	 * @see worm.EntitySpawningFeature#spawn(net.puppygames.applet.Screen, int, int)
	 */
	@Override
	public Entity spawn(int x, int y) {
		return build(x * MapRenderer.TILE_SIZE + Util.random(0, MapRenderer.TILE_SIZE - 1), y * MapRenderer.TILE_SIZE + Util.random(0, MapRenderer.TILE_SIZE - 1));
	}

	@Override
	public boolean removeAfterSpawn() {
		// Let gidrahs walk into obstacles and batter them down
		return true;
	}
}
