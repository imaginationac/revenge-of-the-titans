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
import net.puppygames.applet.screens.TitleScreen;
import worm.Worm;
import worm.WormGameState;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.interpolators.CosineInterpolator;
import com.shavenpuppy.jglib.resources.ColorMapFeature;

/**
 * Select which level you want to play in Endless mode
 */
public class SelectEndlessLevelScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static SelectEndlessLevelScreen instance;

	private static final String ID_BACK = "back";
	private static final int MAX_LEVEL = 39;
	private static final int MAX_ENDLESS_LEVELS = 249;
	private static final String ENDLESS_GROUP = "endless";

	/**
	 * C'tor
	 */
	public SelectEndlessLevelScreen(String name) {
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
	protected void onOpen() {

		// Always use earth color map
		ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get("earth.colormap"));

		setGroupEnabled(ENDLESS_GROUP, false);

		int maxLevel = Worm.getMaxLevel(WormGameState.GAME_MODE_ENDLESS);
		boolean set = false;

		for (int i = 0; i <= MAX_LEVEL; i++) {

			int level = (int) CosineInterpolator.instance.interpolate(0, MAX_ENDLESS_LEVELS - MAX_LEVEL, (float) i / (float) MAX_LEVEL) + i;

			if (level >= maxLevel && !set) {
				set = true;
				level = maxLevel;
			}

			if (i == MAX_LEVEL && maxLevel >= MAX_ENDLESS_LEVELS) {
				level = maxLevel;
			}

			String levStr = String.valueOf(level + 1);
			if (level < 9) {
				levStr = "00" + levStr;
			} else if (level < 99) {
				levStr = "0" + levStr;
			}

			getArea("level.num." + i).setText(levStr);
			if (level <= maxLevel) {
				setEnabled("level.num." + i, true);
				setEnabled("level." + i, true);

				if (i == 0) {
					getArea("level." + i).setText("");
				} else {
					getArea("level." + i).setText("$" + Worm.getExtraLevelData(Game.getPlayerSlot(), level, WormGameState.GAME_MODE_ENDLESS, "money", 0) );
				}
			} else {
				getArea("level." + i).setText("");
			}
		}
	}

	@Override
	protected void onClicked(String id) {
		if (ID_BACK.equals(id)) {
			TitleScreen.show();
		} else if (id.startsWith("level.")) {
			Area area = getArea("level.num."+Integer.parseInt(id.substring(6)));
			int level = Integer.parseInt(area.getText()) - 1;
			Worm.getGameState().doInit(level);
		}

	}
}
