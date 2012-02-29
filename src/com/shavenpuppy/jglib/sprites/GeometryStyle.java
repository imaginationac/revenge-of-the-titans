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

import java.util.ArrayList;

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.vector.ReadableVector2f;

import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.util.FloatList;
import com.shavenpuppy.jglib.util.ShortList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * Renders arbitrary OpenGL geometry using VBOs.
 */
public abstract class GeometryStyle extends AbstractStyle implements SimpleRenderer {

	/** Geometry runs */
	private final ArrayList<GeometryRun> geometry = new ArrayList<GeometryRun>(1);

	/** The geometry data, which describes a list of vertices, and a list of indices */
	private transient GeometryData data;

	/** Vertices: this is the vertex data from the {@link #data} member */
	private transient FloatList vertices;

	/** Number of vertices */
	private transient short numVertices;

	/** Indices: this is the index data from the {@link #data} member */
	private transient ShortList indices;

	/** Current u/v coordinates */
	private transient float u, v;

	/** Current color (RGBA packed) */
	private transient int c;

	/** Whether the geometry specified has color and/or texture coordinates */
	private transient boolean hasColor, hasTexture;

	/** The last bit of RenderableGeometry we did */
	private transient MeshGeometry previous;

	/**
	 * Describes either a "renderable" (arbitrary OpenGL commands) or a call to glDrawRangeElements that will
	 * draw geometry specified in the geometry data.
	 */
	private interface GeometryRun {
		/**
		 * Render using the given vertex offset and index offset
		 * @param vertexOffset
		 * @param indexOffset
		 */
		void render(int vertexOffset, int indexOffset);
	}

	private static class RenderableGeometry implements GeometryRun {

		/** Renderable command */
		GLRenderable renderable;

		/**
		 * C'tor
		 * @param renderable
		 */
		public RenderableGeometry(GLRenderable renderable) {
	        this.renderable = renderable;
        }

		@Override
		public void render(int vertexOffset, int indexOffset) {
			renderable.render();
		}

	}

	private class MeshGeometry implements GeometryRun {

		int primitiveType;
		int numIndices;
		int offset;

		/**
		 * C'tor
         * @param primitiveType
         * @param startVertex
         * @param endVertex
         * @param numIndices
         */
        public MeshGeometry(int primitiveType, int offset, int numIndices) {
	        this.primitiveType = primitiveType;
	        this.numIndices = numIndices;
	        this.offset = offset;
        }

		@Override
		public void render(int vertexOffset, int indexOffset) {
			glDrawRangeElements(primitiveType, vertexOffset, vertexOffset + numVertices, numIndices, GL_UNSIGNED_SHORT, offset + indexOffset);
		}

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
	public final GeometryData build() {
		// Write the geometry to the buffer
		if (data == null) {
			data = new GeometryData(new FloatList(true, 128), new ShortList(true, 128));
			vertices = data.getVertexData();
			indices = data.getIndexData();
		}

		// TODO: optimise for static geometry - don't clear away everything, only call render() once, etc.
		data.clear();
		numVertices = 0;
		render();
		return data;
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
	public void glRender(GLRenderable renderable) {
		geometry.add(new RenderableGeometry(renderable));
		// Stop continuation
		previous = null;
	}

	private static boolean isContinuable(int type) {
    	return type == GL_TRIANGLES || type == GL_QUADS || type == GL_POINTS || type == GL_LINES;
    }

	@Override
	public void glRender(final int primitiveType, final short[] indices) {
		if (indices.length == 0) {
			return;
		}
		// If the last GeometryRun was the same type and is continuable, we'll extend it instead of doing a new one
		MeshGeometry gr;
		if (previous != null && previous.primitiveType == primitiveType && isContinuable(primitiveType)) {
			gr = previous;
			gr.numIndices += indices.length;
		} else {
			gr = new MeshGeometry(primitiveType, this.indices.size() * 2, indices.length);
			geometry.add(gr);
			previous = gr;
		}

		this.indices.addAll(indices);
	}

	@Override
	public short glVertex2f(float x, float y) {
		vertices.add(x);
		vertices.add(y);
		vertices.add(u);
		vertices.add(v);
		vertices.add(Float.intBitsToFloat(c));
		return numVertices ++;
	}

	@Override
    public short getVertexOffset() {
		return numVertices;
	}

	@Override
	public short glVertex(ReadablePoint vertex) {
		return glVertex2f(vertex.getX(), vertex.getY());
	}

	@Override
	public short glVertex(ReadableVector2f vertex) {
		return glVertex2f(vertex.getX(), vertex.getY());
	}

	@Override
	public void glTexCoord2f(float u, float v) {
		this.u = u;
		this.v = v;
		hasTexture = true;
	}

	@Override
	public void glColori(int color) {
		this.c = color;
		hasColor = true;
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
	public final void render(int vertexOffset, int indexOffset) {
		glEnableClientState(GL_VERTEX_ARRAY);
		if (hasColor) {
			glEnableClientState(GL_COLOR_ARRAY);
		} else {
			glDisableClientState(GL_COLOR_ARRAY);
		}
		if (hasTexture) {
			glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		} else {
			glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		}
		int n = geometry.size();
		for (int i = 0; i < n; i++) {
			GeometryRun run = geometry.get(i);
			run.render(vertexOffset, indexOffset * 2);
		}
		geometry.clear();
		previous = null;
		if (hasColor) {
			hasColor = false;
		}
		if (hasTexture) {
			hasTexture = false;
		}
		glEnableClientState(GL_COLOR_ARRAY);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
//		glDisableClientState(GL_VERTEX_ARRAY);

	}

}
