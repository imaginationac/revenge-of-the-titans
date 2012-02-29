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
package worm.effects;

import java.util.ArrayList;

import net.puppygames.applet.Game;
import net.puppygames.applet.effects.Effect;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.GameMap;
import worm.Layers;
import worm.MapRenderer;
import worm.Tile;
import worm.Worm;
import worm.WormGameState;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.Interpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.interpolators.SineInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.util.ShortList;
import com.shavenpuppy.jglib.util.Util;

import static org.lwjgl.opengl.GL11.*;

public class ElectronZapEffect extends Effect {

	private static final long serialVersionUID = 1L;

	/** Length of a segment */
	private static final int SEGMENT_LENGTH = 8;

	/** Zap fade out duration in ticks */
	private static final int FADE_DURATION = 15;

	/** Damage */
	private static final int DAMAGE_INTERVAL = 2;

	static final Interpolator INTERPOLATOR;
	static {
		INTERPOLATOR = new Interpolator() {
			private static final long serialVersionUID = 1L;

			@Override
			public float interpolate(float a, float b, float ratio) {
				if (ratio < 0.5f) {
					return SineInterpolator.instance.interpolate(a, b, ratio * 2.0f);
				} else {
					return SineInterpolator.instance.interpolate(a, b, 1.0f - (ratio - 0.5f) * 2.0f);
				}
			}
		};
	}

	private static final GLRenderable SETUP = new GLRenderable() {
		@Override
		public void render() {
			glDisable(GL_TEXTURE_2D);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE);
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		}
	};

	private static final ArrayList<Entity> TEMP_ENTITIES = new ArrayList<Entity>();
	private static final Rectangle TEMP_RECT = new Rectangle();

	/** Rendering indices */
	private final ShortList indices = new ShortList(true, 32);

	private final float maxWidth;
	private final float maxWobble;
	private final float wobbleFactor;
	private final float widthFactor;
	private final boolean collides;

	private final EmitterFeature beamStartEmitter, beamEndEmitter;
	private final ALBuffer soundBuffer;

	private final ReadableColor color0, color1;
	private final int alpha;

	/** Source & target location */
	private final float sx, sy;
	private float tx, ty;

	/** Actual beam endpoint */
	private int x, y;

	/** Fading out */
	private boolean fading, done;

	/** Ticker */
	private int tick, soundTick, fadeSoundInTick;

	/** Segments */
	private float[] x0, y0, x1, y1, x2, y2, x3, y3;

	/** Current length (distance) */
	private int length;

	/** Number of segments */
	private int numSegments;

	/** Indices start and end */
	private int start, end, startPos, endPos;

	/** Sound effect */
	private SoundEffect soundEffect;

	public ElectronZapEffect
		(
			boolean collides,
			ALBuffer soundBuffer,
			ReadableColor color0,
			ReadableColor color1,
			int alpha,
			EmitterFeature beamStartEmitter,
			EmitterFeature beamEndEmitter,
			float sx,
			float sy,
			float maxWidth,
			float maxWobble,
			float wobbleFactor,
			float widthFactor
		)
	{
		this.collides = collides;
		this.soundBuffer = soundBuffer;
		this.color0 = color0;
		this.color1 = color1;
		this.alpha = alpha;
		this.beamStartEmitter = beamStartEmitter;
		this.beamEndEmitter = beamEndEmitter;
		this.sx = sx;
		this.sy = sy;
		this.maxWidth = maxWidth;
		this.maxWobble = maxWobble;
		this.wobbleFactor = wobbleFactor;
		this.widthFactor = widthFactor;
	}

	public void setTarget(float tx, float ty) {
		this.tx = tx;
		this.ty = ty;

		double dx = sx - tx;
		double dy = sy - ty;

		length = (int) Math.sqrt(dx * dx + dy * dy);
	}

	@Override
	protected void doUpdate() {
		ReadablePoint offset = getOffset();
		int ox, oy;
		if (offset == null) {
			ox = 0;
			oy = 0;
		} else {
			ox = offset.getX();
			oy = offset.getY();
		}

		// Calculate length & angle
		double dx = x - sx;
		double dy = y - sy;
		double tangent = Math.atan2(dy, dx) + Math.PI / 2.0;
		double dist = Math.sqrt(dx * dx + dy * dy);
		numSegments = (int) dist / SEGMENT_LENGTH + 1;
		if (x0 == null || x0.length < numSegments) {
			x0 = null;
			y0 = null;
			x1 = null;
			y1 = null;
			x2 = null;
			y2 = null;
			x3 = null;
			y3 = null;
			x0 = new float[numSegments];
			y0 = new float[numSegments];
			x1 = new float[numSegments];
			y1 = new float[numSegments];
			x2 = new float[numSegments];
			y2 = new float[numSegments];
			x3 = new float[numSegments];
			y3 = new float[numSegments];
		}
		for (int i = 0; i < numSegments; i ++) {
			float r2 = (float) i / (float) (numSegments - 1);
			double width = INTERPOLATOR.interpolate(0.0f, Math.min(maxWidth, i * widthFactor * SEGMENT_LENGTH), r2);
			double wobble = INTERPOLATOR.interpolate(0, Math.min(maxWobble, i * wobbleFactor * SEGMENT_LENGTH), r2);
			wobble *= Util.random() - 0.5;
			float xx = LinearInterpolator.instance.interpolate(sx, x, r2) + (float) (Math.cos(tangent) * wobble) + ox;
			float yy  = LinearInterpolator.instance.interpolate(sy, y, r2) + (float) (Math.sin(tangent) * wobble) + oy;
			x0[i] = xx - (float) (Math.cos(tangent) * width);
			y0[i] = yy - (float) (Math.sin(tangent) * width);
			x1[i] = xx + (float) (Math.cos(tangent) * width);
			y1[i] = yy + (float) (Math.sin(tangent) * width);
			x2[i] = xx - (float) (Math.cos(tangent) * (width + 1.0));
			y2[i] = yy - (float) (Math.sin(tangent) * (width + 1.0));
			x3[i] = xx + (float) (Math.cos(tangent) * (width + 1.0));
			y3[i] = yy + (float) (Math.sin(tangent) * (width + 1.0));
		}

		if (soundEffect != null) {
			soundEffect.setGain(Worm.calcGain(tx, ty) * Game.getSFXVolume() * soundBuffer.getGain(), this);
		}
	}

	@Override
	public void finish() {
		if (!fading) {
			fading = true;
			tick = 0;
		}
	}

	@Override
	protected void doTick() {
		tick ++;
		if (soundEffect != null && soundEffect.getOwner() != this) {
			// We lost the looping sound effect, so bring it back in a mo
			soundTick ++;
			if (soundTick >= FADE_DURATION) {
				soundEffect = Game.allocateSound(soundBuffer, 0.0f, 1.0f, this);
				fadeSoundInTick = FADE_DURATION;
			}
		}
		if (fading && soundEffect != null) {
			soundEffect.setGain(Math.max(0.0f, Math.min(1.0f, Worm.calcGain(tx, ty) * Game.getSFXVolume() * (1.0f - (float) tick / (float) FADE_DURATION) * soundBuffer.getGain())), this);
			return;
		}
		if (fadeSoundInTick > 0) {
			fadeSoundInTick --;
		}
		if (soundEffect != null) {
			soundEffect.setGain(Math.max(0.0f, Math.min(1.0f, Worm.calcGain(tx, ty) * Game.getSFXVolume() * (1.0f - (float) fadeSoundInTick / (float) FADE_DURATION) * soundBuffer.getGain())), this);
		}

		int totalLength = 0;
		if (collides) {
			WormGameState gameState = Worm.getGameState();
			x = (int) sx;
			y = (int) sy;
			GameMap map = gameState.getMap();
			int tileX = x / MapRenderer.TILE_SIZE;
			int tileY = y / MapRenderer.TILE_SIZE;

			int ox = x, oy = y;
			outer: for (int i = 0; i < length; i ++) {
				x = (int) LinearInterpolator.instance.interpolate(sx, tx, (float) i / (float) length);
				y = (int) LinearInterpolator.instance.interpolate(sy, ty, (float) i / (float) length);

				// Check map collision
				int newTileX = x / MapRenderer.TILE_SIZE;
				int newTileY = y / MapRenderer.TILE_SIZE;

				if (newTileX != tileX || newTileY != tileY) {
					tileX = newTileX;
					tileY = newTileY;

					for (int tileZ = 0; tileZ < GameMap.LAYERS; tileZ ++) {
						Tile t = map.getTile(tileX, tileY, tileZ);
						if (t != null && !t.isBulletThrough()) {
							// Hit a wall
							break outer;
						}
					}
				}

				// Check collision with entities
				if (ox != x || oy != y) {
					TEMP_ENTITIES.clear();
					TEMP_RECT.setBounds(x, y, 1, 1);
					Entity.getCollisions(TEMP_RECT, TEMP_ENTITIES);
					int numEntities = TEMP_ENTITIES.size();
					for (int j = 0; j < numEntities; j ++) {
						Entity entity = TEMP_ENTITIES.get(j);
						if (entity.isActive() && entity.isShootable() && entity.canCollide()) {
							if (tick % DAMAGE_INTERVAL == 0) {
								entity.capacitorDamage(1);
							} else {
								entity.stunDamage(3);
							}

							// And here the beam stops.
							break outer;
						}
					}
					ox = x;
					oy = y;
				}
				totalLength ++;
			}
		} else {
			x = (int) tx;
			y = (int) ty;

		}
		if (!collides || totalLength > 0) {
			// Spawn sparks and ting at end of beam
			Emitter e = beamEndEmitter.spawn(GameScreen.getInstance());
			e.setLocation(x, y);
			e.setOffset(GameScreen.getSpriteOffset());
			Emitter e2 = beamStartEmitter.spawn(GameScreen.getInstance());
			e2.setLocation(sx, sy);
			e2.setOffset(GameScreen.getSpriteOffset());
		}
	}

	@Override
	public boolean isEffectActive() {
		return (!fading || tick < FADE_DURATION) && !done;
	}

	@Override
	protected void doRemove() {
		done = true;

		if (soundEffect != null) {
			soundEffect.stop(this);
			soundEffect = null;
		}
	}

	@Override
	protected void render() {
		if (!isStarted() || !isVisible()) {
			return;
		}
		glRender(SETUP);
		int endAlpha = fading ? (int) LinearInterpolator.instance.interpolate(255.0f, 0.0f, (float) tick / FADE_DURATION) : 255;

		indices.ensureCapacity(numSegments * 4);
		indices.clear();

		for (int i = 0; i < numSegments; i ++) {
			if (i == 0 || i == numSegments - 1) {
				ColorUtil.setGLColorPre(color0, 0, this);
			} else {
				ColorUtil.setGLColorPre(color0, endAlpha * alpha / 255, this);
			}
			indices.add(glVertex2f(x2[i], y2[i]));
			indices.add(glVertex2f(x3[i], y3[i]));
		}
		glRender(GL_TRIANGLE_STRIP, indices.toArray(null));

		indices.clear();
		ColorUtil.setGLColorPre(color1, endAlpha * alpha / 255, this);
		for (int i = 0; i < numSegments; i ++) {
			indices.add(glVertex2f(x0[i], y0[i]));
			indices.add(glVertex2f(x1[i], y1[i]));
		}
		glRender(GL_TRIANGLE_STRIP, indices.toArray(null));
	}

	@Override
	protected void doSpawnEffect() {
		soundEffect = Game.allocateSound(soundBuffer, 1.0f, 1.0f, this);
	}

	@Override
	public int getDefaultLayer() {
		return Layers.CAPACITOR_EFFECT;
	}
}