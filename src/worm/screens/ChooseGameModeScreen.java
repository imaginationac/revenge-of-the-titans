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
import worm.*;

/**
 * Choose a game to play
 */
public class ChooseGameModeScreen extends Screen {

	private static ChooseGameModeScreen instance;

	private static final String ID_CANCEL = "cancel";
	private static final String ID_RESTORE = "mode_restore";
	private static final String ID_CAMPAIGN = "mode_campaign";
	private static final String ID_ENDLESS = "mode_endless";
	private static final String ID_SURVIVAL = "mode_survival";

	private static final String ID_BUTTON = "mode_";
	private static final String ID_TITLE = "_title";
	private static final String ID_DESC = "_desc";
	private static final String GROUP_LABELS = "labels";
	private static final String ID_DEFAULT_MSG = "default_msg";

	private transient String hoveredID;

	/**
	 * @param name
	 */
	public ChooseGameModeScreen(String name) {
		super(name);
		setAutoCreated();
	}

	public static void show() {
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
		Worm.setMouseAppearance(Res.getMousePointer());
		setEnabled(ID_RESTORE, Game.isRestoreAvailable());
	}

	@Override
	protected void onClicked(String id) {
		if (ID_CANCEL.equals(id)) {
			close();
		} else if (ID_RESTORE.equals(id)) {
			close();
			Game.restoreGame();
		} else if (ID_CAMPAIGN.equals(id)) {
			close();
			Worm.newGame(WormGameState.GAME_MODE_CAMPAIGN);
		} else if (ID_ENDLESS.equals(id)) {
			close();
			Worm.newGame(WormGameState.GAME_MODE_ENDLESS);
		} else if (ID_SURVIVAL.equals(id)) {
			close();
			if (Worm.getMaxLevel(WormGameState.GAME_MODE_CAMPAIGN) < 10) {
				net.puppygames.applet.Res.getCancelDialog().doModal("SURVIVAL MODE", "SURVIVAL MODE IS UNLOCKED BY COMPLETING THE {color:text-bold}EARTH CAMPAIGN LEVELS!{color:text}\n\nTHE BUILDINGS AVAILABLE TO YOU ARE DETERMINED BY YOUR PROGRESS IN CAMPAIGN MODE.", null);
			} else {
				Worm.newGame(WormGameState.GAME_MODE_SURVIVAL);
			}
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
