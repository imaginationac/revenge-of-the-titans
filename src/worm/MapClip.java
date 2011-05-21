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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Implements map tile storage (and nothing more!)
 * @author Cas
 */
public class MapClip implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Size */
	private final int width, height, layers;

	/** The tiles. These look up into the Tile index */
	private final short[][] tile;

	/**
	 * C'tor
	 */
	public MapClip(int width, int height, int layers, short fill) {
		this.width = width;
		this.height = height;
		this.layers = layers;
		tile = new short[layers][];
		for (int i = 0; i < layers; i ++) {
			tile[i] = new short[width * height];
		}
		Arrays.fill(tile[0], fill);
	}

	/**
	 * Copy c'tor
	 * @param toCopy
	 */
	protected MapClip(MapClip toCopy) {
		this.width = toCopy.width;
		this.height = toCopy.height;
		this.layers = toCopy.layers;
		tile = new short[layers][];
		for (int i = 0; i < layers; i ++) {
			tile[i] = new short[width * height];
			System.arraycopy(toCopy.tile[i], 0, tile[i], 0, tile[i].length);
		}
	}

	/**
	 * Gets the tile at the specified x, y, and z
	 * @param x
	 * @param y
	 * @param z
	 * @return a Tile, or null if out-of-bounds
	 */
	public Tile getTile(int x, int y, int z) {
		if (x < 0) {
			return null;
			//x = 0;
		} else if (x >= width) {
			return null;
//			x = width - 1;
		}
		if (y < 0) {
			return null;
//			y = 0;
		} else if (y >= height) {
			return null;
//			y = height - 1;
		}
		int idx = getIndex(x, y);
		if (z < 0 || z >= layers) {
			return null;
		}
		return Tile.getTile(tile[z][idx]);
	}

	/**
	 * Sets the tile at a specified location. Attempting to set a tile at a location out-of-bounds
	 * does nothing.
	 * @param x
	 * @param y
	 * @param newTile The new tile, or null, to clear to 0.
	 */
	public void setTile(int x, int y, int z, Tile newTile) {
		int idx = getIndex(x, y);
		if (idx == -1) {
			return;
		}
		if (z < 0 || z >= layers) {
			return;
		}
		if (newTile == null) {
			tile[z][idx] = 0;
		} else {
			tile[z][idx] = newTile.getIndex();
		}
	}

	/**
	 * Gets the index into the arrays for a given coordinate.
	 * @param x
	 * @param y
	 * @return the index, or -1 if out-of-bounds
	 */
	protected final int getIndex(int x, int y) {
		if (x < 0) {
			return -1;
		} else if (x >= width) {
			return -1;
		} else if (y < 0) {
			return -1;
		} else if (y >= height) {
			return -1;
		} else {
			return x + y * width;
		}
	}

	/**
	 * @return Returns the width.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return Returns the height.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @return Returns the layers.
	 */
	public int getLayers() {
		return layers;
	}

}
