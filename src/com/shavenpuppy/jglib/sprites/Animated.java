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

import com.shavenpuppy.jglib.resources.ResourceArray;


/**
 * Animated things must implement this interface at a bare minimum.
 */
public interface Animated {

	/**
	 * Reset the animation state of this object
	 */
	void reset();

	/**
	 * Rewind the animation to the beginning
	 */
	void rewind();

	/**
	 * Pause the animation
	 * @param paused
	 */
	void setPaused(boolean paused);

	/**
	 * Is the animation paused?
	 * @return boolean
	 */
	boolean isPaused();

	/**
	 * Sets the current loop counter
	 * @param i
	 */
	void setLoop(int i);

	/**
	 * Get the current loop counter
	 * @return the current loop counter
	 */
	int getLoop();

	/**
	 * Add a value to the loop counter
	 * @param d
	 */
	void addLoop(int d);

	/**
	 * Sets the current "event" state
	 * @param id The event ID
	 */
	void setEvent(int id);

	/**
	 * @return the current "event" state
	 */
	int getEvent();

	/**
	 * Sets the current animation. The new animation is {@link #rewind()ed}, meaning it gets reset and ticked once.
	 * @param animation The new animation to use
	 */
	void setAnimation(Animation animation);

	/**
	 * Gets the current animation
	 * @return an Animation, or null
	 */
	Animation getAnimation();

	/**
	 * Sets the current sequence number
	 * @param seq The new sequence number
	 */
	void setSequence(int seq);

	/**
	 * Sets the current tick
	 * @param tick The new tick
	 */
	void setTick(int tick);

	/**
	 * Deactivate the target. It will no longer animate.
	 */
	void deactivate();

	/**
	 * @return the current sequence
	 */
	int getSequence();

	/**
	 * @return the current tick
	 */
	int getTick();

	/**
	 * Fires an "event" to the Animated thing
	 * @param event The event "id"
	 */
	void eventReceived(int event);

	/**
	 * Tick. Call this every frame to animate the animated thing.
	 */
	void tick();

	/**
	 * @return the current childXOffset
	 */
	float getChildXOffset();

	/**
	 * Sets the current childXOffset - for positioning additional sprites or emitters to a point in a sprite anim
	 * @param childXOffset The childXOffset
	 */
	void setChildXOffset(float childXOffset);

	/**
	 * @return the current childYOffset
	 */
	float getChildYOffset();

	/**
	 * Sets the current childYOffset - for positioning additional sprites or emitters to a point in a sprite anim
	 * @param childYOffset The childYOffset
	 */
	void setChildYOffset(float childYOffset);

	/**
	 * Push the current animation and sequence number onto a stack
	 */
	void pushSequence();

	/**
	 * Pop the current sequence number and animation from the stack
	 */
	void popSequence();

	/**
	 * Sets the {@link FrameList} to use.
	 * @param frameList the frameList, or null to clear
	 */
	void setFrameList(ResourceArray frameList);

	/**
	 * @return the {@link FrameList} in use, or null if none is in use currently
	 */
	ResourceArray getFrameList();

	int getFrame();

	boolean setFrame(int newFrame);
}