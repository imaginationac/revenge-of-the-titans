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
package worm.weapons;

import worm.Entity;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.util.Util;


/**
 * Shotgun weapon, used by some gidrahs
 * @author Cas
 */
public class ShotgunFeature extends WeaponFeature {

	private int numPellets;
	private double spread;

	/**
	 * Shotgun instance
	 */
	private class ShotgunInstance extends WeaponInstance {

		private static final long serialVersionUID = 1L;

		/**
		 * C'tor
		 * @param entity
		 */
		public ShotgunInstance(Entity entity) {
			super(entity);
		}

		/* (non-Javadoc)
		 * @see tomb.weapons.WeaponFeature.WeaponInstance#doFire(int, int)
		 */
		@Override
		protected void doFire(float targetX, float targetY) {
			// Spawn several bullets
			double x = entity.getMapX() + entity.getOffsetX();
			double y = entity.getMapY() + entity.getOffsetY();
			double angle = Math.atan2(targetY - y, targetX - x);

			for (int i = 0; i < numPellets; i ++) {
				double a2 = angle + Util.random() * spread - spread / 2.0;
				int tx = (int)(x + Math.cos(a2) * 120.0);
				int ty = (int)(y + Math.sin(a2) * 120.0);
				bullet.spawn(GameScreen.getInstance(), entity, (int) x, (int) y, tx, ty, 0);
			}
		}

	}

	/**
	 * C'tor
	 * @param name
	 */
	public ShotgunFeature(String name) {
		super(name);
	}

	@Override
	public WeaponInstance spawn(Entity entity) {
		return new ShotgunInstance(entity);
	}

	@Override
	protected String getDamageStats() {
		StringBuilder sb = new StringBuilder();
		sb.append(bullet.getDamage());
		sb.append("x");
		sb.append(numPellets);
		return sb.toString();
	}
}
