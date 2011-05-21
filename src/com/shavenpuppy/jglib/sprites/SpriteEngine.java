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

import java.io.Serializable;

import com.shavenpuppy.jglib.opengl.GLRenderable;

/**
 * SpriteEngine
 *
 * The interface to sprite engines.
 *
 * @author caspian.prince
 * @version $Revision: 1.32 $
 */
public interface SpriteEngine extends GLRenderable
{

	/** 'Cos most SpriteEngines are actually derived from Resource we need to be able to do proper
	 *   creation and destruction.
	 *
	 *  Maybe what we actually want is some kind of Resource interface that we can share from here and
	 *  the existing Resouce base class?
	 */
	public void create();
	public void destroy();
	public boolean isCreated();

	/**
	 * Sets a new sprite processor to use for this sprite engine
	 * @param spriteProcessor
	 */
	public void setSpriteProcessor(SpriteProcessor spriteProcessor);

	/**
	 * Gets the sprite processor being used by this sprite engine
	 * @return a SpriteProcessor
	 */
	public SpriteProcessor getSpriteProcessor();

	/**
	 * Sets the tick rate
	 * @param newTickRate
	 */
	public void setTickRate(int newTickRate);

	/**
	 * Gets the tick rate
	 * @return the tick rate
	 */
	public int getTickRate();

	/**
	 * Allocate and initialize sprite. If there are no sprites available an exception is thrown.
	 * The sprite returned will be visible and active, with no animation or image.
	 * @param owner The sprite's new owner, which is used to keep track of debugging
	 * @return a sprite
	 */
	public Sprite allocate(Serializable owner);

	/**
	 * Deallocate a sprite. The sprite is returned to the sprite pool, and no matter what you do
	 * with it now, it won't do anything on the screen.
	 * @param sprite The sprite to return to the pool
	 */
	public void deallocate(Sprite sprite);

	/**
	 * Remove all sprites
	 */
	public void clear();

	/**
	 * Tick all sprites.
	 */
	public void tick();

	/**
	 * @return Returns the alpha applied to all the sprites
	 */
	public float getAlpha();

	/**
	 * Sets the base alpha for all the sprites.
	 * @param alpha The alpha to set.
	 */
	public void setAlpha(float alpha);

	/**
	 * Get the SpriteRenderer
	 */
	public SpriteRenderer getSpriteRenderer();
}