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
package net.puppygames.applet;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;

import static org.lwjgl.opengl.GL11.*;

/**
 * The original zoom transition effect
 */
public class ZoomTransition extends Transition {

	/** Fade in/out duration, in ticks */
	private static final int FADE_TIME = 15;

	/** Initial screen depth */
	private static final float START_DEPTH = -200.0f;

	/** Final screen depth */
	private static final float END_DEPTH = 400.0f;

	/**
	 * C'tor
	 */
	public ZoomTransition() {
	}

	/**
	 * C'tor
	 * @param name
	 */
	public ZoomTransition(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#getClosingDuration()
	 */
	@Override
	public int getClosingDuration() {
		return FADE_TIME;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#getOpeningDuration()
	 */
	@Override
	public int getOpeningDuration() {
		return FADE_TIME;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#postRenderClosing(int)
	 */
	@Override
	public void postRenderClosing(SpriteAllocator screen, int tick) {
		glPopMatrix();
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#postRenderOpening(int)
	 */
	@Override
	public void postRenderOpening(SpriteAllocator screen, int tick) {
		glPopMatrix();
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#preRenderClosing(int)
	 */
	@Override
	public void preRenderClosing(Screen screen, int tick) {
		float alpha = LinearInterpolator.instance.interpolate(1.0f, 0.0f, tick / (float) FADE_TIME);
		float depth = LinearInterpolator.instance.interpolate(0.0f, END_DEPTH, tick / (float) FADE_TIME);
		// Offset
		glPushMatrix();
		glTranslatef(0f, 0f, depth);

		screen.setAlpha(alpha);

	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#preRenderOpening(int)
	 */
	@Override
	public void preRenderOpening(Screen screen, int tick) {
		float alpha = LinearInterpolator.instance.interpolate(0.0f, 1.0f, tick / (float) FADE_TIME);
		float depth = LinearInterpolator.instance.interpolate(START_DEPTH, 0.0f, tick / (float) FADE_TIME);
		// Offset
		glPushMatrix();
		glTranslatef(0f, 0f, depth);

		screen.setAlpha(alpha);
	}

}
