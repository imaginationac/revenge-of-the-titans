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
package worm.entities;

import java.io.Serializable;

import worm.GameMap;
import worm.Tile;
import worm.Worm;
import worm.WormGameState;
import worm.path.Topology;

import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.IntList;

/**
 * Topology that can be used to navigate the GameMap
 */
class UnitGameMapTopology implements Topology, Serializable {

	private static final long serialVersionUID = 1L;

	final UnitMovement movement;

	/**
	 * C'tor
	 * @param movement
	 */
	UnitGameMapTopology(UnitMovement movement) {
		this.movement = movement;
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getWidth()
	 */
	@Override
	public int getWidth() {
		return Worm.getGameState().getMap().getWidth();
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getHeight()
	 */
	@Override
	public int getHeight() {
		return Worm.getGameState().getMap().getHeight();
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getCost(int, int)
	 */
	@Override
	public int getCost(int from, int to) {
		if (from == to) {
			return 0;
		}

		int sx = getX(from);
		int sy = getY(from);
		int tx = getX(to);
		int ty = getY(to);
		int steps = Math.abs(tx - sx) + Math.abs(ty - sy);
		if (steps == 1) {
			return FPMath.ONE;
		} else {
			assert steps == 2;
			// Diagonal
			return FPMath.fpValue(1.4142135623730950488016887242097f);
		}
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getDistance(int, int)
	 */
	@Override
	public int getDistance(int from, int to) {
		int fromX = getX(from);
		int fromY = getY(from);
		int toX = getX(to);
		int toY = getY(to);

		// Use actual distance
		int dx = fromX - toX;
		int dy = fromY - toY;
		int h = FPMath.fpValue(Math.sqrt(dx * dx + dy * dy));
		h *= 1.0f + 1.0f / WormGameState.ABS_MAX_SIZE;
		return h;
	}

	/* (non-Javadoc)
	 * @see worm.path.Topology#getNeighbours(int, int, worm.path.IntList)
	 */
	@Override
	public void getNeighbours(int node, int parent, IntList dest) {
		dest.clear();

		int x = getX(node);
		int y = getY(node);


		int n = pack(x, y + 1);
		if (n != parent && canMove(x, y, x, y + 1)) {
			dest.add(n);
		}
		n = pack(x, y - 1);
		if (n != parent && canMove(x, y, x, y - 1)) {
			dest.add(n);
		}
		n = pack(x + 1, y);
		if (n != parent && canMove(x, y, x + 1, y)) {
			dest.add(n);
		}
		n = pack(x - 1, y);
		if (n != parent && canMove(x, y, x - 1, y)) {
			dest.add(n);
		}

		// Diagonal moves
		n = pack(x + 1, y + 1);
		if (n != parent && canMove(x, y, x + 1, y + 1)) {
			dest.add(n);
		}
		n = pack(x - 1, y + 1);
		if (n != parent && canMove(x, y, x - 1, y + 1)) {
			dest.add(n);
		}
		n = pack(x + 1, y - 1);
		if (n != parent && canMove(x, y, x + 1, y - 1)) {
			dest.add(n);
		}
		n = pack(x - 1, y - 1);
		if (n != parent && canMove(x, y, x - 1, y - 1)) {
			dest.add(n);
		}

	}

	/**
	 * Can we make a move from s to t? (s and t must be adjacent)
	 * @param sx
	 * @param sy
	 * @param tx
	 * @param ty
	 * @return true if this move is valid
	 */
	boolean canMove(int sx, int sy, int tx, int ty) {
		// Check destination tile is free
		if (isImpassable(tx, ty)) {
			return false;
		}

		return true;
	}

	/**
	 * Determines whether the specified map location is impassable by the gidrah
	 * @param x
	 * @param y
	 * @return
	 */
	boolean isImpassable(int x, int y) {
		GameMap map = Worm.getGameState().getMap();
		if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) {
			//System.out.println(x+","+y+" is impassable");
			return true;
		}

		// If this square is directly next to the unit, we can't pass into a square reserved by another thing.
		int absDX = Math.abs(x - movement.getUnit().getTileX());
		int absDY = Math.abs(y - movement.getUnit().getTileY());
		if (absDX <= 1 && absDY <= 1) {
			if (map.isOccupied(x, y)) {
				return true;
			}
		}

		// Otherwise we just check the tiles at all levels of the game map.
		for (int z = 0; z < GameMap.LAYERS; z ++) {
			Tile t = map.getTile(x, y, z);
			if (t != null && (t.isImpassable() || t.isSolid())) {
				//System.out.println("Tile "+t+" at "+x+", "+y+", "+z+" is impassable");
				return true;
			}
		}

		return false;
	}

	static int pack(int x, int y) {
		return x & 0xFFFF | y << 16;
	}

	static int getX(int state) {
		if ((state & 0xFFFF) <= 0x7FFF) {
			return state & 0x7FFF;
		} else {
			return state & 0xFFFF | 0xFFFF0000;
		}
	}

	static int getY(int state) {
		return state >> 16;
	}

}
