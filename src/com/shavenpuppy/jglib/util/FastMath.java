package com.shavenpuppy.jglib.util;

/**
 * Fast maths! Courtesy of Riven.
 */
public class FastMath {
//	private static final float RAD, DEG;
	public static final float PI = (float) Math.PI;
	public static final float TAU = PI * 2.0f;

	private static final int SIN_BITS, SIN_MASK, SIN_COUNT;
	private static final float radToIndex;
	private static final float degToIndex;
	private static final float[] sin, cos;

	static {

		SIN_BITS = 12;
		SIN_MASK = ~(-1 << SIN_BITS);
		SIN_COUNT = SIN_MASK + 1;

		radToIndex = SIN_COUNT / TAU;
		degToIndex = SIN_COUNT / 360.0f;

		sin = new float[SIN_COUNT];
		cos = new float[SIN_COUNT];

		System.out.println(SIN_COUNT);

		for (int i = 0; i < SIN_COUNT; i++) {
			sin[i] = (float) Math.sin((i + 0.5f) / SIN_COUNT * TAU);
			cos[i] = (float) Math.cos((i + 0.5f) / SIN_COUNT * TAU);
		}
	}

	public static final float sin(float rad) {
		return sin[(int) (rad * radToIndex) & SIN_MASK];
	}

	public static final float cos(float rad) {
		return cos[(int) (rad * radToIndex) & SIN_MASK];
	}

	public static final float sinDeg(float deg) {
		return sin[(int) (deg * degToIndex) & SIN_MASK];
	}

	public static final float cosDeg(float deg) {
		return cos[(int) (deg * degToIndex) & SIN_MASK];
	}


}
