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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * A MultiBuffer adds ints and floats on top of the bytes in a MemoryBuffer.
 * This kind of buffer is particularly useful at storing data to be sent to
 * OpenGL for client-side arrays, pointed to by vertexPointer() etc.
 *
 * @author Caspian Rychlik-Prince <cix_foo@users.sourceforge.net>
 */
public class MultiBuffer {

	/** The data, as bytes */
	public final ByteBuffer bytes;

	/** The data, as ints */
	public final IntBuffer ints;

	/** The data, as shorts */
	public final ShortBuffer shorts;

	/** The data, as floats */
	public final FloatBuffer floats;

	/**
	 * Constructor for MultiBuffer.
	 * @param size
	 */
	public MultiBuffer(int size) {
		bytes = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		ints = bytes.asIntBuffer();
		shorts = bytes.asShortBuffer();
		floats = bytes.asFloatBuffer();
	}

	/**
	 * Constructor for MultiBuffer.
	 * @param data
	 */
	public MultiBuffer(byte[] data) {
		this(data.length);
		bytes.put(data);
		bytes.rewind();
	}

	/**
	 * Constructor for MultiBuffer.
	 * @param buf
	 */
	public MultiBuffer(ByteBuffer buf) {
		bytes = buf;
		ints = bytes.asIntBuffer();
		shorts = bytes.asShortBuffer();
		floats = bytes.asFloatBuffer();
	}

}
