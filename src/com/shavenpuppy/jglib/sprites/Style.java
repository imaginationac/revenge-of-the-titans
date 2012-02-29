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


/**
 * The default sprite renderer has various different styles of rendering, which
 * affect what data it uses from the buffer and what GL state that it sets and
 * resets before and after rendering.
 */
public interface Style {

	/**
	 * Called to set up GL rendering state before actual drawing is done.
	 */
	void setupState();

	/**
	 * Called to reset GL rendering state after actual drawing is done.
	 */
	void resetState();

	/**
	 * @return the style's ID, which affects its rendering order
	 */
	int getStyleID();

	/**
	 * @return the style's alpha operation
	 */
	AlphaOp getAlphaOp();

	/**
	 * Whether to actually render a sprite when using this Style.
	 * @return boolean
	 */
	boolean getRenderSprite();

	/**
	 * If not rendering a sprite, then we perform a build() to create {@link GeometryData}
	 * Later, render() is called, with a vertex offset position and index offset position
	 * @return the number of vertices we wrote
	 */
	GeometryData build();

	/**
	 * If not rendering a sprite, then render stuff. Our geometry was written to a pre-prepared buffer which is pointed to
	 * already.
	 * @param vertexOffset
	 */
	 void render(int vertexOffset, int indexOffset);

}