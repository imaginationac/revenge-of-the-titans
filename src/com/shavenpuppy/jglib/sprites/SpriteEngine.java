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
import com.shavenpuppy.jglib.opengl.GLRenderable;

/**
 * SpriteEngine The interface to sprite engines.
 *
 * @author caspian.prince
 * @version $Revision: 1.34 $
 */
public interface SpriteEngine extends IResource, GLRenderable, SpriteAllocator {

	/**
	 * Sets a new sprite processor to use for this sprite engine
	 *
	 * @param spriteProcessor
	 */
	void setSpriteProcessor(SpriteProcessor spriteProcessor);

	/**
	 * Gets the sprite processor being used by this sprite engine
	 *
	 * @return a SpriteProcessor
	 */
	SpriteProcessor getSpriteProcessor();

	/**
	 * Sets the tick rate
	 *
	 * @param newTickRate
	 */
	void setTickRate(int newTickRate);

	/**
	 * Gets the tick rate
	 *
	 * @return the tick rate
	 */
	int getTickRate();

	/**
	 * Deallocate a sprite. The sprite is returned to the sprite pool, and no matter what you do with it now, it won't do anything
	 * on the screen.
	 *
	 * @param sprite The sprite to return to the pool
	 */
	void deallocate(Sprite sprite);

	/**
	 * Remove all sprites
	 */
	void clear();

	/**
	 * Tick all sprites.
	 */
	void tick();

	/**
	 * @return Returns the alpha applied to all the sprites
	 */
	float getAlpha();

	/**
	 * Sets the base alpha for all the sprites.
	 *
	 * @param alpha The alpha to set.
	 */
	void setAlpha(float alpha);

	/**
	 * Get the SpriteRenderer
	 */
	SpriteRenderer getSpriteRenderer();
}
