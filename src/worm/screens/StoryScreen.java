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
package worm.screens;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.FadeEffect;

import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;

import worm.GameMap;
import worm.Res;
import worm.Worm;
import worm.WormGameState;
import worm.features.Setting;
import worm.features.StoryFeature;
import worm.generator.MapGenerator;
import worm.generator.MapGeneratorParams;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.ColorMapFeature;
import com.shavenpuppy.jglib.resources.Data;

/**
 * Shows story stuff
 */
public class StoryScreen extends Screen {

	private static final String ID_OK = "ok";
	private static final String ID_BACK = "back";
	private static final String ID_PROGRESS = "progress";
	private static final String ID_MENU = "menu";

	@Data
	private String progressBar;
	private int progressBarLayer;

	/** The setting effect. There's just one */
	private transient Setting settingEffect;

	private transient Area progressArea;

	/** Progress bar definition */
	private transient Background progressBarFeature;

	/** Sprite to render the progress bar */
	private transient TickableObject tickableObject;

	/** Progress bar */
	private transient Background.Instance progressBarInstance;

	/** The map we're generating - will be null until it's ready */
	private transient GameMap map;

	/** Are we ready to enable the ok button? */
	private transient boolean ready;

	/** What to do when the screen goes "back" */
	private transient Runnable backAction;

	/** What to do when the screen goes "next" */
	private transient Runnable nextAction;

	/** Whether to generate a map */
	private transient boolean generate;

	/** Story to show */
	private transient StoryFeature story;

	/** The level generator thread */
	private class LevelGeneratorThread extends Thread {

		MapGenerator generator;

		/**
		 * C'tor
		 */
		public LevelGeneratorThread() {
			super("Level Generator");
			setPriority(Thread.NORM_PRIORITY - 1);
			setDaemon(true);
		}

		void finish() {
			if (generator != null) {
				generator.finish();
			}
		}

		@Override
		public void run() {
			WormGameState gameState = Worm.getGameState();
			gameState.calcBasicDifficulty();

			MapGeneratorParams params = new MapGeneratorParams
				(
					gameState.getBasicDifficulty(),
					gameState.getGameMode(),
					gameState.getLevel(),
					gameState.getLevelFeature(),
					gameState.getLevelInWorld(),
					gameState.getMoney(),
					gameState.getResearchHash(),
					gameState.getWorld()
				);

			if (gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL) {
				if (gameState.getSurvivalParams().getGenerateNew()) {
					generator = gameState.getLevelFeature().getTemplate().createGenerator(params);
				} else {
					map = gameState.getMap();
					return;
				}
			} else if (gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) {
				if (gameState.isXmasReset()) {
					generator = gameState.getLevelFeature().getTemplate().createGenerator(params);
				} else {
					map = gameState.getMap();
					return;
				}
			} else {
				generator = gameState.getLevelFeature().getTemplate().createGenerator(params);
			}
			map = generator.generate();
			gameState.setMap(map);
		}

		float getProgress() {
			if (generator == null) {
				return 0.0f;
			} else {
				return generator.getProgress();
			}
		}
	}

	private transient LevelGeneratorThread thread;

	/**
	 * C'tor
	 */
	public StoryScreen(String name) {
		super(name);
		setAutoCreated();
	}

	public static void tidyUp(String name) {
		StoryScreen ss = (StoryScreen) Resources.get(name);
		ss.backAction = null;
		ss.nextAction = null;
	}

	public static void show(String name, boolean generate, StoryFeature story, Runnable backAction, Runnable nextAction) {
		if (nextAction == null) {
			throw new IllegalArgumentException("nextAction cannot be null");
		}
		if (story == null || story.getSetting() == null) {
			nextAction.run();
		} else {
			StoryScreen ss = (StoryScreen) Resources.get(name);
			ss.doShow(generate, story, backAction, nextAction);
		}
	}

	public void doShow(boolean generate, StoryFeature story, Runnable backAction, Runnable nextAction) {
		this.generate = generate;
		this.backAction = backAction;
		this.nextAction = nextAction;
		this.story = story;
		open();
	}

	@Override
	protected void onClicked(String id) {
		if (ID_OK.equals(id)) {
			close();
			nextAction.run();
			map = null;
		} else if (ID_BACK.equals(id)) {
			close();
			backAction.run();
			map = null;
		} else if (id.equals(ID_MENU)) {
			switch (Worm.getGameState().getGameMode()) {
				case WormGameState.GAME_MODE_SURVIVAL:
					SurvivalMenuScreen.show(SurvivalMenuScreen.MENU_STORY_MODE);
					break;
				case WormGameState.GAME_MODE_XMAS:
					net.puppygames.applet.screens.TitleScreen.show();
					break;
				default:
					MenuScreen.show(MenuScreen.MENU_STORY_MODE);
			}
		}
	}

	@Override
	protected void onResized() {
		if (settingEffect != null) {
			settingEffect.onResized();
		}
	}

	@Override
	protected void onBlocked() {
		setPaused(true);
	}

	@Override
	protected void onUnblocked() {
		setPaused(false);
	}

	@Override
	protected void doTick() {
		ReadableRectangle b = progressArea.getBounds();
		if (map != null) {
			if (!ready) {
				setEnabled(ID_OK, true);
				setEnabled(ID_BACK, backAction != null);
				setVisible(ID_BACK, backAction != null);
				progressBarInstance.setBounds(b);
				new FadeEffect(0, 60) {
					@Override
                    protected void onTicked() {
						progressBarInstance.setAlpha(getAlpha());
						getArea(ID_PROGRESS).setAlpha(getAlpha());
					}
				}.spawn(this);
				ready = true;
			}
		} else {
			// Update the progress bar...
			if (progressBarInstance != null) {
				progressBarInstance.setBounds(new Rectangle(b.getX(), b.getY(), (int) (b.getWidth() * thread.getProgress()), b.getHeight()));
			}
		}
	}

	@Override
	protected void doCleanup() {
		if (settingEffect != null) {
			settingEffect.remove();
			settingEffect = null;
		}
		if (thread != null) {
			thread.finish();
			thread = null;
		}
		if (tickableObject != null) {
			tickableObject.remove();
			tickableObject = null;
		}
		progressBarInstance = null;
	}

	@Override
	protected void onOpen() {


		WormGameState gameState = Worm.getGameState();
		switch (gameState.getGameMode()) {
			case WormGameState.GAME_MODE_ENDLESS:
				ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get("earth.colormap"));
				break;
			case WormGameState.GAME_MODE_XMAS:
				ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get("xmas.colormap"));
				break;
			default:
				String world = gameState.getWorld().getUntranslated();
				ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get(world+".colormap"));
		}

		settingEffect = story.getSetting().spawn(this, story);
		settingEffect.onResized();

		Game.playMusic(gameState.getWorld().getStream(), 180, 0.5f);

		if (!generate) {
			setEnabled(ID_OK, true);
			setVisible(ID_BACK, backAction != null);
			setVisible(ID_PROGRESS, false);
			getArea(ID_PROGRESS).setAlpha(0);
			ready = true;
		} else {
			ready = false;
			setEnabled(ID_OK, false);
			setVisible(ID_BACK, false);
			setVisible(ID_PROGRESS, true);
			getArea(ID_PROGRESS).setAlpha(255);

			// Start the level generator thread
			thread = new LevelGeneratorThread();
			thread.start();

			progressBarInstance = progressBarFeature.spawn();
			progressBarInstance.setBounds(progressArea.getBounds());
			tickableObject = new TickableObject() {
				@Override
				protected void render() {
					progressBarInstance.render(this);
				}
			};
			tickableObject.setLayer(progressBarLayer);
			tickableObject.spawn(this);
		}

		Worm.setMouseAppearance(Res.getMousePointer());
	}

	@Override
	protected void doCreateScreen() {
		progressArea = getArea(ID_PROGRESS);
	}
}
