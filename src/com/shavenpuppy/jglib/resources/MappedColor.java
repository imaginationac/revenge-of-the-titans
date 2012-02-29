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
import java.nio.ByteBuffer;
import java.text.ParseException;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.util.Parseable;

/**
 * A MappedColor can provide either a fixed specified color, or will return the color from a {@link ColorMapFeature}.
 */
public class MappedColor implements ReadableColor, Parseable, Serializable {

	private static final long serialVersionUID = 1L;

	private String mapName;
	private String colorName;
	private ReadableColor color;

	/**
	 * C'tor
	 */
	public MappedColor() {
	}

	public MappedColor(String def) {
		fromString(def);
	}

	public MappedColor(ReadableColor color) {
		this.color = color;
	}

	@Override
	public void fromString(String src) {
		// Either mapName:colorName or just a color
		src = src.toLowerCase();
		int idx = src.indexOf(':');
		if (idx != -1) {
			mapName = src.substring(0, idx);
			colorName = src.substring(idx + 1, src.length());
		} else {
			try {
				colorName = null;
				color = ColorParser.parse(src);
			} catch (ParseException e) {
				// Use the default color map and hope it's there
				mapName = null;
				colorName = src;
			}
		}
	}

	@Override
	public String toString() {
		if (colorName != null) {
			return (mapName == null ? "" : mapName + ":") + colorName;
		} else {
			Color c = new Color(color);
			float[] hsv = c.toHSB(null);
			return "!"+(int)(hsv[0] * 255.0f)+","+(int)(hsv[1] * 255.0f)+","+(int)(hsv[2] * 255.0f)+","+color.getAlpha();
		}
	}

	private ReadableColor getColor() {
		if (color == null) {
			ReadableColorMap colorMap;
			if (mapName == null) {
				colorMap = ColorMapFeature.getDefaultColorMap();
			} else {
				colorMap = (ReadableColorMap) Resources.get(mapName);
			}
			if (colorMap != null) {
				color = colorMap.getColor(colorName);
			}
			if (color == null) {
				color = ReadableColor.WHITE;
			}
		}
		return color;
	}

	public String getColorName() {
		return colorName;
	}

	public String getMapName() {
		return mapName;
	}

	public void setColor(ReadableColor newColor) {
		color = newColor;
	}

	@Override
	public int getAlpha() {
		return getColor().getAlpha();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#getAlphaByte()
	 */
	@Override
	public byte getAlphaByte() {
		return getColor().getAlphaByte();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#getBlue()
	 */
	@Override
	public int getBlue() {
		return getColor().getBlue();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#getBlueByte()
	 */
	@Override
	public byte getBlueByte() {
		return getColor().getBlueByte();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#getGreen()
	 */
	@Override
	public int getGreen() {
		return getColor().getGreen();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#getGreenByte()
	 */
	@Override
	public byte getGreenByte() {
		return getColor().getGreenByte();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#getRed()
	 */
	@Override
	public int getRed() {
		return getColor().getRed();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#getRedByte()
	 */
	@Override
	public byte getRedByte() {
		return getColor().getRedByte();
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#writeABGR(java.nio.ByteBuffer)
	 */
	@Override
	public void writeABGR(ByteBuffer dest) {
		getColor().writeABGR(dest);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#writeARGB(java.nio.ByteBuffer)
	 */
	@Override
	public void writeARGB(ByteBuffer dest) {
		getColor().writeARGB(dest);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#writeBGR(java.nio.ByteBuffer)
	 */
	@Override
	public void writeBGR(ByteBuffer dest) {
		getColor().writeBGR(dest);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#writeBGRA(java.nio.ByteBuffer)
	 */
	@Override
	public void writeBGRA(ByteBuffer dest) {
		getColor().writeBGRA(dest);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#writeRGB(java.nio.ByteBuffer)
	 */
	@Override
	public void writeRGB(ByteBuffer dest) {
		getColor().writeRGB(dest);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.ReadableColor#writeRGBA(java.nio.ByteBuffer)
	 */
	@Override
	public void writeRGBA(ByteBuffer dest) {
		getColor().writeRGBA(dest);
	}

}
