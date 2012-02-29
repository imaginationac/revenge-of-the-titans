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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.puppygames.applet.Game;
import net.puppygames.applet.GameInputStream;
import net.puppygames.applet.GameOutputStream;
import net.puppygames.applet.RoamingFile;

import org.lwjgl.util.Point;
import org.lwjgl.util.ReadablePoint;

import worm.GameMap;
import worm.IntGrid;
import worm.Res;
import worm.Tile;
import worm.Worm;
import worm.WormGameState;
import worm.features.LevelFeature;
import worm.path.AStar;
import worm.path.Topology;
import worm.tiles.Crystal;
import worm.tiles.Exclude;
import worm.tiles.Ruin;
import worm.tiles.TotalExclude;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.util.IntList;
import com.shavenpuppy.jglib.util.Util;

/**
 * Base class for {@link MapGenerator}s
 */
abstract class AbstractMapGenerator implements MapGenerator, SimpleTiles {

	protected static final float ROADS_COMPLETED_PROGRESS = 0.5f;

	/* chaz hack - amount of random tile 2 */
	private static final double RANDOM_ROCKY_THRESHOLD = 0.05;

	/** Bucket of common properties for generator classes */
	protected final MapGeneratorParams mapGeneratorParams;

	/** The template; this provides tile sets for various features */
	protected final MapTemplate template;

	/** The scenery */
	protected final Scenery scenery;

	/** The level which we're going to make maps for */
	protected final int level;

	/** The level-in-world which we're going to make maps for */
	protected final int levelInWorld;

	/** The levelfeature itself */
	protected final LevelFeature levelFeature;

	/** The spawnpoints */
	private final List<Point> spawnPoints = new ArrayList<Point>();

	/** The bases */
	private final List<Point> bases = new ArrayList<Point>();

	/** Ruins (map of Points to Ruins) */
	private final Map<Point, Ruin> ruins = new HashMap<Point, Ruin>();

	/** Ruins (map of Points to Crystals) */
	private final Map<Point, Crystal> crystals = new HashMap<Point, Crystal>();

	/** A much simplified version of the map */
	private IntGrid map;

	/** Roads overlay */
	protected IntGrid roadsOverlay;

	/** Topology used for pathfinding */
	private IntGridTopology topology;

	private boolean test;

	/** Progress */
	private float progress;

	/** Abort */
	private boolean abort;

	/** Formation */
	protected char[] formation;

	/** Duff attempts so far */
	private int duff;

	/**
	 * C'tor
	 * @param template
	 * @param level
	 * @param levelInWorld
	 * @param levelFeature
	 */
	public AbstractMapGenerator(MapTemplate template, MapGeneratorParams mapGeneratorParams) {
		this.mapGeneratorParams = mapGeneratorParams;
		this.template = template;
		this.level = mapGeneratorParams.getLevel();
		this.levelInWorld = mapGeneratorParams.getLevelInWorld();
		this.levelFeature = mapGeneratorParams.getLevelFeature();
		this.scenery = levelFeature.getScenery();

		if (mapGeneratorParams.getGameMode() != WormGameState.GAME_MODE_SURVIVAL && mapGeneratorParams.getGameMode() != WormGameState.GAME_MODE_XMAS) {
			Util.setSeed(getSeed());
		}
	}

	@Override
	public void finish() {
		abort = true;
	}

	/**
	 * @return the map
	 */
	protected final IntGrid getMap() {
		return map;
	}

	/**
	 * @param test the test to set
	 */
	public void setTest(boolean test) {
		this.test = test;
	}

	/**
	 * @return the level
	 */
	public final int getLevel() {
		return level;
	}

	/**
	 * @return the levelInWorld
	 */
	public final int getLevelInWorld() {
		return levelInWorld;
	}

	/**
	 * @return the template
	 */
	public final MapTemplate getTemplate() {
		return template;
	}

	/**
	 * @return the map width
	 */
	protected int getWidth() {
		return levelFeature.getWidth();
	}

	/**
	 * @return the map height
	 */
	protected int getHeight() {
		return levelFeature.getHeight();
	}

	/**
	 * @return the roads overlay
	 */
	public final IntGrid getRoadsOverlay() {
		return roadsOverlay;
	}

	/**
	 * Update the progress
	 * @param newProgress
	 * @throws GenerationAbortedException if the generator has been aborted
	 */
	protected final void setProgress(float newProgress) throws GenerationAbortedException {
		this.progress = newProgress;
		if (abort) {
			throw new GenerationAbortedException();
		}
		Thread.yield();
	}

	protected final long getSeed() {
		String levelName = levelFeature.getTitle();
		int gameMode = mapGeneratorParams.getGameMode();
		duff = Worm.getExtraLevelData(Game.getPlayerSlot(), level, gameMode, "duff_" + levelName, 0);
		long unique = Game.getPlayerSlot().getPreferences().getLong("unique_"+gameMode, 0L);
		if (unique == 0L) {
			unique = new Random().nextLong();
			Game.getPlayerSlot().getPreferences().putLong("unique_"+gameMode, unique);
			Game.flushPrefs();
		}
		long seed =
					((Game.getPlayerSlot().getName().hashCode() + duff + WormGameState.getDifficultyAdjust(level, gameMode)))
				|
					(((long) Float.floatToRawIntBits(mapGeneratorParams.getBasicDifficulty())) << 32L);

		seed ^= mapGeneratorParams.getResearchHash();
		seed ^= unique;
		return seed;
	}

	private String getFileName() {
		String levelName = levelFeature.getTitle();
		int gameMode = mapGeneratorParams.getGameMode();
		long seed = getSeed();
		return Game.getPlayerDirectoryPrefix()+levelName.replace(' ', '_')+"_"+gameMode+"_"+Long.toHexString(seed)+".dat";
	}

	private boolean exists() {
		return level != -1 && new RoamingFile(getFileName()).exists();
	}

	/**
	 * Load the map based on seed
	 * @throws IOException
	 */
	private GameMap load() throws IOException {
		if (!exists()) {
			throw new IOException(getFileName()+" does not exist");
		}
		GameInputStream gis = null;
		ObjectInputStream ois = null;
		GameMap ret = null;
		try {
			gis = new GameInputStream(getFileName());
			ois = new ObjectInputStream(gis);

			ret = (GameMap) ois.readObject();
			return ret;
		} catch (Exception e) {
			throw new IOException("Couldn't load map due to "+e);

		} finally {
			try {
				if (gis != null) {
					gis.close();
				}
			} catch (IOException e) {
			}
		}
	}

	private void save(GameMap gameMap) throws IOException {
		GameOutputStream gos = null;
		ObjectOutputStream oos = null;
		try {
			gos = new GameOutputStream(getFileName());
			oos = new ObjectOutputStream(gos);

			oos.writeObject(gameMap);
			oos.flush();
		} finally {
			try {
				if (gos != null) {
					gos.close();
				}
			} catch (IOException e) {
			}
		}
	}


	@Override
	public GameMap generate() {
		try {
			if (exists()) {
				try {
					return load();
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
			}

			boolean valid = false;
			while (!valid) {
				map = null;
				map = new IntGrid(getWidth(), getHeight(), SimpleTiles.EMPTY);
				roadsOverlay = new IntGrid(getWidth(), getHeight(), 0);
				spawnPoints.clear();
				bases.clear();
				ruins.clear();
				crystals.clear();
				topology = new IntGridTopology(map);

				// Alien formations. Let's randomize the order
				formation = levelFeature.getFormation().toCharArray();
				for (int i = 0; i < formation.length; i ++) {
					int idx = Util.random(0, formation.length - 1);
					char c = formation[i];
					formation[i] = formation[idx];
					formation[idx] = c;
				}

				setProgress(0.0f);

				generateBases();
				setProgress(0.05f);

				generateAreas();
				setProgress(0.1f);

				clean();
				setProgress(0.2f);

				generateRuins();
				setProgress(0.35f);

				generateSpawnPoints();
				setProgress(0.45f);

				checkRuins();
				setProgress(0.47f);

				generateRoads();
				setProgress(ROADS_COMPLETED_PROGRESS);

				generateObstacles();
				setProgress(0.6f);

				valid = isValid();
				if (!isValid()) {
					duff ++;
					Worm.setExtraLevelData(level, Worm.getGameState().getGameMode(), "duff_"+levelFeature.getTitle(), duff);
				}
			}

			if (test) {
				prebuild(new GameMap(map.getWidth(), map.getHeight(), template.getFill()));
				dump();
				return null;
			} else {
				return build();
			}
		} catch (GenerationAbortedException e) {
			return null;
		}
	}

	/**
	 * @return the progress of the map g	eneration
	 */
	@Override
	public float getProgress() {
		return progress;
	}

	/**
	 * Access the map
	 * @param x
	 * @param y
	 * @return
	 */
	protected final int getValue(int x, int y) {
		return map.getValue(x, y);
	}

	/**
	 * Access the map
	 * @param x
	 * @param y
	 * @param newValue
	 */
	protected final void setValue(int x, int y, int newValue) {
		map.setValue(x, y, newValue);
	}

	/**
	 * @return the bases
	 */
	public List<Point> getBases() {
		return Collections.unmodifiableList(bases);
	}

	/**
	 * @return the spawn points
	 */
	public List<Point> getSpawnPoints() {
		return Collections.unmodifiableList(spawnPoints);
	}

	/**
	 * Calculates the distance to the closest base from the specified coordinates
	 * @param x
	 * @param y
	 * @return distance, &ge; 0.0f
	 */
	protected final float getDistanceToClosestBase(int x, int y) {
		ReadablePoint p = getClosestBase(x, y);
		float dx = x - p.getX();
		float dy = y - p.getY();
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	protected ReadablePoint getClosestBase(int x, int y) {
		Point nearestPoint = null;
		float nearest = Float.MAX_VALUE;
		for (int i = 0; i < bases.size(); i ++) {
			Point p = bases.get(i);
			float dx = x - p.getX();
			float dy = y - p.getY();
			float dist = (float) Math.sqrt(dx * dx + dy * dy);
			if (dist < nearest) {
				nearest = dist;
				nearestPoint = p;
			}
		}

		return new Point(nearestPoint);
	}

	/**
	 * Specifies the largely passable or impassable terrain areas, either by carving holes in solid maps, or adding
	 * walls in empty maps. After this method is called, no map tile should be EMPTY.
	 */
	protected abstract void generateAreas();

	/**
	 * Places a number of bases at random locations around the map. Use {@link #addBase(int, int)}
	 */
	protected abstract void generateBases();

	/**
	 * Places a number of alien spawn points at random locations around the map. Use {@link #addSpawnPoint(int, int)}
	 */
	protected abstract void generateSpawnPoints();

	/**
	 * Generates a random road network, and joins bases together with roads.
	 */
	protected abstract void generateRoads();

	/**
	 * Place a few random obstacles around on the map
	 */
	protected abstract void generateObstacles();

	/**
	 * Place some ruins on the map
	 */
	protected abstract void generateRuins();

	/**
	 * Place some crystals on the map
	 */
	protected abstract void generateCrystals();

	/**
	 * Adds a base at the specified location
	 * @param x
	 * @param y
	 */
	protected final void addBase(int x, int y) {
		assert x >= 0 && y >= 0 && x < getWidth() - 2 && y < getHeight() - 2 : "Attempt to add base at "+x+","+y+" (w x h ="+getWidth()+"x"+getHeight()+")";
		for (int yy = 0; yy < 3; yy ++) {
			for (int xx = 0; xx < 4; xx ++) {
				map.setValue(x + xx, y + yy, BASE);
			}
		}
		//System.out.println("Base added at "+x+","+y);
		bases.add(new Point(x, y));
	}

	/**
	 * Add a ruin
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	protected final void addRuin(int x, int y, int w, int h) {
		// Pick a ruin from the template at this point
		Ruin ruin = scenery.getRuin(w, h);
		if (ruin == null) {
			return;
		}

		// chaz hack - added bThruMap stuff...
		//System.out.println("Ruin:"+ruin.getName()+" "+w+"x"+h+" added at "+x+","+y);

		String bThruMap = ruin.getBThruMap();
		//if (bThruMap!=null) System.out.println("   bThruMap:"+bThruMap);

		for (int yy = 0; yy < h; yy ++) {
			for (int xx = 0; xx < w; xx ++) {
				//assert map.getValue(x + xx, y + yy) == FLOOR : "Argh! Map is "+map.getValue(x + xx, y + yy)+" not FLOOR!";

				int val;
				if (bThruMap != null) {
					val = bThruMap.charAt(xx + yy * w) == '0' ? (ruin.getRoads() ? IMPASSABLE : SPECIAL_IMPASSABLE) : TOTAL_IMPASSABLE;
				} else {
					val = ruin.isBulletThrough() ? (ruin.getRoads() ? IMPASSABLE : SPECIAL_IMPASSABLE) : TOTAL_IMPASSABLE;
				}

				map.setValue(x + xx, y + yy, val);
				//System.out.println("   add impassable: "+(x + xx)+", "+(y + yy)+": "+val);
			}
		}

		map.setValue(x, y, ruin.getRoads() || bThruMap != null && bThruMap.charAt(0) == '0' || ruin.isBulletThrough() ? RUIN : RUIN_IMPASSABLE);
		ruins.put(new Point(x, y), ruin);

	}

	/**
	 * Checks each ruin is still "intact" on the map. If not, the ruin is removed.
	 */
	private void checkRuins() {
		for (Iterator<Map.Entry<Point, Ruin>> i = ruins.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<Point, Ruin> entry = i.next();
			ReadablePoint p = entry.getKey();
			Ruin r = entry.getValue();

			boolean ok = true;
			outer: for (int x = 0; x < r.getWidth(); x ++) {
				for (int y = 0; y < r.getHeight(); y ++) {
					int f = map.getValue(x + p.getX(), y + p.getY());
					if (f == FLOOR) {
						// Ruin's been damaged. Remove it completely
						ok = false;
						break outer;
					}
				}
			}
			if (!ok) {
				for (int x = 0; x < r.getWidth(); x ++) {
					for (int y = 0; y < r.getHeight(); y ++) {
						map.setValue(x + p.getX(), y + p.getY(), FLOOR);
					}
				}
				i.remove();
			}
		}
	}


	/**
	 * Add a crystal
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	protected final boolean addCrystal(int x, int y, int size) {
		// Pick a crystal from the template at this point
		Crystal crystal = scenery.getCrystal(size);
		if (crystal == null) {
			return false;
		}
		int w = crystal.getWidth();
		int h = crystal.getHeight();
		for (int yy = 0; yy < h; yy ++) {
			for (int xx = 0; xx < w; xx ++) {
				assert map.getValue(x + xx, y + yy) == FLOOR;
				map.setValue(x + xx, y + yy, TOTAL_IMPASSABLE);
			}
		}
		map.setValue(x, y, CRYSTAL);
		crystals.put(new Point(x, y), crystal);
		if (Game.DEBUG) {
			System.out.println("Crystal:"+size+" added at "+x+","+y);
		}
		return true;
	}

	/**
	 * @return the ruins: an unmodifiable map of Points to Dimensions
	 */
	public final Map<Point, Ruin> getRuins() {
		return Collections.unmodifiableMap(ruins);
	}

	/**
	 * @return the crystals: an unmodifiable map of Points to Dimensions
	 */
	public final Map<Point, Crystal> getCrystals() {
		return Collections.unmodifiableMap(crystals);
	}

	/**
	 * Adds a spawn point at the specified location
	 * @param x
	 * @param y
	 */
	protected final void addSpawnPoint(int x, int y) {
		if (levelFeature.useFixedSpawnPoints()) {
			assert map.getValue(x, y) == FLOOR : "Can't put spawnpoint down at "+x+","+y+": "+map.getValue(x, y);
			map.setValue(x, y, SPAWN);
			spawnPoints.add(new Point(x, y));
		}
	}

	/**
	 * Ensure that each base is accessible by at least one spawnpoint
	 * @return true if the map is valid, false if not
	 */
	private boolean isValid() {
		if (spawnPoints.size() == 0 && levelFeature.useFixedSpawnPoints()) {
			System.out.println("NO SPAWNPOINTS!");
			return false;
		}
		IntList path = new IntList(true, getWidth() + getHeight());
		int[] steps = new int[1];
		SolidCheck check = new SolidCheck() {
			@Override
			public boolean isSolid(int x, int y, int v) {
				return x < 0 || y < 0 || x >= getWidth() || y >= getHeight() || v == EMPTY || v == WALL || v == WATER || v == IMPASSABLE || v == RUIN_IMPASSABLE || v == TOTAL_IMPASSABLE || v == SPECIAL_IMPASSABLE || v == CRYSTAL || v == RUIN;
			}
		};

		outer: for (Iterator<Point> i = spawnPoints.iterator(); i.hasNext(); ) {
			Point spawnP = i.next();
			if (getValue(spawnP.getX(), spawnP.getY()) != SPAWN) {
				continue;
			}
			for (Iterator<Point> j = bases.iterator(); j.hasNext(); ) {
				Point baseP = j.next();
				if (getValue(baseP.getX(), baseP.getY()) != BASE) {
					continue;
				}

				assert !spawnP.equals(baseP);

				if (findPath(spawnP.getX(), spawnP.getY(), baseP.getX(), baseP.getY(), path, check, steps)) {
//					// Stash in route cache
//					Gidrah.addCachedRoute(spawnP.getX(), spawnP.getY(), baseP.getX(), baseP.getY(), path);
					continue outer;
				}
			}
			// We couldn't find a route between this spawn point and any bases.
			if (Game.DEBUG) {
				System.out.println("No route between "+spawnP+" and any base. Here's the duff map:");
				dump();
			}
//			Gidrah.clearCachedRoutes(null);
			return false;
		}
		return true;
	}

	protected void dump() {
		for (int y = getHeight(); --y >= 0;) {
			if (y < 10) {
				System.out.print("  "+y+" ");
			} else if (y < 100) {
				System.out.print(" "+y+" ");
			} else {
				System.out.print(y+" ");
			}
			for (int x = 0; x < getWidth(); x ++) {
				switch (getValue(x, y)) {
					case FLOOR:
						System.out.print('.');
						break;
					case WALL:
						System.out.print('#');
						break;
					case INTERNAL:
						System.out.print('$');
						break;
					case SPAWN:
						System.out.print('+');
						break;
					case OBSTACLE:
						System.out.print('%');
						break;
					case CRYSTAL:
						System.out.print('^');
						break;
					case IMPASSABLE:
						System.out.print('*');
						break;
					case BASE:
						System.out.print('@');
						break;
					case SPECIAL:
					case SPECIAL_IMPASSABLE:
						System.out.print('%');
						break;
					case TOTAL_IMPASSABLE:
						System.out.print('$');
						break;
					case EMPTY:
						System.out.print(' ');
						break;
					case WALLEDGE:
						System.out.print('X');
						break;
					case WATER:
						System.out.print('~');
						break;
					case RUIN:
					case RUIN_IMPASSABLE:
						System.out.print('H');
						break;
					case DEEP:
						System.out.print(' ');
						break;
					default:
						assert false : getValue(x, y);
						break;
				}
			}
			System.out.println();
		}
		System.out.print("   ");
		for (int x = 0; x < getWidth(); x ++) {
			System.out.print(x % 10);
		}
		System.out.println();

//		for (Iterator i = spawnPoints.iterator(); i.hasNext(); ) {
//			System.out.println("Spawnpoint "+i.next());
//		}
	}


	/**
	 * Can we plot an unobstructed path between two points?
	 */
	protected final boolean findPath(int sx, int sy, int tx, int ty, IntList path, Topology topology, int[] steps) {
		AStar astar = new AStar(topology);
		path.clear();
		astar.findPath(IntGridTopology.pack(sx, sy), IntGridTopology.pack(tx, ty), path);

		int result;//, count = 0;
		while ((result = astar.nextStep()) == AStar.SEARCH_STATE_SEARCHING) {
			// Do nothing...
//			System.out.println("Search steps..."+(++count));
		}
//		if (result == AStar.SEARCH_STATE_FAILED) {
//			System.out.println("Result for "+sx+", "+sy+"->"+tx+", "+ty+":"+result);
//			dump();
//		}

//		System.out.println("Path found in "+astar.getNumSteps());

		steps[0] = astar.getNumSteps();

		// Clean up nodes
		astar.cancel();
		return result == AStar.SEARCH_STATE_SUCCEEDED;
	}

	/**
	 * Can we plot an unobstructed path between two points?
	 */
	protected final boolean findPath(int sx, int sy, int tx, int ty, IntList path, SolidCheck check, int[] steps) {
		topology.setCheck(check);
		return findPath(sx, sy, tx, ty, path, topology, steps);
	}

	/**
	 * Perform any cleanup after bases and spawnpoints and areas have been generated
	 */
	protected void clean() {
	}

	private void prebuild(GameMap ret) {

		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				int currValue = map.getValue(x, y);
				if (currValue == WALL) {
					ret.setFade(x, y, 1);
				}
			}
		}
		for (int depth = 1; depth < 10; depth ++) {
			for (int y = 0; y < map.getHeight(); y ++) {
				for (int x = 0; x < map.getWidth(); x ++) {
					int currValue = ret.getFade(x, y);
					if (currValue == depth) {
						int neighbours =
								(ret.getFade(x, y + 1) >= depth || y == map.getHeight() - 1 ? 1 : 0)
							+	(ret.getFade(x, y - 1) >= depth || y == 0 ? 1 : 0)
							+	(ret.getFade(x + 1, y) >= depth || x == map.getWidth() - 1 ? 1 : 0)
							+	(ret.getFade(x - 1, y) >= depth || x == 0 ? 1 : 0)
							+	(ret.getFade(x + 1, y + 1) >= depth || x == map.getWidth() - 1 || y == map.getHeight() - 1 ? 1 : 0)
							+	(ret.getFade(x + 1, y - 1) >= depth || x == map.getWidth() - 1 || y == 0  ? 1 : 0)
							+	(ret.getFade(x - 1, y + 1) >= depth || x == 0 || y == map.getHeight() - 1  ? 1 : 0)
							+	(ret.getFade(x - 1, y - 1) >= depth || x == 0 || y == 0  ? 1 : 0)
								;
						if (neighbours == 8) {
							ret.setFade(x, y, depth + 1);
						}
					}
				}
			}
		}

		// First process the wall edges
		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				int currValue = map.getValue(x, y);

				if (currValue == WALL) {
					// If we've got any non-WALL neighbours we'll become WALLEDGE.
					int neighbours =
							(map.getValue(x, y + 1) == WALL || map.getValue(x, y + 1) == EMPTY || map.getValue(x, y + 1) == WALLEDGE ? 1 : 0)
						+	(map.getValue(x, y - 1) == WALL || map.getValue(x, y - 1) == EMPTY || map.getValue(x, y - 1) == WALLEDGE ? 1 : 0)
						+	(map.getValue(x + 1, y) == WALL || map.getValue(x + 1, y) == EMPTY || map.getValue(x + 1, y) == WALLEDGE ? 1 : 0)
						+	(map.getValue(x - 1, y) == WALL || map.getValue(x - 1, y) == EMPTY || map.getValue(x - 1, y) == WALLEDGE ? 1 : 0)
						+	(map.getValue(x + 1, y + 1) == WALL || map.getValue(x + 1, y + 1) == EMPTY || map.getValue(x + 1, y + 1) == WALLEDGE ? 1 : 0)
						+	(map.getValue(x + 1, y - 1) == WALL || map.getValue(x + 1, y - 1) == EMPTY || map.getValue(x + 1, y - 1) == WALLEDGE ? 1 : 0)
						+	(map.getValue(x - 1, y + 1) == WALL || map.getValue(x - 1, y + 1) == EMPTY || map.getValue(x - 1, y + 1) == WALLEDGE ? 1 : 0)
						+	(map.getValue(x - 1, y - 1) == WALL || map.getValue(x - 1, y - 1) == EMPTY || map.getValue(x - 1, y - 1) == WALLEDGE ? 1 : 0)
							;

					if (neighbours != 8) {
						map.setValue(x, y, WALLEDGE);
					}
				}
			}
			Thread.yield();
		}
		setProgress(0.65f);

		// Second process the internal walls. Any wall which has 9 WALL or INTERNAL entries itself becomes INTERNAL.
		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				int currValue = map.getValue(x, y);

				if (currValue == WALL) {
					int neighbours =
							(map.getValue(x, y + 1) == WALL || map.getValue(x, y + 1) == EMPTY || map.getValue(x, y + 1) == INTERNAL ? 1 : 0)
						+	(map.getValue(x, y - 1) == WALL || map.getValue(x, y - 1) == EMPTY || map.getValue(x, y - 1) == INTERNAL ? 1 : 0)
						+	(map.getValue(x + 1, y) == WALL || map.getValue(x + 1, y) == EMPTY || map.getValue(x + 1, y) == INTERNAL ? 1 : 0)
						+	(map.getValue(x - 1, y) == WALL || map.getValue(x - 1, y) == EMPTY || map.getValue(x - 1, y) == INTERNAL ? 1 : 0)
						+	(map.getValue(x + 1, y + 1) == WALL || map.getValue(x + 1, y + 1) == EMPTY || map.getValue(x + 1, y + 1) == INTERNAL ? 1 : 0)
						+	(map.getValue(x + 1, y - 1) == WALL || map.getValue(x + 1, y - 1) == EMPTY || map.getValue(x + 1, y - 1) == INTERNAL ? 1 : 0)
						+	(map.getValue(x - 1, y + 1) == WALL || map.getValue(x - 1, y + 1) == EMPTY || map.getValue(x - 1, y + 1) == INTERNAL ? 1 : 0)
						+	(map.getValue(x - 1, y - 1) == WALL || map.getValue(x - 1, y - 1) == EMPTY || map.getValue(x - 1, y - 1) == INTERNAL ? 1 : 0)
							;

					if (neighbours == 8) {
						map.setValue(x, y, INTERNAL);
					}
				}
			}
			Thread.yield();
		}
		setProgress(0.7f);



		// Now any WALLEDGE that doesn't have at least one WALL or EMPTY neighbour gets turned to FLOOR.
		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				int currValue = map.getValue(x, y);


				if (currValue == WALLEDGE) {
					int neighbours =
							(map.getValue(x, y + 1) == WALL ? 1 : 0)
						+	(map.getValue(x, y - 1) == WALL ? 1 : 0)
						+	(map.getValue(x + 1, y) == WALL ? 1 : 0)
						+	(map.getValue(x - 1, y) == WALL ? 1 : 0)
						+	(map.getValue(x + 1, y + 1) == WALL ? 1 : 0)
						+	(map.getValue(x + 1, y - 1) == WALL ? 1 : 0)
						+	(map.getValue(x - 1, y + 1) == WALL ? 1 : 0)
						+	(map.getValue(x - 1, y - 1) == WALL ? 1 : 0)
							;

					if (neighbours == 0) {
						map.setValue(x, y, FLOOR);
					}
				}
			}
			Thread.yield();
		}

		// Any road at the edge of the map that doesn't have a neighbour just inside the edge gets removed
		// North & south edges
		for (int x = 0; x < map.getWidth(); x ++) {
			if (roadsOverlay.getValue(x, 0) == 1 && roadsOverlay.getValue(x, 1) == 0) {
				roadsOverlay.setValue(x, 0, 0);
			}
			if (roadsOverlay.getValue(x, getHeight() - 1) == 1 && roadsOverlay.getValue(x, getHeight() - 2) == 0) {
				roadsOverlay.setValue(x, getHeight() - 1, 0);
			}
		}
		// East & west edges
		for (int y = 0; y < map.getHeight(); y ++) {
			if (roadsOverlay.getValue(0, y) == 1 && roadsOverlay.getValue(1, y) == 0) {
				roadsOverlay.setValue(0, y, 0);
			}
			if (roadsOverlay.getValue(getWidth() - 1, y) == 1 && roadsOverlay.getValue(getWidth() - 2, y) == 0) {
				roadsOverlay.setValue(getWidth() - 1, y, 0);
			}
		}
		setProgress(0.75f);


	}

	/**
	 * Take the generated map and draw tiles using the template, returning a GameMap.
	 * @return a {@link GameMap}
	 */
	protected GameMap build() {
		GameMap ret = new GameMap(map.getWidth(), map.getHeight(), template.getFill());
		if (Game.DEBUG) {
			System.out.println("Width:"+map.getWidth()+" x Height:"+map.getHeight());
		}
		prebuild(ret);

		int currentFormation = 0;

		// Noise to do the 2 kinds of floor terrain with
		IntGrid floorGrid = new IntGrid(map.getWidth(), map.getHeight(), 0);
		PerlinNoise noise = new PerlinNoise(Util.random(0, Integer.MAX_VALUE - 1), 6, 3, 0.5f);
		float threshold = LinearInterpolator.instance.interpolate(0.9f, 0.5f, levelInWorld / 10.0f);
		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				floorGrid.setValue(x, y, noise.perlinNoise(x, y) > threshold ? 1 : 0);
			}
			Thread.yield();
		}
		setProgress(0.8f);

		// Don't use floor1 near ruins or wall edges or roads... or inside internal rock stuff!
		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				int currValue = map.getValue(x, y);
				switch (currValue) {
					case WALLEDGE:
					case WALL:
					case INTERNAL:
					case RUIN:
					case CRYSTAL:
					case IMPASSABLE:
					case TOTAL_IMPASSABLE:
					case RUIN_IMPASSABLE:
					case SPECIAL_IMPASSABLE:
					case BASE:
						for (int yy = y - 2; yy <= y + 2; yy ++) {
							for (int xx = x - 2; xx <= x + 2; xx ++) {
								int dx = xx - x;
								int dy = yy - y;
								if (Math.sqrt(dx * dx + dy * dy) <= 2.0) {
									floorGrid.setValue(xx, yy, 0);
								}
							}
						}
						break;
					default:
						break;
				}
				if (roadsOverlay.getValue(x, y) != 0) {
					for (int yy = y - 2; yy <= y + 2; yy ++) {
						for (int xx = x - 2; xx <= x + 2; xx ++) {
							int dx = xx - x;
							int dy = yy - y;
							if (Math.sqrt(dx * dx + dy * dy) <= 2.0) {
								floorGrid.setValue(xx, yy, 0);
							}
						}
					}
				}
			}
			Thread.yield();
		}
		setProgress(0.85f);

		generateCrystals();

		dump();

		double randomRockyThreshold = scenery.getRandomRockyThreshold();

		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				int currValue = map.getValue(x, y);

				// warning - chaz hack! - to make walledges use a diff tile
				int floorTile = 0;

				switch (currValue) {
					case EMPTY:
						Tile.getEmptyTile().toMap(ret, x, y, false);
						break;

					case DEEP:
						Tile.getTile(1).toMap(ret, x, y, false);
						break;

					case FLOOR:
						if (Util.random() < randomRockyThreshold) {
							floorTile = 2;
						}

						writeFloor(ret, x, y, floorGrid, floorTile);
						break;

					case WALLEDGE:
						floorTile=2;
						writeFloor(ret, x, y, floorGrid, floorTile);
						break;

					case IMPASSABLE:
						floorTile=2;
						writeFloor(ret, x, y, floorGrid, floorTile);
						Exclude.getInstance().toMap(ret, x, y, true);
						break;

					case WALL:
						int neighbours =
								(map.getValue(x, y + 1) == WALL || map.getValue(x, y + 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x, y - 1) == WALL || map.getValue(x, y - 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x + 1, y) == WALL || map.getValue(x + 1, y) == EMPTY ? 1 : 0)
							+	(map.getValue(x - 1, y) == WALL || map.getValue(x - 1, y) == EMPTY ? 1 : 0)
							+	(map.getValue(x + 1, y + 1) == WALL || map.getValue(x + 1, y + 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x + 1, y - 1) == WALL || map.getValue(x + 1, y - 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x - 1, y + 1) == WALL || map.getValue(x - 1, y + 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x - 1, y - 1) == WALL || map.getValue(x - 1, y - 1) == EMPTY ? 1 : 0)
								;


						writeFloor(ret, x, y, floorGrid, floorTile);
						template.getWall(neighbours).toMap(ret, x, y, true);
						break;

					case INTERNAL:
						floorTile=3;
						int neighbours2 =
								(map.getValue(x, y + 1) == INTERNAL || map.getValue(x, y + 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x, y - 1) == INTERNAL || map.getValue(x, y - 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x + 1, y) == INTERNAL || map.getValue(x + 1, y) == EMPTY ? 1 : 0)
							+	(map.getValue(x - 1, y) == INTERNAL || map.getValue(x - 1, y) == EMPTY ? 1 : 0)
							+	(map.getValue(x + 1, y + 1) == INTERNAL || map.getValue(x + 1, y + 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x + 1, y - 1) == INTERNAL || map.getValue(x + 1, y - 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x - 1, y + 1) == INTERNAL || map.getValue(x - 1, y + 1) == EMPTY ? 1 : 0)
							+	(map.getValue(x - 1, y - 1) == INTERNAL || map.getValue(x - 1, y - 1) == EMPTY ? 1 : 0)
								;

						// chaz hack! gradient edges?
						if (neighbours2 < 8) {
							Res.getFloorEdgeTransition(
								(map.getValue(x, 	y+1) == WALL),
								(map.getValue(x+1, 	y+1) == WALL),
								(map.getValue(x+1, 	y) == WALL),
								(map.getValue(x+1, 	y-1) == WALL),
								(map.getValue(x, 	y-1) == WALL),
								(map.getValue(x-1,	y-1) == WALL),
								(map.getValue(x-1,	y) == WALL),
								(map.getValue(x-1,	y+1) == WALL)
							).toMap(ret, x, y, false);
						} else {
							writeFloor(ret, x, y, floorGrid, floorTile);
						}


						// Hackery: if 8 neighbours, every other tile is blank
						// but only if getBigInternalWall == true
						if (template.getBigInternalWalls()) {
							if (neighbours2 < 8 || neighbours2 == 8 && (x & 1 ^ y & 1) == 0) {
								template.getInternalWall(neighbours2).toMap(ret, x, y, true);
							}
						} else {
							template.getInternalWall(neighbours2).toMap(ret, x, y, true);
						}
						break;

					case BASE:
						// Draw some floor under the base
						writeFloor(ret, x, y, floorGrid, floorTile);

						// Then add the base
						template.getBase().toMap(ret, x, y, true);

						// Erase base squares
						for (int yy = 0; yy < 3; yy ++) {
							for (int xx = 0; xx < 4; xx ++) {
								map.setValue(x + xx, y + yy, SPECIAL);
							}
						}

						break;
					case SPAWN:
						// Draw some floor under the spawn point.
						writeFloor(ret, x, y, floorGrid, floorTile);

						if (x == 0) {
							char type = formation[currentFormation ++];
							Res.getWestSpawnPoint(type - '1').toMap(ret, x, y, true);
						} else if (x == map.getWidth() - 1) {
							char type = formation[currentFormation ++];
							Res.getEastSpawnPoint(type - '1').toMap(ret, x, y, true);
						} else if (y == 0) {
							char type = formation[currentFormation ++];
							Res.getSouthSpawnPoint(type - '1').toMap(ret, x, y, true);
						} else if (y == map.getHeight() - 1) {
							char type = mapGeneratorParams.getGameMode() == WormGameState.GAME_MODE_XMAS ? '1' : formation[currentFormation ++]; // Xmas Hax!
							Res.getNorthSpawnPoint(type - '1').toMap(ret, x, y, true);
						} else {
							template.getMidSpawn().toMap(ret, x, y, true);
						}

						break;

					case OBSTACLE:
						floorTile=2;
						//floorTile=3;
						writeFloor(ret, x, y, floorGrid, floorTile);
						if (roadsOverlay.getValue(x, y) == 1) {
							scenery.getRoadObstacle().toMap(ret, x, y, true);
						} else {
							scenery.getObstacle().toMap(ret, x, y, true);
						}
						break;

					case RUIN_IMPASSABLE:
					case RUIN:
						// Draw a ruin. First the floor
						floorTile=2;
						writeFloor(ret, x, y, floorGrid, floorTile);

						// Get ruin from ruins map
						Ruin ruin = ruins.get(new Point(x, y));
						if (ruin != null) {
							ruin.toMap(ret, x, y, true);
						}

						break;

					case CRYSTAL:
						// Draw a crystal. First the floor
						floorTile=2;
						writeFloor(ret, x, y, floorGrid, floorTile);
						// and an exclude
						Exclude.getInstance().toMap(ret, x, y, false);

						// Get crystal from crystals map
						Crystal crystal = crystals.get(new Point(x, y));
						if (crystal != null) {
							crystal.toMap(ret, x, y, true);
						}

						break;

					case SPECIAL:
						// Put floor down
						writeFloor(ret, x, y, floorGrid, floorTile);
						break;

					case SPECIAL_IMPASSABLE:
						// Put floor down and an exclude on top
						writeFloor(ret, x, y, floorGrid, floorTile);
						Exclude.getInstance().toMap(ret, x, y, false);
						break;

					case TOTAL_IMPASSABLE:
						writeFloor(ret, x, y, floorGrid, floorTile);
						TotalExclude.getInstance().toMap(ret, x, y, true);
						break;

					default:
						assert false;
				}

				// Now plonk road overlay down
				if (roadsOverlay.getValue(x, y) == 1) {
					// Draw road depending on the NSEW neighbours.
					boolean nRoad = roadsOverlay.getValue(x, y + 1) == 1 || y == getHeight() - 1;
					boolean sRoad = roadsOverlay.getValue(x, y - 1) == 1 || y == 0;
					boolean eRoad = roadsOverlay.getValue(x + 1, y) == 1 || x == getWidth() - 1;
					boolean wRoad = roadsOverlay.getValue(x - 1, y) == 1 || x == 0;

					scenery.getRoad(nRoad, eRoad, sRoad, wRoad).toMap(ret, x, y, true);
					ret.setCost(x, y, Topology.ROAD_COST);
				} else if (isBog(x, y, floorGrid)) {
					ret.setCost(x, y, Topology.BOG_COST);
				} else {
					ret.setCost(x, y, Topology.NORMAL_COST);
				}
			}
			Thread.yield();
		}

		if (mapGeneratorParams.getGameMode() == WormGameState.GAME_MODE_CAMPAIGN || mapGeneratorParams.getGameMode() == WormGameState.GAME_MODE_ENDLESS) {
			try {
				save(ret);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
		setProgress(1.0f);

		return ret;
	}

	private boolean isBog(int x, int y, IntGrid floorGrid) {
		return floorGrid.getValue(x, y) == 1;
	}

	private void writeFloor(GameMap map, int x, int y, IntGrid floorGrid, int floorTile) {
		if (isBog(x, y, floorGrid)) {
			// Count the neighbours which are also above the threshold...
			int neighbours =
					floorGrid.getValue(x, y + 1)
				+	floorGrid.getValue(x, y - 1)
				+	floorGrid.getValue(x + 1, y)
				+	floorGrid.getValue(x - 1, y)
				+	floorGrid.getValue(x + 1, y + 1)
				+	floorGrid.getValue(x + 1, y - 1)
				+	floorGrid.getValue(x - 1, y + 1)
				+	floorGrid.getValue(x - 1, y - 1)
					;
			if (neighbours == 8) {
				template.getFloor1().toMap(map, x, y, true);
			} else {
				template.getFloorTransitions(neighbours).toMap(map, x, y, true);
			}
		} else {
			// chaz hack! - how you do eval(string) in java?
			//eval("template.getFloor"+floorTile+"().toMap(map, x, y, true)");
			// spose getFloor should use an array but hey it works ok
			switch (floorTile) {
				case 1:
					template.getFloor1().toMap(map, x, y, true);
					break;
				case 2:
					template.getFloor2().toMap(map, x, y, true);
					break;
				case 3:
					template.getFloor3().toMap(map, x, y, true);
					break;
				default:
					template.getFloor0().toMap(map, x, y, true);
			}

		}
	}

}
