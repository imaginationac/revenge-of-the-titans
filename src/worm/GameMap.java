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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.lwjgl.util.Rectangle;


/**
 * A game map. Note that this is not a Resource.
 * @author Cas
 */
public class GameMap implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Max layers */
	public static final int LAYERS = 3;

	/** Fill tile index */
	private final short fill;

	/** The map, marked transient because we serialize it specially */
	private transient MapClip map;

	/** Visibility of bottom left corner of each tile */
	private transient IntGrid visibility;

	/** Gidrah occupation (origin at -1, -1, and bigger than main map by 1) */
	private transient IntGrid occupied;

	/** Gidrah attacking (origin at -1, -1, and bigger than main map by 1) */
	private transient IntGrid attacking;

	/** Danger level (total turret coverage) */
	private transient IntGrid danger;

	/** Cost (used to calc actual movement speed of gids) */
	private transient IntGrid cost;

	/** Difficulty (used by gids to guess actual movement speed; affected by barricades) */
	private transient IntGrid difficulty;

	/** Fade level */
	private transient IntGrid fade;

	/** Listener */
	private transient MapListener listener;

	/**
	 * C'tor
	 */
	public GameMap(int width, int height, short fill) {
		this.fill = fill;
		map = new MapClip(width, height, LAYERS, fill);
		visibility = new IntGrid(width + 1, height + 1, 0);
		occupied = new IntGrid(width + 2, height + 2, 0);
		danger = new IntGrid(width, height, 0);
		attacking = new IntGrid(width + 2, height + 2, 0);
		fade = new IntGrid(width, height, 0);
		cost = new IntGrid(width, height, 0);
		difficulty = new IntGrid(width, height, 0);
	}

	/**
	 * @return Returns the width.
	 */
	public int getWidth() {
		return map.getWidth();
	}

	/**
	 * @return Returns the height.
	 */
	public int getHeight() {
		return map.getHeight();
	}

	/**
	 * Gets the danger level of a tile - this is the number of nearby turrets.
	 * @param x
	 * @param y
	 * @return a visibility value
	 */
	public int getDanger(int x, int y) {
		return danger.getValue(x, y);
	}

	/**
	 * Gets the cost of traversing a tile
	 * @param x
	 * @param y
	 * @return the cost of tile traversal
	 */
	public int getCost(int x, int y) {
		return cost.getValue(x, y);
	}

	/**
	 * Gets the extra guessed difficulty of traversing a tile
	 * @param x
	 * @param y
	 * @return the cost of tile traversal
	 */
	public int getDifficulty(int x, int y) {
		return difficulty.getValue(x, y);
	}

	/**
	 * Gets the visibility of the bottom left corner of a tile.
	 * @param x
	 * @param y
	 * @return a visibility value
	 */
	public int getVisibility(int x, int y) {
		return visibility.getValue(x, y);
	}

	public int getFade(int x, int y) {
		return fade.getValue(x, y);
	}

	public int getTFade(int x, int y) {
		return Math.max(0, fade.getValue(x, y) - 4);
	}

	public int getFFade(int x, int y) {
		return Math.max(0, fade.getValue(x, y) - 2);
	}

	public void setFade(int x, int y, int value) {
		fade.setValue(x, y, value);
	}


	/**
	 * Determines if a square is occupied by a gidrah
	 * @param x
	 * @param y
	 * @return true if it is
	 */
	public boolean isOccupied(int x, int y) {
		return occupied.getValue(x + 1, y + 1) != 0;
	}
	public boolean isAttacking(int x, int y) {
		return attacking.getValue(x + 1, y + 1) != 0;
	}

	/**
	 * Sets the occupied state of a square. If the square must not already be occupied.
	 * @param x
	 * @param y
	 * @param occ
	 */
	public void setOccupied(int x, int y) {
		occupied.setValue(x + 1, y + 1, 1);
	}

	public void clearOccupied(int x, int y) {
		occupied.setValue(x + 1, y + 1, 0);
	}

	public void setAttacking(int x, int y) {
		attacking.setValue(x + 1, y + 1, 1);
	}

	public void clearAttacking(int x, int y) {
		attacking.setValue(x + 1, y + 1, 0);
	}

	/**
	 * @param x
	 * @param y
	 * @param newValue
	 */
	public void setVisibility(int x, int y, int newValue) {
		visibility.setValue(x, y, newValue);
	}

	/**
	 * @param x
	 * @param y
	 * @param newValue
	 */
	public void setCost(int x, int y, int newValue) {
		cost.setValue(x, y, newValue);
	}

	/**
	 * @param x
	 * @param y
	 * @param newValue
	 */
	public void setDifficulty(int x, int y, int newValue) {
		difficulty.setValue(x, y, newValue);
	}

	/**
	 * @param x
	 * @param y
	 * @param newValue
	 */
	public void setDanger(int x, int y, int newValue) {
		danger.setValue(x, y, newValue);
	}

	/**
	 * Stash in a TileInfo
	 * @param x
	 * @param y
	 * @param tileInfo Destination, or null
	 * @return tileInfo, or a new TileInfo if TileInfo was null
	 */
	public TileInfo toTileInfo(int x, int y, TileInfo tileInfo) {
		if (tileInfo == null) {
			tileInfo = new TileInfo();
		}
		tileInfo.set(getTile(x, y, tileInfo.get()));
		return tileInfo;
	}

	/**
	 * Get the tiles at a particular location. If the location is out of bounds, the tiles are nulled
	 * @param x
	 * @param y
	 * @param dest Destination array of Tiles, or null
	 * @return dest[], or a new array of Tiles, or null
	 */
	public Tile[] getTile(int x, int y, Tile[] dest) {
		if (dest == null) {
			dest = new Tile[LAYERS];
		}
		for (int i = 0; i < LAYERS; i ++) {
			dest[i] = map.getTile(x, y, i);
		}
		return dest;
	}



	/**
	 * Get the tile at a particular location and layer
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 * @see droid.MapClip#getTile(int, int, int)
	 */
	public Tile getTile(int x, int y, int z) {
		return map.getTile(x, y, z);
	}

	/**
	 * Sets a tile at a particular location. If out of bounds, this is a no-op.
	 * @param x
	 * @param y
	 * @param newTile the new tile to set (may not be null)
	 */
	public void setTile(final int x, final int y, final int z, final Tile newTile) {
		// Will anything actually happen?
		if (!isValidDraw(x, y, z, newTile)) {
			// No, so just bomb out
			return;
		}

		// Remember the old tile for a sec..
		Tile oldTile = map.getTile(x, y, z);
		oldTile.onCleared(this, x, y);

		// Draw onto the map
		map.setTile(x, y, z, newTile);
		newTile.onDrawn(this, x, y);

		// Calculate tile rules if the map changed on layer 0
		if (z == 0 && groupChanged(newTile.getGroup(), oldTile.getGroup())) {
			// Recursively start twiddling neighbours
			recalculateNeighbours(x, y);
		}

		// Calculate visibility
		calcVis(x, y);
		calcVis(x + 1, y);
		calcVis(x + 1, y + 1);
		calcVis(x, y + 1);
		calcVis(x - 1, y + 1);
		calcVis(x - 1, y);
		calcVis(x - 1, y - 1);
		calcVis(x, y - 1);
		calcVis(x + 1, y - 1);

		if (listener != null) {
			listener.onChanged(x, y);
		}
	}

	private void calcVis(int x, int y) {
		outer: for (int z = 0; z < LAYERS; z ++) {
			for (int yy = y - 1; yy <= y; yy ++) {
				for (int xx = x - 1; xx <= x; xx ++) {
					Tile t = getTile(xx, yy, z);
					if (t == null || !t.isSolid()) {
						continue outer;
					}
				}
			}
			// This whole layer is solid at this point, so we're invisible to the outside.
			setVisibility(x, y, 0);
			return;
		}
		setVisibility(x, y, 1);
	}

	/**
	 * Checks to see if a coordinate is in bounds
	 * @param x
	 * @param y
	 * @return true if (x, y) is within the boundary of the map
	 */
	private boolean isInBounds(int x, int y) {
		return x >= 0 && y >= 0 && x < getWidth() && y < getHeight();
	}

	/**
	 * Determines whether a setTile operation will actually have any effect
	 * @param x
	 * @param y
	 * @param z
	 * @param newTile
	 * @return true if a draw will have an effect
	 */
	private boolean isValidDraw(final int x, final int y, final int z, final Tile newTile) {
		// Bomb out straight away if it's out-of-bounds
		if (!isInBounds(x, y)) {
			return false;
		}

		// Ensure no-one draws items on solid floortiles
		if (map.getTile(x, y, 0).isSolid() && z > 0 && newTile != Tile.getEmptyTile()) {
			return false;
		}

		// Looks like a draw will occur
		return true;
	}

	/**
	 * Clear the items at the specified position (thats things in layer 2 and above)
	 * @param x
	 * @param y
	 */
	public void clearItem(final int x, final int y) {
		for (int i = 2; i < LAYERS; i ++) {
			setTile(x, y, i, Tile.getEmptyTile());
		}
	}

	/**
	 * @param g1
	 * @param g2
	 * @return
	 */
	private boolean groupChanged(String g1, String g2) {
		if (g1 == g2) {
			return false;
		}
		if (g1 == null || g2 == null) {
			return true;
		}
		return !g1.equals(g2);
	}

	/**
	 * Recalculate tile's neighbours. We do this by reading and drawing the
	 * NSEW neighbours of a tile. Tile rules then cause the appropriate tile
	 * to be drawn instead if necessary.
	 * @param x
	 * @param y
	 */
	private void recalculateNeighbours(int x, int y) {
		Tile t = map.getTile(x, y + 1, 0);
		if (t != null) {
			t.toMap(this, x, y + 1, false);
		}
		t = map.getTile(x + 1, y, 0);
		if (t != null) {
			t.toMap(this, x + 1, y, false);
		}
		t = map.getTile(x, y - 1, 0);
		if (t != null) {
			t.toMap(this, x, y - 1, false);
		}
		t = map.getTile(x - 1, y, 0);
		if (t != null) {
			t.toMap(this, x - 1, y, false);
		}
	}

	/**
	 * Override standard object writing so we can gzip the stream for MapStorage
	 * @param stream
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		GZIPOutputStream gzos = new GZIPOutputStream(stream);
		ObjectOutputStream oos = new ObjectOutputStream(gzos);
		oos.writeObject(map);
		oos.writeObject(visibility);
		oos.writeObject(occupied);
		oos.writeObject(danger);
		oos.writeObject(attacking);
		oos.writeObject(fade);
		oos.writeObject(cost);
		oos.writeObject(difficulty);
		oos.flush();
		gzos.finish();
	}

	/**
	 * Override standard object reading to read the zipped MapStorage
	 * @param stream
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		GZIPInputStream gzis = new GZIPInputStream(stream, 1024 * 1024);
		ObjectInputStream ois = new ObjectInputStream(gzis);
		map = (MapClip) ois.readObject();
		visibility = (IntGrid) ois.readObject();
		occupied = (IntGrid) ois.readObject();
		danger = (IntGrid) ois.readObject();
		attacking = (IntGrid) ois.readObject();
		fade = (IntGrid) ois.readObject();
		cost = (IntGrid) ois.readObject();
		difficulty = (IntGrid) ois.readObject();
	}

	/**
	 * Given a set of pixel coordinates, ensure that the rectangle is in a clear bit of map, that is,
	 * with no solid or impassable terrain under it.
	 * @param bounds A {@link Rectangle} in pixel coordinates
	 * @return true if the rectangle lies in a clear space
	 */
	public boolean isClearPX(Rectangle bounds) {
		for (int y = bounds.getY() / MapRenderer.TILE_SIZE; y <= (bounds.getHeight() - 1) / MapRenderer.TILE_SIZE; y ++) {
			for (int x = bounds.getX() / MapRenderer.TILE_SIZE; x <= (bounds.getWidth() - 1) / MapRenderer.TILE_SIZE; x ++) {
				for (int z = 0; z < GameMap.LAYERS; z ++) {
					Tile tile = map.getTile(x, y, z);
					if (tile != null && (tile.isSolid() || tile.isImpassable())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public void setListener(MapListener listener) {
		this.listener = listener;
	}

}
