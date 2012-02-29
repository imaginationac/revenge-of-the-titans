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

import java.io.Serializable;
import java.util.Calendar;

import net.puppygames.applet.Game;
import net.puppygames.applet.GameState;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.PlayerSlot;
import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.LabelEffect;
import net.puppygames.applet.effects.Particle;
import net.puppygames.applet.screens.DialogScreen;
import net.puppygames.applet.screens.NagScreen;
import net.puppygames.gamecommerce.shared.RegistrationDetails;
import net.puppygames.steam.NotificationPosition;
import net.puppygames.steam.Steam;
import net.puppygames.steam.SteamException;

import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;

import worm.animation.SimpleThingWithLayers;
import worm.features.LayersFeature;
import worm.screens.ChooseGameModeScreen;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Attenuator;
import com.shavenpuppy.jglib.resources.AttenuatorFeature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.sprites.SoundCommand;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;
import com.shavenpuppy.jglib.sprites.SpriteEngine;
import com.shavenpuppy.jglib.sprites.SpriteImage;
import com.shavenpuppy.jglib.sprites.StaticSpriteEngine;

/**
 * Revenge of the Titans game
 */
public class Worm extends MiniGame {

	private static final long serialVersionUID = 1L;

	private static final boolean TEST_XMAS = true; // Only honoured if DEBUG is also true
	private static final boolean FORCE_XMAS = true; // Force Xmas to this value when TEST_XMAS is true

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
			Point spriteOffset = GameScreen.getSpriteOffset();
			double ddx = -spriteOffset.getX() + Game.getWidth() / 2.0f - x;
			double ddy = -spriteOffset.getY() + Game.getHeight() / 2.0f  - y;
			double dist = Math.sqrt(ddx * ddx + ddy * ddy);
			return LinearInterpolator.instance.interpolate(1.0f, MIN_ATTENUATION_GAIN, (float) dist / MAX_ATTENUATION_DISTANCE);
		}
	};
	public static final Attenuator LOUD_ATTENUATOR = new Attenuator() {
		@Override
		public float getVolume(float x, float y) {
			Point spriteOffset = GameScreen.getSpriteOffset();
			double ddx = -spriteOffset.getX() + Game.getWidth() / 2.0f - x;
			double ddy = -spriteOffset.getY() + Game.getHeight() / 2.0f  - y;
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

	private static boolean xmas;

	static {
		Resources.put(ATTENUATOR_FEATURE);
		Resources.put(LOUD_ATTENUATOR_FEATURE);
		Resources.put(DEFAULT_ATTENUATOR_FEATURE);

		Calendar c = Calendar.getInstance();
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		if ((month == Calendar.DECEMBER && day >= 19) || (month == Calendar.JANUARY && day < 9) || (TEST_XMAS && DEBUG)) {
			xmas = TEST_XMAS && DEBUG ? FORCE_XMAS : true;
		}
	}

	private static final SpriteAllocator MOUSE_SPRITE_ALLOCATOR = new SpriteAllocator() {
        private static final long serialVersionUID = 1L;

		@Override
		public Sprite allocateSprite(Serializable owner) {
			return instance.mouseSpriteEngine.allocateSprite(owner);
		}
	};

	/** Global mouse pointers rendered using this very small sprite engine */
	private transient SpriteEngine mouseSpriteEngine;

	/** Mouse pointer */
	private transient SimpleThingWithLayers mouseLayers;

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
		boolean insideWindow = Mouse.isInsideWindow() && !Game.isPaused();
		if (mouseLayers.getSprites() != null) {
			float lx = physicalXtoLogicalX(getMouseX());
			float ly = physicalYtoLogicalY(getMouseY());
			for (int i = 0; i < mouseLayers.getSprites().length; i++) {
				mouseLayers.getSprite(i).setLocation(lx, ly);
				mouseLayers.getSprite(i).setVisible(insideWindow);
			}
		}
		mouseSpriteEngine.tick();
	}

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
		Sys.openURL("http://www.puppygames.net/revenge-of-the-titans/help/" + getLanguage());
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

		Particle.setMaxParticles(4096);
		SoundCommand.setDefaultAttenuator(DEFAULT_ATTENUATOR_FEATURE);

		if (isUsingSteam() && Steam.isCreated() && Steam.isSteamRunning()) {
			Steam.getUtils().setOverlayNotificationPosition(NotificationPosition.TopRight);
			try {
			Steam.getUserStats().requestCurrentStats();
			} catch (SteamException e) {
				System.err.println("Failed to request user stats due to "+e);
			}
		}
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
		if (MiniGame.isDemoExpired()) {
			NagScreen.show("You know you want to!", true);
		} else {
			net.puppygames.applet.screens.TitleScreen.show();
		}
	}

	@Override
	protected void doTick() {
		updateMouse();

		GameScreen gameScreen = GameScreen.getInstance();
		if (gameScreen.isFastForward()) {
			for (int i = 0; i < FAST_FORWARD_SPEED; i ++) {
				gameScreen.tick();
			}
			gameScreen.clearFastForward();
		}
	}

	@Override
	protected void onPaused() {
		updateMouse();
		Screen.tickAllScreens();
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
		LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getBigFont(), getMessage("ultraworm.worm.game_saved"), new MappedColor("titles.colormap:text-bold"), new MappedColor("titles.colormap:text-dark"), 180, 60);
		le.setLayer(100);
		le.setLocation(Game.getWidth() / 2, Game.getHeight() / 2);
		le.setSound((ALBuffer) Resources.get("gamesaved.buffer"));
	 	le.spawn(net.puppygames.applet.screens.TitleScreen.getInstance());
	}

	@Override
	protected void doRequestExit() {
		final DialogScreen reallyDialog = (DialogScreen) Resources.get("yescancel.dialog");
		reallyDialog.doModal(getMessage("ultraworm.worm.exit_game"), getMessage("ultraworm.worm.exit_question"), new Runnable() {
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
		if (xmas) {
			((ChooseGameModeScreen)Resources.get("choose-game-mode-xmas.screen")).open();
		} else {
			((ChooseGameModeScreen)Resources.get("choose-game-mode.screen")).open();
		}
	}

	@Override
	protected void onPreRegisteredStartup() {
		if (getPlayerSlot() == null) {
			MiniGame.showTitleScreen();
			return;
		}
		super.onPreRegisteredStartup();
	}

	@Override
	protected String getRestoreFile() {
		return getPlayerDirectoryPrefix() + "savedGame_"+newMode+".dat";
	}

	/**
	 * Start a new game from scratch
	 * @param mode A game mode (0, 1, 2)
	 */
	public static void newGame(int mode) {
		newMode = mode;
		// Is there a saved game file?
		if (MiniGame.isRestoreAvailable()) {
			MiniGame.restoreGame();
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

	/**
	 * Registration check for allowing Sandbox mode.
	 */
	public static boolean isSandboxRegistered() {
		RegistrationDetails sandboxRegistrationDetails = null;
		try {
			sandboxRegistrationDetails = RegistrationDetails.checkRegistration("Sandbox Mode");
		} catch (Exception e) {
		}
		return (sandboxRegistrationDetails!=null ? true : false);
	}

	/**
	 * @return true if it's Christmas!
	 */
	public static boolean isXmas() {
		return xmas;
	}
}
