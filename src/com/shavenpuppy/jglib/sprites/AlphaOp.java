package com.shavenpuppy.jglib.sprites;

import java.nio.FloatBuffer;

import org.lwjgl.util.ReadableColor;

/**
 * What to do with the alpha to the colours of a sprite
 */
public enum AlphaOp {

	PREMULTIPLY {
		@Override
        protected int calc(ReadableColor c, int alpha) {
			float alpha00 = c.getAlpha() * alpha * PREMULT_ALPHA;
			float preMultAlpha00 = alpha00 * PREMULT_ALPHA;
			return ((int)(c.getRed() * preMultAlpha00) << 0) | ((int)(c.getGreen() * preMultAlpha00) << 8) | ((int)(c.getBlue() * preMultAlpha00) << 16) | (int) alpha00 << 24;
		}
	},
	KEEP {
		@Override
        protected int calc(ReadableColor c, int alpha) {
			return (c.getRed() << 0) | (c.getGreen() << 8) | (c.getBlue() << 16) | ((c.getAlpha() * alpha) << 24);
		}
	},
	ZERO {
		@Override
        protected int calc(ReadableColor c, int alpha) {
			float alpha00 = c.getAlpha() * alpha * PREMULT_ALPHA;
			float preMultAlpha00 = alpha00 * PREMULT_ALPHA;
			return ((int)(c.getRed() * preMultAlpha00) << 0) | ((int)(c.getGreen() * preMultAlpha00) << 8) | ((int)(c.getBlue() * preMultAlpha00) << 16);
		}
	};

	private static final float PREMULT_ALPHA = 1.0f / 255.0f;

	public final void op(ReadableColor c, int alpha, FloatBuffer dest) {
		dest.put(Float.intBitsToFloat(calc(c, alpha)));
	}
	public final void op(ReadableColor c, int alpha, SimpleRenderer renderer) {
		renderer.glColori(calc(c, alpha));
	}

	protected abstract int calc(ReadableColor c, int alpha);
}
