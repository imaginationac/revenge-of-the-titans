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

import java.io.*;
import java.util.*;

import net.puppygames.applet.*;
import net.puppygames.applet.effects.LabelEffect;
import net.puppygames.applet.effects.Particle;
import net.puppygames.applet.screens.DialogScreen;
import net.puppygames.applet.screens.NagScreen;
import net.puppygames.gamecommerce.shared.NewsletterIncentive;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Rectangle;

import worm.animation.SimpleThingWithLayers;
import worm.features.LayersFeature;
import worm.features.PrizeFeature;
import worm.screens.*;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.*;
import com.shavenpuppy.jglib.sprites.*;
import com.shavenpuppy.jglib.util.Util;

/**
 * $Id: Worm.java,v 1.56 2010/11/06 17:20:03 foo Exp $
 * The Puppytron game
 * <p>
 * @author $Author: foo $
 * @version $Revision: 1.56 $
 */
public class Worm extends Game {

	private static final long serialVersionUID = 1L;

	private static final boolean TESTSIGNUP = false;

	public static final float MAX_LOUD_ATTENUATION_DISTANCE = 1280.0f;
	public static final float MAX_ATTENUATION_DISTANCE = 720.f;
	public static final float MIN_ATTENUATION_GAIN = 0.01f;

	private static final int FAST_FORWARD_SPEED = 9;

	/** Singleton */
	private static Worm instance;

	/** Game state mode for when we create a new {@link WormGameState} */
	private static int newMode;

	/** Game state (shadows Game.gameState) */
	private static WormGameState gameState;

	/** Sound attenuators */
	public static final Attenuator ATTENUATOR = new Attenuator() {
		@Override
		public float getVolume(float x, float y) {
			double ddx = -GameScreen.getSpriteOffset().getX() + Game.getWidth() / 2.0f - x;
			double ddy = -GameScreen.getSpriteOffset().getY() + Game.getHeight() / 2.0f  - y;
			double dist = Math.sqrt(ddx * ddx + ddy * ddy);
			return LinearInterpolator.instance.interpolate(1.0f, MIN_ATTENUATION_GAIN, (float) dist / MAX_ATTENUATION_DISTANCE);
		}
	};
	public static final Attenuator LOUD_ATTENUATOR = new Attenuator() {
		@Override
		public float getVolume(float x, float y) {
			double ddx = -GameScreen.getSpriteOffset().getX() + Game.getWidth() / 2.0f - x;
			double ddy = -GameScreen.getSpriteOffset().getY() + Game.getHeight() / 2.0f  - y;
			double dist = Math.sqrt(ddx * ddx + ddy * ddy);
			return LinearInterpolator.instance.interpolate(1.0f, MIN_ATTENUATION_GAIN, (float) dist / MAX_LOUD_ATTENUATION_DISTANCE);
		}
	};
	private static final AttenuatorFeature ATTENUATOR_FEATURE = new AttenuatorFeature("default.attenuator") {
		@Override
		public float getVolume(float x, float y) {
			return Game.getSFXVolume() * ATTENUATOR.getVolume(x, y);
		}
	};
	private static final AttenuatorFeature LOUD_ATTENUATOR_FEATURE = new AttenuatorFeature("loud.attenuator") {
		@Override
		public float getVolume(float x, float y) {
			return Game.getSFXVolume() * LOUD_ATTENUATOR.getVolume(x, y);
		}
	};
	private static final AttenuatorFeature DEFAULT_ATTENUATOR_FEATURE = new AttenuatorFeature("none.attenuator") {
		@Override
		public float getVolume(float x, float y) {
			return Game.getSFXVolume();
		}
	};

	static {
		Resources.put(ATTENUATOR_FEATURE);
		Resources.put(LOUD_ATTENUATOR_FEATURE);
		Resources.put(DEFAULT_ATTENUATOR_FEATURE);
	}

	private static final SpriteAllocator MOUSE_SPRITE_ALLOCATOR = new SpriteAllocator() {
		@Override
		public Sprite allocateSprite(Serializable owner) {
			return instance.mouseSpriteEngine.allocate(owner);
		}
	};

	/** Max mouse speed */
	public static final int MOUSE_MAX_SCALE = 10;

	/** Mouse speed multiplier */
	public static final int MOUSE_SPEED_MULTIPLIER = 3;

	/** Mouse speed */
	private static int mouseSpeed = MOUSE_MAX_SCALE / MOUSE_SPEED_MULTIPLIER;

	/** Global mouse pointers rendered using this very small sprite engine */
	private transient SpriteEngine mouseSpriteEngine;

	/** Mouse pointer */
	private transient SimpleThingWithLayers mouseLayers;

	/** Current scaled mouse coordinates */
	private transient float mouseDX, mouseDY, mouseX, mouseY;


	/*
	 * Options
	 */

	private static boolean showTooltips, showInfo, showHints, autoDifficulty;



	/**
	 * C'tor
	 */
	public Worm(String name) {
		super(name);
	}

	public static void setMouseAppearance(LayersFeature newMouseAppearance) {
		instance.doSetMouseAppearance(newMouseAppearance);
	}
	private void doSetMouseAppearance(LayersFeature newMouseAppearance) {
		mouseLayers.requestSetAppearance(newMouseAppearance);
		updateMouse();
	}

	private void updateMouse() {
		if (mouseLayers.getSprites() != null) {
			float lx = physicalXtoLogicalX(getMouseX());
			float ly = physicalYtoLogicalY(getMouseY());
			for (int i = 0; i < mouseLayers.getSprites().length; i++) {
				mouseLayers.getSprite(i).setLocation(lx, ly, 0.0f);
			}
		}
		mouseSpriteEngine.tick();
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.Game#setGameState(net.puppygames.applet.GameState)
	 */
	@Override
	protected void setGameState(GameState newGameState) {
		super.setGameState(newGameState);

		Worm.gameState = (WormGameState) newGameState;
	}

	/**
	 * @return the game state
	 */
	public static WormGameState getGameState() {
		return gameState;
	}

	@Override
	protected void doGameOver() {
		gameState.onGameOver();
		GameScreen.gameOver();
	}

	@Override
	protected void doShowOptions() {
		net.puppygames.applet.screens.OptionsScreen.show();
	}

	@Override
	protected void doShowHelp() {
		Sys.openURL("http://www.puppygames.net/revenge-of-the-titans/help");
	}

	@Override
	protected void doCreate() {
		setInstance(this);

		super.doCreate();

		Sprite.setMissingImage((SpriteImage) Resources.get("spriteimage.missing"));

		mouseSpriteEngine = new StaticSpriteEngine(false, 0, false, 1);
		mouseSpriteEngine.create();
		mouseLayers = new SimpleThingWithLayers(MOUSE_SPRITE_ALLOCATOR);

		setMouseAppearance(Res.getMousePointer());
		mouseSpeed = getPreferences().getInt("mouseSpeed", mouseSpeed);

		prefsSaver = new PrefsSaverThread();
		prefsSaver.start();

		Particle.setMaxParticles(4096);
		SoundCommand.setDefaultAttenuator(DEFAULT_ATTENUATOR_FEATURE);
	}

	public static void setInstance(Worm instance) {
		Worm.instance = instance;
	}


	@Override
	protected void postRender() {
		mouseSpriteEngine.render();
	}

	@Override
	protected void doEndGame() {
		if (Game.isDemoExpired()) {
			NagScreen.show("You know you want to!", true);
		} else {
			net.puppygames.applet.screens.TitleScreen.show();
		}
	}

	@Override
	protected void doGameTick() {
		int newMouseDX = Mouse.getDX() * MOUSE_SPEED_MULTIPLIER * getMouseSpeed();
		int newMouseDY = Mouse.getDY() * MOUSE_SPEED_MULTIPLIER * getMouseSpeed();
		float newMouseX = mouseX + newMouseDX;
		float newMouseY = mouseY + newMouseDY;
		Rectangle viewPort = getViewPort();
		int minX = viewPort.getX() * Worm.MOUSE_MAX_SCALE;
		int maxX = (viewPort.getWidth() + viewPort.getX()) * Worm.MOUSE_MAX_SCALE  - 1;
		mouseX = Math.max(minX, Math.min(maxX, newMouseX));
		int minY = viewPort.getY() * Worm.MOUSE_MAX_SCALE;
		int maxY = (viewPort.getHeight() + viewPort.getY()) * Worm.MOUSE_MAX_SCALE - 1;
		mouseY = Math.max(minY, Math.min(maxY, newMouseY));
		mouseDX = 0.0f;
		mouseDY = 0.0f;
		if (!isCatchUp()) {
			if (newMouseX < minX) {
				mouseDX = (newMouseX - minX) * getWidth() / (Worm.MOUSE_MAX_SCALE * viewPort.getWidth());
			} else if (newMouseX >= maxX) {
				mouseDX = (newMouseX - maxX) * getWidth() / (Worm.MOUSE_MAX_SCALE * viewPort.getWidth());
			}
			if (newMouseY < minY) {
				mouseDY = (newMouseY - minY) * getHeight() / (Worm.MOUSE_MAX_SCALE * viewPort.getHeight());
			} else if (newMouseY >= maxY) {
				mouseDY = (newMouseY - maxY) * getHeight() / (Worm.MOUSE_MAX_SCALE * viewPort.getHeight());
			}
		}
		updateMouse();

		GameScreen gameScreen = GameScreen.getInstance();
		if (gameScreen.isFastForward()) {
			for (int i = 0; i < FAST_FORWARD_SPEED; i ++) {
				gameScreen.tick();
			}
			gameScreen.clearFastForward();
		}

		if (DEBUG) {
			if (wasKeyPressed(Keyboard.KEY_F2)) {
				//setSize(Util.random(640, 960), 640);
				//setSize(1138,640); //16:9 widescreen
				setSize(569,320); //16:9 widescreen
			}

			if (wasKeyPressed(Keyboard.KEY_F3)) {
				setSize(640,Util.random(640, 960) );
			}

			if (wasKeyPressed(Keyboard.KEY_F4)) {
				setSize(640,640);
			}

			if (wasKeyPressed(Keyboard.KEY_F5)) {
				setSize(816, 640);
			}

		}
	}

	@Override
	protected int doGetMouseX() {
		return (int) (mouseX / Worm.MOUSE_MAX_SCALE);
	}

	@Override
	protected int doGetMouseY() {
		return (int) (mouseY / Worm.MOUSE_MAX_SCALE);
	}

	public static float getMouseDX() {
		return instance.mouseDX;
	}

	public static float getMouseDY() {
		return instance.mouseDY;
	}

	/**
	 * @return the mouse speed (0..{@link #MOUSE_MAX_SCALE})
	 */
	public static int getMouseSpeed() {
		return mouseSpeed;
	}

	/**
	 * @param newSpeed
	 */
	public static void setMouseSpeed(int newSpeed) {
		mouseSpeed = newSpeed;
		getPreferences().putInt("mouseSpeed", newSpeed);
		flushPrefs();
	}

	/**
	 * @return the number of worlds unlocked the current slot has, past Earth
	 */
	public static int getMaxWorld() {
		return getPlayerSlot().getPreferences().getInt("maxlevel_"+WormGameState.GAME_MODE_CAMPAIGN, 0) / WormGameState.LEVELS_IN_WORLD;
	}

	public static int getMaxLevelUnlockedInWorld(int world) {
		if (world < getMaxWorld()) {
			return WormGameState.LEVELS_IN_WORLD - 1;
		} else {
			return getMaxLevel(WormGameState.GAME_MODE_CAMPAIGN) % WormGameState.LEVELS_IN_WORLD;
		}
	}

	public static void setMaxLevel(int level, int gameMode) {
		getPlayerSlot().getPreferences().putInt("maxlevel_"+gameMode, level);
		flushPrefs();
	}

	public static int getMaxLevel(int gameMode) {
		return getMaxLevel(getPlayerSlot(), gameMode);
	}

	public static int getMaxLevel(PlayerSlot slot, int gameMode) {
		return slot.getPreferences().getInt("maxlevel_"+gameMode, 0);
	}

	/**
	 * Sets some extra level data
	 * @param level
	 * @param gameMode TODO
	 * @param key
	 * @param value
	 */
	public static void setExtraLevelData(int level, int gameMode, String key, String value) {
		getPlayerSlot().getPreferences().put(key+"."+gameMode+"."+level, value);
		flushPrefs();
	}

	/**
	 * Sets some extra level data
	 * @param level
	 * @param gameMode TODO
	 * @param key
	 * @param value
	 */
	public static void setExtraLevelData(int level, int gameMode, String key, int value) {
		getPlayerSlot().getPreferences().putInt(key+"."+gameMode+"."+level, value);
		flushPrefs();
	}

	/**
	 * Sets some extra level data
	 * @param level
	 * @param gameMode TODO
	 * @param key
	 * @param value
	 */
	public static void setExtraLevelData(int level, int gameMode, String key, float value) {
		getPlayerSlot().getPreferences().putFloat(key+"."+gameMode+"."+level, value);
		flushPrefs();
	}

	/**
	 * Sets some extra level data
	 * @param level
	 * @param gameMode TODO
	 * @param key
	 * @param value
	 */
	public static void setExtraLevelData(int level, int gameMode, String key, long value) {
		getPlayerSlot().getPreferences().putLong(key+"."+gameMode+"."+level, value);
		flushPrefs();
	}

	/**
	 * Gets extra level data
	 * @param slot TODO
	 * @param level
	 * @param gameMode TODO
	 * @param key
	 * @param default_
	 * @return
	 */
	public static String getExtraLevelData(PlayerSlot slot, int level, int gameMode, String key, String default_) {
		return slot.getPreferences().get(key+"."+gameMode+"."+level, default_);
	}

	/**
	 * Gets extra level data
	 * @param slot TODO
	 * @param level
	 * @param gameMode TODO
	 * @param key
	 * @param default_
	 * @return
	 */
	public static float getExtraLevelData(PlayerSlot slot, int level, int gameMode, String key, float default_) {
		return slot.getPreferences().getFloat(key+"."+gameMode+"."+level, default_);
	}

	/**
	 * Gets extra level data
	 * @param slot TODO
	 * @param level
	 * @param gameMode TODO
	 * @param key
	 * @param default_
	 * @return
	 */
	public static long getExtraLevelData(PlayerSlot slot, int level, int gameMode, String key, long default_) {
		return slot.getPreferences().getLong(key+"."+gameMode+"."+level, default_);
	}

	public static int getExtraLevelData(PlayerSlot slot, int level, int gameMode, String key, int default_) {
		return slot.getPreferences().getInt(key+"."+gameMode+"."+level, default_);
	}

	/**
	 * Utility to calculate gain
	 */
	public static float calcGain(float x, float y) {
		return ATTENUATOR.getVolume(x, y);
	}

	/**
	 * Utility to calculate gain for loud noises
	 */
	public static float calcLoudGain(float x, float y) {
		return LOUD_ATTENUATOR.getVolume(x, y);
	}

	@Override
	protected String getSaveGameRegistryMagicLocation() {
		return "tox"+gameState.getGameMode();
	}

	@Override
	protected void onGameSaved() {
		LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getBigFont(), "GAME SAVED", new MappedColor("titles.colormap:text-bold"), new MappedColor("titles.colormap:text-dark"), 180, 60);
		le.setLayer(100);
		le.setLocation(Game.getWidth() / 2, Game.getHeight() / 2);
		le.setSound((ALBuffer) Resources.get("gamesaved.buffer"));
	 	le.spawn(net.puppygames.applet.screens.TitleScreen.getInstance());
	}

	@Override
	protected void doRequestExit() {
		final DialogScreen reallyDialog = (DialogScreen) Resources.get("yescancel.dialog");
		reallyDialog.doModal("EXIT GAME", "REALLY EXIT THE GAME?", new Runnable() {
			@Override
			public void run() {
				if (reallyDialog.getOption() == DialogScreen.OK_OPTION) {
					Game.exit();
				}
			}
		});
	}

	@Override
	protected boolean doIsDemoExpired() {
		// Never expire the demo
		return false;
	}

	@Override
	protected boolean doMaybeShowHelp() {
		// Don't show help
		return false;
	}

	/**
	 * @return autodifficulty setting
	 */
	public static boolean getAutoDifficulty() {
		return autoDifficulty;
	}

	/**
	 * @return the showHints
	 */
	public static boolean getShowHints() {
		return showHints;
	}

	/**
	 * @return the showInfo
	 */
	public static boolean getShowInfo() {
		return showInfo;
	}

	/**
	 * @return the showTooltips
	 */
	public static boolean getShowTooltips() {
		return showTooltips;
	}

	/**
	 * @param showHints the showHints to set
	 */
	public static void setShowHints(boolean showHints) {
		Worm.showHints = showHints;
		saveSlotOptions();
	}

	/**
	 * @param showInfo the showInfo to set
	 */
	public static void setShowInfo(boolean showInfo) {
		Worm.showInfo = showInfo;
		saveSlotOptions();
	}

	/**
	 * @param showTooltips the showTooltips to set
	 */
	public static void setShowTooltips(boolean showTooltips) {
		Worm.showTooltips = showTooltips;
		saveSlotOptions();
	}

	public static void setAutoDifficulty(boolean autoDifficulty) {
	    Worm.autoDifficulty = autoDifficulty;
	    saveSlotOptions();
    }

	private static void saveSlotOptions() {
		getPlayerSlot().getPreferences().putBoolean("showHints", showHints);
		getPlayerSlot().getPreferences().putBoolean("showTooltips", showTooltips);
		getPlayerSlot().getPreferences().putBoolean("showInfo", showInfo);
		getPlayerSlot().getPreferences().putBoolean("autoDifficulty", autoDifficulty);
		flushPrefs();
	}

	private static void loadSlotOptions() {
		showHints = getPlayerSlot().getPreferences().getBoolean("showHints", true);
		showTooltips = getPlayerSlot().getPreferences().getBoolean("showTooltips", true);
		showInfo = getPlayerSlot().getPreferences().getBoolean("showInfo", true);
		autoDifficulty = getPlayerSlot().getPreferences().getBoolean("autoDifficulty", true);
	}

	@Override
	protected void onSetPlayerSlot() {
		loadSlotOptions();
	}

	@Override
	protected void onBeginNewGame() {
		// Open the game mode choice dialog
		ChooseGameModeScreen.show();
	}

	/**
	 * Choose a random valid prize
	 * @return a {@link PrizeFeature}, or null
	 */
	private PrizeFeature choosePrize() {
		List<PrizeFeature> prizes = new ArrayList<PrizeFeature>(PrizeFeature.getPrizes());
		Collections.shuffle(prizes);
		for (PrizeFeature pf : prizes) {
			if (pf.isValid()) {
				return pf;
			}
		}
		return null;
	}

	public static NagState getNagState() {
		return NagState.valueOf(getPreferences().get("nagstate", NagState.NOT_YET_SHOWN.name()));
	}

	public static void setNagState(NagState newNagState) {
		getPreferences().put("nagstate", newNagState.name());
		flushPrefs();
	}

	@SuppressWarnings("unused")
	@Override
	protected void onPreRegisteredStartup() {
		if (getPlayerSlot() == null) {
			showTitleScreen();
			return;
		}
		NagState nagState = getNagState();
		if (TESTSIGNUP && DEBUG) {
			nagState = NagState.NOT_YET_SHOWN;
		}
		switch (nagState) {
			case NOT_YET_SHOWN:
				PrizeFeature prize = choosePrize();
				if (prize != null) {
					SignUpScreen.show(prize);
				} else {
					showTitleScreen();
				}
				break;
			case PRIZE_AWAITS:
				// Restore the incentive file
				FileInputStream fis = null;
				BufferedInputStream bis = null;
				ObjectInputStream ois = null;
				try {
					fis = new FileInputStream(getIncentiveFile());
					bis = new BufferedInputStream(fis);
					ois = new ObjectInputStream(bis);
					NewsletterIncentive ni = (NewsletterIncentive) ois.readObject();
					if (!ni.validate()) {
						throw new Exception("Existing incentive file is invalid.");
					}
					UnlockBonusScreen.show(ni);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					showTitleScreen();
				} finally {
					if (fis != null) {
						try {
							fis.close();
						} catch (IOException e) {
						}
					}
				}
				break;
			case DONT_NAG:
			case REDEEMED:
				showTitleScreen();
				break;
			default:
				assert false : "Unknown nag state "+nagState;
		}
	}

	public static File getIncentiveFile() {
		return new File(Game.getDirectoryPrefix() + "incentive.dat");
	}

	@Override
	protected File getRestoreFile() {
		return new File(getPlayerDirectoryPrefix() + "savedGame_"+newMode+".dat");
	}

	/**
	 * Start a new game from scratch
	 * @param mode A game mode (0, 1, 2)
	 */
	public static void newGame(int mode) {
		newMode = mode;
		// Is there a saved game file?
		if (isRestoreAvailable()) {
			restoreGame();
		} else {
			cleanGame();
		}
	}

	@Override
	protected GameState createGameState() {
		return new WormGameState(newMode);
	}

	/**
	 * Reset game state (used by survival level select)
	 */
	public static void resetGameState() {
		instance.setGameState(instance.createGameState());
	}

	@Override
	protected void onRestoreGameFailed(Exception e) {
		// Clean up the GameScreen
		GameScreen.getInstance().panic();
	}
}
