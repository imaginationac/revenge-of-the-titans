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

import com.shavenpuppy.jglib.resources.ResourceArray;

/**
 * A Sprite has associated with it an Animation and frame counter OR a single SpriteImage, and has a position
 * and offset.
 */
public abstract class AbstractAnimated implements Animated, Serializable {

	private static final long serialVersionUID = 1L;

	/** The Animation if any */
	private Animation animation;

	/** Framelist, if any */
	private ResourceArray frameList;

	/** Index into framelist */
	private int frame;

	/** Loop counter */
	private int loop;

	/** Current frame tick */
	private int tick;

	/** Animation sequence */
	private int sequence;

	/** The current "event" state */
	private int event;

	/** Pause animation */
	private boolean paused;

	/** Current child x and y offsets */
	private float childXOffset, childYOffset;

	/**
	 * C'tor
	 */
	public AbstractAnimated() {
	}

	@Override
	public void reset() {
		animation = null;
		frameList = null;
		sequence = 0;
		frame = 0;
		tick = 0;
		event = 0;
		paused = false;
		childXOffset = 0;
		childYOffset = 0;
	}

	/**
	 * Gets the animation.
	 * @return Returns a Animation
	 */
	@Override
	public Animation getAnimation() {
		return animation;
	}

	/**
	 * Sets the animation.
	 * @param animation The animation to set
	 */
	@Override
	public void setAnimation(Animation animation) {
		if (animation != null) {
			assert animation.isCreated();
		}
		this.animation = animation;
		rewind();
	}

	/**
	 * Sets the animation, without rewinding
	 * @param animation
	 */
	void setAnimationNoRewind(Animation animation) {
		this.animation = animation;
		tick = -1;
	}

	/**
	 * Rewind the animation
	 */
	@Override
	public void rewind() {
		sequence = 0;
		tick = -1;
		tick();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animation.Animated#getCurrentSequence()
	 */
	@Override
	public int getSequence() {
		return sequence;
	}
	/**
	 * @see com.shavenpuppy.jglib.sprites.Animation.Animated#getCurrentTick()
	 */
	@Override
	public int getTick() {
		return tick;
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animation.Animated#eventReceived(int)
	 */
	@Override
	public void eventReceived(int event) {
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#setCurrentSequence(int)
	 */
	@Override
	public void setSequence(int newSeq) {
		sequence = newSeq;
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Animated#setCurrentTick(int)
	 */
	@Override
	public void setTick(int newTick) {
		tick = newTick;
	}

	/**
	 * @return int
	 */
	@Override
	public int getEvent() {
		return event;
	}

	/**
	 * Sets the event.
	 * @param event The event to set
	 */
	@Override
	public void setEvent(int event) {
		this.event = event;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AbstractAnimated[animation="+animation+", seq="+sequence+", tick="+tick+", event="+event+"]";
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#isPaused()
	 */
	@Override
	public final boolean isPaused() {
		return paused;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#setPaused(boolean)
	 */
	@Override
	public final void setPaused(boolean paused) {
		this.paused = paused;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#addLoop(int)
	 */
	@Override
	public final void addLoop(int d) {
		loop += d;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.sprites.Animated#getLoop()
	 */
	@Override
	public final int getLoop() {
		return loop;
	}

	@Override
	public final void setLoop(int i) {
		loop = i;
	}

	@Override
	public float getChildXOffset(){
		return childXOffset;
	}

	@Override
	public void setChildXOffset(float childXOffset) {
		this.childXOffset = childXOffset;
	}

	@Override
	public float getChildYOffset(){
		return childYOffset;
	}

	@Override
	public void setChildYOffset(float childYOffset) {
		this.childYOffset = childYOffset;
	}

	@Override
	public void setFrameList(ResourceArray frameList) {
		this.frameList = frameList;
		if (frameList != null) {
			updateFrame();
		}
	}

	@Override
	public ResourceArray getFrameList() {
		return frameList;
	}

	/**
	 * @return the frame
	 */
	@Override
	public int getFrame() {
		return frame;
	}

	/**
	 * @param frame the frame to set
	 */
	@Override
	public boolean setFrame(int frame) {
		this.frame = frame;
		if (frameList != null) {
			return updateFrame();
		} else {
			return false;
		}
	}

	private boolean updateFrame() {
		if (frame < 0 || frame >= frameList.getNumResources()) {
			return false;
		}
		AnimatedAppearance newAppearance = (AnimatedAppearance) frameList.getResource(frame);
		if ((animation != null && newAppearance != animation) || animation == null) {
			return newAppearance.toAnimated(this);
		} else {
			return false;
		}

	}
}
