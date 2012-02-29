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

import com.shavenpuppy.jglib.Resource;

/**
 * The sprite engine contains a list of sprites which it takes care of,
 * and has a SpriteRenderer to take care of actually doing the drawing.
 */
public class StaticSpriteEngine extends Resource implements SpriteEngine {

	private static final long serialVersionUID = 1L;

	/** The Renderer instance */
	private final SpriteRenderer renderer;

	/** The sprites */
	private Sprite[] sprites;

	/** The number of allocated sprites */
	private int numAllocated;

	/** The number of visible sprites */
	private int numVisible;

	/** The tick rate */
	private int tickRate;

	/** Base alpha */
	private float alpha = 1.0f;

	/** Sprite processor */
	private SpriteProcessor spriteProcessor;

	/** Handy thing that just records the total number of sprites allocated in the entire system */
	static int totalAllocated;

	/**
	 * @return the total number of sprites allocated in all StaticSpriteEngines
	 */
	public static int getTotalAllocated() {
		return totalAllocated;
	}

	/**
	 * C'tor
	 * @param sortY
	 * @param sortLayer
	 * @param uniqueSprites
	 * @param tickRate
	 */
	public StaticSpriteEngine(boolean sortY, int sortLayer, boolean uniqueSprites, int tickRate) {
		if (tickRate < 1) {
			throw new IllegalArgumentException("tickRate must be > 0");
		}

		this.sprites = new Sprite[1];
		this.tickRate = tickRate;

		for (int i = 0; i < sprites.length; i ++) {
			sprites[i] = new Sprite(this);
			sprites[i].index = i;
		}

		renderer = new DefaultSpriteRenderer(sortY, sortLayer, uniqueSprites);

		spriteProcessor = new SpriteProcessor() {
			@Override
			public void processRendering(Sprite[] sprite, int start, int end, SpriteRenderer renderer) {
				for (int i = start; i < end; i++) {
					Sprite s = sprites[i];
					if (s.isVisible() && s.isActive()) {
						renderer.render(s);
						numVisible++;
					}
				}
			}
		};
	}

	@Override
	public SpriteProcessor getSpriteProcessor() {
		return spriteProcessor;
	}

	@Override
	public void setSpriteProcessor(SpriteProcessor spriteProcessor) {
		this.spriteProcessor = spriteProcessor;
	}

	@Override
	protected void doCreate() {
		renderer.create();
	}

	@Override
	protected void doDestroy() {
		renderer.destroy();
	}


	/**
	 * Sets the tick rate
	 * @param newTickRate
	 */
	@Override
	public void setTickRate(int newTickRate) {
		tickRate = newTickRate;
	}

	/**
	 * Gets the tick rate
	 * @return the tick rate
	 */
	@Override
	public int getTickRate() {
		return tickRate;
	}

	/**
	 * Allocate and initialize sprite. If there are no sprites available an exception is thrown.
	 * The sprite returned will be visible and active, with no animation or image.
	 * @param owner The sprite's new owner, which is used to keep track of debugging
	 * @return a sprite
	 */
	@Override
	public Sprite allocateSprite(Serializable owner) {
		if (owner == null) {
			throw new NullPointerException("No owner specified");
		}

		totalAllocated ++;
		if (numAllocated == sprites.length) {
			// Grow
			Sprite[] old = sprites;
			sprites = new Sprite[numAllocated * 2];
			System.arraycopy(old, 0, sprites, 0, numAllocated);
		}
		Sprite s = sprites[numAllocated];
		if (s == null) {
			s = sprites[numAllocated] = new Sprite(this);
			s.index = numAllocated;
		}
		s.init(owner);
		numAllocated ++;
		return s;

	}

	public int maxSprites() {
		return sprites.length;
	}

	public int numAllocated() {
		return numAllocated;
	}

	public int numFree() {
		return sprites.length - numAllocated;
	}

	/**
	 * Deallocate a sprite. The sprite is returned to the sprite pool, and no matter what you do
	 * with it now, it won't do anything on the screen.
	 * @param sprite The sprite to return to the pool
	 */
	@Override
	public void deallocate(Sprite sprite) {
		totalAllocated --;
		sprites[sprite.index] = sprites[-- numAllocated];
		sprites[sprite.index].index = sprite.index;
		sprites[numAllocated] = sprite;
		sprite.index = numAllocated;
	}

	/**
	 * Remove all sprites
	 */
	@Override
	public void clear() {
		for (int i = numAllocated; --i >= 0; ) {
			sprites[i].deallocate();
		}
	}

	/**
	 * Tick all sprites.
	 */
	@Override
	public void tick() {
		final int n = numAllocated;
		for (int i = 0; i < n; i++) {
			Sprite s = sprites[i];
			if (s.isActive()) {
				s.tick();
			}
		}
	}

//	/**
//	 * Dump sprites
//	 */
//	public void dump() {
//		final int n = numAllocated;
//		System.err.println("Sprite dump:");
//		for (int i = 0; i < n; i++) {
//			System.err.println(sprites[i]);
//		}
//	}

	@Override
	public void render() {
		numVisible = 0;

		// Now render the sprites
		renderer.setAlpha(alpha);
		renderer.preRender();
		spriteProcessor.processRendering(sprites, 0, numAllocated, renderer);
		renderer.postRender();
	}

	/**
	 * @return Returns the alpha.
	 */
	@Override
	public float getAlpha() {
		return alpha;
	}

	/**
	 * Sets the base alpha for all the sprites.
	 * @param alpha The alpha to set.
	 */
	@Override
	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	@Override
	public SpriteRenderer getSpriteRenderer() {
		return renderer;
	}
}
