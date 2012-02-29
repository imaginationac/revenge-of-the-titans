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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;

/**
 * Grid-based collision manager.
 */
class GridCollisionManager implements CollisionManager {

	private static final int INITIAL_ENTITIES = 512;
	private static final int BORDER_TILES = 2;

	private static class Cell {
		final ArrayList<Entity> contents = new ArrayList<Entity>(4);
		boolean inUsed;
	}

	private static class Pair {

		Entity a, b;

		Pair(Entity a, Entity b) {
	        this.a = a;
	        this.b = b;
        }

		@Override
		public int hashCode() {
		    return a.hashCode() + b.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			Pair p = (Pair) obj;
			return ((p.a == a && p.b == b) || (p.a == b && p.b == a));
		}
	}

	/** Temp pair */
	private final Pair tempPair = new Pair(null, null);

	/** Temp rects */
	private final Rectangle temp = new Rectangle(), cells = new Rectangle();

	/** Collision pairs */
	private final List<Pair> collisions = new ArrayList<Pair>();

	/** Cell size */
	private final int cellSize;

	/** The sparse grid */
	private Cell[] grid;

	/** All cells which actually contain at least 1 entity */
	private ArrayList<Cell> used = new ArrayList<Cell>(INITIAL_ENTITIES), used0 = new ArrayList<Cell>(INITIAL_ENTITIES);

	/** Origin (grid) */
	private int ox, oy;

	/** Size (grid) */
	private int w, h;

	/** Map of Entities to ReadableRectangles; this maps Entities to the cells in which they have been placed */
	private Map<Entity, ReadableRectangle> entityMap = new HashMap<Entity, ReadableRectangle>(INITIAL_ENTITIES);

	private static int fastFloor(float x) {
		int i = (int) x;
		return x >= 0.0f ? i : i == x ? i : i - 1;
	}

    GridCollisionManager(int cellSize) {
		this.cellSize = cellSize;
		ox = -BORDER_TILES;
		oy = -BORDER_TILES;
		w = WormGameState.ABS_MAX_SIZE + BORDER_TILES * 2;
		h = WormGameState.ABS_MAX_SIZE + BORDER_TILES * 2;
		grid = new Cell[w * h];
	}

	@Override
	public void clear() {
		Arrays.fill(grid, null);
		used.clear();
		entityMap.clear();
	}

	private void calcBounds(Entity entity) {
		float radius = entity.getRadius();
		if (radius != 0.0f) {
			// it's a round entity - we'll expand its bounds a bit to account for freaky rounding
			int size = (int)(radius * 2.0f) + 2;
			temp.setBounds((int)(entity.getX() - radius) - 1, (int)(entity.getY() - radius) - 1, size, size);
		} else {
			entity.getBounds(temp);
		}
	}

	@Override
	public void store(Entity entity) {
		// See which cells we should be in and maybe resize the grid
		calcBounds(entity);
		Rectangle r = calcCells(temp, new Rectangle());

		if (entityMap.put(entity, r) != null) {
			assert false : "Entity "+entity+" is already in the entityMap!";
		}

		// Add this entity to each cell
		int cell = r.getX() + r.getY() * w;
		for (int y = r.getHeight(); -- y >= 0; ) {
			for (int x = r.getWidth(); -- x >= 0; ) {
				Cell c = grid[cell];
				if (c == null) {
					c = new Cell();
					grid[cell] = c;
				}
				c.contents.add(entity);
				if (!c.inUsed) {
					c.inUsed = true;
					used.add(c);
				}

				cell ++;
			}
			cell += w - r.getWidth();
		}
	}

	private Rectangle calcCells(ReadableRectangle src, Rectangle dest) {
		dest.setBounds(fastFloor(src.getX() / cellSize) - ox, fastFloor(src.getY() / cellSize) - oy, 0, 0);
		dest.add(fastFloor((src.getX() + src.getWidth() - 1) / cellSize) - ox + 1, fastFloor((src.getY() + src.getHeight() - 1) / cellSize) - oy + 1);
		dest.setBounds(Math.max(0, dest.getX()), Math.max(0, dest.getY()), Math.min(w, dest.getX() + dest.getWidth()) - dest.getX(), Math.min(h, dest.getY() + dest.getHeight()) - dest.getY());
		return dest;
	}

	@Override
	public CollisionManager add(Entity entity) {
		store(entity);
		return this;
	}

	@Override
	public CollisionManager submit(ReadableRectangle rect) {
		return this;
	}

	@Override
	public boolean remove(Entity entity) {

		// Shortcut: look in entity map first
		ReadableRectangle cells = entityMap.remove(entity);
		if (cells != null) {
			int cell = cells.getX() + cells.getY() * w;
			for (int yy = cells.getHeight(); -- yy >= 0; ) {
				for (int xx = cells.getWidth(); -- xx >= 0; ) {
					Cell test = grid[cell];
					if (test == null) {
						assert false : "Entity "+entity+" was supposed to be at "+cells+" but isn't at "+(cell % w)+","+(cell / w);
					}
					if (!test.contents.remove(entity)) {
						assert false : "Entity "+entity+" not found where it was expected!";
					}
					cell ++;
				}

				cell += w - cells.getWidth();
			}
			return true;

		} else {
			assert false : "Entity "+entity+" not found!";
			return false;
		}
	}

	@Override
	public List<Entity> checkCollisions(Entity entity, List<Entity> dest) {
		if (dest == null) {
			dest = new ArrayList<Entity>();
		}

		dest.clear();

		if (!entity.isActive() || !entity.canCollide()) {
			// Shortcut: src entity can't collide
			return dest;
		}

		calcBounds(entity);
		calcCells(temp, cells);

		int cell = cells.getX() + cells.getY() * w;
		for (int y = cells.getHeight(); -- y >= 0; ) {
			for (int x = cells.getWidth(); -- x >= 0; ) {
				Cell c = grid[cell];
				if (c != null) {
					List<Entity> contents = c.contents;
					final int n = contents.size();
					for (int i = n; --i >= 0; ) {
						Entity test = contents.get(i);
						if (entity != test && test.isActive() && test.canCollide() && test.isTouching(entity) && !dest.contains(test)) {
							dest.add(test);
						}
					}
				}
				cell ++;
			}
			cell += w - cells.getWidth();
		}

		collisions.clear();

		return dest;
	}

	@Override
	public List<Entity> checkCollisions(ReadableRectangle rect, List<Entity> dest) {
		if (dest == null) {
			dest = new ArrayList<Entity>();
		}

		dest.clear();

		calcCells(rect, cells);

		int cell = cells.getX() + cells.getY() * w;
		for (int y = cells.getHeight(); -- y >= 0; ) {
			for (int x = cells.getWidth(); --x >= 0; ) {
				Cell c = grid[cell];
				if (c != null) {
					List<Entity> contents = c.contents;
					final int n = contents.size();
					for (int i = n; --i >= 0; ) {
						Entity entity = contents.get(i);
						if (entity.isActive() && entity.canCollide() && entity.isTouching(rect) && !dest.contains(entity)) {
							dest.add(entity);
						}
					}
				}
				cell ++;
			}
			cell += w - cells.getWidth();
		}

		collisions.clear();

		return dest;
	}

	@Override
	public void checkCollisions() {
		// For each cell with something in it...
		for (int i = used.size(); --i >= 0; ) {
			Cell cell = used.get(i);
			if (cell.contents.size() > 0) {
				used0.add(cell);
			}
		}

		for (int cell = used0.size(); --cell >= 0; ) {
			// Process all combinations within that cell
			List<Entity> contents = used0.get(cell).contents;
			for (int i = 0; i < contents.size(); i ++) {
				Entity src = contents.get(i);
				if (src.isActive() && src.canCollide()) {
					for (int j = i + 1; j < contents.size(); j ++) {
						Entity dest = contents.get(j);
						if (dest.isActive() && src.isActive() && src.canCollide() && dest.canCollide() && src.isTouching(dest)) {
							// Inform both entities of the collision, in no particular order, unless already done
							tempPair.a = src;
							tempPair.b = dest;
							if (collisions.contains(tempPair)) {
								continue;
							}
							collisions.add(new Pair(src, dest));
							src.onCollision(dest);
							dest.onCollision(src);
						}
					}
				}
			}
		}

		// Compact used list
		used0.clear();
		for (int i = used.size(); --i >= 0; ) {
			Cell cell = used.get(i);
			if (cell.contents.size() > 0) {
				used0.add(cell);
			} else {
				cell.inUsed = false;
			}
		}
		ArrayList<Cell> temp = used;
		used = used0;
		used0 = temp;
		used0.clear();

		// Clear away collisions now they're all processed
		collisions.clear();
	}



}
