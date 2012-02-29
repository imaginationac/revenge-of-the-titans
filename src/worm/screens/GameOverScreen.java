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
import net.puppygames.applet.effects.LabelEffect;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.ReadableColor;

import worm.Res;
import worm.Worm;
import worm.WormGameState;

import com.shavenpuppy.jglib.resources.MappedColor;


/**
 * The game over screen is shown when you is deaded
 */
public class GameOverScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static GameOverScreen instance;

	private static final int END_OF_GAME_DURATION = 900;

	private static final String ID_MISSION_STATUS = "mission_status";
	private static final String ID_CANCEL = "cancel";
	private static final String ID_ANALYSIS = "analysis";


	private transient int tick;
	private transient boolean waitForMouse;

	private transient MappedColor endColor;

	/**
	 * C'tor
	 */
	public GameOverScreen(String name) {
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
		waitForMouse = true;
		tick = 0;

		endColor = new MappedColor("gui-bright");

		Area a = getArea(ID_MISSION_STATUS);
		float ypos = a.getBounds().getY() + a.getBounds().getHeight() - 40.0f;
		int delay = 30;


		LabelEffect le = new LabelEffect(net.puppygames.applet.Res.getSmallFont(), Game.getMessage("ultraworm.gameover.mission_failed"), ReadableColor.WHITE, endColor, 180, END_OF_GAME_DURATION * 1000);
		le.setLocation(Game.getWidth() / 2.0f, ypos);
		le.setDelay(delay);
		le.setSound(Res.getEndLevelBonusSound());
		le.spawn(this);
		le.setOffset(null);

		ypos -= 16.0f;
		delay += 60;

		LabelEffect le2 = new LabelEffect(net.puppygames.applet.Res.getTinyFont(), Game.getMessage("ultraworm.gameover.base_destroyed"), ReadableColor.WHITE, endColor, 180, END_OF_GAME_DURATION * 1000);
		le2.setLocation(Game.getWidth() / 2.0f, ypos);
		le2.setDelay(delay);
		le2.setSound(Res.getEndLevelBonusSound());
		le2.spawn(this);
		le2.setOffset(null);

		getArea(ID_ANALYSIS).setText(Worm.getGameState().getAnalysis());

		Worm.setMouseAppearance(Res.getMousePointer());

	}

	@Override
	protected void onClicked(String id) {
		if (ID_CANCEL.equals(id)) {
			close();
		}
	}

	@Override
	protected void onClose() {
		switch (Worm.getGameState().getGameMode()) {
			case WormGameState.GAME_MODE_SURVIVAL:
				SurvivalEndGameScreen.show();
				break;
			case WormGameState.GAME_MODE_XMAS:
				XmasEndGameScreen.show();
				break;
			default:
				EndGameScreen.show();
		}
	}

	@Override
	protected void doTick() {
		if (Mouse.isButtonDown(0)) {
			if (!waitForMouse && tick > 45) {
				// Skip
				tick = END_OF_GAME_DURATION;
			}
		} else {
			waitForMouse = false;
		}
		tick ++;
		if (tick >= END_OF_GAME_DURATION) {
			close();
		}
	}
}
