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
package worm.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.puppygames.applet.Game;

import org.w3c.dom.Element;

import worm.MapRenderer;
import worm.Res;
import worm.SandboxParams;
import worm.SurvivalParams;
import worm.Worm;
import worm.WormGameState;
import worm.Xmas;
import worm.effects.WeatherEmitter;
import worm.generator.BaseMapGenerator;
import worm.generator.MapTemplate;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.util.Util;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Describes a level
 */
public class LevelFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/** Autonaming hints counter */
	private static int hintCount;

	private static final String ENDLESS_INTRO = "endless.intro";
	private static final String ENDLESS_STORY = "endless.story";
	private static final String ENDLESS_SURROUNDED_STORY = "endless.surrounded.story";
	private static final String ENDLESS_BOSS_STORY = "endless.boss.story";
	private static final String ENDLESS_SURROUNDED_BOSS_STORY = "endless.surrounded-boss.story";
	private static final String SURVIVAL_INTRO = "survival.intro";
	private static final String XMAS_INTRO = "xmas.intro";

	public static final int MIN_SIZE = 20;
	public static final int MAX_SIZE = 72;

	/** Compass bias: the bases will be clustered in a particular part of the map and the spawnpoints opposite. -1 means central bases. */
	public static final int[] BIAS = {6, 2, 0, 4, 5, 1, 7, 3, -1, -1, 2, 6, 4, 0, 3, 5, 1, 7, -1, -1, 0, 4, 6, 2, -1, 1, 7, 3, 5, -1, 0, 1, 2, 3, 4, 5, -1, 6, 7, -1};


	private static final int ENDLESS_MARCH = 1000; // Each level, add this notional value to research

	private static final ArrayList<LevelFeature> LEVELS = new ArrayList<LevelFeature>(50);

	// Note use of @Data as we need to copy LevelFeatures for Endless and Survival modes

	/** Index */
	protected int index;

	/** World */
	@Data
	protected String world;

	/** Title */
	@Data
	protected String title;

	/** Level coloration */
	@Data
	protected String colors;

	/** Bosses */
	protected ResourceArray bosses;

	/** The stories */
	protected StoryFeature[] storyFeature;

	/** Scenery */
	@Data
	protected String scenery;

	/** Hints */
	protected HintFeature[] eventFeature;

	/** Formation */
	@Data
	protected String formation;

	/** Width */
	protected int width;

	/** Height */
	protected int height;

	/** Bias */
	protected int bias;

	/** Xmas level */
	private boolean xmas;

	/** Weather emitter */
	@Data
	protected String weather;

	/*
	 * Transient data
	 */

	protected transient WorldFeature worldFeature;
	protected transient LevelColorsFeature colorsFeature;
	protected transient SceneryFeature sceneryFeature;
	protected transient WeatherEmitter weatherFeature;

	/**
	 * C'tor
	 */
	public LevelFeature() {
		setAutoCreated();
	}

	/**
	 * C'tor
	 * @param name
	 */
	public LevelFeature(String name) {
		super(name);
		setAutoCreated();
		setName("level."+name);
	}


	@Override
	protected void doRegister() {
		if (!xmas) {
			index = LEVELS.size();
			LEVELS.add(this);
		}
	}


	@Override
	protected void doDeregister() {
		if (!xmas) {
			LEVELS.remove(this);
		}
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> children = XMLUtil.getChildren(element, "story");
		storyFeature = new StoryFeature[children.size()];
		int count = 0;
		for (Element child : children) {
			storyFeature[count] = new StoryFeature();
			storyFeature[count].load(child, loader);
			count ++;
		}

		List<Element> hintChildren = XMLUtil.getChildren(element, "event");
		eventFeature = new HintFeature[hintChildren.size()];
		int eventCount = 0;
		for (Element child : hintChildren) {
			if (child.hasAttribute("name")) {
				eventFeature[eventCount] = new HintFeature(XMLUtil.getString(child, "name"));
				eventFeature[eventCount].register();
			} else {
				eventFeature[eventCount] = new HintFeature();
				eventFeature[eventCount].setName("hintfeature."+(hintCount++));
			}
			eventFeature[eventCount].load(child, loader);
			Resources.put(eventFeature[eventCount]);

			eventCount ++;
		}
	}

	/**
	 * @return the events
	 */
	public HintFeature[] getEvents() {
		return eventFeature;
	}

	/**
	 * @return the colors for this level
	 */
	public LevelColorsFeature getColors() {
		return colorsFeature;
	}

	/**
	 * @param colorsFeature the colorsFeature to set
	 */
	public void setColors(LevelColorsFeature colorsFeature) {
		this.colorsFeature = colorsFeature;
	}

	/**
	 * @return the world this level is in
	 */
	public WorldFeature getWorld() {
		return worldFeature;
	}

	/**
	 * @return the number of levels
	 */
	public static int getNumLevels() {
		return LEVELS.size();
	}

	/**
	 * Gets a campaign level by its index
	 * @param idx
	 * @return a level
	 */
	public static LevelFeature getLevel(int idx) {
		return LEVELS.get(idx % LEVELS.size());
	}

	/**
	 * @return the bosses
	 */
	public ResourceArray getBosses() {
		return bosses;
	}

	/**
	 * @return the story
	 */
	public StoryFeature[] getStories() {
		return storyFeature;
	}

	@Override
	protected void doCreate() {
		super.doCreate();
		for (StoryFeature element : storyFeature) {
			element.create();
		}
		for (HintFeature element : eventFeature) {
			element.create();
		}

		calculateDimensions();

		assert weather == null || (weather != null && weatherFeature != null);
	}

	protected void calculateDimensions() {
		int levelInWorld = index % WormGameState.LEVELS_IN_WORLD;
		bias = getBias(index);

		if (index <= 5) {
			width = height = getMinHeight();
		} else {
			width = height = (int) LinearInterpolator.instance.interpolate
				(
					LinearInterpolator.instance.interpolate(MIN_SIZE, MAX_SIZE, index / WormGameState.LEVELS_IN_WORLD / 5.0f),
					LinearInterpolator.instance.interpolate(MIN_SIZE, WormGameState.ABS_MAX_SIZE, (index / WormGameState.LEVELS_IN_WORLD + 1) / 5.0f),
					levelInWorld / (float) (WormGameState.LEVELS_IN_WORLD - 1)
				);
		}

		switch (getBias()) {
			case -1:
				height = Math.min(WormGameState.ABS_MAX_SIZE, (int)(height * 1.5f));
				width = Math.min(WormGameState.ABS_MAX_SIZE, (int)(height * 1.5f));
				break;
			case 0:
			case 4:
				width = Math.min(WormGameState.ABS_MAX_SIZE, (int)(height * 1.25f));
				break;
			case 2:
			case 6:
				height = Math.min(WormGameState.ABS_MAX_SIZE, (int)(height * 1.25f));
				break;
			default:
				// No adjustment
		}

		// Make sure map's an even width / height
		height = Math.max(height, getMinHeight()) & 0xFFFE;
		width = Math.max(width, getMinWidth()) & 0xFFFE;
	}

	/**
	 * @return the bias
	 */
	public int getBias() {
		return bias;
	}

	private static int getMinWidth() {
		int displayWidth = Game.getWidth();
		int displayHeight = Game.getHeight();
		int smallerDimension = Math.min(displayWidth, displayHeight);
		int res = smallerDimension / Game.getScale();
		int fits = displayWidth / res; // This is the largest number of pixels
		return Math.max(MIN_SIZE, fits / MapRenderer.TILE_SIZE + 2);
	}

	private static int getMinHeight() {
		int displayWidth = Game.getWidth();
		int displayHeight = Game.getHeight();
		int smallerDimension = Math.min(displayWidth, displayHeight);
		int res = smallerDimension / Game.getScale();
		int fits = displayHeight / res; // This is the largest number of pixels
		return Math.max(MIN_SIZE, fits / MapRenderer.TILE_SIZE + 2);
	}


	@Override
	protected void doDestroy() {
		super.doDestroy();
		for (StoryFeature element : storyFeature) {
			element.destroy();
		}
		for (HintFeature element : eventFeature) {
			element.destroy();
		}
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	public SceneryFeature getScenery() {
		return sceneryFeature;
	}

	public MapTemplate getTemplate() {
		return worldFeature.getTemplate();
	}

	public int getNumSpawnPoints() {
		return formation.length();
	}

	/**
	 * Generates a random level
	 * @param level Level number
	 * @return an unnamed LevelFeature
	 */
	public static LevelFeature generateEndless(int level) {
		Random r = new Random((long) level ^ Game.getPlayerSlot().getName().hashCode() << 32L); // Random, but always the same sequence
		float rf = r.nextFloat();
		int idx = (int)(rf * (LEVELS.size() - 1));
		idx %= Worm.getMaxLevel(WormGameState.GAME_MODE_ENDLESS) + 1;
		LevelFeature ret = new EndlessLevelFeature(level, getLevel(idx));
		ret.create();
		return ret;
	}

	/**
	 * Generates a survival level
	 * @param worldIndex World index, 0...4
	 * @param size Size in tiles
	 * @return an unnamed LevelFeature
	 */
	public static LevelFeature generateSurvival(SurvivalParams params) {
		LevelFeature ret = new SurvivalLevelFeature(params);
		ret.create();
		return ret;
	}

	/**
	 * Generates an Xmas level
	 * @return an unnamed LevelFeature
	 */
	public static LevelFeature generateXmas() {
		LevelFeature ret = new XmasLevelFeature();
		ret.create();
		return ret;
	}

	public static LevelFeature generateSandbox(SandboxParams params) {
		LevelFeature ret = new SandboxLevelFeature(params);
		ret.create();
		return ret;
	}


	public String getFormation() {
		return formation;
	}

	public boolean useFixedSpawnPoints() {
		return true;
	}

	public GidrahFeature getAngryGidrah(int type) {
		return worldFeature.getAngryGidrah(type);
	}

	public GidrahFeature getGidrah(int type) {
		return worldFeature.getGidrah(type);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	private static int getBias(int level) {
		if (level == -1) {
			// Survival mode
			return -1;
		}
		return BIAS[level % BIAS.length];
	}

	/**
	 * Endless levels
	 */
	public static class EndlessLevelFeature extends LevelFeature {

		private static final long serialVersionUID = 1L;

		private ArrayList<GidrahFeature> gidrahs = new ArrayList<GidrahFeature>(4);
		private ArrayList<GidrahFeature> angryGidrahs = new ArrayList<GidrahFeature>(4);

		private static boolean hasBosses(int level) {
			if (level < 9) {
				// No bosses for first 10 levels
				return false;
			} else if (level == 9 || level == 19) {
				// Boss on levels 10, 20
				return true;
			} else if (level >= 20 && level <= 40) {
				// Boss every 5 levels
				return level % 5 == 4;
			} else {
				// Boss every 3 levels
				return level % 3 == 2;
			}
		}

		/**
		 * C'tor
		 */
		public EndlessLevelFeature(int level, LevelFeature src) {
			index = level;
			title = "LEVEL "+(level + 1);
			colors = src.colors;
			scenery = src.scenery;
			world = src.world;
			weather = src.weather;
			weatherFeature = src.weatherFeature;

			boolean central = BaseMapGenerator.isBaseCentralForLevel(level, WormGameState.GAME_MODE_ENDLESS);
			boolean bosses = hasBosses(level);
			String storyType;

			if (level == 0) {
				storyType = ENDLESS_INTRO;
			} else {
				if (bosses) {
					if (central) {
						storyType = ENDLESS_SURROUNDED_BOSS_STORY;
					} else {
						storyType = ENDLESS_BOSS_STORY;
					}
				} else if (central) {
					storyType = ENDLESS_SURROUNDED_STORY;
				} else {
					storyType = ENDLESS_STORY;
				}
			}
			storyFeature = new StoryFeature[] {(StoryFeature) Resources.get(storyType)};
			eventFeature = new HintFeature[0]; // No events

			// Spawnpoints are arranged in 10 level groups: small to begin with, up to a larger figure. We interpolate between them in the 10 intervening levels.
			int levelInGroup = level % WormGameState.LEVELS_IN_WORLD;
			int minSpawnpoints = level / WormGameState.LEVELS_IN_WORLD + 1; // 1, 2, 3, etc
			int maxSpawnpoints = Math.min(14, minSpawnpoints + 4 + level / (WormGameState.LEVELS_IN_WORLD * 2)); // 5, 6, 7 etc
			int numSpawnpoints = (int) LinearInterpolator.instance.interpolate(minSpawnpoints, maxSpawnpoints, levelInGroup / (WormGameState.LEVELS_IN_WORLD - 1.0f));
			if (level > 0) {
				numSpawnpoints ++;
			}
			if (level > 1) {
				numSpawnpoints ++;
			}

			// Find out which gidrahs are unlocked
			@SuppressWarnings("unchecked")
			List<GidrahFeature>[] gidrahFeatures = new ArrayList[4];
			@SuppressWarnings("unchecked")
			List<GidrahFeature>[] angryGidrahFeatures = new ArrayList[4];
			for (int i = 0; i < 4; i ++) {
				gidrahFeatures[i] = new ArrayList<GidrahFeature>(4);
				angryGidrahFeatures[i] = new ArrayList<GidrahFeature>(4);

				ResourceArray ga = Res.getEndlessGidrahs(i);
				ResourceArray aga = Res.getEndlessAngryGidrahs(i);
				assert ga.getNumResources() == aga.getNumResources();
				for (int j = 0; j < ga.getNumResources(); j ++) {
					GidrahFeature gf = (GidrahFeature) ga.getResource(j);
					if (gf != null && gf.isUnlocked()) {
						gidrahFeatures[i].add(gf);
						GidrahFeature agf = (GidrahFeature) aga.getResource(j);
						if (agf != null && agf.isUnlocked()) {
							angryGidrahFeatures[i].add(agf);
						} else {
							// Downgrade
							angryGidrahFeatures[i].add(gf);
						}
					}
				}
			}

			// Gradually adjust ratio of gidrahs over the 10 level group, biasing heavily towards difficult and exotic
			int type4ratio = gidrahFeatures[3].size() == 0 ? 0 : levelInGroup * 2;
			int type3ratio = gidrahFeatures[2].size() == 0 ? 0 : levelInGroup * 5;
			int type2ratio = gidrahFeatures[1].size() == 0 ? 0 : levelInGroup * 10;
			int type1ratio = 40 - (int) Math.min(20.0f, Worm.getGameState().getBasicDifficulty() * 20.0f); // Less rank and file the better you are
			int total = type4ratio + type3ratio + type2ratio + type1ratio;

			int numType4 = numSpawnpoints * type4ratio / total;
			int numType3 = numSpawnpoints * type3ratio / total;
			int numType2 = numSpawnpoints * type2ratio / total;
			int numType1 = Math.max(0, numSpawnpoints - (numType4 + numType3 + numType2));

			System.out.println("Ratios: 1:"+type1ratio+" 2:"+type2ratio+" 3:"+type3ratio+" 4:"+type4ratio);
			System.out.println("Numbers: 1:"+numType1+" 2:"+numType2+" 3:"+numType3+" 4:"+numType4);
			System.out.println("Num spawnpoints:"+numSpawnpoints);
			int[] numOfEachType = {numType1, numType2, numType3, numType4};

			for (int i = 0; i < 4; i ++) {
				if (numOfEachType[i] > 0) {
					// Pick a random type
					int idx = Util.random(0, gidrahFeatures[i].size() - 1);
					GidrahFeature gf = gidrahFeatures[i].get(idx);
					GidrahFeature agf = angryGidrahFeatures[i].get(idx);
					gidrahs.add(gf);
					System.out.println("Gidrah Type "+i+" is "+gf);
					angryGidrahs.add(agf);
				} else {
					// Pad with a null
					gidrahs.add(null);
					angryGidrahs.add(null);
				}
			}

			// Ok, that's up to four types of gidrah. Allocate spawnpoints according to ratio
			StringBuilder sb = new StringBuilder(numSpawnpoints);
			for (int i = 0; i < numType1; i ++) {
				sb.append('1');
			}
			for (int i = 0; i < numType2; i ++) {
				sb.append('2');
			}
			for (int i = 0; i < numType3; i ++) {
				sb.append('3');
			}
			for (int i = 0; i < numType4; i ++) {
				sb.append('4');
			}
			formation = sb.toString();
			System.out.println("Formation:" +formation);

		}

		@Override
		public GidrahFeature getAngryGidrah(int type) {
			GidrahFeature gf = angryGidrahs.get(type);
			if (gf == null) {
				return getGidrah(type);
			}
			return gf;
		}

		@Override
		public GidrahFeature getGidrah(int type) {

			return gidrahs.get(type);
		}

	}

	/**
	 * Survival level
	 */
	public static class SurvivalLevelFeature extends LevelFeature {

		private static final long serialVersionUID = 1L;

		private ArrayList<GidrahFeature> gidrahs = new ArrayList<GidrahFeature>(4);
		private ArrayList<GidrahFeature> angryGidrahs = new ArrayList<GidrahFeature>(4);

		private MapTemplate template;

		/**
		 * C'tor
		 */
		public SurvivalLevelFeature(SurvivalParams params) {
			index = -1;
			worldFeature = params.getWorld();
			template = params.getTemplate();
			title = "SURVIVAL";
			// Pick random colours
			LevelFeature src = LevelFeature.getLevel(Util.random(0, WormGameState.LEVELS_IN_WORLD - 1) + worldFeature.getIndex() * WormGameState.LEVELS_IN_WORLD);
			colors = src.colors;
			scenery = src.scenery;
			weather = src.weather;
			weatherFeature = src.weatherFeature;
			world = worldFeature.getName();
			storyFeature = new StoryFeature[] {(StoryFeature) Resources.get(SURVIVAL_INTRO)};
			eventFeature = new HintFeature[0]; // No events
			formation = "";
			width = params.getSize();
			height = params.getSize();
			bias = -1;
		}

		@Override
		public int getNumSpawnPoints() {
			return width / 2; // Loads! Approx 1/8th of the edge is spawnpoints.
		}

		@Override
		public MapTemplate getTemplate() {
			return template;
		}

		@Override
		protected void calculateDimensions() {
			// Do nothing
		}

		@Override
		public GidrahFeature getAngryGidrah(int type) {
			GidrahFeature gf = angryGidrahs.get(type);
			if (gf == null) {
				return getGidrah(type);
			}
			return gf;
		}

		@Override
		public GidrahFeature getGidrah(int type) {
			GidrahFeature ret = gidrahs.get(type);
			if (ret == null) {
				throw new RuntimeException("Failed to get gidrah of type "+type);
			}
			return ret;
		}

		@Override
		public boolean useFixedSpawnPoints() {
			return false;
		}
	}

	/**
	 * Xmas level
	 */
	public static class XmasLevelFeature extends LevelFeature {

		private static final long serialVersionUID = 1L;

		/**
		 * C'tor
		 */
		public XmasLevelFeature() {
			index = -1;
			title = "XMAS";
			worldFeature = Resources.get(Xmas.XMAS_WORLD);
			LevelFeature src = Resources.get(Xmas.XMAS_LEVEL);
			colors = src.colors;
			scenery = src.scenery;
			weather = src.weather;
			weatherFeature = src.weatherFeature;
			world = worldFeature.getName();
			storyFeature = src.getStories();
			eventFeature = src.getEvents();
			formation = "";
			width = Xmas.XMAS_WIDTH;
			height = Xmas.XMAS_HEIGHT;
			bias = 6; // Base in the south
		}

		@Override
		public int getNumSpawnPoints() {
			return 1; // 1 stream of gids
		}

		@Override
		public MapTemplate getTemplate() {
			return worldFeature.getTemplate();
		}

		@Override
		protected void calculateDimensions() {
			// Do nothing
		}

		@Override
		public GidrahFeature getAngryGidrah(int type) {
			return null;
		}

		@Override
		public GidrahFeature getGidrah(int type) {
			return null;
		}
	}

	/**
	 * Sandbox level
	 */
	public static class SandboxLevelFeature extends LevelFeature {

		private static final long serialVersionUID = 1L;

		private ArrayList<GidrahFeature> gidrahs = new ArrayList<GidrahFeature>(4);
		private ArrayList<GidrahFeature> angryGidrahs = new ArrayList<GidrahFeature>(4);

		private MapTemplate template;

		/**
		 * C'tor
		 */
		public SandboxLevelFeature(SandboxParams params) {
			index = -1;
			worldFeature = params.getWorld();
			template = params.getTemplate();
			title = "SANDBOX";
			// Pick random colours
			LevelFeature src = LevelFeature.getLevel(Util.random(0, WormGameState.LEVELS_IN_WORLD - 1) + worldFeature.getIndex() * WormGameState.LEVELS_IN_WORLD);
			colors = src.colors;
			scenery = src.scenery;
			weatherFeature = src.weatherFeature;
			world = worldFeature.getName();
			storyFeature = new StoryFeature[0]; // No story
			eventFeature = new HintFeature[0]; // No events
			formation = "";
			width = params.getSize();
			height = params.getSize();
			bias = -1;
		}

		@Override
		public int getNumSpawnPoints() {
			return 0; // none, we're going to add them later somehow
		}

		@Override
		public MapTemplate getTemplate() {
			return template;
		}

		@Override
		protected void calculateDimensions() {
			// Do nothing
		}

		@Override
		public GidrahFeature getAngryGidrah(int type) {
			GidrahFeature gf = angryGidrahs.get(type);
			if (gf == null) {
				return getGidrah(type);
			}
			return gf;
		}

		@Override
		public GidrahFeature getGidrah(int type) {
			GidrahFeature ret = gidrahs.get(type);
			if (ret == null) {
				throw new RuntimeException("Failed to get gidrah of type "+type);
			}
			return ret;
		}

		@Override
		public boolean useFixedSpawnPoints() {
			return false;
		}
	}

	/**
	 * @return the weather, if any
	 */
	public WeatherEmitter getWeather() {
	    return weatherFeature;
    }

}
