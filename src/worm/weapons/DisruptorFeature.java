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

import java.util.ArrayList;

import net.puppygames.applet.effects.BlastEffect;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;
import net.puppygames.applet.effects.StrobeEffect;

import org.lwjgl.util.Color;

import worm.Entity;
import worm.Worm;
import worm.WormGameState;
import worm.entities.Building;
import worm.entities.Turret;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.opengl.GLTexture;



/**
 * The disruptor weapon
 * @author Cas
 */
public class DisruptorFeature extends WeaponFeature {

	private static final int DEFAULT_DURATION = 20;

	/** Radius of the blast */
	private float radius, minRadius, maxRadius;

	/** Radius per reactor */
	private float radiusPerReactor;

	/** Damage of the blast */
	private int damage;

	/** Blast texture */
	private String texture;

	/** Strobe colour */
	private Color strobe;

	/** Emitter */
	private String emitter;

	/** duration */
	private int duration;

	private transient GLTexture textureResource;
	private transient EmitterFeature emitterFeature;

	/**
	 * Disruptor instances
	 */
	private class DisruptorWeaponInstance extends WeaponInstance {
		private static final long serialVersionUID = 1L;

		/**
		 * C'tor
		 * @param entity
		 */
		public DisruptorWeaponInstance(Entity entity) {
			super(entity);
		}

		@Override
		public boolean isAutomatic() {
			// Only triggered by <shoot> command
			return false;
		}

		@Override
		public float getBlastRange() {
			boolean turret = getEntity() instanceof Turret;
			if (turret) {
				return radius + Math.min(maxReactors, ((Building) getEntity()).getReactors()) * radiusPerReactor;
			} else {
				return CosineInterpolator.instance.interpolate(minRadius, maxRadius, Worm.getGameState().getDifficulty());
			}
		}

		@Override
		protected void doFire(float targetX, float targetY) {
			// Override completely to simply fire a disruptor blast
			WormGameState gameState = Worm.getGameState();

			float sourceX, sourceY;
			if (entity == null) {
				sourceX = targetX;
				sourceY = targetY;
			} else {
				sourceX = entity.getMapX() + entity.getOffsetX();
				sourceY = entity.getMapY() + entity.getOffsetY();
			}

			if (duration == 0) {
				duration = DEFAULT_DURATION;
			}

			float totalRadius = getBlastRange();
			BlastEffect blast = new BlastEffect(sourceX, sourceY, duration, duration, totalRadius, totalRadius, textureResource);
			blast.setOffset(GameScreen.getSpriteOffset());
			blast.setFadeWhenExpanding(true);
			blast.spawn(GameScreen.getInstance());
			if (strobe != null) {
				StrobeEffect se = new StrobeEffect(strobe, 10);
				se.spawn(GameScreen.getInstance());
			}

			GameScreen.shake(8);

			// Emit emitter
			if (emitterFeature != null) {
				Emitter e = emitterFeature.spawn(GameScreen.getInstance());
				e.setLocation(sourceX, sourceY);
			}

			// Damage nearby gidrahs or buildings depending on who we are.
			boolean turret = getEntity() instanceof Turret;
			ArrayList<Entity> entities = new ArrayList<Entity>(gameState.getEntities());
			int n = entities.size();
			for (int i = 0; i < n; ) {
				Entity e = entities.get(i);
				if (e != entity && e.canCollide() && e.isActive()) {
					double dist = e.getDistanceTo(sourceX, sourceY);
					if (dist <= totalRadius) {
						e.disruptorDamage(damage, turret);
						if (e.isDisruptorProof()) {
							getEntity().onBulletDeflected(e);
						}
						entities.remove(i);
						n --;
					} else {
						i ++;
					}
				} else {
					i ++;
				}
			}
		}
	}

	/**
	 * C'tor
	 * @param name
	 */
	public DisruptorFeature(String name) {
		super(name);
	}

	@Override
	public WeaponInstance spawn(Entity entity) {
		return new DisruptorWeaponInstance(entity);
	}

	public int getDamage() {
		return damage;
	}

	public int getArmourPiercing() {
		return 0;
	}

	@Override
	public boolean isDisruptor() {
		return true;
	}

	@Override
	protected String getDamageStats() {
		return String.valueOf(damage);
	}

	@Override
	public boolean confersImmunityFromDisruptors() {
		return true;
	}

}
