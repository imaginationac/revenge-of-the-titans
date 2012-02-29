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

import com.shavenpuppy.jglib.IResource;


/**
 * A SpriteRenderer renders multiple sprites. In usage, one should call {@link #preRender()}, then
 * {@link #render(Sprite)} for each sprite to be drawn, then {@link #postRender()}.
 */
public interface SpriteRenderer extends IResource {

	/**
	 * Render the sprite. The sprite itself is copied and subsequent changes to the sprite will not
	 * be reflected in the SpriteRenderer
	 * @param sprite The sprite to render; may not be null
	 */
	void render(Sprite sprite);

	/**
	 * Called after all {@link #render(Sprite)} calls have been made. This should reset the
	 * sprite renderer ready for the next frame.
	 */
	void postRender();

	/**
	 * Call before any calls to {@link #render(Sprite)}, at the beginning of a frame
	 */
	void preRender();

	/**
	 * Sets the alpha blend for the entire engine
	 * @param alpha 0.0f ... 1.0.f
	 */
	void setAlpha(float alpha);
}