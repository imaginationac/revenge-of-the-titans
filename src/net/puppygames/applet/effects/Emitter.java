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
package net.puppygames.applet.effects;

/**
 * $Id: Emitter.java,v 1.2 2009/08/01 14:49:44 foo Exp $
 * A particle emitter. Implemented in EmitterFeature.
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public abstract class Emitter extends Effect {

	protected boolean floorSet, ceilingSet, leftWallSet, rightWallSet;
	protected float floor, ceiling, leftWall, rightWall;
	protected float angle, gain = 1.0f;

	/**
	 * C'tor
	 */
	public Emitter() {
	}

	/**
	 * @return the tag name for the particle; or null if none (for debugging)
	 */
	public abstract String getTag();

	/**
	 * Set the location of the emitter relative to its parent
	 * @param x
	 * @param y
	 */
	public abstract void setLocation(float x, float y);

	/**
	 * Set the Y offset of the emitter
	 */
	public abstract void setYOffset(float yOffset);

	public void setCeiling(float ceiling) {
		this.ceiling = ceiling;
		ceilingSet = true;
	}

	public void setFloor(float floor) {
		this.floor = floor;
		floorSet = true;
	}

	public void setLeftWall(float leftWall) {
		this.leftWall = leftWall;
		leftWallSet = true;
	}

	public void setRightWall(float rightWall) {
		this.rightWall = rightWall;
		rightWallSet = true;
	}

	/**
	 * Sets the angle by which all the particles will be rotated, and the emission angle too.
	 * @param angle the angle to set
	 */
	public void setAngle(float angle) {
		this.angle = angle;
	}

	/**
	 * Sets the gain of the sound effect, if there is one
	 * @param gain
	 */
	public abstract void setGain(float gain);

	@Override
	protected final void render() {
		// Don't actually need to do any rendering!
	}
}
