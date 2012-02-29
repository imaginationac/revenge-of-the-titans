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
import net.puppygames.applet.widgets.PowerDisplay;
import net.puppygames.applet.widgets.PowerDisplayFeature;

import org.lwjgl.opengl.Display;

/**
 * $Id: OptionsScreen.java,v 1.3 2010/06/22 22:24:30 foo Exp $
 * Takes care of a few common options like music and sfx volume control
 * @author $Author: foo $
 * @version $Revision: 1.3 $
 */
public class OptionsScreen extends Screen {

	private static final long serialVersionUID = 1L;

	/*
	 * Static data
	 */

	/** Singleton */
	private static OptionsScreen instance;

	/*
	 * Areas
	 */
	private static final String MUSICVOLUME = "musicvolume";
	private static final String SFXVOLUME = "sfxvolume";
	private static final String FULLSCREEN_ON = "fullscreen_on";
	private static final String FULLSCREEN_OFF = "fullscreen_off";
	private static final String ONLINE_HISCORES_ON = "online_hiscores_on";
	private static final String ONLINE_HISCORES_OFF = "online_hiscores_off";
	private static final String OPTIONS_HISCORES_PANEL = "options_hiscores_panel";

	/*
	 * Resource data
	 */

	private PowerDisplayFeature musicVolumePowerDisplay;
	private PowerDisplayFeature sfxVolumePowerDisplay;

	/*
	 * Transient data
	 */

	private transient PowerDisplay
		musicVolumePowerDisplayInstance,
		sfxVolumePowerDisplayInstance;

	/**
	 * C'tor
	 */
	public OptionsScreen(String name) {
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
	 * Show the options screen
	 */
	public static void show() {
		instance.open();
	}

	@Override
	protected void onOpen() {
		GenericButtonHandler.onOpen(this);
		musicVolumePowerDisplayInstance = musicVolumePowerDisplay.spawn(this);
		sfxVolumePowerDisplayInstance = sfxVolumePowerDisplay.spawn(this);
		musicVolumePowerDisplayInstance.setUsed((int)(Game.getMusicVolume() * musicVolumePowerDisplayInstance.getMax()) + 1);
		sfxVolumePowerDisplayInstance.setUsed((int)(Game.getSFXVolume() * sfxVolumePowerDisplayInstance.getMax()) + 1);
		setVisible(OPTIONS_HISCORES_PANEL, !MiniGame.getDontUseRemoteHiscores());
		enableButtons();
		onResized();
	}

	@Override
	protected void onClose() {
		Game.flushPrefs();
	}

	protected void enableButtons() {
		getArea(FULLSCREEN_ON).init();
		getArea(FULLSCREEN_OFF).init();
		getArea(ONLINE_HISCORES_ON).init();
		getArea(ONLINE_HISCORES_OFF).init();
		setEnabled(FULLSCREEN_ON, true);
		setEnabled(FULLSCREEN_OFF, true);
		setVisible(FULLSCREEN_ON, Display.isFullscreen());
		setVisible(FULLSCREEN_OFF, !Display.isFullscreen());
		setVisible(ONLINE_HISCORES_ON, MiniGame.getSubmitRemoteHiscores() && !MiniGame.getDontUseRemoteHiscores());
		setVisible(ONLINE_HISCORES_OFF, !MiniGame.getSubmitRemoteHiscores() && !MiniGame.getDontUseRemoteHiscores());
	}

	@Override
	protected void doCleanup() {
		musicVolumePowerDisplayInstance.cleanup();
		sfxVolumePowerDisplayInstance.cleanup();
		musicVolumePowerDisplayInstance = null;
		sfxVolumePowerDisplayInstance = null;
	}

	@Override
	protected void onClicked(String id) {
		GenericButtonHandler.onClicked(id);

		if (MUSICVOLUME.equals(id)) {
			int vol = musicVolumePowerDisplayInstance.getBarAt(getMouseX(), getMouseY());
			if (vol >= 0) {
				musicVolumePowerDisplayInstance.setUsed(vol + 1);
				Game.setMusicVolume((float) vol / (float) (musicVolumePowerDisplayInstance.getMax() - 1));
			}
		} else if (SFXVOLUME.equals(id)) {
			int vol = sfxVolumePowerDisplayInstance.getBarAt(getMouseX(), getMouseY());
			if (vol >= 0) {
				sfxVolumePowerDisplayInstance.setUsed(vol + 1);
				Game.setSFXVolume((float) vol / (float) (sfxVolumePowerDisplayInstance.getMax() - 1));
			}
		} else if (FULLSCREEN_ON.equals(id)) {
			try {
				Game.setFullscreen(false);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			enableButtons();
		} else if (FULLSCREEN_OFF.equals(id)) {
			try {
				Game.setFullscreen(true);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			enableButtons();
		} else if (ONLINE_HISCORES_ON.equals(id)) {
			MiniGame.setSubmitRemoteHiscores(false);
			enableButtons();
		} else if (ONLINE_HISCORES_OFF.equals(id)) {
			MiniGame.setSubmitRemoteHiscores(true);
			enableButtons();
		}
	}

	@Override
	protected void onResized() {
		if (musicVolumePowerDisplayInstance != null) {
			musicVolumePowerDisplayInstance.onResized();
		}
		if (sfxVolumePowerDisplayInstance != null) {
			sfxVolumePowerDisplayInstance.onResized();
		}
	}
}
