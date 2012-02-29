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

import net.puppygames.applet.Game;
import net.puppygames.applet.Screen;
import net.puppygames.applet.screens.DialogScreen;
import worm.Res;
import worm.Worm;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;
import com.shavenpuppy.jglib.sprites.Appearance;

/**
 * The ingame menu - survival mode version
 */
public class SurvivalMenuScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static SurvivalMenuScreen instance;

	private static final String BACKGROUND = "background_glow";

	private static final String ID_CANCEL = "cancel";
	private static final String ID_MEDALS = "esc_medals";
	private static final String ID_RESTART = "esc_restart";
	private static final String ID_NEWMAP = "esc_newmap";
	private static final String ID_SAVEQUIT = "esc_savequit";
	private static final String ID_ABANDON = "esc_abandon";
	private static final String ID_QUIT = "esc_quit";

	private static final String ID_BUTTON = "esc_";
	private static final String ID_TITLE = "_title";
	private static final String ID_DESC = "_desc";
	private static final String ID_ALT_DESC = "_desc_alt";
	private static final String GROUP_LABELS = "labels";
	private static final String ID_DEFAULT_MSG = "default_msg";

	private transient String hoveredID;

	public static final int MENU_GAME_MODE = 0;
	public static final int MENU_STORY_MODE = 1;

	/** Small menu: only "medals", "quit" and "cancel" used */
	private transient boolean small;

	/**
	 * @param name
	 */
	public SurvivalMenuScreen(String name) {
		super(name);
		setAutoCreated();
	}

	public static void show(int mode) {
		if (mode==MENU_STORY_MODE) {
			instance.small = true;
		} else {
			instance.small = false;
		}
		instance.open();
	}

	@Override
	protected void doRegister() {
		instance = this;
	}

	@Override
	protected void doDeregister() {
		instance = null;
	}

	@Override
	protected void onOpen() {

		setGroupVisible(GROUP_LABELS, false);

		String world = Worm.getGameState().getWorld().getUntranslated();
		ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get(world+".colormap"));
		getArea(BACKGROUND).setMouseOffAppearance((Appearance) Resources.get(world+".research.background.anim"));

		Worm.setMouseAppearance(Res.getMousePointer());

		if (small) {
			setEnabled(ID_MEDALS, false);
			setEnabled(ID_RESTART, false);
			setEnabled(ID_NEWMAP, false);
			setEnabled(ID_SAVEQUIT, false);
		} else {
			setEnabled(ID_MEDALS, true);
			setEnabled(ID_RESTART, true);
			setEnabled(ID_NEWMAP, true);
			setEnabled(ID_SAVEQUIT, true);
		}

	}

	@Override
	protected void onClicked(String id) {
		if (ID_CANCEL.equals(id)) {
			close();
		} else if (ID_MEDALS.equals(id)) {
			close();
			MedalsScreen.show();

		} else if (ID_RESTART.equals(id)) {
			net.puppygames.applet.Res.getYesCancelDialog().doModal(Game.getMessage("ultraworm.survivalmenu.restart_game"), Game.getMessage("ultraworm.survivalmenu.confirm"), new Runnable() {
				@Override
				public void run() {
					int option = net.puppygames.applet.Res.getYesCancelDialog().getOption();
					if (option == DialogScreen.OK_OPTION || option == DialogScreen.YES_OPTION) {
						Worm.getGameState().restartSurvival(false);
					}
				}
			});
		} else if (ID_NEWMAP.equals(id)) {
			net.puppygames.applet.Res.getYesCancelDialog().doModal(Game.getMessage("ultraworm.survivalmenu.generate_new_map"), Game.getMessage("ultraworm.survivalmenu.confirm"), new Runnable() {
				@Override
				public void run() {
					int option = net.puppygames.applet.Res.getYesCancelDialog().getOption();
					if (option == DialogScreen.OK_OPTION || option == DialogScreen.YES_OPTION) {
						Worm.getGameState().restartSurvival(true);
					}
				}
			});
		} else if (ID_SAVEQUIT.equals(id)) {
			Worm.getGameState().save();
			close();
		} else if (ID_QUIT.equals(id)) {
			net.puppygames.applet.Res.getYesCancelDialog().doModal(Game.getMessage("ultraworm.survivalmenu.quit_game"), Game.getMessage("ultraworm.survivalmenu.confirm"), new Runnable() {
				@Override
				public void run() {
					int option = net.puppygames.applet.Res.getYesCancelDialog().getOption();
					if (option == DialogScreen.OK_OPTION || option == DialogScreen.YES_OPTION) {
						Worm.getGameState().quit();
					}
				}
			});
		} else if (ID_ABANDON.equals(id)) {
			net.puppygames.applet.Res.getYesCancelDialog().doModal(Game.getMessage("ultraworm.survivalmenu.quit_to_parameters_screen"), Game.getMessage("ultraworm.survivalmenu.confirm"), new Runnable() {
				@Override
				public void run() {
					int option = net.puppygames.applet.Res.getYesCancelDialog().getOption();
					if (option == DialogScreen.OK_OPTION || option == DialogScreen.YES_OPTION) {
						Worm.getGameState().showLevelSelectScreen();
					}
				}
			});
		}
	}

	@Override
	protected void onHover(String id, boolean on) {
		if (on) {
			if (id.startsWith(ID_BUTTON)) {
				if (!id.endsWith(ID_TITLE) & !id.endsWith(ID_DESC) & !id.endsWith(ID_ALT_DESC)) {
					setVisible(id + ID_TITLE, true);
					if (small && getArea(id + ID_ALT_DESC) != null) {
						setVisible(id + ID_ALT_DESC, true);
					} else {
						setVisible(id + ID_DESC, true);
					}
					setVisible(ID_DEFAULT_MSG, false);
					hoveredID = id;
				}
			}
		} else {
			if (id.startsWith(ID_BUTTON)) {
				setVisible(id + ID_TITLE, false);
				setVisible(id + ID_DESC, false);
				if (getArea(id + ID_ALT_DESC) != null) {
					setVisible(id + ID_ALT_DESC, false);
				}
				if (hoveredID == id) {
					hoveredID = null;
					setVisible(ID_DEFAULT_MSG, true);
				}
			}
		}
	}

}
