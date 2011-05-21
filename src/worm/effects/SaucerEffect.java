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
import worm.Layers;
import worm.entities.Saucer;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.opengl.GLRenderable;

import static org.lwjgl.opengl.GL11.*;

/**
 * Special effect highlighting where a saucer has appeared
 */
public class SaucerEffect extends Effect {

	public static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	/** Duration */
	private static final int RES_IN_DURATION = 120;
	private static final int RES_OUT_DURATION = 30;

	private static final float ALPHA = 0.25f;
	private static final float OUTER_ALPHA_MULT = 0.5f;

	/** Size of the outer circle */
	private static final float OUTER_START_SIZE = 1024.0f;

	private static final float OUTER_END_SIZE = 32.0f;

	private static final float INNER_SIZE = 128.0f;

	/** Speed the inner circle rotates at */
	private static final float INNER_ROTATION = 0.5f;

	/** Speed the outer circle rotates at */
	private static final float OUTER_ROTATION = -0.5f;

	/** Length of crosshair lines */
	private static final float LINE_LENGTH = 8.0f;

	/** line widths */
	private static final float LINE_WIDTH =  4.0f;

	/** Length of crosshair lines on inner circle */
	private static final float INNER_LINE_RADIUS = INNER_SIZE - LINE_LENGTH;

	/** Tick */
	private int tick;

	/** Outer circle angle */
	private float outerAngle;

	/** Inner circle angle */
	private float innerAngle;

	/** The saucer we are targeting on */
	private Saucer saucer;

	/** Phase */
	private int phase;
	private static final int PHASE_RES_IN = 0;
	private static final int PHASE_RES_OUT = 1;

	/** Rendered by... */
	private TickableObject tickableObject;

	/**
	 * C'tor
	 * @param saucer
	 */
	public SaucerEffect(Saucer saucer) {
		this.saucer = saucer;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.Effect#doSpawn()
	 */
	@Override
	protected void doSpawn() {
		tickableObject = new TickableObject() {
			@Override
			public void render() {
				final float x = saucer.getMapX() + saucer.getCollisionX();
				final float y = saucer.getMapY() + saucer.getCollisionY();
				float alpha;
				float radius;
				switch (phase) {
					case PHASE_RES_IN:
						radius = LinearInterpolator.instance.interpolate(OUTER_START_SIZE, OUTER_END_SIZE, tick / (float) RES_IN_DURATION);
						alpha = LinearInterpolator.instance.interpolate(0.0f, ALPHA, tick / (float) RES_IN_DURATION);
						break;
					case PHASE_RES_OUT:
						radius = LinearInterpolator.instance.interpolate(OUTER_END_SIZE, OUTER_START_SIZE, tick / (float) RES_OUT_DURATION);
						alpha = LinearInterpolator.instance.interpolate(ALPHA, 0.0f, tick / (float) RES_OUT_DURATION);
						break;
					default:
						assert false;
						radius = 0.0f;
						alpha = 0.0f;
				}

				float lineRadius = radius + LINE_LENGTH;

				// Draw outer circle
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glEnable(GL_BLEND);
						glDisable(GL_TEXTURE_2D);
						glBlendFunc(GL_SRC_ALPHA, GL_ONE);
						glLineWidth(LINE_WIDTH);
						glPushMatrix();
						glTranslatef(getOffset().getX() + x, getOffset().getY() + y, 0.0f);
						glEnableClientState(GL_COLOR_ARRAY);
					}
				});
				glBegin(GL_LINE_LOOP);
				{
					glColor4f(0.2f, 1.0f, 0.2f, alpha);
					for (int i = 0; i < 32; i++) {
						glVertex2f((float) Math.cos(i * Math.PI / 16.0) * radius, (float) Math.sin(i * Math.PI / 16.0) * radius);
					}
				}
				glEnd();

				// Draw crosshair lines at the edge of the circle
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glRotatef(outerAngle, 0.0f, 0.0f, 1.0f);
					}
				});
				glBegin(GL_LINES);
				{
					for (int i = 0; i < 32; i += 8) {
						glVertex2f((float) Math.cos(i * Math.PI / 16.0) * radius, (float) Math.sin(i * Math.PI / 16.0) * radius);
						glVertex2f((float) Math.cos(i * Math.PI / 16.0) * lineRadius, (float) Math.sin(i * Math.PI / 16.0) *
								lineRadius);
					}
				}
				glEnd();

				glRender(new GLRenderable() {
					@Override
					public void render() {
						glPopMatrix();
						glPushMatrix();
						glTranslatef(getOffset().getX() + x, getOffset().getY() + y, 0.0f);
						glRotatef(innerAngle, 0.0f, 0.0f, 1.0f);
						glLineStipple(1, (short) 0xF0F0);
						glEnable(GL_LINE_STIPPLE);
					}
				});
				glBegin(GL_LINE_LOOP);
				{
					glColor4f(0.2f, 1.0f, 0.2f, alpha*OUTER_ALPHA_MULT);
					for (int i = 0; i < 32; i++) {
						glVertex2f((float) Math.cos(i * Math.PI / 16.0) * INNER_SIZE, (float) Math.sin(i * Math.PI / 16.0) *
								INNER_SIZE);
					}
				}
				glEnd();

				glRender(new GLRenderable() {
					@Override
					public void render() {
						glDisable(GL_LINE_STIPPLE);
					}
				});
				glBegin(GL_LINES);
				{
					for (int i = 0; i < 32; i += 4) {
						glVertex2f((float) Math.cos(i * Math.PI / 16.0) * INNER_SIZE, (float) Math.sin(i * Math.PI / 16.0) *
								INNER_SIZE);
						glVertex2f((float) Math.cos(i * Math.PI / 16.0) * INNER_LINE_RADIUS, (float) Math.sin(i * Math.PI / 16.0) *
								INNER_LINE_RADIUS);
					}
				}
				glEnd();
				glRender(new GLRenderable() {
					@Override
					public void render() {
						glPopMatrix();
						glLineWidth(1.0f);
					}
				});
			}
		};
		tickableObject.spawn(getScreen());
		tickableObject.setLayer(Layers.BUILDING_INFO);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.Effect#doRemove()
	 */
	@Override
	protected void doRemove() {
		if (tickableObject != null) {
			tickableObject.remove();
			tickableObject = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see xap.resources.SpecialEffectResource.SpecialEffectInstance#doTick()
	 */
	@Override
	protected void doTick() {
		innerAngle += INNER_ROTATION;
		outerAngle += OUTER_ROTATION;

		switch (phase) {
			case PHASE_RES_IN:
				tick ++;
				// If saucer is no longer shootable, res out
				if (saucer.isDead() || !saucer.isActive()) {
					phase = PHASE_RES_OUT;
					tick = 0;
				}
				break;
			case PHASE_RES_OUT:
				tick ++;
				break;
			default:
				assert false;
		}
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.Effect#doRender()
	 */
	@Override
	protected void doRender() {
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Tickable#isActive()
	 */
	@Override
	public boolean isActive() {
		return !(phase == PHASE_RES_OUT && tick == RES_OUT_DURATION);
	}


}