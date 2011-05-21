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

import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

/**
 * $Id: GLBackBufferTexture.java,v 1.9 2011/04/18 23:28:06 cix_foo Exp $
 *
 * A texture thjat is created from the back buffer.
 *
 * @author $Author: cix_foo $
 * @version $Revision: 1.9 $
 */
public class GLBackBufferTexture extends GLBaseTexture {

	private static final long serialVersionUID = 1L;

	protected final int width, height;

	/**
	 * @param name
	 */
	public GLBackBufferTexture(String name, int width, int height) {
		super(name, GL_TEXTURE_2D, GL_NEAREST, GL_NEAREST, false);
		this.width = width;
		this.height = height;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLBaseTexture#doCreateTexture()
	 */
	@Override
	protected void doCreateTexture() {
		glBindTexture(target, texture);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glTexParameteri(target, GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
		glTexParameteri(target, GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
		glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magMode);
		glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minMode);
		glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 0, 0, width, height, 0);
	}

	/**
	 * Update the buffer
	 */
	public void update(int x, int y) {
		glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, x, y, width, height);
	}

	/**
	 * Update the buffer
	 */
	public void update(int x, int y, int w, int h) {
		glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, x, y, Math.min(w, width), Math.min(h, height));
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLBaseTexture#getWidth()
	 */
	@Override
	public int getWidth() {
		return width;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLBaseTexture#getHeight()
	 */
	@Override
	public int getHeight() {
		return height;
	}
}
