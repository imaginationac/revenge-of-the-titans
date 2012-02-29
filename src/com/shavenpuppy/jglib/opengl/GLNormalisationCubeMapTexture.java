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

import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.util.vector.Vector3f;

import com.shavenpuppy.jglib.MultiBuffer;
import com.shavenpuppy.jglib.Resource;

import static org.lwjgl.opengl.ARBTextureCubeMap.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * A cube-map used to normalise light vectors for per-pixel lighting.
 * @author: cas
 */
public class GLNormalisationCubeMapTexture extends Resource implements GLRenderableObject {

	private static final long serialVersionUID = 1L;

	private final int size;

	private transient int textureId;

	/**
	 * @param size
	 */
	public GLNormalisationCubeMapTexture(int size) {
		super();
		this.size = size;
	}

	@Override
    protected void doCreate() {

		GLUtil.scratch.ints.clear().limit(1);
		glGenTextures(GLUtil.scratch.ints);
		textureId = GLUtil.scratch.ints.get(0);
		glBindTexture(GL_TEXTURE_CUBE_MAP_ARB, textureId);

		Vector3f vector = new Vector3f();
		int i, x, y;
		MultiBuffer pixels = new MultiBuffer(size * size * 3);

		glTexParameteri(GL_TEXTURE_CUBE_MAP_ARB, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP_ARB, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP_ARB, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP_ARB, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP_ARB, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		for (i = 0; i < 6; i++) {
			pixels.bytes.clear();
			for (y = 0; y < size; y++) {
				for (x = 0; x < size; x++) {
					getCubeVector(i, size, x, y, vector);
					pixels.bytes.put((byte) (127 * vector.x + 128));
					pixels.bytes.put((byte) (127 * vector.y + 128));
					pixels.bytes.put((byte) (127 * vector.z + 128));
				}
			}
			pixels.bytes.flip();
			glTexImage2D(
				GL_TEXTURE_CUBE_MAP_POSITIVE_X_ARB + i,
				0,
				GL_RGBA8,
				size,
				size,
				0,
				GL_RGB,
				GL_UNSIGNED_BYTE,
				pixels.bytes);
		}

	}

	@Override
    protected void doDestroy() throws OpenGLException {
		if (textureId != 0) {
			glDeleteTextures(textureId);
			textureId = 0;
		}
	}

	private static void getCubeVector(int i, int cubesize, int x, int y, Vector3f vector) {
		float s, t, sc, tc, mag;

		s = (x + 0.5f) / cubesize;
		t = (y + 0.5f) / cubesize;
		sc = s * 2.0f - 1.0f;
		tc = t * 2.0f - 1.0f;

		switch (i) {
			case 0 :
				vector.x = 1.0f;
				vector.y = -tc;
				vector.z = -sc;
				break;
			case 1 :
				vector.x = -1.0f;
				vector.y = -tc;
				vector.z = sc;
				break;
			case 2 :
				vector.x = sc;
				vector.y = 1.0f;
				vector.z = tc;
				break;
			case 3 :
				vector.x = sc;
				vector.y = -1.0f;
				vector.z = -tc;
				break;
			case 4 :
				vector.x = sc;
				vector.y = -tc;
				vector.z = 1.0f;
				break;
			case 5 :
				vector.x = -sc;
				vector.y = -tc;
				vector.z = -1.0f;
				break;
		}

		mag = (float) (1.0 / Math.sqrt(vector.x * vector.x + vector.y * vector.y + vector.z * vector.z));
		vector.x *= mag;
		vector.y *= mag;
		vector.z *= mag;
	}

	@Override
	public void render() throws OpenGLException {
		glBindTexture(GL_TEXTURE_CUBE_MAP_ARB, textureId);
	}

	@Override
	public int getID() {
		return textureId;
	}

}
