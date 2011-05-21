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
package com.shavenpuppy.jglib.util;

/**
 * Fixed-point math routines
 */
public final class FPMath {

	private static final float INV = 1.0f/65536.0f;
	private static final double INVD = 1.0/65536.0;

	/** Handy constants */
	public static final int SIXTEEN = FPMath.fpValue(16);
	public static final int EIGHT = FPMath.fpValue(8);
	public static final int FOUR = FPMath.fpValue(4);
	public static final int TWO = FPMath.fpValue(2);
	public static final int ONE = FPMath.fpValue(1);
	public static final int MINUSONE = FPMath.fpValue(-1);
	public static final int HALF = FPMath.fpValue(0.5);
	public static final int QUARTER = FPMath.fpValue(0.25);
	public static final int EIGHTH = FPMath.fpValue(0.125);
	public static final int SIXTEENTH = FPMath.fpValue(0.0625);
	public static final int THIRTYTWOTH = FPMath.fpValue(0.03125);
	public static final int PI = FPMath.fpValue(Math.PI);
	public static final int PIBY2 = FPMath.fpValue(Math.PI * 0.5);
	public static final int PITIMES2 = FPMath.fpValue(Math.PI * 2.0);

	/**
	 * Cosine table
	 */
	private static final int[] cos = new int[65536];
	static {
		for (int i = 0; i < 65536; i ++) {
	        cos[i] = fpValue(Math.cos(i * Math.PI / 32768.0));
        }
	}

	/** No constructor */
	private FPMath() {}

	/**
	 * Parse an FP value from a String. IF the string is terminated with the
	 * characters "fp" then they are ignored.
	 * @param in
	 * @return a fixed-point value
	 * @throws NumberFormatException
	 */
	public static int parse(String in) throws NumberFormatException {
		if (in.endsWith("fp")) {
			return fpValue(Float.parseFloat(in.substring(0, in.length() - 2)));
		} else {
			return fpValue(Float.parseFloat(in));
		}
	}

	/**
	 * Square root.
	 * @param v A value in 16:16 FP
	 * @return the square root of V in 16:16 FP
	 */
	public static final int sqrt(int len) {
		return FPMath.fpValue(Math.sqrt(FPMath.doubleValue(len)));
	}

	/**
	 * Square root.
	 * @param v A value in 16:16 FP stored in a long (top 32 bits ignored!)
	 * @return the square root of V in 16:16 FP
	 */
	public static final int sqrt(long len) {
		return FPMath.fpValue(Math.sqrt(FPMath.doubleValue(len)));
	}

	/**
	 * Multiply two fixed point math numbers together
	 * @param a
	 * @param b
	 * @return a fixed point value
	 */
	public static int mul(int a, int b) {

		long aa = a;
		long bb = b;
		long cc = aa * bb;
		return (int)(cc >> 16L);

		/*		long aa = ((long)a) & 0xFFFFL;
		long bb = ((long)b) & 0xFFFFL;

		long a8 = ((long)a) >> 8;
		long b8 = ((long)b) >> 8;

		return (int) ((a8) * (b8)
			   +((aa * (b8)) >> 8L)
			   +(((a8) * bb) >> 8L)
			   +((aa * bb) >> 16)); // Note: this line can be removed to sacrifice a little accuracy for more speed
			   */
	}

	/**
	 * Multiply two fixed point math numbers together
	 * returning the result in a long
	 * @param a
	 * @param b
	 * @return a fixed point value
	 */
	public static long lmul(int a, int b) {

		long aa = a;
		long bb = b;
		long cc = aa * bb;
		return cc >> 16L;
/*
		long aa = ((long)a) & 0xFFFFL;
		long bb = ((long)b) & 0xFFFFL;

		long a8 = ((long)a) >> 8;
		long b8 = ((long)b) >> 8;

		return 	(a8) * (b8)
			   +((aa * (b8)) >> 8L)
			   +(((a8) * bb) >> 8L)
			   +((aa * bb) >> 16); // Note: this line can be removed to sacrifice a little accuracy for more speed
*/
	}

	/**
	 * Divide a fixed point maths number
	 * @param num The numerator
	 * @param den The denominator
	 * @return a fixed point value
	 */
	public static int div(int num, int den) {
		if (den == 0) {
	        return 0;
        }
		else {
	        return (int)((((long)num << 32L) / den) >> 16L);
//		if (den == 0)
//			return 0;
//		else
//			return (num << 16) / den;
		//return fpValue(floatValue(num) / floatValue(den));
        }
	}

	/**
	 * Convert a fixed point number to an int
	 * @param fp
	 * @return int
	 */
	public static int intValue(int fp) {
		return fp >> 16;
	}

	/**
	 * Convert a fixed point number to an int
	 * @param fp
	 * @return int
	 */
	public static int intValue(long fp) {
		return (int)(fp >> 16);
	}

	/**
	 * Convert a float to a fixed point
	 * @param f
	 * @return fp
	 */
	public static int fpValue(float f) {
		return (int)(f * 65536.0f);
	}

	/**
	 * Convert a double to a fixed point
	 * @param f
	 * @return fp
	 */
	public static int fpValue(double f) {
		return (int)(f * 65536.0);
	}

	/**
	 * Convert a fixed point number to a float
	 * @param fp
	 * @return int
	 */
	public static float floatValue(int fp) {
		return fp * INV;
	}

	/**
	 * Convert a fixed point number to a double
	 * @param fp
	 * @return int
	 */
	public static double doubleValue(long fp) {
		return fp * INVD;
	}

	/**
	 * Convert an int to a fixed point value
	 * @param int
	 * @return fp
	 */
	public static int fpValue(int i) {
		return i << 16;
	}

	/**
	 * Cosines. This is very approximate cosine.
	 * @param theta The angle, in 16:16 Yakly Degrees
	 * @return cos(theta) in 16:16
	 */
	public static int cos(int theta) {
		return cos[theta & 0xFFFF];
	}

	/**
	 * Sines. This is very approximate sine.
	 * @param theta The angle, in 16:16 Yakly Degrees
	 * @return sin(theta) in 16:16
	 */
	public static int sin(int theta) {
		return cos[(theta - 16384) & 0xFFFF];
	}

	/**
	 * Convert an angle in floating point radians to 16:16 Yakly degrees
	 * @param theta The angle, in floating point radians
	 * @return The angle, in 16:16 Yakly degrees
	 */
	public static int fpYaklyDegrees(float theta) {
		int yakTheta = ((int)(theta * 32768.0f / Math.PI)) & 0xFFFF;
		return yakTheta;
	}

	/**
	 * Convert an angle in floating point radians to 16:16 Yakly degrees
	 * @param theta The angle, in floating point radians
	 * @return The angle, in 16:16 Yakly degrees
	 */
	public static int fpYaklyDegrees(double theta) {
		return ((int)(theta * 32768.0 / Math.PI)) & 0xFFFF;
	}


}
