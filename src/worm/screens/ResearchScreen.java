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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.puppygames.applet.Area;
import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Screen;
import net.puppygames.applet.screens.DialogScreen;
import worm.Res;
import worm.Worm;
import worm.WormGameState;
import worm.animation.SimpleThingWithLayers;
import worm.buildings.BuildingFeature;
import worm.features.LayersFeature;
import worm.features.ResearchFeature;
import worm.features.SettingFeature;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;
import com.shavenpuppy.jglib.sprites.Appearance;
import com.shavenpuppy.jglib.sprites.Sprite;

/**
 * Choose from one of several items to research.
 */
public class ResearchScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static ResearchScreen instance;

//	private static final String ID_CASH = "id_cash";
	private static final String ID_BACK = "id_back";
	private static final String ID_NEXT_LEVEL = "id_next_level";

	private static final String BACKGROUND = "background";
	private static final String DESCRIPTION = "description";
	private static final String TITLE = "title";
	private static final String COST = "cost";
	private static final String STATS_COLUMN_1 = "stats_1";
	private static final String STATS_COLUMN_2 = "stats_2";
	private static final String INFO_SPRITE_LOCATION = "info_sprite_location";

	private static final String RESEARCH = "research_table";
	private static final String CANT_AFFORD = "_off";
	private static final String ALREADY_HAVE = "_on";

	private static final String GROUP_RESEARCH = "research";
	private static final String GROUP_INFO = "info";
	private static final String GROUP_ALL_RESEARCHED = "all_researched";
	private static final String GROUP_REQUIRES = "requires";
	private static final String REQUIRES_LABEL ="requires_label";

//	private static final String GROUP_TABLE = "table";
//	private static final String GROUP_TREE = "tree";
//	private static final String ID_SWITCH_BUTTON = "id_switch_button";
//	private static final String ID_SWITCH_LABEL_TREE = "id_switch_label_tree";
//	private static final String ID_SWITCH_LABEL_TABLE = "id_switch_label_table";
	private static final String ID_MENU = "menu";

	private static final int ITEM_RESEARCHED = 0, ITEM_AVAILABLE = 1, ITEM_UNAVAILABLE = 2, ITEM_NOT_READY = 3;

	private static Runnable backAction, forwardAction;

	private transient SimpleThingWithLayers layers;
	private transient boolean atLeastOneAvailable = false;
	private transient ResearchFeature current;

	/**
	 * @param name
	 */
	public ResearchScreen(String name) {
		super(name);
	}

	public static void show(Runnable backAction, Runnable forwardAction) {
		ResearchScreen.backAction = backAction;
		ResearchScreen.forwardAction = forwardAction;
		instance.open();
	}

	@Override
	protected void doCleanup() {
		ResearchScreen.backAction = null;
		ResearchScreen.forwardAction = null;

		removeSprites();
		current = null;
	}

	private void removeSprites() {
		if (layers != null) {
			layers.remove();
			layers = null;
		}
	}

	@Override
	protected void doRegister() {
		assert instance == null;
		instance = this;
	}

	@Override
	protected void doDeregister() {
		assert instance == this;
		instance = null;
	}

	@Override
	protected void doCreateScreen() {
	}

	@Override
	protected void onClicked(String id) {

		if (id.equals(ID_NEXT_LEVEL)) {
			close();
			forwardAction.run();
		} else if (id.equals(ID_BACK)) {
			close();
			backAction.run();
		} else if (id.startsWith(RESEARCH)) {

			String researchID = id.split("_")[2];

			final ResearchFeature rf = ResearchFeature.getResearch().get(researchID);

			if (rf!=null && ( getItemState(rf)==ITEM_AVAILABLE || Game.DEBUG)) {
				if (rf.isRegisteredOnly() && !Game.isRegistered()) {
					// Show the BUY dialog
					Res.getResearchNagDialog().doModal(Game.getMessage("ultraworm.researchscreen.nag_title"), Game.getMessage("ultraworm.researchscreen.nag_message"), new Runnable() {
						@Override
						public void run() {
							if (Res.getResearchNagDialog().getOption() == DialogScreen.OK_OPTION) {
								MiniGame.buy(true);
							}
						}
					});
				} else if (!Worm.getGameState().isResearched(current.getID())) {
					// Story screen depending on building type and world
					Worm.getGameState().setResearched(rf.getID());
					getResearchRunnable(rf).run();
				}
			}
		} else if (id.equals(ID_MENU)) {
			MenuScreen.show(MenuScreen.MENU_STORY_MODE);
		}
	}

	private Runnable getResearchRunnable(final ResearchFeature rf) {
		return new Runnable() {
			@Override
			public void run() {
				SettingFeature setting = Worm.getGameState().getWorld().getSetting(rf.getSetting());
				rf.getStory().setSetting(setting);
				StoryScreen.show("story.screen."+Worm.getGameState().getWorld().getUntranslated(), false, rf.getStory(), new Runnable() {
					@Override
					public void run() {
						// Unresearch!
						Worm.getGameState().setUnresearched(rf.getID());
						Worm.getGameState().showResearchScreen();
					}
				}, forwardAction);
			}
		};
	}

	private void calcDepends(ResearchFeature rf, Set<ResearchFeature> required) {
		if (rf == null || rf.getDepends() == null) {
			return;
		}
		StringTokenizer st = new StringTokenizer(rf.getDepends(), ",", false);
		while (st.hasMoreTokens()) {
			String depends = st.nextToken();
			ResearchFeature req = ResearchFeature.getResearch().get(depends);
			if (req == null) {
				throw new RuntimeException("There's no '"+depends+"' in research features!");
			}
			required.add(req);
			calcDepends(req, required);
		}
	}

	@Override
	protected void onHover(String id, boolean on) {

		if (id.startsWith(RESEARCH)) {

			setGroupVisible(GROUP_ALL_RESEARCHED, false);

			String researchID = id.split("_")[2];
			boolean alreadyHave = researchID.endsWith("_"+ALREADY_HAVE);
			if (alreadyHave) {
				researchID = researchID.substring(0, researchID.length() - ALREADY_HAVE.length());
			}

			ResearchFeature hovered = ResearchFeature.getResearch().get(researchID);

			if (on) {
				if (hovered == null || hovered == current) {
					return;
				}

				// Turn this lot off every time..
				setGroupVisible(GROUP_REQUIRES, false);

				current = hovered;
				int thisItemState = getItemState(current);

				String titleText;

				if (thisItemState==ITEM_UNAVAILABLE) {
					titleText = "{font:smallfont.glfont color:text}"+current.getTitle();
				} else if (thisItemState==ITEM_NOT_READY){
					titleText = "{font:smallfont.glfont color:text}COMING SOON!";
				} else {
					titleText = "{font:smallfont.glfont color:text-bold}"+current.getTitle();
				}

				getArea(TITLE).setText(titleText);

				getArea(DESCRIPTION).setText("");
				getArea(STATS_COLUMN_1).setText("");
				getArea(STATS_COLUMN_2).setText("");

				if (thisItemState!=ITEM_NOT_READY) {

					if (thisItemState==ITEM_UNAVAILABLE) {

						setVisible(REQUIRES_LABEL, true);

						Set<ResearchFeature> required = new HashSet<ResearchFeature>();
						if (hovered.getDepends() != null) {
							if (hovered.getDepends().equals("cantResearchThisYet")) {
							} else {
								calcDepends(hovered, required);
								List<ResearchFeature> sorted = new ArrayList<ResearchFeature>(required);
								Collections.sort(sorted, new Comparator<ResearchFeature>() {
									WormGameState gameState = Worm.getGameState();

									@Override
									public int compare(ResearchFeature o1,	ResearchFeature o2) {
										int is1 = getItemState(o1);
										int is2 = getItemState(o2);
										if (is1 < is2) {
											return -1;
										} else if (is1 > is2) {
											return 1;
										} else {
											return o1.getID().compareToIgnoreCase(o2.getID());
										}
									}
								});
								int i = 0;
								for (ResearchFeature rf : sorted) {
									Area boxArea = getArea("requires_"+i+"_box");
									String areaID = "requires_"+i;

									getArea(areaID).setMouseOffAppearance(getArea(RESEARCH + "_" + rf.getID()).getMouseOffAppearance());
									getArea(areaID + CANT_AFFORD).setMouseOffAppearance(getArea(RESEARCH + "_" + rf.getID() + CANT_AFFORD).getMouseOffAppearance());
									getArea(areaID + ALREADY_HAVE).setMouseOffAppearance(getArea(RESEARCH + "_" + rf.getID() + ALREADY_HAVE).getMouseOffAppearance());

									boxArea.setVisible(true);
									boxArea.setEnabled(true);
									switch (getItemState(rf)) {
										case ITEM_RESEARCHED:
											boxArea.setMouseOffAppearance((Appearance) Resources.get("research.button.researched.off.animation"));
											setVisible(areaID + ALREADY_HAVE, true);
											setVisible(areaID, false);
											setVisible(areaID + CANT_AFFORD, false);
											setEnabled(areaID + ALREADY_HAVE, true);
											break;

										case ITEM_AVAILABLE:
											boxArea.setMouseOffAppearance((Appearance) Resources.get("research.button.on.off.animation"));
											setVisible(areaID + ALREADY_HAVE, false);
											setVisible(areaID, true);
											setVisible(areaID + CANT_AFFORD, false);
											setEnabled(areaID, true);
											break;

										default:
											boxArea.setMouseOffAppearance((Appearance) Resources.get("research.button.off.off.animation"));
											setVisible(areaID + ALREADY_HAVE, false);
											setVisible(areaID, false);
											setVisible(areaID + CANT_AFFORD, true);
											setEnabled(areaID + CANT_AFFORD, true);
											break;
									}
									i ++;
								}
							}
						}

					} else {

						if (!current.getSetting().equals("tech") && thisItemState==ITEM_RESEARCHED) {

							BuildingFeature thisBuilding = BuildingFeature.getBuildingByResearchName(current.getID());
							String descText = thisBuilding.getDescription();

							StringBuilder stats_1_text = new StringBuilder();
							StringBuilder stats_2_text = new StringBuilder();
							thisBuilding.getResearchStats(stats_1_text, stats_2_text);
							getArea(DESCRIPTION).setText("{font:tinyfont.glfont color:text}"+descText);
							getArea(STATS_COLUMN_1).setText(stats_1_text.toString());
							getArea(STATS_COLUMN_2).setText(stats_2_text.toString());

						} else {
							getArea(DESCRIPTION).setText("{font:tinyfont.glfont color:text}"+current.getDescription());
						}
					}
				}

				if (thisItemState==ITEM_RESEARCHED) {
					getArea(COST).setText("{font:tinyfont.glfont color:text-bold}"+Game.getMessage("ultraworm.researchscreen.already_researched"));
				} else {
					getArea(COST).setText("");
				}

				setGroupVisible(GROUP_INFO, true);
				removeSprites();
				LayersFeature l = current.getAppearance();
				if (l != null) {
					Area spriteArea = getArea(INFO_SPRITE_LOCATION);
					int spriteX = spriteArea.getBounds().getX();
					int spriteY = spriteArea.getBounds().getY();
					layers = new SimpleThingWithLayers(this);
					l.createSprites(this, layers);
					Sprite[] sprite = layers.getSprites();
					for (Sprite element : sprite) {
						element.setLocation(spriteX, spriteY);
					}
				}
//				if (researched) {
					Worm.setMouseAppearance(Res.getMousePointer());
//				} else {
//					Worm.setMouseAppearance(Res.getMousePointerCantBuild());
//				}
			} else {
				if (hovered == current) {
					removeSprites();
					setGroupVisible(GROUP_INFO, false);
					current = null;
					Worm.setMouseAppearance(Res.getMousePointer());
					// Turn this lot off every time..
					setGroupVisible(GROUP_REQUIRES, false);
				}
			}
		}
	}

	@Override
	protected void onOpen() {
		removeSprites();

		WormGameState gameState = Worm.getGameState();
		String world = gameState.getWorld().getUntranslated();
		getArea(BACKGROUND).setMouseOffAppearance((Appearance) Resources.get(world+".research.background.anim"));


		current = null;
		Worm.setMouseAppearance(Res.getMousePointer());

		ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get(world+".colormap"));

		if (gameState.getLevel() == 1) {
			Res.getIngameInfoDialog().doModal(Game.getMessage("ultraworm.researchscreen.hint_title"), Game.getMessage("ultraworm.researchscreen.hint_message"), null);
		}


		setGroupEnabled(GROUP_RESEARCH, false);
		setGroupVisible(GROUP_RESEARCH, true);
		setGroupVisible(GROUP_INFO, false);
		setGroupVisible(GROUP_REQUIRES, false);

		initButtons();

		setGroupVisible(GROUP_ALL_RESEARCHED, !atLeastOneAvailable);

		getArea(ID_NEXT_LEVEL).setVisible(forwardAction != null && !atLeastOneAvailable);
		getArea(ID_BACK).setVisible(backAction != null);


	}

	protected void initButtons() {

		Map<String, ResearchFeature> allResearch = ResearchFeature.getResearch();

		atLeastOneAvailable = false;

		for (ResearchFeature rf : allResearch.values()) {

			// check state...
			int thisItemState = getItemState(rf);

			Area boxArea;
			String areaID, boxAreaID;

			areaID = RESEARCH + "_" + rf.getID();

			boxAreaID = areaID + "_box";
			boxArea = getArea(boxAreaID);
			if (boxArea == null) {
				continue;
			}
			boxArea.setEnabled(true);

			// set appearances
			switch (thisItemState) {
				case ITEM_RESEARCHED:
					boxArea.setMouseOffAppearance((Appearance) Resources.get("research.button.researched.off.animation"));
					boxArea.setMouseOnAppearance((Appearance) Resources.get("research.button.researched.on.animation"));
					setVisible(areaID + ALREADY_HAVE, true);
					setVisible(areaID, false);
					setVisible(areaID + CANT_AFFORD, false);
					setEnabled(areaID + ALREADY_HAVE, true);
					break;

				case ITEM_AVAILABLE:
					atLeastOneAvailable = true;
					boxArea.setMouseOffAppearance((Appearance) Resources.get("research.button.on.off.animation"));
					boxArea.setMouseOnAppearance((Appearance) Resources.get("research.button.on.on.animation"));
					setVisible(areaID + ALREADY_HAVE, false);
					setVisible(areaID, true);
					setVisible(areaID + CANT_AFFORD, false);
					setEnabled(areaID, true);
					break;

				default:
					boxArea.setMouseOffAppearance((Appearance) Resources.get("research.button.off.off.animation"));
					boxArea.setMouseOnAppearance((Appearance) Resources.get("research.button.off.on.animation"));
					setVisible(areaID + ALREADY_HAVE, false);
					setVisible(areaID, false);
					setVisible(areaID + CANT_AFFORD, true);
					setEnabled(areaID, false);
					setEnabled(areaID + CANT_AFFORD, true);
					break;
			}

			boxArea.update();
		}
	}


	protected int getItemState(ResearchFeature rf) {

		WormGameState gameState = Worm.getGameState();
		int thisItemState = ITEM_RESEARCHED;

		if (!rf.isDefaultAvailable() && !gameState.isResearched(rf.getID())) {
			thisItemState = ITEM_AVAILABLE;
		}

		if (rf.getDepends() != null) {
			if (rf.getDepends().equals("cantResearchThisYet")) {
				thisItemState = ITEM_NOT_READY;
			} else {
				StringTokenizer st = new StringTokenizer(rf.getDepends(), ",", false);
				while (st.hasMoreTokens()) {
					String depends = st.nextToken();
					if (!gameState.isResearched(depends)) {
						thisItemState = ITEM_UNAVAILABLE;
						break;
					}
				}
			}
		}

		return thisItemState;

	}

}
