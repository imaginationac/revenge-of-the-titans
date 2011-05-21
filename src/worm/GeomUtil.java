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
package worm;


/**
 * Some geometry utils ripped out of SPGL-Extra.
 * @author Cas
 */
public final class GeomUtil {

	public static final int INSIDE = 0;
	public static final int SPANNING = 1;
	public static final int OUTSIDE = 2;

	public static final int CENTER_CENTER =	0x00000000;
	public static final int LEFT_CODE	=	0x00F00000;
	public static final int RIGHT_CODE	=	0x000F0000;
	public static final int TOP_CODE	=	0x0000F000;
	public static final int BOTTOM_CODE	=	0x00000F00;

	/**
	 * No c'tor
	 */
	private GeomUtil() {
	}

	public static boolean rectangleContainsCircle(float x, float y, float radius, float rx, float ry, float width, float height) {
		// Treat the circle like an AABB and see if it lies within the bounds
		float halfSize = radius / 2f;

		float boundsXMax = rx + width;
		float boundsYMax = ry + height;

		// Left edge
		if (x - halfSize < rx) {
			return false;
		}

		// Right edge
		if (x + halfSize > boundsXMax) {
			return false;
		}

		// Top edge
		if (y - halfSize < boundsYMax) {
			return false;
		}

		if (y + halfSize > ry) {
			return false;
		}

		// All edges ok, circle is within bounding rect
		return true;
	}

	public static int classifyCircle(float x, float y, float radius, float rx, float ry, float width, float height) {
		// Totally inside?
		if (rectangleContainsCircle(x, y, radius, rx, ry, width, height)) {
			return INSIDE;
		}

		// Totally outside?
		int region = findRegionForCircle(x, y, radius, rx, ry, width, height);
		if (region != CENTER_CENTER) {
			return OUTSIDE;
		}

		return SPANNING;
	}

	/** Finds a region code for a circle against this rectangle's bounds
	 *  Outer region codes are returned if the circle is totally within the region.
	 *  Otherwise we return CENTER_CENTER.
	 *
	 *  NB: This means that CENTER_CENTER could indicate total containment or spanning an edge.
	 */
	public static int findRegionForCircle(float x, float y, float radius, float rx, float ry, float width, float height) {

		int region = CENTER_CENTER;

		if (x + radius < rx) {
			region |= LEFT_CODE;
		} else if (x - radius > rx + width) {
			region |= RIGHT_CODE;
		}

		if (y + radius < ry) {
			region |= BOTTOM_CODE;
		} else if (y - radius > ry + height) {
			region |= TOP_CODE;
		}

		return region;
	}

	public static boolean pointInCircle(float x, float y, float radius, float px, float py) {
		float dx = px - x;
		float dy = py - y;
		return Math.sqrt(dx * dx + dy * dy) < radius;
	}

	public static boolean circleRectCollision(float x, float y, float radius, float rx, float ry, float width, float height) {
		// 1. Corners in circle?
		if (pointInCircle(x, y, radius, rx, ry)) {
			return true;
		}
		if (pointInCircle(x, y, radius, rx + width, ry)) {
			return true;
		}
		if (pointInCircle(x, y, radius, rx + width, ry + height)) {
			return true;
		}
		if (pointInCircle(x, y, radius, rx, ry + height)) {
			return true;
		}

		// 2. Edge in circle?
		if (x >= rx && x < rx + width) {
			if (Math.abs(ry - y) < radius) {
				return true;
			}
			if (Math.abs(y - (ry + height)) < radius) {
				return true;
			}
		} else if (y >= ry && y < ry + height) {
			if (Math.abs(rx - x) < radius) {
				return true;
			}
			if (Math.abs(x - (rx + width)) < radius) {
				return true;
			}
		}

		// 3. Wholly contained?
		return x >= rx + radius && y >= ry + radius && x < rx + width - radius && y < ry + height - radius;

	}


}
