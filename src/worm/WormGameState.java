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


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.GameInputStream;
import net.puppygames.applet.GameOutputStream;
import net.puppygames.applet.GameState;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.RoamingFile;
import net.puppygames.applet.effects.LabelEffect;
import net.puppygames.applet.screens.DialogScreen;
import net.puppygames.steam.Steam;
import net.puppygames.steam.SteamException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;

import worm.buildings.BuildingFeature;
import worm.effects.ArrowEffect;
import worm.entities.Building;
import worm.entities.Capacitor;
import worm.entities.Factory;
import worm.entities.Gidrah;
import worm.entities.Saucer;
import worm.entities.Smartbomb;
import worm.entities.Turret;
import worm.entities.Unit;
import worm.features.GidrahFeature;
import worm.features.HintFeature;
import worm.features.LevelFeature;
import worm.features.MedalFeature;
import worm.features.RankFeature;
import worm.features.ResearchFeature;
import worm.features.StoryFeature;
import worm.features.WorldFeature;
import worm.generator.BaseMapGenerator;
import worm.powerups.BatteryPowerupFeature;
import worm.powerups.BezerkPowerupFeature;
import worm.powerups.CapacitorPowerupFeature;
import worm.powerups.CoolingTowerPowerupFeature;
import worm.powerups.FreezePowerupFeature;
import worm.powerups.MoneyPowerupFeature;
import worm.powerups.PowerupFeature;
import worm.powerups.ReactorPowerupFeature;
import worm.powerups.RepairPowerupFeature;
import worm.powerups.ResourcePowerupFeature;
import worm.powerups.ScannerPowerupFeature;
import worm.powerups.ShieldGeneratorPowerupFeature;
import worm.powerups.ShieldPowerupFeature;
import worm.powerups.SmartbombPowerupFeature;
import worm.screens.CompleteGameScreen;
import worm.screens.CompleteXmasScreen;
import worm.screens.GameScreen;
import worm.screens.IntermissionScreen;
import worm.screens.MenuScreen;
import worm.screens.NewWorldScreen;
import worm.screens.ResearchScreen;
import worm.screens.SelectEndlessLevelScreen;
import worm.screens.SelectLevelScreen;
import worm.screens.SelectSandboxLevelScreen;
import worm.screens.SelectSurvivalLevelScreen;
import worm.screens.SelectWorldScreen;
import worm.screens.StoryScreen;
import worm.screens.SurvivalMenuScreen;
import worm.screens.XmasMenuScreen;
import worm.tiles.Crystal;
import worm.tiles.Exclude;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.interpolators.SineInterpolator;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.util.Util;

/**
 * The entire game state during play
 */
public class WormGameState extends GameState {

	private static final long serialVersionUID = 3L;

	private static final Rectangle TEMPBOUNDS = new Rectangle();
	private static final ArrayList<Entity> BUILD_COLLISIONS = new ArrayList<Entity>();

	public static final int GAME_MODE_CAMPAIGN = 0;
	public static final int GAME_MODE_ENDLESS = 1;
	public static final int GAME_MODE_SURVIVAL = 2;
	public static final int GAME_MODE_SANDBOX = 3;
	public static final int GAME_MODE_XMAS = 4;

	/** Mouse dragging speed */
	private static final float MOUSE_DRAG_SPEED = 2.0f;

	/** Absolute max map size in tiles */
	public static final int ABS_MAX_SIZE = 96;

	/** Absolute max map width in pixels */
	public static final int MAP_WIDTH = ABS_MAX_SIZE * MapRenderer.TILE_SIZE;

	/** Absolute max map height in pixels */
	public static final int MAP_HEIGHT = MAP_WIDTH;

	/** Map resolution - defines the grid size upon which we place buildings */
	public static final int MAP_RESOLUTION = MapRenderer.TILE_SIZE / 4;

	/** Maximum number of gidrahs allowed */
	public static final int MAX_GIDRAHS = 160;

	/** Number of levels in a world */
	public static final int LEVELS_IN_WORLD = 10;

	/** Number of worlds */
	public static final int NUM_WORLDS = 5;

	/** Number of levels */
	public static final int MAX_LEVELS = LEVELS_IN_WORLD * NUM_WORLDS;

	/** Duration in ticks that we wait after save is initiated */
	private static final int SAVE_DURATION = 45;

	/** End of level duration, in ticks */
	private static final int END_OF_LEVEL_DURATION = 30 * 60;

	/** Wait a few seconds delay, after a boss is killed */
	private static final int WAIT_A_FEW_SECONDS_DELAY = 5 * 60;

	/** Endgame duration, ticks */
	private static final int END_OF_GAME_DURATION = 420;

	/** After this interval (ticks), we warn about crystals that aren't being mined */
	private static final int UNMINED_WARNING_TIME = 600;

	/** After this interval (ticks) of nothing happening, we mention the fast forward button */
	private static final int INTERESTING_INTERVAL = 500;

	/** Right mouse button drag sensitivity */
	private static final int RMB_DRAG_SENSITIVITY = 3;

	/** Game configuration */
	private final GameConfiguration config;

	private static final ArrayList<Capacitor> TEMP_CAPACITORS = new ArrayList<Capacitor>();

	/*
	 * Transient data
	 */

	/** Save game stuff */
	private transient int saveTick;
	private transient boolean saving;

	/** Xmas reset */
	private transient boolean xmasReset;


	/**
	 * Meta state - holds all the information that should be saved on a level-by-level basis, not the actual in-play state, in
	 * a campaign game
	 */
	public static class MetaState implements Serializable {

		private static final long serialVersionUID = 3L;

		private static final int MAX_RANDOM = 30;
		private static final int MAX_DEFAULT_RANDOM = 24;
		private static final int MAX_EXOTIC_RANDOM = 6;
		private static final int MAX_CRAPPY_RANDOM = 10;

		/** Game mode */
		private final int gameMode;

		/** A map of stats (Strings to Integers) */
		private final Map<String, Integer> stats = new HashMap<String, Integer>();

		/** Powerups: a Map of PowerupFeatures to Integers, which is the number of each of that sort of powerup the player has. Or null for 0. */
		private final Map<PowerupFeature, Integer> powerups = new HashMap<PowerupFeature, Integer>();

		/** Medals: a Map of MedalFeatures to Integers */
		private final Map<MedalFeature, Integer> medals = new HashMap<MedalFeature, Integer>();

		/** Total score for all medals */
		private int score;

		/** Current rank */
		private RankFeature rank;

		/** Which BuildingFeatures have been researched (Strings - from BuildingFeature.getID()) */
		private final HashSet<String> researched = new HashSet<String>();

		/** Money */
		private int money;

		/** Number of attempts at current level so far */
		private int attempts;

		/** Level index */
		private int level = 0;

		/** World */
		private WorldFeature world;

		/** Level */
		private LevelFeature levelFeature;

		/** Base difficulty, usually negative as player starts to lose */
		private float difficulty = 0.0f;

		/** Survival params */
		private SurvivalParams survivalParams;

		/** Sandbox params */
		private SandboxParams sandboxParams;

		/** Powerups shuffle */
		private ArrayList<Integer> shuffle;
		private ArrayList<Integer> crappyShuffle;
		private ArrayList<Integer> randomShuffle;
		private ArrayList<Integer> exoticShuffle;

		/** Resources */
		private Map<BuildingFeature, Integer> availableStock;

		public MetaState(int gameMode) {
			this.gameMode = gameMode;
			reset();
		}

		public void bonusMoney(int amount) {
			money += amount;
		}

		public int getAvailableStock(BuildingFeature bf) {
			if (availableStock == null) {
				availableStock = new HashMap<BuildingFeature, Integer>();
			}
			Integer ret = availableStock.get(bf);
			if (ret == null) {
				return 0;
			} else {
				return ret.intValue();
			}
		}

		public void addAvailableStock(BuildingFeature bf, int n) {
			int current = getAvailableStock(bf);
			if (current == -1) {
				// Hmm.
				assert false;
				return;
			}
			current = Math.min(bf.getMaxAvailable(), Math.max(0, current + n));
			availableStock.put(bf, new Integer(current));
		}

		void reset() {
			stats.clear();
			powerups.clear();
			medals.clear();
			researched.clear();
			if (availableStock == null) {
				availableStock = new HashMap<BuildingFeature, Integer>();
			} else {
				availableStock.clear();
			}
			switch (gameMode) {
				case GAME_MODE_SURVIVAL:
					if (survivalParams != null) {
						money = GameConfiguration.getInstance().getSurvivalInitialMoney()[survivalParams.getWorld().getIndex()];
					}
					break;
				case GAME_MODE_XMAS:
					money = GameConfiguration.getInstance().getXmasInitialMoney();
					break;
				default:
					money = GameConfiguration.getInstance().getNormalInitialMoney();
					break;
			}
			score = 0;
			rank = RankFeature.getRank(0);
			shuffle = new ArrayList<Integer>(MAX_RANDOM);
			randomShuffle = new ArrayList<Integer>(MAX_DEFAULT_RANDOM);
			crappyShuffle = new ArrayList<Integer>(MAX_CRAPPY_RANDOM);
			exoticShuffle = new ArrayList<Integer>(MAX_EXOTIC_RANDOM);
		}

		private int addStat(String stat, int n) {
			Integer current = stats.get(stat);
			if (current == null) {
				current = Integer.valueOf(n);
			} else {
				current = Integer.valueOf(current.intValue() + n);
			}
			stats.put(stat, current);
			return current.intValue();
		}

		private static String getPath(int level, int gameMode) {
			return Game.getPlayerDirectoryPrefix() + "metastate_" + level + "_"+gameMode+".ser";
		}

		public static MetaState load(int level, int gameMode) throws Exception {
			GameInputStream gis = null;
			ObjectInputStream ois = null;
			try {
				String path = getPath(level, gameMode);
				RoamingFile file = new RoamingFile(path);
				if (!file.exists()) {
					throw new FileNotFoundException("The save game file "+path+" was not found");
				}
				gis = new GameInputStream(path);
				ois = new ObjectInputStream(gis);

				MetaState ret = (MetaState) ois.readObject();
				Resources.dequeue();

				if (ret.shuffle == null || ret.exoticShuffle == null || ret.randomShuffle == null || ret.crappyShuffle == null) {
					ret.shuffle = new ArrayList<Integer>(MAX_RANDOM);
					ret.randomShuffle = new ArrayList<Integer>(MAX_DEFAULT_RANDOM);
					ret.crappyShuffle = new ArrayList<Integer>(MAX_CRAPPY_RANDOM);
					ret.exoticShuffle = new ArrayList<Integer>(MAX_EXOTIC_RANDOM);
				}

				if (ret.availableStock == null) {
					ret.availableStock = new HashMap<BuildingFeature, Integer>();
				}
				return ret;
			} finally {
				try {
					if (gis != null) {
						gis.close();
					}
				} catch (IOException e) {
				}
			}
		}

		public void save() throws Exception {
			GameOutputStream gos = null;
			ObjectOutputStream oos = null;
			try {
				gos = new GameOutputStream(getPath(level, gameMode));
				oos = new ObjectOutputStream(gos);

				oos.writeObject(this);
				oos.flush();
				gos.flush();
			} finally {
				try {
					gos.close();
				} catch (IOException e) {
				}
			}
		}

		public int getMoney() {
			return money;
		}

		public void bonusPowerup(PowerupFeature powerup, int amount) {
			Integer current = powerups.get(powerup);
			if (current == null) {
				current = Integer.valueOf(amount);
			} else {
				current = Integer.valueOf(current.intValue() + amount);
			}
			powerups.put(powerup, current);
		}

		private PowerupFeature getCrappyPowerup() {
    		if (crappyShuffle.size() == 0) {
    			for (int i = 0; i < MAX_CRAPPY_RANDOM; i ++) {
    				crappyShuffle.add(new Integer(i));
    			}
    			Collections.shuffle(crappyShuffle);
    		}
    		Integer i = crappyShuffle.remove(crappyShuffle.size() - 1);
        	switch (i.intValue()) {
        		case 0:
        		case 1:
        		case 2:
        		case 3:
        		case 4:
        		case 5:
        		case 6:
        		case 7:
        		case 8:
        			return MoneyPowerupFeature.getInstance(50);
        		default:
        			return getPowerup();
        	}
        }

		private PowerupFeature getRandomPowerup() {
    		if (randomShuffle.size() == 0) {
    			for (int i = 0; i < MAX_DEFAULT_RANDOM; i ++) {
    				randomShuffle.add(new Integer(i));
    			}
    			Collections.shuffle(randomShuffle);
    		}
    		Integer i = randomShuffle.remove(randomShuffle.size() - 1);
        	switch (i.intValue()) {
        		case 0:
        		case 1:
        		case 2:
        		case 3:
        		case 4:
        		case 5:
        		case 6:
        			return MoneyPowerupFeature.getInstance(50);
        		case 7:
        		case 8:
        		case 9:
        		case 10:
        		case 11:
        			return MoneyPowerupFeature.getInstance(100);
        		case 12:
        		case 13:
        		case 14:
        		case 15:
        			return MoneyPowerupFeature.getInstance(250);
        		case 16:
        		case 17:
        		case 18:
        			return MoneyPowerupFeature.getInstance(500);
        		case 19:
        		case 20:
        		case 21:
        			return getPowerup();
        		default:
        			return getExoticPowerup();
        	}
        }

		/**
         * @return an exotic powerup
         */
        private PowerupFeature getExoticPowerup() {
        	if (gameMode == GAME_MODE_SURVIVAL || gameMode == GAME_MODE_XMAS || level < 4) {
        		return getPowerup();
        	}
        	PowerupFeature ret = null;
        	do {
        		if (exoticShuffle.size() == 0) {
        			for (int i = 0; i < MAX_EXOTIC_RANDOM; i ++) {
        				exoticShuffle.add(new Integer(i));
        			}
        			Collections.shuffle(exoticShuffle);
        		}
        		Integer i = exoticShuffle.remove(exoticShuffle.size() - 1);
        		WormGameState gameState = Worm.getGameState();
				switch (i.intValue()) {
        			case 0:
        				if (gameState.isResearched(ResearchFeature.BATTERY)) {
        					ret = BatteryPowerupFeature.getInstance();
        				} else {
        					ret = getCrappyPowerup();
        				}
        				break;
        			case 1:
        				if (gameState.isResearched(ResearchFeature.CAPACITOR)) {
        					ret = CapacitorPowerupFeature.getInstance();
        				} else {
        					ret = getCrappyPowerup();
        				}
        				break;
        			case 2:
        				if (gameState.isResearched(ResearchFeature.COOLINGTOWER)) {
        					ret = CoolingTowerPowerupFeature.getInstance();
        				} else {
        					ret = getCrappyPowerup();
        				}
        				break;
        			case 3:
        				if (gameState.isResearched(ResearchFeature.REACTOR)) {
        					ret = ReactorPowerupFeature.getInstance();
        				} else {
        					ret = getCrappyPowerup();
        				}
        				break;
        			case 4:
        				if (gameState.isResearched(ResearchFeature.SHIELDGENERATOR)) {
        					ret = ShieldGeneratorPowerupFeature.getInstance();
        				} else {
        					ret = getCrappyPowerup();
        				}
        				break;
        			case 5:
        				if (gameState.isResearched(ResearchFeature.SCANNER)) {
        					ret = ScannerPowerupFeature.getInstance();
        				} else {
        					ret = getCrappyPowerup();
        				}
        				break;
        			default:
        				assert false;
        				return null;
        		}
        	} while (!ret.isAvailable());

        	return ret;
        }

		/**
         * @return a non-exotic powerup
         */
        private PowerupFeature getPowerup() {
        	if (shuffle.size() == 0) {
        		for (int i = 0; i < MAX_RANDOM; i ++) {
        			shuffle.add(new Integer(i));
        		}
        		Collections.shuffle(shuffle);
        	}
        	Integer i = shuffle.remove(shuffle.size() - 1);
        	WormGameState gameState = Worm.getGameState();
        	switch (i.intValue()) {
        		case 0:
        		case 1:
        		case 2:
        			return BezerkPowerupFeature.getInstance();
        		case 3:
        		case 4:
        		case 5:
        			return RepairPowerupFeature.getInstance();
        		case 6:
        		case 7:
        		case 8:
        			return ShieldPowerupFeature.getInstance();
        		case 9:
        		case 10:
        		case 11:
        			return SmartbombPowerupFeature.getInstance();
        		case 12:
        		case 13:
        		case 14:
        			return FreezePowerupFeature.getInstance();
        		case 15:
        		case 16:
        		case 17:
        			if ((gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) && gameState.isResearched(ResearchFeature.MINES)) {
        				return ResourcePowerupFeature.getInstance("mines.powerup");
        			} else {
        				return getPowerup(); // Recurse
        			}
        		case 18:
        		case 19:
        			if ((gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) && gameState.isResearched(ResearchFeature.CLUSTERMINES)) {
        				return ResourcePowerupFeature.getInstance("clustermines.powerup");
        			} else {
        				return getPowerup(); // Recurse
        			}
        		case 20:
        			if ((gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) && gameState.isResearched(ResearchFeature.BLASTMINES)) {
        				return ResourcePowerupFeature.getInstance("blastmines.powerup");
        			} else {
        				return getPowerup(); // Recurse
        			}
        		case 21:
        		case 22:
        		case 23:
        			if ((gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) && gameState.isResearched(ResearchFeature.CONCRETE)) {
        				return ResourcePowerupFeature.getInstance("concrete.powerup");
        			} else {
        				return getPowerup(); // Recurse
        			}
        		case 24:
        		case 25:
        		case 26:
        			if ((gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) && gameState.isResearched(ResearchFeature.STEEL)) {
        				return ResourcePowerupFeature.getInstance("steel.powerup");
        			} else {
        				return getPowerup(); // Recurse
        			}
        		case 27:
        		case 28:
        			if ((gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) && gameState.isResearched(ResearchFeature.TITANIUM)) {
        				return ResourcePowerupFeature.getInstance("titanium.powerup");
        			} else {
        				return getPowerup(); // Recurse
        			}
        		case 29:
        			if ((gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) && gameState.isResearched(ResearchFeature.NANOMESH)) {
        				return ResourcePowerupFeature.getInstance("nanomesh.powerup");
        			} else {
        				return getPowerup(); // Recurse
        			}

        		default:
        			assert false;
        			return null;
        	}
        }
	}

	/*
	 * Game state
	 */

	/** Current metastate */
	private MetaState metaState;

	/** Current map */
	private GameMap map;

	/** Spawn points */
	private final ArrayList<SpawnPoint> spawnPoints = new ArrayList<SpawnPoint>();

	/** Debugging aid */
	private transient boolean forceDifficulty;

	/** Current total difficulty */
	private float currentDifficulty;

	/** Powerup difficulty */
	private float powerupDifficulty;

	/** Whether to begin the level */
	private boolean beginLevel;

	/** Money at start of level */
	private int startingMoney;

	/** Factories */
	private int factories, totalFactories;

	/** Warehouses */
	private int warehouses;

	/** Shield generators */
	private int shieldGenerators;

	/** Autoloaders */
	private int autoloaders;

	/** Spawners */
	private int spawners;

	/** Are we alive? */
	private boolean alive = true;

	/** Freeze tick */
	private int freezeTick;

	/** Shield tick */
	private int shieldTick;

	/** Capacitor boost */
	private int capacitorBoost;

	/** Cooling boost */
	private int coolingBoost;

	/** Battery boost */
	private int batteryBoost;

	/** Reactor boost */
	private int reactorBoost;

	/** Scanner boost */
	private int scannerBoost;

	/** Shield boost: extra hitpoints for all buildings */
	private int shieldBoost;

	/** Level duration */
	private int levelDuration;

	/** Aliens spawned */
	private int aliensSpawnedValue, aliensSpawnedAtLevelEnd;

	/** Aliens vanquished value */
	private int aliensVanquishedValue, aliensVanquishedSinceEndOfLevel;

	/** Last story index */
	private int currentStoryIndex;

	/** Something interesting happened this many ticks ago */
	private int somethingInterestingHappenedTick;

	/** Last hovered entity */
	private Entity lastHovered;

	/** Total value of buildings created on this level */
	private int valueOfBuiltBuildings;

	/** Number of buildings made this level */
	private int numberOfBuildingsMade;

	/** Total value of buildings destroyed by gidrahs on this level */
	private int valueOfDestroyedBuildings;

	/** Number of buildings destroyed this level */
	private int numberOfBuildingsDestroyed;

	/** Have any buildings been damaged on this level? (for Pristine medal) */
	private boolean anyDamaged;

	/** Number of crystal buildings */
	private int crystals, initialCrystals;

	/** The base */
	private Building base;

	/** Entities */
	private final ArrayList<Entity> entities = new ArrayList<Entity>();

	/** All the gidrahs currently on the level */
	private final ArrayList<Gidrah> gidrahs = new ArrayList<Gidrah>();

	/** All the bosses currently on the level */
	private final ArrayList<Gidrah> bosses = new ArrayList<Gidrah>();

	/** All the units currently on the level */
	private final ArrayList<Unit> units = new ArrayList<Unit>();

	/** All the buildings currently on the level */
	private final ArrayList<Building> buildings = new ArrayList<Building>();

	/** All the saucers currently on the level */
	private final ArrayList<Saucer> saucers = new ArrayList<Saucer>();

	/** Medals earned during play this level */
	private final Set<MedalFeature> medalsThisLevel = new HashSet<MedalFeature>();

	/** Armed capacitors */
	private final ArrayList<Capacitor> armedCapacitors = new ArrayList<Capacitor>();

	/** Event hint seq number */
	private int eventHintSeq;

	/** Unmined crystals warning */
	private int unminedTick;

	/** Game state interface */
	private final GameStateInterface gameStateInterface = new GameStateInterface() {

		private static final long serialVersionUID = 1L;

		@Override
		public void addToGidrahs(Gidrah gidrah) {
			gidrahs.add(gidrah);
			if (gidrah.getFeature().isBoss()) {
				bosses.add(gidrah);
				addStat(Stats.BOSSES_SPAWNED, 1);
			}
			if (gidrah.getFeature().isAngry()) {
				addStat(Stats.ANGRY_SPAWNED, 1);
			}
			if (gidrah.getFeature().isGidlet()) {
				addStat(Stats.GIDLETS_SPAWNED, 1);
			}
			aliensSpawnedValue += gidrah.getFeature().getValue();
			addStat(Stats.ALIENS_SPAWNED, 1);
		}

		@Override
		public void addToUnits(Unit unit) {
			units.add(unit);
		}

		@Override
		public void addToBuildings(Building building) {
			buildings.add(building);
			if (building.isCity()) {
				base = building;
			}
			onSomethingInterestingHappened();
		}

		@Override
		public void addToSaucers(Saucer saucer) {
			saucers.add(saucer);
			onSomethingInterestingHappened();
		}

		@Override
		public void removeFromBuildings(Building building) {
			buildings.remove(building);
			onSomethingInterestingHappened();
		}

		@Override
		public void removeFromGidrahs(Gidrah gidrah) {
			gidrahs.remove(gidrah);
			aliensVanquishedValue += gidrah.getFeature().getValue();
			if (!isLevelActive()) {
				aliensVanquishedSinceEndOfLevel += gidrah.getFeature().getValue();
			}
			onSomethingInterestingHappened();
		}

		@Override
		public void removeFromUnits(Unit unit) {
			units.remove(unit);
		}

		@Override
		public void removeFromSaucers(Saucer saucer) {
			saucers.remove(saucer);
		}

		@Override
		public void buffBatteries() {
			batteryBoost ++;
			onSomethingInterestingHappened();
		}

		@Override
		public void buffScanners() {
			scannerBoost ++;
			onSomethingInterestingHappened();
		}

		@Override
		public void buffCapacitors() {
			capacitorBoost ++;
			onSomethingInterestingHappened();
		}

		@Override
		public void buffCoolingTowers() {
			coolingBoost ++;
			onSomethingInterestingHappened();
		}

		@Override
		public void buffReactors() {
			reactorBoost ++;
			onSomethingInterestingHappened();
		}

		@Override
		public void buffShieldGenerators() {
			shieldBoost ++;
			for (Building building : buildings) {
				building.addShieldBoost();
			}
			onSomethingInterestingHappened();
		}

		@Override
		public void repairFully() {
			for (Building building : buildings) {
				building.repairFully();
			}
			if (metaState.addStat(Stats.REPAIRS_USED, 1) == 5) {
				awardMedal(Medals.JIMLL_FIX_IT);
			}
			onSomethingInterestingHappened();
		}

		@Override
		public void setSmartbombMode() {
			setBuilding(null);
			mode = Mode.MODE_SMARTBOMB;
			onSomethingInterestingHappened();
		}

		@Override
		public void freeze(int duration) {
			// Freeze all the gidrahs!
			freezeTick += duration;
			GameScreen.instance.onFreezeTimerIncreased(freezeTick);
			if (metaState.addStat(Stats.FREEZES_USED, 1) == 5) {
				awardMedal(Medals.TOILET_BREAK);
			}
			onSomethingInterestingHappened();
		}

		@Override
		public void bezerk(int duration) {
			bezerkTick += duration;
			GameScreen.getInstance().onBezerkTimerIncreased(bezerkTick);

			// Bezerk turrets reload instantly
			for (Building building : buildings) {
				building.onBezerk();
			}
			if (metaState.addStat(Stats.BEZERKS_USED, 1) == 5) {
				awardMedal(Medals.SHORT_TEMPERED);
			}
			onSomethingInterestingHappened();
		}

		@Override
		public void invulnerable(int duration) {
			shieldTick += duration;
			GameScreen.getInstance().onShieldTimerIncreased(shieldTick);
			if (metaState.addStat(Stats.SHIELDS_USED, 1) == 5) {
				awardMedal(Medals.BATFINK);
			}
			onSomethingInterestingHappened();
		}

		@Override
		public void addMoney(int delta) {
			WormGameState.this.addMoney(delta);
			onSomethingInterestingHappened();
		}

		@Override
		public int getAvailableStock(BuildingFeature bf) {
			return WormGameState.this.getAvailableStock(bf);
		}

		@Override
		public void addAvailableStock(BuildingFeature bf, int n) {
			WormGameState.this.addAvailableStock(bf, n);
		}

	};

	/** Ticks */
	private int tick, totalTicks, saucerTick, levelTick, bezerkTick, survivalSpawnPointTick, crystalTick;

	/** Next saucer at... */
	private int nextSaucer;

	/** Contains a shuffle bag of crystal sizes (Integers), valued between 1 and 3 inclusive */
	private final ArrayList<Integer> survivalCrystals = new ArrayList<Integer>();

	/** Survival mode: next boss at this tick */
	private int survivalNextBossTick;

	/** Survival mode: next crystal at this tick */
	private int survivalNextCrystalTick;

	/** Survival mode: next survival spawn point count increase at this tick */
	private int survivalNextSpawnPointTick;

	/** Survival mode: survival spawn point interval */
	private int survivalSpawnPointInterval;

	/** Number of survival spawnpoints that there should be */
	private int numSurvivalSpawnPoints;

	/** Survival mode gidrah unlock levels */
	private final int[] survivalGidrahUnlock = new int[] {1, 0, 0, 0};

	/** Survival mode gidrah unlock levels */
	private final int[] survivalGidrahUnlockNext = new int[] {0, 0, 0, 0};

	/** Survival mode gidrah kill counts */
	private final int[] survivalGidrahKills = new int[] {0, 0, 0, 0};

	/** Whether mouse button was down last tick */
	private transient boolean leftMouseWasDown, rightMouseWasDown;

	/** Wait for mouse button to be released */
	private transient boolean waitForMouse;

	/** Building we want to build */
	private transient BuildingFeature building;

	/** Build entity */
	private transient Building buildEntity;

	/** Things we've clicked on */
	private transient ArrayList<Entity> clicked;

	/** Back button runnable */
	private transient Runnable backButtonRunnable;

	/** RMB scroll */
	private transient boolean rmbScroll;

	/** Unmined crystals */
	private final ArrayList<Building> unminedCrystalList = new ArrayList<Building>();

	/** Game phase */
	private int phase;
	private static final int PHASE_NORMAL = 0;
	private static final int PHASE_END_OF_GAME = 1;
	private static final int PHASE_WAIT_FOR_GIDRAHS = 2;
	private static final int PHASE_WAIT_A_FEW_SECONDS = 3;

	/** Rush mode */
	private boolean rush;

	/** Whether we've expired the demo */
	private boolean demoExpired;

	/** Survival boss counter (earth, moon, mars, etc) */
	private int survivalBoss;

	/** Survival boss ticker */
	private int survivalBossTick;

	/** Input mode (see {@link Mode} */
	private int mode;

	/** Scroll origin */
	private transient int scrollOriginX, scrollOriginY;

	/** Are we in range of a capacitor? */
	private transient boolean capacitorRange;

	/** RMB drag sensor */
	private transient int rmbDragSensor;

	/** Hint map maps HintFeatures to Integers, which is the number of times that hint has been shown in sequence */
	private final Map<HintFeature, Integer> hintMap = new HashMap<HintFeature, Integer>();

	/** Stories for this level */
	private transient List<StoryFeature> currentStories;

	/** Survival init params */
	private SurvivalParams survivalParams;

	/** Sandbox init params */
	private SandboxParams sandboxParams;


	/** Suppress medals display on game screen */
	private boolean suppressMedals;

	/** Awesome awarded this level */
	private boolean awesome;

	/** Number of attempts */
	private transient int attempts;

	/**
	 * Gidrah spawn point
	 */
	private class SpawnPoint implements Serializable {
		private static final long serialVersionUID = 1L;

		int tileX, tileY, type, subType;
		final boolean edge;

		/** Gidrah generator tick */
		private int gidrahTick;

		/** Waiting for spawn point to clear */
		private int waitingToSpawn;

		/** Gidrah position indicator */
		private int gidrahsSpawned;

		/** Spawn boss */
		private GidrahFeature boss;

		/** Stop spawning */
		private boolean stop;

		/** More aliens please */
		private int moreAliens;

		/** Effect */
		private transient ArrowEffect arrowEffect;

		SpawnPoint(int tileX, int tileY, int type, boolean edge) {
			this.tileX = tileX;
			this.tileY = tileY;
			this.type = type;
			this.edge = edge;
			gidrahTick = Util.random(getInitialLevelDelay(map.getWidth(), map.getHeight()), getInitialLevelDelay(map.getWidth(), map.getHeight()) + 600);
			waitingToSpawn = 0;

			if (getGameMode() == GAME_MODE_SURVIVAL) {
				chooseType();
			}

			reinit();
		}

		void spawnBoss(GidrahFeature boss) {
			this.boss = boss;
			waitingToSpawn = 0;
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && obj instanceof SpawnPoint && ((SpawnPoint) obj).tileX == tileX && ((SpawnPoint) obj).tileY == tileY && ((SpawnPoint) obj).type == type;
		}

		@Override
		public int hashCode() {
			return (tileX ^ tileY << 16) * (type + 1);
		}

		/**
		 * Tick the spawnpoint. Returns true if still active
		 * @return
		 */
		boolean tick() {
			if (!base.isAlive()) {
				// No base?
				return !stop;
			}

			if (getGameMode() == GAME_MODE_XMAS) {
				if (levelTick % Xmas.BOSS_INTERVAL == 0 && levelTick > 0) {
					// Every 5 minutes spawn a boss
					type = 2;
				} else if (levelTick % Xmas.ANGRY_INTERVAL == 0 && levelTick > 0 && type == 0) {
					type = 1;
				}
			}

			if (gidrahs.size() >= MAX_GIDRAHS) {
				return !stop;
			}

			if (freezeTick > 0) {
				return !stop;
			}

			if (getGameMode() != GAME_MODE_SURVIVAL) {
				if (crystals == 0 && !isLevelActive()) {
					return !stop;
				}
			}

			if (waitingToSpawn > 0) {
				waitingToSpawn --;
				if (waitingToSpawn == 0) {
					spawnGidrah();
				}
			} else {
				if (stop) {
					return false;
				}
				if (boss != null) {
					spawnGidrah();
					stop = true;
					arrowEffect.remove();
					arrowEffect = null;
				} else if (--gidrahTick <= 0) {
					// Get next item
					gidrahsSpawned ++;

					// xmas mode: continuous stream
					if (getGameMode() == GAME_MODE_XMAS) {
						spawnGidrah();
					} else {
						if (gidrahsSpawned >= getWaveLength()) {
							// We've got to the end. Go into a very long delay and start again at the beginning
							gidrahTick = getLongGidrahDelay();
							gidrahsSpawned = 0;
							// In Survival we move the spawnpoint around, and choose a new alien to spawn
							if (getGameMode() == GAME_MODE_SURVIVAL) {
								moveSpawnPoint();
							} else {
								moreAliens = Math.min(3, moreAliens + 1);
							}
						} else {
							spawnGidrah();
						}
					}
				}
			}
			return true;
		}

		void moveSpawnPoint() {
			int ox = 0, oy = 0;
			if (tileX == -1) {
				tileY += Util.random(-2, 2);
				ox = 1;
				oy = 0;
			} else if (tileX == getMap().getWidth()) {
				tileY += Util.random(-2, 2);
				ox = 1;
				oy = 0;
			} else if (tileY == -1) {
				tileX += Util.random(-2, -2);
				ox = 0;
				oy = 1;
			} else if (tileY == getMap().getHeight()) {
				tileX += Util.random(-2, -2);
				ox = 0;
				oy = -1;
			}

			for (int z = 0; z < GameMap.LAYERS; z ++) {
				Tile t = getMap().getTile(tileX + ox, tileY + oy, z);
				if (t == null || t.isImpassable() || t.isSolid()) {
					// Zap!
					stop = true;
					remove();
					return;
				}
			}

			arrowEffect.setSpawnLocation(tileX, tileY);

			// Choose a new type
			chooseType();
		}

		void chooseType() {
			assert getGameMode() == GAME_MODE_SURVIVAL;
			int maxType = 0;
			for (int i = survivalGidrahUnlock.length; -- i >= 0; ) {
				if (survivalGidrahUnlock[i] != 0) {
					maxType = i;
					break;
				}
			}
			type = Util.random(0, maxType);
			subType = Util.random(0, Math.min(getNumTypes(type) - 1, survivalGidrahUnlock[type] - 1));
		}

		int getNumTypes(int type) {
			assert getGameMode() == GAME_MODE_SURVIVAL;
			return Res.getSurvivalGidrahs(type).getNumResources();
		}

		int getWaveLength() {
			switch (metaState.gameMode) {
				case GAME_MODE_CAMPAIGN:
					return getLevelInWorld() / 3 + 10 + moreAliens - Math.min(9, type * 3);
				case GAME_MODE_ENDLESS:
					return metaState.level / LEVELS_IN_WORLD + 10 + moreAliens - Math.min(9, type * 3);
				case GAME_MODE_SURVIVAL:
					return config.getSurvivalWaveLength() - Math.max(0, type * 3 - Math.max(0, getLevelTick() - config.getSurvivalWaveLengthTimeOffset()) / config.getSurvivalWaveLengthTimeAdjust());
				case GAME_MODE_XMAS:
					assert false;
					return -1; // Constant stream
				case GAME_MODE_SANDBOX:
					return metaState.level / LEVELS_IN_WORLD + 10 + moreAliens - Math.min(9, type * 3);
				default:
					assert false : "Unknown game mode " + metaState.gameMode;
					return 1;
			}
		}

		void spawnGidrah() {
			if (map.isOccupied(tileX, tileY)) {
				if (metaState.gameMode == GAME_MODE_XMAS) {
					waitingToSpawn = 1;
				} else {
					waitingToSpawn = config.getSpawnDelay();
				}
			} else {
				waitingToSpawn = 0;
				if (boss != null) {
					boss.spawn(GameScreen.getInstance(), tileX, tileY, 0);
					boss = null;
					gidrahTick = Util.random(getInitialLevelDelay(map.getWidth(), map.getHeight()), getInitialLevelDelay(map.getWidth(), map.getHeight()) + 600);
				} else {
					GidrahFeature gf;
					if (getGameMode() == GAME_MODE_XMAS) {
						// Choose gidrah based on level duration so far
						float ratio = levelTick == getLevelDuration() ? 0.99999f : (float) levelTick / (float) getLevelDuration();
						int gidIndex = Util.random(0, (int) LinearInterpolator.instance.interpolate(0.0f, Res.getXmasGidrahs().getNumResources(), ratio));
						switch (type) {
							case 0: // Ordinary gid
								gf = (GidrahFeature) Res.getXmasGidrahs().getResource(gidIndex);
								break;
							case 1: // Angry gid
								gf = (GidrahFeature) Res.getXmasAngryGidrahs().getResource(gidIndex);
								type = 0;
								break;
							case 2: // Boss
								gf = Res.getXmasBoss(survivalBoss ++);
								doBossWarning(1);
								// Stop when last boss made
								if (survivalBoss == Xmas.MAX_BOSSES) {
									stop = true;
								}
								type = 0;
								break;
							default:
								assert false;
								return;
						}
					} else if ((getGameMode() == GAME_MODE_SURVIVAL || metaState.level > 2) && gidrahsSpawned == 1) {
						// Maybe an angry gidrah at the head of the column?
						if (Util.random() < getDifficulty()) {
							gf = getAngryGidrah(type, subType);
						} else {
							gf = getGidrah(type, subType);
						}
					} else {
						gf = getGidrah(type, subType);
					}
					gf.spawn(GameScreen.getInstance(), tileX, tileY, type);
					gidrahTick = 1;
				}
			}
		}

		int getLongGidrahDelay() {
			switch (getGameMode()) {
				case GAME_MODE_SURVIVAL:
					return config.getLongDelay() + (int) LinearInterpolator.instance.interpolate(config.getSpawnpointDelayAdjust() * 5.0f, 0.0f, getDifficulty());
				case GAME_MODE_XMAS:
					return 0; // No delay
				default:
					return getShortGidrahDelay() * 2;
			}
		}

		int getShortGidrahDelay() {
			return config.getLongDelay() + Math.max(0, spawnPoints.size() - metaState.level / LEVELS_IN_WORLD) * config.getSpawnpointDelayAdjust();
		}

		void reinit() {
			arrowEffect = new ArrowEffect(tileX, tileY);
			arrowEffect.spawn(GameScreen.getInstance());
			arrowEffect.setOffset(null);
		}

		void remove() {
			if (arrowEffect != null) {
				arrowEffect.remove();
				arrowEffect = null;
			}
			map.clearItem(tileX, tileY);
			stop = true;
		}

	}

	/**
	 * C'tor
	 */
	public WormGameState(int mode) {
		this.config = GameConfiguration.getInstance();
		this.metaState = new MetaState(mode);
		reset();
	}

	private GidrahFeature getAngryGidrah(int type, int subType) {
		assert getGameMode() != GAME_MODE_XMAS;
		switch (getGameMode()) {
			case GAME_MODE_SURVIVAL:
				return (GidrahFeature) Res.getSurvivalAngryGidrahs(type).getResource(subType);
			default:
				return getLevelFeature().getAngryGidrah(type);
		}
	}

	private GidrahFeature getGidrah(int type, int subType) {
		assert getGameMode() != GAME_MODE_XMAS;
		switch (getGameMode()) {
			case GAME_MODE_SURVIVAL:
				return (GidrahFeature) Res.getSurvivalGidrahs(type).getResource(subType);
			default:
				return getLevelFeature().getGidrah(type);
		}
	}

	/**
	 * Repair all the buildings by one shield
	 */
	public void repair() {
		for (Iterator<Building> i = buildings.iterator(); i.hasNext(); ) {
			i.next().repair();
		}
	}

	/**
	 * Get the player's money
	 * @return int
	 */
	public int getMoney() {
		return metaState.money;
	}

	/**
	 * Adjust the player's money
	 * @param moneyDelta The amount of money to adjust the player's purse by
	 */
	public void addMoney(int moneyDelta) {
		metaState.money += moneyDelta;
		if (metaState.money < 0) {
			metaState.money = 0;
		}
	}

	/**
	 * Add an entity to the game
	 * @param entity
	 */
	public void addEntity(Entity entity) {
		entities.add(entity);

		// Now get the entity to add itself to the appropriate list
		entity.addToGameState(gameStateInterface);
	}

	/**
	 * Add a powerup
	 * @param powerup
	 * @param doSound TODO
	 */
	public void addPowerup(PowerupFeature powerup, boolean doSound) {
		Integer num = metaState.powerups.get(powerup);
		int n;
		if (num == null) {
			n = 1;
		} else {
			n = num.intValue() + 1;
		}
		metaState.powerups.put(powerup, new Integer(n));
		if (doSound && powerup != null) {
			Game.allocateSound(powerup.getCollectSound());
		}
		calcPowerupDifficulty();
		GameScreen.onPowerupsUpdated();
	}

	private void calcPowerupDifficulty() {
		powerupDifficulty = 0.0f;
		for (PowerupFeature pf : metaState.powerups.keySet()) {
			powerupDifficulty += pf.getDifficulty();
		}
	}

	/**
	 * Remove a powerup
	 * @param powerup
	 */
	public void removePowerup(PowerupFeature powerup) {
		Integer num = metaState.powerups.get(powerup);
		int n;
		if (num == null) {
			assert false;
			return;
		} else {
			n = num.intValue() - 1;
		}
		if (n == 0) {
			metaState.powerups.remove(powerup);
		} else {
			metaState.powerups.put(powerup, new Integer(n));
		}
		calcPowerupDifficulty();
		GameScreen.onPowerupsUpdated();
	}

	/**
	 * Return the number of powerups of this type the player has
	 * @param powerup
	 * @return int
	 */
	public int getNumPowerups(PowerupFeature powerup) {
		Integer num = metaState.powerups.get(powerup);
		if (num == null) {
			return 0;
		} else {
			return num.intValue();
		}
	}

	/**
	 * Use a powerup
	 * @param powerup
	 */
	public void usePowerup(PowerupFeature powerup) {
		if (getNumPowerups(powerup) == 0) {
			return;
		}
		removePowerup(powerup);
		Game.allocateSound(powerup.getCollectSound());
		powerup.activate(gameStateInterface);
	}

	/**
	 * Remove an entity from the game
	 * @param entity
	 */
	public void removeEntity(Entity entity) {
		// We don't actually remove the entity from entities list here; that occurs
		// for us in checkCollisions().

		// Now get the entity to remove itself from the appropriate list
		entity.removeFromGameState(gameStateInterface);
	}

	/**
	 * Cleanup
	 */
	public void cleanup() {
		for (int i = 0; i < entities.size(); i ++) {
			Entity e = entities.get(i);
			if (e.isActive()) {
				e.remove();
			}
		}
		Entity.reset();
		System.gc();
	}

	/**
	 * Adjust the number of factories in play
	 * @param n
	 */
	public void addFactories(int n) {
		factories += n;
		if (n > 0) {
			totalFactories += n;
		}
		beginLevel = true;
		assert factories >= 0;
	}

	public void addCrystals(int n) {
		crystals += n;
		if (crystals == 0 && getGameMode() != GAME_MODE_SURVIVAL && getGameMode() != GAME_MODE_XMAS) {
			// Mined all crystals before timer expires?
			if (levelTick < getLevelDuration() && beginLevel) {
				awardMedal(Medals.EFFICIENT);
			}
		}
	}

	public void addInitialCrystals(int n) {
		initialCrystals += n;
	}

	/**
	 * @return the number of factories in play
	 */
	public int getFactories() {
		return factories;
	}

	public int getAutoloaders() {
		return autoloaders;
	}

	public int getWarehouses() {
		return warehouses;
	}

	public void addWarehouses(int n) {
		warehouses += n;
	}

	public int getShieldGenerators() {
		return shieldGenerators;
	}

	public void addShieldGenerators(int n) {
		shieldGenerators += n;
	}

	public void addAutoloaders(int n) {
		autoloaders += n;
	}

	/**
	 * Adjust the number of spawners in play
	 * @param n
	 */
	public void addSpawners(int n) {
		spawners += n;
		beginLevel = true;
		assert spawners >= 0;
	}

	/**
	 * @return the number of spawners in play
	 */
	public int getSpawners() {
		return spawners;
	}

	/**
	 * @return Returns the tick.
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * @return Returns the totalTicks.
	 */
	public int getTotalTicks() {
		return totalTicks;
	}

	/**
	 * Reset the total tick count
	 */
	public void resetTotalTicks() {
		totalTicks = 0;
	}

	/**
	 * Is the player still "alive"?
	 * @return true if the player hasn't lost a base
	 */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * Called when a base is destroyed or built
	 */
	public void addBases(int n) {
		if (n == -1) {
			kill();
		}
	}

	/**
	 * Are we playing the game?
	 * @return true if we're in PHASE_NORMAL or WAIT_FOR_GIDRAHS
	 */
	public boolean isPlaying() {
		return phase == PHASE_NORMAL || phase == PHASE_WAIT_FOR_GIDRAHS;
	}

	/**
	 * @return true if we're in rush mode
	 */
	public boolean isRushActive() {
		return rush;
	}

	/**
	 * End the level
	 */
	private void endLevel() {
		GameScreen.onEndLevel();

		// Cancel build mode
		setBuilding(null);

		// Shut down etc.
		shutdownFactories();

		if (metaState.level == 19 && !Game.isRegistered()) {
			expireDemo();
		} else {
			// And open the intermission screen
			suppressMedals = true;
			IntermissionScreen.show();
		}
	}

	public static int getDifficultyAdjust(int level, int gameMode) {
		return Worm.getExtraLevelData(Game.getPlayerSlot(), level, gameMode, "diff", 0);
	}

	public static void setDifficultyAdjust(int level, int gameMode, int diff) {
		Worm.setExtraLevelData(level, gameMode, "diff", diff);
	}

	/**
	 * Checks for new medals at the end of the level. Right now, can create up to *9* medals, which will have to be listed on the
	 * {@link IntermissionScreen}. Check what got created with {@link #getMedalsEarnedThisLevel()}, which could have a whole bunch
	 * more medals in it!
	 */
	public void checkForNewMedals() {
		if (base.isAlive() && base.getHitPoints() <= 4) {
			awardMedal(Medals.TAPE_AND_STRING);
		}

		if (metaState.gameMode == GAME_MODE_CAMPAIGN) {
			// Award various medals
			int diff = getDifficultyAdjust(metaState.level, GAME_MODE_CAMPAIGN);
			if (diff == 0) {
				// Gold!
				awardMedal(Medals.GOLD);
			} else if (diff == 1) {
				// Silver!
				awardMedal(Medals.SILVER);
			} else if (diff == 2) {
				// Bronze!
				awardMedal(Medals.BRONZE);
			}

			boolean good = false;
			if (getLevel() == 9) {
				good = true;
				awardMedal(Medals.EARTH_COMPLETE);
				for (int i = 0; i < 10; i ++) {
					if (getDifficultyAdjust(i, metaState.gameMode) > 0) {
						good = false;
						break;
					}
				}
			} else if (getLevel() == 19) {
				good = true;
				awardMedal(Medals.MOON_COMPLETE);
				for (int i = 10; i < 20; i ++) {
					if (getDifficultyAdjust(i, metaState.gameMode) > 0) {
						good = false;
						break;
					}
				}
			} else if (getLevel() == 29) {
				good = true;
				awardMedal(Medals.MARS_COMPLETE);
				for (int i = 20; i < 30; i ++) {
					if (getDifficultyAdjust(i, metaState.gameMode) > 0) {
						good = false;
						break;
					}
				}
			} else if (getLevel() == 39) {
				good = true;
				awardMedal(Medals.SATURN_COMPLETE);
				for (int i = 30; i < 40; i ++) {
					if (getDifficultyAdjust(i, metaState.gameMode) > 0) {
						good = false;
						break;
					}
				}
			} else if (getLevel() == 49) {
				good = true;
				awardMedal(Medals.TITAN_COMPLETE);
				for (int i = 40; i < 50; i ++) {
					if (getDifficultyAdjust(i, metaState.gameMode) > 0) {
						good = false;
						break;
					}
				}
				if (good) {
					boolean reallyGood = true;
					for (int i = 0; i < 50; i ++) {
						if (getDifficultyAdjust(i, metaState.gameMode) > 0) {
							reallyGood = false;
							break;
						}
					}
					if (reallyGood) {
						awardMedal(Medals.PERFECT_GAME);
					}
				}
			}
			if (good) {
				awardMedal(Medals.PERFECT_WORLD);
			}
		}

		if (!anyDamaged) {
			awardMedal(Medals.PRISTINE);
		} else if (valueOfDestroyedBuildings == 0) {
			awardMedal(Medals.CAREFUL);
		} else if ((float) numberOfBuildingsDestroyed / (float) numberOfBuildingsMade >= 0.9f) {
			awardMedal(Medals.SKIN_OF_YOUR_TEETH);
		}

		if (getMoney() - startingMoney >= 15000) {
			awardMedal(Medals.SHORT_ARMS_DEEP_POCKETS);
		} else if (getMoney() - startingMoney >= 10000) {
			awardMedal(Medals.TIGHT_FISTED);
		} else if (getMoney() - startingMoney >= 5000) {
			awardMedal(Medals.THRIFTY);
		}

		if (getMoney() >= 50000) {
			awardMedal(Medals.HOARDED_$50000);
		} else if (getMoney() >= 25000) {
			awardMedal(Medals.HOARDED_$25000);
		} else if (getMoney() >= 10000) {
			awardMedal(Medals.HOARDED_$10000);
		} else if (getMoney() >= 5000) {
			awardMedal(Medals.HOARDED_$5000);
		}

	}

	/**
	 * Store the state of play at the end of a level so it's ready to be used by the next level
	 */
	public void checkPoint() {
		metaState.level ++;

		if (Worm.getMaxLevel(metaState.gameMode) < metaState.level) {
			Worm.setMaxLevel(metaState.level, metaState.gameMode);
		}
		Worm.setExtraLevelData(metaState.level, metaState.gameMode, "money", metaState.money);

		int diff = getDifficultyAdjust(metaState.level, metaState.gameMode);
		if (diff > 0) {
			setDifficultyAdjust(metaState.level, metaState.gameMode, diff - 1);
		}

		try {
			metaState.save();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		// Just be sure...
		Game.flushPrefs();

		metaState.level --;
	}

	/**
	 * End the game
	 */
	private void endGame() {
		setBuilding(null);
		phase = PHASE_END_OF_GAME;
		tick = 0;
	}


	/**
	 * Sell a building
	 * @param building
	 */
	public void sell(Building building) {
		building.destroy(true);
		SFX.sold();
		LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getTinyFont(), "$"+building.getSalePrice(), ReadableColor.WHITE, new Color(255, 255, 100), 30, 10);
		le.setAcceleration(0.0f, -0.025f);
		le.setVelocity(0.0f, 1.0f);
		le.setLocation(building.getMapX() + building.getCollisionX(),  building.getMapY() + building.getFeature().getBounds().getHeight());
		le.spawn(GameScreen.getInstance());
		addMoney(building.getSalePrice());
		Worm.getGameState().flagHint(Hints.DRAGSELL);
		if (building.isWorthAttacking()) {
			addStat(Stats.SOLD, 1);
		}
		addAvailableStock(building.getFeature(), 1);

		// If selling stuff when there are no gidrahs and level ended, there's no point...
		if (beginLevel && gidrahs.size() == 0) {
			flagHint(Hints.DONTSELL);
		}
	}

	/**
	 * Get the level duration, in ticks
	 * @return int
	 */
	public int getLevelDuration() {
		return levelDuration;
	}

	private void calcLevelDuration(int w, int h) {
		switch (metaState.gameMode) {
			case GAME_MODE_CAMPAIGN:
				float min, max; // In seconds
				switch (getLevel() / LEVELS_IN_WORLD) {
					case 0: // Earth
						min = 30.0f;
						max = 90.0f;
						break;
					case 1: // Moon
						min = 90.0f;
						max = 150.0f;
						break;
					case 2: // Mars
						min = 150.0f;
						max = 210.0f;
						break;
					case 3: // Saturn
						min = 210.0f;
						max = 270.0f;
						break;
					case 4: // Titan
						min = 270.0f;
						max = 300.0f;
						break;
					default:
						assert false : "Bad world for campaign: "+metaState.level;
						min = max = 300.0f;
				}
				levelDuration = (int) LinearInterpolator.instance.interpolate(min, max, getLevelInWorld() / (LEVELS_IN_WORLD - 1.0f)) * 60 + getInitialLevelDelay(w, h); // *60 converts seconds to ticks
				return;

			case GAME_MODE_ENDLESS:
				levelDuration = getInitialLevelDelay(w, h) + getLevel() * 300 + 1800; // 5 seconds per level
				return;

			case GAME_MODE_SURVIVAL:
			case GAME_MODE_SANDBOX:
				levelDuration = -1;
				return;

			case GAME_MODE_XMAS:
				levelDuration = Xmas.DURATION;
				return;

			default:
				assert false : "Unknown game mode "+metaState.gameMode;
				return;
		}
	}

	/**
	 * @return time in ticks to delay before starting enemy spawning
	 */
	private int getInitialLevelDelay(int w, int h) {
		if (metaState.level < 4 && getGameMode() != GAME_MODE_SURVIVAL && getGameMode() != GAME_MODE_XMAS) {
			return 0;
		}
		return (int) (Math.sqrt(w * h) * config.getInitialLevelDelayFactor());
	}

	/**
	 * Get the current level tick
	 * @return int
	 */
	public int getLevelTick() {
		return levelTick;
	}

	/**
	 * Quantize a coordinate to the map resolution
	 * @param coord
	 * @return a quantized coordinate
	 */
	public static float quantize(float coord) {
		return (float) ((int) coord / MAP_RESOLUTION) * MAP_RESOLUTION;
	}

	private void tickSurvivalBosses() {
		survivalBossTick ++;

		if (survivalBossTick > survivalNextBossTick) {
			survivalBossTick = 0;
			survivalNextBossTick += config.getSurvivalBossIntervalAdjust();
			createSurvivalBosses();
		}
	}

	/**
	 * Ticks all the spawnpoints.
	 * @return true if any spawnpoint is still active
	 */
	private boolean tickSpawnPoints() {
		// Survival mode: occasionally create a new spawnpoint
		if (getGameMode() == GAME_MODE_SURVIVAL) {
			survivalSpawnPointTick ++;
			if (survivalSpawnPointTick >= survivalNextSpawnPointTick) {
				survivalSpawnPointInterval += config.getSurvivalSpawnpointSpawnIntervalAdjust();
				survivalNextSpawnPointTick += survivalSpawnPointInterval;
				numSurvivalSpawnPoints = Math.min(config.getBaseMaxSurvivalSpawnpoints() + getMap().getWidth() / config.getSurvivalSpawnpointsPerMapSize(), numSurvivalSpawnPoints + 1);
			}

			while (spawnPoints.size() < numSurvivalSpawnPoints) {
				createSurvivalSpawnPoint();
			}
		}

		boolean active = false;
		for (int i = 0; i < spawnPoints.size(); ) {
			SpawnPoint sp = spawnPoints.get(i);
			boolean wasActive = sp.tick();
			active |= wasActive;
			if (!wasActive) {
				sp.remove();
				spawnPoints.remove(i);
			} else {
				i ++;
			}
		}
		return active;
	}

	/**
	 * Is the mouse pointer in range of a capacitor?
	 * @return boolean
	 */
	public boolean inRangeOfCapacitor() {
		return capacitorRange;
	}

	/**
	 * Check for clicking and set the mouse pointer to the right animation for whatever's going on
	 */
	private void checkMouse() {
		if (GameScreen.getInstance().isBlocked()) {
			return;
		}
		if (GameScreen.isSomethingElseGrabbingMouse()) {
			return;
		}
		// Ignore mouse on active areas, unless grabbed
		int mouseX = GameScreen.getInstance().getMouseX();
		int mouseY = GameScreen.getInstance().getMouseY();
		List<Area> areasUnderMouse = GameScreen.getInstance().getAreasUnder(mouseX, mouseY);
		if (GameScreen.getInstance().getGrabbed() == null) {
			for (Area area : areasUnderMouse) {
				if (area.isFocusable()) {
					if (buildEntity != null) {
						buildEntity.setVisible(false);
					}
					Worm.setMouseAppearance(Res.getMousePointer());
					return;
				}
			}
		}

		if (buildEntity != null && !buildEntity.isVisible()) {
			buildEntity.setVisible(true);
		}

		float x = mouseX - GameScreen.getSpriteOffset().getX();
		float y = mouseY - GameScreen.getSpriteOffset().getY();
		int n = entities.size();
		Entity hovered = null;
		if (clicked == null) {
			clicked = new ArrayList<Entity>(8);
		} else {
			clicked.clear();
		}
		for (int i = 0; i < n; i ++) {
			Entity entity = entities.get(i);
			if (entity.isTouching(x, y)) {
				if (entity.isClickable()) {
					clicked.add(entity);
					hovered = entity;
				} else if (hovered == null && entity.isHoverable()) {
					hovered = entity;
				}
			}
		}

		// Check range of capacitors
		capacitorRange = false;
		TEMP_CAPACITORS.clear();
		for (int i = 0; i < buildings.size(); i ++) {
			Building b = buildings.get(i);
			if (b.isAlive() && b instanceof Capacitor) {
				Capacitor capacitor = (Capacitor) b;
				if (b.getDistanceTo(x, y) <= capacitor.getZapRadius()) {
					capacitorRange = true;
					TEMP_CAPACITORS.add(capacitor);
				}
			}
		}

		if (mode == Mode.MODE_BUILD) {
			buildEntity.setLocation(quantize(x - building.getBounds().getX() - building.getBounds().getWidth() / 2 + MAP_RESOLUTION / 2), quantize(y - building.getBounds().getY() - building.getBounds().getHeight() / 2 + MAP_RESOLUTION / 2));
		}

		boolean auxDown = false;
		int count = Mouse.getButtonCount();
		for (int i = 2; i < count; i ++) {
			if (Mouse.isButtonDown(i)) {
				auxDown = true;
				break;
			}
		}

		if (Mouse.isButtonDown(0)) {
			if (waitForMouse) {
				return;
			}

			GameScreen.onMapGrabbedMouse(true);

			switch (mode) {
				case Mode.MODE_SELL:
				case Mode.MODE_NORMAL:
				case Mode.MODE_BUILD:
				case Mode.MODE_DRAG:

					// Left mouse down in Normal mode. If we're over something, we click on it unless in "no click" mode. If we're not over something, and in capacitor range,
					// we enter zap mode. If we're outside capacitor range, we set "no click" mode.
					if (clicked.size() > 0) {
						if (leftMouseWasDown) {
							// But we're in "no click" mode...
							return;
						}

						// Ok, enter no click mode
						if (mode != Mode.MODE_DRAG) {
							leftMouseWasDown = true;
						}

						// Hovering over something. Pick the first one and let that decide what mouse pointer to use.
						Worm.setMouseAppearance(clicked.get(0).getMousePointer(true));
						boolean fire = false, maybeSelect = false;
						for (Iterator<Entity> i = clicked.iterator(); i.hasNext(); ) {
							Entity clickable = i.next();
							switch (clickable.onClicked(mode)) {
								case ClickAction.IGNORE:
									maybeSelect = true;
									continue;
								case ClickAction.DRAG:
									// Allow dragging over factories & turrets. Unless in build mode.
									if (mode == Mode.MODE_BUILD) {
										waitForMouse = true;
										leftMouseWasDown = true;
									} else {
										mode = Mode.MODE_DRAG;
										leftMouseWasDown = false;
									}
									return;
								case ClickAction.CONSUME:
									if (mode == Mode.MODE_SELL) {
										// Allow drag
										leftMouseWasDown = false;
										waitForMouse = false;
									} else {
										waitForMouse = true;
										leftMouseWasDown = true;
									}
									return;

								case ClickAction.FIRE:
									if (mode == Mode.MODE_NORMAL) {
										fire = true;
									} else {
										// Build or drag mode; don't zap
									}
									break;
							}
						}

						if (!fire && mode == Mode.MODE_NORMAL) {
							if (maybeSelect) {
								mode = Mode.MODE_SCROLL;
								Worm.setMouseAppearance(Res.getMousePointerGrab());
								scrollOriginX = mouseX;
								scrollOriginY = mouseY;
							}
							return;
						}

					} else {
						if (mode == Mode.MODE_DRAG) {
							Worm.setMouseAppearance(Res.getMousePointer());
						}
					}

					if (mode == Mode.MODE_DRAG || mode == Mode.MODE_SELL || mode == Mode.MODE_SCROLL || mode == Mode.MODE_SELECT) {
						return;
					}

					if (mode == Mode.MODE_BUILD) {
						build();
						break;
					}


					if (!capacitorRange) {
						leftMouseWasDown = true;
						Worm.setMouseAppearance(Res.getMousePointer());
						if (mode == Mode.MODE_NORMAL) {
							// Go into scroll mode
							mode = Mode.MODE_SCROLL;
							scrollOriginX = mouseX;
							scrollOriginY = mouseY;
							leftMouseWasDown = true;
							Worm.setMouseAppearance(Res.getMousePointerGrab());
						}
						return;
					} else {
						leftMouseWasDown = true;
						mode = Mode.MODE_ZAP;
						armedCapacitors.clear();
						armedCapacitors.addAll(TEMP_CAPACITORS);
					}
					// Intentional fall-through

				case Mode.MODE_ZAP:
					// LMB down in ZAP mode. If we are inside capacitor range,
					// fire. Otherwise do nothing.
					for (int i = 0; i < armedCapacitors.size(); i ++) {
						Capacitor capacitor = armedCapacitors.get(i);
						if (capacitor.isAlive()) {
							capacitor.zap(x, y);
						}
					}
					if (armedCapacitors.size() == 0) {
						mode = Mode.MODE_NORMAL;
					}
					if (clicked.size() > 0) {
						Worm.setMouseAppearance(clicked.get(0).getMousePointer(true));
					} else if (isBezerk()) {
						Worm.setMouseAppearance(capacitorRange ? Res.getMousePointerBezerkOffTarget() : Res.getMousePointerBezerkOutOfRange());
					} else {
						Worm.setMouseAppearance(capacitorRange ? Res.getMousePointerOffTarget() : Res.getMousePointerOutOfRange());
					}

					break;

				case Mode.MODE_SMARTBOMB:
					// Boooooooooom!
					Smartbomb bomb = new Smartbomb(x, y);
					bomb.spawn(GameScreen.getInstance());
					mode = Mode.MODE_NORMAL;
					Worm.setMouseAppearance(Res.getMousePointerOffTarget()); // That'll probably change immediately next frame
					waitForMouse = true;
					if (metaState.addStat(Stats.SMARTBOMBS_USED, 1) == 5) {
						awardMedal(Medals.THE_ONLY_WAY_TO_BE_SURE);
					}
					break;

				case Mode.MODE_SCROLL:
					// Scroll the screen around under the mouse
					int dx = scrollOriginX - mouseX;
					int dy = scrollOriginY - mouseY;
					scrollOriginX = mouseX;
					scrollOriginY = mouseY;
					GameScreen.getInstance().scroll(dx * MOUSE_DRAG_SPEED, dy * MOUSE_DRAG_SPEED);
					break;

				case Mode.MODE_SELECT:
					// Do nothing
					return;

				default:
					assert false;
			}

		} else if (auxDown) {
			// Sell!
			GameScreen.onMapGrabbedMouse(true);
			for (Iterator<Entity> i = clicked.iterator(); i.hasNext(); ) {
				Entity entity = i.next();
				if (entity instanceof Building) {
					Building building = (Building) entity;
					if (building.canSell()) {
						sell(building);
					}
				}
			}
		} else {
			GameScreen.onMapGrabbedMouse(false);
			boolean rmbDown = Mouse.getButtonCount() > 1 && Mouse.isButtonDown(1) || Game.wasKeyPressed(Keyboard.KEY_ESCAPE);
			if (rmbDown) {
				if (waitForMouse) {
					return;
				}
				if (!rightMouseWasDown) {
					scrollOriginX = mouseX;
					scrollOriginY = mouseY;
					rmbDragSensor = 0;
				}
				leftMouseWasDown = false;
				rightMouseWasDown = true;

				int dx = scrollOriginX - mouseX;
				int dy = scrollOriginY - mouseY;
				scrollOriginX = mouseX;
				scrollOriginY = mouseY;
				if (dx != 0 || dy != 0) {
					rmbDragSensor ++;
					if (rmbDragSensor > RMB_DRAG_SENSITIVITY) {
						GameScreen.getInstance().scroll(dx * MOUSE_DRAG_SPEED, dy * MOUSE_DRAG_SPEED);
						rmbScroll = true;
						Worm.setMouseAppearance(Res.getMousePointerGrab());
					}
				}
			} else {
				if (rightMouseWasDown && !rmbScroll) {
					// It was a RMB click. Cancel building / selling / blowing stuff up. Or pick up whatever building is under the mouse to build.
					switch (mode) {
						case Mode.MODE_BUILD:
							setBuilding(null);
							rightMouseWasDown = false;
							return;
						case Mode.MODE_SELL:
							setSellMode(false);
							rightMouseWasDown = false;
							return;
						case Mode.MODE_DRAG:
						case Mode.MODE_NORMAL:
						case Mode.MODE_SELECT:
						case Mode.MODE_ZAP:
							for (Iterator<Entity> i = clicked.iterator(); i.hasNext(); ) {
								Entity clickable = i.next();
								if (clickable instanceof Building) {
									BuildingFeature bf = ((Building) clickable).getFeature();
									if (bf.isAvailable()) {
										clickable.onLeave(mode);
										lastHovered = null;
										setBuilding(bf);
										rightMouseWasDown = false;
										return;
									}
								}
							}
							break;
						case Mode.MODE_SMARTBOMB:
							// Cancel the smartbomb and return it to the stores
							addPowerup(SmartbombPowerupFeature.getInstance(), false);
							mode = Mode.MODE_NORMAL;
							rightMouseWasDown = false;
							return;
						case Mode.MODE_SCROLL:
							break;
						default:
							assert false;
					}
				}
				rmbScroll = false;
				rightMouseWasDown = false;
				leftMouseWasDown = false;
				waitForMouse = false;

				if (mode == Mode.MODE_BUILD) {
					if (canBuild(false)) {
						Worm.setMouseAppearance(Res.getMousePointer());
					} else {
						Worm.setMouseAppearance(Res.getMousePointerCantBuild());
					}

				} else if (hovered != null) {
					Worm.setMouseAppearance(hovered.getMousePointer(false));
					if (lastHovered != hovered) {
						if (lastHovered != null) {
							lastHovered.onLeave(mode);
						}
						lastHovered = hovered;
						hovered.onHovered(mode);
					}

				} else {
					if (lastHovered != null) {
						lastHovered.onLeave(mode);
						lastHovered = null;
					}

					if (mode == Mode.MODE_SMARTBOMB) {
						Worm.setMouseAppearance(Res.getMousePointerSmartbomb());
					} else if (mode == Mode.MODE_SELL) {
						Worm.setMouseAppearance(Res.getMousePointerSellOff());
					} else if (isBezerk()) {
						Worm.setMouseAppearance(capacitorRange ? Res.getMousePointerBezerkOffTarget() : Res.getMousePointerBezerkOutOfRange());
					} else {
						Worm.setMouseAppearance(capacitorRange ? Res.getMousePointerOffTarget() : Res.getMousePointerOutOfRange());
					}
				}

				if (mode == Mode.MODE_ZAP || mode == Mode.MODE_DRAG || mode == Mode.MODE_SCROLL || mode == Mode.MODE_SELECT) {
					mode = Mode.MODE_NORMAL;
				}
			}
		}
	}

	/**
	 * Can the build entity be drawn where it is?
	 * @param doingBuild If we're actually attempting to build
	 * @return true if we can build here
	 */
	private boolean canBuild(boolean doingBuild) {
		// Can we build?
		if (!buildEntity.canBuild()) {
			return false;
		}

		// Don't allow us to overlap with any solid entities
		buildEntity.getBounds(TEMPBOUNDS);
		// Ensure no part of the building is on a tile that can't be built upon or off the map
		if (TEMPBOUNDS.getX() < 0 || TEMPBOUNDS.getY() < 0 || TEMPBOUNDS.getX() + TEMPBOUNDS.getWidth() >= map.getWidth() * MapRenderer.TILE_SIZE || TEMPBOUNDS.getY() + TEMPBOUNDS.getHeight() >= map.getHeight() * MapRenderer.TILE_SIZE) {
			return false;
		}
		for (int y = TEMPBOUNDS.getY() / MapRenderer.TILE_SIZE; y <= (TEMPBOUNDS.getY() + TEMPBOUNDS.getHeight() - 1) / MapRenderer.TILE_SIZE; y ++) {
			for (int x = TEMPBOUNDS.getX() / MapRenderer.TILE_SIZE; x <= (TEMPBOUNDS.getX() + TEMPBOUNDS.getWidth() - 1) / MapRenderer.TILE_SIZE; x ++) {
				for (int z = 0; z < GameMap.LAYERS; z ++) {
					Tile tile = map.getTile(x, y, z);
					if (tile != null && (tile.isSolid() || tile.isImpassable())) {
						return false;
					}
				}
			}
		}

		Entity.getCollisions(TEMPBOUNDS, BUILD_COLLISIONS);
		int n = BUILD_COLLISIONS.size();
		for (int i = 0; i < n; i ++) {
			Entity entity = BUILD_COLLISIONS.get(i);
			if (entity.isActive() && entity.canCollide() && entity.isTouching(buildEntity) && !buildEntity.canBuildOnTopOf(entity)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Build a building at the specified location.
	 * @param x
	 * @param y
	 */
	private void build() {

		// Make all buildings paintable
		//waitForMouse = !building.isPaintable();

		if (!canBuild(true)) {
			return;
		}

		// Unfortunately need to do these nice cheap checks after canBuild check, to prevent double labels being drawn
		if (!isBuildingAvailable()) {
			SFX.insufficientFunds();
			waitForMouse = true;
			LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getTinyFont(), Game.getMessage("ultraworm.wormgamestate.no_longer_available"), ReadableColor.WHITE, ReadableColor.CYAN, 120, 60);
			le.setAcceleration(0.0f, -0.003125f);
			le.setVelocity(0.0f, 0.5f);
			le.setLocation(buildEntity.getMapX() + buildEntity.getCollisionX(),  buildEntity.getMapY() + buildEntity.getFeature().getBounds().getHeight());
			le.spawn(GameScreen.getInstance());
			setBuilding(null);
			waitForMouse = true;
			GameScreen.getInstance().enableBuildings();
			return;
		}

		int cost = building.getShopValue();
		if (getMoney() < cost) {
			SFX.insufficientFunds();
			waitForMouse = true;
			LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getTinyFont(), Game.getMessage("ultraworm.wormgamestate.insufficient_funds")+" $"+(cost - getMoney()), ReadableColor.WHITE, ReadableColor.CYAN, 30, 10);
			le.setAcceleration(0.0f, -0.025f);
			le.setVelocity(0.0f, 1.0f);
			le.setLocation(buildEntity.getMapX() + buildEntity.getCollisionX(),  buildEntity.getMapY() + buildEntity.getFeature().getBounds().getHeight());
			le.spawn(GameScreen.getInstance());
			waitForMouse = true;
			return;
		}

		// Demolish any entities we're building on top of
		int damage = 0;
		ArrayList<Entity> ents = new ArrayList<Entity>(entities);
		for (Iterator<Entity> i = ents.iterator(); i.hasNext(); ) {
			Entity entity = i.next();
			if (entity != buildEntity && entity.canBeCrushed() && entity.isTouching(buildEntity) && buildEntity.canBuildOnTopOf(entity)) {
				damage += entity.crush();
			}
		}

		// Create the building
		Building b = building.build((int) buildEntity.getMapX(), (int) buildEntity.getMapY());
		if (damage > 0) {
			b.damage(damage);
		}
		// Record the cost
		b.setCost(cost);

		// Pay the price
		addMoney(-cost);
		if (!(b.isBarricade() || b.isMineField())) {
			valueOfBuiltBuildings += cost;
			numberOfBuildingsMade ++;
			addStat(Stats.BUILDINGS_BUILT, 1);
			addStat(Stats.VALUE_OF_BUILDINGS_BUILT, cost);
		}
		LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getTinyFont(), "$"+cost, ReadableColor.WHITE, ReadableColor.CYAN, 30, 10);
		le.setAcceleration(0.0f, -0.025f);
		le.setVelocity(0.0f, 1.0f);
		le.setLocation(buildEntity.getMapX() + buildEntity.getCollisionX(),  buildEntity.getMapY() + buildEntity.getFeature().getBounds().getHeight());
		le.spawn(GameScreen.getInstance());

		Worm.getGameState().flagHint(Hints.DRAGBUILD);
		SFX.build();

		if (building.getNumAvailable() != 0) {
			GameScreen.getInstance().enableBuildings();
		}

	}

	/**
	 * Puts us in Build Mode and sets the building that we wish to build, or cancels build mode if building is null
	 * @param building
	 */
	public void setBuilding(BuildingFeature building) {
		if (buildEntity != null) {
			buildEntity.remove();
			buildEntity = null;
		}
		if (building == null) {
			// Leave "build mode"
			mode = Mode.MODE_NORMAL;
			this.building = null;
		} else {
			if (mode == Mode.MODE_SMARTBOMB) {
				// Return the active smartbomb to the stash
				addPowerup(SmartbombPowerupFeature.getInstance(), false);
			}
			// Go into "build mode"
			mode = Mode.MODE_BUILD;
			this.building = building;

			// Create the build entity
			float x = quantize(GameScreen.getInstance().getMouseX() - GameScreen.getSpriteOffset().getX()), y = quantize(GameScreen.getInstance().getMouseY() - GameScreen.getSpriteOffset().getY());
			buildEntity = building.ghost(quantize(x - building.getBounds().getX() - building.getBounds().getWidth() / 2 + MAP_RESOLUTION / 2), quantize(y - building.getBounds().getY() - building.getBounds().getHeight() / 2 + MAP_RESOLUTION / 2));
		}
		waitForMouse = true;
	}

	public void setSellMode(boolean sellMode) {
		if (sellMode) {
			if (mode == Mode.MODE_SMARTBOMB) {
				// Cancel the smartbomb and return it to the stores
				addPowerup(SmartbombPowerupFeature.getInstance(), true);
			} else if (mode == Mode.MODE_BUILD) {
				setBuilding(null);
			}
			mode = Mode.MODE_SELL;
		} else {
			mode = building == null ? Mode.MODE_NORMAL : Mode.MODE_BUILD;
		}
	}

	public boolean isSelling() {
		return mode == Mode.MODE_SELL;
	}

	private void tickEndOfGame() {
		if (tick == END_OF_GAME_DURATION) {
			// Game over! This puts up teh Game Over dialog. Or goes to the title screen if we've expired the demo.
			if (demoExpired) {
				MiniGame.endGame();
			} else {
				MiniGame.gameOver();
			}
		}
	}

	/**
	 * Expire the demo
	 */
	private void expireDemo() {
		if (demoExpired) {
			return;
		}
		String msg =
			getGameMode() == GAME_MODE_CAMPAIGN
					? Game.getMessage("ultraworm.wormgamestate.completed_campaign_demo")
					: Game.getMessage("ultraworm.wormgamestate.completed_endless_demo");
		Res.getResearchNagDialog().doModal("): DEMO EXPIRED :(", msg, new Runnable() {
			@Override
			public void run() {
				if (Res.getResearchNagDialog().getOption() == DialogScreen.OK_OPTION) {
					MiniGame.buy(true);
				} else {
					LabelEffect nagEffect = new LabelEffect(net.puppygames.applet.Res.getBigFont(), "): ): "+Game.getMessage("ultraworm.wormgamestate.demo_expired")+" :( :(", ReadableColor.YELLOW, ReadableColor.RED, 160, 240);
					nagEffect.setLocation(Game.getWidth() / 2, Game.getHeight() / 2 + 32);
					nagEffect.setVisible(true);
					nagEffect.spawn(GameScreen.getInstance());
					nagEffect.setOffset(null);

					Game.getLocalPreferences().putBoolean("showregister", true);
					phase = PHASE_END_OF_GAME;
					tick = 0;
					demoExpired = true;
				}
			}
		});

	}

	/**
	 * @return true if player is using bezerk weapon
	 */
	public boolean isBezerk() {
		return bezerkTick > 0;
	}

	/**
	 * @return true if we've got a smartbomb loaded
	 */
	public boolean isSmartbombMode() {
		return mode == Mode.MODE_SMARTBOMB;
	}

	public void handleESC() {
		if (isBuilding()) {
			setBuilding(null);
		} else if (isSelling()) {
			setSellMode(false);
		} else if (!saving && !GameScreen.getInstance().isBlocked()) {
			// Open ingame menu
			switch (getGameMode()) {
				case GAME_MODE_SURVIVAL:
					SurvivalMenuScreen.show(SurvivalMenuScreen.MENU_GAME_MODE);
					break;
				case GAME_MODE_XMAS:
					XmasMenuScreen.show(SurvivalMenuScreen.MENU_GAME_MODE);
					break;
				default:
					MenuScreen.show(MenuScreen.MENU_GAME_MODE);
					break;
			}
		}
	}

	/**
	 * Tick, called every frame that the game is playing
	 */
	public void tick() {
//		// Check for escape to save the game and quit, or cancel building
//		if (Game.wasKeyPressed(Keyboard.KEY_ESCAPE)) {
//		}

		if (saving && isAlive()) {
			saveTick ++;
			if (saveTick >= SAVE_DURATION) {
				MiniGame.saveGame();
				return;
			}
		}

		// Do colours & animation syncs
		LevelFeature lf = getLevelFeature();
		if (lf == null) {
			return;
		}
		getLevelFeature().getColors().tick();

		// If the shops open do nothing else
		if (GameScreen.getInstance().isBlocked() || GameScreen.getInstance().isShowingPausedHint()) {
			return;
		}

		tick ++;
		totalTicks ++;

		calcCurrentDifficulty();

		// Tick all entities, cull dead ones
		tickEntities();

		if (freezeTick > 0) {
			freezeTick --;
		}

		if (bezerkTick > 0) {
			bezerkTick --;
		}

		if (shieldTick > 0) {
			shieldTick --;
		}

		if (isAlive()) {
			checkMouse();
		}

		if (Game.DEBUG) {
			if (Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
				try {
					Thread.sleep(20);
				} catch (Exception e) {
				}
			}
			if (Game.wasKeyPressed(Keyboard.KEY_M)) {
				metaState.money = Util.random(1, 100) * 100;
//				addMoney(10000);
			}
			if (Game.wasKeyPressed(Keyboard.KEY_R)) {
				Map<String, ResearchFeature> research = ResearchFeature.getResearch();
				for (String id : research.keySet()) {
					setResearched(id);
				}
				GameScreen.getInstance().enableBuildings();

				for (PowerupFeature pf : PowerupFeature.getPowerups()) {
					addPowerup(pf, false);
				}

				for (Iterator<BuildingFeature> i = BuildingFeature.getBuildings().iterator(); i.hasNext(); ) {
					BuildingFeature bf = i.next();
					if (bf.isAvailable()) {
						int num = bf.getNumAvailable();
						if (num > 0) {
							addAvailableStock(bf, num);
						}
					}
				}

				// chaz hack! powerup topup for testing
				//for (int i = 0; i < 5; i ++) {
				//	addPowerup(PowerupFeature.getPowerup());
				//}
			}
			if (Game.wasKeyPressed(Keyboard.KEY_PERIOD)) {
				Saucer saucer = new Saucer();
				saucer.spawn(GameScreen.getInstance());

			}
//			if (Worm.wasKeyPressed(Keyboard.KEY_S)) {
//				gameStateInterface.invulnerable(200);
//			}
			if (Game.wasKeyPressed(Keyboard.KEY_L)) {
				endLevel();
				return;
			}
			if (Game.wasKeyPressed(Keyboard.KEY_C)) {
				CompleteGameScreen.show();
				return;
			}
			if (Game.wasKeyPressed(Keyboard.KEY_X)) {
				CompleteXmasScreen.show();
				return;
			}
		}

		switch (phase) {
			case PHASE_NORMAL:
				tickNormal();
				break;
			case PHASE_WAIT_FOR_GIDRAHS:
				tickWaitForGidrahs();
				break;
			case PHASE_END_OF_GAME:
				tickEndOfGame();
				break;
			case PHASE_WAIT_A_FEW_SECONDS:
				tickWaitAFewSeconds();
				break;
			default:
				assert false;
		}

		Entity.checkCollisions();

		// Now cull inactive entities
		for (int i = 0; i < entities.size(); ) {
			Entity src = entities.get(i);
			if (!src.isActive()) {
				entities.remove(i);
			} else {
				i ++;
			}
		}

		updateEntities();
	}

	/**
	 * Clears the collision quadtree and then ticks each entity in turn, placing it back in the quadtree once it has
	 * been ticked.
	 */
	private void tickEntities() {
		Unit.resetTotalThinkTime();
		Gidrah.resetTotalThinkTime();

		for (int i = 0; i < entities.size(); ) {
			Entity e = entities.get(i);
			if (e.isActive()) {
				e.tick();
			}
			if (e.isActive()) {
				i ++;
			} else {
				entities.remove(i);
			}
		}
	}

	/**
	 * Update all the entities
	 */
	private void updateEntities() {
		for (int i = 0; i < entities.size(); i ++ ) {
			Entity entity = entities.get(i);
			try {
				entity.update();
			} catch (Exception ex) {
				System.err.println("Error updating entity "+entity);
				ex.printStackTrace(System.err);
				entity.remove();
			}
		}
	}

	/**
	 * Normal gameplay: make the gidrahs spawn, and the occasional saucer.
	 */
	private void tickNormal() {
		// Dead? Game over!
		if (!isAlive()) {
			endGame();
			return;
		}

		if (Game.DEBUG) {
			if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD1)) {
				forceDifficulty = true;
				currentDifficulty = 0.0f;
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD2)) {
				forceDifficulty = true;
				currentDifficulty = 0.5f;
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD3)) {
				forceDifficulty = true;
				currentDifficulty = 1.0f;
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD0)) {
				forceDifficulty = false;
				calcCurrentDifficulty();
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD4)) {
				createSurvivalBosses();
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD5)) {
				crystalTick = 1;
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD6)) {
				setBuilding((BuildingFeature) Resources.get("building.generic.tangleweb"));
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD7)) {
				CompleteGameScreen.show();
				return;
			} else if (Game.wasKeyPressed(Keyboard.KEY_NUMPAD8)) {
				levelTick += 3600;
			}
		}

		boolean xmas = getGameMode() == GAME_MODE_XMAS;
		// Has the level ended?
		if (getGameMode() == GAME_MODE_SURVIVAL) {
			// No, it's survival mode, and goes on forever and starts right away
			levelTick ++;
			tickSurvivalBosses();
			tickSpawnPoints();
			doCrystals();
			doSaucers();

		} else if (levelTick < getLevelDuration()) {
			if (beginLevel) {
				levelTick ++;
				if (xmas) {
					doCrystals();
				}
				checkUnminedCrystals();
				tickSpawnPoints();
				doSaucers();

				if (levelTick == getLevelDuration()) {
					aliensSpawnedAtLevelEnd = aliensSpawnedValue;
					rush = true;
					if (!xmas) {
						removeSpawnPoints();
					}
				}

				checkInterestingThingsHappening();

				// Maybe do hints
				HintFeature[] events = getLevelFeature().getEvents();
				if (events != null) {
					if (events.length > eventHintSeq) {
						if (events[eventHintSeq].getSeconds() * 60 <= levelTick) {
							flagHint(events[eventHintSeq ++]);
						}
					}
				}

			} else if (getMoney() < 250) {
				beginLevel = true;
			}
		} else {
			waitForGidrahs();
		}
		if (Game.DEBUG && tick % 240 == 0) {
			System.out.println(gidrahs.size()+" gidrahs, difficulty "+getDifficulty()+", valueSpawned="+aliensSpawnedValue+" vanquished="+aliensVanquishedValue);
		}
	}

	/**
	 * @return true if the gidrahs have started their advance
	 */
	public boolean isLevelStarted() {
		return beginLevel;
	}

	private void checkUnminedCrystals() {
		if (unminedCrystalList.size() == 0) {
			unminedTick = 0;
		} else {
			unminedTick ++;
			if (unminedTick == UNMINED_WARNING_TIME) {
				Worm.getGameState().flagHint(Hints.UNMINED);
				unminedTick = 0;
			}
		}
	}

	/**
	 * @return the number of unmined crystals
	 */
	public int getNumUnminedCrystals() {
		return unminedCrystalList.size();
	}

	private void checkInterestingThingsHappening() {
		// Interesting stuff happened?
		somethingInterestingHappenedTick ++;
		if (somethingInterestingHappenedTick > INTERESTING_INTERVAL && unminedCrystalList.size() == 0 ) {
			flagHint(Hints.FASTFORWARD);
			somethingInterestingHappenedTick = 0;
		}

	}

	private void removeSpawnPoints() {
		for (Iterator<SpawnPoint> i = spawnPoints.iterator(); i.hasNext(); ) {
			SpawnPoint s = i.next();
			s.remove();
		}
		spawnPoints.clear();
	}

	/**
	 * Wait for all the gidrahs to die at the end of a level.
	 */
	private void tickWaitForGidrahs() {
		// No bases left? Game over!
		if (!isAlive()) {
			endGame();
			return;
		}

		if (gidrahs.size() == 0) {
			if (getGameMode() == GAME_MODE_XMAS) {
				// Wait until spawnpoint is gone
				if (spawnPoints.size() > 0) {
					tickSpawnPoints();
					return;
				}
			}
			// Wait until (all crystals are mined / factory not available) and gidrahs and bosses are dead
			int n = bosses.size();
			for (int i = 0; i < n; i ++) {
				Gidrah boss = bosses.get(i);
				if (boss.isActive()) {
					return;
				}
			}
			bosses.clear();
			// Yay! level complete. Wait a few seconds.
			waitAFewSeconds();
			return;
		} else {
			if (Game.DEBUG && tick % 240 == 0) {
				System.out.println(gidrahs.size()+" gidrahs, difficulty "+getDifficulty());
			}
			tickSpawnPoints();
			checkInterestingThingsHappening();
		}
	}

	private void waitAFewSeconds() {
		tick = 0;
		rush = false;
		phase = PHASE_WAIT_A_FEW_SECONDS;

		// Collect any powerups
		for (Saucer saucer : new ArrayList<Saucer>(saucers)) {
			saucer.onClicked(Mode.MODE_NORMAL);
		}
	}

	private void tickWaitAFewSeconds() {
		if (tick == WAIT_A_FEW_SECONDS_DELAY) {
			endLevel();
		}
	}

	/**
	 * Spawns the bosses
	 */
	private void spawnBosses() {
		// Spawn the boss at some of the spawnpoints.
		int n = 0;
		ArrayList<SpawnPoint> sp = new ArrayList<SpawnPoint>(spawnPoints);
		Collections.shuffle(sp);
		ResourceArray bosses = LevelFeature.getLevel(metaState.level).getBosses();
		for (int i = 0; i < bosses.getNumResources() && n < sp.size(); ) {
			SpawnPoint bossSpawnPoint = sp.get(n ++);
			if (bossSpawnPoint.edge) {
				bossSpawnPoint.spawnBoss((GidrahFeature) bosses.getResource(i));
				i ++;
			}
		}

		doBossWarning(bosses.getNumResources());
	}

	private void doBossWarning(int numBosses) {
		// TODO: sfx
		LabelEffect challengeEffect = new LabelEffect(net.puppygames.applet.Res.getBigFont(), Game.getMessage("ultraworm.wormgamestate.extreme_danger"), ReadableColor.YELLOW, ReadableColor.RED, 60, 240);
		challengeEffect.setLocation(Game.getWidth() / 2, Game.getHeight() / 2 + 40);
		challengeEffect.setVisible(true);
		challengeEffect.spawn(GameScreen.getInstance());
		challengeEffect.setOffset(null);
		LabelEffect challengeEffect2 = new LabelEffect(net.puppygames.applet.Res.getSmallFont(), (numBosses > 1 ? Game.getMessage("ultraworm.wormgamestate.large_enemies_approaching") : Game.getMessage("ultraworm.wormgamestate.large_enemy_approaching")), ReadableColor.YELLOW, ReadableColor.RED,
				60, 240);
		challengeEffect2.setLocation(Game.getWidth() / 2, Game.getHeight() / 2);
		challengeEffect2.setVisible(true);
		challengeEffect2.spawn(GameScreen.getInstance());
		challengeEffect2.setDelay(120);
		challengeEffect2.setOffset(null);
	}

	private void shutdownFactories() {
		boolean doSound = false;
		for (int i = 0; i < buildings.size(); i ++) {
			Building b = buildings.get(i);
			if (b.isActive()) {
				if (b instanceof Factory) {
					doSound |= ((Factory) b).isMining();
				}
				b.onEndLevel();
			}
		}
		if (doSound) {
			SFX.factoryShutdown();
		}
	}

	private void waitForGidrahs() {
		phase = PHASE_WAIT_FOR_GIDRAHS;
		tick = 0;
	}

	/**
	 * Activate a powerup
	 * @param powerup
	 */
	public void activatePowerup(PowerupFeature powerup) {
		powerup.activate(gameStateInterface);
	}

	/**
	 * Every now and again, launch a saucer.
	 */
	private void doSaucers() {
		saucerTick ++;
		if (saucerTick > nextSaucer) {
			saucerTick = 0;
			if (getGameMode() != GAME_MODE_SURVIVAL && getGameMode() != GAME_MODE_XMAS) {
				nextSaucer += config.getNextSaucerInterval();
			}
			Saucer saucer = new Saucer();
			saucer.spawn(GameScreen.getInstance());
		}
	}

	/**
	 * Every now and again, spawn a crystal.
	 */
	private void doCrystals() {
		crystalTick --;
		if (crystalTick <= 0) {
			if (survivalCrystals.size() == 0) {
				// Refill the shuffle bag
				for (int i = 0; i < 3; i ++) {
					survivalCrystals.add(new Integer(1));
				}
				for (int i = 0; i < 2; i ++) {
					survivalCrystals.add(new Integer(2));
				}
				for (int i = 0; i < 1; i ++) {
					survivalCrystals.add(new Integer(3));
				}
				Collections.shuffle(survivalCrystals);
			}
			Integer size = survivalCrystals.remove(survivalCrystals.size() - 1);
			Crystal crystalDefinition = getLevelFeature().getScenery().getCrystal(size.intValue());
			int width = crystalDefinition.getWidth();
			int height = crystalDefinition.getHeight();
			int tileX, tileY;

			// Choose a spot some way from the base depending on difficulty
			float minDistance = 10.0f;
			float maxDistance = getMap().getWidth() * 0.45f;
			float ratio = (float) Math.random();
			float distance = LinearInterpolator.instance.interpolate
				(
					CosineInterpolator.instance.interpolate(minDistance, maxDistance, ratio),
					SineInterpolator.instance.interpolate(minDistance, maxDistance, ratio),
					getDifficulty()
				);
			double angle = Math.random() * Math.PI * 2.0;
			tileX = getBase().getTileX() + (int) (Math.cos(angle) * distance);
			tileY = getBase().getTileY() + (int) (Math.sin(angle) * distance);
			for (int x = 0; x < width; x ++) {
				for (int y = 0; y < height; y ++) {
					if (getMap().isOccupied(x + tileX, y + tileY)) {
						return;
					}
					if (getMap().isAttacking(x + tileX, y + tileY)) {
						return;
					}
					for (int z = 0; z < GameMap.LAYERS; z ++) {
						Tile t = getMap().getTile(x + tileX, y + tileY, z);
						if (t == null || t.isImpassable() || t.isSolid()) {
							return;
						}
					}
				}
			}

			ReadableRectangle bounds = new Rectangle(tileX * MapRenderer.TILE_SIZE, tileY * MapRenderer.TILE_SIZE, width * MapRenderer.TILE_SIZE, height * MapRenderer.TILE_SIZE);
			for (Iterator<Entity> i = entities.iterator(); i.hasNext(); ) {
				Entity entity = i.next();
				if (entity.isActive() && entity.isSolid() && entity.canCollide() && entity.isTouching(bounds)) {
					return;
				}
			}

			// Write excludes
			for (int x = 0; x < width; x ++) {
				for (int y = 0; y < height; y ++) {
					Exclude.getInstance().toMap(getMap(), x + tileX, y + tileY, false);
				}
			}

			EntitySpawningFeature esf = crystalDefinition.getCrystalFeature();
			esf.spawn(tileX, tileY);
			crystalTick = survivalNextCrystalTick * size.intValue();
			survivalNextCrystalTick += getGameMode() == GAME_MODE_SURVIVAL ? config.getSurvivalCrystalIntervalAdjust() : config.getXmasCrystalIntervalAdjust();
		}
	}

	/**
	 * @return the game mode
	 */
	public int getGameMode() {
		return metaState.gameMode;
	}

	/**
	 * Init for a new game
	 */
	@Override
	public void init() {
		switch (metaState.gameMode) {
			case GAME_MODE_CAMPAIGN:
				if (Worm.getMaxWorld() == 0) {
					SelectLevelScreen sls = (SelectLevelScreen) Resources.get("select.level.screen.0");
					sls.open();
				} else {
					SelectWorldScreen.show();
				}
				break;

			case GAME_MODE_ENDLESS:
				SelectEndlessLevelScreen.show();
				break;

			case GAME_MODE_SURVIVAL:
				SelectSurvivalLevelScreen.show();
				break;

			case GAME_MODE_SANDBOX:
				SelectSandboxLevelScreen.show();
				break;

			case GAME_MODE_XMAS:
				initXmas(true);
				break;

			default:
				assert false : "Unknown game mode "+mode;
		}
	}

	/**
	 * Initialise a Christmas mode game (instead of calling {@link #doInit(int)})
	 */
	private void initXmas(boolean reset) {
		System.out.println("Initialising Xmas Mode");
		waitForMouse = true;
		xmasReset = reset;
		if (reset) {
			metaState = null;
			metaState = new MetaState(GAME_MODE_XMAS);
		} else {
			metaState.reset();
		}
		GameScreen.beginGame(this);
		beginLevel();
		initXmasResearch();
		doInitMessages(Game.getMessage("ultraworm.wormgamestate.init_xmas1"), Game.getMessage("ultraworm.wormgamestate.init_xmas2"));
	}


	/**
	 * Initialise a survival mode game (instead of calling {@link #doInit(int)})
	 */
	private void initSurvival(boolean reset) {
		System.out.println("Initialising Survival Mode");
		waitForMouse = true;
		if (reset) {
			metaState = null;
			metaState = new MetaState(GAME_MODE_SURVIVAL);
		} else {
			metaState.reset();
		}
		GameScreen.beginGame(this);
		beginLevel();
		initSurvivalResearch();
		doInitMessages(Game.getMessage("ultraworm.wormgamestate.init_survival1"), Game.getMessage("ultraworm.wormgamestate.init_survival2"));
	}

	/**
	 * Initialise a sandbox mode game (instead of calling {@link #doInit(int)})
	 */
	private void initSandbox() {
		System.out.println("Initialising Sandbox Mode");
		waitForMouse = true;
		metaState = null;
		metaState = new MetaState(GAME_MODE_SANDBOX);
		GameScreen.beginGame(this);
		beginLevel();
		//initSurvivalResearch();
		//doInitMessages(Game.getMessage("ultraworm.wormgamestate.init_sandbox1"), Game.getMessage("ultraworm.wormgamestate.init_sandbox2"));
		doInitMessages("SANDBOX TEST", "No really, just a test!");
	}

	/**
	 * Begin a new game at the specified level
	 * @param level
	 */
	public void doInit(int level) {
		System.out.println("DoInit: "+level);
		waitForMouse = true;
		if (level == 0) {
			// Research the default researchy things
			metaState = new MetaState(metaState.gameMode);
			initDefaultResearch();
		} else {
			// Load research and powerups etc we had last time
			try {
				metaState = MetaState.load(level, metaState.gameMode);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				metaState = new MetaState(metaState.gameMode);
				initDefaultResearch();
			}
		}

		GameScreen.beginGame(this);

		if (Game.isRegistered()) {
			awardMedal(Medals.REGISTERED);
		}
		beginLevel();

		doInitMessages(Game.getMessage("ultraworm.wormgamestate.init1"), Game.getMessage("ultraworm.wormgamestate.init2"));
	}

	/**
	 * Begin a Survival game with the specified characteristics
	 */
	public void doInit(SurvivalParams survivalParams) {
		this.survivalParams = survivalParams;
		System.out.println("DoInit: "+survivalParams);
		initSurvival(survivalParams.getGenerateNew());
		if (Game.isRegistered()) {
			awardMedal(Medals.REGISTERED);
		}
	}

	/**
	 * @return the survival parameters, if we're in survival mode
	 */
	public SurvivalParams getSurvivalParams() {
		return survivalParams;
	}

	private void initDefaultResearch() {
		for (ResearchFeature rf : ResearchFeature.getResearch().values()) {
			if (rf.isDefaultAvailable()) {
				setResearched(rf.getID());
			}
		}
	}

	private void initSurvivalResearch() {
		MetaState tempMetaState;
        try {
	        tempMetaState = MetaState.load(survivalParams.getWorld().getIndex() * LEVELS_IN_WORLD + LEVELS_IN_WORLD, GAME_MODE_CAMPAIGN);
			for (String s : tempMetaState.researched) {
				setResearched(s);
			}
        } catch (Exception e) {
	        e.printStackTrace(System.err);
	        initDefaultResearch();
	        return;
        }
	}

	private void initXmasResearch() {
		for (String s : Xmas.RESEARCH) {
			setResearched(s);
		}
	}

	/**
	 * Begin a Sandbox game with the specified characteristics
	 */
	public void doInit(SandboxParams sandboxParams) {
		this.sandboxParams = sandboxParams;
		System.out.println("DoInit: "+sandboxParams);
		initSandbox();
	}


	/**
	 * We've completed the game!
	 */
	private void completeGame() {
		if (getGameMode() == GAME_MODE_XMAS) {
			CompleteXmasScreen.show();
		} else {
			CompleteGameScreen.show();
		}
	}

	/**
	 * Go to the next level. If we run out of levels, we complete the game.
	 */
	public void nextLevel() {
		metaState.level ++;
		if (getGameMode() == GAME_MODE_XMAS) {
			completeGame();
			return;
		}
		if (getGameMode() == GAME_MODE_ENDLESS || getLevel() < MAX_LEVELS) {
			beginLevel();
		} else {
			completeGame();
		}

	}

	/**
	 * Initialise a level. We need to load the game map, remove any entities, spawn the buildings, make a note of the spawn points on
	 * the map, etc.
	 */
	public void beginLevel() {
		System.out.println("Beginning level "+metaState.gameMode+"/"+metaState.level);

		WorldFeature newWorld;
		StoryFeature[] sf;

		reset();

		switch (metaState.gameMode) {
			case GAME_MODE_CAMPAIGN:
				// Set the level
				metaState.levelFeature = LevelFeature.getLevel(metaState.level);

				// We might have changed worlds
				newWorld = getLevelFeature().getWorld();
				if (newWorld != getWorld()) {
					setWorld(newWorld);
					return;
				}

				currentStoryIndex = 0;
				sf = getLevelFeature().getStories();
				currentStories = new ArrayList<StoryFeature>(sf.length);
				for (StoryFeature element : sf) {
					if (element.qualifies()) {
						currentStories.add(element);
					}
				}

				suppressMedals = false;
				showStoryScreen();
				break;

			case GAME_MODE_ENDLESS:
				// Generate a random level
				metaState.levelFeature = LevelFeature.generateEndless(metaState.level);

				// We might have changed worlds
				newWorld = getLevelFeature().getWorld();
				if (newWorld != getWorld()) {
					setWorld(newWorld);
				}

				currentStoryIndex = 0;
				sf = getLevelFeature().getStories();
				currentStories = new ArrayList<StoryFeature>(sf.length);
				for (StoryFeature element : sf) {
					if (element.qualifies()) {
						currentStories.add(element);
					}
				}

				suppressMedals = false;
				showStoryScreen();
				break;

			case GAME_MODE_SURVIVAL:
				if (survivalParams.getGenerateNew()) {
					// Generate a random level
					metaState.levelFeature = LevelFeature.generateSurvival(survivalParams);
				} else {
					// Use last level
				}
				metaState.difficulty = survivalParams.getDifficulty();
				metaState.survivalParams = survivalParams;
				metaState.reset();

				// We might have changed worlds
				newWorld = getLevelFeature().getWorld();
				if (newWorld != getWorld()) {
					setWorld(newWorld);
				}

				currentStoryIndex = 0;
				sf = getLevelFeature().getStories();
				currentStories = new ArrayList<StoryFeature>(sf.length);
				for (StoryFeature element : sf) {
					if (element.qualifies()) {
						currentStories.add(element);
					}
				}

				suppressMedals = false;
				showStoryScreen();
				break;

			case GAME_MODE_SANDBOX:
				// Generate a random level
				metaState.levelFeature = LevelFeature.generateEndless(metaState.level);

				// We might have changed worlds
				newWorld = getLevelFeature().getWorld();
				if (newWorld != getWorld()) {
					setWorld(newWorld);
				}

				suppressMedals = true;
				beginLevel2();
				break;

			case GAME_MODE_XMAS:
				// Generate a random level
				metaState.levelFeature = LevelFeature.generateXmas();
				metaState.difficulty = 0.0f;
				metaState.reset();

				// We might have changed worlds
				newWorld = getLevelFeature().getWorld();
				if (newWorld != getWorld()) {
					setWorld(newWorld);
				}

				currentStoryIndex = 0;
				sf = getLevelFeature().getStories();
				currentStories = new ArrayList<StoryFeature>(sf.length);
				for (StoryFeature element : sf) {
					if (element.qualifies()) {
						currentStories.add(element);
					}
				}

				suppressMedals = false;
				showStoryScreen();
				break;

			default:
				assert false : "Unknown game mode "+metaState.gameMode;
		}

	}

	private void nextStoryScreen() {
		if (++currentStoryIndex >= currentStories.size()) {
			StoryScreen.tidyUp("story.screen."+getWorld().getUntranslated());
			// Open research screen, unless this is level 0 or survival mode or sandbox mode, in which case jump straight into the action
			if (getLevel() == 0 || getGameMode() == GAME_MODE_SURVIVAL || getGameMode() == GAME_MODE_SANDBOX) {
				beginLevel2();
			} else {
				showResearchScreen();
			}
		} else {
			// Open next story screen
			showStoryScreen();
		}
	}

	private void previousStoryScreen() {
		if (--currentStoryIndex == -1) {
			switch (metaState.gameMode) {
				case GAME_MODE_CAMPAIGN:
					// Go to level select screen of current world
					StoryScreen.tidyUp("story.screen."+getWorld().getUntranslated());
					showLevelSelectScreen();
					break;

				case GAME_MODE_ENDLESS:
				case GAME_MODE_SURVIVAL:
					showLevelSelectScreen();
					break;

				case GAME_MODE_SANDBOX:
					showLevelSelectScreen();
					break;

				case GAME_MODE_XMAS:
					net.puppygames.applet.screens.TitleScreen.show();
					break;

				default:
					assert false : "Shouldn't be here: "+metaState.gameMode;
			}
		} else {
			// Open previous story
			showStoryScreen();
		}
	}

	public void showLevelSelectScreen() {
		switch (metaState.gameMode) {
			case GAME_MODE_CAMPAIGN:
				SelectLevelScreen sls = (SelectLevelScreen) Resources.get("select.level.screen."+metaState.level / LEVELS_IN_WORLD);
				sls.open();
				break;
			case GAME_MODE_ENDLESS:
				SelectEndlessLevelScreen.show();
				break;
			case GAME_MODE_SURVIVAL:
				SelectSurvivalLevelScreen.show();
				break;
			case GAME_MODE_SANDBOX:
				SelectSandboxLevelScreen.show();
				break;
			case GAME_MODE_XMAS:
				net.puppygames.applet.screens.TitleScreen.show();
				break;
			default:
				assert false : "Shouldn't be here: "+metaState.gameMode;
		}
	}

	public void showResearchScreen() {
		ResearchScreen.show
			(
				new Runnable() {
					@Override
					public void run() {
						previousStoryScreen();
					}
				},
				new Runnable() {
					@Override
					public void run() {
						Worm.getGameState().beginLevel2();
					}
				}
			);
	}

	private void showStoryScreen() {
		StoryScreen.show
			(
				"story.screen."+getWorld().getUntranslated(),
				currentStoryIndex == 0,
				currentStories.get(currentStoryIndex),
				new Runnable() {
					@Override
					public void run() {
						previousStoryScreen();
					}
				},
				new Runnable() {
					@Override
					public void run() {
						nextStoryScreen();
					}
				}
			);
	}

	public void setMap(GameMap newMap) {
		this.map = newMap;
	}

	/**
	 * Reinforcements!
	 */
	private void reinforcements() {
		for (Iterator<BuildingFeature> i = BuildingFeature.getBuildings().iterator(); i.hasNext(); ) {
			BuildingFeature bf = i.next();
			if (bf.isAvailable()) {
				int num = bf.getNumAvailable();
				if (num > 0) {
					addAvailableStock(bf, num);
				}
			}
		}
	}

	/**
	 * Begin the level with the specified map.
	 */
	public void beginLevel2() {
		// Increase available stock of everything (barricades and mines)
		reinforcements();

		// Ensure all entities are removed first
		for (Iterator<Entity> i = entities.iterator(); i.hasNext(); ) {
			Entity entity = i.next();
			entity.remove();
		}
		entities.clear();

		// Init gids stuff at start of level
		Gidrah.init();

		phase = PHASE_NORMAL;
		rush = false;
		levelTick = 0;
		tick = 0;
		beginLevel = !isResearched(ResearchFeature.FACTORY) || getMoney() < 250 || metaState.gameMode == GAME_MODE_SURVIVAL || metaState.gameMode == GAME_MODE_XMAS;
		nextSaucer = config.getSaucerInterval();

		System.out.println("MODE: "+metaState.gameMode+"\nPHASE: "+phase);

		// Reset buffs
		scannerBoost = 0;
		batteryBoost = 0;
		reactorBoost = 0;
		coolingBoost = 0;
		shieldBoost = isResearched(ResearchFeature.NANOHARDENING) ? 1 : 0;
		capacitorBoost = isResearched(ResearchFeature.IONISATION) ? 1 : 0;

		// Reset counters
		factories = 0;
		warehouses = 0;
		autoloaders = 0;
		shieldGenerators = 0;
		spawners = 0;
		crystals = 0;
		initialCrystals = 0;

		// Reset powerups
		bezerkTick = 0;
		shieldTick = 0;
		freezeTick = 0;

		// Reset buildings value
		anyDamaged = false;
		valueOfBuiltBuildings = 0;
		valueOfDestroyedBuildings = 0;

		// Reset aliens spawned value
		aliensVanquishedValue = 0;
		aliensSpawnedValue = 0;

		// Clear medals earned this level
		medalsThisLevel.clear();
		awesome = false;

		// Remember starting money
		startingMoney = metaState.money;

		attempts = Worm.getExtraLevelData(Game.getPlayerSlot(), metaState.level, metaState.gameMode, "attempts", 0);
		System.out.println("Number of attempts at this level so far: "+attempts);

		// Adjust difficulty by number of attempts
		calcBasicDifficulty();
		calcPowerupDifficulty();

		System.out.println("Base difficulty "+metaState.difficulty);

		// How long's the level?
		calcLevelDuration(map.getWidth(), map.getHeight());

		// Clear everything out
		unminedCrystalList.clear();
		Entity.reset();
		System.gc();

		// Sort out the map
		initMap();

		// Open the game screen
		GameScreen.onBeginLevel();

		// Maybe spawn bosses
		ResourceArray bosses = metaState.levelFeature.getBosses();
		if (bosses != null && bosses.getNumResources() > 0) {
			spawnBosses();
		}

		// Reset event sequence
		eventHintSeq = 0;

		leftMouseWasDown = true;
		waitForMouse = true;

		// Research medals
		if (metaState.gameMode != GAME_MODE_SURVIVAL && metaState.gameMode != GAME_MODE_XMAS) {
			Map<String, List<String>> medalGroups = ResearchFeature.getMedalGroups();
			for (Entry<String, List<String>> entry : medalGroups.entrySet()) {
				String medal = entry.getKey();
				List<String> researchRequired = entry.getValue();
				synchronized (metaState.researched) {
					if (metaState.researched.containsAll(researchRequired)) {
						awardMedal(medal);
					}
				}
			}
		}
	}

	/**
	 * Calculates base difficulty and research difficulty
	 */
	public void calcBasicDifficulty() {
		if (metaState.gameMode == GAME_MODE_SURVIVAL) {
			metaState.difficulty = survivalParams.getDifficulty();
			return;
		} else if (metaState.gameMode == GAME_MODE_XMAS) {
			metaState.difficulty = 0.0f;
			return;
		}

		int diff = getDifficultyAdjust(getLevel(), getGameMode());
		metaState.difficulty = diff * config.getDifficultyAdjustmentFactor();

		// Central base levels - offset difficulty
		float offset = BaseMapGenerator.isBaseCentralForLevel(metaState.level, metaState.gameMode) ? config.getCentralDifficultyAdjustPerLevel() * metaState.level : 0.0f;

		// Endless mode: gets a bit harder every level after level 50...
		if (metaState.gameMode == GAME_MODE_ENDLESS && metaState.level > MAX_LEVELS) {
			offset -= (metaState.level - MAX_LEVELS) * config.getEndlessDifficultyCreep();
		}
		metaState.difficulty -= offset;

		System.out.println("BASIC DIFFICULTY "+getBasicDifficulty()+" (was offset by "+offset+")");
	}

	/**
	 * @return the basic level difficulty (&ge; 0.0f)
	 */
	public float getBasicDifficulty() {
		return Math.max(0.0f, metaState.difficulty + (config.getBankFactor() * getMoney()) / (config.getDifficultyFactor() + config.getDifficultyFactorPerLevel() * getLevel()));
	}

	/**
	 * Initialise the map generated in the story screen
	 */
	private void initMap() {

		// Clear away the old spawn points
		spawnPoints.clear();

		// Have we been here before?
		MapProcessor processor = new MapProcessor() {
			@Override
			public void addSpawnPoint(int x, int y, int type, boolean edge) {
				SpawnPoint sp = new SpawnPoint(x, y, type, edge);
				assert !spawnPoints.contains(sp);
				spawnPoints.add(sp);
			}

			@Override
			public void createBase(int x, int y) {
				getWorld().getBase().build(x * MapRenderer.TILE_SIZE, y * MapRenderer.TILE_SIZE);
			}

			@Override
			public void spawnEntity(EntitySpawningFeature feature, int tileX, int tileY) {
				// Spawn the entity...

				Entity e = feature.spawn(tileX, tileY);
				e.update();
				// Remove from the map?
				if (feature.removeAfterSpawn()) {
					getMap().clearItem(tileX, tileY);
				}
			}
		};

		// Process the map one tile at a time.
		for (int y = 0; y < map.getHeight(); y ++) {
			for (int x = 0; x < map.getWidth(); x ++) {
				for (int z = 0; z < GameMap.LAYERS; z ++) {
					Tile t = map.getTile(x, y, z);
					t.process(processor, x, y);
				}
			}
		}

		// Survival mode: create spawn point.
		if (getGameMode() == GAME_MODE_SURVIVAL) {
			createSurvivalSpawnPoint();
		}
	}

	private ReadablePoint getEdgePoint() {
		int edge = getGameMode() == GAME_MODE_XMAS ? 0 : Util.random(0, 9); // Bias away from south: only 1/10 edges are picked from south

		boolean blocked;
		int x, y, ox, oy, count = 0;
		do {
			count ++;
			blocked = false;
			switch (edge) {
				case 0: // North
				case 1:
				case 2:
					x = Util.random(1, getMap().getWidth() - 2);
					y = getMap().getHeight() - 1;
					ox = 0;
					oy = 1;
					break;
				case 3: // East
				case 4:
				case 5:
					y = Util.random(1, getMap().getHeight() - 2);
					x = getMap().getWidth() - 1;
					ox = 1;
					oy = 0;
					break;
				case 6: // West
				case 7:
				case 8:
					y = Util.random(1, getMap().getHeight() - 2);
					x = 0;
					ox = -1;
					oy = 0;
					break;
				case 9: // South
					x = Util.random(1, getMap().getWidth() - 2);
					y = 0;
					ox = 0;
					oy = -1;
					break;
				default:
					assert false;
					return new Point();
			}
			for (int z = 0; z < GameMap.LAYERS; z ++) {
				Tile t = getMap().getTile(x, y, z);
				if (t.isImpassable() || t.isSolid()) {
					blocked = true;
					break;
				}
			}
		} while (blocked && count < 50);
		return new Point(x + ox, y + oy);
	}

	/**
	 * Creates a brand new survival mode spawnpoint by finding an empty tile on the edge of the map.
	 */
	private void createSurvivalSpawnPoint() {
		ReadablePoint edgePoint = getEdgePoint();
		if (edgePoint == null) {
			return;
		}
		spawnPoints.add(new SpawnPoint(edgePoint.getX(), edgePoint.getY(), 0, true));
	}

	/**
	 * Hack for Earth-only survival / xmas bosses
	 * @param n
	 * @return
	 */
	private static String survivalBoss(int n) {
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i ++) {
			sb.append('0');
		}
		return sb.toString();
	}

	/**
	 * Spawns survival mode bosses
	 */
	private void createSurvivalBosses() {
		// Get best world player has seen...
		int bestWorldIndex = Math.min(WorldFeature.getNumWorlds() - 1, Worm.getMaxLevel(GAME_MODE_CAMPAIGN) / LEVELS_IN_WORLD);
		int numBossTypes = Math.min(WorldFeature.getWorld(bestWorldIndex).getSurvivalMaxBoss() + 1, Res.getNumSurvivalBosses());
		String s = numBossTypes == 1 ? survivalBoss(survivalBoss) : Integer.toString(survivalBoss, numBossTypes);
		for (int i = 0; i < s.length(); i ++) {
			ReadablePoint edgePoint = getEdgePoint();
			GidrahFeature bossFeature = Res.getSurvivalBoss(s.charAt(i) - '0');
			if (bossFeature != null) {
				bossFeature.spawn(GameScreen.getInstance(), edgePoint.getX(), edgePoint.getY(), 0);
			}
		}
		survivalBoss ++;
		doBossWarning(s.length());
		reinforcements();
	}

	/**
	 * @return the current map
	 */
	public GameMap getMap() {
		return map;
	}

	private void setWorld(WorldFeature newWorld) {
		assert newWorld != null;

		if (metaState.world == newWorld) {
			return;
		}

		metaState.world = newWorld;

		// Put up a New World screen in campaign mode
		if (metaState.gameMode == GAME_MODE_CAMPAIGN) {
			NewWorldScreen nw = (NewWorldScreen) Resources.get("newworld.screen."+getWorld().getUntranslated());
			nw.open();
		}

	}

	private void doInitMessages(String big, String small) {
		LabelEffect newLevelEffect = new LabelEffect(net.puppygames.applet.Res.getBigFont(), big, ReadableColor.WHITE, ReadableColor.RED, 40, 160);
		newLevelEffect.setLocation(Game.getWidth() / 2, Game.getHeight() / 2 + 60);
		newLevelEffect.setVisible(true);
		newLevelEffect.spawn(GameScreen.getInstance());
		newLevelEffect.setOffset(null);

		LabelEffect hintEffect = new LabelEffect(net.puppygames.applet.Res.getSmallFont(), small, ReadableColor.WHITE, ReadableColor.RED, 40, 160);
		hintEffect.setDelay(30);
		hintEffect.setLocation(Game.getWidth() / 2, Game.getHeight() / 2 + 20);
		hintEffect.setVisible(true);
		hintEffect.spawn(GameScreen.getInstance());
		hintEffect.setOffset(null);
	}

	@Override
	public void reinit() {
		GameScreen.beginGame(this);

		doInitMessages(Game.getMessage("ultraworm.wormgamestate.reinit1"), Game.getMessage("ultraworm.wormgamestate.reinit2"));

		waitForMouse = true;

		if (mode == Mode.MODE_BUILD) {
			setBuilding(building);
		}

		// Process entities
		for (Iterator<Entity> i = entities.iterator(); i.hasNext(); ) {
			Entity entity = i.next();
			entity.respawn(GameScreen.getInstance());
		}

		// And spawnpoints
		for (Iterator<SpawnPoint> i = spawnPoints.iterator(); i.hasNext(); ) {
			SpawnPoint sp = i.next();
			sp.reinit();
		}

		GameScreen.onBeginLevel();

		if (!isAlive()) {
			MiniGame.endGame();
		}
	}

	/**
	 * @return Returns the entities.
	 */
	public ArrayList<Entity> getEntities() {
		return entities;
	}

	/**
	 * @return Returns the buildings
	 */
	public ArrayList<Building> getBuildings() {
		return buildings;
	}

	/**
	 * @return Returns the gidrahs
	 */
	public ArrayList<Gidrah> getGidrahs() {
		return gidrahs;
	}

	/**
	 * @return all the saucers
	 */
	public ArrayList<Saucer> getSaucers() {
		return saucers;
	}

	/**
	 * Called when it's game over time
	 */
	public void onGameOver() {
		if (phase == PHASE_END_OF_GAME) {
			return;
		}
		phase = PHASE_END_OF_GAME;
		tick = 0;

		// Stop building
		setBuilding(null);
	}

	/**
	 * Freeze the gidrahs!
	 * @param duration
	 */
	public void freeze(int duration) {
		freezeTick += duration;
		GameScreen.getInstance().onFreezeTimerIncreased(freezeTick);
	}

	/**
	 * Called whenever a gidrah is killed. This removes the gidrah from the gidrahs list,
	 * updates stats, and awards medals appropriately
	 * @param gidrah The gidrah that was killed
	 * @param causeOfDeath For stats and medals purposes
	 */
	public void onGidrahKilled(Gidrah gidrah, int causeOfDeath) {
		gidrahs.remove(gidrah);

		switch (causeOfDeath) {
			case CauseOfDeath.ATTACK:
				return;

			case CauseOfDeath.DISRUPTOR:
			case CauseOfDeath.BULLET:
			case CauseOfDeath.LASER:
				metaState.addStat(Stats.ALIENS_SHOT, 1);
				break;
			case CauseOfDeath.CAPACITOR:
				int fried = metaState.addStat(Stats.ALIENS_FRIED, 1);
				if (fried == 100) {
					awardMedal(Medals.FRIED_100);
				} else if (fried == 250) {
					awardMedal(Medals.FRIED_250);
				} else if (fried == 500) {
					awardMedal(Medals.FRIED_500);
				}
				break;
			case CauseOfDeath.CRUSHED:
				int crushed = metaState.addStat(Stats.ALIENS_CRUSHED, 1);
				if (crushed == 25) {
					awardMedal(Medals.CRUSHED_25);
				} else if (crushed == 50) {
					awardMedal(Medals.CRUSHED_50);
				} else if (crushed == 100) {
					awardMedal(Medals.CRUSHED_100);
				}
				break;
			case CauseOfDeath.EXPLOSION:
				int exploded = metaState.addStat(Stats.ALIENS_BLOWN_UP, 1);
				if (exploded == 100) {
					awardMedal(Medals.EXPLODED_100);
				} else if (exploded == 250) {
					awardMedal(Medals.EXPLODED_250);
				} else if (exploded == 500) {
					awardMedal(Medals.EXPLODED_500);
				}
				break;
			case CauseOfDeath.SMARTBOMB:
				int nuked = metaState.addStat(Stats.ALIENS_SMARTBOMBED, 1);
				if (nuked == 50) {
					awardMedal(Medals.NUKED_50);
				} else if (nuked == 100) {
					awardMedal(Medals.NUKED_100);
				} else if (nuked == 250) {
					awardMedal(Medals.NUKED_250);
				}
				break;
			case CauseOfDeath.GRID_BUG:
				// Bah
				return;
			default:
				assert false : "Unknown cause of death "+causeOfDeath;
				break;
		}

		int vanquished = metaState.addStat(Stats.ALIENS_VANQUISHED, 1);
		if (vanquished == 100) {
			awardMedal(Medals.VANQUISHED_100);
		} else if (vanquished == 250) {
			awardMedal(Medals.VANQUISHED_250);
		} else if (vanquished == 500) {
			awardMedal(Medals.VANQUISHED_500);
		} else if (vanquished == 1000) {
			awardMedal(Medals.VANQUISHED_1000);
		} else if (vanquished == 2500) {
			awardMedal(Medals.VANQUISHED_2500);
		} else if (vanquished == 5000) {
			awardMedal(Medals.VANQUISHED_5000);
		} else if (vanquished == 10000) {
			awardMedal(Medals.VANQUISHED_10000);
		}

		// Survival mode: gidrah deaths unlocks more gidrahs
		if (getGameMode() == GAME_MODE_SURVIVAL) {
			if (gidrah.getFeature().isBoss() || gidrah.getFeature().isGidlet()) {
				// Not bosses or gidlets
			} else {
				int type = gidrah.getType();
				survivalGidrahKills[type] += gidrah.getFeature().isAngry() ? 10 : 1;
				if (survivalGidrahKills[type] >= survivalGidrahUnlockNext[type]) {
					int bestWorldIndex = Math.min(WorldFeature.getNumWorlds() - 1, Worm.getMaxLevel(GAME_MODE_CAMPAIGN) / LEVELS_IN_WORLD);
					if (type < survivalGidrahUnlock.length - 2 && survivalGidrahUnlock[type + 1] == 0) {
						// Unlock next type first. If there is one.
						int max = WorldFeature.getWorld(bestWorldIndex).getSurvivalMaxType(type + 1);
						if (max != -1) {
							survivalGidrahUnlock[type + 1] = 1;
						} else {
							metaState.difficulty += config.getSurvivalDifficultyAdjuster();
						}
					} else {
						int max = WorldFeature.getWorld(bestWorldIndex).getSurvivalMaxType(type);
						if (max > survivalGidrahUnlock[type]) {
							survivalGidrahUnlock[type] ++;
						} else {
							metaState.difficulty += config.getSurvivalDifficultyAdjuster();;
						}
					}
					survivalGidrahUnlockNext[type] += survivalGidrahUnlockNext[type] + config.getSurvivalGidrahUnlockIntervalAdjust()[type];
				}
			}
		}
	}

	/**
	 * Marks a building type as having been researched
	 * @param type
	 */
	public void setResearched(String type) {
		synchronized (metaState.researched) {
			if (metaState.researched.add(type)) {
				(ResearchFeature.getResearch().get(type)).onResearched();
			}
		}
	}

	/**
	 * Marks a building type as having NOT been researched
	 * @param type
	 */
	public void setUnresearched(String type) {
		synchronized (metaState.researched) {
			if (metaState.researched.remove(type)) {
				(ResearchFeature.getResearch().get(type)).onUnresearched();
			}
		}
	}

	/**
	 * Is the specified ResearchItem researched?
	 * @param type
	 * @return
	 */
	public boolean isResearched(String type) {
		return metaState.researched.contains(type);
	}

	/**
	 * @return
	 */
	public boolean isBuilding() {
		return building != null;
	}

	/**
	 * @return the building we're building, or null, if we're not building
	 */
	public BuildingFeature getBuilding() {
		return building;
	}

	/**
	 * @return the cost of the current building
	 */
	public int getBuildingCost() {
		return building.getShopValue();
	}

	/**
	 * @return true if the building we're trying to build is still available to build
	 */
	public boolean isBuildingAvailable() {
		return building.isAvailable() && building.isEnabledInShop();
	}

	/**
	 * @return the name of the current building
	 */
	public String getBuildingName() {
		return building.getTitle();
	}


	/**
	 * @return current level index, 0 based
	 */
	public int getLevel() {
		return metaState.level;
	}

	/**
	 * @return the level in the world (0..LEVELS_IN_WORLD or so)
	 */
	public int getLevelInWorld() {
		return metaState.level % LEVELS_IN_WORLD;
	}

	/**
	 * @return current world feature
	 */
	public WorldFeature getWorld() {
		return metaState.world;
	}

	/**
	 * @return current level feature
	 */
	public LevelFeature getLevelFeature() {
		return metaState.levelFeature;
	}

	/**
	 * Calculates and returns a difficulty value based on level number in the world.
	 * @return a float value between 0.0f (very easy) and 1.0f (very hard)
	 */
	public float getDifficulty() {
		return currentDifficulty;
	}

	private void calcCurrentDifficulty() {
		if (forceDifficulty) {
			System.out.println("Difficulty forced to "+currentDifficulty);
			return;
		}

		float ret = 0.0f;
		float attenuate = 1.0f;
		float irate = 0.0f;
		boolean survival = getGameMode() == GAME_MODE_SURVIVAL;
		boolean xmas = getGameMode() == GAME_MODE_XMAS;

		// And then attenuate this difficulty by how badly the player is being kicked in:
		if (!survival && !xmas && valueOfDestroyedBuildings > 0 && valueOfBuiltBuildings > config.getBuiltBuildingsValueThreshold()) {
			float slaughterAttenuation = 1.0f;
			// Attenuate the attenuation by the aliens losses when level is ended
			if (!isLevelActive() && aliensSpawnedAtLevelEnd > 0 && aliensVanquishedSinceEndOfLevel > 0) {
				slaughterAttenuation = 1.0f - Math.min(1.0f, Math.max(0.0f, (float) aliensVanquishedSinceEndOfLevel / (float ) aliensSpawnedAtLevelEnd));
			}
			attenuate = 1.0f - Math.min(1.0f, Math.max(0.0f, (float) valueOfDestroyedBuildings / (float ) valueOfBuiltBuildings)) * slaughterAttenuation;
		}

		// Calculate total money in-play
		float total = getMoney() * config.getBankFactor();
		for (int i = buildings.size(); -- i >= 0; ) {
			Building b = buildings.get(i);
			if (b.isAlive()) {
				total += b.getCost();
				irate += b.getAgitation();
			}
		}

		// For every $DIFFICULTY_FACTOR in play add 1.0 difficulty
		switch (getGameMode()) {
			case GAME_MODE_SURVIVAL:
				total += valueOfDestroyedBuildings; // It never gets easier :)
				ret += total / config.getSurvivalModeDifficultyFactors()[survivalParams.getWorld().getIndex()];
				break;
			case GAME_MODE_XMAS:
				total += valueOfDestroyedBuildings;
				ret += total / config.getXmasDifficultyFactor();
				break;
			default:
				ret += total / (config.getDifficultyFactor() + config.getDifficultyFactorPerLevel() * getLevel());
				ret *= attenuate;
				break;
		}

		// Add difficulty for powerups...
		currentDifficulty += powerupDifficulty * attenuate;

		// Finally, adjust by base difficulty offset
		float attemptsDifficulty = Worm.getAutoDifficulty() ? config.getDifficultyAttempts()[Math.min(config.getDifficultyAttempts().length -1, attempts)] : 0.0f;
		currentDifficulty = Math.max(0.0f, irate + ret + metaState.difficulty + attemptsDifficulty);
	}

	public int getResearchHash() {
		return metaState.researched.hashCode();
	}

	/**
	 * Kill the player
	 */
	public void kill() {
		alive = false;
	}

	/**
	 * Quit the game
	 */
	public void quit() {
		MiniGame.endGame();
	}

	private void reset() {
		alive = true;
		rush = false;
		mode = Mode.MODE_NORMAL;
		beginLevel = false;
		startingMoney = 500;
		somethingInterestingHappenedTick = 0;
		crystals = 0;
		for (int i = entities.size(); --i >= 0; ) {
			(entities.get(i)).remove();
		}
		entities.clear();
		for (int i = spawnPoints.size(); --i >= 0; ) {
			(spawnPoints.get(i)).remove();
		}
		spawnPoints.clear();
		gidrahs.clear();
		bosses.clear();
		units.clear();
		buildings.clear();
		saucers.clear();
		medalsThisLevel.clear();
		armedCapacitors.clear();
		unminedCrystalList.clear();
		tick = 0;
		totalTicks = 0;
		saucerTick = 0;
		levelTick = 0;
		bezerkTick = 0;
		survivalSpawnPointTick = 0;
		crystalTick = 0;
		survivalCrystals.clear();
		survivalNextBossTick = config.getSurvivalBossInterval();
		survivalNextCrystalTick = 0;
		survivalNextSpawnPointTick = 0;
		survivalSpawnPointInterval = config.getSurvivalSpawnpointSpawnInterval();
		numSurvivalSpawnPoints = 1;
		survivalGidrahUnlock[0] = 1;
		survivalGidrahUnlock[1] = 0;
		survivalGidrahUnlock[2] = 0;
		survivalGidrahUnlock[3] = 0;
		survivalGidrahUnlockNext[0] = config.getSurvivalGidrahUnlockInterval()[0];
		survivalGidrahUnlockNext[1] = config.getSurvivalGidrahUnlockInterval()[1];
		survivalGidrahUnlockNext[2] = config.getSurvivalGidrahUnlockInterval()[2];
		survivalGidrahUnlockNext[3] = config.getSurvivalGidrahUnlockInterval()[3];
		survivalGidrahKills[0] = 0;
		survivalGidrahKills[1] = 0;
		survivalGidrahKills[2] = 0;
		survivalGidrahKills[3] = 0;
		survivalBoss = 0;
		survivalBossTick = 0;

		GameScreen.getInstance().removeAllTickables();
	}

	/**
	 * Restart the level
	 */
	public void restart() {
		if (metaState.gameMode != GAME_MODE_SURVIVAL && metaState.gameMode != GAME_MODE_XMAS) {
			int attempts = Worm.getExtraLevelData(Game.getPlayerSlot(), metaState.level, metaState.gameMode, "attempts", 0);
			Worm.setExtraLevelData(metaState.level, metaState.gameMode, "attempts", attempts + 1);
		}
		reset();
		doInit(metaState.level);
	}

	private void removeCrystals() {
		// Remove crystals properly so the exclude tiles are removed
		for (Building b : buildings) {
			if (b instanceof CrystalResource) {
				((CrystalResource) b).clearMap();
			}
		}
	}

	public void restartSurvival(boolean generateNew) {
		removeCrystals();
		reset();
		survivalParams.setGenerateNew(generateNew);
		doInit(survivalParams);
	}

	public void restartXmas(boolean generateNew) {
		removeCrystals();
		reset();
		initXmas(generateNew);
	}

	/**
	 * Make the current level easier next time it's played
	 */
	public void easier() {
		int diff = getDifficultyAdjust(metaState.level, metaState.gameMode);
		setDifficultyAdjust(metaState.level, metaState.gameMode, diff + 1);
		if (metaState.gameMode == GAME_MODE_CAMPAIGN || metaState.gameMode == GAME_MODE_ENDLESS) {
			// Reset attempts count
			Worm.setExtraLevelData(metaState.level, metaState.gameMode, "attempts", 0);
		}
	}

	/**
	 * Save and quit
	 */
	public void save() {
		saving = true;
		saveTick = 0;
		LabelEffect saveEffect = new LabelEffect(net.puppygames.applet.Res.getBigFont(), Game.getMessage("ultraworm.wormgamestate.saving_game"), new MappedColor("gui-bright"), new MappedColor("gui-dark"), SAVE_DURATION / 2, SAVE_DURATION / 2);
		saveEffect.setLocation(Game.getWidth() / 2, Game.getHeight() / 2);
		saveEffect.setVisible(true);
		saveEffect.spawn(GameScreen.getInstance());
		saveEffect.setOffset(null);
	}

	public int getCapacitorBoost() {
		return capacitorBoost;
	}

	public int getBatteryBoost() {
		return batteryBoost;
	}

	public int getShieldBoost() {
		return shieldBoost;
	}

	public int getReactorBoost() {
		return reactorBoost;
	}

	public int getScannerBoost() {
		return scannerBoost;
	}

	public int getCoolingBoost() {
		return coolingBoost;
	}

	public void flagHint(String hintName) {
		if (getGameMode() == GAME_MODE_SURVIVAL) {
			// No hints in survival mode
			return;
		}
		HintFeature hint = HintFeature.getHint(hintName);
		if (hint == null) {
			return;
		}
		flagHint(hint);
	}

	private void flagHint(HintFeature hintFeature) {
		if (hintFeature.getMinLevel() > 0 && metaState.level <= hintFeature.getMinLevel()) {
			return;
		}
		if (hintFeature.getMaxLevel() > 0 && metaState.level > hintFeature.getMaxLevel()) {
			return;
		}
		GameScreen.getInstance().showHint(hintFeature);
	}

	public int getHintSequence(HintFeature hintFeature) {
		if (hintFeature.getText() != null) {
			// It's a medal hint
			return 0;
		}

		Integer seq = hintMap.get(hintFeature);
		if (seq == null) {
			seq = new Integer(0);
		}
		int seqValue = seq.intValue();
		if (seqValue == -1) {
			// Hint suppressed
			return -1;
		}
		if (hintFeature.isRandom()) {
			return Util.random(0, hintFeature.getHints().getNumResources() - 1);
		}

		if (seqValue == hintFeature.getHints().getNumResources()) {
			// No hints left
			return -1;
		}
		hintMap.put(hintFeature, new Integer(seqValue + 1));
		return seqValue;
	}

	public void suppressHint(String hint) {
		HintFeature hint2 = HintFeature.getHint(hint);
		if (hint2 == null) {
			System.out.println("Hint "+hint+" not defined");
			return;
		}
		suppressHint(hint2);
	}

	public void suppressHint(HintFeature hintFeature) {
		hintMap.put(hintFeature, new Integer(-1));
		GameScreen.getInstance().dequeueHint(hintFeature);
	}

	/**
	 * @return the number of active Units
	 */
	public int getNumUnits() {
		return units.size();
	}

	/**
	 * Called when a building is destroyed by aliens
	 * @param b
	 */
	public void onBuildingDestroyed(Building b, boolean deliberate) {
		if (b.isWorthAttacking()) {
			if (!deliberate) {
				valueOfDestroyedBuildings += b.getFeature().getInitialValue();
				numberOfBuildingsDestroyed ++;

				if (b.isCity()) {
					GameScreen.instance.zoom(b.getMapX() + b.getCollisionX() - Game.getWidth() / 2, b.getMapY() + b.getCollisionY() - Game.getHeight() / 2);
				}

				int totalBuildingsDestroyed = addStat(Stats.BUILDINGS_DESTROYED, 1);
				if (totalBuildingsDestroyed == 10) {
					awardMedal(Medals.CARELESS);
				} else if (totalBuildingsDestroyed == 25) {
					awardMedal(Medals.RASH);
				} else if (totalBuildingsDestroyed == 50) {
					awardMedal(Medals.RECKLESS);
				} else if (totalBuildingsDestroyed == 100) {
					awardMedal(Medals.NEGLIGENT);
				}

				addStat(Stats.VALUE_OF_BUILDINGS_DESTROYED, b.getFeature().getInitialValue());
			} else {
				valueOfBuiltBuildings -= b.getFeature().getInitialValue();
			}
		}
	}

	/**
	 * Called when any building worth attacking takes actual damage
	 */
	public void onBuildingDamaged() {
		anyDamaged = true;
	}

	/**
	 * Is the level still active? This is the case when we're in {@link #PHASE_NORMAL} phase and either it's a survival mode game
	 * (the level is always active), or the timer's still timing.
	 * @return true if the level is still active
	 */
	public boolean isLevelActive() {
		return phase == PHASE_NORMAL && (metaState.gameMode == GAME_MODE_SURVIVAL || levelTick < getLevelDuration());
	}

	public Building getBase() {
		return base;
	}

	public Building getNextUnminedCrystal() {
		if (unminedCrystalList.size() == 0) {
			return null;
		}
		Building ret = unminedCrystalList.remove(0);
		unminedCrystalList.add(ret);
		return ret;
	}

	public void addUnminedCrystal(Building crystal) {
		unminedCrystalList.add(crystal);
	}
	public void removeUnminedCrystal(Building crystal) {
		unminedCrystalList.remove(crystal);
	}

	public int getBezerkTick() {
		return bezerkTick;
	}

	public int getFreezeTick() {
		return freezeTick;
	}

	public int getShieldTick() {
		return shieldTick;
	}

	public int getAvailableStock(BuildingFeature bf) {
		return metaState.getAvailableStock(bf);
	}

	public void addAvailableStock(BuildingFeature bf, int n) {
		metaState.addAvailableStock(bf, n);
		GameScreen.getInstance().enableBuildings();
	}

	public float getGidrahDeathRatio() {
		if (aliensSpawnedAtLevelEnd == 0) {
			return 0.0f;
		}
		return (float) aliensVanquishedSinceEndOfLevel / (float) aliensSpawnedAtLevelEnd;
	}

	private void onSomethingInterestingHappened() {
		somethingInterestingHappenedTick = 0;
	}

	/**
	 * @return an unmodifiable map of the medals
	 */
	public Map<MedalFeature, Integer> getMedals() {
		return Collections.unmodifiableMap(metaState.medals);
	}

	/**
	 * Award a medal
	 * @param medal
	 * @return a MedalFeature if the medal was awarded, null if not
	 */
	public MedalFeature awardMedal(String medal) {
		MedalFeature mf = MedalFeature.getMedals().get(medal);
		if (mf == null) {
			if (Game.DEBUG) {
				System.out.println("Warning: medal "+medal+" not found");
			}
			return null;
		}
		Integer n = metaState.medals.get(mf);
		if (n == null) {
			n = Integer.valueOf(1);
		} else {
			if (mf.isRepeatable()) {
				n = Integer.valueOf(n.intValue() + 1);
			} else {
				return null;
			}
		}
		metaState.score += mf.getPoints();
		addMoney(mf.getMoney());
		RankFeature newRank = RankFeature.getRank(metaState.score);
		boolean storeSteamStats = false;
		if (newRank != metaState.rank) {
			System.out.println("New Rank: "+newRank.getTitle());
			metaState.rank = newRank;
			addMoney(newRank.getPoints() / 10);
			if (!suppressMedals) {
				GameScreen.getInstance().showHint(newRank.getHint());
			}
			SFX.newRank();
			if (Game.isUsingSteam() && Steam.isCreated() && Steam.isSteamRunning()) {
				try {
				Steam.getUserStats().setAchievement(newRank.getName());
				} catch (SteamException e) {
					System.err.println("Failed to set achievement "+newRank.getName()+" due to "+e);
				}
				storeSteamStats = true;
			}
		}
		metaState.medals.put(mf, n);

		System.out.println("Awarded "+medal+" ("+n+")");
		if (mf.isSteam() && Game.isUsingSteam() && Steam.isCreated() && Steam.isSteamRunning()) {
			try {
			Steam.getUserStats().setAchievement(mf.getName());
			storeSteamStats = true;
			} catch (SteamException e) {
				System.err.println("Failed to set achievement "+mf.getName()+" due to "+e);
		}
		}

		// Update medals earned this level too
		if (!mf.getSuppressHint()) {
			medalsThisLevel.add(mf);
		}

		// Now, if we're currently playing a level, pop up a hint saying what they just got
		if (phase != PHASE_END_OF_GAME && !mf.getSuppressHint() && !suppressMedals) {
			if (mf.getHint() != null) {
				if (Game.DEBUG) {
					System.out.println("Queued hint "+mf.getHint());
				}
				GameScreen.getInstance().showHint(mf.getHint());
				if (mf.isRepeatable()) {
					SFX.medalAwarded();
				} else {
					SFX.achievementUnlocked();
				}
			} else {
				System.out.println("Medal "+mf+" doesn't have a hint!");
			}
		}

		// Store steam stats
		if (storeSteamStats) {
			try {
			Steam.getUserStats().storeStats();
			} catch (SteamException e) {
				System.err.println("Failed to store steam stats due to "+e);
			}
		}
		return mf;
	}

	/**
	 * @return the total score for all awarded medals
	 */
	public int getScore() {
		return metaState.score;
	}

	/**
	 * @return an unmodifiable map of all the medals earned in this level
	 */
	public Set<MedalFeature> getMedalsEarnedThisLevel() {
		return Collections.unmodifiableSet(medalsThisLevel);
	}

	public RankFeature getRank() {
		return metaState.rank;
	}

	/**
	 * Gets the value of a stat
	 * @param stat
	 * @return
	 */
	public int getStat(String stat) {
		return addStat(stat, 0);
	}

	/**
	 * Add a value to a stat
	 * @param stat
	 * @param n
	 * @return the new stat value
	 */
	public int addStat(String stat, int n) {
		return metaState.addStat(stat, n);
	}

	/**
	 * @return text for the story screen in Endless mode
	 */
	public String getStatsText() {

		boolean showAllStats = false;

		StringBuilder sb = new StringBuilder(256);

		if (getGameMode() == GAME_MODE_SURVIVAL) {
			sb.append(Game.getMessage("ultraworm.wormgamestate.stats_survival"));
		} else if (getGameMode() == GAME_MODE_XMAS) {
			if (getLevel() == 0) {
				// Goes in the ZX bot at the start...
				sb.append(Game.getMessage("ultraworm.wormgamestate.stats_xmas"));
			} else {
				// Goes on the Xmas victory screen at the end
				sb.append("{font:smallfont.glfont color:text-bold}");
				sb.append(Game.getMessage("ultraworm.wormgamestate.xmas.battle_statistics"));
				sb.append("{font:tinyfont.glfont}\n\n");

				int angry = getStat(Stats.ANGRY_SPAWNED);
				int bosses = getStat(Stats.BOSSES_SPAWNED);
				int gidlets = getStat(Stats.GIDLETS_SPAWNED);

				int spawned = getStat(Stats.ALIENS_SPAWNED);
				sb.append("{color:text-bold}"+spawned+" "+Game.getMessage("ultraworm.wormgamestate.xmas.aliens_spawned"));

				if (angry > 0 || bosses > 0 || gidlets > 0 || showAllStats) {
					sb.append("\n\t\t {color:text}");
					int normal = spawned - (angry + gidlets + bosses);
					boolean addSpace = false;
					if (normal > 0 || showAllStats) {
						sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.normal_sized_aliens")+": "+normal);
						addSpace = true;
					}
					if (angry > 0 || showAllStats) {
						if (addSpace) {
							sb.append(' ');
						}
						sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.large_sized_aliens")+": "+angry);
						addSpace = true;
					}
					if (gidlets > 0 || showAllStats) {
						if (addSpace) {
							sb.append(' ');
						}
						sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.tiny_sized_aliens")+": "+gidlets);
						addSpace = true;
					}
					if (bosses > 0 || showAllStats) {
						if (addSpace) {
							sb.append(' ');
						}
						sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.bosses")+": "+bosses);
					}
				}


				int vanquished = getStat(Stats.ALIENS_VANQUISHED);
				sb.append("\n{color:text-bold}"+vanquished+" "+Game.getMessage("ultraworm.wormgamestate.xmas.aliens_vanquished"));
				int shot = getStat(Stats.ALIENS_SHOT);

				if (vanquished != shot || showAllStats) {

					int vanquishedCount = 1;
					sb.append("\n\t\t {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.aliens_shot")+": "+shot);

					int crushed = getStat(Stats.ALIENS_CRUSHED);
					if (crushed > 0 || showAllStats) {
						sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.aliens_crushed")+": "+crushed);
						vanquishedCount++;
					}
					int blownUp = getStat(Stats.ALIENS_BLOWN_UP);
					if (blownUp > 0 || showAllStats) {
						sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.aliens_blown_up")+": "+blownUp);
						vanquishedCount++;
					}
					int fried = getStat(Stats.ALIENS_FRIED);
					if (fried > 0 || showAllStats) {
						vanquishedCount++;
						if (vanquishedCount>3) {
							vanquishedCount=0;
							sb.append("\n\t\t");
						}
						sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.aliens_fried")+": "+fried);
					}
					int nuked = getStat(Stats.ALIENS_SMARTBOMBED);
					if (nuked > 0 || showAllStats) {
						vanquishedCount++;
						if (vanquishedCount>3) {
							vanquishedCount=0;
							sb.append("\n\t\t");
						}
						sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.aliens_nuked")+": "+nuked);
					}
				}


				int alienAttacksOnBuildings = getStat(Stats.ALIEN_ATTACKS_ON_BUILDINGS);
				if (alienAttacksOnBuildings > 0 || showAllStats) {
					String msg = Game.getMessage("ultraworm.wormgamestate.xmas.alien_attacks");
					msg = msg.replace("[num]", String.valueOf(alienAttacksOnBuildings));
					msg = msg.replace("[value]", String.valueOf(getStat(Stats.VALUE_OF_BUILDINGS_DESTROYED)));
					sb.append(msg);
				}

				sb.append("\n{color:text-bold}"+getStat(Stats.BUILDINGS_BUILT)+" "+Game.getMessage("ultraworm.wormgamestate.xmas.buildings_built"));
				sb.append("\n\t\t {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.buildings_destroyed")+": "+getStat(Stats.BUILDINGS_DESTROYED));
				int recycled = getStat(Stats.RECYCLED);
				int sold = getStat(Stats.SOLD);
				sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.buildings_sold")+": "+sold);
				sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.xmas.buildings_recycled")+": "+recycled);

				int buildCosts = getStat(Stats.VALUE_OF_BUILDINGS_BUILT);
				sb.append("\n {color:text-bold}\\$"+buildCosts+" "+Game.getMessage("ultraworm.wormgamestate.xmas.money_spent"));
			}

		} else if (getLevel() == 0) {
			sb.append(Game.getMessage("ultraworm.wormgamestate.stats_level0"));
		} else {
			sb.append("{font:smallfont.glfont color:text-bold}");
			sb.append(Game.getMessage("ultraworm.wormgamestate.battle_statistics"));
			sb.append("{font:tinyfont.glfont}\n\n");

			int angry = getStat(Stats.ANGRY_SPAWNED);
			int bosses = getStat(Stats.BOSSES_SPAWNED);
			int gidlets = getStat(Stats.GIDLETS_SPAWNED);

			int spawned = getStat(Stats.ALIENS_SPAWNED);
			sb.append("{color:text-bold}"+spawned+" "+Game.getMessage("ultraworm.wormgamestate.aliens_spawned"));

			if (angry > 0 || bosses > 0 || gidlets > 0 || showAllStats) {
				sb.append("\n\t\t {color:text}");
				int normal = spawned - (angry + gidlets + bosses);
				boolean addSpace = false;
				if (normal > 0 || showAllStats) {
					sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.normal_sized_aliens")+": "+normal);
					addSpace = true;
				}
				if (angry > 0 || showAllStats) {
					if (addSpace) {
						sb.append(' ');
					}
					sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.large_sized_aliens")+": "+angry);
					addSpace = true;
				}
				if (gidlets > 0 || showAllStats) {
					if (addSpace) {
						sb.append(' ');
					}
					sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.tiny_sized_aliens")+": "+gidlets);
					addSpace = true;
				}
				if (bosses > 0 || showAllStats) {
					if (addSpace) {
						sb.append(' ');
					}
					sb.append("{color:text}"+Game.getMessage("ultraworm.wormgamestate.bosses")+": "+bosses);
				}
			}


			int vanquished = getStat(Stats.ALIENS_VANQUISHED);
			sb.append("\n{color:text-bold}"+vanquished+" "+Game.getMessage("ultraworm.wormgamestate.aliens_vanquished"));
			int shot = getStat(Stats.ALIENS_SHOT);

			if (vanquished != shot || showAllStats) {

				int vanquishedCount = 1;
				sb.append("\n\t\t {color:text}"+Game.getMessage("ultraworm.wormgamestate.aliens_shot")+": "+shot);

				int crushed = getStat(Stats.ALIENS_CRUSHED);
				if (crushed > 0 || showAllStats) {
					sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.aliens_crushed")+": "+crushed);
					vanquishedCount++;
				}
				int blownUp = getStat(Stats.ALIENS_BLOWN_UP);
				if (blownUp > 0 || showAllStats) {
					sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.aliens_blown_up")+": "+blownUp);
					vanquishedCount++;
				}
				int fried = getStat(Stats.ALIENS_FRIED);
				if (fried > 0 || showAllStats) {
					vanquishedCount++;
					if (vanquishedCount>3) {
						vanquishedCount=0;
						sb.append("\n\t\t");
					}
					sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.aliens_fried")+": "+fried);
				}
				int nuked = getStat(Stats.ALIENS_SMARTBOMBED);
				if (nuked > 0 || showAllStats) {
					vanquishedCount++;
					if (vanquishedCount>3) {
						vanquishedCount=0;
						sb.append("\n\t\t");
					}
					sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.aliens_nuked")+": "+nuked);
				}
			}


			int alienAttacksOnBuildings = getStat(Stats.ALIEN_ATTACKS_ON_BUILDINGS);
			if (alienAttacksOnBuildings > 0 || showAllStats) {
				String msg = Game.getMessage("ultraworm.wormgamestate.alien_attacks");
				msg = msg.replace("[num]", String.valueOf(alienAttacksOnBuildings));
				msg = msg.replace("[value]", String.valueOf(getStat(Stats.VALUE_OF_BUILDINGS_DESTROYED)));
				sb.append(msg);
			}

			sb.append("\n{color:text-bold}"+getStat(Stats.BUILDINGS_BUILT)+" "+Game.getMessage("ultraworm.wormgamestate.buildings_built"));
			sb.append("\n\t\t {color:text}"+Game.getMessage("ultraworm.wormgamestate.buildings_destroyed")+": "+getStat(Stats.BUILDINGS_DESTROYED));
			int recycled = getStat(Stats.RECYCLED);
			int sold = getStat(Stats.SOLD);
			sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.buildings_sold")+": "+sold);
			sb.append(" {color:text}"+Game.getMessage("ultraworm.wormgamestate.buildings_recycled")+": "+recycled);

			int buildCosts = getStat(Stats.VALUE_OF_BUILDINGS_BUILT);
			sb.append("\n {color:text-bold}\\$"+buildCosts+" "+Game.getMessage("ultraworm.wormgamestate.money_spent"));

		}

		return sb.toString();
	}

	public void setAwesome() {
		awesome = true;
	}

	public boolean isAwesome() {
		return awesome;
	}

	public int getCrystals() {
		return crystals;
	}

	public int getInitialCrystals() {
		return initialCrystals;
	}

	public PowerupFeature getCrappyPowerup() {
		return metaState.getCrappyPowerup();
    }

	public PowerupFeature getRandomPowerup() {
		return metaState.getRandomPowerup();
    }

	/**
     * @return an exotic powerup
     */
	public PowerupFeature getExoticPowerup() {
    	return metaState.getExoticPowerup();
    }

	/**
     * @return a non-exotic powerup
     */
    public PowerupFeature getPowerup() {
    	return metaState.getPowerup();
    }

    /**
     * @return an analysis of what went wrong
     */
    public String getAnalysis() {
    	boolean lotsOfMoneyLeft = getMoney() > 750 * (getLevel() / LEVELS_IN_WORLD);
    	int numBuildings = 0;
    	int numTurrets = 0;
    	for (Building b : getBuildings()) {
    		if (b.canSell()) {
    			numBuildings ++;
    		}
    		if (b instanceof Turret) {
    			numTurrets ++;
    		}
    	}
    	boolean lotsOfBuildings = numBuildings > getLevel();
    	boolean lotsOfRefineries = totalFactories >= initialCrystals * 3;
    	boolean lotsOfTurrets = numTurrets > spawnPoints.size() * 1.5f + getWorld().getIndex();
    	boolean hasUnrefinedCrystals = unminedCrystalList.size() > 0;
    	boolean lotsOfAliensSlain = aliensVanquishedValue > aliensSpawnedValue * 0.75 && aliensSpawnedValue > 1000;
    	boolean lotsDestroyed = (float) valueOfDestroyedBuildings / (float) valueOfBuiltBuildings > 0.5f;
    	boolean hasPowerups = false;
    	for (Integer i : metaState.powerups.values()) {
    		if (i > 0) {
    			hasPowerups = true;
    			break;
    		}
    	}

    	class Message implements Comparable<Message> {
    		int priority;
    		String msg;

    		public Message(int priority, String msg) {
	            this.priority = priority;
	            this.msg = msg;
            }

			@Override
            public int compareTo(Message o) {
				if (o.priority > priority) {
					return 1;
				} else if (o.priority < priority) {
					return -1;
				} else {
					return 0;
				}
    		}
    	}

    	List<Message> messages = new ArrayList<Message>();

    	if (!lotsOfTurrets) {
    		messages.add(new Message(700, Game.getMessage("ultraworm.wormgamestate.hint1")));
    	}
    	if (lotsDestroyed) {
    		messages.add(new Message(800, Game.getMessage("ultraworm.wormgamestate.hint2")));
    	} else if (valueOfBuiltBuildings > 2500) {
    		messages.add(new Message(600, Game.getMessage("ultraworm.wormgamestate.hint3")));
    	}
    	if (!lotsOfAliensSlain && lotsOfRefineries) {
    		messages.add(new Message(200, Game.getMessage("ultraworm.wormgamestate.hint4")));
    	}
    	if (hasUnrefinedCrystals) {
    		messages.add(new Message(100, Game.getMessage("ultraworm.wormgamestate.hint5")));
    	}
    	if (!lotsOfRefineries && (getLevel() > 4 || getGameMode() == GAME_MODE_SURVIVAL)) {
    		messages.add(new Message(900, Game.getMessage("ultraworm.wormgamestate.hint6")));
    	}
    	if (hasPowerups) {
    		messages.add(new Message(500, Game.getMessage("ultraworm.wormgamestate.hint7")));
    	}
    	if (!lotsOfMoneyLeft) {
    		if (lotsOfBuildings) {
    			messages.add(new Message(100, Game.getMessage("ultraworm.wormgamestate.hint8")));
    		} else {
    			if (isResearched(ResearchFeature.CONCRETE)) {
    				messages.add(new Message(100, Game.getMessage("ultraworm.wormgamestate.hint9")));
    			} else if (isResearched(ResearchFeature.TANGLEWEB)) {
    				messages.add(new Message(100, Game.getMessage("ultraworm.wormgamestate.hint10")));
    			} else if (isResearched(ResearchFeature.MINES)) {
    				messages.add(new Message(100, Game.getMessage("ultraworm.wormgamestate.hint11")));
    			} else {
    				messages.add(new Message(100, Game.getMessage("ultraworm.wormgamestate.hint12")));
    			}
    		}
    	} else {
    		messages.add(new Message(1000, Game.getMessage("ultraworm.wormgamestate.hint13")));
    	}
    	if (getLevelFeature().getBosses() != null) {
    		if (getLevelFeature().getBosses().getNumResources() == 1) {
    			messages.add(new Message(900, Game.getMessage("ultraworm.wormgamestate.hint14")));
    		} else {
    			messages.add(new Message(900, Game.getMessage("ultraworm.wormgamestate.hint15")));
    		}
    	}

    	StringBuilder sb = new StringBuilder(256);
    	sb.append("{color:text-bold}");
    	sb.append(Game.getMessage("ultraworm.wormgamestate.battle_analysis"));
    	sb.append(":{color:text}");

    	Collections.sort(messages);
    	int count = 0;
    	for (Message m : messages) {
    		sb.append("\n\t\t");
    		sb.append(m.msg);
    		if (++ count == 3) {
    			break;
    		}
    	}

    	return sb.toString();
   	}

    public float getScavengeRate() {
		int rate = Worm.getGameState().isResearched(ResearchFeature.FINETUNING) ? 1 : 0;
		rate += Worm.getGameState().isResearched(ResearchFeature.EXTRACTION) ? 1 : 0;
		return config.getScavengeRate()[rate];
    }

//    public MapGeneratorParams getMapGeneratorParams() {
//    	return new MapGeneratorParams( this.getBasicDifficulty(), this.getGameMode(), this.getLevel(), this.getLevelFeature(), this.getLevelInWorld(), this.getMoney(), this.getResearchHash(), this.getWorld() );
//    }

    /**
     * Hax! This returns true if we want the level generator thread to generate a new level, or false if not.
     * @return
     * @see #initXmas(boolean)
     */
    public boolean isXmasReset() {
	    return xmasReset;
    }
}
