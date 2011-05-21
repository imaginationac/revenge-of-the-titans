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
package worm;

import com.shavenpuppy.jglib.opengl.GLBaseTexture;

/**
 *
 */
public class CosineTexture extends GLBaseTexture {

	/**
	 *
	 */
	public CosineTexture() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public CosineTexture(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param target
	 * @param minMode
	 * @param magMode
	 * @param wrap
	 */
	public CosineTexture(String name, int target, int minMode, int magMode, boolean wrap) {
		super(name, target, minMode, magMode, wrap);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLBaseTexture#doCreateTexture()
	 */
	@Override
	protected void doCreateTexture() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLBaseTexture#getWidth()
	 */
	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.opengl.GLBaseTexture#getHeight()
	 */
	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

}
