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
package com.shavenpuppy.jglib;

import java.io.*;
import java.nio.*;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

/**
 * A colour palette. The palette is always 32-bit.
 *
 * @author foo
 */
public final class Palette implements Externalizable {

	private static final long serialVersionUID = 1L;

	/** The palette format */
	private int format;

	/** Palette formats */
	public static final int ABGR = 0;
	public static final int BGRA = 1;
	public static final int ARGB = 2;
	public static final int RGBA = 3;

	/** The palette data, stored in the specified format */
	private ByteBuffer data;

	/**
	 * Default constructor for Palette, for serialization
	 */
	public Palette() {
		super();
	}

	/**
	 * Construct a palette with the specified format and specified number of entries.
	 * The number of entries must be a power of 2.
	 * @param format The palette format
	 * @param size The number of entries
	 */
	public Palette(int format, int size) {
		this.format = format;
		data = ByteBuffer.allocateDirect(size << 2).order(ByteOrder.nativeOrder());
	}

	/**
	 * Construct a palette with the specified format from an existing buffer
	 * @param format The palette format
	 * @param buf The existing buffer
	 */
	public Palette(int format, IntBuffer buf) {
		this.format = format;
		data = ByteBuffer.allocateDirect(buf.capacity() << 2).order(ByteOrder.nativeOrder());
		data.asIntBuffer().put(buf);
	}

	/**
	 * Construct a palette with the specified format from an existing array
	 * @param format The palette format
	 * @param buf The existing array of palette entries
	 */
	public Palette(int format, int[] buf) {
		this.format = format;
		data = ByteBuffer.allocateDirect(buf.length << 2).order(ByteOrder.nativeOrder());
		data.asIntBuffer().put(buf);
	}

	/* (non-Javadoc)
	 * @see java.io.Externalizable#readExternal(ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {

		format = in.readInt();

		switch (format) {
			case ABGR:
			case ARGB:
			case BGRA:
			case RGBA:
				break;
			default:
				throw new IOException("Illegal palette format "+format);
		}

		int size = in.readInt();
		data = ByteBuffer.allocateDirect(size << 2).order(ByteOrder.nativeOrder());

		IntBuffer buf =	data.asIntBuffer();
		for (int i = 0; i < size; i ++) {
	        buf.put(in.readInt());
        }

		buf.flip();
	}

	/* (non-Javadoc)
	 * @see java.io.Externalizable#writeExternal(ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(format);
		int size = getSize();
		out.writeInt(size);
		IntBuffer buf = data.asIntBuffer();
		for (int i = 0; i < size; i ++) {
	        out.writeInt(buf.get(i));
        }
	}

	/**
	 * Gets the format of the palette
	 * @return the format of the palette
	 */
	public final int getFormat() {
		return format;
	}

	/**
	 * Gets the size of the palette
	 * @return the number of entries in the palette
	 */
	public final int getSize() {
		return data.capacity() >> 2;
	}

	/**
	 * @return the buffer used to store the palette
	 */
	public final ByteBuffer getBuffer() {
		return data;
	}

	/**
	 * Write a color to the palette
	 * @param index The palette index
	 * @param color The color to write
	 */
	public void setColor(int index, ReadableColor color) {
		data.position(index << 2);
		switch (format) {
			case RGBA:
				color.writeRGBA(data);
				break;
			case ARGB:
				color.writeARGB(data);
				break;
			case BGRA:
				color.writeBGRA(data);
				break;
			case ABGR:
				color.writeABGR(data);
				break;
			default:
				assert false;
		}
	}

	/**
	 * Get a color from the palette
	 * @param index The palette index
	 * @param color A color to store the result in, or null, to create a new Color
	 * @return color, or a new Color
	 */
	public Color getColor(int index, Color color) {
		if (color == null) {
	        color = new Color();
        }

		data.position(index << 2);
		switch (format) {
		}
		return color;
	}

}
