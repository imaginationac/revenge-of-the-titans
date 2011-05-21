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

import net.puppygames.applet.Screen;
import net.puppygames.applet.Transition;

import org.lwjgl.opengl.GL11;

import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;

/**
 * @author foo
 *
 */
public class ScrollTransition extends Transition {

	private static final int DURATION = 10;
	private static final float Y = -280.0f;

	/**
	 *
	 */
	public ScrollTransition() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public ScrollTransition(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#getClosingDuration()
	 */
	@Override
	public int getClosingDuration() {
		return DURATION;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#getOpeningDuration()
	 */
	@Override
	public int getOpeningDuration() {
		return DURATION;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#postRenderClosing(net.puppygames.applet.Screen, int)
	 */
	@Override
	public void postRenderClosing(SpriteAllocator screen, int tick) {
		GL11.glPopMatrix();
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#postRenderOpening(net.puppygames.applet.Screen, int)
	 */
	@Override
	public void postRenderOpening(SpriteAllocator screen, int tick) {
		GL11.glPopMatrix();
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#preRenderClosing(net.puppygames.applet.Screen, int)
	 */
	@Override
	public void preRenderClosing(Screen screen, int tick) {
		GL11.glPushMatrix();
		GL11.glTranslatef(0.0f, CosineInterpolator.instance.interpolate(0.0f, Y, (float) tick / DURATION), 0.0f);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Transition#preRenderOpening(net.puppygames.applet.Screen, int)
	 */
	@Override
	public void preRenderOpening(Screen screen, int tick) {
		GL11.glPushMatrix();
		GL11.glTranslatef(0.0f, CosineInterpolator.instance.interpolate(Y, 0.0f, (float) tick / DURATION), 0.0f);
		screen.setAlpha(1.0f);
	}

}
