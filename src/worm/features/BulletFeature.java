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
package worm.features;

import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.BlastEffect;
import net.puppygames.applet.effects.EmitterFeature;
import worm.Entity;
import worm.Worm;
import worm.entities.Bullet;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.opengl.GLTexture;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.Range;


/**
 * Describes a bullet
 * @author Cas
 */
public class BulletFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Feature data
	 */

	/** Appearance */
	private LayersFeature appearance;

	/** Movement speed in pixels / frame. 1 = slow, 16 = ludicrously fast */
	private Range speed;

	/** Base speed, for attenuating damage with distance */
	private float baseSpeed;

	/** Blaster tech */
	private boolean blaster;

	/** Deceleration rate */
	private float deceleration;

	/** Ricochet emitter */
	private String ricochetEmitter;

	/** Flash emitter */
	private String flashEmitter;

	/** Damage */
	private int damage;

	/** Armour piercing */
	private int ap;

	/** Explosion delay */
	private int explosionDelay;

	/** Exploding */
	private boolean exploding;

	/** Explosion radius */
	private int explosionRadius;

	/** Explosion duration */
	private int explosionDuration;

	/** Explosion fade duration */
	private int explosionFadeDuration;

	/** Explosion texture */
	private String explosionTexture;

	/** Stun duration */
	private int stun;

	/** Passthrough */
	private boolean passThrough;

	/** Targeted - stops when it reaches its target */
	private boolean targeted;

	/** Mini - dangerous to gidlets */
	private boolean mini;

	/** Max range, ticks */
	private int range;

	private transient EmitterFeature ricochetEmitterResource;
	private transient EmitterFeature flashEmitterResource;
	private transient GLTexture explosionTextureResource;

	/**
	 * C'tor
	 */
	public BulletFeature() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public BulletFeature(String name) {
		super(name);
	}

	public boolean isMini() {
	    return mini;
    }

	/**
	 * @return Returns the speed.
	 */
	public float getSpeed() {
		return speed.getValue();
	}

	/**
	 * @return the damage
	 */
	public int getDamage() {
		return damage;
	}

	/**
	 * @return true if this is a blaster round
	 */
	public boolean isBlaster() {
		return blaster;
	}

	/**
	 * @return the armour piercing value
	 */
	public int getArmourPiercing() {
		return ap;
	}

	/**
	 * @return true if this bullet passes through aliens
	 */
	public boolean isPassThrough() {
		return passThrough;
	}

	/**
	 * @return the ricochetEmitterResource
	 */
	public EmitterFeature getRicochetEmitter() {
		return ricochetEmitterResource;
	}

	/**
	 * @return the flashEmitterResource
	 */
	public EmitterFeature getFlashEmitter() {
		return flashEmitterResource;
	}

	/**
	 * Spawn!
	 * @param screen
	 * @return
	 */
	public Bullet spawn(Screen screen, Entity source, int sx, int sy, int tx, int ty, int extraDamage) {
		Bullet b = new Bullet(source, sx, sy, tx, ty, this, extraDamage);
		b.spawn(screen);
		b.setRemainingRange(range);
		return b;
	}

	/**
	 * @return Returns the explosionDuration.
	 */
	public int getExplosionDuration() {
		return explosionDuration;
	}

	/**
	 * @return Returns the exploding.
	 */
	public boolean isExploding() {
		return exploding;
	}

	public BlastEffect createBlastEffect(boolean dangerousToGidrahs, int x, int y) {
		float radius = dangerousToGidrahs && exploding && Worm.getGameState().isResearched(ResearchFeature.PLASTIC) ? explosionRadius * 2.0f : explosionRadius;
		BlastEffect ret = new BlastEffect(x, y, explosionDuration, explosionFadeDuration, radius, radius, explosionTextureResource);
		ret.setOffset(GameScreen.getSpriteOffset());
		ret.spawn(GameScreen.getInstance());
		GameScreen.shake(10);
		// Sound effect is done by ricochet emitter
		return ret;
	}

	/**
	 * @return Returns the explosionDelay.
	 */
	public int getExplosionDelay() {
		return explosionDelay;
	}

	/**
	 * @return Returns the deceleration.
	 */
	public float getDeceleration() {
		return deceleration;
	}

	/**
	 * @return the appearance
	 */
	public LayersFeature getAppearance() {
		return appearance;
	}

	/**
	 * @return stun duration
	 */
	public int getStun() {
		return stun;
	}

	/**
	 * @return the targeted
	 */
	public boolean isTargeted() {
		return targeted;
	}

	public float getBaseSpeed() {
		return baseSpeed;
	}
}
