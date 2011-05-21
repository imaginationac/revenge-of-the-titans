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

import java.nio.*;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.vector.Vector3f;

import com.shavenpuppy.jglib.*;
import com.shavenpuppy.jglib.algorithms.RadixSort;
import com.shavenpuppy.jglib.opengl.GLBaseTexture;
import com.shavenpuppy.jglib.opengl.GLVertexBufferObject;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.FloatList;

import static org.lwjgl.opengl.ARBBufferObject.*;
import static org.lwjgl.opengl.ARBMultitexture.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * A default sprite renderer. This sorts incoming sprites by layer, Z, then
 * optionally by Y coordinate, state, then texture. and stashes vertex, texture,
 * and colour coordinates in a buffer. The sprites are rendered by OpenGL11.
 */
class DefaultSpriteRenderer extends Resource implements SpriteRenderer {

	private static final long serialVersionUID = 1L;

	/** Scratch vector */
	private static final Vector3f offset = new Vector3f();

	/**
	 * Ring buffer, used by all the sprite engines.
	 */
	private static class RingBuffer {

		private static final int DEFAULT_STATE_RUNS = 1024;

		private class StateRun {
			GLBaseTexture texture0, texture1;
			Style style;
			int start, length;

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
				if (texture1 != lastRenderedTexture1) {
					if (texture1 != null) {
						texture1.render();
					}
					lastRenderedTexture1 = texture1;
				}

				if (length == 0) {
					return;
				}

				if (style.getRenderSprite()) {
					glDrawArrays(GL_QUADS, start, length);
				} else {
					style.render(start);
				}
			}
		}

		int bufferSize, bufferSizeInVertices;
		int numBuffers;

		boolean useVBOs;
		GLVertexBufferObject[] vbo;
		MultiBuffer[] buffer;
		int sequence = -1, mark = -1;
		MultiBuffer current;
		StateRun[] stateRun;

		GLBaseTexture lastRenderedTexture0, lastRenderedTexture1, currentTexture0, currentTexture1;
		Style lastRenderedStyle, currentStyle;
		int vertexCursor;
		int numRuns;
		StateRun currentRun;

		RingBuffer() {
		}

		void create() {
			useVBOs = GLContext.getCapabilities().GL_ARB_vertex_buffer_object;
			if (useVBOs) {
				bufferSize = 1024 * 256;
				bufferSizeInVertices = bufferSize / VERTEX_SIZE;
				numBuffers = 32;
			} else {
				bufferSize = 1024 * 1024 * 4;
				bufferSizeInVertices = bufferSize / VERTEX_SIZE;
				numBuffers = 1;
			}
			buffer = new MultiBuffer[numBuffers];

			if (useVBOs) {
				vbo = new GLVertexBufferObject[numBuffers];
				for (int i = 0; i < numBuffers; i ++) {
					vbo[i] = new GLVertexBufferObject(bufferSize, GL_ARRAY_BUFFER_ARB, GL_STREAM_DRAW_ARB);
					vbo[i].create();
				}
			} else {
				for (int i = 0; i < numBuffers; i ++) {
					buffer[i] = new MultiBuffer(bufferSize);
				}
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
			if (useVBOs) {
				vbo[sequence].render();
				ByteBuffer buf = vbo[sequence].map();
				if (buffer[sequence] == null || buffer[sequence].bytes != buf) {
					buffer[sequence] = new MultiBuffer(buf);
				}
			}
			current = buffer[sequence];
			current.bytes.clear();
			current.floats.clear();
			current.ints.clear();
			ContextCapabilities capabilities = GLContext.getCapabilities();
			if (useVBOs) {
				glVertexPointer(3, GL_FLOAT, VERTEX_SIZE, 0);
				if (capabilities.GL_ARB_multitexture) {
					glClientActiveTextureARB(GL_TEXTURE1_ARB);
					glTexCoordPointer(2, GL_FLOAT, VERTEX_SIZE, TEXTURE1_COORD_OFFSET);
					glClientActiveTextureARB(GL_TEXTURE0_ARB);
				} else if (capabilities.OpenGL13) {
					glClientActiveTexture(GL_TEXTURE1);
					glTexCoordPointer(2, GL_FLOAT, VERTEX_SIZE, TEXTURE1_COORD_OFFSET);
					glClientActiveTexture(GL_TEXTURE1);
				}
				glTexCoordPointer(2, GL_FLOAT, VERTEX_SIZE, TEXTURE0_COORD_OFFSET);
				glColorPointer(4, GL_UNSIGNED_BYTE, VERTEX_SIZE, COLOR_OFFSET);
			} else {
				glVertexPointer(3, VERTEX_SIZE, current.floats);
				if (capabilities.GL_ARB_multitexture) {
					glClientActiveTextureARB(GL_TEXTURE1_ARB);
					glTexCoordPointer(2, VERTEX_SIZE, Memory.chop(current.bytes, TEXTURE1_COORD_OFFSET).asFloatBuffer());
					glClientActiveTextureARB(GL_TEXTURE0_ARB);
				} else if (capabilities.OpenGL13) {
					glClientActiveTexture(GL_TEXTURE1);
					glTexCoordPointer(2, VERTEX_SIZE, Memory.chop(current.bytes, TEXTURE1_COORD_OFFSET).asFloatBuffer());
					glClientActiveTexture(GL_TEXTURE1);
				}
				glTexCoordPointer(2, VERTEX_SIZE, Memory.chop(current.bytes, TEXTURE0_COORD_OFFSET).asFloatBuffer());
				glColorPointer(4, true, VERTEX_SIZE, Memory.chop(current.bytes, COLOR_OFFSET));
			}

			vertexCursor = 0;
			numRuns = 0;
			currentRun = null;
		}

		void begin() {
			currentStyle = null;
			currentTexture0 = null;
			currentTexture1 = null;
			next();
			mark = sequence;
			glEnableClientState(GL_VERTEX_ARRAY);
		}

		void finish() {
			// Render out what's left over
			render();

			if (useVBOs) {
				glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
			}
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
			if (vertexCursor + 4 > bufferSizeInVertices) {
				// Sprite won't fit, so flush first
				render();
				next();
			}

			SpriteImage image = s.getImage();
			GLBaseTexture newTexture0 = image != null ? image.getTexture() : null;
			GLBaseTexture newTexture1 = s.getTexture();
			if (currentRun == null || newStyle != currentStyle || newTexture0 != currentTexture0 || newTexture1 != currentTexture1) {
				// Changed state. Start new state.
				currentRun = stateRun[numRuns];
				currentRun.start = vertexCursor;
				currentRun.length = 0;
				currentRun.style = newStyle;
				currentRun.texture0 = newTexture0;
				currentRun.texture1 = newTexture1;
				numRuns ++;

				if (numRuns == stateRun.length) {
					// Grow the array
					growStateRuns();
				}

				currentStyle = newStyle;
				currentTexture0 = newTexture0;
				currentTexture1 = newTexture1;
			}

			final float w = image.getWidth();
			final float h = image.getHeight();
			final float tx0 = image.getTx0();
			final float tx1 = image.getTx1();
			final float ty0 = image.getTy0();
			final float ty1 = image.getTy1();
			final int xscale = s.getXScale(); // 8 bits fraction
			final int yscale = s.getYScale(); // 8 bits fraction
			s.getOffset(offset);
			final float x = s.getX() + offset.getX();
			final float y = s.getY() + offset.getY();
			final float z = s.getZ() + offset.getZ();
			final float alphaDiv = 1.0f / 255.0f;
			final float alpha = engineAlpha * s.getAlpha() * alphaDiv;
			final double angle = FPMath.doubleValue(s.getAngle()) * Math.PI * 2.0;

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
				double cos = Math.cos(angle);
				double sin = Math.sin(angle);

				scaledx00 = (float) (cos * scaledx0 - sin * scaledy0);
				scaledx10 = (float) (cos * scaledx1 - sin * scaledy0);
				scaledx11 = (float) (cos * scaledx1 - sin * scaledy1);
				scaledx01 = (float) (cos * scaledx0 - sin * scaledy1);
				scaledy00 = (float) (sin * scaledx0 + cos * scaledy0);
				scaledy10 = (float) (sin * scaledx1 + cos * scaledy0);
				scaledy11 = (float) (sin * scaledx1 + cos * scaledy1);
				scaledy01 = (float) (sin * scaledx0 + cos * scaledy1);
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

			FloatBuffer floats = current.floats;
			IntBuffer ints = current.ints;
			int vertex = vertexCursor * VERTEX_SIZE;
			floats.position(vertex >> 2);
			ints.position((vertex + COLOR_OFFSET) >> 2);
			floats.put(x00);
			floats.put(y00);
			floats.put(z);
			floats.put(s.isMirrored() ? tx1 : tx0);
			floats.put(s.isFlipped() ? ty0 : ty1);
			floats.put(s.getTx00());
			floats.put(s.getTy00());
			ReadableColor color = s.getColor(0);
			ints.put((color.getRed() << 0) | (color.getGreen() << 8) | (color.getBlue() << 16)
					| (int) (color.getAlpha() * alpha) << 24);

			vertex += VERTEX_SIZE;
			floats.position(vertex >> 2);
			ints.position((vertex + COLOR_OFFSET) >> 2);
			floats.put(x10);
			floats.put(y10);
			floats.put(z);
			floats.put(s.isMirrored() ? tx0 : tx1);
			floats.put(s.isFlipped() ? ty0 : ty1);
			floats.put(s.getTx10());
			floats.put(s.getTy10());
			color = s.getColor(1);
			ints.put((color.getRed() << 0) | (color.getGreen() << 8) | (color.getBlue() << 16)
					| (int) (color.getAlpha() * alpha) << 24);

			vertex += VERTEX_SIZE;
			floats.position(vertex >> 2);
			ints.position((vertex + COLOR_OFFSET) >> 2);
			floats.put(x11);
			floats.put(y11);
			floats.put(z);
			floats.put(s.isMirrored() ? tx0 : tx1);
			floats.put(s.isFlipped() ? ty1 : ty0);
			floats.put(s.getTx11());
			floats.put(s.getTy11());
			color = s.getColor(2);
			ints.put((color.getRed() << 0) | (color.getGreen() << 8) | (color.getBlue() << 16)
					| (int) (color.getAlpha() * alpha) << 24);

			vertex += VERTEX_SIZE;
			floats.position(vertex >> 2);
			ints.position((vertex + COLOR_OFFSET) >> 2);
			floats.put(x01);
			floats.put(y01);
			floats.put(z);
			floats.put(s.isMirrored() ? tx1 : tx0);
			floats.put(s.isFlipped() ? ty1 : ty0);
			floats.put(s.getTx01());
			floats.put(s.getTy01());
			color = s.getColor(3);
			ints.put((color.getRed() << 0) | (color.getGreen() << 8) | (color.getBlue() << 16)
					| (int) (color.getAlpha() * alpha) << 24);


			vertexCursor += 4;
			currentRun.length += 4;
		}

		void add(Style s) {
			// Build the geometry
			FloatList data = s.build();

			// Does it fit?
			int vertsToWrite = data.size() >> 3;
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
			currentRun.texture1 = null;
			numRuns ++;
			if (numRuns == stateRun.length) {
				growStateRuns();
			}

			// Write the data out
			current.floats.position(vertexCursor * VERTEX_SIZE >> 2);
			current.floats.put(data.array(), 0, data.size());
			vertexCursor += vertsToWrite;

			currentStyle = null;
			currentTexture0 = null;
			currentTexture1 = null;

		}

		void render() {
			//System.out.println("  RENDER "+numRuns+" RUNS");
			if (useVBOs) {
				vbo[sequence].unmap();
			}
			lastRenderedStyle = null;
			lastRenderedTexture0 = null;
			lastRenderedTexture1 = null;
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
	private int[] sort_z;
	private int[] sort_y;
	private int[] sort_x;
	private int[] sort_layer;
	private int[] sort_sublayer;
	private int[] sort_style;
	private int[] sort_texture0;
	private int[] sort_texture1;

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

	/** The size of a single vertex in bytes: x,y,z,tx,ty,tx1,ty1,r,g,b,a */
	static final int VERTEX_SIZE = 32;
	static final int TEXTURE0_COORD_OFFSET = 12;
	static final int TEXTURE1_COORD_OFFSET = 20;
	static final int COLOR_OFFSET = 28;

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
		sort_texture1 = new int[1];
		sort_style = new int[1];
		sort_z = new int[1];
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
			style[numSprites] = FlashStyle.instance;
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
			int[] old_sort_z = sort_z;
			sort_z = new int[numSprites * 2];
			System.arraycopy(old_sort_z, 0, sort_z, 0, numSprites);
			old_sort_z = null;

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

			int[] old_sort_texture1 = sort_texture1;
			sort_texture1 = new int[numSprites * 2];
			System.arraycopy(old_sort_texture1, 0, sort_texture1, 0, numSprites);
			old_sort_z = null;

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
						GLBaseTexture t1 = s.getTexture();
						if (t1 != null) {
							sort_texture1[i] = t1.getID();
						} else {
							sort_texture1[i] = -1;
						}

						if (s.getLayer() >= sortLayer) {
							sort_y[i] = 0;
						} else {
							sort_y[i] = (int) -(s.getY() + s.getYSortOffset());
						}
					} else {
						sort_texture0[i] = 0;
						sort_texture1[i] = -1;
						sort_y[i] = 0;
					}
				}
			}

			sort.resetIndices().sort(sort_style, n).sort(sort_texture0, n).sort(sort_texture1, n).sort(sort_x, n).sort(
					sort_sublayer, n).sort(sort_y, n).sort(sort_layer, n);
		} else {
			for (int i = 0; i < n; i++) {
				final Sprite s = sprite[i];
				if (s.isVisible()) {
					sort_z[i] = (int) s.getZ();
					sort_layer[i] = s.getLayer();
					sort_sublayer[i] = s.getSubLayer();
					sort_style[i] = style[i].getStyleID();
					if (s.getStyle().getRenderSprite()) {
						final SpriteImage si = s.getImage();
						GLBaseTexture tex = si.getTexture();
						sort_texture0[i] = tex == null ? 0 : tex.getID();
						GLBaseTexture t1 = s.getTexture();
						if (t1 != null) {
							sort_texture1[i] = t1.getID();
						} else {
							sort_texture1[i] = -1;
						}
					} else {
						sort_texture0[i] = 0;
						sort_texture1[i] = -1;
					}
				}
			}
			sort.resetIndices().sort(sort_style, n).sort(sort_texture0, n).sort(sort_texture1, n).sort(sort_sublayer, n).sort(
					sort_z, n).sort(sort_layer, n);
		}
	}

	/**
	 * @param alpha
	 *            The alpha to set.
	 */
	void setAlpha(float alpha) {
		this.alpha = alpha;
	}
}
