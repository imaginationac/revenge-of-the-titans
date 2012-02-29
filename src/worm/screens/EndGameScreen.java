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

/**
 * The End Game Screen ends the game when the player is killed
 */
public class EndGameScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static EndGameScreen instance;

	private static final String ID_QUIT = "gameover_quit";
	private static final String ID_RESTART = "gameover_restart";
	private static final String ID_EASIER = "gameover_easier";
	private static final String ID_ABANDON = "gameover_abandon";
	private static final String ID_MEDALS = "gameover_medals";

	private static final String ID_BUTTON = "gameover_";
	private static final String ID_TITLE = "_title";
	private static final String ID_DESC = "_desc";
	private static final String GROUP_LABELS = "labels";
	private static final String ID_DEFAULT_MSG = "default_msg";

	private transient String hoveredID;

	/**
	 * C'tor
	 */
	public EndGameScreen(String name) {
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

	@Override
	protected void onOpen() {

		setGroupVisible(GROUP_LABELS, false);
		Worm.setMouseAppearance(Res.getMousePointer());

	}

	/**
	 * Shows the Research Screen with research information about the specified world.
	 * @param world
	 */
	public static void show() {
		instance.open();
	}

	@Override
	protected void onClicked(String id) {
		if (ID_RESTART.equals(id)) {
			net.puppygames.applet.Res.getYesCancelDialog().doModal(Game.getMessage("ultraworm.endgame.title_restart"), Game.getMessage("ultraworm.endgame.message"), new Runnable() {
				@Override
				public void run() {
					int option = net.puppygames.applet.Res.getYesCancelDialog().getOption();
					if (option == DialogScreen.OK_OPTION || option == DialogScreen.YES_OPTION) {
						Worm.getGameState().restart();
					}
				}
			});
		} else if (ID_EASIER.equals(id)) {
			net.puppygames.applet.Res.getYesCancelDialog().doModal(Game.getMessage("ultraworm.endgame.title_easier"), Game.getMessage("ultraworm.endgame.message"), new Runnable() {
				@Override
				public void run() {
					int option = net.puppygames.applet.Res.getYesCancelDialog().getOption();
					if (option == DialogScreen.OK_OPTION || option == DialogScreen.YES_OPTION) {
						Worm.getGameState().easier();
						Worm.getGameState().restart();
					}
				}
			});
		} else if (ID_QUIT.equals(id)) {
			Worm.getGameState().quit();
		} else if (ID_ABANDON.equals(id)) {
			Worm.getGameState().showLevelSelectScreen();
		} else if (ID_MEDALS.equals(id)) {
			//close();
			MedalsScreen.show();
		}
	}

	@Override
	protected void onHover(String id, boolean on) {
		if (on) {
			if (id.startsWith(ID_BUTTON)) {
				if (!id.endsWith(ID_TITLE) & !id.endsWith(ID_DESC)) {
					setVisible(id + ID_TITLE, true);
					setVisible(id + ID_DESC, true);

					setVisible(ID_DEFAULT_MSG, false);
					hoveredID = id;
				}
			}
		} else {
			if (id.startsWith(ID_BUTTON)) {
				setVisible(id + ID_TITLE, false);
				setVisible(id + ID_DESC, false);

				if (hoveredID == id) {
					hoveredID = null;
					setVisible(ID_DEFAULT_MSG, true);
				}
			}
		}
	}

}
