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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Stack;

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.vector.Vector2f;

/**
 * $Id: SerializedSprite.java,v 1.21 2011/10/04 00:05:22 cix_foo Exp $
 * For persisting sprites
 * @author $Author: cix_foo $
 * @version $Revision: 1.21 $
 */
public class SerializedSprite implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Sprite engine we should be allocated to */
	private SpriteAllocator spriteAllocator;

	/** The animation, if any */
	private Animation animation;

	/** Current image, if any */
	private SpriteImage image;

	/** Current frame tick */
	private int tick;

	/** Animation sequence */
	private int sequence;

	/** Loop */
	private int loop;

	/** Layer */
	private int layer;

	/** Sublayer */
	private int subLayer;

	/** World coordinates */
	private Vector2f location = new Vector2f();

	/** Offset coordinates */
	private Vector2f offset = new Vector2f();

	/** Flash mode: renders an additive mode version on top */
	private boolean flash;

	/** Colours of the four corners (bottom left, bottom right, top right, top left) */
	private final ReadableColor[] color = new ReadableColor[4];

	/** Sprite scale */
	private int xscale, yscale;

	/** Sprite alpha 0..255 */
	private int alpha;

	/** Rotation angle */
	private int angle;

	/** Sprite visibility */
	private boolean visible;

	/** Sprite active flag */
	private boolean active;

	/** The current "event" state */
	private int event;

	/** Pause animation */
	private boolean paused;

	/** Mirrored */
	private boolean mirrored;

	/** Flipped */
	private boolean flipped;

	/** The current child x and y offsets */
	private float childXOffset, childYOffset;

	/** doChildOffset */
	private boolean doChildOffset;

	/** Y-sort offset */
	private float ySortOffset;

	/** Stack */
	private Stack<Sprite.StackEntry> stack;

	/** Owner */
	private Serializable owner;

	/**
	 * C'tor
	 */
	public SerializedSprite() {
	}

	/**
	 * Initialise from a sprite
	 * @param source The sprite we want to serialize
	 */
	public void fromSprite(Sprite source) {
		spriteAllocator = source.getAllocator();
		animation = source.getAnimation();
		image = source.getImage();
		loop = source.getLoop();
		tick = source.getTick();
		sequence = source.getSequence();
		layer = source.getLayer();
		subLayer = source.getSubLayer();
		source.getLocation(location);
		source.getOffset(offset);
		flash = source.isFlashing();
		color[0] = source.getColor(0);
		color[1] = source.getColor(1);
		color[2] = source.getColor(2);
		color[3] = source.getColor(3);
		xscale = source.getXScale();
		yscale = source.getYScale();
		alpha = source.getAlpha();
		angle = source.getAngle();
		visible = source.isVisible();
		active = source.isActive();
		event = source.getEvent();
		paused = source.isPaused();
		mirrored = source.isMirrored();
		flipped = source.isFlipped();
		childXOffset = source.getChildXOffset();
		childYOffset = source.getChildYOffset();
		doChildOffset = source.isDoChildOffset();
		stack = source.getStack();
		ySortOffset = source.getYSortOffset();
		owner = source.getOwner();
	}

	/**
	 * Init a sprite from this serialized sprite
	 * @param dest
	 * @throws Exception if the sprite can't be recreated
	 */
	public void toSprite(Sprite dest) throws Exception {
		if (!dest.isAllocated()) {
			throw new Exception("Sprite is not allocated!");
		}
		dest.setAnimationNoRewind(animation);
		dest.setImage(image);
		dest.setLoop(loop);
		dest.setTick(tick);
		dest.setSequence(sequence);
		dest.setLayer(layer);
		dest.setSubLayer(subLayer);
		dest.setLocation(location.getX(), location.getY());
		dest.setOffset(offset);
		dest.setFlash(flash);
		dest.setColor(0, color[0]);
		dest.setColor(1, color[1]);
		dest.setColor(2, color[2]);
		dest.setColor(3, color[3]);
		dest.setScale(xscale, yscale);
		dest.setAlpha(alpha);
		dest.setAngle(angle);
		dest.setVisible(visible);
		dest.setActive(active);
		dest.setEvent(event);
		dest.setPaused(paused);
		dest.setMirrored(mirrored);
		dest.setFlipped(flipped);
		dest.setChildXOffset(childXOffset);
		dest.setChildYOffset(childYOffset);
		dest.setDoChildOffset(doChildOffset);
		dest.setStack(stack);
		dest.setYSortOffset(ySortOffset);
	}

	/**
	 * When deserializing, we allocate a Sprite from the currently specified
	 * sprite engine and return that, instead. Remember to set a sprite engine!!
	 * @returns a Sprite
	 */
	private Object readResolve() throws ObjectStreamException {
		Sprite sprite = spriteAllocator.allocateSprite(owner);
		sprite.setAllocator(spriteAllocator);
		try {
			toSprite(sprite);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new InvalidObjectException("Failed to deserialize sprite due to "+e);
		}
		return sprite;
	}
}
