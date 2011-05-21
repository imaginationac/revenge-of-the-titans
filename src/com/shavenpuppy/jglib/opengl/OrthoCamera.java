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
package com.shavenpuppy.jglib.opengl;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Simple Ortho2D style camera, for 2D rendering.
 *
 * @author John Campbell
 */
public class OrthoCamera {
	protected org.lwjgl.util.vector.Vector2f position;

	protected float halfWidth, halfHeight;

	/**
	 * Default constructor queries the LWJGL display and sets its width and
	 * height to match the current pixel size (so you can render using pixel
	 * positions like a normal 2d API).
	 */
	public OrthoCamera() {
		position = new Vector2f(0f, 0f);

		halfWidth = Display.getDisplayMode().getWidth() / 2f;
		halfHeight = Display.getDisplayMode().getHeight() / 2f;
	}

	/**
	 * Create a camera with the given width and height values.
	 *
	 * @param width
	 * @param height
	 */
	public OrthoCamera(float width, float height) {
		position = new Vector2f(0f, 0f);

		halfWidth = width / 2f;
		halfHeight = height / 2f;
	}

	public void preRender() {
		glMatrixMode(GL11.GL_PROJECTION);
		glLoadIdentity();

		glOrtho(-halfWidth, halfWidth, -halfHeight, halfHeight, -8.1f, 8.1f);

		// Translate to position
		glMatrixMode(GL11.GL_MODELVIEW);

		glPushMatrix();
		glLoadIdentity();

		int x = Math.round(position.x);
		int y = Math.round(position.y);
		glTranslatef(-x, -y, 0f);
	}

	public void postRender() {
		glPopMatrix();
	}

	public void setPosition(float x, float y) {
		position.set(x, y);
	}

	public void addToPosition(float x, float y) {
		position.x += x;
		position.y += y;
	}

	public ReadableVector2f getPosition() {
		return position;
	}

	public Vector2f screenToWorld(ReadableVector2f screenPos) {
		Vector2f worldCoords = new Vector2f(screenPos);

		float pixelHalfW = Display.getDisplayMode().getWidth() / 2f;
		float pixelHalfH = Display.getDisplayMode().getHeight() / 2f;

		// From pixel to normalise (+/-0.5) rect
		worldCoords.x -= pixelHalfW;
		worldCoords.y -= pixelHalfH;
		worldCoords.x /= pixelHalfW;
		worldCoords.y /= pixelHalfH;

		// From normalised to abstract rect
		worldCoords.x *= halfWidth;
		worldCoords.y *= halfHeight;

		// Take into account camera pos
		worldCoords.x += position.x;
		worldCoords.y += position.y;

		return worldCoords;
	}

	/** Rectangle intersection test with the camera */
	public boolean intersects(float x, float y, float width, float height) {
		if (x <= position.x + halfWidth) {
			if (x + width >= position.x - halfWidth) {
				if (y <= position.y + halfHeight) {
					if (y + height >= position.y - halfHeight) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
