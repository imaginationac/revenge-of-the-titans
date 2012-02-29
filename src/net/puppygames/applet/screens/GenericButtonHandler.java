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
package net.puppygames.applet.screens;

import net.puppygames.applet.Game;
import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Screen;

/**
 * $Id: GenericButtonHandler.java,v 1.2 2010/03/30 22:04:08 foo Exp $
 * Handle generically named buttons on all the screens
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public class GenericButtonHandler implements GenericButtons {

	private static final String REGISTERED_AREAS = "REGISTERED_AREAS";
	private static final String DEMO_AREAS = "DEMO_AREAS";

	/**
	 * No c'tor
	 */
	private GenericButtonHandler() {
	}

	public static void onClicked(String id) {
		if (id.equals(CLOSE)) {
			TitleScreen.show();
		} else if (id.equals(EXIT)) {
			if (Game.isRegistered()) {
				Game.requestExit();
			} else {
				NagScreen.show("Nag! Nag! Nag! Buy! Buy! Buy!", true);
			}
		} else if (id.equals(BUY)) {
			MiniGame.buy(true);
		} else if (id.equals(REDEFINE_KEYS)) {
			Game.redefineKeys();
		} else if (id.equals(PLAY)) {
			MiniGame.beginNewGame();
		} else if (id.equals(HISCORES)) {
			MiniGame.showHiscores();
		} else if (id.equals(CREDITS)) {
			MiniGame.showCredits();
		} else if (id.equals(OPTIONS)) {
			MiniGame.showOptions();
		} else if (id.equals(HELP)) {
			MiniGame.showHelp();
		} else if (id.equals(MOREGAMES)) {
			MiniGame.showMoreGames();
		}
	}

	public static void onOpen(Screen screen) {
		screen.setGroupVisible(REGISTERED_AREAS, Game.isRegistered());
		screen.setGroupVisible(DEMO_AREAS, !Game.isRegistered());
		screen.setEnabled(BUY, !Game.isRegistered());
		screen.setVisible(EXIT, true);
	}
}
