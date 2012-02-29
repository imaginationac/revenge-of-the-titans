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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.WritableDimension;
import org.lwjgl.util.WritablePoint;
import org.lwjgl.util.WritableRectangle;
/**
 * A single glyph in a Font.
 */
public final class Glyph implements Serializable, ReadableRectangle {

	private static final long serialVersionUID = 1L;

	private static final int MAGIC = 0x3456;

	/** Glyph character */
	private char character;

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
	private char[] kernsWith;
	private int[] kerning;

	/**
	 * Constructor for Glyph.
	 */
	public Glyph() {
		super();
	}

	/**
	 * High-speed serialisation
	 * @param os
	 * @throws IOException
	 */
	public void writeExternal(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(MAGIC);
		dos.writeChar(character);
		dos.writeInt(advance);
		dos.writeInt(width);
		dos.writeInt(height);
		dos.writeInt(x);
		dos.writeInt(y);
		dos.writeInt(bearingX);
		dos.writeInt(bearingY);
		if (kernsWith == null) {
			dos.writeInt(0);
		} else {
			dos.writeInt(kernsWith.length);
			for (char c : kernsWith) {
				dos.writeChar(c);
			}
		}
		if (kerning == null) {
			dos.writeInt(0);
		} else {
			dos.writeInt(kerning.length);
			for (int i : kerning) {
				dos.writeInt(i);
			}
		}
	}

	/**
	 * High-speed deserialisation
	 * @param is
	 * @return a new Glyph
	 * @throws IOException
	 */
	public void readExternal(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		int magic = dis.readInt();
		if (magic != MAGIC) {
			throw new IOException("Expected "+MAGIC+" but got "+magic);
		}
		character = dis.readChar();
		advance = dis.readInt();
		width = dis.readInt();
		height = dis.readInt();
		x = dis.readInt();
		y = dis.readInt();
		bearingX = dis.readInt();
		bearingY = dis.readInt();
		int numKerns = dis.readInt();
		if (numKerns > 0) {
			kernsWith = new char[numKerns];
			for (int i = 0; i < kernsWith.length; i ++) {
				kernsWith[i] = dis.readChar();
			}
		}
		int numKerning = dis.readInt();
		if (numKerning > 0) {
			kerning = new int[numKerning];
			for (int i = 0; i < kerning.length; i ++) {
				kerning[i] = dis.readInt();
			}
		}
	}

	/**
	 * Initialize
	 */
	public void init(
		char character,
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
		this.character = character;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.bearingX = bearingX;
		this.bearingY = bearingY;
		this.advance = advance;
		if (kernsWith != null) {
			this.kernsWith = new char[kernsWith.length];
			for (int i = 0; i < kernsWith.length; i ++) {
				this.kernsWith[i] = kernsWith[i].character;
			}
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

		for (int i = 0; i < kernsWith.length; i ++) {
			if (kernsWith[i] == g.character) {
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

	@Override
	public void getSize(WritableDimension dest) {
		dest.setSize(width, height);
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public void getLocation(WritablePoint dest) {
		dest.setLocation(x, y);
	}

}
