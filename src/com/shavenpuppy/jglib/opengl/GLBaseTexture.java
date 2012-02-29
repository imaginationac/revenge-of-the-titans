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
import org.lwjgl.opengl.OpenGLException;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.util.XMLUtil;

import static org.lwjgl.opengl.GL11.*;

/**
 * The base class for GL textures.
 */
public abstract class GLBaseTexture extends Resource implements GLRenderableObject {

	private static final long serialVersionUID = 1L;

	/*
	 * Resource data
	 */

	/** Texture target (GL_TEXTURE_1D, GL_TEXTURE_2D, etc) */
	protected int target;

	/** Minification mode */
	protected int minMode;

	/** Magnification mode */
	protected int magMode;

	/** Whether to wrap. All axes are wrapped. */
	protected boolean wrap;

	/*
	 * Transient data
	 */

	/** The OpenGL texture ID */
	protected transient int texture;

	/**
	 * Resource constructor
	 */
	public GLBaseTexture() {
		super();
	}

	/**
	 * Resource constructor
	 */
	public GLBaseTexture(String name) {
		super(name);
	}

	/**
	 * Construct directly.
	 */
	public GLBaseTexture(
		String name,
		int target,
		int minMode,
		int magMode,
		boolean wrap)
	{
		this(name);

		this.target = target;
		this.minMode = minMode;
		this.magMode = magMode;
		this.wrap = wrap;

	}

	/**
	 * Create a GLTexture.
	 */
	@Override
	protected final void doCreate() {

		GLUtil.scratch.ints.clear().limit(1);
		glGenTextures(GLUtil.scratch.ints);
		texture = GLUtil.scratch.ints.get(0);

		doCreateTexture();

	}

	/**
	 * Actually generate the texture
	 */
	protected abstract void doCreateTexture();

	/**
	 * Destroy the texture.
	 */
	@Override
	protected void doDestroy() throws OpenGLException {
		if (texture != 0) {
			GLUtil.scratch.ints.clear();
			GLUtil.scratch.ints.put(0, texture);
			GLUtil.scratch.ints.limit(1);
			// FIXME: this crashes the driver if the texture is bound somewhere
			// HACK: just unbind texture 2d for now
			glBindTexture(GL_TEXTURE_2D, 0);
			glDeleteTextures(GLUtil.scratch.ints);
		}
	}

	/**
	 * Bind a texture
	 */
	@Override
	public void render() {
		assert isCreated() : this + " is not created yet";
		glBindTexture(target, texture);
	}

	public void bind()
	{
		render();
	}

	/* (non-Javadoc)
	 * @see GLXMLResource#load(Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		super.load(element, loader);

		target = GLUtil.decode(XMLUtil.getString(element, "target"));
		minMode = GLUtil.decode(XMLUtil.getString(element, "min"));
		magMode = GLUtil.decode(XMLUtil.getString(element, "mag"));
		wrap = GL11.GL_TRUE == GLUtil.decode(XMLUtil.getString(element, "wrap"));
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLObject#getID()
	 */
	@Override
	public final int getID() {
		assert isCreated() : this + " is not created yet";
		return texture;
	}

	public boolean isWrapped()
	{
		return wrap;
	}

	public abstract int getWidth();
	public abstract int getHeight();
}