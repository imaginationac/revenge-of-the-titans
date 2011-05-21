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

import net.puppygames.applet.Game;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.Res;
import worm.effects.ElectronZapEffect;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.resources.MappedColor;



/**
 * The capacitor weapon
 * @author Cas
 */
public class ElectronZapFeature extends WeaponFeature {

	private static final Rectangle TEMP = new Rectangle();

	/** Fattest bit of the zap beam */
	private static final float MAX_WIDTH = 1f;
	private static final float MAX_WOBBLE = 8.0f;
	private static final float WOBBLE_FACTOR = 0.5f;
	private static final float WIDTH_FACTOR = 0.025f;

	/** Beam emitter */
	private EmitterFeature beamStartEmitter, beamEndEmitter;

	/**
	 * Instance of the electron zap weapon
	 */
	private class ElectronZapInstance extends WeaponInstance {

		private static final long serialVersionUID = 1L;

		private transient ElectronZapEffect beam;
		private int tick;

		/**
		 * C'tor
		 * @param entity
		 */
		public ElectronZapInstance(Entity entity) {
			super(entity);
		}

		@Override
		protected void doFire(float targetX, float targetY) {
			// Spawn a beam if we don't yet have one
			if (beam == null) {
				beam = new ElectronZapEffect
					(
						true,
						Res.getCapacitorBuffer(),
						new MappedColor("capacitorzap.background"),
						new MappedColor("capacitorzap.foreground"),
						255,
						beamStartEmitter,
						beamEndEmitter,
						entity.getMapX() + entity.getOffsetX(),
						entity.getMapY() + entity.getOffsetY(),
						MAX_WIDTH,
						MAX_WOBBLE,
						WOBBLE_FACTOR,
						WIDTH_FACTOR
					);
				beam.setOffset(GameScreen.getSpriteOffset());
				beam.spawn(GameScreen.getInstance());
				Game.allocateSound(Res.getCapacitorStartBuffer());
			}

			// Target the beam
			beam.setTarget(targetX, targetY);
			tick = 1;
		}

		@Override
		public void tick() {
			super.tick();

			// If we didn't doFire this tick, then remove any beam
			if (tick > 0) {
				tick --;
			} else if (beam != null) {
				beam.finish();
				beam = null;
			}
		}

		@Override
		public void remove() {
			if (beam != null) {
				beam.finish();
				beam = null;
			}
		}
	}

	/**
	 * C'tor
	 * @param name
	 */
	public ElectronZapFeature(String name) {
		super(name);
	}

	@Override
	public WeaponInstance spawn(Entity entity) {
		return new ElectronZapInstance(entity);
	}


}
