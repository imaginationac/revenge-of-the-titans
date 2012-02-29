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

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.vector.ReadableVector2f;

import com.shavenpuppy.jglib.opengl.GLRenderable;

/**
 * The basic building blocks of drawing things with OpenGL.
 */
public interface SimpleRenderer {

	short getVertexOffset();
	short glVertex2f(float x, float y);
	short glVertex(ReadablePoint vertex);
	short glVertex(ReadableVector2f vertex);
	void glTexCoord2f(float u, float v);
	void glColor4ub(byte r, byte g, byte b, byte a);
	void glColor3ub(byte r, byte g, byte b);
	void glColor4f(float r, float g, float b, float a);
	void glColor3f(float r, float g, float b);
	void glColor(ReadableColor color);
	void glColori(int color);

	/**
	 * Render arbitrary OpenGL commands
	 * @param renderable
	 */
	void glRender(GLRenderable renderable);

	/**
	 * Render index mode geometry
	 * @param type GL_TRIANGLES, GL_QUADS, etc.
	 * @param indices The indices to draw
	 */
	void glRender(int type, short[] indices);
}