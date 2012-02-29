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

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;

import com.shavenpuppy.jglib.opengl.GLFont;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLString;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: LabelEffect.java,v 1.6 2010/03/24 23:18:25 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.6 $
 */
public class LabelEffect extends SimpleBaseEffect {

	private static final GLRenderable SETUP_RENDERING = new GLRenderable() {
		@Override
		public void render() {
			glEnable(GL_TEXTURE_2D);
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		}
	};

	/** Text to render */
	private GLString label;

	/**
	 * @param font
	 * @param text
	 * @param startColor
	 * @param endColor
	 * @param duration
	 * @param fadeDuration
	 */
	public LabelEffect(GLFont font, String text, ReadableColor startColor, ReadableColor endColor, int duration, int fadeDuration) {
		super(startColor, endColor, duration, fadeDuration);

		label = new GLString(text, font);
		centre();
	}

	@Override
	protected void render() {
		if (!isStarted() || !isVisible()) {
			return;
		}
		glRender(SETUP_RENDERING);

		label.setColour(getCachedColor());
		label.render(this);
	}

	public void setText(String newText) {
		label.setText(newText);
		centre();
	}

	private void centre() {
		ReadableRectangle bounds = label.getBounds(null);
		setSize(bounds.getWidth(), bounds.getHeight());
	}

	@Override
	protected void doSetLocation() {
		// No need to do anything
	}

	@Override
	protected void doUpdate() {
		int x = (int) getX() - (getWidth() / 2);
		int y = (int) getY() - (getHeight() / 2);
		ReadablePoint offset = getOffset();
		if (offset != null) {
			x += offset.getX();
			y += offset.getY();
		}
		label.setLocation(x, y);
	}

	@Override
	public String toString() {
	    return "LabelEffect["+label.getText()+"]";
	}
}
