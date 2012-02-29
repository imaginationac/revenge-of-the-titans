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
import worm.features.LevelFeature;
import worm.features.WorldFeature;

import com.shavenpuppy.jglib.Resources;

/**
 * Select which world you want to play. Only comes into play when you've completed Earth. After selection,
 * it opens the MapScreen and allows the player to choose an unlocked level from there.
 */
public class SelectWorldScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static SelectWorldScreen instance;

	private static final String ID_CANCEL = "cancel";

	/**
	 * C'tor
	 */
	public SelectWorldScreen(String name) {
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

	/**
	 * Shows the screen
	 */
	public static void show() {
		instance.open();
	}

	@Override
	protected void onOpen() {

		int maxWorld = Worm.getMaxWorld();
		int n = Math.min(WorldFeature.getNumWorlds(), maxWorld + 1);
		for (int i = 0; i < n; i ++) {

			String maxLevel;
			if (i < maxWorld) {
				maxLevel = Game.getMessage("world.secure.text");
			} else {
				maxLevel = "> " +LevelFeature.getLevel(Worm.getMaxLevelUnlockedInWorld(i)+i*10).getTitle();
			}

			setEnabled("world."+i, true);
			getArea("world."+i).setText(maxLevel);

			getArea("world.num."+i).setText("0"+String.valueOf(i+1));

			String worldName = WorldFeature.getWorld(i).getTitle();
			getArea("world.name."+i).setText(worldName);

		}

		// Disable worlds we haven't unlocked

		for (int i = maxWorld + 1; i < 5; i ++) {

			setEnabled("world."+i, false);

		}


	}


	/* (non-Javadoc)
	 * @see net.puppygames.applet.Screen#onClicked(java.lang.String)
	 */
	@Override
	protected void onClicked(String id) {
		if (ID_CANCEL.equals(id)) {
			TitleScreen.show();
		}
		if (id.startsWith("world.")) {
			int world = Integer.parseInt(id.substring(6));
			SelectLevelScreen sls = (SelectLevelScreen) Resources.get("select.level.screen."+world);
			sls.open();
		}
	}
}
