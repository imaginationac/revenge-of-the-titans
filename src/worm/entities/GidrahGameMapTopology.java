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
import worm.features.GidrahFeature;
import worm.path.Topology;

import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.IntList;

/**
 * Topology that can be used to navigate the GameMap
 */
class GidrahGameMapTopology implements Topology, Serializable {

	private static final long serialVersionUID = 1L;

	private static final float DIAGONAL_FACTOR = 1.4142135623730950488016887242097f;
	private static final int CLUMP_DISTANCE_THRESHOLD = 5 * 5; // Only worry about clumping when < 5 squares away

	final GidrahMovement movement;
	final GidrahFeature gidrahFeature;
	final Gidrah gidrah;
	final WormGameState gameState;
	final GameMap map;
	final boolean diagonal;
	final int width, height;

	/**
	 * C'tor
	 * @param movement
	 */
	GidrahGameMapTopology(GidrahMovement movement) {
		this.movement = movement;
		this.diagonal = movement.isDiagonal();
		this.gameState = Worm.getGameState();
		this.map = gameState.getMap();
		this.gidrah = movement.getGidrah();
		this.gidrahFeature = gidrah.getFeature();
		this.width = map.getWidth();
		this.height = map.getHeight();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int getCost(int from, int to) {
		if (from == to) {
			return 0;
		}

		// Bias gidrahs away from turrets
		int sx = getX(from);
		int sy = getY(from);
		int tx = getX(to);
		int ty = getY(to);
		boolean wraith = gidrahFeature.isWraith();
		boolean angry = gidrahFeature.isAngry();
		float bias = SPEED_SCALE * (wraith ? 0.0f : Math.max(0.0f, map.getDanger(tx, ty) - gidrahFeature.getArmour())) * gidrahFeature.getBrain().getAvoidanceFactor() * (angry ? 0.75f : 1.0f) * (1.0f - movement.getSpeedupRamp());
		int cost = wraith ? NORMAL_COST : map.getCost(tx, ty);
		int difficulty = wraith ? 0 : map.getDifficulty(tx, ty);
		int steps = Math.abs(tx - sx) + Math.abs(ty - sy);
		int tileX = gidrah.getTileX();
		int tileY = gidrah.getTileY();
		int basicDistance = (tileX - tx) * (tileX - tx) + (tileY - ty) * (tileY - ty);
		// Prevent clumping nearby
		if (cost == NORMAL_COST || cost == BOG_COST) {
			if (!wraith && basicDistance < CLUMP_DISTANCE_THRESHOLD) {
				int occupiedCount = 0;
				if (map.isOccupied(tx, ty)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx + 1, ty)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx, ty + 1)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx - 1, ty)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx, ty - 1)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx + 1, ty + 1)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx - 1, ty - 1)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx - 1, ty + 1)) {
					occupiedCount ++;
				}
				if (map.isOccupied(tx + 1, ty - 1)) {
					occupiedCount ++;
				}
				if (occupiedCount >= 4) {
					cost += FPMath.ONE;
				}
			}
		} else {
			// Roads mean we worry much less about danger or difficulty
			bias *= 0.25f;
			difficulty >>= 1;
		}
		if (map.isAttacking(tx, ty)) {
			cost += FPMath.FOUR;
		}

		if (diagonal) {
			if (steps == 2) {
				// Diagonal
				return FPMath.fpValue(bias) + cost + FPMath.fpValue(difficulty);
			} else {
				assert steps == 1;
				return (int) (cost * DIAGONAL_FACTOR + FPMath.fpValue(DIAGONAL_FACTOR * bias)  + FPMath.fpValue(difficulty * DIAGONAL_FACTOR)) * 5; // Really discourage horiz/vert movement with x5 multiplier to cost
			}
		} else {
			if (steps == 1) {
				return FPMath.fpValue(bias) + cost + FPMath.fpValue(difficulty);
			} else {
				assert steps == 2;
				// Diagonal
				return (int) (cost * DIAGONAL_FACTOR) + FPMath.fpValue(DIAGONAL_FACTOR * bias) + FPMath.fpValue(difficulty * DIAGONAL_FACTOR);
			}
		}
	}

	public float getSpeed(int tx, int ty) {
		if (gidrahFeature.isBoss() || gidrahFeature.isWraith()) {
			return 1.0f;
		}
		return FPMath.floatValue(map.getCost(tx, ty)) * SPEED_SCALE_FACTOR;
	}

	@Override
	public int getDistance(int from, int to) {
		int fromX = getX(from);
		int fromY = getY(from);
		int toX = getX(to);
		int toY = getY(to);

		// Use actual distance
		int dx = fromX - toX;
		int dy = fromY - toY;
		return FPMath.fpValue(Math.sqrt(dx * dx + dy * dy));
	}

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
		int dx = Math.abs(sx - tx);
		int dy = Math.abs(sy - ty);
		if (dx > 1 || dy > 1 || dx == 0 && dy == 0) {
			return false;
		}
		// Check destination tile is free
		if (isImpassable(tx, ty)) {
			return false;
		}

//		// Diagonal: only allow move if it's the final target square, or a diagonal
//		if (diagonal) {
//			if (dx == 1 && dy == 1) {
//				return true;
//			}
//
//			Entity target = gidrah.getTarget();
//			if (target == null) {
//				return false;
//			}
//			if (tx != target.getTileX() || ty != target.getTileY()) {
//				return false;
//			}
//
//		}

		return true;
	}

	/**
	 * Determines whether the specified map location is impassable by the gidrah
	 * @param x
	 * @param y
	 * @return
	 */
	boolean isImpassable(int x, int y) {
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return true;
		}

		// If this square is directly next to the gidrah, we can't pass into a square reserved by another thing, unless
		// we're a gidlet
		if (!gidrahFeature.isGidlet()) {
			if (map.isAttacking(x, y)) {
				// This square is being attacked by another gidrah, so we shall avoid it too
				return true;
			}
			int absDX = Math.abs(x - gidrah.getTileX());
			int absDY = Math.abs(y - gidrah.getTileY());
			if (absDX <= 1 && absDY <= 1) {
				if (map.isOccupied(x, y)) {
					return true;
				}
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
