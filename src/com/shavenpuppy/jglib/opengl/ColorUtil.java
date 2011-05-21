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
package com.shavenpuppy.jglib.opengl;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

import com.shavenpuppy.jglib.sprites.SimpleRenderer;

/**
 * GL colour utility
 *
 * @author cas
 */
public final class ColorUtil {

	/**
	 * No construction
	 */
	private ColorUtil() {}

	/**
	 * Set a GL color
	 * @param color
	 */
	public static void setGLColor(ReadableColor color) {
		setGLColor(color, SimpleRenderer.GL_RENDERER);
	}

	/**
	 * Set a GL color
	 * @param color
	 */
	public static void setGLColor(ReadableColor color, SimpleRenderer renderer) {
		renderer.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
	}


	/**
	 * Set a GL color
	 * @param color
	 * @param alpha
	 */
	public static void setGLColor(ReadableColor color, int alpha) {
		setGLColor(color, alpha, SimpleRenderer.GL_RENDERER);
	}

	/**
	 * Set a GL color
	 * @param color
	 * @param alpha
	 */
	public static void setGLColor(ReadableColor color, int alpha, SimpleRenderer renderer) {
		renderer.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), (byte) (color.getAlpha() * alpha >> 8));
	}

	/**
	 * Set a GL color, modulated by another color
	 * @param color1
	 * @param color2
	 */
	public static void setGLColor(ReadableColor color1, ReadableColor color2) {
		setGLColor(color1, color2, SimpleRenderer.GL_RENDERER);
	}

	/**
	 * Set a GL color, modulated by another color
	 * @param color1
	 * @param color2
	 */
	public static void setGLColor(ReadableColor color1, ReadableColor color2, SimpleRenderer renderer) {
		byte red = (byte) ((color1.getRed() * color2.getRed()) / 255);
		byte green = (byte) ((color1.getGreen() * color2.getGreen()) / 255);
		byte blue = (byte) ((color1.getBlue() * color2.getBlue()) / 255);
		byte alpha = (byte) ((color1.getAlpha() * color2.getAlpha()) / 255);
		renderer.glColor4ub(red, green, blue, alpha);
	}

	/**
	 * Blend two colors together and return the result in a destination color.
	 * @param color1
	 * @param color2
	 * @param dest If null, a new color is constructed.
	 * @return dest, or a new color
	 */
	public static Color blendColor(ReadableColor color1, ReadableColor color2, Color dest) {
		if (dest == null) {
			dest = new Color();
		}
		dest.set
			(
				(byte) ((color1.getRed() * color2.getRed()) / 255),
				(byte) ((color1.getGreen() * color2.getGreen()) / 255),
				(byte) ((color1.getBlue() * color2.getBlue()) / 255),
				(byte) ((color1.getAlpha() * color2.getAlpha()) / 255)
			);
		return dest;
	}

	public static Color setAlpha(ReadableColor color, int alpha, Color dest) {
		if (dest == null) {
			dest = new Color();
		}
		dest.set(color.getRed(), color.getGreen(), color.getBlue(), (color.getAlpha() * alpha) / 255);
		return dest;
	}
}
