package worm.generator;

import worm.features.LevelFeature;
import worm.features.WorldFeature;

/*
 * Used for passing values into AbstractMapGenerator and BaseMapGenerator methods.
 * Created in WormGameState and populated with values from there.
 * or - Created in SandboxEditScreen and populated with defaults.
 */
public class MapGeneratorParams {

	/*
	 * Full Constructor
	 */
	public MapGeneratorParams(float basicDifficulty, int gameMode, int level, LevelFeature levelFeature, int levelInWorld,
			int money, int researchHash, WorldFeature worldFeature) {
		this.basicDifficulty = basicDifficulty;
		this.gameMode = gameMode;
		this.level = level;
		this.levelFeature = levelFeature;
		this.levelInWorld = levelInWorld;
		this.money = money;
		this.researchHash = researchHash;
		this.worldFeature = worldFeature;
	}
//	/*
//	 * Level-related stuff only Constructor
//	 */
//	public MapGeneratorParams(int level, int levelInWorld, LevelFeature levelFeature) {
//		this.basicDifficulty = 0.5f;
//		this.gameMode = -1;
//		this.level = level;
//		this.levelFeature = levelFeature;
//		this.levelInWorld = levelInWorld;
//		this.money = 0;
//		this.researchHash = 0;
//		this.worldFeature = levelFeature.getWorld();
//	}
//	/*
//	 * LevelFeature only Constructor
//	 */
//	public MapGeneratorParams(LevelFeature levelFeature) {
//		this.basicDifficulty = 0.5f;
//		this.gameMode = -1;
//		this.level = -1;
//		this.levelFeature = levelFeature;
//		this.levelInWorld = -1;
//		this.money = 0;
//		this.researchHash = 0;
//		this.worldFeature = levelFeature.getWorld();
//	}
	private final float basicDifficulty;
	private final int gameMode;
	private final int level;
	private final LevelFeature levelFeature;
	private final int levelInWorld;
	private final int money;
	private final int researchHash;
	private final WorldFeature worldFeature;

	public float getBasicDifficulty() {
		return basicDifficulty;
	}
	public int getGameMode() {
		return gameMode;
	}
	public int getLevel() {
		return level;
	}
	public LevelFeature getLevelFeature() {
		return levelFeature;
	}
	public int getLevelInWorld() {
		return levelInWorld;
	}
	public int getMoney() {
		return money;
	}
	public int getResearchHash() {
		return researchHash;
	}
	public WorldFeature getWorldFeature() {
		return worldFeature;
	}
}
