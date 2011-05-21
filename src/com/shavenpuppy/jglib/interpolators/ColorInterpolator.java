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
package com.shavenpuppy.jglib.interpolators;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

/**
 * $Id: ColorInterpolator.java,v 1.5 2011/04/18 23:28:06 cix_foo Exp $
 * Interpolates between two colors into a third color.
 * @author $Author: cix_foo $
 * @version $Revision: 1.5 $
 */
public final class ColorInterpolator {

	/**
	 * No c'tor
	 */
	private ColorInterpolator() {
	}

	/**
	 * Interpolate between color A and color B using the specified interpolator,
	 * placing the resulting color in C.
	 * @param a, b
	 * @param ratio (0..1)
	 * @param interpolator
	 * @param c
	 * @return c
	 */
	public static Color interpolate(ReadableColor a, ReadableColor b, float ratio, Interpolator interpolator, Color c) {
		if (c == null) {
			c = new Color();
		}
		c.set(
				(byte)interpolator.interpolate(a.getRed(), b.getRed(), ratio),
				(byte)interpolator.interpolate(a.getGreen(), b.getGreen(), ratio),
				(byte)interpolator.interpolate(a.getBlue(), b.getBlue(), ratio),
				(byte)interpolator.interpolate(a.getAlpha(), b.getAlpha(), ratio)
				);
		return c;
	}

	/**
	 * Interpolate between color A and color B using the specified interpolator,
	 * placing the resulting color in C.
	 * @param a, b
	 * @param ratio (0fp..1fp)
	 * @param interpolator
	 * @param c
	 * @return TODO
	 */
	public static Color interpolate(ReadableColor a, ReadableColor b, int ratio, Interpolator interpolator, Color c) {
		if (c == null) {
			c = new Color();
		}
		c.set(
				(byte)interpolator.interpolate(a.getRed(), b.getRed(), ratio),
				(byte)interpolator.interpolate(a.getGreen(), b.getGreen(), ratio),
				(byte)interpolator.interpolate(a.getBlue(), b.getBlue(), ratio),
				(byte)interpolator.interpolate(a.getAlpha(), b.getAlpha(), ratio)
				);
		return c;
	}


}
