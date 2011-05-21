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
package worm.generator;

import com.shavenpuppy.jglib.interpolators.Interpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;

/**
 * A class for making noise on the fly
 */
class PerlinNoise {

	private final Interpolator interpolator; // Used for interpolated noise.

	// Perlin noise parameters
	private final float persistence;
	private final int octaves;
	private final int scale;
	private final int seed;

	public PerlinNoise(int seed, int scale, int octaves, float persistence) {
		this(seed, scale, octaves, persistence, LinearInterpolator.instance);
	}

	public PerlinNoise(
		int seed,
		int scale,
		int octaves,
		float persistence,
		Interpolator i)
	{
		this.seed = seed;
		this.scale = scale;
		this.octaves = octaves;
		this.persistence = persistence;
		interpolator = i;
	}

	/**
	 * Get the Perlin noise at a particular point.
	 * Creation date: (30/03/2001 01:26:21)
	 */
	public float fireNoise(int x, int y) {
		float total = 0;
		float amplitude = persistence;
		int frequency = 1;

		for (int i = 0; i < octaves; i++) {
			total += Math.abs(interpolatedNoise(x * frequency, y * frequency))
				* amplitude;
			amplitude /= 2.0f;
			frequency <<= 1;
		}
		return total;
	}
	/**
	 * Returns interpolated noise (i.e. noise using fractional coordinates).
	 */
	public float interpolatedNoise(int x, int y) {
		int iX = x / scale;
		float fX = x % scale / (float) scale;
		int iY = y / scale;
		float fY = y % scale / (float) scale;

		float v1 = noise(iX, iY);
		float v2 = noise(iX + 1, iY);
		float v3 = noise(iX, iY + 1);
		float v4 = noise(iX + 1, iY + 1);
		float i1 = interpolator.interpolate(v1, v2, fX);
		float i2 = interpolator.interpolate(v3, v4, fX);
		return interpolator.interpolate(i1, i2, fY);
	}
	/**
	 * Return the raw noise at a particular point
	 */
	public float noise(int x, int y) {
		int n = x + y * (57 + seed);
		n = n << 13 ^ n;
		return 1.0f
			- (n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff)
				/ 1073741824.0f;
	}
	/**
	 * Get the Perlin noise at a particular point.
	 * Creation date: (30/03/2001 01:26:21)
	 */
	public float perlinNoise(int x, int y) {
		float total = 0;
		float amplitude = 1.0f;
		int frequency = 1;

		for (int i = 0; i < octaves; i++) {
			total += interpolatedNoise(x * frequency, y * frequency)
				* amplitude;
			amplitude *= persistence;
			frequency <<= 1;
		}
		return total;
	}
	/**
	 * Returns interpolated noise (i.e. noise using fractional coordinates).
	 */
	public float smoothInterpolatedNoise(int x, int y) {
		int iX = x / scale;
		float fX = x % scale / (float) scale;
		int iY = y / scale;
		float fY = y % scale / (float) scale;

		float v1 = smoothNoise(iX, iY);
		float v2 = smoothNoise(iX + 1, iY);
		float v3 = smoothNoise(iX, iY + 1);
		float v4 = smoothNoise(iX + 1, iY + 1);
		float i1 = interpolator.interpolate(v1, v2, fX);
		float i2 = interpolator.interpolate(v3, v4, fX);
		return interpolator.interpolate(i1, i2, fY);
	}
	public float smoothNoise(int x, int y) {
		float corners =
			(noise(x - 1, y - 1)
				+ noise(x + 1, y - 1)
				+ noise(x - 1, y + 1)
				+ noise(x + 1, y + 1))
				/ 16.0f;
		float sides =
			(noise(x - 1, y)
				+ noise(x + 1, y)
				+ noise(x, y - 1)
				+ noise(x, y + 1))
				/ 8.0f;
		float center = noise(x, y) / 4.0f;
		return corners + sides + center;
	}
}
