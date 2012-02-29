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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Screen;
import net.puppygames.applet.TickableObject;
import net.puppygames.applet.effects.AdjusterEffect;
import net.puppygames.applet.effects.Effect;
import net.puppygames.applet.effects.FadeEffect;
import net.puppygames.applet.effects.LabelEffect;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Color;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;

import worm.AttenuatedColor;
import worm.Hints;
import worm.Layers;
import worm.MapRenderer;
import worm.Res;
import worm.SFX;
import worm.ShopItem;
import worm.TimeUtil;
import worm.Worm;
import worm.WormGameState;
import worm.animation.SimpleThingWithLayers;
import worm.buildings.BuildingFeature;
import worm.entities.Building;
import worm.entities.Gidrah;
import worm.features.HintFeature;
import worm.features.LayersFeature;
import worm.features.LevelColorsFeature;
import worm.features.LevelFeature;
import worm.powerups.PowerupFeature;
import worm.powerups.RepairPowerupFeature;
import worm.powerups.ShieldPowerupFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.ColorInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.openal.ALStream;
import com.shavenpuppy.jglib.opengl.ColorUtil;
import com.shavenpuppy.jglib.opengl.GLRenderable;
import com.shavenpuppy.jglib.opengl.GLTextArea;
import com.shavenpuppy.jglib.resources.ColorMapFeature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.resources.TextResource;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.Util;

import static org.lwjgl.opengl.GL11.*;



/**
 * $Id: GameScreen.java,v 1.155 2010/11/06 18:56:33 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.155 $
 */
public class GameScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static final boolean ALLOW_SCROLL_PAST = true;
	private static final GLRenderable SETUP_RENDERING = new GLRenderable() {
		@Override
		public void render() {
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
			glDisable(GL_TEXTURE_2D);
		}
	};

	private transient boolean debug = false;

	private static final int HINT_SCALE = FPMath.fpValue(0.33f);
	private static final int SAVE_DURATION = 45;
	private static final int ZOOM_SPEED = 32;
	private static final int SCROLL_SPEED = 8;
//	private static final int MAX_MOUSE_SCROLL_SPEED = 32;

	/** How long the shop lurks around for after it's not needed */
	private static final int SHOP_LURK = 60;

	/** Ticks before full shop info appears */
	private static final int SHOP_SENSITIVITY = 60;

	/** Shop fade in rate */
	private static final int SHOP_FADE_RATE = 32;

	/** Hint fade in rate */
	private static final int HINT_FADE_RATE = 96;

	/** Hud fade rate */
	private static final int HUD_ALPHA_FADE_SPEED = 32;

	/** Hint duration */
	private static final int HINT_DURATION = 10 * 60;

	/** Areas */
	private static final String ID_BASE_UNDER_ATTACK = "base_attack_alert";
	private static final String ID_DESTRUCTION_IMMINENT = "base_critical_alert";
	private static final String ID_POWERUP_SHORTCUTS = "id_powerup_quicklaunch_";
	private static final String ID_BUILDING_SHORTCUTS = "build_";
	private static final String HUD = "hud";
	private static final String ID_HIDEABLE_GROUP = "hideable";
	private static final String ID_TOP_HUD_GROUP ="hud_top";
	private static final String ID_MAP = "map";

	private static final String ID_BASE = "shortcut_base_zoom";
	private static final String ID_CRYSTAL = "shortcut_crystal_zoom";
	private static final String ID_CRYSTAL_REMAINING = "shortcut_crystal_remaining";

	private static final String ID_BASE_GROUP = "base_button_group";
	private static final String ID_CRYSTAL_GROUP = "crystal_button_group";
	private static final String ID_CRYSTAL_MESSAGE_GROUP = "crystal_message";

	private static final String ID_INFO_ALERT_GROUP = "info_alert";
	private static final String ID_HUD_SIDES = "hud_sides";
	private static final String ID_BEZERK_TIMER = "hud_bezerk_timer";
	private static final String ID_SHIELD_TIMER = "hud_shield_timer";
	private static final String ID_FREEZE_TIMER = "hud_freeze_timer";
	private static final String ID_BEZERK_TIMER_BAR = "hud_bezerk_timer_bar";
	private static final String ID_SHIELD_TIMER_BAR = "hud_shield_timer_bar";
	private static final String ID_FREEZE_TIMER_BAR = "hud_freeze_timer_bar";

	private static final String ID_HUD_TOP_HIDE = "hud_top_hide";

	private static final String ID_TIMER_BAR = "hud_timer_bar";
	private static final String ID_THREAT_LOW = "shortcut_threat_low";
	private static final String ID_THREAT_HIGH = "shortcut_threat_high";

	private static final String ID_TOOLTIPS = "tooltip";
	private static final String ID_SELL = "shortcut_sell";
	private static final String ID_SHOWHIDE = "showhide";
	private static final String ID_MENU = "shortcut_esc";
	private static final String SHOW_ICON_GROUP = "showicon";

	// Misc
	private static final String COST_GROUP = "cost_group";
	private static final String ID_COST = "cost_info";
	private static final String ID_BUILDING_DONE = "building_done";
	private static final String ID_SELL_DONE = "sell_done";

	private static final String SELL_INFO_GROUP = "sell_info_group";
	private static final String ID_SELL_INFO_DONE = "sell_info_done";

	private static final String ID_HINT_GROUP = "hint";
	private static final String ID_HINT_TEXT = "hintscreen-text";
	private static final String ID_HINT_CLOSE = "hintscreen-close";
	private static final String ID_HINT_SPRITE_LOCATION = "hintscreen-sprite";


	// HUD bits
	private static final String ID_MONEY = "money";
	private static final String ID_LEVEL = "level";

	private static final String ID_FASTFORWARD = "shortcut_fastforward";
	private static final String ID_MISC_SHORTCUTS = "shortcut";

	// chaz hack! build cost info positions - should get from bounds of some areas, but this'll do */
	private static final int COST_INFO_Y_HIGH = 46;
	private static final int COST_INFO_Y_LOW = 1;
	private static final int COST_INFO_BUTTON_OFFSET = 8;
	/*
	 * Static data
	 */

	private static final String TIME_LEFT_COLOR_BOTTOM = "time-left-color-left";
	private static final String TIME_LEFT_COLOR_TOP = "time-left-color-right";
	private static final String TIME_PAST_COLOR_BOTTOM = "time-past-color-left";
	private static final String TIME_PAST_COLOR_TOP = "time-past-color-right";
	private static final Color TEMPCOLOR = new Color();

	private static final String POWERUP_TIME_LEFT_COLOR_BOTTOM = "gui-bright";
	private static final String POWERUP_TIME_LEFT_COLOR_TOP = "gui-bright";
	private static final String POWERUP_TIME_PAST_COLOR_BOTTOM = "gui-dark";
	private static final String POWERUP_TIME_PAST_COLOR_TOP = "gui-dark";

	private static final String CANT_BUILD_COLOR = "gui-dark";
	private static final String BUILD_COLOR = "gui-bright";

	/*
	 * Shop stuff
	 */

	private static final String ID_BUILDINGS_TITLETEXT = "id_buildings_titletext";
	private static final String ID_BUILDINGS_TITLETEXT_NO_KEY = "id_buildings_titletext_no_key";
	private static final String ID_BUILDINGS_INFOTEXT = "id_buildings_infotext";
	private static final String ID_BUILDINGS_BONUSTEXT = "id_buildings_bonustext";
	private static final String ID_BUILDINGS_COSTTEXT = "id_buildings_costtext";


	private static final String ID_POWERUPS_LABEL = "id_powerups_label";

	private static final String ID_BUILDINGS_INFO_GROUP ="buildings_info";
	private static final String ID_POWERUPS_INFO_GROUP ="powerups_info";
	private static final String ID_INFO_GROUP ="info";

	private static final String ID_BUILDINGS_DATA_GROUP ="buildings_data";
	private static final String ID_POWERUPS_DATA_GROUP ="powerups_data";
	private static final String[] dataGroups = {ID_BUILDINGS_DATA_GROUP, ID_POWERUPS_DATA_GROUP};

	private static final String ID_INFO_SPRITE_LOCATION = "id_info_sprite_location";

	private MappedColor shopItemColor;



	/** Singleton */
	public static GameScreen instance;

	/** Cached colour */
	private static final Color cachedColor = new Color();

	/** Temp rectangle */
	private static final Rectangle tempRect = new Rectangle();

	/** Current game state */
	private static WormGameState gameState;

	/** Sprite offset */
	private static final Point OFFSET = new Point();

	/*
	 * Resource data
	 */

	/*
	 * Transient data
	 */

	/** Map rendering */
	private transient MapRenderer renderer;

	/** Locations of stuff */
	private transient Area moneyArea, levelArea, costArea, costAreaLabel, crystalArea, mapArea, timerBarArea, threatLowArea, threatHighArea, freezeBarArea, bezerkBarArea, shieldBarArea, crystalRemainingArea;

	/** Currently displayed cash amount */
	private transient int displayedCash;

	/** Currently displayed crystals amount */
	private transient int displayedCrystals;

	/** Map coordinates */
	private transient float mapX, mapY;

	/** Shaking */
	private transient int shakeTick;
	private transient boolean shook;

	/** Particle debug */
	private transient GLTextArea particleDebug;

	/** base destruction klaxon */
	private transient SoundEffect baseDestructionEffect;

	/** Base attacked effect */
	private transient FadeEffect baseAttackedEffect;

	/** Base destruction effect */
	private transient FadeEffect baseDestructionImminentEffect;

	/** Crstals unattended */
	private transient FadeEffect unminedCrystalEffect;

	/** Tooltip effect */
	private transient FadeEffect tooltipEffect;

	/** Wet rumble */
	private transient SoundEffect wetRumble;

	/** Dry rumble */
	private transient SoundEffect dryRumble;

	/** Rumble gain */
	private transient float rumbleGain, targetRumbleGain;

	/** Rumble buffers */
	private transient ALBuffer wetBuffer;
	private transient ALBuffer dryBuffer;

	/** Powerup sounds */
	private transient SoundEffect bezerkSoundEffect, shieldSoundEffect, freezeSoundEffect;

	/** Zooming */
	private transient int zoomTick, zoomDuration;
	private transient float startZoomX, startZoomY, endZoomX, endZoomY;

	/** Side HUD alpha */
	private transient int sideHUDAlpha;

	/** Timer bar colors */
	private transient ReadableColor timeLeftColorBottom, timeLeftColorTop, timePastColorBottom, timePastColorTop;

	/** Powerup timer bar colors */
	private transient ReadableColor powerupTimeLeftColorBottom, powerupTimeLeftColorTop, powerupTimePastColorBottom, powerupTimePastColorTop;

	private transient boolean shieldTimerVisible, freezeTimerVisible, bezerkTimerVisible;
	private transient int shieldTimerMax, freezeTimerMax, bezerkTimerMax;

	/** build info colors */
	private transient ReadableColor buildColor;
	private transient ReadableColor cantBuildColor;



	/** Rendery stuff */
	private transient TickableObject tickableObject;

	/** FF processing */
	private transient boolean fastForward;

	/** Info sprite location */
	private transient Area infoSpriteLocationArea;

	private transient SimpleThingWithLayers infoLayers;
	private transient Area titleTextArea;
	private transient Area titleTextNoKeyArea;
	private transient Area infoTextArea;
	private transient Area bonusTextArea;
	private transient Area costTextArea;

	/* Tooltippery shop stuff */
	private transient ShopItem hover = null;
	private transient String currentTooltipID;
	private transient int infoTimer;
	private transient ShopItem infoItem;
	private transient AdjusterEffect shopEffect;

	private transient boolean shopSpritesVisible;

	/* hints stuff */
	private transient AdjusterEffect hintEffect;
	private transient HintFeature currentHint;
	private transient Area hintTextArea;
	private transient SimpleThingWithLayers hintLayers;
	private transient boolean hintSpritesVisible;
	private transient Area hintSpriteLocationArea;

	private transient boolean repairFlagged, shieldFlagged;

	/** Show/hide bottom hud */
	private transient int hudAlpha, hudAlphaTarget;

	/** Show/hide top hud */
	private transient int topHudAlpha, topHudAlphaTarget;

	/** Showing sell stuff */
	private transient boolean sellActive;

	/** hint queue */
	private transient List<HintFeature> hintQueue;

	/** Hint duration ticker */
	private transient int hintTick;

	/**
	 * C'tor
	 */
	public GameScreen(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	protected void doRegister() {
		instance = this;
	}

	@Override
	protected void doDeregister() {
		instance = null;
	}

//	transient boolean buttonWasDown;
//	transient Emitter emitter;
//	transient EmitterDiddler diddler;
//	transient LevelColourDiddler levelColourDiddler;
	public static boolean isDiddlerOpen() {
//		return instance.levelColourDiddler != null && instance.levelColourDiddler.isShowing();
		return false;
	}
//	transient int medals;
	@Override
	protected void doTick() {
		if (Game.DEBUG) {

			if (Game.wasKeyPressed(Keyboard.KEY_D)) {
				debug = !debug;
				renderer.setDebug(debug);
			}

//			if (Game.wasKeyPressed(Keyboard.KEY_F8)) {
//				ArrayList a = new ArrayList(MedalFeature.getMedals().keySet());
//				if (medals == a.size()) {
//					medals = 0;
//				}
//				String m = (String) a.get(medals ++);
//				gameState.awardMedal(m);
//			}
//
//			if (Game.wasKeyPressed(Keyboard.KEY_1)) {
//				Gidrah g = ((GidrahFeature) Resources.get("gidrah.earth.boss")).spawn(this, (getMouseX() - OFFSET.getX()) / MapRenderer.TILE_SIZE, (getMouseY() - OFFSET.getY()) / MapRenderer.TILE_SIZE, 1);
//				g.setHitPoints(1);
//			}
//			if (Game.wasKeyPressed(Keyboard.KEY_2)) {
//				Gidrah g = ((GidrahFeature) Resources.get("gidrah.moon.boss")).spawn(this, (getMouseX() - OFFSET.getX()) / MapRenderer.TILE_SIZE, (getMouseY() - OFFSET.getY()) / MapRenderer.TILE_SIZE, 1);
//				g.setHitPoints(1);
//			}
//			if (Game.wasKeyPressed(Keyboard.KEY_3)) {
//				Gidrah g = ((GidrahFeature) Resources.get("gidrah.mars.boss")).spawn(this, (getMouseX() - OFFSET.getX()) / MapRenderer.TILE_SIZE, (getMouseY() - OFFSET.getY()) / MapRenderer.TILE_SIZE, 1);
//				g.setHitPoints(1);
//			}
//			if (Game.wasKeyPressed(Keyboard.KEY_4)) {
//				Gidrah g = ((GidrahFeature) Resources.get("gidrah.saturn.boss")).spawn(this, (getMouseX() - OFFSET.getX()) / MapRenderer.TILE_SIZE, (getMouseY() - OFFSET.getY()) / MapRenderer.TILE_SIZE, 1);
//				g.setHitPoints(1);
//			}
//			if (Game.wasKeyPressed(Keyboard.KEY_5)) {
//				Gidrah g = ((GidrahFeature) Resources.get("gidrah.titan.boss")).spawn(this, (getMouseX() - OFFSET.getX()) / MapRenderer.TILE_SIZE, (getMouseY() - OFFSET.getY()) / MapRenderer.TILE_SIZE, 1);
//				g.setHitPoints(1);
//			}
//
//
////
//			if (Game.wasKeyPressed(Keyboard.KEY_H)) {
//				renderer.setDebug(debug);
//				// tip test
//				HintFeature hf = HintFeature.getNext();
//				GameScreen.getInstance().showHint(hf);
//			}
//
//			if (Game.wasKeyPressed(Keyboard.KEY_Y) && isOpen()) {
//				diddler = new EmitterDiddler();
//				diddler.addWindowListener(new WindowAdapter() {
//					@Override
//                    public void windowClosed(WindowEvent e) {
//						synchronized (this) {
//							diddler = null;
//						}
//					}
//				});
//			}
//
//			if (Mouse.isButtonDown(0) && diddler != null) {
//				synchronized (this) {
//					if (!buttonWasDown) {
//						if (Resources.exists(diddler.getSelectedEmitter())) {
//							EmitterFeature ef;
//							try {
//								if (emitter != null) {
//									emitter.remove();
//									emitter = null;
//								}
//								ef = (EmitterFeature) Resources.get(diddler.getSelectedEmitter());
//								emitter = ef.spawn(this);
//								emitter.setLocation(getMouseX() - OFFSET.getX(), getMouseY() - OFFSET.getY());
//								emitter.setOffset(OFFSET);
//								//if (ef.isDoYOffset()) {
//								//	emitter.setFloor(getMouseY() - OFFSET.getY());
//								//}
//							} catch (Exception e) {
//								e.printStackTrace(System.err);
//							}
//						}
//					}
//				}
//				buttonWasDown = true;
//			} else {
//				buttonWasDown = false;
//			}
//
//			if (Game.wasKeyPressed(Keyboard.KEY_C)) {
//				levelColourDiddler = new LevelColourDiddler();
//				levelColourDiddler.setListener(new ChangeListener() {
//					public void stateChanged(ChangeEvent e) {
//						if (levelColourDiddler.getSelected() instanceof LevelColorsFeature) {
//							LevelColorsFeature levelColors = (LevelColorsFeature) levelColourDiddler.getSelected();
//							ColorMapFeature.getDefaultColorMap().copy(levelColors);
//						}
//					}
//				});
//				Game.setAlwaysRun(true);
//			}
//
//			if (levelColourDiddler != null && !levelColourDiddler.isVisible()) {
//				levelColourDiddler = null;
//				Game.setAlwaysRun(false);
//			}

		}

		if (Game.isPaused()) {
			return;
		}

		// Update the mouse location (scaled by screen size)
		if (!fastForward) {
			handleMouse();
		}

		MiniGame.onTicked();
		gameState.tick();
		renderer.render((int) mapX + renderer.getOriginX(), (int) mapY + renderer.getOriginY());
		calcRumble();


		Rectangle r = (Rectangle) getArea(ID_HUD_TOP_HIDE).getBounds();
		if (r.contains(getMouseX(), getMouseY())) {
			topHudAlphaTarget = 0;
		} else {
			topHudAlphaTarget = 255;
		}

		if (gameState.isBuilding()) {
			setGroupVisible(COST_GROUP, true);

			// Get rid of the shop
			setHover(null);

			boolean canBuild = true;
			StringBuilder buildText = new StringBuilder(64);
			buildText.append(gameState.getBuilding().getShortTitle());
			buildText.append(' ');

			if (!gameState.isBuildingAvailable()) {
				buildText.append("N/A");
				canBuild = false;
			} else {
				int inv = gameState.getBuilding().getInventory();
				int buildingCost = gameState.getBuildingCost();

				if (buildingCost == 0) {
					buildText.append(Game.getMessage("ultraworm.gamescreen.free"));
				} else {
					buildText.append('$');
					buildText.append(buildingCost);
					if (gameState.getBuilding().getMaxAvailable() > 0) {
						buildText.append(" [");
						buildText.append(inv);
						buildText.append("]");
					}
				}
				if (buildingCost > gameState.getMoney()) {
					canBuild = false;
				}
			}

			Appearance buildInfoIconAnim;

			if (canBuild) {
				costArea.setTextColors(buildColor, buildColor);
				buildInfoIconAnim = (Appearance) Resources.get("buildInfo."+gameState.getBuilding().getShopIcon()+".on.animation");
			} else {
				costArea.setTextColors(cantBuildColor, cantBuildColor);
				buildInfoIconAnim = (Appearance) Resources.get("buildInfo."+gameState.getBuilding().getShopIcon()+".off.animation");
			}

			costArea.setMouseOffAppearance(buildInfoIconAnim);
			costArea.setMouseOnAppearance(buildInfoIconAnim);
			costArea.setText(buildText.toString());

		} else {
			setGroupVisible(COST_GROUP, false);
		}

		updateHUD();
	}

	@Override
	protected void onBlocked() {
		setPaused(true);
	}

	@Override
	protected void onUnblocked() {
		setPaused(false);
	}

	private void calcRumble() {

		if (wetRumble == null || dryRumble == null || wetBuffer == null || dryBuffer == null) {
			return;
		}

		if (isBlocked()) {
			targetRumbleGain = 0.0f;
		} else {
			targetRumbleGain = 1.0f;
		}

		if (rumbleGain < targetRumbleGain) {
			rumbleGain = Math.min(targetRumbleGain, rumbleGain + 0.0125f);
		} else if (rumbleGain > targetRumbleGain) {
			rumbleGain = Math.max(targetRumbleGain, rumbleGain - 0.0125f);
		}

		float x = mapX + renderer.getOriginX() + Game.getWidth() / 2.0f;
		float y = mapY + renderer.getOriginY() + Game.getHeight() / 2.0f;
		ArrayList<Gidrah> gidrahs = gameState.getGidrahs();
		int n = gidrahs.size();
		float totalDry = 0.0f;
		float totalWet = 0.0f;
		for (int i = 0; i < n; i ++) {
			Gidrah g = gidrahs.get(i);
			if (g.isActive() && !g.isFrozen() && !(g.getFeature().isWraith() || g.getFeature().isFlying())) {
				float dist = g.getDistanceTo(x, y);
				// Wet drops off at distance
				totalWet += LinearInterpolator.instance.interpolate(1.0f, 0.0f, dist / Worm.MAX_LOUD_ATTENUATION_DISTANCE);
				// Dry drops off much quicker
				totalDry += LinearInterpolator.instance.interpolate(1.0f, 0.0f, 4.0f * dist / Worm.MAX_ATTENUATION_DISTANCE);
			}
		}

		totalWet = Math.min(1.0f, totalWet * 0.025f);
		totalDry = Math.min(1.0f, totalDry * 0.1f);

		wetRumble.setGain(Game.getSFXVolume() * wetBuffer.getGain() * rumbleGain * totalWet, Game.class);
		dryRumble.setGain(Game.getSFXVolume() * dryBuffer.getGain() * rumbleGain * totalDry, Game.class);
	}

	public void scroll(float dx, float dy) {
		if (zoomTick > 0) {
			// Ignore
			return;
		}

		int adjustWidth = Game.getWidth() % MapRenderer.TILE_SIZE;
		int adjWTile = adjustWidth == 0 ? 6 : 7;
		int adjustHeight = Game.getHeight() % MapRenderer.TILE_SIZE;
		int adjHTile = adjustHeight == 0 ? 6 : 7;
		if (dx < 0) {
			mapX = Math.max(mapX + dx, -MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
		} else if (dx > 0) {
			mapX = Math.min(mapX + dx, (gameState.getMap().getWidth() - renderer.getWidth()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * adjWTile - adjustWidth + MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
			gameState.suppressHint(Hints.SCROLL);
		}
		if (dy < 0) {
			mapY = Math.max(mapY + dy, -MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
			gameState.suppressHint(Hints.SCROLL);
		} else if (dy > 0) {
			mapY = Math.min(mapY + dy, (gameState.getMap().getHeight() - renderer.getHeight()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * adjHTile - adjustHeight + MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
			gameState.suppressHint(Hints.SCROLL);
		}

		OFFSET.setLocation((int) -mapX, (int) -mapY);

		setHover(null);
		stopInfoTimer();
	}

	/**
	 * Handle scrolling using the cursor keys and check for mouse movement at the edges
	 * of the screen
	 */
	private void handleMouse() {
		if (isBlocked()) {
			return;
		}

//		float dx = Worm.getMouseDX();
//		float dy = Worm.getMouseDY();
//		double length = Math.sqrt(dx * dx + dy * dy);
//		if (length > MAX_MOUSE_SCROLL_SPEED) {
//			dx = (float) (MAX_MOUSE_SCROLL_SPEED * dx / length);
//			dy = (float) (MAX_MOUSE_SCROLL_SPEED * dy / length);
//		}

		float dx = 0.0f, dy = 0.0f;

		float oldMapX = mapX;
		float oldMapY = mapY;

		if (zoomTick > 0) {
			zoomTick --;
			mapX = (int) LinearInterpolator.instance.interpolate(endZoomX, startZoomX, (float) zoomTick / (float) zoomDuration);
			mapY = (int) LinearInterpolator.instance.interpolate(endZoomY, startZoomY, (float) zoomTick / (float) zoomDuration);
		} else {
			int mx = getMouseX();
			int my = getMouseY();

			int adjustWidth = Game.getWidth() % MapRenderer.TILE_SIZE;
			int adjWTile = adjustWidth == 0 ? 6 : 7;
			int adjustHeight = Game.getHeight() % MapRenderer.TILE_SIZE;
			int adjHTile = adjustHeight == 0 ? 6 : 7;

			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_S)) {
				my = 0;
				dy = - SCROLL_SPEED;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_A)) {
				mx = 0;
				dx = - SCROLL_SPEED;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_UP) || Keyboard.isKeyDown(Keyboard.KEY_W)) {
				my = Game.getHeight() - 1;
				dy = SCROLL_SPEED;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D)) {
				mx = Game.getWidth() - 1;
				dx = SCROLL_SPEED;
			}
			if (mx == 0 && dx < 0) {
				if (ALLOW_SCROLL_PAST) {
					mapX = Math.max(mapX + dx, -MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
				} else {
					mapX = Math.max(mapX + dx, 0);
				}
				gameState.suppressHint(Hints.SCROLL);
			} else if (mx == Game.getWidth() - 1 && dx > 0) {
				if (ALLOW_SCROLL_PAST) {
					mapX = Math.min(mapX + dx, (gameState.getMap().getWidth() - renderer.getWidth()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * adjWTile - adjustWidth + MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
				} else {
					mapX = Math.min(mapX + dx, (gameState.getMap().getWidth() - renderer.getWidth()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * adjWTile - adjustWidth);
				}
				gameState.suppressHint(Hints.SCROLL);
			}
			if (my == 0 && dy < 0) {
				if (ALLOW_SCROLL_PAST) {
					mapY = Math.max(mapY + dy, -MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
				} else {
					mapY = Math.max(mapY + dy, 0);
				}
				gameState.suppressHint(Hints.SCROLL);
			} else if (my == Game.getHeight() - 1 && dy > 0) {
				if (ALLOW_SCROLL_PAST) {
					mapY = Math.min(mapY + dy, (gameState.getMap().getHeight() - renderer.getHeight()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * adjHTile - adjustHeight + MapRenderer.TILE_SIZE * (MapRenderer.OPAQUE_SIZE - 1));
				} else {
					mapY = Math.min(mapY + dy, (gameState.getMap().getHeight() - renderer.getHeight()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * adjHTile - adjustHeight);
				}
				gameState.suppressHint(Hints.SCROLL);
			}
		}

		OFFSET.setLocation((int) -mapX, (int) -mapY);

		if (oldMapX != mapX || oldMapY != mapY) {
			setHover(null);
			stopInfoTimer();
		}
	}

	/**
	 * Cleanup after a disaster
	 */
	public void panic() {
		removeAllTickables();
		doCleanup();
		removeAllSprites();
		for (Area a : getAreas()) {
			detachTickable(a);
			a.destroy();
			a.create();
			a.spawn(this);
		}
	}

	@Override
	protected void doCleanup() {
		if (baseDestructionEffect != null) {
			baseDestructionEffect.stop(Game.class);
			baseDestructionEffect = null;
		}
		if (wetRumble != null) {
			wetRumble.setFade(20, 0.0f, true, Game.class);
			wetRumble = null;
		}
		if (dryRumble != null) {
			dryRumble.setFade(20, 0.0f, true, Game.class);
			dryRumble = null;
		}
		if (renderer != null) {
			renderer.cleanup();
			renderer = null;
		}
		if (bezerkSoundEffect != null) {
			bezerkSoundEffect.setFade(20, 0.0f, true, ID_BEZERK_TIMER);
			bezerkSoundEffect = null;
		}
		if (freezeSoundEffect != null) {
			freezeSoundEffect.setFade(20, 0.0f, true, ID_FREEZE_TIMER);
			freezeSoundEffect = null;
		}
		if (shieldSoundEffect != null) {
			shieldSoundEffect.setFade(20, 0.0f, true, ID_SHIELD_TIMER);
			shieldSoundEffect = null;
		}
		if (gameState != null) {
			gameState.cleanup();
			// Don't null it; we'll be needing it most likely.
		}
	}

	@Override
	protected void onResized() {
		if (renderer == null) {
			renderer = new MapRenderer(this);
			renderer.setOrigin(-MapRenderer.TILE_SIZE * 4, -MapRenderer.TILE_SIZE * 4);
			renderer.setMap(gameState.getMap());
		}
		renderer.onResized();
		renderer.render((int) mapX + renderer.getOriginX(), (int) mapY + renderer.getOriginY());
		setHover(null);
	}

	@Override
	protected void onOpen() {
		onResized();

		currentHint = null;

		displayedCash = 0;
		displayedCrystals = -1;

		Worm.setMouseAppearance(Res.getMousePointer());

		sideHUDAlpha = 255;
		topHudAlphaTarget = hudAlphaTarget = 255;
		enableBuildings();
		doOnPowerupsUpdated();

		tickableObject = new TickableObject() {
			@Override
			protected void render() {
				glRender(SETUP_RENDERING);

				ReadableColor currtimeLeftColorBottom = timeLeftColorBottom;
				ReadableColor currtimeLeftColorTop = timeLeftColorTop;
				ReadableColor currtimePastColorBottom = timePastColorBottom;
				ReadableColor currtimePastColorTop = timePastColorTop;
				float ratio;
				int h;
				if (gameState.getGameMode() != WormGameState.GAME_MODE_SURVIVAL && !gameState.isRushActive()) {
					ratio = 1.0f - (float) gameState.getLevelTick() / gameState.getLevelDuration();
					ReadableRectangle r = timerBarArea.getBounds();
					h = (int) LinearInterpolator.instance.interpolate(0.0f, r.getHeight(), ratio);

					if (gameState.getLevelTick() > 0) {
						ColorUtil.setGLColorPre(currtimeLeftColorBottom, sideHUDAlpha, this);
						short idx = glVertex2f(r.getX(), r.getY());
						glVertex2f(r.getX() + r.getWidth(), r.getY());
						ColorInterpolator.interpolate(currtimeLeftColorBottom, currtimeLeftColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						glVertex2f(r.getX() + r.getWidth(), r.getY() + h);
						glVertex2f(r.getX(), r.getY() + h);

						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}

					if (gameState.getLevelTick() < gameState.getLevelDuration()) {
						ColorInterpolator.interpolate(currtimePastColorBottom, currtimePastColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						short idx = glVertex2f(r.getX(), r.getY() + h);
						glVertex2f(r.getX() + r.getWidth(), r.getY() + h);
						ColorUtil.setGLColorPre(currtimePastColorTop, sideHUDAlpha, this);
						glVertex2f(r.getX() + r.getWidth(), r.getY() + r.getHeight());
						glVertex2f(r.getX(), r.getY() + r.getHeight());

						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}

				}

				currtimeLeftColorBottom = powerupTimeLeftColorBottom;
				currtimeLeftColorTop = powerupTimeLeftColorTop;
				currtimePastColorBottom = powerupTimePastColorBottom;
				currtimePastColorTop = powerupTimePastColorTop;

				// BEZERK
				if (bezerkTimerVisible) {
					ReadableRectangle r2 = bezerkBarArea.getBounds();
					ratio = (float) gameState.getBezerkTick() / (float) bezerkTimerMax;
					h = (int) LinearInterpolator.instance.interpolate(0.0f, r2.getHeight(), ratio);

					if (gameState.getBezerkTick() > 0) {
						ColorUtil.setGLColorPre(currtimeLeftColorBottom, sideHUDAlpha, this);
						short idx = glVertex2f(r2.getX(), r2.getY());
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY());
						ColorInterpolator.interpolate(currtimeLeftColorBottom, currtimeLeftColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + h);
						glVertex2f(r2.getX(), r2.getY() + h);

						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}
					if (gameState.getBezerkTick() < bezerkTimerMax) {
						ColorInterpolator.interpolate(currtimePastColorBottom, currtimePastColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						short idx = glVertex2f(r2.getX(), r2.getY() + h);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + h);
						ColorUtil.setGLColorPre(currtimePastColorTop, sideHUDAlpha, this);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + r2.getHeight());
						glVertex2f(r2.getX(), r2.getY() + r2.getHeight());

						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}
				}

				// SHIELD
				if (shieldTimerVisible) {
					ReadableRectangle r2 = shieldBarArea.getBounds();
					ratio = (float) gameState.getShieldTick() / (float) shieldTimerMax;
					h = (int) LinearInterpolator.instance.interpolate(0.0f, r2.getHeight(), ratio);

					if (gameState.getShieldTick() > 0) {
						ColorUtil.setGLColorPre(currtimeLeftColorBottom, sideHUDAlpha, this);
						short idx = glVertex2f(r2.getX(), r2.getY());
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY());
						ColorInterpolator.interpolate(currtimeLeftColorBottom, currtimeLeftColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + h);
						glVertex2f(r2.getX(), r2.getY() + h);
						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}
					if (gameState.getShieldTick() < shieldTimerMax) {
						ColorInterpolator.interpolate(currtimePastColorBottom, currtimePastColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						short idx = glVertex2f(r2.getX(), r2.getY() + h);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + h);
						ColorUtil.setGLColorPre(currtimePastColorTop, sideHUDAlpha, this);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + r2.getHeight());
						glVertex2f(r2.getX(), r2.getY() + r2.getHeight());
						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}
				}

				// FREEZE
				if (freezeTimerVisible) {
					ReadableRectangle r2 = freezeBarArea.getBounds();
					ratio = (float) gameState.getFreezeTick() / (float) freezeTimerMax;
					h = (int) LinearInterpolator.instance.interpolate(0.0f, r2.getHeight(), ratio);

					if (gameState.getFreezeTick() > 0) {
						ColorUtil.setGLColorPre(currtimeLeftColorBottom, sideHUDAlpha, this);
						short idx = glVertex2f(r2.getX(), r2.getY());
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY());
						ColorInterpolator.interpolate(currtimeLeftColorBottom, currtimeLeftColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + h);
						glVertex2f(r2.getX(), r2.getY() + h);
						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}
					if (gameState.getFreezeTick() < freezeTimerMax) {
						ColorInterpolator.interpolate(currtimePastColorBottom, currtimePastColorTop, ratio, LinearInterpolator.instance, TEMPCOLOR);
						ColorUtil.setGLColorPre(TEMPCOLOR, sideHUDAlpha, this);
						short idx = glVertex2f(r2.getX(), r2.getY() + h);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + h);
						ColorUtil.setGLColorPre(currtimePastColorTop, sideHUDAlpha, this);
						glVertex2f(r2.getX() + r2.getWidth(), r2.getY() + r2.getHeight());
						glVertex2f(r2.getX(), r2.getY() + r2.getHeight());
						glRender(GL_TRIANGLES, new short[] {(short) (idx + 0), (short) (idx + 1), (short) (idx + 2), (short) (idx + 0), (short) (idx + 2), (short) (idx + 3)});
					}
				}
			}
		};

		tickableObject.setLayer(Layers.HUD);
		tickableObject.spawn(this);

	}

	@Override
	protected void onClose() {
		if (tickableObject != null) {
			tickableObject.remove();
			tickableObject = null;
		}
		// Weather
		LevelFeature levelFeature = gameState.getLevelFeature();
		if (levelFeature.getWeather() != null) {
			levelFeature.getWeather().remove();
		}
	}

	/**
	 * Get the game ticks
	 */
	public static int getTick() {
		return gameState.getTick();
	}

	/**
	 * Get total ticks
	 */
	public static int getTotalTicks() {
		int oldTotalTicks = gameState.getTotalTicks();
		gameState.resetTotalTicks();
		return oldTotalTicks;
	}

	/**
	 * @return the offset for sprites
	 */
	public static Point getSpriteOffset() {
		return OFFSET;
	}

	/**
	 * Begin the game
	 */
	public static void beginGame(WormGameState newGameState) {
		GameScreen.gameState = newGameState;
		instance.hintQueue.clear();
		instance.open();
	}

	/**
	 * Called at the start of a level
	 */
	public static void onBeginLevel() {
		instance.doOnBeginLevel();
	}
	private void doOnBeginLevel() {
		open();

		setGroupVisible(ID_BASE_UNDER_ATTACK, false);
		setGroupVisible(ID_DESTRUCTION_IMMINENT, false);
		setGroupVisible(COST_GROUP, false);
		setGroupVisible(ID_INFO_ALERT_GROUP, false);
		setGroupVisible(ID_CRYSTAL_MESSAGE_GROUP, false);
		setGroupVisible(ID_BEZERK_TIMER, false);
		setGroupVisible(ID_SHIELD_TIMER, false);
		setGroupVisible(ID_FREEZE_TIMER, false);
		setGroupVisible(ID_TOOLTIPS, false);
		setGroupVisible(ID_HINT_GROUP, false);
		setGroupVisible(SHOW_ICON_GROUP, false);

		// chaz hack!
		threatLowArea.setVisible(false);
		threatHighArea.setVisible(false);

		setSellOff();

		bezerkTimerVisible = false;
		shieldTimerVisible = false;
		freezeTimerVisible = false;
		getArea(ID_BASE).setMouseOffAppearance((Appearance) Resources.get("quicklaunch.focus.base.off.animation"));
		onMapChanged();
		doOnPowerupsUpdated();
		enableBuildings();

		repairFlagged = false;
		shieldFlagged = false;

		targetRumbleGain = 1.0f;
		rumbleGain = 0.0f;
		wetRumble = Game.allocateSound(wetBuffer);
		dryRumble = Game.allocateSound(dryBuffer);

		Building building = gameState.getBase();
		if (building == null) {
			return;
		}
		zoom(building.getMapX() + building.getCollisionX() - Game.getWidth() / 2, building.getMapY() + building.getCollisionY() - Game.getHeight() / 2);
	}

	/**
	 * Called whenever the map changes
	 */
	private void onMapChanged() {
		// And here's the map...
		int levelNumber = gameState.getLevel();
		LevelFeature levelFeature = gameState.getLevelFeature();
		String levelName = levelFeature.getTitle().toUpperCase();
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
				break;
		}
		LevelColorsFeature colors = levelFeature.getColors();

		renderer.setMap(gameState.getMap());
		colors.init(renderer);
		AttenuatedColor.setAttenuation(colors.getColor(AttenuatedColor.ATTENUATE));

		timeLeftColorBottom = new MappedColor(TIME_LEFT_COLOR_BOTTOM);
		timeLeftColorTop = new MappedColor(TIME_LEFT_COLOR_TOP);
		timePastColorBottom = new MappedColor(TIME_PAST_COLOR_BOTTOM);
		timePastColorTop = new MappedColor(TIME_PAST_COLOR_TOP);

		powerupTimeLeftColorBottom = new MappedColor(POWERUP_TIME_LEFT_COLOR_BOTTOM);
		powerupTimeLeftColorTop = new MappedColor(POWERUP_TIME_LEFT_COLOR_TOP);
		powerupTimePastColorBottom = new MappedColor(POWERUP_TIME_PAST_COLOR_BOTTOM);
		powerupTimePastColorTop = new MappedColor(POWERUP_TIME_PAST_COLOR_TOP);

		buildColor = new MappedColor(BUILD_COLOR);
		cantBuildColor = new MappedColor(CANT_BUILD_COLOR);

		if (gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL) {
			levelArea.setText("00:00:00:00");
		} else if (gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) {
			levelArea.setText(levelName);
		} else if (levelNumber < 9) {
			levelArea.setText("0"+String.valueOf(levelNumber + 1)+" "+levelName);
		} else {
			levelArea.setText(String.valueOf(levelNumber + 1)+" "+levelName);
		}

		// Ambient music
		if (gameState.getGameMode() == WormGameState.GAME_MODE_XMAS) {
			Game.playMusic((ALStream) Resources.get("december.stream"), 180);
		} else if (levelNumber == -1) {
			// Survival mode. Music will change periodically.
		} else {
			Game.playMusic(Res.getAmbient(levelNumber), 180);
		}

		// And the map scrolly display
		mapX = (gameState.getMap().getWidth() * MapRenderer.TILE_SIZE - Game.getWidth()) / 2;
		mapY = (gameState.getMap().getHeight() * MapRenderer.TILE_SIZE - Game.getHeight()) / 2;

		// Weather
		if (levelFeature.getWeather() != null) {
			levelFeature.getWeather().spawn(this);
		}
	}

	public static void onBaseAttacked() {
		instance.doOnBaseAttacked();
	}
	private void doOnBaseAttacked() {
		if (baseDestructionImminentEffect != null) {
			return;
		}
		SFX.baseAttacked();

		if (baseAttackedEffect != null) {
			baseAttackedEffect.remove();
			baseAttackedEffect = null;
		}
		setGroupVisible(ID_BASE_UNDER_ATTACK, true);
		baseAttackedEffect = new FadeEffect(120, 60) {
			@Override
			protected void onTicked() {
				setGroupAlpha(ID_BASE_UNDER_ATTACK, getAlpha());
			}
			@Override
			protected void doRemove() {
				setGroupVisible(ID_BASE_UNDER_ATTACK, false);
				getArea(ID_BASE).setMouseOffAppearance((Appearance) Resources.get("quicklaunch.focus.base.off.animation"));
			}
		};
		getArea(ID_BASE).setMouseOffAppearance((Appearance) Resources.get("quicklaunch.focus.base.flash.animation"));
		baseAttackedEffect.spawn(this);
	}

	public static void onBaseDestructionImminent() {
		instance.doOnBaseDestructionImminent();
	}
	private void doOnBaseDestructionImminent() {
		if (baseDestructionImminentEffect != null) {
			return;
		}
		if (baseAttackedEffect != null) {
			baseAttackedEffect.remove();
			baseAttackedEffect = null;
		}
		if (baseDestructionEffect == null) {
			baseDestructionEffect = SFX.baseDestructionImminent();
		}
		setGroupVisible(ID_DESTRUCTION_IMMINENT, true);
		baseDestructionImminentEffect = new FadeEffect(180, 60) {
			@Override
			protected void onTicked() {
				setGroupAlpha(ID_DESTRUCTION_IMMINENT, getAlpha());
			}
			@Override
			protected void doRemove() {
				setGroupVisible(ID_DESTRUCTION_IMMINENT, false);
				getArea(ID_BASE).setMouseOffAppearance((Appearance) Resources.get("quicklaunch.focus.base.off.animation"));
				baseDestructionImminentEffect = null;
			}
		};
		getArea(ID_BASE).setMouseOffAppearance((Appearance) Resources.get("quicklaunch.focus.base.flash.animation"));
		baseDestructionImminentEffect.spawn(this);
		if (RepairPowerupFeature.getInstance().isEnabledInShop() && !repairFlagged) {
			gameState.flagHint(Hints.REPAIR);
			repairFlagged = true;
		} else if (ShieldPowerupFeature.getInstance().isEnabledInShop() && !shieldFlagged) {
			gameState.flagHint(Hints.SHIELD);
			shieldFlagged = true;
		}
	}
	public static void onBaseRepaired() {
		instance.doOnBaseRepaired();
	}
	private void doOnBaseRepaired() {
		if (baseDestructionImminentEffect != null) {
			baseDestructionImminentEffect.finish();
			baseDestructionImminentEffect = null;
		}
		if (baseDestructionEffect != null) {
			baseDestructionEffect.stop(Game.class);
			baseDestructionEffect = null;
		}
		shieldFlagged = false;
		repairFlagged = false;
	}

	/**
	 * Game over
	 */
	public static void gameOver() {
		instance.doGameOver();
	}
	private void doGameOver() {
		if (baseDestructionEffect != null) {
			baseDestructionEffect.stop(Game.class);
			baseDestructionEffect = null;
		}
		Game.playMusic((ALBuffer) null, 180);
		// And open the game over screen
		GameOverScreen.show();
	}

	@Override
	protected void doCreateScreen() {

		moneyArea = getArea(ID_MONEY);
		crystalRemainingArea = getArea(ID_CRYSTAL_REMAINING);
		crystalArea = getArea(ID_CRYSTAL);
		levelArea = getArea(ID_LEVEL);
		costArea = getArea(ID_COST);
		mapArea = getArea(ID_MAP);

		timerBarArea = getArea(ID_TIMER_BAR);
		threatLowArea = getArea(ID_THREAT_LOW);
		threatHighArea = getArea(ID_THREAT_HIGH);

		bezerkBarArea = getArea(ID_BEZERK_TIMER_BAR);
		freezeBarArea = getArea(ID_FREEZE_TIMER_BAR);
		shieldBarArea = getArea(ID_SHIELD_TIMER_BAR);
		infoTextArea = getArea(ID_BUILDINGS_INFOTEXT);
		bonusTextArea = getArea(ID_BUILDINGS_BONUSTEXT);
		costTextArea = getArea(ID_BUILDINGS_COSTTEXT);
		titleTextArea = getArea(ID_BUILDINGS_TITLETEXT);
		titleTextNoKeyArea = getArea(ID_BUILDINGS_TITLETEXT_NO_KEY);
		infoSpriteLocationArea = getArea(ID_INFO_SPRITE_LOCATION);
		hintTextArea = getArea(ID_HINT_TEXT);
		hintSpriteLocationArea = getArea(ID_HINT_SPRITE_LOCATION);

		particleDebug = new GLTextArea();
		particleDebug.setBounds(220, 240, 100, 60);
		particleDebug.setFont(net.puppygames.applet.Res.getTinyFont());

		// And the default offset for effects
		Effect.setDefaultOffset(this, OFFSET);

		// And sound attenuation
		Effect.setDefaultAttenuator(this, Worm.ATTENUATOR);

		wetBuffer = (ALBuffer) Resources.get("rumble_ambience.buffer");
		dryBuffer = (ALBuffer) Resources.get("rumble.buffer");

		hintQueue = new ArrayList<HintFeature>(4);

	}

	@Override
	protected void preRender() {
		if (shakeTick > 0) {
			shakeTick --;
			shook = true;
			glPushMatrix();
			glTranslatef(0.0f, Util.random(-shakeTick, shakeTick), 0.0f);
		} else {
			shook = false;
		}
	}

	public void updateHUD() {
		int currentCash = gameState.getMoney();
		int oldDisplayedCash = displayedCash;
		if (displayedCash > currentCash + 5000) {
			displayedCash = Math.max(currentCash, displayedCash - 1000);
		} else if (displayedCash > currentCash + 500) {
			displayedCash = Math.max(currentCash, displayedCash - 100);
		} else if (displayedCash > currentCash) {
			displayedCash = Math.max(currentCash, displayedCash - 10);
		} else if (displayedCash < currentCash - 5000) {
			displayedCash = Math.min(currentCash, displayedCash + 1000);
		} else if (displayedCash < currentCash - 500) {
			displayedCash = Math.min(currentCash, displayedCash + 100);
		} else if (displayedCash < currentCash) {
			displayedCash = Math.min(currentCash, displayedCash + 10);
		}

		if (oldDisplayedCash != displayedCash) {
			moneyArea.setText(String.valueOf(displayedCash));
		}

		int crystals = gameState.getCrystals();
		if (crystals != displayedCrystals) {
			displayedCrystals = crystals;
			if (crystals == 0) {
				crystalRemainingArea.setText("{color:gui-dark}0");
			} else {
				crystalRemainingArea.setText("{color:gui-mid}"+crystals);
			}
		}

		if (gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL || gameState.getGameMode() == WormGameState.GAME_MODE_SURVIVAL) {
			levelArea.setText(TimeUtil.format(gameState.getLevelTick()).toString());
		} else {
			if (gameState.isRushActive()) {
				threatLowArea.setVisible(false);
				threatHighArea.setVisible(true);
			} else {
				threatLowArea.setVisible(true);
				threatHighArea.setVisible(false);
			}
		}

		int numUnminedCrystals = gameState.getNumUnminedCrystals();
		if (crystalArea.isEnabled() && numUnminedCrystals == 0) {
			if (unminedCrystalEffect != null) {
				unminedCrystalEffect.remove();
				unminedCrystalEffect = null;
			}
			setGroupEnabled(ID_CRYSTAL_GROUP, false);
			setGroupVisible(ID_CRYSTAL_MESSAGE_GROUP, false);
		} else if (!crystalArea.isEnabled() && numUnminedCrystals > 0) {
			if (unminedCrystalEffect != null) {
				unminedCrystalEffect.reset();
			} else {
				unminedCrystalEffect = new FadeEffect(180, 60) {
					@Override
					protected void onTicked() {
						setGroupAlpha(ID_CRYSTAL_MESSAGE_GROUP, getAlpha() * sideHUDAlpha / 255);
					}
					@Override
					protected void doRemove() {
						setGroupVisible(ID_CRYSTAL_MESSAGE_GROUP, false);
					}
				};
				unminedCrystalEffect.spawn(this);
				setGroupVisible(ID_CRYSTAL_MESSAGE_GROUP, true);
			}
			setGroupEnabled(ID_CRYSTAL_GROUP, true);
		}
		if (unminedCrystalEffect != null && !unminedCrystalEffect.isActive()) {
			unminedCrystalEffect = null;
		}

		if (gameState.getBezerkTick() == 0 && bezerkTimerVisible) {
			setGroupVisible(ID_BEZERK_TIMER, false);
			bezerkTimerVisible = false;
			if (bezerkSoundEffect != null) {
				bezerkSoundEffect.setFade(20, 0.0f, true, ID_BEZERK_TIMER);
				bezerkSoundEffect = null;
			}
		}
		if (gameState.getFreezeTick() == 0 && freezeTimerVisible) {
			setGroupVisible(ID_FREEZE_TIMER, false);
			freezeTimerVisible = false;
			if (freezeSoundEffect != null) {
				freezeSoundEffect.setFade(20, 0.0f, true, ID_FREEZE_TIMER);
				freezeSoundEffect = null;
			}
		}
		if (gameState.getShieldTick() == 0 && shieldTimerVisible) {
			setGroupVisible(ID_SHIELD_TIMER, false);
			shieldTimerVisible = false;
			if (shieldSoundEffect != null) {
				shieldSoundEffect.setFade(20, 0.0f, true, ID_SHIELD_TIMER);
				shieldSoundEffect = null;
			}
		}

		if (infoTimer > 0) {
			infoTimer --;
			if (infoTimer == 0) {
				setHover(infoItem);
			}
		}

		if (hintEffect != null) {
			hintTick ++;
			if (hintTick == HINT_DURATION) {
				closeHint();
			}
			if (hintEffect.getCurrent() == 0 && hintEffect.getTarget() == 0) {
				hintEffect.remove();
				hintEffect = null;
				currentHint = null;
				if (hintQueue.size() > 0) {
					showHint(hintQueue.remove(0));
				}
			}
		}

		if (hudAlpha != hudAlphaTarget) {
			if (hudAlpha < hudAlphaTarget) {
				if (hudAlpha == 0) {
					setGroupVisible(ID_HIDEABLE_GROUP, true);
					doOnPowerupsUpdated();
					enableBuildings();
				}
				hudAlpha = Math.min(hudAlphaTarget, hudAlpha + HUD_ALPHA_FADE_SPEED);
			} else {
				hudAlpha = Math.max(hudAlphaTarget, hudAlpha - HUD_ALPHA_FADE_SPEED);
				if (hudAlpha == 0) {
					setGroupVisible(ID_HIDEABLE_GROUP, false);
				}
			}
			setGroupAlpha(ID_HIDEABLE_GROUP, hudAlpha);
		}

		if (topHudAlpha != topHudAlphaTarget) {
			if (topHudAlpha < topHudAlphaTarget) {
				topHudAlpha = Math.min(topHudAlphaTarget, topHudAlpha + HUD_ALPHA_FADE_SPEED);
			} else {
				topHudAlpha = Math.max(topHudAlphaTarget, topHudAlpha - HUD_ALPHA_FADE_SPEED);
			}
			setGroupAlpha(ID_TOP_HUD_GROUP, topHudAlpha);
		}

		if (gameState.isSelling() && !sellActive) {
			setSellOn();
		}
		if (!gameState.isSelling() && sellActive) {
			setSellOff();
		}

	}

	public void onBezerkTimerIncreased(int newMax) {
		bezerkTimerMax = newMax;
		if (!bezerkTimerVisible) {
			bezerkTimerVisible = true;
			setGroupVisible(ID_BEZERK_TIMER, true);

			if (bezerkSoundEffect == null) {
				bezerkSoundEffect = Game.allocateSound(Res.getBezerkSound(), 1.0f, 1.0f, ID_BEZERK_TIMER);
			}
		}
	}

	public void onFreezeTimerIncreased(int newMax) {
		freezeTimerMax = newMax;
		if (!freezeTimerVisible) {
			freezeTimerVisible = true;
			setGroupVisible(ID_FREEZE_TIMER, true);

			if (freezeSoundEffect == null) {
				freezeSoundEffect = Game.allocateSound(Res.getFreezeSound(), 1.0f, 1.0f, ID_FREEZE_TIMER);
			}
		}
	}

	public void onShieldTimerIncreased(int newMax) {
		shieldTimerMax = newMax;
		if (!shieldTimerVisible) {
			shieldTimerVisible = true;
			setGroupVisible(ID_SHIELD_TIMER, true);

			if (shieldSoundEffect == null) {
				shieldSoundEffect = Game.allocateSound(Res.getShieldSound(), 1.0f, 1.0f, ID_SHIELD_TIMER);
			}
		}
	}

	@Override
	protected void postRender() {
		if (renderer != null) {
			renderer.postRender();
		}

		if (shook) {
			glPopMatrix();
		}
	}

	/**
	 * Shake the screen
	 * @param amount
	 */
	public static void shake(int amount) {
		instance.shakeTick = amount;
	}

	/**
	 * @return Returns the instance.
	 */
	public static GameScreen getInstance() {
		return instance;
	}

	/**
	 * Called just before app shutdown
	 */
	public static void onExit() {
		if (instance != null && instance.isOpen()) {
			MiniGame.saveGame();
		}
	}

	@Override
	protected void onHover(final String id, final boolean on) {
		// Tooltip manager. No tooltips when building
		if 	(
				!gameState.isBuilding() && !gameState.isSelling() && Worm.getShowTooltips()
				&&	(id.startsWith(ID_POWERUP_SHORTCUTS) || id.startsWith(ID_BUILDING_SHORTCUTS) || id.startsWith(ID_MISC_SHORTCUTS))
			)
		{
			// Make sure its teh same tooltip
			if (on) {
				if (currentTooltipID != id) {
					// No, so remove the previous tooltip if there is one
					if (tooltipEffect != null) {
						tooltipEffect.finish();
						tooltipEffect = null;
					}
					currentTooltipID = id;
					setVisible(id+"_tooltip", true);
					setVisible(id+"_tooltip_glow", true);

					if ( id.startsWith(ID_BUILDING_SHORTCUTS) ) {
						// chaz hack! adjust area length - doesnt take 'I' or 'M' etc. into account tho
						ReadableRectangle r = getArea(id+"_tooltip").getBounds();
						int w = getArea(id+"_tooltip").getText().length()*6+30;
						getArea(id+"_tooltip").setBounds(r.getX(), r.getY(), w, r.getHeight());
						getArea(id+"_tooltip_glow").setBounds(r.getX(), r.getY(), w, r.getHeight());
						//System.out.println(id+": size=\""+w+","+r.getHeight()+"\"");
					}

					tooltipEffect = new FadeEffect(90, 5) {
						@Override
						protected void onTicked() {
							getArea(id+"_tooltip").setAlpha(getAlpha());
							getArea(id+"_tooltip_glow").setAlpha(getAlpha());
						}
						@Override
						protected void doRemove() {
							GameScreen.this.setVisible(id+"_tooltip_glow", false);
							GameScreen.this.setVisible(id+"_tooltip", false);
						}

					};
					tooltipEffect.spawn(this);
					if (id.startsWith(ID_BUILDING_SHORTCUTS)) {
						for (Iterator<BuildingFeature> i = BuildingFeature.getBuildings().iterator(); i.hasNext(); ) {
							BuildingFeature bf = i.next();
							if (id.equals(bf.getHUD()) && bf.getShopAppearance() != null) {
								startInfoTimer(bf);
								break;
							}
						}
					} else if (id.startsWith(ID_POWERUP_SHORTCUTS)) {
						String powerupName = id.substring(ID_POWERUP_SHORTCUTS.length());
						startInfoTimer((PowerupFeature) Resources.get(powerupName));
					} else {
						stopInfoTimer();
					}
				}
			} else {
				if (currentTooltipID == id) {
					// No, so remove the previous tooltip if there is one
					if (tooltipEffect != null) {
						tooltipEffect.finish();
						tooltipEffect = null;
					}
					currentTooltipID = null;
					stopInfoTimer();
				}
			}
		} else {
			// We're not over a tooltippable thing, so remove any tooltip
			if (tooltipEffect != null) {
				tooltipEffect.finish();
				tooltipEffect = null;
			}
			stopInfoTimer();
		}
	}

	private void startInfoTimer(ShopItem shopItem) {
		if (hover == shopItem) {
			setHover(shopItem);
		} else if (hover != null) {
			setHover(shopItem);
		} else {
			if (!Worm.getShowInfo()) {
				return;
			}
			infoTimer = SHOP_SENSITIVITY;
			infoItem = shopItem;
		}
	}

	private void stopInfoTimer() {
		infoTimer = SHOP_LURK;
		infoItem = null;
	}


	@Override
	protected void onClicked(String id) {
		if (fastForward) {
			return;
		}

		if (ID_BUILDING_DONE.equals(id)) {
			gameState.setBuilding(null);
		} else if (ID_MENU.equals(id)) {
			gameState.handleESC();
		} else if (ID_BASE.equals(id)) {
			Building building = gameState.getBase();
			if (building == null) {
				return;
			}
			zoom(building.getX() - Game.getWidth() / 2, building.getY() - Game.getHeight() / 2);
		} else if (ID_CRYSTAL.equals(id)) {
			Building building = gameState.getNextUnminedCrystal();
			if (building == null) {
				return;
			}
			zoom(building.getX() - Game.getWidth() / 2, building.getY() - Game.getHeight() / 2);

		} else if (ID_FASTFORWARD.equals(id)) {
			fastForward = true;
		} else if (ID_HINT_CLOSE.equals(id)) {
			closeHint();
		} else if (ID_SHOWHIDE.equals(id)) {

			// Toggle visibility of bottom HUD
			if (hudAlphaTarget == 0) {
				hudAlphaTarget = 255;
				setGroupVisible(SHOW_ICON_GROUP, false);
			} else {
				hudAlphaTarget = 0;
				setGroupVisible(SHOW_ICON_GROUP, true);
				// remove shop
				setHover(null);
			}

			// set location of build bar
			if (gameState.isBuilding()) {
				setBuildInfoPanelPosition();
			}
			// set sell stuff
			if (gameState.isSelling()) {
				setSellOn();
			} else {
				setSellOff();
			}
			doOnPowerupsUpdated();


		} else if (ID_SELL.equals(id)) {
			gameState.setSellMode(true);
			setSellOn();

		} else if (ID_SELL_DONE.equals(id) || ID_SELL_INFO_DONE.equals(id)) {
			gameState.setSellMode(false);
			setSellOff();

		} else {
			// Maybe clicked on a building shortcut or a powerup shortcut
			if (id.startsWith(ID_POWERUP_SHORTCUTS)) {
				String powerupName = id.substring(ID_POWERUP_SHORTCUTS.length());
				gameState.usePowerup((PowerupFeature) Resources.get(powerupName));
				if (powerupName.equals("smartbomb.powerup")) {
					setSellOff();
				}

			} else if (id.startsWith(ID_BUILDING_SHORTCUTS)) {
				for (Iterator<BuildingFeature> i = BuildingFeature.getBuildings().iterator(); i.hasNext(); ) {
					BuildingFeature bf = i.next();
					if (id.equals(bf.getHUD()) && bf.getShopAppearance() != null) {
						gameState.setBuilding(bf);
						// move pos of build info bar to correct
						setBuildInfoPanelPosition();
						setSellOff();
						break;
					}
				}
			}
		}
	}

	public static void doSaveEffect() {
		LabelEffect saveEffect = new LabelEffect(net.puppygames.applet.Res.getBigFont(), Game.getMessage("ultraworm.gamescreen.game_saved"), ReadableColor.WHITE, ReadableColor.BLUE, SAVE_DURATION / 2, SAVE_DURATION / 2);
		saveEffect.setLocation(Game.getWidth() / 2, Game.getHeight() / 2 + MapRenderer.TILE_SIZE);
		saveEffect.setVisible(true);
		saveEffect.spawn(instance);
		saveEffect.setOffset(null);
	}

	public static void doFailedSaveEffect() {
		LabelEffect failEffect = new LabelEffect(net.puppygames.applet.Res.getBigFont(), Game.getMessage("ultraworm.gamescreen.failed_to_save"), ReadableColor.WHITE, ReadableColor.RED, SAVE_DURATION / 2, SAVE_DURATION / 2);
		failEffect.setLocation(Game.getWidth() / 2, Game.getHeight() / 2 + MapRenderer.TILE_SIZE);
		failEffect.setVisible(true);
		failEffect.spawn(instance);
		failEffect.setOffset(null);
	}

	public static void onEndLevel() {
		instance.doOnEndLevel();
	}
	private void doOnEndLevel() {
		if (baseDestructionEffect != null) {
			baseDestructionEffect.stop(Game.class);
			baseDestructionEffect = null;
		}
		closeHint();
	}

	public static void onPowerupsUpdated() {
		instance.doOnPowerupsUpdated();
	}
	private void doOnPowerupsUpdated() {
		boolean hasPowerups = false;
		for (PowerupFeature pf : PowerupFeature.getPowerups()) {
			if (pf.getShopIcon() != null) {
				Area area = getArea(ID_POWERUP_SHORTCUTS + pf.getName());
				boolean enabled = pf.isEnabledInShop();
				area.setEnabled(enabled);
				hasPowerups |= enabled;
				getArea("counter_"+ ID_POWERUP_SHORTCUTS + pf.getName()).setMouseOffAppearance(Res.getQuicklaunchCountOff(Math.min(9, gameState.getNumPowerups(pf))));
				getArea("counter_"+ ID_POWERUP_SHORTCUTS + pf.getName()).setMouseOnAppearance(Res.getQuicklaunchCountOn(Math.min(9, gameState.getNumPowerups(pf))));
				setVisible("counter_"+ ID_POWERUP_SHORTCUTS + pf.getName(), enabled && hudAlphaTarget > 0);
			}
		}
		setEnabled(ID_POWERUPS_LABEL, hasPowerups);
	}

	public void zoom(float x, float y) {
		startZoomX = mapX;
		startZoomY = mapY;
		if (ALLOW_SCROLL_PAST) {
			endZoomX = x;
			endZoomY = y;
		} else {
			endZoomX = Math.max(0, Math.min(x, (gameState.getMap().getWidth() - renderer.getWidth()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * 6));
			endZoomY = Math.max(0, Math.min(y, (gameState.getMap().getHeight() - renderer.getHeight()) * MapRenderer.TILE_SIZE + MapRenderer.TILE_SIZE * 6));
		}
		double dx = startZoomX - endZoomX;
		double dy = startZoomY - endZoomY;
		double dist = Math.sqrt(dx * dx + dy * dy);
		if (dist < ZOOM_SPEED) {
			return;
		}
		zoomTick = zoomDuration = (int) (dist / ZOOM_SPEED);
	}


	/**
	 * Enable / disable / hide building HUD buttons
	 */
	public void enableBuildings() {
		Collection<BuildingFeature> buildings = BuildingFeature.getBuildings();
		for (Iterator<BuildingFeature> i = buildings.iterator(); i.hasNext(); ) {
			BuildingFeature bf = i.next();
			if (bf.getHUD() != null && bf.getShopAppearance() != null) {
				setEnabled(bf.getHUD(), bf.isAvailable());
				int numAvailable = Math.min(99, gameState.getAvailableStock(bf));
				if (bf.getNumAvailable() == 0 || !bf.isAvailable() || hudAlphaTarget == 0) {
					setVisible("counter_"+bf.getHUD(), false);
					setVisible("counter10_"+bf.getHUD(), false);
				} else if (numAvailable >= 0 && numAvailable < 10) {
					getArea("counter10_"+bf.getHUD()).setMouseOffAppearance(Res.getQuicklaunchCountOff(numAvailable));
					getArea("counter10_"+bf.getHUD()).setMouseOnAppearance(Res.getQuicklaunchCountOn(numAvailable));
					setVisible("counter10_"+bf.getHUD(), true);
					setVisible("counter_"+bf.getHUD(), false);
				} else if (numAvailable >= 10) {
					getArea("counter10_"+bf.getHUD()).setMouseOffAppearance(Res.getQuicklaunchCount10Off(numAvailable % 10));
					getArea("counter10_"+bf.getHUD()).setMouseOnAppearance(Res.getQuicklaunchCount10On(numAvailable % 10));
					getArea("counter_"+bf.getHUD()).setMouseOffAppearance(Res.getQuicklaunchCountOff(numAvailable / 10));
					getArea("counter_"+bf.getHUD()).setMouseOnAppearance(Res.getQuicklaunchCountOn(numAvailable / 10));
					setVisible("counter10_"+bf.getHUD(), true);
					setVisible("counter_"+bf.getHUD(), true);
				}
			}
		}
	}

	public void setSideHUDAlpha(int alpha) {
		setGroupAlpha(ID_HUD_SIDES, alpha);
		sideHUDAlpha = alpha;
	}

	public static void onMapGrabbedMouse(boolean grabbed) {
		instance.setGrabbed(grabbed? instance.mapArea : null);
	}

	public static boolean isSomethingElseGrabbingMouse() {
		return instance.getGrabbed() != null && instance.getGrabbed() != instance.mapArea;
	}

	public boolean isFastForward() {
		return fastForward;
	}

	public void clearFastForward() {
		fastForward = false;
	}

	/**
	 * Sets the currently hovered item
	 * @param newHover The thing being hovered over, or null, to clear
	 */
	private void setHover(ShopItem newHover) {
		if (newHover == hover) {
			return;
		}

		hover = newHover;
		if (hover == null) {
			if (shopEffect != null) {
				shopEffect.setTarget(0);
			}
		} else {
			if (gameState.getBuilding() != null) {
				// Don't open shop in build mode
				hover = null;
				return;
			}

			// Turn on entire data group
			setGroupVisible(ID_INFO_GROUP, true);

			if (hover.getTooltipGraphic()!=null) {
				titleTextNoKeyArea.setVisible(false);
				titleTextArea.setText(hover.getTitle());
			} else {
				titleTextArea.setVisible(false);
				titleTextNoKeyArea.setText(hover.getTitle());
			}

			infoTextArea.setText(hover.getDescription());
			bonusTextArea.setText(hover.getBonusDescription());

			if (infoLayers != null) {
				infoLayers.remove();
				infoLayers = null;
			}

			if (shopEffect != null) {
				shopEffect.setTarget(255);
				showShopSprites();
			} else {
				shopEffect = new AdjusterEffect(SHOP_FADE_RATE, 0, 255) {
					int lastInventory = -999;
					ShopItem currentHover;

					@Override
					protected void onTicked() {
						int a = getCurrent();
						setGroupAlpha(ID_INFO_GROUP, a);
						if (a == 0 && shopSpritesVisible) {
							hide();
						} else if (a != 0 && !shopSpritesVisible) {
							show();
						}
						if (infoLayers != null) {
							for (int i = 0; i < infoLayers.getSprites().length; i ++) {
								infoLayers.getSprite(i).setAlpha(a);
							}
						}
						if (a != 0 && hover != null) {
							int inventory = hover.getInventory();

							if (currentHover != hover || inventory != lastInventory) {

								getArea("id_shortcutkey").setMouseOffAppearance(hover.getTooltipGraphic());

								currentHover = hover;
								lastInventory = inventory;
								if (hover.getInitialValue() > 0) {
									// It's a building
									StringBuilder sb = new StringBuilder(128);
									sb.append(Game.getMessage("ultraworm.gamescreen.cost")+": ");
									if (hover.isEnabledInShop()) {
										int shopValue = hover.getShopValue();
										if (shopValue > 0) {
											sb.append(shopValue);
										} else {
											sb.append(Game.getMessage("ultraworm.gamescreen.free"));
										}
									} else {
										sb.append("N/A");
									}
									if (inventory > 0) {
										sb.append("     "+Game.getMessage("ultraworm.gamescreen.available")+": ");
										sb.append(inventory);
									} else if (inventory == 0 && hover.getNumAvailable() > 0) {
										sb.append("     "+Game.getMessage("ultraworm.gamescreen.none_available"));
									}
									costTextArea.setText(sb.toString());
								} else {
									// It's a powerup
									costTextArea.setText(Game.getMessage("ultraworm.gamescreen.available")+": "+inventory);
								}
							}
						} else {
							lastInventory = -999;
							currentHover = null;
						}

					}
					void hide() {
						setGroupVisible(ID_INFO_GROUP, false);
						removeShopSprites();
					}
					void show() {
						showShopSprites();
					}

					@Override
					protected void doRemove() {
						super.doRemove();
						hide();
						shopEffect = null;
					}
					@Override
					protected void doSpawnEffect() {
						currentHover = hover;
					}
				};
				shopEffect.spawn(this);
			}

		}
		if (tooltipEffect != null) {
			tooltipEffect.remove();
			tooltipEffect = null;
		}
	}

	private void removeShopSprites() {
		if (infoLayers != null) {
			infoLayers.remove();
			infoLayers = null;
		}
		shopSpritesVisible = false;
	}

	private void showShopSprites() {
		removeShopSprites();
		LayersFeature shopAppearance = hover.getShopAppearance();
		ReadablePoint offset = hover.getShopOffset();
		int ox, oy;
		if (offset == null) {
			ox = 0;
			oy = 0;
		} else {
			ox = offset.getX();
			oy = offset.getY();
		}
		infoLayers = new SimpleThingWithLayers(instance);
		shopAppearance.createSprites(instance, infoLayers);
		for (int i = 0; i < infoLayers.getSprites().length; i ++) {
			infoLayers.getSprite(i).setLocation(infoSpriteLocationArea.getBounds().getX() + ox, infoSpriteLocationArea.getBounds().getY() + oy);
		}
		shopSpritesVisible = true;
	}

	public void dequeueHint(HintFeature hintFeature) {
		while (hintQueue.remove(hintFeature)) {
			;
		}
	}

	/**
	 * Shows a hint. If there's already a hint being shown, this hint is queued.
	 */
	public void showHint(HintFeature hintFeature) {

		if (!Worm.getShowHints()) {
			return;
		}

		assert hintFeature != null;

		if (currentHint != null) {
			if (currentHint == hintFeature) {
				return;
			}
			if (hintQueue.contains(hintFeature)) {
				return;
			}
			hintQueue.add(hintFeature);
			return;
		}

		int seq = gameState.getHintSequence(hintFeature);
		if (seq == -1) {
			// No more hints of this sort
			return;
		}

		currentHint = hintFeature;

		// Turn on entire data group
		setGroupVisible(ID_HINT_GROUP, true);
		if (hintFeature.getText() != null) {
			hintTextArea.setText(hintFeature.getText());
		} else {
			hintTextArea.setText(((TextResource) hintFeature.getHints().getResource(seq)).getText());
		}

		if (hintEffect != null) {
			hintEffect.setTarget(255);
			showHintSprites();
		} else {
			hintEffect = new AdjusterEffect(HINT_FADE_RATE, 0, 255) {

				@Override
				protected void onTicked() {
					int current = getCurrent();
					int a = current * topHudAlpha / 255;
					setGroupAlpha(ID_HINT_GROUP, a);
					if (current == 0 && hintSpritesVisible) {
						hide();
					} else if (current != 0 && !hintSpritesVisible) {
						show();
					}
					if (hintLayers != null) {
						for (int i = 0; i < hintLayers.getSprites().length; i++) {
							hintLayers.getSprite(i).setAlpha(a);
						}
					}
				}

				void hide() {
					setGroupVisible(ID_HINT_GROUP, false);
					removeHintSprites();
				}

				void show() {
					showHintSprites();
				}

				@Override
				protected void doRemove() {
					super.doRemove();
					hide();
					hintEffect = null;
				}
			};
			hintEffect.spawn(this);
		}

	}

	public void closeHint() {
		if (hintEffect != null && hintEffect.getTarget() != 0) {
			hintEffect.setTarget(0);
		}
	}

	public boolean isShowingPausedHint() {
		return currentHint != null && currentHint.getPause() && hintEffect != null && hintEffect.getTarget() == hintEffect.getCurrent();
	}

	private void removeHintSprites() {
		if (hintLayers != null) {
			hintLayers.remove();
			hintLayers = null;
		}
		hintSpritesVisible = false;
	}

	private void showHintSprites() {
		removeHintSprites();
		if (currentHint != null && currentHint.getIcon() != null) {
			hintLayers = new SimpleThingWithLayers(instance);
			currentHint.getIcon().createSprites(instance, hintLayers);
			ReadableRectangle hsb = hintSpriteLocationArea.getBounds();
			for (int i = 0; i < hintLayers.getSprites().length; i++) {
				hintLayers.getSprite(i).setLocation(hsb.getX() + hsb.getWidth() / 2, hsb.getY() + hsb.getHeight() / 2);
				hintLayers.getSprite(i).setAlpha(topHudAlpha);
				if (!currentHint.getIcon().getName().contentEquals("hint.info.layers")) {
					hintLayers.getSprite(i).setScale(HINT_SCALE);
				}
			}
			hintSpritesVisible = true;
			hintTick = 0;
		}
	}


	private void setBuildInfoPanelPosition() {

		int buildInfoPos;
		if (hudAlphaTarget == 0) {
			buildInfoPos = COST_INFO_Y_LOW;
		} else {
			buildInfoPos = COST_INFO_Y_HIGH;
		}

		if (getArea(ID_BUILDING_DONE).getPosition().getY()!=buildInfoPos+COST_INFO_BUTTON_OFFSET) {
			List<Area> costGroupAreas = getAreas(COST_GROUP);
			for (int i = 0; i < costGroupAreas.size(); i ++) {
				Area a = costGroupAreas.get(i);
				ReadableRectangle aBounds=a.getBounds();
				int y = buildInfoPos;
				if (a.getID().equals(ID_BUILDING_DONE)) {
					y += COST_INFO_BUTTON_OFFSET;
				}
				a.setBounds(aBounds.getX(), y, aBounds.getWidth(), aBounds.getHeight());
			}
		}
	}

	private void setSellOn() {

		boolean minimized=false;
		if (hudAlphaTarget==0) {
			minimized=true;
		}

		// minimized mini sell panel
		setGroupVisible(SELL_INFO_GROUP, minimized);
		setGroupEnabled(SELL_INFO_GROUP, minimized);

		// sell button on hud
		setVisible(ID_SELL, false);
		//setEnabled(ID_SELL, false); don't disable, otherwise DEL wont work when hud hidden

		// cancel button on hud
		setVisible(ID_SELL_DONE, !minimized);
		setEnabled(ID_SELL_DONE, !minimized);

		setGroupVisible(ID_INFO_GROUP,false);

		sellActive = true;

	}

	private void setSellOff() {

		boolean minimized=false;
		if (hudAlphaTarget==0) {
			minimized=true;
		}

		// minimized mini sell panel
		setGroupVisible(SELL_INFO_GROUP, false);
		setGroupEnabled(SELL_INFO_GROUP, false);

		if (!minimized) {
			// sell button on hud
			setVisible(ID_SELL, true);
			setEnabled(ID_SELL, true);
		}

		// cancel button on hud
		setVisible(ID_SELL_DONE, false);
		setEnabled(ID_SELL_DONE, false);

		setGroupVisible(ID_INFO_GROUP,false);

		sellActive = false;

	}

}
