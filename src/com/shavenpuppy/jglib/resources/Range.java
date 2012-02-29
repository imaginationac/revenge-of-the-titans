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
package com.shavenpuppy.jglib.resources;

import java.io.Serializable;

import com.shavenpuppy.jglib.util.Parseable;

/**
 * $Id: Range.java,v 1.6 2011/10/01 00:33:45 cix_foo Exp $
 * A linear range of floating point values.
 * @author $Author: cix_foo $
 * @version $Revision: 1.6 $
 */
public class Range implements Parseable, Serializable {

	private static final long serialVersionUID = 1L;

	/** Minimum value */
	private float min;

	/** Maximum value */
	private float max;

	/**
	 * C'tor
	 */
	public Range() {
	}

	/**
	 * C'tor
	 */
	public Range(float min, float max) {
		this.min = min;
		this.max = max;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.util.Parseable#fromString(java.lang.String)
	 */
	@Override
	public void fromString(String src) throws Exception {
		int idx = src.indexOf(',');
		if (idx == -1) {
			min = max = Float.parseFloat(src);
		} else {
			min = Float.parseFloat(src.substring(0, idx).trim());
			max = Float.parseFloat(src.substring(idx + 1, src.length()).trim());
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (min == max) {
			return String.valueOf(min);
		} else {
			return min+", "+max;
		}
	}

	/**
	 * Get a value from the range
	 * @return float, min <= getValue() <= max
	 */
	public float getValue() {
		double r = Math.random();
		return (float)((max - min) * r + min);
	}

	/**
	 * Accessor
	 * @return float
	 */
	public float getMax() {
		return max;
	}

	/**
	 * Accessor
	 * @return float
	 */
	public float getMin() {
		return min;
	}
}
