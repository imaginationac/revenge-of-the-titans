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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.opengl.OpenGLException;

import com.shavenpuppy.jglib.Resource;

import static org.lwjgl.opengl.ARBBufferObject.*;

/**
 * Vertex buffer object wrapper
 */
public class GLVertexBufferObject extends Resource implements GLRenderableObject {

	private int size;
	private final int type, usage;

	private transient int id;
	private transient boolean mapped, cleared;
	private transient ByteBuffer buffer;

	/**
	 * C'tor
	 */
	public GLVertexBufferObject(int size, int type, int usage) {
		this.size = size;
		this.type = type;
		this.usage = usage;
	}

	/**
	 * C'tor
	 */
	public GLVertexBufferObject(int type, int usage) {
		this.size = 0;
		this.type = type;
		this.usage = usage;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.opengl.GLResource#doGLCreate()
	 */
	@Override
	protected void doCreate() {
		id = glGenBuffersARB();
	}

	/**
	 * Tells the driver we don't care about the data in our buffer any more (may improve performance before mapping)
	 */
	public void clear() {
		if (!cleared) {
			glBufferDataARB(type, size, usage);
			cleared = true;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.shavenpuppy.jglib.opengl.GLResource#doGLDestroy()
	 */
	@Override
	protected void doDestroy() {
		if (id != 0) {
			unmap();
			glDeleteBuffersARB(id);
			id = 0;
		}
	}

	public ByteBuffer map(int size) {
		if (!mapped) {
			if (this.size != size) {
				this.size = size;
				clear();
			}
			if (buffer != null && buffer.capacity() < size) {
				buffer = null;
			}
			//System.out.println("Mapping buffer "+this);
			ByteBuffer old = buffer;
			buffer = glMapBufferARB(type, GL_WRITE_ONLY_ARB, size, buffer);
			if (buffer == null) {
				throw new OpenGLException("Failed to map buffer "+this);
			}
			if (buffer != old && old != null) {
				//System.out.println("Had to map a new buffer of size "+size);
			}
			buffer.order(ByteOrder.nativeOrder()).clear().limit(size);
			mapped = true;
			cleared = false;
		}
		return buffer;
	}

	public ByteBuffer map() {
		if (!mapped) {
			assert size > 0;
			clear();
			ByteBuffer old = buffer;
			buffer = glMapBufferARB(type, GL_WRITE_ONLY_ARB, size, buffer);
			if (buffer == null) {
				throw new OpenGLException("Failed to map a buffer "+size+" bytes long");
			}
			if (buffer != old && old != null) {
				//System.out.println("Had to allocate a new buffer of size "+size+" for "+id);
			}
			buffer.order(ByteOrder.nativeOrder()).clear().limit(size);
			mapped = true;
			cleared = false;
		}
		return buffer;
	}

	public void orphan() {
		glMapBufferARB(type, usage, size, null);
	}


	public boolean unmap() {
		if (mapped) {
			mapped = false;
			return glUnmapBufferARB(type);
		} else {
			return true;
		}
	}

	public boolean isMapped() {
		return mapped;
	}

	@Override
	public String toString() {
		return "GLVertexBufferObject[" + id + ", " + size+", "+isCreated() + "]";
	}

	@Override
	public void render() {
		glBindBufferARB(type, id);
	}

	@Override
	public int getID() {
		return id;
	}

}
