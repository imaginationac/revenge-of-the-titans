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
package net.puppygames.applet.widgets;

import java.io.Serializable;

import org.lwjgl.util.ReadableColor;

import com.shavenpuppy.jglib.sprites.SimpleRenderable;
import com.shavenpuppy.jglib.sprites.SimpleRenderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Draws a textured line between two points, of a specified width.
 */
public class Beam implements SimpleRenderable, Serializable {

	private static final long serialVersionUID = 1L;

	/** Line drawn between two points */
	private float x0, y0, x1, y1;

	/** Width */
	private float width;

	/** Colour at each end */
	private ReadableColor startColor, endColor;

	/**
	 * C'tor
	 */
	public Beam() {
	}

	/**
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 */
	public void setLocation(float x0, float y0, float x1, float y1) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}

	public void setStartColor(ReadableColor startColor) {
	    this.startColor = startColor;
    }

	public void setEndColor(ReadableColor endColor) {
	    this.endColor = endColor;
    }

	public void setWidth(float width) {
		this.width = width;
	}

	public float getWidth() {
		return width;
	}

	public float getX0() {
		return x0;
	}

	public float getX1() {
		return x1;
	}

	public float getY0() {
		return y0;
	}

	public float getY1() {
		return y1;
	}

	@Override
	public void render(SimpleRenderer renderer) {
		if (width <= 0.0f) {
			return;
		}
		renderer.glTexCoord2f(0.0f, 0.0f);
		if (startColor != null) {
			renderer.glColor(startColor);
		}
		double angle = Math.atan2(y1 - y0, x1 - x0);
		short idx = renderer.glVertex2f
			(
				x0 + (float) Math.cos(angle + Math.PI * 0.75) * width * 0.5f,
				y0 + (float) Math.sin(angle + Math.PI * 0.75) * width * 0.5f
			);
		renderer.glTexCoord2f(0.0f, 1.0f);
		renderer.glVertex2f
			(
				x0 + (float) Math.cos(angle + Math.PI * 1.25) * width * 0.5f,
				y0 + (float) Math.sin(angle + Math.PI * 1.25) * width * 0.5f
			);
		renderer.glTexCoord2f(1.0f, 1.0f);
		if (endColor != null) {
			renderer.glColor(endColor);
		}
		renderer.glVertex2f
			(
				x1 + (float) Math.cos(angle + Math.PI * 1.75) * width * 0.5f,
				y1 + (float) Math.sin(angle + Math.PI * 1.75) * width * 0.5f
			);
		renderer.glTexCoord2f(1.0f, 0.0f);
		renderer.glVertex2f
			(
				x1 + (float) Math.cos(angle + Math.PI * 0.25) * width * 0.5f,
				y1 + (float) Math.sin(angle + Math.PI * 0.25) * width * 0.5f
			);

		renderer.glRender(GL_TRIANGLE_FAN, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 3)});
	}

}
