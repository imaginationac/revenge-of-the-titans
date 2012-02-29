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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;

import org.lwjgl.util.ReadableRectangle;

import worm.Worm;
import worm.WormGameState;
import worm.animation.SimpleThingWithLayers;
import worm.features.LayersFeature;
import worm.features.MedalFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;

/**
 * The medals screen
 */
public class MedalsScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static final int MEDALS_PER_PAGE = 6;

	private static final String BACKGROUND = "background_glow";
	private static final String ID_PREV = "prev";
	private static final String ID_NEXT = "next";
	private static final String ID_CLOSE = "close";




	private static MedalsScreen instance;

	/** Current page */
	private transient int page;

	/** Number of pages */
	private transient int numPages;

	/** Map of medals to number earned */
	private transient Map<MedalFeature, Integer> medals;

	/** Sorted list of medals */
	private transient MedalFeature[] sortedMedals;

	/** Sprites */
	private transient List<SimpleThingWithLayers> medalLayers;
	private transient SimpleThingWithLayers rankLayers;

	/**
	 * C'tor
	 * @param name
	 */
	public MedalsScreen(String name) {
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

	public static void show() {
		instance.open();
	}

	@Override
	protected void onOpen() {
		WormGameState gameState = Worm.getGameState();
		medals = new HashMap<MedalFeature, Integer>(gameState.getMedals());

		String world = gameState.getWorld().getUntranslated();
		getArea(BACKGROUND).setMouseOffAppearance((Appearance) Resources.get(world+".research.background.anim"));
		ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get(world+".colormap"));

		getArea("score").setText("{color:text font:tinyfont.glfont}"+Game.getMessage("ultraworm.medalsscreen.score")+": {color:text-bold}"+gameState.getScore());
		getArea("rank").setText("{color:text font:smallfont.glfont}"+Game.getMessage("ultraworm.medalsscreen.rank")+": {color:text-bold}"+gameState.getRank().getTitle());

		// rank img

		if (rankLayers != null) {
			rankLayers.remove();
			rankLayers = null;
		}

		LayersFeature layers = gameState.getRank().getAppearance();
		if (layers != null) {
			Area rankImgArea = getArea("rank-img");
			ReadableRectangle pos = rankImgArea.getBounds();
			rankLayers = new SimpleThingWithLayers(this);
			layers.createSprites(this, rankLayers);
			Sprite[] rankSprite = rankLayers.getSprites();
			for (Sprite element : rankSprite) {
				element.setLocation(pos.getX()+pos.getWidth()/2, pos.getY()+pos.getHeight()/2);
			}
		}




		// Add all missing medals
		for (Iterator<MedalFeature> i = MedalFeature.getMedals().values().iterator(); i.hasNext(); ) {
			MedalFeature mf = i.next();
			if (!medals.containsKey(mf) && (!mf.isXmas() || (mf.isXmas() && gameState.getGameMode() == WormGameState.GAME_MODE_XMAS))) {
				medals.put(mf, Integer.valueOf(0));
			}
		}

		// Calculate number of pages
		numPages = medals.size() / MEDALS_PER_PAGE + (medals.size() % MEDALS_PER_PAGE > 0 ? 1 : 0);
		sortedMedals = medals.keySet().toArray(new MedalFeature[medals.size()]);
		Arrays.sort(sortedMedals, new Comparator<MedalFeature>() {
			@Override
			public int compare(MedalFeature mf0, MedalFeature mf1) {
				int n0 = medals.get(mf0).intValue();
				int n1 = medals.get(mf1).intValue();

				if (n0 > 0 && n1 == 0) {
					return -1;
				} else if (n1 > 0 && n0 == 0) {
					return 1;
				} else if (mf0.getPoints() > mf1.getPoints()) {
					return -1;
				} else if (mf0.getPoints() < mf1.getPoints()) {
					return 1;
				} else {
					return mf0.getTitle().compareTo(mf1.getTitle());
				}
			}
		});

		// Start on first page
		setPage(0);


	}

	private void setPage(int newPage) {
		if (medalLayers != null) {
			for (Iterator<SimpleThingWithLayers> i = medalLayers.iterator(); i.hasNext(); ) {
				SimpleThingWithLayers s = i.next();
				if (s != null) {
					s.remove();
				}
			}
			medalLayers = null;
		}
		medalLayers = new ArrayList<SimpleThingWithLayers>(MEDALS_PER_PAGE);
		page = newPage;
		getArea("page").setText(page + 1 + "/" + numPages);
		int idx = page * MEDALS_PER_PAGE;
		for (int i = 0; i < MEDALS_PER_PAGE; i ++) {
			if (idx < sortedMedals.length) {
				// Show
				setGroupVisible("slot_"+(i + 1)+"_group", true);
				MedalFeature medal = sortedMedals[idx];
				Integer n = medals.get(medal);
				String titleColor, descColor;
				if (n.intValue() == 0) {
					titleColor = "{color:text-darkest}";
					descColor = "{color:text-darkest}";
				} else if (n.intValue() == 1) {
					descColor = "{color:text}";
					titleColor = "{color:text-bold}";
				} else {
					descColor = "{color:text}";
					titleColor = "{color:text-bold}";
				}
				getArea("id_medal_slot_"+(i + 1)+"_title").setText(titleColor+medal.getTitle()+(n.intValue() > 1 ? " ["+n.intValue()+"]" : ""));
				getArea("id_medal_slot_"+(i + 1)+"_desc").setText(descColor+medal.getDescription());
				boolean hasPoints = medal.getPoints() > 0;
				boolean hasBonus = medal.getMoney() > 0;
				getArea("id_medal_slot_"+(i + 1)+"_value").setText
					(
						(hasPoints ? titleColor+String.valueOf(medal.getPoints()) : "")
					+	(hasPoints && hasBonus ? "\n" : "")
					+	(hasBonus ? "$"+String.valueOf(medal.getMoney()) : "")
					);

				Area imgArea = getArea("id_medal_slot_"+(i + 1)+"_img");
				if (n.intValue() > 0) {
					ReadableRectangle pos = imgArea.getBounds();
					LayersFeature layers = medal.getAppearance();
					if (layers != null) {

						SimpleThingWithLayers sl = new SimpleThingWithLayers(this);
						layers.createSprites(this, sl);
						Sprite[] s = sl.getSprites();
						for (Sprite element : s) {
							if (element != null) {
								element.setLocation(pos.getX() + pos.getWidth() / 2, pos.getY() + pos.getHeight() / 2);
							}
						}
						medalLayers.add(sl);
					}
				}

			} else {
				// Hide
				setGroupVisible("slot_"+(i + 1)+"_group", false);
			}

			idx ++;
		}

		setEnabled(ID_PREV, page > 0);
		setEnabled(ID_NEXT, page < numPages - 1);
	}

	@Override
	protected void onClicked(String id) {
		if (ID_PREV.equals(id)) {
			if (page > 0) {
				setPage(page - 1);
			}
		} else if (ID_NEXT.equals(id)) {
			if (page < numPages - 1) {
				setPage(page + 1);
			}
		} else if (ID_CLOSE.equals(id)) {
			close();
		}
	}
}
