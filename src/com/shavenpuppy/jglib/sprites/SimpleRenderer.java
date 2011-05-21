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
package com.shavenpuppy.jglib.sprites;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;

import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLRenderable;

/**
 * The basic building blocks of drawing things with OpenGL.
 */
public interface SimpleRenderer {

	public static final SimpleRenderer GL_RENDERER = new SimpleRenderer() {

		@Override
		public void glVertex3f(float x, float y, float z) {
			GL11.glVertex3f(x, y, z);
		}

		@Override
		public void glVertex2f(float x, float y) {
			GL11.glVertex2f(x, y);
		}

		@Override
		public void glVertex(ReadableVector3f vertex) {
			GL11.glVertex3f(vertex.getX(), vertex.getY(), vertex.getZ());
		}

		@Override
		public void glVertex(ReadableVector2f vertex) {
			GL11.glVertex2f(vertex.getX(), vertex.getY());
		}

		@Override
		public void glVertex(ReadablePoint vertex) {
			GL11.glVertex2i(vertex.getX(), vertex.getY());
		}

		@Override
		public void glTexCoord2f(float u, float v) {
			GL11.glTexCoord2f(u, v);
		}

		@Override
		public void glBegin(int type) {
			GL11.glBegin(type);
		}

		@Override
		public void glEnd() {
			GL11.glEnd();
		}

		@Override
		public void glColor4ub(byte r, byte g, byte b, byte a) {
			GL11.glColor4ub(r, g, b, a);
		}

		@Override
		public void glColor4f(float r, float g, float b, float a) {
			GL11.glColor4f(r, g, b, a);
		}

		@Override
		public void glColor3ub(byte r, byte g, byte b) {
			GL11.glColor3ub(r, g, b);
		}

		@Override
		public void glColor3f(float r, float g, float b) {
			GL11.glColor3f(r, g, b);
		}

		@Override
		public void glColor(ReadableColor color) {
			ColorUtil.setGLColor(color);
		}

		@Override
		public void glRender(GLRenderable renderable) {
			renderable.render();
		}
	};

	public void glBegin(int type);

	public void glEnd();

	public void glVertex2f(float x, float y);

	public void glVertex3f(float x, float y, float z);

	public void glVertex(ReadablePoint vertex);

	public void glVertex(ReadableVector2f vertex);

	public void glVertex(ReadableVector3f vertex);

	public void glTexCoord2f(float u, float v);

	public void glColor4ub(byte r, byte g, byte b, byte a);

	public void glColor3ub(byte r, byte g, byte b);

	public void glColor4f(float r, float g, float b, float a);

	public void glColor3f(float r, float g, float b);

	public void glColor(ReadableColor color);

	public void glRender(GLRenderable renderable);


}