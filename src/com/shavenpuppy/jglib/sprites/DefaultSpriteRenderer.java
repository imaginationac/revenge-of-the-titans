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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.vector.Vector2f;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.algorithms.RadixSort;
import com.shavenpuppy.jglib.opengl.GLBaseTexture;
import com.shavenpuppy.jglib.opengl.GLVertexBufferObject;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.FastMath;
import com.shavenpuppy.jglib.util.FloatList;
import com.shavenpuppy.jglib.util.ShortList;

import static org.lwjgl.opengl.ARBBufferObject.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * A default sprite renderer. This sorts incoming sprites by layer, Z, then
 * optionally by Y coordinate, state, then texture. and stashes vertex, texture,
 * and colour coordinates in a buffer. The sprites are rendered by OpenGL11.
 */
class DefaultSpriteRenderer extends Resource implements SpriteRenderer {

	private static final long serialVersionUID = 1L;

	private static final float PREMULT_ALPHA = 1.0f / 255.0f;

	/** Scratch vector */
	private static final Vector2f offset = new Vector2f();

	/**
	 * Ring buffer, used by all the sprite engines.
	 */
	private static class RingBuffer {

		private static final int DEFAULT_STATE_RUNS = 1024;

		private class StateRun {
			GLBaseTexture texture0;
			Style style;
			int start, length;
			ShortBuffer indices;
			int startIndex, endIndex;

			void render() {
				if (style != lastRenderedStyle) {
					if (lastRenderedStyle != null) {
						lastRenderedStyle.resetState();
					}
					if (style != null) {
						style.setupState();
					}
					lastRenderedStyle = style;
				}
				if (texture0 != lastRenderedTexture0) {
					if (texture0 != null) {
						texture0.render();
					}
					lastRenderedTexture0 = texture0;
				}

				if (length == 0) {
					return;
				}

				if (style.getRenderSprite()) {
					glDrawRangeElements(GL_TRIANGLES, start, start + length, endIndex - startIndex, GL_UNSIGNED_SHORT, startIndex * 2);
				} else {
					style.render(start, startIndex);
				}
			}
		}

		int bufferSize, bufferSizeInVertices, indexBufferSize;
		int numBuffers;

		GLVertexBufferObject[] vbo, ibo;
		FloatBuffer[] vertices;
		ByteBuffer[] verticesBytes;
		ShortBuffer[] indices;
		ByteBuffer[] indicesBytes;
		int sequence = -1, mark = -1;
		FloatBuffer currentVertices;
		ShortBuffer currentIndices;
		StateRun[] stateRun;

		GLBaseTexture lastRenderedTexture0, currentTexture0;
		Style lastRenderedStyle, currentStyle;
		int vertexCursor, indexCursor;
		int numRuns;
		StateRun currentRun;

		RingBuffer() {
		}

		void create() {
			bufferSize = 1024 * 256;
			bufferSizeInVertices = bufferSize / VERTEX_SIZE;
			numBuffers = 8;
			indexBufferSize = bufferSizeInVertices * 3;
			vertices = new FloatBuffer[numBuffers];
			verticesBytes = new ByteBuffer[numBuffers];
			indices = new ShortBuffer[numBuffers];
			indicesBytes = new ByteBuffer[numBuffers];

			vbo = new GLVertexBufferObject[numBuffers];
			ibo = new GLVertexBufferObject[numBuffers];
			for (int i = 0; i < numBuffers; i ++) {
				vbo[i] = new GLVertexBufferObject(bufferSize, GL_ARRAY_BUFFER_ARB, GL_STREAM_DRAW_ARB);
				vbo[i].create();
				ibo[i] = new GLVertexBufferObject(indexBufferSize * 2, GL_ELEMENT_ARRAY_BUFFER_ARB, GL_STREAM_DRAW_ARB);
				ibo[i].create();
			}
			stateRun = new StateRun[DEFAULT_STATE_RUNS];
			for (int i = 0; i < stateRun.length; i ++) {
				stateRun[i] = new StateRun();
			}
		}

		private void next() {
			// Select the next buffer in the sequence.
			sequence ++;
			if (sequence == numBuffers) {
				sequence = 0;
			}
			if (sequence == mark) {
				System.out.println("Buffer overrun");
			}
			vbo[sequence].render();
			ibo[sequence].render();
			ByteBuffer buf = vbo[sequence].map();
			if (vertices[sequence] == null || verticesBytes[sequence] != buf) {
				verticesBytes[sequence] = buf;
				vertices[sequence] = buf.asFloatBuffer();
			}
			ByteBuffer ibuf = ibo[sequence].map();
			if (indices[sequence] == null || indicesBytes[sequence] != ibuf) {
				indicesBytes[sequence] = ibuf;
				indices[sequence] = ibuf.asShortBuffer();
			}

			currentVertices = vertices[sequence];
			currentVertices.clear();

			currentIndices = indices[sequence];
			currentIndices.clear();

			glVertexPointer(2, GL_FLOAT, VERTEX_SIZE, 0);
			glTexCoordPointer(2, GL_FLOAT, VERTEX_SIZE, TEXTURE0_COORD_OFFSET);
			glColorPointer(4, GL_UNSIGNED_BYTE, VERTEX_SIZE, COLOR_OFFSET);

			vertexCursor = 0;
			indexCursor = 0;
			numRuns = 0;
			currentRun = null;
		}

		void begin() {
			currentStyle = null;
			currentTexture0 = null;
			next();
			mark = sequence;
			glEnableClientState(GL_VERTEX_ARRAY);
		}

		void finish() {
			// Render out what's left over
			render();

			glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
			glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, 0);
		}

		void growStateRuns() {
			StateRun[] newStateRun = new StateRun[(int)(stateRun.length * 1.5f)];
			System.arraycopy(stateRun, 0, newStateRun, 0, stateRun.length);
			for (int i = numRuns; i < newStateRun.length; i ++) {
				newStateRun[i] = new StateRun();
			}
			stateRun = newStateRun;
		}

		void add(Sprite s, Style newStyle, float engineAlpha) {
			if (vertexCursor + 4 > bufferSizeInVertices || indexCursor + 6 > indexBufferSize) {
				// Sprite won't fit, so flush first
				render();
				next();
			}

			SpriteImage image = s.getImage();
			GLBaseTexture newTexture0 = image != null ? image.getTexture() : null;
			if (currentRun == null || (newStyle != currentStyle && (currentStyle == null || newStyle.getStyleID() != currentStyle.getStyleID())) || newTexture0 != currentTexture0) {
				// Changed state. Start new state.
				currentRun = stateRun[numRuns];
				currentRun.start = vertexCursor;
				currentRun.length = 0;
				currentRun.style = newStyle;
				currentRun.texture0 = newTexture0;
				currentRun.indices = currentIndices;
				currentRun.startIndex = indexCursor;
				currentRun.endIndex = indexCursor;
				numRuns ++;

				if (numRuns == stateRun.length) {
					// Grow the array
					growStateRuns();
				}

				currentStyle = newStyle;
				currentTexture0 = newTexture0;
			}

			final float w = image.getWidth();
			final float h = image.getHeight();
			final float tx0 = image.getTx0();
			final float tx1 = image.getTx1();
			final float ty0 = image.getTy0();
			final float ty1 = image.getTy1();
			final int xscale = s.getXScale(); // 16 bits fraction
			final int yscale = s.getYScale(); // 16 bits fraction
			s.getOffset(offset);
			final float x = s.getX() + offset.getX();
			final float y = s.getY() + offset.getY();
			final int alpha = (int) (engineAlpha * s.getAlpha());
			final float angle = FPMath.floatValue(s.getAngle()) * FastMath.TAU;

			// First scale then rotate coordinates
			float scaledx0 = -image.getHotspotX();
			float scaledy0 = -image.getHotspotY();
			float scaledx1 = scaledx0 + w;
			float scaledy1 = scaledy0 + h;

			// Scale 'em first
			if (xscale != NO_SCALE || yscale != NO_SCALE) {
				float fxScale = FPMath.floatValue(xscale);
				float fyScale = FPMath.floatValue(yscale);
				scaledx0 = scaledx0 * fxScale;
				scaledx1 = scaledx1 * fxScale;
				scaledy0 = scaledy0 * fyScale;
				scaledy1 = scaledy1 * fyScale;
			}

			float scaledx00, scaledx10, scaledx11, scaledx01, scaledy00, scaledy10, scaledy11, scaledy01;

			// Then rotate
			if (angle != 0) {
				float cos = (float) Math.cos(angle);
				float sin = (float) Math.sin(angle);

				scaledx00 = cos * scaledx0 - sin * scaledy0;
				scaledx10 = cos * scaledx1 - sin * scaledy0;
				scaledx11 = cos * scaledx1 - sin * scaledy1;
				scaledx01 = cos * scaledx0 - sin * scaledy1;
				scaledy00 = sin * scaledx0 + cos * scaledy0;
				scaledy10 = sin * scaledx1 + cos * scaledy0;
				scaledy11 = sin * scaledx1 + cos * scaledy1;
				scaledy01 = sin * scaledx0 + cos * scaledy1;
			} else {
				scaledx00 = scaledx0;
				scaledx10 = scaledx1;
				scaledx11 = scaledx1;
				scaledx01 = scaledx0;
				scaledy00 = scaledy0;
				scaledy10 = scaledy0;
				scaledy11 = scaledy1;
				scaledy01 = scaledy1;
			}

			// Then translate them
			final float x00 = scaledx00 + x;
			final float x01 = scaledx01 + x;
			final float x11 = scaledx11 + x;
			final float x10 = scaledx10 + x;
			final float y00 = scaledy00 + y;
			final float y01 = scaledy01 + y;
			final float y11 = scaledy11 + y;
			final float y10 = scaledy10 + y;

			AlphaOp alphaOp = newStyle.getAlphaOp();
			FloatBuffer floats = currentVertices;
			float mtx = s.isMirrored() ? tx1 : tx0;
			float fty = s.isFlipped() ? ty0 : ty1;
			float mtx1 = s.isMirrored() ? tx0 : tx1;
			float mty1 = s.isFlipped() ? ty1 : ty0;
			floats.put(x00);
			floats.put(y00);
			floats.put(mtx);
			floats.put(fty);
			ReadableColor color = s.getColor(0);
			alphaOp.op(color, alpha, floats);

			floats.put(x10);
			floats.put(y10);
			floats.put(mtx1);
			floats.put(fty);
			color = s.getColor(1);
			alphaOp.op(color, alpha, floats);

			floats.put(x11);
			floats.put(y11);
			floats.put(mtx1);
			floats.put(mty1);
			color = s.getColor(2);
			alphaOp.op(color, alpha, floats);

			floats.put(x01);
			floats.put(y01);
			floats.put(mtx);
			floats.put(mty1);
			color = s.getColor(3);
			alphaOp.op(color, alpha, floats);


			// Write indices: need 6, for two triangles
			currentIndices.put((short) vertexCursor);
			currentIndices.put((short) (vertexCursor + 1));
			currentIndices.put((short) (vertexCursor + 2));
			currentIndices.put((short) vertexCursor);
			currentIndices.put((short) (vertexCursor + 2));
			currentIndices.put((short) (vertexCursor + 3));

			indexCursor += 6;
			vertexCursor += 4;
			currentRun.endIndex += 6;
			currentRun.length += 4;

		}

		void add(Style s) {
			// Build the geometry
			GeometryData data = s.build();

			// Does it fit?
			FloatList vertexData = data.getVertexData();
			ShortList indexData = data.getIndexData();

			int vertsToWrite = vertexData.size() / 5;
			if (vertexCursor + vertsToWrite > bufferSizeInVertices) {
				// No. Flush what we have so far.
//				System.out.println("  FLUSH 2");
				render();
				next();
			}

			// Always create a new run
			currentRun = stateRun[numRuns];
			currentRun.start = vertexCursor;
			currentRun.length = vertsToWrite;
			currentRun.style = s;
			currentRun.texture0 = null;
			currentRun.startIndex = indexCursor;
			numRuns ++;
			if (numRuns == stateRun.length) {
				growStateRuns();
			}

			// Write the data out
			currentVertices.position(vertexCursor * VERTEX_SIZE >> 2);
			currentVertices.put(vertexData.array(), 0, vertexData.size());

			currentIndices.position(indexCursor);
			short[] idx = indexData.array();
			int indicesToWrite = indexData.size();
			// Note that this irritation will go away once glDrawRangeElementsOffset is available generally...
			for (int i = 0; i < indicesToWrite; i ++) {
				idx[i] += vertexCursor;
			}
			currentIndices.put(idx, 0, indicesToWrite);

			vertexCursor += vertsToWrite;
			indexCursor += indicesToWrite;

			currentStyle = null;
			currentTexture0 = null;

		}

		void render() {
			//System.out.println("  RENDER "+numRuns+" RUNS");
			vbo[sequence].unmap();
			ibo[sequence].unmap();
			lastRenderedStyle = null;
			lastRenderedTexture0 = null;
			for (int i = 0; i < numRuns; i ++) {
				stateRun[i].render();
			}
			if (lastRenderedStyle != null) {
				lastRenderedStyle.resetState();
			}
		}
	}

	private static RingBuffer ringBuffer;

	/** A radix sorter for sorting */
	private final RadixSort sort = new RadixSort();

	/** Handy constant */
	private static final int NO_SCALE = FPMath.ONE;

	/** Debug */
	private static final boolean DEBUG = true;

	/** Alpha */
	private float alpha;

	/** Whether sprites are unique (rendered only once) */
	private final boolean uniqueSprites;

	/*
	 * Arrays for sorting
	 */
	private int[] sort_y;
	private int[] sort_x;
	private int[] sort_layer;
	private int[] sort_sublayer;
	private int[] sort_style;
	private int[] sort_texture0;

	/** Whether to sort Y coords */
	private final boolean sortY;

	/** The sprites being rendered */
	private Sprite[] sprite;

	/** Sprite styles */
	private Style[] style;

	/** The number of sprites being rendered */
	private int numSprites;

	/** Sort layer - sprites above this layer won't be Y sorted */
	private int sortLayer;

	/** The size of a single vertex in bytes: x,y,tx,ty,r,g,b,a */
	static final int VERTEX_SIZE = 20;
	static final int TEXTURE0_COORD_OFFSET = 8;
	static final int COLOR_OFFSET = 16;

	/**
	 * Constructor for SpriteRenderer.
	 * @param sortY
	 *            sort the sprites by Y coordinate
	 * @param sortLayer TODO
	 * @param uniqueSprites
	 *            If true, then you may submit a sprite more than once to the
	 *            render() function; this comes at some penalty in speed and
	 *            memory bandwidth. If false, then each sprite may be rendered
	 *            only once. Set to true if you are doing scrolling and need to
	 *            render sprites multiple times.
	 */
	DefaultSpriteRenderer(boolean sortY, int sortLayer, boolean uniqueSprites) {
		this.sortY = sortY;
		this.sortLayer = sortLayer;
		this.uniqueSprites = uniqueSprites;

		sprite = new Sprite[1];
		if (uniqueSprites) {
			for (int i = 0; i < sprite.length; i++) {
				sprite[i] = new Sprite(null);
			}
		}
		style = new Style[1];

		// Initialize radix sort
		sort_layer = new int[1];
		sort_sublayer = new int[1];
		sort_texture0 = new int[1];
		sort_style = new int[1];
		if (sortY) {
			sort_y = new int[1];
			sort_x = new int[1];
		} else {
			sort_y = null;
			sort_x = null;
		}
		sort.resetIndices();

	}

	@Override
	protected void doCreate() {
		if (ringBuffer == null) {
			ringBuffer = new RingBuffer();
			ringBuffer.create();
		}

		// Ensure the flash style is created too
		if (FlashStyle.instance == null) {
			FlashStyle.instance = new FlashStyle("flash.style");
			FlashStyle.instance.create();
		}
	}

	@Override
	protected void doDestroy() {
	}

	@Override
	public void render(Sprite s) {
		if (!s.isVisible()) {
			return;
		}
		SpriteImage image = s.getImage();
		Style spriteStyle = s.getStyle();
		if (image == null && spriteStyle == null) {
			return; // No point in doing anything with this sprite
		}
		// Check for flashing sprites, which overrides their default image rendering style with the FlashingStyle:
		if (s.isFlashing() && GLContext.getCapabilities().GL_EXT_secondary_color) {
			// Only need to render the sprite once, which will make it stark
			// white:
			addSprite(FlashStyle.instance, s);
//			if (uniqueSprites) {
//				Sprite copy = sprite[numSprites++];
//				copy.copy(s);
//			} else {
//				sprite[numSprites++] = s;
//			}
		} else {
			addSprite(spriteStyle, s);
//			style[numSprites] = spriteStyle;
//			if (uniqueSprites) {
//				sprite[numSprites++].copy(s);
//			} else {
//				sprite[numSprites++] = s;
//			}
		}
	}

	private void addSprite(Style spriteStyle, Sprite s) {
		if (numSprites == sprite.length) {
			if (sortY) {
				int[] old_sort_y = sort_y;
				sort_y = new int[numSprites * 2];
				System.arraycopy(old_sort_y, 0, sort_y, 0, numSprites);
				old_sort_y = null;

				int[] old_sort_x = sort_x;
				sort_x = new int[numSprites * 2];
				System.arraycopy(old_sort_x, 0, sort_x, 0, numSprites);
				old_sort_x = null;
			}


			int[] old_sort_layer = sort_layer;
			sort_layer = new int[numSprites * 2];
			System.arraycopy(old_sort_layer, 0, sort_layer, 0, numSprites);
			old_sort_layer = null;

			int[] old_sort_sublayer = sort_sublayer;
			sort_sublayer = new int[numSprites * 2];
			System.arraycopy(old_sort_sublayer, 0, sort_sublayer, 0, numSprites);
			old_sort_sublayer = null;

			int[] old_sort_style = sort_style;
			sort_style = new int[numSprites * 2];
			System.arraycopy(old_sort_style, 0, sort_style, 0, numSprites);
			old_sort_style = null;

			int[] old_sort_texture0 = sort_texture0;
			sort_texture0 = new int[numSprites * 2];
			System.arraycopy(old_sort_texture0, 0, sort_texture0, 0, numSprites);
			old_sort_texture0 = null;

			Sprite[] old_sprite = sprite;
			sprite = new Sprite[numSprites * 2];
			System.arraycopy(old_sprite, 0, sprite, 0, numSprites);
			old_sprite = null;

			Style[] old_style = style;
			style = new Style[numSprites * 2];
			System.arraycopy(old_style, 0, style, 0, numSprites);
			old_style = null;
		}
		style[numSprites] = spriteStyle;
		if (uniqueSprites) {
			Sprite copy = sprite[numSprites];
			if (copy == null) {
				copy = sprite[numSprites] = new Sprite(null);
			}
			copy.copy(s);
		} else {
			sprite[numSprites] = s;
		}
		numSprites ++;
	}

	@Override
	public void postRender() {

		// If there are no sprites, do nothing
		if (numSprites == 0) {
			return;
		}

		// Sort sprites first
		sort();

		// Build the texture/style runs up
		ringBuffer.begin();
		build();
		ringBuffer.finish();

	}

	/**
	 * Build up a list of texture runs. We scan through the sorted list of
	 * sprites so far and build up runs of sprites which share the same texture.
	 */
	private void build() {
		final int[] index = sort.getIndices();
		for (int i = 0; i < numSprites; i++) {
			int idx = index[i];
			Sprite s = sprite[idx];
			if (s.isVisible()) {
				Style newStyle = style[idx];

				if (newStyle.getRenderSprite()) {
					// It's a sprite
					ringBuffer.add(s, newStyle, alpha);

				} else {
					// It's geometry; we build it to find out how many vertices it's got
					ringBuffer.add(newStyle);
				}
			}
		}
	}

	@Override
	public void preRender() {
		// Reset everything
		numSprites = 0;
	}

	/**
	 * Sort sprites
	 */
	private void sort() {
		final int n = numSprites;
		if (sortY) {
			for (int i = 0; i < n; i++) {
				final Sprite s = sprite[i];
				if (s.isVisible()) {
					sort_x[i] = (int) s.getX();
					sort_layer[i] = s.getLayer();
					sort_sublayer[i] = s.getSubLayer();
					sort_style[i] = style[i].getStyleID();
					if (s.getStyle().getRenderSprite()) {
						final SpriteImage si = s.getImage();
						GLBaseTexture tex = si.getTexture();
						sort_texture0[i] = tex == null ? 0 : tex.getID();

						if (s.getLayer() >= sortLayer) {
							sort_y[i] = 0;
						} else {
							sort_y[i] = (int) -(s.getY() + s.getYSortOffset());
						}
					} else {
						sort_texture0[i] = 0;
						sort_y[i] = 0;
					}
				}
			}

			sort.resetIndices().sort(sort_style, n).sort(sort_texture0, n).sort(sort_x, n).sort(
					sort_sublayer, n).sort(sort_y, n).sort(sort_layer, n);
		} else {
			for (int i = 0; i < n; i++) {
				final Sprite s = sprite[i];
				if (s.isVisible()) {
					sort_layer[i] = s.getLayer();
					sort_sublayer[i] = s.getSubLayer();
					sort_style[i] = style[i].getStyleID();
					if (s.getStyle().getRenderSprite()) {
						final SpriteImage si = s.getImage();
						GLBaseTexture tex = si.getTexture();
						sort_texture0[i] = tex == null ? 0 : tex.getID();
					} else {
						sort_texture0[i] = 0;
					}
				}
			}
			sort.resetIndices().sort(sort_style, n).sort(sort_texture0, n).sort(sort_sublayer, n).sort(sort_layer, n);
		}
	}

	/**
	 * @param alpha
	 *            The alpha to set.
	 */
	@Override
    public void setAlpha(float alpha) {
		this.alpha = alpha;
	}
}
