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
 * $Id: NagScreen.java,v 1.1 2009/07/01 15:38:53 foo Exp $
 * Displays nag
 * @author $Author: foo $
 * @version $Revision: 1.1 $
 */
public class NagScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static final int NAG_DURATION = 300;

	/** Nag screen instance */
	private static NagScreen instance;

	/** Whether to exit or return to the title screen */
	private static boolean exitOnClose;

	/*
	 * Button IDs
	 */
	private static final String BUY = "buy";
	private static final String LATER = "later";

	/*
	 * Transient
	 */

	private transient int tick;

	/**
	 * @param name
	 */
	public NagScreen(String name) {
		super(name);
	}

	@Override
	protected void doRegister() {
		instance = this;
	}

	/**
	 * Show the endgame screen
	 * @param reason The reason we're here, eg. "You've completed the demo!"
	 * @param exitOnClose Exit the game when the screen is dismissed
	 */
	public static void show(String reason, boolean exitOnClose) {
		NagScreen.exitOnClose = exitOnClose;
		if (!instance.isCreated()) {
			try {
				instance.create();
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		instance.open();
	}

	@Override
	protected void onOpen() {
		setVisible(LATER, !exitOnClose);
		tick = 0;
	}

	@Override
	protected void doTick() {
		tick ++;
		if (tick > NAG_DURATION) {
			setVisible(LATER, true);
		}
	}

	@Override
	protected void onClicked(String id) {
		if (BUY.equals(id)) {
			MiniGame.buy(true);
		} else if (LATER.equals(id)) {
			if (exitOnClose) {
				MiniGame.clearBuy();
				Game.exit();
			} else {
				TitleScreen.show();
			}
		}
	}
}
