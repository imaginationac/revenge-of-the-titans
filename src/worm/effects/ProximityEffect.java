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

import net.puppygames.applet.effects.Effect;

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import worm.Layers;
import worm.Res;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.resources.Background;

import static org.lwjgl.opengl.GL11.*;

/**
 * Proximity effect between buildings
 */
public class ProximityEffect extends Effect {

	private static final int FADE_DURATION = 5;
	private static final float FADE_RATE = 5.0f;

	private static final float START_RADIUS = 0.0f, END_RADIUS = 0.0f;

	/** Default width */
	private static final float WIDTH = 9.0f;

	/** Default alpha */
	private static final float ALPHA = 100.0f;

	/** Colour of the line */
	private ReadableColor color;

	/** Line drawn between two points */
	private float x0, y0, x1, y1;

	/** Alpha */
	private float alpha = ALPHA, currentAlpha;

	/** A background */
	private Background.Instance background;

	/** Maximum distance */
	private final float maxDist;

	private int phase;
	private static final int PHASE_NORMAL = 0;
	private static final int PHASE_FADE = 1;

	/** Fade tick */
	private int tick;

	/**
	 * C'tor
	 */
	public ProximityEffect(ReadableColor color, float maxDist, float x0, float y0) {
		this.color = color;
		this.maxDist = maxDist;
		this.x0 = x0;
		this.y0 = y0;
	}

	/**
	 * Set the target of the beam
	 * @param x1
	 * @param y1
	 */
	public void setTarget(float x1, float y1) {
		this.x1 = x1;
		this.y1 = y1;

		float dx = x1 - x0;
		float dy = y1 - y0;
		float distance = (float) Math.sqrt(dx * dx + dy * dy);
		alpha = distance <= maxDist ? ALPHA : 0;
		background.setBounds(new Rectangle(0, 0, (int) (distance - END_RADIUS), (int) WIDTH)); // Yeah notice width / height seem wrong way round...
	}

	@Override
	protected void doSpawnEffect() {
		background = Res.getBeamBackground().spawn();
		background.setColor(color);
	}

	@Override
    protected void render() {
		if (currentAlpha == 0.0f) {
			return;
		}
		glRender(new GLRenderable() {
			@Override
			public void render() {
				int xo = getOffset().getX();
				int yo = getOffset().getY();
				glPushMatrix();
				glTranslatef(xo + x1, yo + y1, 0.0f);
				double angle = Math.toDegrees(Math.atan2(y0 - y1, x0 - x1));
				glRotatef((float) angle, 0.0f, 0.0f, 1.0f);
				glTranslatef(START_RADIUS, - WIDTH / 2.0f, 0.0f);
//				glEnable(GL_TEXTURE_2D);
//				glEnable(GL_BLEND);
//				glBlendFunc(GL_SRC_ALPHA, GL_ONE);
//				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
			}
		});

		background.setAlpha((int) currentAlpha);
//		background.renderBackground(this);
		background.render(this);

		// Now reset state
		glRender(new GLRenderable() {
			@Override
			public void render() {
				glPopMatrix();
				glDisable(GL_TEXTURE_2D);
			}
		});

	}

	@Override
    public int getDefaultLayer() {
		return Layers.HITPOINTS;
	}

	@Override
	protected void doTick() {
		switch (phase) {
			case PHASE_NORMAL:
				if (Math.abs(currentAlpha - alpha) < FADE_RATE) {
					currentAlpha = alpha;
				} else if (currentAlpha < alpha) {
					currentAlpha += FADE_RATE;
				} else if (currentAlpha > alpha) {
					currentAlpha -= FADE_RATE;
				}
				return;
			case PHASE_FADE:
				tick ++;
				currentAlpha = LinearInterpolator.instance.interpolate(alpha, 0.0f, (float) tick / FADE_DURATION);
				return;
			default:
				assert false;
				return;
		}
	}

	@Override
	protected void doUpdate() {
	}

	@Override
	public void finish() {
		if (phase == PHASE_NORMAL) {
			phase = PHASE_FADE;
			tick = 0;
		}
	}

	@Override
	public boolean isEffectActive() {
		switch (phase) {
			case PHASE_NORMAL:
				return true;
			case PHASE_FADE:
				return tick < FADE_DURATION;
			default:
				assert false;
				return false;
		}
	}
}
