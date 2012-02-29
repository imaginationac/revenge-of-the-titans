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

import com.shavenpuppy.jglib.MultiBuffer;
import com.shavenpuppy.jglib.Resource;

import static org.lwjgl.opengl.GL11.*;

/**
 * A Distance Attenuation texture. This is a 1D luminance texture which simply fades from full
 * brightness at 0.0, through to black at 1.0.
 */
public class GLDistanceAttenuationTexture extends Resource implements GLRenderableObject {

	private static final long serialVersionUID = 1L;

	/** Scratch buffer */
	private static final MultiBuffer buf = new MultiBuffer(256);

	/** The texture ID */
	private transient int textureId;

	/**
	 *
	 */
	public GLDistanceAttenuationTexture() {
		super();
	}

	/**
	 * @param name
	 */
	public GLDistanceAttenuationTexture(String name) {
		super(name);
	}

	@Override
	public int getID() {
		assert isCreated() : this + " is not created yet";
		return textureId;
	}

	@Override
	public void render() {
		assert isCreated() : this + " is not created yet";
		glBindTexture(GL_TEXTURE_1D, textureId);
	}

	@Override
	protected void doCreate() {

		// 1. Generate a texture ID
		textureId = glGenTextures();

		// 2. Draw the image
		buf.bytes.clear();
		for (int i = 0; i < 256; i ++) {
			buf.bytes.put((byte)(255 - i));
		}

		// Prepare for reading
		buf.bytes.flip();

		// 3. Create the texture
		glBindTexture(GL_TEXTURE_1D, textureId);
		glTexImage1D(GL_TEXTURE_1D, 0, GL_ALPHA8, 256, 0, GL_ALPHA, GL_UNSIGNED_BYTE, buf.bytes);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP);

	}

	@Override
	protected void doDestroy() {
		if (textureId != 0) {
			glDeleteTextures(textureId);
			textureId = 0;
		}
	}



}
