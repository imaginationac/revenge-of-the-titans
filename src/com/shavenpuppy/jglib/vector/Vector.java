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
package com.shavenpuppy.jglib.vector;

import java.io.Serializable;
import java.nio.IntBuffer;

import com.shavenpuppy.jglib.util.FPMath;

/**
 * Base class for integer vectors. Integer vectors are stored in 16:16 fixed point
 * format; eg. the bottom 16 bits is the fraction part.
 *
 * In general, coordinates should not wander beyond the range -32767 to +32767.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision: 1.10 $
 */
public abstract class Vector implements ReadableVector, WritableVector, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for Vector.
	 */
	public Vector() {
		super();
	}

	/**
	 * @return the length of the vector in 16-bit FP
	 */
	@Override
	public final int length() {
		return FPMath.sqrt(lengthSquared());
	}

	/**
	 * Normalise this vector. More or less.
	 */
	@Override
	public final void normalize() {
		int len = length();
		if (len != 0) {
			invscale(len);
		}
	}


    /**
     * Store this vector in an IntBuffer
     * @param buf The buffer to store it in, at the current position
     */
    @Override
	public abstract void store(IntBuffer buf);


	/**
	 * Scale this vector by a fraction
	 * @param scale The scale, in 16-bit FP
	 */
	@Override
	public abstract void scale(int scale);

	/**
	 * Scale this vector by an inverse fraction
	 * @param scale The scale, in 8-bit FP
	 */
	@Override
	public abstract void invscale(int scale);



}
