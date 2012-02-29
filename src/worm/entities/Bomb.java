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
package worm.entities;

import java.util.ArrayList;
import java.util.List;

import net.puppygames.applet.effects.BlastEffect;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.GameStateInterface;
import worm.features.BombFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.util.FPMath;

/**
 * @author foo
 *
 */
public class Bomb extends Entity {

	private static final long serialVersionUID = 1L;

	private static final float GRAVITY = 0.01f;
	private static final float FRICTION = 0.99f;

	private final List<Entity> hit = new ArrayList<Entity>();

	private final BombFeature feature;
	private final Gidrah carrier;

	private float z;
	private float dz;
	private float vx;
	private float vy;
	private int phase;

	private static final int PHASE_SPAWNING = 0;
	private static final int PHASE_CARRIED = 1;
	private static final int PHASE_DROPPING = 2;
	private static final int PHASE_EXPLODING = 3;

	private transient BlastEffect blastEffect;
	private transient Emitter[] emitter;

	/**
	 * C'tor
	 */
	public Bomb(BombFeature feature, Gidrah carrier) {
		this.feature = feature;
		this.carrier = carrier;
	}

	@Override
	protected void doSpawn() {
		doRespawn();
	}

	@Override
	protected void doRespawn() {
		float x = carrier.getMapX() + feature.getBombOffset(isMirrored()).getX();
		float y = carrier.getMapY() + feature.getBombOffset(isMirrored()).getY();
		feature.getAppearance().createSprites(GameScreen.getInstance(), x, y, this);

		float z = carrier.getFinalYOffset();
		getSprite(0).setOffset(0.0f, z);
		getSprite(0).setChildYOffset(z*1.0f/FPMath.floatValue(getSprite(0).getYScale()));

		emitter = feature.getAppearance().createEmitters(GameScreen.getInstance(), x, y);
		if (emitter != null) {
			for (Emitter element : emitter) {
				element.setLocation(x, y);
				element.setYOffset(z);
			}
		}
	}

	@Override
	protected void doRemove() {
		if (emitter != null) {
			for (Emitter element : emitter) {
				element.remove();
			}
			emitter = null;
		}
	}

	@Override
	protected void doTick() {
		switch (phase) {
			case PHASE_SPAWNING:
				tickSpawn();
				break;
			case PHASE_CARRIED:
				tickCarried();
				break;
			case PHASE_DROPPING:
				tickDropping();
				break;
			case PHASE_EXPLODING:
				tickExploding();
				break;
			default:
				assert false : "Unknown phase "+phase;
		}
	}

	private void tickSpawn() {
		phase = PHASE_CARRIED;
	}

	private void tickCarried() {
		float x = carrier.getMapX() + feature.getBombOffset(isMirrored()).getX();
		float y = carrier.getMapY() + feature.getBombOffset(isMirrored()).getY();
		setLocation(x, y);

		float z = carrier.getFinalYOffset();
		getSprite(0).setOffset(0.0f, z);
		getSprite(0).setChildYOffset(z*1.0f/FPMath.floatValue(getSprite(0).getYScale()));

		if (emitter != null) {
			for (Emitter element : emitter) {
				element.setLocation(x, y);
				element.setYOffset(z);
			}
		}
	}

	private void tickDropping() {
		vx *= FRICTION;
		vy *= FRICTION;
		if (Math.abs(vx) < 0.01f) {
			vx = 0.0f;
		}
		if (Math.abs(vy) < 0.01f) {
			vy = 0.0f;
		}
		setLocation(getMapX() + vx, getMapY() + vy);
		z -= dz;
		dz += GRAVITY;
		if (z <= 0.0) {
			z = 0.0f;
			explode();
			return;
		}

		getSprite(0).setOffset(0.0f, z);
		getSprite(0).setChildYOffset(z*1.0f/FPMath.floatValue(getSprite(0).getYScale()));

		if (emitter != null) {
			for (Emitter element : emitter) {
				element.setYOffset(z);
			}
		}
	}

	private void explode() {
		phase = PHASE_EXPLODING;
		blastEffect = new BlastEffect(getMapX(), getMapY(), 16, 16, feature.getExplosionRadius(), feature.getExplosionRadius(), feature.getExplosionTexture());
		blastEffect.setFadeWhenExpanding(true);
		blastEffect.setOffset(GameScreen.getSpriteOffset());
		blastEffect.spawn(GameScreen.getInstance());
		EmitterFeature blastEmitterFeature = feature.getExplosion();
		Emitter blastEmitter = blastEmitterFeature.spawn(GameScreen.getInstance());
		blastEmitter.setLocation(getMapX(), getMapY());
		GameScreen.shake(feature.getShake());
		setVisible(false);

	}

	private void tickExploding() {
		if (blastEffect.isFinished()) {
			remove();
		}
	}

	public void drop(float vx, float vy) {
		if (phase != PHASE_CARRIED || !isActive()) {
			return;
		}
		ALBuffer dropNoise = feature.getDropNoise();
		if (dropNoise != null) {
			// TODO
		}
		phase = PHASE_DROPPING;

		this.vx = vx;
		this.vy = vy;
		z = carrier.getFinalYOffset();
	}

	@Override
	public float getZ() {
		return z;
	}

	@Override
	protected void doUpdate() {
	}

	@Override
	public void addToGameState(GameStateInterface gsi) {
	}

	@Override
	public void removeFromGameState(GameStateInterface gsi) {
	}

	@Override
	public boolean canCollide() {
		return phase == PHASE_EXPLODING;
	}

	@Override
	public void onCollision(Entity entity) {
		entity.onCollisionWithBomb(this);
	}

	@Override
	public void onCollisionWithBuilding(Building building) {
		if (hit.contains(building)) {
			return;
		}
		hit.add(building);
		building.damage(feature.getExplosionDamage());
	}

	@Override
	public void onCollisionWithGidrah(Gidrah gidrah) {
		if (hit.contains(gidrah)) {
			return;
		}
		hit.add(gidrah);
		gidrah.explosionDamage(feature.getExplosionDamage() * 4, false);
	}

	@Override
	public float getRadius() {
		return canCollide() && blastEffect != null ? blastEffect.getRadius() : 0.0f;
	}

	@Override
	public Rectangle getBounds(Rectangle bounds) {
		return null;
	}

	@Override
	public boolean isRound() {
		return true;
	}

	@Override
	public boolean isShootable() {
		return false;
	}

}
