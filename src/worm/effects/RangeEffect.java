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

import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.Effect;

import org.lwjgl.util.ReadableColor;

import worm.Layers;
import worm.Res;

import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLRenderable;

import static org.lwjgl.opengl.GL11.*;

/**
 * For showing the range of turrets & capacitors
 */
public class RangeEffect extends Effect {

	private static final int ALPHA_DELTA = 8;
	private static final float THICKNESS = 2.0f;

	private final ReadableColor color;

	private float radius;
	private float mapX, mapY;
	private int alpha;

	private boolean show = true;
	private boolean finished;
	private boolean done;

	private TickableObject tickableObject;

	/**
	 * C'tor
	 */
	public RangeEffect(ReadableColor color) {
		this.color = color;
	}

	/**
	 * @param radius the radius to set
	 */
	public void setRadius(float radius) {
		this.radius = radius;
	}

	/**
	 * Show or hide the effect.
	 * @param show the show to set
	 */
	public void setShow(boolean show) {
		this.show = show;
	}

	@Override
	public void finish() {
		finished = true;
		setShow(false);
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	protected void doRender() {
	}

	@Override
	protected void doTick() {
		if (!show) {
			alpha = Math.max(0, alpha - ALPHA_DELTA);
			if (finished && alpha == 0) {
				remove();
				return;
			}
		} else {
			alpha = Math.min(255, alpha + ALPHA_DELTA);
		}
	}

	@Override
	protected void doSpawn() {
		tickableObject = new TickableObject() {
			@Override
			protected void render() {
				if (!RangeEffect.this.isVisible()) {
					return;
				}
				float x = getOffset().getX() + mapX;
				float y = getOffset().getY() + mapY;
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glEnable(GL_BLEND);
						glDisable(GL_TEXTURE_2D);
						glEnable(GL_TEXTURE_1D);
						glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
						Res.getRangeTexture().bind();
						glBlendFunc(GL_SRC_ALPHA, GL_ONE);
					}
				});
				glBegin(GL_TRIANGLE_STRIP);
				ColorUtil.setGLColor(color, alpha, this);
				glTexCoord2f(0.0f, 0.0f);
				for (int i = 0; i <= 64; i ++) {
					double angle = i * Math.PI * 2.0 / 64.0;
					glTexCoord2f(0.0f, 0.0f);
					glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
					glTexCoord2f(1.0f, 0.0f);
					glVertex2f(x + (float) Math.cos(angle) * (radius - THICKNESS), y + (float) Math.sin(angle) * (radius - THICKNESS));
				}
				glEnd();
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glDisable(GL_TEXTURE_1D);
					}
				});
			}
		};
		tickableObject.setLayer(Layers.BUILDING_INFO);
		tickableObject.spawn(getScreen());
	}

	@Override
	protected void doRemove() {
		done = true;
		if (tickableObject != null) {
			tickableObject.remove();
			tickableObject = null;
		}
	}

	@Override
	public boolean isActive() {
		return !done;
	}

	/**
	 * Set the location of the effect
	 * @param mapX
	 * @param mapY
	 */
	public void setLocation(float mapX, float mapY) {
		this.mapX = mapX;
		this.mapY = mapY;
	}
}
