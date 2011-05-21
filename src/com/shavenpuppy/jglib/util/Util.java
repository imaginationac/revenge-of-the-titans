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

import java.util.*;

import org.lwjgl.opengl.Display;

/**
 * $Id: Util.java,v 1.13 2011/04/18 23:28:06 cix_foo Exp $
 *
 * Static utilities.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @version $Revision: 1.13 $
 */
public class Util {

	private static final Random random = new Random();

	/**
	 * NO constructor for Util.
	 */
	private Util() {
		super();
	}

	/**
	 * Returns the nearest power of 2, which is either n if n is already
	 * a power of 2, or the next higher number than n which is a power of 2.
	 */
	public static int nextPowerOf2(int n) {
		int x = 1;

		while (x < n) {
			x <<= 1;
		}

		return x;
	}

	/**
	 * Returns a random number
	 */
	public static int random(int min, int max) {
//		return ((int)(Math.random() * (1 + max - min))) + min;
		if (max - min == 0) {
			return min;
		}

		int rmin = Math.min(max, min);
		int rmax = Math.max(max, min);

		return random.nextInt(1 + rmax - rmin) + rmin;
//		return ((int)(random.nextFloat() * (1 + max - min))) + min;

	}

	public static float random() {
		return random.nextFloat();
	}

	/**
	 * Sets the seed for the random number generator
	 * @param seed The new seed
	 */
	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

	/**
	 * Create a consistent seed from the machine.
	 */
	@SuppressWarnings("rawtypes")
	public static long getMachineSeed() {
		long ret = 0L;
		int lsb = 0, msb = 0;
		// Get the hashcode of all the System properties
		Properties props = System.getProperties();
		for (Iterator i = props.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			lsb ^= entry.getKey().hashCode();
			lsb ^= entry.getValue().hashCode();
		}
		// Get hashcode of some other bits and bobs
		String adapter = Display.getAdapter();
		String version = Display.getVersion();
		msb ^= adapter != null ? adapter.hashCode() : 0;
		msb ^= version != null ? version.hashCode() : 0;
		ret = lsb | ((long)msb << 32L);
		return ret;
	}

	public static float angleFromDirection(float dirX, float dirY)
	{
		if (dirX == 0f) {
			return 0f;
		}

		float inv = dirY / dirX;
		float ang = (float)Math.atan(inv);

		// Extra half rotation if we're past 180
		if (dirX < 0) {
			ang += (float)Math.PI;
		}

		// Offset so that angle of 0 is straight up
		ang -= (float)Math.PI * 0.5f;

		return ang;
	}

	/** Does a spherical linear interpolation of two angles (in rads)
	 *  Works for any angles ie. doesn't have to be in [0, 2PI] range.
	 *  Will correctly handle wrapping around from 2PI -> 0
	 */
	public static float slerpAngle(float angle1, float angle2, float weight)
	{
		assert (weight >= 0f && weight <= 1f);

		float oneMinus = 1f - weight;

		if (Math.abs(angle1 - angle2) > Math.PI)
		{
			float a = angle1;
			float b = angle2;
			float twoPi = (float)Math.PI * 2f;

			if (weight < 0.5f)
			{
				if (a > b) {
					b += twoPi;
				} else {
					b -= twoPi;
				}
			}
			else
			{
				if (b > a) {
					a += twoPi;
				} else {
					a -= twoPi;
				}
			}
			return a*weight + b*oneMinus;
		}
		else
		{
			return angle1*weight + angle2*oneMinus;
		}
	}

	/**
	 * Turn a thing that is currently facing at <code>currentAngle</code> to face
	 * <code>targetAngle</code> at a maximum rate of <code>rate</code> degrees. The
	 * shortest turn is performed.
	 * @param currentAngle Current angle, in Yakly degrees
	 * @param targetAngle Target angle, in Yakly degrees
	 * @param rate The rate of turn
	 * @return the new angle
	 */
	public static int moveToAngle(int currentAngle, int targetAngle, int rate) {
		int newAngle;
		int diff = Math.abs(targetAngle - currentAngle);
		if (diff < rate) {
			newAngle = targetAngle;
		} else if (diff > 32768) {
			// Go the other way
			if (targetAngle < currentAngle) {
				newAngle = currentAngle + rate;
			} else {
				newAngle = currentAngle - rate;
			}
		} else {
			if (targetAngle < currentAngle) {
				newAngle = currentAngle - rate;
			} else {
				newAngle = currentAngle + rate;
			}
		}
		while (newAngle < 0) {
			newAngle += 65536;
		}
		return newAngle & 0xFFFF;
	}

	public static int getAngleDifference(int currentAngle, int targetAngle) {
		currentAngle &= 0xFFFF;
		while (currentAngle < 0) {
			currentAngle += 65536;
		}
		targetAngle &= 0xFFFF;
		while (targetAngle < 0) {
			targetAngle += 65536;
		}
		int diff = Math.abs(targetAngle - currentAngle);
		if (diff <= 32768) {
			return diff;
		} else {
			if (targetAngle < currentAngle) {
				return targetAngle + 65536 - currentAngle;
			} else {
				return currentAngle + 65536 - targetAngle;
			}
		}
	}

	public static double getAngleDifference(double currentAngle, double targetAngle) {
		while (currentAngle >= Math.PI * 2.0) {
			currentAngle -= Math.PI * 2.0;
		}
		while (targetAngle >= Math.PI * 2.0) {
			targetAngle -= Math.PI * 2.0;
		}
		while (currentAngle < 0.0) {
			currentAngle += Math.PI * 2.0;
		}
		while (targetAngle < 0.0) {
			targetAngle += Math.PI * 2.0;
		}
		double diff = Math.abs(targetAngle - currentAngle);
		if (diff <= Math.PI) {
			return diff;
		} else {
			if (targetAngle < currentAngle) {
				return targetAngle + Math.PI * 2.0 - currentAngle;
			} else {
				return currentAngle + Math.PI * 2.0 - targetAngle;
			}
		}
	}

	/**
	 * Turn a thing that is currently facing at <code>currentAngle</code> to face
	 * <code>targetAngle</code> at a maximum rate of <code>rate</code> degrees. The
	 * shortest turn is performed.
	 * @param currentAngle Current angle, in radians
	 * @param targetAngle Target angle, in radians
	 * @param rate The rate of turn
	 * @return the new angle, in radians
	 */
	public static double moveToAngle(double currentAngle, double targetAngle, double rate) {
		double newAngle;
		double diff = Math.abs(targetAngle - currentAngle);
		if (diff < rate) {
			newAngle = targetAngle;
		} else if (diff > Math.PI) {
			// Go the other way
			if (targetAngle < currentAngle) {
				newAngle = currentAngle + rate;
			} else {
				newAngle = currentAngle - rate;
			}
		} else {
			if (targetAngle < currentAngle) {
				newAngle = currentAngle - rate;
			} else {
				newAngle = currentAngle + rate;
			}
		}
		while (newAngle < 0.0) {
			newAngle += Math.PI * 2.0;
		}
		while (newAngle > Math.PI * 2.0) {
			newAngle -= Math.PI * 2.0;
		}
		return newAngle;
	}

	/**
	 * Get the shortest distance from a line segment to a point
	 * @param x1
	 * @param y1
	 * @param z1
	 * @param x2
	 * @param y2
	 * @param z3
	 * @param px
	 * @param py
	 * @param pz
	 * @return the distance, which will be >= 0.0; or -1.0, if the point is beyond the segment's ends
	 */
	public static double distanceFromLineToPoint(double x1, double y1, double z1, double x2, double y2, double z2, double px, double py, double pz) {
	    double ddx = x2 - x1;
		double ddy = y2 - y1;
		double ddz = z2 - z1;
		double lineMag = ddx * ddx + ddy * ddy + ddz * ddz;

		double u = (((px - x1) * ddx) + ((py - y1) * ddy) + ((pz - z1) * ddz)) / lineMag;

		if (u < 0.0f || u > 1.0f) {
			return -1.0; // closest point does not fall within the line segment
		}

		double ix = x1 + u * (x2 - x1);
		double iy = y1 + u * (y2 - y1);
		double iz = z1 + u * (z2 - z1);

		ddx = ix - px;
		ddy = iy - py;
		ddz = iz - pz;
		return Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
	}

	public static double distanceFromLineToPoint(double x1, double y1, double x2, double y2, double px, double py) {
		return distanceFromLineToPoint(x1, y1, 0.0, x2, y2, 0.0, px, py, 0.0);
	}

}