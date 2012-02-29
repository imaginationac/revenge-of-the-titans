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
import worm.features.Setting;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;

/**
 * The New World Screen shows information about a new world
 */
public class NewWorldScreen extends Screen {

	private static final long serialVersionUID = 1L;

	/** The setting effect */
	private transient Setting settingEffect;

	private static final String ID_OK = "ok";
	private static final String ID_BACK = "cancel";

	/**
	 * C'tor
	 */
	public NewWorldScreen(String name) {
		super(name);
		setAutoCreated();
	}

	@Override
	protected void onResized() {
		if (settingEffect!=null) {
			settingEffect.onResized();
		}
	}

	@Override
	protected void onOpen() {
		settingEffect = Worm.getGameState().getWorld().getSettingFeature().spawn(this, Worm.getGameState().getWorld().getStory());
		settingEffect.onResized();
		Game.playMusic(Worm.getGameState().getWorld().getStream(), 60);

		if (Worm.getGameState().getWorld().getIndex() == 1 && !Game.getPlayerSlot().getPreferences().getBoolean("survivalUnlocked", false)) {
			Game.getPlayerSlot().getPreferences().putBoolean("survivalUnlocked", true);
			Game.flushPrefs();
			ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get("moon.colormap"));
			net.puppygames.applet.Res.getInfoDialog().doModal(Game.getMessage("ultraworm.newworld.title"), Game.getMessage("ultraworm.newworld.message"), null);
		}
	}

	@Override
	protected void doCleanup() {
		if (settingEffect != null) {
			settingEffect.remove();
			settingEffect = null;
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
		} else if (ID_OK.equals(id)) {
			close();
			// Begin the level
			Worm.getGameState().beginLevel();
		}

	}
}
