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
package worm.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.puppygames.applet.Game;

import org.lwjgl.util.Point;
import org.lwjgl.util.ReadablePoint;

import worm.IntGrid;
import worm.MapRenderer;
import worm.Worm;
import worm.WormGameState;
import worm.features.LevelFeature;
import worm.path.Topology;
import worm.tiles.Crystal;
import worm.tiles.Ruin;

import com.shavenpuppy.jglib.algorithms.Bresenham;
import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.interpolators.Interpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.interpolators.SineInterpolator;
import com.shavenpuppy.jglib.util.IntList;
import com.shavenpuppy.jglib.util.Util;

/**
 * Draws bases and spawnpoints in a standard way.
 */
public abstract class BaseMapGenerator extends AbstractMapGenerator {

//	public static void main(String[] args) {
//		for (int level = 0; level < 50; level ++) {
//			int num;
//			if (level % 10 == 9) {
//				num = BASE_CRYSTALS_BOSS;
//			} else if (LevelFeature.BIAS[level % LevelFeature.BIAS.length] == -1) {
//				num = BASE_CRYSTALS_CENTRAL;
//			} else {
//				num = BASE_CRYSTALS;
//			}
//			int worldIndex = level / WormGameState.LEVELS_IN_WORLD;
//			//int c = (int) ((CRYSTALS_PER_LEVEL[Math.min(CRYSTALS_PER_LEVEL.length - 1, worldIndex)] * level) + num + worldIndex * BASE_CRYSTALS_PER_WORLD);
//			int c = (int) ((BASE_CRYSTALS_PER_LEVEL * level) + num + worldIndex * BASE_CRYSTALS_PER_WORLD);
//			int c2 = (int) ((0.2f * level) + num + worldIndex * BASE_CRYSTALS_PER_WORLD);
//			System.out.print("Level "+level+": $"+(c * 1000));
//			System.out.print("           $"+(c2 * 1000));
//			System.out.println("           $"+((c - c2) * 1000));
//		}
//	}

	private static final int MAX_ATTEMPTS = 500;
	private static final int MAX_MID_SPAWNERS = 4;
	private static final int SPAWN_POINT_MARGIN = 5;
	protected static final int OBSTACLE_MARGIN = 2;
	private static final int MIN_BASE_RANGE_TO_SPAWNPOINT = 20;
	private static final int MID_SPAWNERS_DIVISOR = 8;
	private static final int MID_SPAWNERS_THRESHOLD = 25;
	private static final int MIN_CRYSTAL_DISTANCE = 4;
	private static final int BASE_MARGIN = (MapRenderer.FADE_SIZE * 2) / 3;
	private static final int BASE_CRYSTALS_PER_WORLD = 1;
	private static final int BASE_CRYSTALS = 2;
	private static final int EXTRA_CRYSTALS_CENTRAL = 2;
	private static final int EXTRA_CRYSTALS_BOSS = 4;
	private static final int BASE_CRYSTALS_SURVIVAL = 0;
	private static final float BASE_CRYSTALS_PER_LEVEL = 0.5f;
	private static final float MAX_MONEY = 25000.0f; // unimpeded up to 25k..
	private static final float ABS_MAX_MONEY = 25000.0f; // then squeezed up to 50k

	private int totalRuins, totalCrystals;

	/**
	 * C'tor
	 * @param template
	 * @param level
	 * @param levelFeature TODO
	 */
	public BaseMapGenerator(MapTemplate template, MapGeneratorParams mapGeneratorParams) {
		super(template, mapGeneratorParams);
	}

	public static boolean isBaseCentralForLevel(int level, int gameMode) {
		return gameMode == WormGameState.GAME_MODE_SURVIVAL || LevelFeature.getLevel(level).getBias() == -1;
	}

	@Override
	protected final void generateBases() {
		int ww = getWidth() - BASE_MARGIN * 2;
		int hh = getHeight() - BASE_MARGIN * 2;
		boolean[] used = new boolean[ww * hh];
		int ox, oy;
		switch (levelFeature.getBias()) {
			case 0:
				ox = ww * 2 / 3;
				oy = hh / 3;
				break;
			case 1:
				ox = ww * 2 / 3;
				oy = hh * 2 / 3;
				break;
			case 2:
				ox = ww / 3;
				oy = hh * 2 / 3;
				break;
			case 3:
				ox = 0;
				oy = hh * 2 / 3;
				break;
			case 4:
				ox = 0;
				oy = hh / 3;
				break;
			case 5:
				ox = 0;
				oy = 0;
				break;
			case 6:
				ox = ww / 3;
				oy = 0;
				break;
			case 7:
				ox = ww * 2 / 3;
				oy = 0;
				break;
			default:
				assert false;
			case -1:
				ox = ww / 3;
				oy = hh / 3;
				break;
		}

		// Choose a set of points, then centre them according to level bias.
		int x = Util.random(3, ww / 3 - 6) + ox + BASE_MARGIN;
		int y = mapGeneratorParams.getGameMode() == WormGameState.GAME_MODE_XMAS ? BASE_MARGIN + 3 : Util.random(3, hh / 3 - 6) + oy + BASE_MARGIN;

		// Ensure not proximal to any other base.
		boolean ok;
		do {
			ok = 	isEmpty(x, y, used)
				&& 	isEmpty(x + 1, y, used)
				&& 	isEmpty(x + 1, y + 1, used)
				&& 	isEmpty(x + 1, y + 2, used)
				&& 	isEmpty(x + 2, y, used)
				&& 	isEmpty(x + 2, y + 1, used)
				&& 	isEmpty(x + 2, y + 2, used)
				&& 	isEmpty(x, y + 2, used)
				&& 	isEmpty(x, y + 1, used)
				;
			Thread.yield();
		} while (!ok);

		// Bases are 4 x 3 tiles
		for (int yy = 0; yy < 3; yy ++) {
			for (int xx = 0; xx < 4; xx ++) {
				setUsed(x + xx, y + yy, used);
			}
		}

		addBase(x, y);
	}

	private void setUsed(int x, int y, boolean[] used) {
		if (x < 0 || y < 0 || x >= getWidth() / 3 || y >= getHeight() / 3) {
			return;
		}
		used[x + y * (getWidth() / 3)] = true;
	}

	protected final boolean isEmpty(int x, int y, boolean[] used) {
		if (x < 0 || y < 0 || x >= getWidth() / 3 || y >= getHeight() / 3) {
			return true;
		}
		return !used[x + y * (getWidth() / 3)];
	}

	protected abstract float getMinSpawnTunnelSize();
	protected abstract float getMaxSpawnTunnelSize();

	@Override
	protected void clean() {
		// Now find and fill any areas which are not either connected to a base or a spawn point
		IntGrid temp = new IntGrid(getWidth(), getHeight(), 0);

		// First, mark walls
		SolidCheck solidCheck = new SolidCheck() {
			@Override
			public boolean isSolid(int x, int y, int value) {
				return x < 0 || y < 0 || x >= getWidth() || y >= getHeight() || value == WALL || value == RUIN || value == IMPASSABLE || value == TOTAL_IMPASSABLE || value == RUIN_IMPASSABLE || value == CRYSTAL || value == SPECIAL_IMPASSABLE;
			}
		};
		for (int y = 0; y < getHeight(); y ++) {
			for (int x = 0; x < getWidth(); x ++) {
				if (solidCheck.isSolid(x, y, getValue(x, y))) {
					temp.setValue(x, y, 1);
				}
			}
		}

		SolidCheck tempCheck = new SolidCheck() {
			@Override
			public boolean isSolid(int x, int y, int value) {
				return value == 1;
			}
		};
		IntList path = new IntList(true, getWidth() + getHeight());
		int[] steps = new int[1];
		// Second, scan
		for (int y = 0; y < getHeight(); y ++) {
			for (int x = 0; x < getWidth(); x ++) {
				if (!tempCheck.isSolid(x, y, temp.getValue(x, y))) {
					// Find a path to any base or spawnpoint. If successful, flood fill temp with 1 so we don't
					// scan any more in this area. If unsuccessful, also fill the real map with wall, overwriting
					// any other tiles.

					boolean found = false;
					// First the bases
					for (Iterator<Point> i = getBases().iterator(); i.hasNext(); ) {
						Point p = i.next();
						if (findPath(x, y, p.getX(), p.getY(), path, solidCheck, steps)) {
							// Yay! found a base.
							found = true;
							break;
						}
					}

					if (!found) {
						// Then spawnpoints
						for (Iterator<Point> i = getSpawnPoints().iterator(); i.hasNext(); ) {
							Point p = i.next();
							if (findPath(x, y, p.getX(), p.getY(), path, solidCheck, steps)) {
								// Yay! found a spawnpoint.
								found = true;
								break;
							}
						}
					}

					if (!found) {
						// Flood fill the real map with wall
						floodFill(getMap(), x, y, WALL, solidCheck);
					}

					// Flood fill temp with wall
					floodFill(temp, x, y, 1, tempCheck);
				}
			}
		}
	}

	private final void floodFill(IntGrid grid, int x, int y, int value, SolidCheck check) {
		if (check.isSolid(x, y, grid.getValue(x, y))) {
			return;
		} else {
			grid.setValue(x, y, value);
		}
		int[] xx = new int[grid.getWidth() * grid.getHeight() * 2];
		int[] yy = new int[grid.getWidth() * grid.getHeight() * 2];
		IntGrid visited = new IntGrid(grid.getWidth(), grid.getHeight(), 0);
		visited.setValue(x, y, 1);

		int size = 1;

		xx[0] = x;
		yy[0] = y;

		while (size > 0) {
			size --;
			int xxx = xx[size];
			int yyy = yy[size];
			assert visited.getValue(xxx, yyy) ==1;
			// Push empty neighbours
			if (xxx < grid.getWidth() - 1 && visited.getValue(xxx + 1, yyy) == 0 && !check.isSolid(xxx + 1, yyy, grid.getValue(xxx + 1, yyy))) {
				visited.setValue(xxx + 1, yyy, 1);
				xx[size] = xxx + 1;
				yy[size] = yyy;
				grid.setValue(xxx + 1, yyy, value);
				size ++;
			}
			if (xxx > 0 && visited.getValue(xxx - 1, yyy) == 0 && !check.isSolid(xxx - 1, yyy, grid.getValue(xxx - 1, yyy))) {
				visited.setValue(xxx - 1, yyy, 1);
				xx[size] = xxx - 1;
				yy[size] = yyy;
				grid.setValue(xxx - 1, yyy, value);
				size ++;
			}
			if (yyy < grid.getHeight() - 1 && visited.getValue(xxx, yyy + 1) == 0 && !check.isSolid(xxx, yyy + 1, grid.getValue(xxx, yyy + 1))) {
				visited.setValue(xxx, yyy + 1, 1);
				xx[size] = xxx;
				yy[size] = yyy + 1;
				grid.setValue(xxx, yyy + 1, value);
				size ++;
			}
			if (yyy > 0 && visited.getValue(xxx, yyy - 1) == 0 & !check.isSolid(xxx, yyy, grid.getValue(xxx, yyy - 1))) {
				visited.setValue(xxx, yyy - 1, 1);
				xx[size] = xxx;
				yy[size] = yyy - 1;
				grid.setValue(xxx, yyy - 1, value);
				size ++;
			}
		}

	}

	@Override
	protected final void generateSpawnPoints() {

		// Number of central spawn points to generate is up to (level - 50) / 10;
		// the remaining spawn points are edge ones.
		// No central spawn point may be placed within 5 squares of any base.

		// Choose formation
		int totalSpawners = levelFeature.getNumSpawnPoints();
		int totalMid = mapGeneratorParams.getGameMode() == WormGameState.GAME_MODE_CAMPAIGN ? Math.max(0, level - (MID_SPAWNERS_THRESHOLD - MID_SPAWNERS_DIVISOR)) / MID_SPAWNERS_DIVISOR
				: mapGeneratorParams.getGameMode() == WormGameState.GAME_MODE_ENDLESS ? (mapGeneratorParams.getWorldFeature().getIndex() > 1 ? Math.max(0, level - (MID_SPAWNERS_THRESHOLD - MID_SPAWNERS_DIVISOR)) / MID_SPAWNERS_DIVISOR : 0)
						: 0;
		int totalEdge = Math.max(0, totalSpawners - totalMid);
		assert totalSpawners > 0;
		assert totalEdge > 0;
		//System.out.println(totalEdge+", "+totalSpawners);
		for (int i = 0; i < totalEdge; i ++) {
			int x, y;
			do {
				// Pick a random edge
				int edge;

				// increasing probability as worlds progress that spawnpoints won't be at the biased edge...
				int bias = levelFeature.getBias();
				switch (bias) {
					case 0:
						// Util.random(0, 20) < level / WormGameState.LEVELS_IN_WORLD gives us a slowly increasing chance ranging from 0:20 to 4:20
						// of putting gidrahs on a non-biased edge
						if (Util.random(0, 20) < level / WormGameState.LEVELS_IN_WORLD) {
							edge = Util.random() < 0.5 ? 0 : 2;
						} else {
							edge = 3;
						}
						break;
					case 1:
						edge = Util.random() < 0.5 ? 2 : 3;
						break;

					case 2:
						if (Util.random(0, 20) < level / WormGameState.LEVELS_IN_WORLD) {
							edge = Util.random() < 0.5 ? 1 : 3;
						} else {
							edge = 2;
						}
						break;
					case 3:
						edge = Util.random() < 0.5 ? 1 : 2;
						break;

					case 4:
						if (Util.random(0, 20) < level / WormGameState.LEVELS_IN_WORLD) {
							edge = Util.random() < 0.5 ? 0 : 2;
						} else {
							edge = 1;
						}
						break;
					case 5:
						edge = Util.random() < 0.5 ? 0 : 1;
						break;

					case 6:
						if (Util.random(0, 20) < level / WormGameState.LEVELS_IN_WORLD && mapGeneratorParams.getGameMode() != WormGameState.GAME_MODE_XMAS) { // Xmas: always from the north.
							edge = Util.random() < 0.5 ? 1 : 3;
						} else {
							edge = 0;
						}
						break;
					case 7:
						edge = Util.random() < 0.5 ? 0 : 3;
						break;

					default:
						assert false;
					case -1:
						edge = Util.random(0, 3);
						break;
				}

				// Pick a point along the edge, excluding the corners
				switch (edge) {
					case 0: // NORTH
						if (bias == 1 || bias == 0 || bias == 7) {
							x = Util.random(1, getWidth() / 2 - 2);
						} else if (bias == 3 || bias == 4 || bias== 5) {
							x = Util.random(getWidth() / 2 + 2, getWidth() - 2);
						} else {
							x = Util.random(1, getWidth() - 2);
						}
						y = getHeight() - 1;
						break;
					case 1: // EAST
						x = getWidth() - 1;
						if (bias == 5 || bias == 6 || bias == 7) {
							y = Util.random(getHeight() / 2 + 2, getHeight() - 2);
						} else if (bias == 1 || bias == 2 || bias== 3) {
							y = Util.random(1, getHeight() / 2 - 2);
						} else {
							y = Util.random(1, getHeight() - 2);
						}
						break;
					case 2: // SOUTH
						if (bias == 1 || bias == 0 || bias == 7) {
							x = Util.random(1, getWidth() / 2 - 2);
						} else if (bias == 3 || bias == 4 || bias== 5) {
							x = Util.random(getWidth() / 2 + 2, getWidth() - 2);
						} else {
							x = Util.random(1, getWidth() - 2);
						}
						y = 0;
						break;
					case 3: // WEST
						x = 0;
						if (bias == 5 || bias == 6 || bias == 7) {
							y = Util.random(getHeight() / 2 + 2, getHeight() - 2);
						} else if (bias == 1 || bias == 2 || bias== 3) {
							y = Util.random(1, getHeight() / 2 - 2);
						} else {
							y = Util.random(1, getHeight() - 2);
						}
						break;
					default:
						assert false;
						x = 0;
						y = 0;
				}
			} while (getValue(x, y) == SPAWN);

			// Carve a line from the spawnpoint to the nearest base, until we reach FLOOR.
			ReadablePoint p = getClosestBase(x, y);
			drawLineWithStop(x, y, p.getX(), p.getY(), getMinSpawnTunnelSize(), getMaxSpawnTunnelSize());


			addSpawnPoint(x, y);
		}

		// Minimum range to spawn point from a base:
		int minRange = (int) LinearInterpolator.instance.interpolate(Math.max(getWidth(), getHeight()) / 2, MIN_BASE_RANGE_TO_SPAWNPOINT, getLevel() / 50.0f);
		for (int i = 0; i < totalMid; i ++) {
			// Pick a random point
			int x = -1, y = -1;
			boolean ok = false;
			again: for (int attempts = 0; attempts < MAX_ATTEMPTS; attempts ++) {
				Point basePos = getBases().get(0);
				double angle = Util.random() * Math.PI * 2.0;
				double distance = Util.random(minRange, Math.max(minRange, (int) (minRange * (2.0 - Worm.getGameState().getBasicDifficulty()))));
				x = (int) (basePos.getX() + Math.cos(angle) * distance);
				y = (int) (basePos.getY() + Math.sin(angle) * distance);
				if (x < SPAWN_POINT_MARGIN || y < SPAWN_POINT_MARGIN || x >= getWidth() - SPAWN_POINT_MARGIN || y >= getHeight() - SPAWN_POINT_MARGIN) {
					continue;
				}
//				x = Util.random(SPAWN_POINT_MARGIN, getWidth() - SPAWN_POINT_MARGIN - 1);
//				y = Util.random(SPAWN_POINT_MARGIN, getHeight() - SPAWN_POINT_MARGIN - 1);

				// Ensure it's clear
				if (getValue(x, y) != FLOOR) {
					continue;
				}

				// Ensure not proximal to a wall
				for (int xx = x - 1; xx <= x + 1; xx ++) {
					for (int yy = y - 1; yy <= y + 1; yy ++) {
						if (getValue(xx, yy) == WALL) {
							continue again;
						}
					}
				}

				ok = true;
				break;
			}
			if (ok) {
				addSpawnPoint(x, y);
			}
		}
	}

	protected final void drawLine(int sx, int sy, int tx, int ty, float minSize, float maxSize, int value) {
		Bresenham bh = new Bresenham();
		float startSize = Util.random() * (maxSize - minSize) + minSize;
		float endSize = Util.random() * (maxSize - minSize) + minSize;
		float steps = bh.plot(sx, sy, tx, ty);
		for (int step = 0; bh.next(); step ++) {
			int x = bh.getX();
			int y = bh.getY();
			drawArea(x, y, LinearInterpolator.instance.interpolate(startSize, endSize, step / steps), value);
		}
	}

	static final Interpolator INTERPOLATOR;
	static {
		INTERPOLATOR = new Interpolator() {
			private static final long serialVersionUID = 1L;

			@Override
			public float interpolate(float a, float b, float ratio) {
				if (ratio < 0.5f) {
					return SineInterpolator.instance.interpolate(a, b, ratio * 2.0f);
				} else {
					return SineInterpolator.instance.interpolate(b, a, (ratio - 0.5f) * 2.0f);
				}
			}
		};
	}

	protected final void drawLineWithStop(int sx, int sy, int tx, int ty, float minSize, float maxSize) {
		Bresenham bh = new Bresenham();
		bh.plot(sx, sy, tx, ty);
		for (int step = 0; bh.next(); step ++) {
			int x = bh.getX();
			int y = bh.getY();
			if (getValue(x, y) == FLOOR) {
				tx = x;
				ty = y;
				break;
			}
		}

		// Randomly wiggle between sx, sy and tx, ty, varying size as we go. If we encounter FLOOR, we're done, and we can then
		// go and carve floor on the points we traversed.
		List<Point> points = new ArrayList<Point>(Math.abs(tx - sx) + Math.abs(ty - sy));

		double x = sx;
		double y = sy;

		points.add(new Point(sx, sy));
		double sdx = tx - sx;
		double sdy = ty - sy;
		double originalDistance = Math.sqrt(sdx * sdx + sdy * sdy);
		double currentAngle = Math.atan2(sdy, sdx);
		while (Math.abs(x - tx) > 1 || Math.abs(y - ty) > 1) {
			// Get angle to destination from where we are...
			double targetAngle = Math.atan2(ty - y, tx - x);

			// Deviation of this angle based on distance. When very far from or very near the destination, deviation is very small;
			// deviation is at its greatest when precisely halfway.
			double dx = x - tx;
			double dy = y - ty;
			double distance = Math.sqrt(dx * dx + dy * dy);
			double ratio = distance / originalDistance;

			// Current angle meanders all over the place...
			currentAngle += Util.random() * Math.PI / 2.0 - Math.PI / 4.0;

			// Now: become more and more like the direct angle, based on distance
			double difference = Util.getAngleDifference(currentAngle, targetAngle);

			double moveAngle = Util.moveToAngle((float) currentAngle, (float) targetAngle, INTERPOLATOR.interpolate((float) difference, 0.0f, (float) ratio));

			// Move in this direction
			int oldx = (int) x;
			int oldy = (int) y;
			x += Math.cos(moveAngle);
			y += Math.sin(moveAngle);
			if (oldx == x && oldy == y) {
				continue;
			}
			Point p = new Point((int) x, (int) y);
			int idx = points.indexOf(p);
			if (idx == -1) {
				points.add(p);
			} else {
				points = new ArrayList<Point>(points.subList(0, idx));
			}

			// Hit floor? We're done
			if (getValue((int) x, (int) y) == FLOOR) {
				break;
			}
		}

		float size = Util.random();
		for (Iterator<Point> i = points.iterator(); i.hasNext(); ) {
			ReadablePoint p = i.next();
			drawArea(p.getX(), p.getY(), LinearInterpolator.instance.interpolate(minSize, maxSize, size), FLOOR);
			int d = Util.random(0, 4);
			if (d == 0) {
				size = Math.max(minSize, size - 0.125f);
			} else if (d == 1) {
				size = Math.min(maxSize, size + 0.125f);
			}
		}
	}

	protected final void drawArea(int x, int y, float radius, int value) {
		for (int yy = (int) (y - radius); yy <= (int) (y + radius); yy ++) {
			for (int xx = (int) (x - radius); xx <= (int) (x + radius); xx ++) {
				float dx = xx - x;
				float dy = yy - y;
				if (Math.sqrt(dx * dx + dy * dy) <= radius && getValue(xx, yy) != BASE && getValue(xx, yy) != SPAWN) {
					setValue(xx, yy, value);
				}
			}
		}
	}

	@Override
	protected void generateObstacles() {
		// Just a random number of obstacles.
		int numObstacles = Util.random(scenery.getMinObstacles(), scenery.getMaxObstacles());
		for (int i = 0; i < numObstacles; i ++) {
			int x = Util.random(OBSTACLE_MARGIN, getWidth() - OBSTACLE_MARGIN - 1);
			int y = Util.random(OBSTACLE_MARGIN, getHeight() - OBSTACLE_MARGIN - 1);
			if (getValue(x, y) == FLOOR) {
				setValue(x, y, OBSTACLE);
			}
		}
	}

	@Override
	protected void generateRuins() {
		totalRuins = 0;
		int numRuinAreas = Util.random(0, scenery.getMaxRuinClusters());
		for (int i = 0; i < numRuinAreas; i ++) {
			int x = Util.random(0, getWidth());
			int y = Util.random(0, getHeight());
			float radius = Util.random() * getAreaScale() * (scenery.getMaxRuinClusterSize() - scenery.getMinRuinClusterSize()) + getAreaScale() * scenery.getMinRuinClusterSize();
			generateRuinArea(x, y, radius);
			if (totalRuins >= (int) (scenery.getAbsMaxRuins() * getAreaScale())) {
				return;
			}
		}
	}

	/**
	 * Generates an area of ruins
	 * @param x
	 * @param y
	 * @param radius
	 */
	private void generateRuinArea(int x, int y, float radius) {
		int numRuins = Util.random((int) radius, (int) (scenery.getMaxRuins() * getAreaScale()));
		outer: for (int i = 0; i < numRuins; i ++) {
			// Pick a random point inside the radius...
			double angle = Util.random() * Math.PI * 2.0;
			double distance = Util.random() * radius;
			int xx = x + (int) (Math.cos(angle) * distance);
			int yy = y + (int) (Math.sin(angle) * distance);

			// Size is dependent on ratio of distance from radius
			int size = (int) LinearInterpolator.instance.interpolate(4.9f, 1.0f, (float) distance / radius);
			int width = 0;
			int height = 0;
			Ruin ruin = null;
			for (int j = 0; j < 10; j ++) {
				width = Util.random(1, size);
				height = Util.random(1, size);
				if (height > width) {
					int temp = width;
					width = height;
					height = temp;
				}
				ruin = scenery.getRuin(width, height);
				if (ruin == null) {
					continue;
				}
			}
			if (ruin == null) {
				continue;
			}

			// Ensure that we're clear to fit it in
			for (int yyy = yy; yyy < yy + height; yyy ++) {
				for (int xxx = xx; xxx < xx + width; xxx ++) {
					if (getValue(xxx, yyy) != FLOOR) {
						continue outer;
					}
				}
			}

			// Also ensure we're not within 5 squares of a base
			for (Iterator<Point> j = getBases().iterator(); j.hasNext(); ) {
				Point base = j.next();
				int dx = base.getX() - xx;
				int dy = base.getY() - yy;
				if (Math.sqrt(dx * dx + dy * dy) < 5.0) {
					continue outer;
				}
			}

			// Ok, write ruin
			addRuin(xx, yy, width, height);
			totalRuins ++;
			if (totalRuins == (int) (scenery.getAbsMaxRuins() * getAreaScale())) {
				return;
			}
		}
	}

	@Override
	protected void generateCrystals() {
		totalCrystals = 0;
		if (Game.DEBUG) {
			System.out.println("Generating "+getNumCrystalsToCreate()+" crystals: min distance "+getCrystalMinDistance(1)+", max distance "+getCrystalMaxDistance(3));
		}
		while (totalCrystals < getNumCrystalsToCreate()) {
			// pick a base
			//System.out.println("Total crystals now "+totalCrystals+" of "+getNumCrystalsToCreate());
			List<Point> bases = getBases();
			Point base = bases.get(Util.random(0, bases.size() - 1));
			// Choose a distance from the base, higher the level, further away
			int x, y;
			int num = getNumCrystalsToCreate() - totalCrystals;
			int size = Math.min(Math.min(3, Util.random(1, 3 + num / 5)), num); // Bias toward larger crystals
			do {
				// larger crystals more likely to be further away.
				// Also take an average of 2 readings to get a bell shaped distribution - not too far, too too close
				float rand0 = Util.random(), rand1 = Util.random();
				Interpolator i;
				switch (size) {
					case 1:
						i = CosineInterpolator.instance;
						break;
					case 2:
						i = LinearInterpolator.instance;
						break;
					case 3:
						i = SineInterpolator.instance;
						break;
					default:
						assert false;
						return;
				}
				rand0 = i.interpolate(0.0f, 1.0f, rand0);
				rand1 = i.interpolate(0.0f, 1.0f, rand1);

				double dist =
						(
							LinearInterpolator.instance.interpolate(getCrystalMinDistance(size), getCrystalMaxDistance(size), rand0)
						+ 	LinearInterpolator.instance.interpolate(getCrystalMinDistance(size), getCrystalMaxDistance(size), rand1)
						) / 2.0;
				double angle = Util.random() * Math.PI * 2.0;
				x = base.getX() + (int) (Math.cos(angle) * dist);
				y = base.getY() + (int) (Math.sin(angle) * dist);
			} while (x < 5 || y < 5 || x >= getWidth() - 5 || y >= getHeight() - 5);
			generateCrystal(x, y, size);
		}
	}

	private int getCrystalMinDistance(int size) {
		return (int) LinearInterpolator.instance.interpolate(MIN_CRYSTAL_DISTANCE + size, MIN_CRYSTAL_DISTANCE * size + size, mapGeneratorParams.getBasicDifficulty());
	}

	private int getCrystalMaxDistance(int size) {
		int crystalMinDistance = (int) (getCrystalMinDistance(size) * 1.5f);
		return (int) LinearInterpolator.instance.interpolate(crystalMinDistance, Math.max(crystalMinDistance, Math.max(getHeight(), getWidth()) * 0.7f), mapGeneratorParams.getBasicDifficulty());
	}

	/**
	 * Generates a crystal
	 * @param x
	 * @param y
	 */
	private void generateCrystal(int x, int y, int size) {
		Crystal crystal = null;
		for (int j = 0; j < 10; j ++) {
			crystal = scenery.getCrystal(size);
			if (crystal == null) {
				if (Game.DEBUG) {
					System.out.println("Warning: no crystal of size "+size+" available");
				}
				continue;
			}
		}
		if (crystal == null) {
			return;
		}

		// Ensure that we're clear to fit it in
		int cw = crystal.getWidth();
		int ch = crystal.getHeight();
		for (int yyy = y; yyy < y + ch; yyy ++) {
			for (int xxx = x; xxx < x + cw; xxx ++) {
				if (getValue(xxx, yyy) != FLOOR || roadsOverlay.getValue(xxx, yyy) != 0) {
					return;
				}
			}
		}

		// Also ensure we're not within 4 squares of a base
		for (Iterator<Point> j = getBases().iterator(); j.hasNext(); ) {
			Point base = j.next();
			int dx = base.getX() + 2 - x;
			int dy = base.getY() + 1 - y;
			if (Math.sqrt(dx * dx + dy * dy) < 4.0) {
				return;
			}
		}

		// Ok, write crystal
		if (addCrystal(x, y, size)) {
			totalCrystals += size;
			if (totalCrystals >= getNumCrystalsToCreate()) {
				return;
			}
		}
	}

	protected int getNumCrystalsToCreate() {
		int num;
		if (level == -1) {
			// Survival mode
			return BASE_CRYSTALS_SURVIVAL;
		} else if (level == 0) {
			return 0; // No crystals on first level
		} else {
			num = BASE_CRYSTALS;
			if (mapGeneratorParams.getLevelFeature().getBosses() != null) {
				num += EXTRA_CRYSTALS_BOSS + mapGeneratorParams.getLevelFeature().getBosses().getNumResources();
			}
			if (levelFeature.getBias() == -1) {
				num += EXTRA_CRYSTALS_CENTRAL;
			}
		}
		int money = mapGeneratorParams.getMoney();
		float ratio = money - MAX_MONEY > 0 ? Math.min(ABS_MAX_MONEY, (money - MAX_MONEY)) / ABS_MAX_MONEY : 0.0f;
		int worldIndex = level / WormGameState.LEVELS_IN_WORLD;
		float baseCrystals = BASE_CRYSTALS_PER_LEVEL * level + num + worldIndex * BASE_CRYSTALS_PER_WORLD;

		return (int) CosineInterpolator.instance.interpolate(baseCrystals, 1.0f, ratio);
	}

	protected float getAreaScale() {
		return (float) Math.sqrt(getWidth() * getHeight()) / LevelFeature.MIN_SIZE;
	}

	@Override
	protected void generateRoads() {
		// Get a collection of base coordinates and ruin coordinates and edges-of-map coordinates,
		// then randomly join them up.

		List<Point> points = new LinkedList<Point>();

		// First get bases...
		List<Point> bases = getBases();
		// Offset roads by random amounts
		Point basePoint = null;
		for (Iterator<Point> i = bases.iterator(); i.hasNext(); ) {
			Point p = i.next();
			points.add(basePoint = new Point(p.getX() + Util.random(0, 3), p.getY() + 1));
		}

		// Now the ruins...
		Map<Point, Ruin> ruins = getRuins();
		int numRuins = 0;
		for (Entry<Point, Ruin> entry : ruins.entrySet()) {
			Point p = entry.getKey();
			Ruin r = entry.getValue();
			if (r.getRoads()) {
				// Choose a spawn point to draw the road to
				points.add(new Point(p.getX() + Util.random(-1, r.getWidth()), p.getY() + Util.random(-1, r.getHeight())));
				numRuins ++;
				//System.out.println("ruins to draw roads from = "+numRuins);
			} else {
				//System.out.println("No road to this ruin "+p+","+r);
			}
		}

		// Add spawnpoints
		List<Point> spawnPoints = new ArrayList<Point>(getSpawnPoints());
		for (Iterator<Point> i = spawnPoints.iterator(); i.hasNext(); ) {
			Point spawnPoint = i.next();
			points.add(spawnPoint);
		}


		float startProgress = getProgress();

		// Now randomly join up stuff
		int[] steps = new int[1];

		// Ensure at least 1 road from base to a spawnpoint
		for (int i = 0; i < 1 + level / WormGameState.LEVELS_IN_WORLD && spawnPoints.size() > 0; i ++) {
			int idx = Util.random(0, spawnPoints.size() - 1);
			Point spawnPoint = spawnPoints.remove(idx);
			drawRoad(basePoint.getX(), basePoint.getY(), spawnPoint.getX(), spawnPoint.getY(), false, steps);
		}

		int edges = Util.random(1, (bases.size() + numRuins) / 2);
		if (points.size() + edges - 2 == 0) {
			// No roads
			return;
		}
		Collections.shuffle(points);
		for (int i = 0; i < points.size() - 1; i ++) {
			Point start = points.get(i);
			Point end = points.get(i + 1);

			boolean ok = drawRoad(start.getX(), start.getY(), end.getX(), end.getY(), false, steps);
			setProgress(LinearInterpolator.instance.interpolate(startProgress, ROADS_COMPLETED_PROGRESS, (float) i / (points.size() + edges - 2)));
			if (!ok) {// || Util.random() < 0.2) {
				// Break the road here
				i ++;
			}
			if (steps[0] > getMaxRoadSteps()) {
				// That's enough roads
				break;
			}
		}

//
//		startProgress = getProgress();
//		// Now pick some random spawn points
//		for (int i = 0; i < edges; i ++) {
//			int tx, ty;
//			switch (Util.random(0, 3)) {
//				case 0: // North
//					tx = Util.random(0, getWidth() / 2) + getWidth() / 2;
//					ty = 0;
//					break;
//				case 1: // South
//					tx = Util.random(0, getWidth() / 2) + getWidth() / 2;
//					ty = getHeight() - 1;
//					break;
//				case 2: // West
//					tx = 0;
//					ty = Util.random(0, getHeight() / 2) + getHeight() / 2;
//					break;
//				case 3: // East
//					tx = getWidth() - 1;
//					ty = Util.random(0, getHeight() / 2) + getHeight() / 2;
//					break;
//				default:
//					assert false;
//					return;
//			}
//
//			// And randomly join up things to the edges
//			if (getValue(tx, ty) == FLOOR) {
//				Point s = (Point) points.get(Util.random(0, points.size() - 1));
//				drawRoad(s.getX(), s.getY(), tx, ty, true, steps);
//			}
//			setProgress(LinearInterpolator.instance.interpolate(startProgress, ROADS_COMPLETED_PROGRESS, (float) i / (edges - 1)));
//			if (steps[0] > getMaxRoadSteps()) {
//				// That's enough roads
//				break;
//			}
//		}

	}

	private int getMaxRoadSteps() {
		return getWidth() * getHeight() * 10;
	}

	private boolean drawRoad(int sx, int sy, int tx, int ty, final boolean stopAtEdge, int[] steps) {

		sx /= 2;
		sx *= 2;
		sy /= 2;
		sy *= 2;
		tx /= 2;
		tx *= 2;
		ty /= 2;
		ty *= 2;


		IntList path = new IntList(getWidth() + getHeight());
		path.add(IntGridTopology.pack(sx, sy));
		if (!findPath(sx, sy, tx, ty, path, new Topology() {

			@Override
			public int getWidth() {
				return BaseMapGenerator.this.getWidth();
			}
			@Override
			public int getHeight() {
				return BaseMapGenerator.this.getHeight();
			}
			@Override
			public int getCost(int from, int to) {
				return 1;
			}

			@Override
			public int getDistance(int from, int to) {
				int fromX = IntGridTopology.getX(from);
				int fromY = IntGridTopology.getY(from);
				int toX = IntGridTopology.getX(to);
				int toY = IntGridTopology.getY(to);
				// Scale it a bit
				int h = Math.abs(fromX - toX) + Math.abs(fromY - toY);
				h *= 1.0f + 1.0f / WormGameState.ABS_MAX_SIZE;
				return h;
			}

			@Override
			public void getNeighbours(int node, int parent, IntList dest) {
				dest.clear();

				int x = IntGridTopology.getX(node);
				int y = IntGridTopology.getY(node);

				boolean lrOK, udOK;
				if (x % 2 == 1 && y % 2 == 0) {
					// Only left and right possible
					lrOK = true;
					udOK = false;
				} else if (x % 2 == 0 && y % 2 == 1) {
					// Only up and down possible
					lrOK = false;
					udOK = true;
				} else {
					lrOK = true;
					udOK = true;
				}

				int n = IntGridTopology.pack(x, y + 1);
				int v = getMap().getValue(x, y + 1);
				if (n != parent && udOK && !isSolid(x, y + 1, v)) {
					dest.add(n);
				}
				n = IntGridTopology.pack(x, y - 1);
				v = getMap().getValue(x, y - 1);
				if (n != parent && udOK && !isSolid(x, y - 1, v)) {
					dest.add(n);
				}
				n = IntGridTopology.pack(x + 1, y);
				v = getMap().getValue(x + 1, y);
				if (n != parent && lrOK && !isSolid(x + 1, y, v)) {
					dest.add(n);
				}
				n = IntGridTopology.pack(x - 1, y);
				v = getMap().getValue(x - 1, y);
				if (n != parent && lrOK && !isSolid(x - 1, y, v)) {
					dest.add(n);
				}
			}

			boolean isSolid(int x, int y, int value) {
				return x < 0 || y < 0 || x > getWidth() - 1 || y > getHeight() - 1 || value == WALL || value == WALLEDGE || value == RUIN || value == CRYSTAL || value == RUIN_IMPASSABLE || value == IMPASSABLE || value == SPECIAL_IMPASSABLE;
			}

		}, steps))
		{
//			System.out.println("Failed to plot road between "+sx+","+sy+","+tx+","+ty);
			return false;
		}

//		System.out.println("Plot road between "+sx+","+sy+","+tx+","+ty+":"+path.size()+" steps");
		// Follow the path
		for (int i = 0; i < path.size(); i ++) {
			int p = path.get(i);
			int x = IntGridTopology.getX(p);
			int y = IntGridTopology.getY(p);
			if (getRoadsOverlay().getValue(x, y) == 1) {
				break;
			}
			getRoadsOverlay().setValue(x, y, 1);
			if (stopAtEdge && (x == 0 || y == 0 || x == getWidth() - 1 || y == getHeight() -1)) {
				break;
			}
		}
		getRoadsOverlay().setValue(sx, sy, 1);

		return true;
	}

}
