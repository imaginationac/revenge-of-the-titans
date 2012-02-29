/*
 * Copyright (c) 2003 Shaven Puppy Ltd
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
package net.puppygames.applet.effects;


import org.lwjgl.util.ReadablePoint;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.interpolators.SineInterpolator;
import com.shavenpuppy.jglib.opengl.GLBaseTexture;
import com.shavenpuppy.jglib.opengl.GLRenderable;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: BlastEffect.java,v 1.2 2010/02/08 22:20:43 foo Exp $
 * The smartbomb effect!
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public class BlastEffect extends Effect {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_LAYER = 99;

	private static final int STEP = 8;
	private static final short[] STRIP_INDICES;
	static {
		STRIP_INDICES = new short[720 / STEP + 2];
		for (int i = 0; i < STRIP_INDICES.length; i ++) {
			STRIP_INDICES[i] = (short) i;
		}
		STRIP_INDICES[STRIP_INDICES.length - 2] = 0;
		STRIP_INDICES[STRIP_INDICES.length - 1] = 1;
	}

	/** Duration */
	private int duration;

	/** Final radius */
	private float finalRadius;

	/** Width */
	private float width;

	/** Radius */
	private float radius;

	/** Tick */
	private int tick;

	/** Position */
	private float x, y;

	/** Texture */
	private GLBaseTexture texture;

	/** Fade duration */
	private int fadeDuration;

	/** Fading */
	private boolean fading;

	/** Fade during expansion */
	private boolean fadeWhenExpanding;

	/**
	 * C'tor
	 */
	public BlastEffect(float x, float y, int duration, int fadeDuration, float radius, float width, GLBaseTexture texture) {
		this.x = x;
		this.y = y;
		this.duration = duration;
		this.fadeDuration = fadeDuration;
		this.finalRadius = radius;
		this.width = width;
		this.texture = texture;
	}

	@Override
	public int getDefaultLayer() {
	    return DEFAULT_LAYER;
	}

	@Override
	protected void doTick() {
		tick ++;
		if (!fading) {
			radius = SineInterpolator.instance.interpolate(0.0f, finalRadius, (float) tick / (float) duration);
			if (tick >= duration && !fadeWhenExpanding) {
				tick = 0;
				fading = true;
			}
		}
	}

	@Override
	protected void render() {
		glRender(new GLRenderable() {
			@Override
			public void render() {
				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE);
				texture.render();
			}
		});

		float xx = x;
		float yy = y;

		ReadablePoint offset = getOffset();
		if (offset != null) {
			xx += offset.getX();
			yy += offset.getY();
		}

		float innerRadius;

		// Draw rings
		float alpha;
		if (fading) {
			alpha = LinearInterpolator.instance.interpolate(1.0f, 0.0f, (float) tick / (float) fadeDuration);
			innerRadius = SineInterpolator.instance.interpolate(Math.max(0.0f, finalRadius - width), finalRadius, (float) tick / (float) fadeDuration);
		} else {
			if (fadeWhenExpanding) {
				alpha = LinearInterpolator.instance.interpolate(1.0f, 0.0f, (float) tick / (float) fadeDuration);
			} else {
				alpha = 1.0f;
			}
			innerRadius = SineInterpolator.instance.interpolate(0.0f, Math.max(0.0f, finalRadius - width), (float) tick / (float) duration);
		}
		for (int i = 0; i <= 360; i += STEP) {
			// Inner vertex
			glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
			glTexCoord2f(i, 1.0f);
			glVertex2f(xx + ((float)Math.cos(Math.toRadians(i))) * innerRadius, yy + ((float)Math.sin(Math.toRadians(i))) * innerRadius);
			// Outer vertex
			glColor4f(1.0f, 1.0f, 1.0f, alpha);
			glTexCoord2f(i, 0.0f);
			glVertex2f(xx + ((float)Math.cos(Math.toRadians(i))) * radius, yy + ((float)Math.sin(Math.toRadians(i))) * radius);
		}

		glRender(GL_TRIANGLE_STRIP, STRIP_INDICES);
	}

	@Override
	public boolean isEffectActive() {
		return 	fadeWhenExpanding ? tick < duration : (!fading || tick < fadeDuration);
	}

	/**
	 * Get the radius
	 * @return float
	 */
	public float getRadius() {
		return radius;
	}

	/**
	 * @return true if the blast is fading
	 */
	public boolean isFading() {
		return fading;
	}

	/**
	 * @param fadeWhenExpanding the fadeWhenExpanding to set
	 */
	public void setFadeWhenExpanding(boolean fadeWhenExpanding) {
		this.fadeWhenExpanding = fadeWhenExpanding;
	}
}
