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
package worm.entities;

import java.io.Serializable;

import org.lwjgl.util.Rectangle;

/**
 * For implementing the movement of something
 */
public interface Movement extends Serializable {

	static final float MAX_SPEED_MULTIPLIER = 3.0f;

	/**
	 * Remove, does cleanup
	 */
	void remove();

	/**
	 * Tick, called every frame
	 */
	void tick();

	/**
	 * No longer looking for our original target
	 */
	void reset();

	/**
	 * Maybe rethink the route if it intersects with the specified Rectangle.
	 * @param bounds May be null - forces a rethink
	 */
	void maybeRethink(Rectangle bounds);

	/**
	 * Adjust movement to begin from the specified location (ie. a gidrah's been knocked back, and this is where it ended up
	 * when it stopped sliding)
	 * @param newX
	 * @param newY
	 */
	void adjust(float newX, float newY);

	/**
	 * @return true if we're on the move
	 */
	boolean isMoving();

	void attack();

	void dontAttack();

}