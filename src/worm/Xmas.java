/**
 *
 */
package worm;

import worm.features.LevelFeature;

/**
 * Steam Xmas stuff!
 */
public class Xmas {

	/**
	 * The research you start with
	 */
	public static final String[] RESEARCH = {
		"optics",
		"anatomy",
		"extraction",
		"factory",
		"reactor",
		"shieldgenerator",
		"barracks",
		"collector",
		"decoy",
		"concrete",
		"steel",
		"titanium",
		"nanomesh",
		"mines",
		"clustermines",
		"blastmine",
		"capacitor",
		"blaster",
		"heavyblaster",
		"multiblaster",
		"blastcannon",
		"spreadercannon",
		"assaultcannon",
		"rockets",
		"laser",
		"disruptor",
		"battery",
		"autoloader",
		"warehouse",
		"scanner",
		"coolingtower",
		"tangleweb",
		"tankfactory",
		"repairdrones",
		"scarecrow",
		"cloakingdevice",
		"shielding"
	};

	/** The size of the special Xmas level */
	public static final int XMAS_WIDTH = (int) (LevelFeature.MAX_SIZE * 0.5);
	public static final int XMAS_HEIGHT = LevelFeature.MAX_SIZE;

	/** Resource name of the Xmas world */
	public static final String XMAS_WORLD = "hoff.world";

	/** Resource name of the Xmas level */
	public static final String XMAS_LEVEL = "level.xmas.level";

	/** Number of bosses (needs to agree with xmas.bosses.array size) */
	public static final int MAX_BOSSES = 5;

	/** Boss interval in ticks */
	public static final int BOSS_INTERVAL = 18000;

	/** Angry gid interval in ticks */
	public static final int ANGRY_INTERVAL = 1800;

	/** Duration of the level */
	public static final int DURATION = BOSS_INTERVAL * MAX_BOSSES;
}
