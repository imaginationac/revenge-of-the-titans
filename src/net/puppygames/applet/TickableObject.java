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
package net.puppygames.applet;


import com.shavenpuppy.jglib.sprites.GeometryStyle;
import com.shavenpuppy.jglib.sprites.Sprite;

/**
 * A TickableObject lives on a {@link Screen}, and renders itself in its own peculiar way. They are expensive to spawn and remove
 * so don't do so too often.
 */
public abstract class TickableObject extends GeometryStyle implements Tickable {

	private boolean done, visible = true;
	private int layer;
	private Sprite sprite;
	private Screen screen;

	public TickableObject() {
		super();
	}

	/**
	 * @param maxVertices
	 */
	public TickableObject(int maxVertices) {
		super();
	}

	@Override
	public final boolean isActive() {
		return !done;
	}

	@Override
	public final void remove() {
		if (!done) {
			done = true;
			if (sprite != null) {
				sprite.deallocate();
				sprite = null;
			}
			doRemove();
			screen = null;
		}
	}
	protected void doRemove() {
	}

	@Override
	public final void spawn(Screen screen) {
		assert !done;
		this.screen = screen;
		screen.addTickable(this);
		sprite = screen.allocateSprite(this);
		sprite.setStyle(this);
		sprite.setLayer(layer);
		sprite.setVisible(visible);
		doSpawn();
	}
	protected void doSpawn() {
	}

	/**
	 * @return the screen upon which we were spawned, or null, if we have been removed.
	 * @see #spawn(Screen)
	 */
	public final Screen getScreen() {
	    return screen;
    }

	public final void setLayer(int layer) {
		this.layer = layer;
		if (sprite != null) {
			sprite.setLayer(layer);
		}
	}
	public final int getLayer() {
		return layer;
	}

	@Override
	public void tick() {
	}

	@Override
	public void update() {
	}

	public final void setVisible(boolean visible) {
		this.visible = visible;
		if (sprite != null) {
			sprite.setVisible(visible);
		}
		onSetVisible();
	}

	protected void onSetVisible() {
	}

	public final boolean isVisible() {
		return visible;
	}

}
