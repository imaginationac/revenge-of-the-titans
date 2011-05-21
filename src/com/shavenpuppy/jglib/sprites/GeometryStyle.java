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

import java.nio.BufferOverflowException;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;

import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.util.FloatList;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders arbitrary OpenGL geometry using VBOs. Every little bit of geometry
 * needs its own new instance of GeometryStyle. To use, override
 * {@link #render()} to call immediate-mode style commands, and
 * {@link #render()} to render it all.
 */
public abstract class GeometryStyle extends AbstractStyle implements SimpleRenderer {

	/** Geometry runs */
	private final ArrayList<GeometryRun> geometry = new ArrayList<GeometryRun>(1);

	/** Current geometry run, or null */
	private transient GeometryRun current;

	private transient FloatList vertices;
	private transient float u, v;
	private transient int c;
	private transient boolean inBeginEnd;
	private transient boolean hasColor, hasTexture;

	private class GeometryRun {
		GLRenderable renderable;
		int type;
		int startVertex, endVertex;
	}

	public GeometryStyle() {
	}

	@Override
	public final boolean getRenderSprite() {
		return false;
	}

	@Override
	public final int getStyleID() {
		return hashCode();
	}

	@Override
	public final FloatList build() {
		// Write the geometry to the buffer
		if (vertices == null) {
			vertices = new FloatList(true, 128);
		}
		vertices.clear();
		render();
		return vertices;
	}

	@Override
	public final void setupState() {
	}


	@Override
	public final void resetState() {
	}

	/**
	 * Override to make calls to the various gl* methods below
	 */
	protected abstract void render();

	@Override
	public void glBegin(int type) {
		assert !inBeginEnd;

		if (current == null) {
			current = new GeometryRun();
			current.type = type;
			current.startVertex = current.endVertex = vertices.size() >> 3;
		}

		inBeginEnd = true;
	}

	@Override
	public void glRender(final GLRenderable renderable) {
		if (inBeginEnd) {
			throw new OpenGLException("Must call glEnd first");
		}
		GeometryRun gr = new GeometryRun();
		gr.renderable = renderable;
		geometry.add(gr);
	}

	@Override
	public void glEnd() {
		if (!inBeginEnd) {
			throw new OpenGLException("Must call glBegin first");
		}
		current.endVertex = vertices.size() >> 3;
		if (current.endVertex - current.startVertex > 0) {
			geometry.add(current);
		}
		current = null;
		inBeginEnd = false;
	}

	@Override
	public void glVertex2f(float x, float y) {
		glVertex3f(x, y, 0.0f);
	}

	@Override
	public void glVertex3f(float x, float y, float z) {
		if (!inBeginEnd) {
			throw new OpenGLException("Must call glBegin first");
		}

		try {
			vertices.add(x);
			vertices.add(y);
			vertices.add(z);
			vertices.add(u);
			vertices.add(v);
			vertices.add(0.0f); // tex1 coords
			vertices.add(0.0f);
			vertices.add(Float.intBitsToFloat(c));
		} catch (BufferOverflowException e) {
			inBeginEnd = false;
			throw e;
		}
	}

	@Override
	public void glVertex(ReadablePoint vertex) {
		glVertex2f(vertex.getX(), vertex.getY());
	}

	@Override
	public void glVertex(ReadableVector2f vertex) {
		glVertex2f(vertex.getX(), vertex.getY());
	}

	@Override
	public void glVertex(ReadableVector3f vertex) {
		glVertex3f(vertex.getX(), vertex.getY(), vertex.getZ());
	}

	@Override
	public void glTexCoord2f(float u, float v) {
		this.u = u;
		this.v = v;
		hasTexture = true;
	}

	@Override
	public void glColor4ub(byte r, byte g, byte b, byte a) {
		this.c = ((a << 24) & 0xFF000000) | ((b << 16) & 0xFF0000) | ((g << 8) & 0xFF00) | (r & 0xFF);
		hasColor = true;
	}

	@Override
	public void glColor3ub(byte r, byte g, byte b) {
		glColor4ub(r, g, b, (byte) 255);
	}

	@Override
	public void glColor4f(float r, float g, float b, float a) {
		glColor4ub((byte) (r * 255.0f), (byte) (g * 255.0f), (byte) (b * 255.0f), (byte) (a * 255.0f));
	}

	@Override
	public void glColor3f(float r, float g, float b) {
		glColor4f(r, g, b, 1.0f);
	}

	@Override
	public void glColor(ReadableColor color) {
		glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
	}

	@Override
	public final void render(int vertexOffset) {
		glEnableClientState(GL_VERTEX_ARRAY);
		if (hasColor) {
			glEnableClientState(GL_COLOR_ARRAY);
		}
		if (hasTexture) {
			glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		}
		int n = geometry.size();
		for (int i = 0; i < n; i++) {
			GeometryRun run = geometry.get(i);
			if (run.renderable != null) {
				run.renderable.render();
			} else if (run.endVertex - run.startVertex > 0) {
				GL11.glDrawArrays(run.type, run.startVertex + vertexOffset, run.endVertex - run.startVertex);
			} else {
				// System.out.println("Empty run!");
			}
		}
		geometry.clear();
		if (hasColor) {
			glDisableClientState(GL_COLOR_ARRAY);
			hasColor = false;
		}
		if (hasTexture) {
			glDisableClientState(GL_TEXTURE_COORD_ARRAY);
			hasTexture = false;
		}
		glDisableClientState(GL_VERTEX_ARRAY);
	}

}
