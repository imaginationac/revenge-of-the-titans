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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import worm.Res;
import worm.Stats;
import worm.Worm;
import worm.WormGameState;
import worm.animation.SimpleThingWithLayers;
import worm.entities.Building;
import worm.features.MedalFeature;
import worm.features.RankFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;

/**
 * The intermission screen is shown at the end of the level
 */
public class IntermissionScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static IntermissionScreen instance;

	private static final String BACKGROUND = "background_glow";

	private static final String ID_OK = "cancel";
	private static final String ID_MAIN = "main";
	private static final String ID_MEDALS = "medals";
	private static final String ID_MEDAL_DESCRIPTION = "medal_description";
	private static final String ID_RANK = "rank";
	private static final String ID_RANK_NAME = "rank_name";

	private static final String ID_GOTO_MEDALS = "goto_medals";

	private static final int MAX_ICONS = 16;
	private static final float MEDAL_ZOOM_SPEED = 0.025f;
	private static final float INITIAL_SCALE = 0.275f;
	private static final float ZOOMED_SCALE = 0.35f;

	private static final int ICON_GAP = 2;
	private static final int ICON_SIZE = 26;
	private static final int ICONS_ACROSS = 9;
	private static final float ICON_X_OFFSET = 13.0f;
	private static final float ICON_Y_OFFSET = 13.0f;

	private transient List<MedalFeature> sortedMedals;
	private transient List<MedalWidget> medalWidgets;
	private transient MedalWidget hovered;

	private class MedalWidget {
		MedalFeature medal;
		SimpleThingWithLayers layers;
		Sprite[] sprite;
		float scale = ZOOMED_SCALE, targetScale = INITIAL_SCALE;
		float x, y;

		MedalWidget(MedalFeature medal, float x, float y) {
			this.medal = medal;
			this.x = x - ICON_X_OFFSET;
			this.y = y - ICON_Y_OFFSET;

			medal.getAppearance().createSprites(IntermissionScreen.this, layers = new SimpleThingWithLayers(IntermissionScreen.this));
			sprite = layers.getSprites();
			for (Sprite element : sprite) {
				if (element != null) {
					element.setLocation(x, y);
					element.setScale(FPMath.fpValue(scale));
				}
			}
		}

		void cleanup() {
			layers.remove();
			layers = null;
			sprite = null;
		}

		void tick() {
			int mx = getMouseX();
			int my = getMouseY();

			if (mx >= x && my >= y && mx <= x + ICON_SIZE && my <= y + ICON_SIZE) {
				zoomIn();
				if (hovered != this) {
					hovered = this;
					getArea(ID_MEDAL_DESCRIPTION).setText("{font:smallfont.glfont color:text-bold}"+medal.getTitle()+"\n{font:tinyfont.glfont color:text}"+medal.getDescription());
				}
			} else {
				zoomOut();
				if (hovered == this) {
					getArea(ID_MEDAL_DESCRIPTION).setText("{font:tinyfont.glfont color:text-dark}"+Game.getMessage("ultraworm.intermission.hover_instruction"));
					hovered = null;
				}
			}

			if (scale < targetScale) {
				scale = Math.min(targetScale, scale + MEDAL_ZOOM_SPEED);
			} else if (scale > targetScale) {
				scale = Math.max(targetScale, scale - MEDAL_ZOOM_SPEED);
			}
			for (Sprite element : sprite) {
				if (element != null) {
					element.setScale(FPMath.fpValue(scale));
				}
			}
		}

		void zoomIn() {
			targetScale = ZOOMED_SCALE;
		}

		void zoomOut() {
			targetScale = INITIAL_SCALE;
		}
	}

	/**
	 * C'tor
	 */
	public IntermissionScreen(String name) {
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

	/**
	 * Shows the Research Screen with research information about the specified world.
	 * @param world
	 */
	public static void show() {
		instance.open();
	}

	@Override
	protected void onOpen() {

		WormGameState gameState = Worm.getGameState();
		String world = gameState.getWorld().getUntranslated();
		getArea(BACKGROUND).setMouseOffAppearance((Appearance) Resources.get(world+".research.background.anim"));
		ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get(world+".colormap"));
		ColorMapFeature.getDefaultColorMap().copy(gameState.getLevelFeature().getColors());

		Worm.setMouseAppearance(Res.getMousePointer());

		int hp = 0;
		int valueOfStandingBuildings = 0;
		int remainingCrystal = 0;

		// Collect all factories etc.
		ArrayList<Building> buildings = gameState.getBuildings();
		for (int i = 0; i < buildings.size(); i ++) {
			Building b = buildings.get(i);
			if (b.isActive()) {
				if (b.isCity()) {
					hp = (b.getHitPoints() + 1) / 4;
				} else if (b.canSell()) {
					valueOfStandingBuildings += b.getSalePrice();
					gameState.addStat(Stats.RECYCLED, 1);
					gameState.addAvailableStock(b.getFeature(), 1);
				} else if (b.isCrystal()) {
					remainingCrystal += b.getSalePrice();
				}
			}
		}


		// Award player a bonus for undamaged base and buildings remaining
		int bonus = hp * 100;
		gameState.addMoney(bonus + valueOfStandingBuildings + remainingCrystal);

		StringBuilder sb = new StringBuilder(256);
		sb.append("{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.intermission.base_integrity_bonus")+": {color:text-bold}$");
		sb.append(bonus);


		if (valueOfStandingBuildings > 0) {
			sb.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.intermission.building_recycling")+": ");
			sb.append("{color:text-bold}$"+valueOfStandingBuildings);
		} else {
			sb.append("\n{font:tinyfont.glfont color:text-dark}"+Game.getMessage("ultraworm.intermission.building_recycling_none"));
		}

		if (gameState.getLevel() > 0) {
			if (remainingCrystal > 0) {
				sb.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.intermission.crystal_scavenging")+"+ @ "+((int) (gameState.getScavengeRate() * 100.0f))+"%: ");
				sb.append("{color:text-bold}$"+remainingCrystal);
			} else {
				sb.append("\n{font:tinyfont.glfont color:text-dark}"+Game.getMessage("ultraworm.intermission.well_done"));
			}
		}

		// medals

		RankFeature oldRank = gameState.getRank();
		gameState.checkForNewMedals();

		Area medalsArea = getArea(ID_MEDALS);

		Set<MedalFeature> medals = gameState.getMedalsEarnedThisLevel();
		int valueOfMedalsEarned = 0;
		for (MedalFeature mf : medals) {
			valueOfMedalsEarned += mf.getMoney();
		}
		if (valueOfMedalsEarned > 0) {
			sb.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.intermission.medal_bonuses")+": ");
			sb.append("{color:text-bold}$"+valueOfMedalsEarned);
		} else {
			sb.append("\n{font:tinyfont.glfont color:text-dark}"+Game.getMessage("ultraworm.intermission.medal_bonuses_none"));
		}

		// new rank?
		Area rankArea = getArea(ID_RANK);
		Area rankNameArea = getArea(ID_RANK_NAME);
		StringBuilder rankSb = new StringBuilder(256);
		StringBuilder rankNameSb = new StringBuilder(256);

		RankFeature newRank = gameState.getRank();
		if (newRank != oldRank) {
			rankSb.append("{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.intermission.new_rank"));
			rankSb.append(" {font:smallfont.glfont color:transparent}");
			rankSb.append(newRank.getTitle());
			rankArea.setText(rankSb.toString());
			rankArea.setVisible(true);

			rankNameSb.append("{font:tinyfont.glfont color:transparent}"+Game.getMessage("ultraworm.intermission.new_rank"));
			rankNameSb.append(" {font:smallfont.glfont color:text-bold}");
			rankNameSb.append(newRank.getTitle());
			rankNameArea.setText(rankNameSb.toString());
			rankNameArea.setVisible(true);

			sb.append("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.intermission.rank_bonus")+": ");
			sb.append("{color:text-bold}$"+newRank.getPoints() / 10);
		} else {
			rankArea.setVisible(false);
			rankNameArea.setVisible(false);
		}

		sb.append("\n");

		Area mainArea = getArea(ID_MAIN);
		mainArea.setText(sb.toString());


		if (medals.size() > 0) {
			getArea(ID_MEDAL_DESCRIPTION).setText("\n{font:tinyfont.glfont color:text}"+Game.getMessage("ultraworm.intermission.medals_earned")+"\n");
		} else {
			medalWidgets = null;
			getArea(ID_MEDAL_DESCRIPTION).setText("\n{font:tinyfont.glfont color:text-dark}"+Game.getMessage("ultraworm.intermission.no_medals_earned")+"\n");
		}

		if (medals.size() > 0) {
			sortedMedals = null;
			sortedMedals = new ArrayList<MedalFeature>(medals);
			Collections.sort(sortedMedals, new Comparator<MedalFeature>() {
				@Override
				public int compare(MedalFeature mf0, MedalFeature mf1) {
					if (mf0.getPoints() > mf1.getPoints()) {
						return -1;
					} else if (mf0.getPoints() < mf1.getPoints()) {
						return 1;
					} else {
						return mf0.getTitle().compareTo(mf1.getTitle());
					}
				}
			});

			medalWidgets = null;
			medalWidgets = new ArrayList<MedalWidget>(MAX_ICONS);
			float ypos = medalsArea.getBounds().getY() + medalsArea.getBounds().getHeight() + medalsArea.getOffset().getY();
			int count = 0;
			float xpos = (Game.getWidth() - (sortedMedals.size() * ICON_SIZE + (sortedMedals.size() - 1) * ICON_GAP)) / 2.0f + ICON_SIZE / 2;


			for (Iterator<MedalFeature> i = sortedMedals.iterator(); i.hasNext(); ) {
				MedalFeature mf = i.next();
				medalWidgets.add(new MedalWidget(mf, xpos + count * (ICON_SIZE + ICON_GAP), ypos));
				count ++;
			}
			getArea(ID_MEDAL_DESCRIPTION).setText("{font:tinyfont.glfont color:text-dark}"+Game.getMessage("ultraworm.intermission.hover_instruction"));
		}

		gameState.checkPoint();

	}

	@Override
	protected void onClicked(String id) {
		if (ID_OK.equals(id)) {
			close();
		} else if (ID_GOTO_MEDALS.equals(id)) {
			MedalsScreen.show();
		}
	}

	@Override
	protected void onClose() {
		Worm.getGameState().nextLevel();
		if (medalWidgets != null) {
			for (Iterator<MedalWidget> i = medalWidgets.iterator(); i.hasNext(); ) {
				MedalWidget mw = i.next();
				mw.cleanup();
			}
			medalWidgets = null;
		}
	}

	@Override
	protected void doTick() {
		GameScreen.getInstance().updateHUD();
		if (medalWidgets != null) {
			for (Iterator<MedalWidget> i = medalWidgets.iterator(); i.hasNext(); ) {
				MedalWidget mw = i.next();
				mw.tick();
			}
		}
	}
}
