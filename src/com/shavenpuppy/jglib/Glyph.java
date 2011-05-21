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
package com.shavenpuppy.jglib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.util.*;
/**
 * A single glyph in a Font.
 */
public final class Glyph implements Serializable, ReadableRectangle {

	static final long serialVersionUID = 1L;

	/** Glyph advance */
	private int advance;

	/** Height */
	private int height;

	/** Width */
	private int width;

	/** Origin in the font's image */
	private int x, y;

	/** Bearing (X) */
	private int bearingX;

	/** Bearing (Y) */
	private int bearingY;

	/** Kerning: sorted in ascending order so binary search will work */
	private ArrayList<Glyph> kernsWith;
	private int[] kerning;

	/**
	 * Constructor for Glyph.
	 */
	public Glyph() {
		super();
	}

	/**
	 * Initialize
	 */
	public void init(
		int x,
		int y,
		int width,
		int height,
		int bearingX,
		int bearingY,
		int advance,
		Glyph[] kernsWith,
		int[] kerning
	) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.bearingX = bearingX;
		this.bearingY = bearingY;
		this.advance = advance;
		if (kernsWith != null) {
			this.kernsWith = new ArrayList<Glyph>(Arrays.asList(kernsWith));
		} else {
			this.kernsWith = null;
		}
		this.kerning = kerning;
	}

	/**
	 * If we have just laid out a glyph, g, and we want to lay out this glyph next to it,
	 * this function will return the required kerning to do so.
	 */
	public int getKerningAfter(Glyph g) {
		if (g == null) {
			return 0;
		}

		if (kernsWith == null) {
//			System.err.println("Kerning after "+g.index+" is not specified");
			return 0;
		}

		for (int i = 0; i < kernsWith.size(); i ++) {
			Glyph g2 = kernsWith.get(i);
			if (g2 == g) {
//				System.err.println("Kerning after "+g.index+" for "+index+" is "+kerning[i]);
				return kerning[i];
			}
		}

//		System.err.println("Kerning after "+g.index+" for "+index+" is not found");
		return 0;
		/*

		int i = Collections.binarySearch(kernsWith, g);
		if (i < 0 || i >= kerning.length)
			return 0;
		else {
			Glyph g2 = (Glyph) kernsWith.get(i);
			if (!g2.equals(g))
				return 0;
			else
				return kerning[i];
		}

		*/
	}

	/**
	 * Returns the bounding box of the glyph in its font's image
	 */
	@Override
	public void getBounds(WritableRectangle dest) {
		dest.setBounds(x, y, width, height);
	}

	/**
	 * Returns the offset origin of the glyph in dest
	 * @param dest a WritablePoint to store the glyph origin
	 */
	public void getBearing(WritablePoint dest) {
		dest.setLocation(bearingX, bearingY);
	}

	/**
	 * Returns the pen advance
	 */
	public int getAdvance() {
		return advance;
	}

	/**
	 * Gets the height.
	 * @return Returns a int
	 */
	@Override
	public int getHeight() {
		return height;
	}


	/**
	 * Gets the width.
	 * @return Returns a int
	 */
	@Override
	public int getWidth() {
		return width;
	}


	/**
	 * Gets the bearingX.
	 * @return Returns a int
	 */
	public int getBearingX() {
		return bearingX;
	}


	/**
	 * Gets the bearingY.
	 * @return Returns a int
	 */
	public int getBearingY() {
		return bearingY;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.ReadableDimension#getSize(com.shavenpuppy.jglib.Dimension)
	 */
	@Override
	public void getSize(WritableDimension dest) {
		dest.setSize(width, height);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.ReadablePoint#getX()
	 */
	@Override
	public int getX() {
		return x;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.ReadablePoint#getY()
	 */
	@Override
	public int getY() {
		return y;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.ReadablePoint#getLocation(com.shavenpuppy.jglib.Point)
	 */
	@Override
	public void getLocation(WritablePoint dest) {
		dest.setLocation(x, y);
	}

}
