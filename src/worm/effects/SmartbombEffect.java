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
import worm.Layers;
import worm.Res;

import com.shavenpuppy.jglib.interpolators.SineInterpolator;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.util.ShortList;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: SmartbombEffect.java,v 1.11 2010/03/29 22:39:46 foo Exp $
 * The smartbomb effect!
 * @author $Author: foo $
 * @version $Revision: 1.11 $
 */
public class SmartbombEffect extends Effect {

	/** Duration */
	public static final int DURATION = 20;

	/** Final radius */
	private static final float RADIUS = 200.0f;

	/** Width */
	private static final float WIDTH = 30.0f;

	/** Steps */
	private static final int STEP = 8;

	/** Radius */
	private float radius;

	/** Tick */
	private int tick;

	/** Position */
	private float x, y;

	/** Indices */
	private final ShortList indices = new ShortList(360 / STEP + 2);

	/**
	 * C'tor
	 * @param x
	 * @param y
	 */
	public SmartbombEffect(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	protected void doTick() {
		tick ++;
		radius = SineInterpolator.instance.interpolate(0.0f, RADIUS, (float) tick / (float) DURATION);
	}

	@Override
	protected void render() {
		float xx = x;
		float yy = y;

		glRender(new GLRenderable() {
			@Override
			public void render() {
				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE);
				Res.getSmartBombTexture().render();
			}
		});

		float innerRadius = SineInterpolator.instance.interpolate(0.0f, Math.max(0.0f, RADIUS - WIDTH), (float) tick / (float) DURATION);

		// Draw rings
		float ox = getOffset().getX();
		float oy = getOffset().getY();
		short count = 0;
		boolean writeIndices = indices.size() == 0;
		for (int i = 0; i <= 360; i += STEP) {
			// Inner vertex
			glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
			glTexCoord2f(i, 1.0f);
			glVertex2f(ox + xx + (float)Math.cos(Math.toRadians(i)) * innerRadius, oy + yy + (float)Math.sin(Math.toRadians(i)) * innerRadius);
			// Outer vertex
			glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			glTexCoord2f(i, 0.0f);
			glVertex2f(ox + xx + (float)Math.cos(Math.toRadians(i)) * radius, oy + yy + (float)Math.sin(Math.toRadians(i)) * radius);

			if (writeIndices) {
				indices.add(count ++);
				indices.add(count ++);
			}
		}
		if (writeIndices) {
			indices.add((short) 0);
			indices.add((short) 1);
			indices.trimToSize();
		}
		glRender(GL_TRIANGLE_STRIP, indices.array());
	}

	@Override
	public int getDefaultLayer() {
		return Layers.SMARTBOMB_EFFECT;
	}

	@Override
	public boolean isEffectActive() {
		return tick < DURATION;
	}

	/**
	 * Get the radius
	 * @return float
	 */
	public float getRadius() {
		return radius;
	}

}
