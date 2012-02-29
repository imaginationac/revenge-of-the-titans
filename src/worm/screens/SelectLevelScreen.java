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
import net.puppygames.applet.screens.TitleScreen;
import worm.Worm;
import worm.WormGameState;
import worm.features.LevelFeature;
import worm.features.WorldFeature;

/**
 * Select which level you want to play in a particular world.
 */
public class SelectLevelScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static final String ID_BACK = "back";

	/** World, 0..4 */
	private int world;

	/**
	 * C'tor
	 */
	public SelectLevelScreen(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	protected void onOpen() {

		int maxLevel = Worm.getMaxLevelUnlockedInWorld(world);

		Game.playMusic(WorldFeature.getWorld(world).getStream(), 60);

		for (int i = 0; i <= maxLevel; i++) {

			int l = i + world * WormGameState.LEVELS_IN_WORLD;

			String levStr = String.valueOf(l + 1);
			if (l < 9) {
				levStr = "0" + levStr;
			}

			getArea("level.num." + i).setText(levStr);

			setEnabled("level." + i, true);
			if (l == 0) {
				getArea("level." + i).setText("");
			} else {
				getArea("level." + i).setText("$" + Worm.getExtraLevelData(Game.getPlayerSlot(), l, WormGameState.GAME_MODE_CAMPAIGN, "money", 0) );
			}

			//String medals = Worm.getExtraLevelData(l, "medals", "");


			String levelName = LevelFeature.getLevel(l).getTitle().toUpperCase();
			getArea("level.name." + i).setText(levelName);

		}

		// Disable levels we haven't unlocked

		for (int i = maxLevel + 1; i < WormGameState.LEVELS_IN_WORLD; i++) {
			setEnabled("level." + i, false);
			getArea("level." + i).setText("");
		}

	}

	@Override
	protected void onClicked(String id) {
		if (ID_BACK.equals(id)) {
			// If only Earth is unlocked, go back to the title screen. Otherwise, go to the select world screen
			if (Worm.getMaxWorld() == 0) {
				TitleScreen.show();
			} else {
				SelectWorldScreen.show();
			}
		} else if (id.startsWith("level.")) {
			int level = Integer.parseInt(id.substring(6)) + world * WormGameState.LEVELS_IN_WORLD;
			Worm.getGameState().doInit(level);
		}

	}
}
