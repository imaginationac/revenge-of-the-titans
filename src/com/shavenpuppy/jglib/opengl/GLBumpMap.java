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

import org.lwjgl.util.vector.Vector3f;

import com.shavenpuppy.jglib.Image;
import com.shavenpuppy.jglib.resources.ImageResource;

import static org.lwjgl.opengl.GL11.*;

/**
 * Normal bump map texture
 * @author cas
 */
public class GLBumpMap extends GLTexture {

	private static final long serialVersionUID = 1L;

	/*
	 * Resource data
	 */

	/** The bump map scale factor. The bigger this is, the smaller the bumps are. */
	protected float scale;

	/*
	 * Transient data
	 */

	/**
	 * Resource constructor
	 * @param name
	 */
	public GLBumpMap(String name) {
		super(name);
	}

	/**
	 * @param name
	 * @param url
	 * @param scale
	 */
	public GLBumpMap(String name, String url, float scale) {
		super(name, url, GL_TEXTURE_2D, GL_RGB, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, true);

		this.scale = scale;
	}

	/**
	 * @param name
	 * @param imageResource
	 * @param scale
	 */
	public GLBumpMap(String name, ImageResource imageResource, float scale) {
		super(name, imageResource, GL_TEXTURE_2D, GL_RGB, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, true);

		this.scale = scale;
	}

	/**
	 * @param name
	 * @param image
	 * @param scale
	 */
	public GLBumpMap(String name, Image image, float scale) {
		super(name, image, GL_TEXTURE_2D, GL_RGB, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, true);

		this.scale = scale;
	}

	/**
	 * Preprocesses the image. The image must be of LUMINANCE type or an assertion
	 * error is thrown.
	 * @see com.shavenpuppy.jglib.opengl.GLTexture#preprocess()
	 */
	@Override
	protected Image preprocess() {
		int index = 0, srcindex = 0;
		final Vector3f[] vec = new Vector3f[image.getWidth() * image.getHeight()];
		final Vector3f[] dst = new Vector3f[image.getWidth() * image.getHeight()];
		final byte[] nrm = new byte[image.getWidth() * image.getHeight() * 3];

		// Create vector3f array first:
		for (int j = 0; j < image.getHeight(); ++j) {
			for (int i = 0; i < image.getWidth(); ++i) {
				vec[index++] = new Vector3f(i, j, (image.getData().get(srcindex++) & 0xff));
			}
		}

		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f c = new Vector3f();

		// Now work out normals
		for (int j = 0; j < image.getHeight(); j++) {
			for (int i = 0; i < image.getWidth(); i++) {
				a.x = 2;
				a.y = 0;
				a.z = vec[index(i + 1, j)].z - vec[index(i - 1, j)].z;
				//			a = vec(i+1,   j) - p(i-1,   j);
				b.x = 0;
				b.y = 2;
				b.z = vec[index(i, j + 1)].z - vec[index(i, j - 1)].z;
				//			b = vec(  i, j+1) - p(  i, j-1);
				Vector3f.cross(a, b, c);
				c.z *= scale;
				c.normalise();
				dst[i + j * image.getWidth()] = new Vector3f(c);
			}
		}
		// Now transform into rgb:
		index = 0;
		srcindex = 0;
		for (int j = 0; j < image.getHeight(); j++) {
			for (int i = 0; i < image.getWidth(); i++) {
				nrm[index++] = (byte) ((dst[srcindex].x + 1f) * 127.5f);
				nrm[index++] = (byte) ((dst[srcindex].y + 1f) * 127.5f);
				nrm[index++] = (byte) ((dst[srcindex++].z + 1f) * 127.5f);
			}
		}

		// Create an SpriteImage out of our bump data
		return new Image(image.getWidth(), image.getHeight(), Image.RGB, nrm);
	}

	/**
	 * Insert the method's description here.
	 * Creation date: (20/11/2001 15:13:11)
	 */
	private int index(int x, int y) {
		if (x < 0) {
			x += image.getWidth();
		} else if (x >= image.getWidth()) {
			x -= image.getWidth();
		}
		if (y < 0) {
			y += image.getHeight();
		} else if (y >= image.getHeight()) {
			y -= image.getHeight();
		}

		return x + (y * image.getWidth());
	}
}
