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
import net.puppygames.applet.widgets.PowerDisplay;
import net.puppygames.applet.widgets.PowerDisplayFeature;

import org.lwjgl.opengl.Display;

import worm.Worm;

/**
 * Options screen with additional mouse speed diddler
 */
public class OptionsScreen extends net.puppygames.applet.screens.OptionsScreen {

	private static final String MOUSESPEED = "mousespeed";

	private static final String SMALLSCREEN_ON = "smallscreen_on";
	private static final String SMALLSCREEN_OFF = "smallscreen_off";
	private static final String MEDIUMSCREEN_ON = "mediumscreen_on";
	private static final String MEDIUMSCREEN_OFF = "mediumscreen_off";
	private static final String LARGESCREEN_ON = "largescreen_on";
	private static final String LARGESCREEN_OFF = "largescreen_off";

	private static final String SHOW_TOOLTIPS_ON = "show_tooltips_on";
	private static final String SHOW_TOOLTIPS_OFF = "show_tooltips_off";
	private static final String SHOW_INFO_ON = "show_info_on";
	private static final String SHOW_INFO_OFF = "show_info_off";
	private static final String SHOW_HINTS_ON = "show_hints_on";
	private static final String SHOW_HINTS_OFF = "show_hints_off";
	private static final String AUTO_DIFFICULTY_ON = "auto_difficulty_on";
	private static final String AUTO_DIFFICULTY_OFF = "auto_difficulty_off";


	/*
	 * Resource data
	 */

	private PowerDisplayFeature mouseSpeedPowerDisplay;


	/*
	 * Transient data
	 */

	private transient PowerDisplay mouseSpeedPowerDisplayInstance;

	/**
	 * C'tor
	 * @param name
	 */
	public OptionsScreen(String name) {
		super(name);
	}

	@Override
	protected void onClicked(String id) {
		super.onClicked(id);
		if (MOUSESPEED.equals(id)) {
			int speed = mouseSpeedPowerDisplayInstance.getBarAt(getMouseX(), getMouseY());
			if (speed >= 0) {
				mouseSpeedPowerDisplayInstance.setUsed(speed + 1);
				Worm.setMouseSpeed(speed + 1);
			}
		} else if (SMALLSCREEN_OFF.equals(id)) {
			Game.setWindowSize(1.0f);
			enableButtons();
		} else if (MEDIUMSCREEN_OFF.equals(id)) {
			Game.setWindowSize(1.5f);
			enableButtons();
		} else if (LARGESCREEN_OFF.equals(id)) {
			Game.setWindowSize(2.0f);
			enableButtons();
		} else if (SHOW_TOOLTIPS_ON.equals(id)) {
			Worm.setShowTooltips(false);
			enableButtons();
		} else if (SHOW_TOOLTIPS_OFF.equals(id)) {
			Worm.setShowTooltips(true);
			enableButtons();
		} else if (SHOW_INFO_ON.equals(id)) {
			Worm.setShowInfo(false);
			enableButtons();
		} else if (SHOW_INFO_OFF.equals(id)) {
			Worm.setShowInfo(true);
			enableButtons();
		} else if (SHOW_HINTS_ON.equals(id)) {
			Worm.setShowHints(false);
			enableButtons();
		} else if (SHOW_HINTS_OFF.equals(id)) {
			Worm.setShowHints(true);
			enableButtons();
		} else if (AUTO_DIFFICULTY_ON.equals(id)) {
			Worm.setAutoDifficulty(false);
			enableButtons();
		} else if (AUTO_DIFFICULTY_OFF.equals(id)) {
			Worm.setAutoDifficulty(true);
			enableButtons();
		}

	}

	@Override
	protected void enableButtons() {
		super.enableButtons();

		setVisible(SMALLSCREEN_ON, Game.getWindowSize() == 1.0f && !Display.isFullscreen() && !Game.isCustomDisplayMode());
		setVisible(SMALLSCREEN_OFF, (Game.getWindowSize() != 1.0f || Display.isFullscreen()) && !Game.isCustomDisplayMode());
		setVisible(MEDIUMSCREEN_ON, Game.getWindowSize() == 1.5f && !Display.isFullscreen() && !Game.isCustomDisplayMode());
		setVisible(MEDIUMSCREEN_OFF, (Game.getWindowSize() != 1.5f || Display.isFullscreen()) && !Game.isCustomDisplayMode());
		setVisible(LARGESCREEN_ON, Game.getWindowSize() == 2.0f && !Display.isFullscreen() && !Game.isCustomDisplayMode());
		setVisible(LARGESCREEN_OFF, (Game.getWindowSize() != 2.0f || Display.isFullscreen()) && !Game.isCustomDisplayMode());

		getArea(SMALLSCREEN_ON).init();
		getArea(SMALLSCREEN_OFF).init();
		getArea(MEDIUMSCREEN_ON).init();
		getArea(MEDIUMSCREEN_OFF).init();
		getArea(LARGESCREEN_ON).init();
		getArea(LARGESCREEN_OFF).init();

		setVisible(SHOW_TOOLTIPS_ON, 	 Worm.getShowTooltips());
		setVisible(SHOW_TOOLTIPS_OFF, 	!Worm.getShowTooltips());
		setVisible(SHOW_INFO_ON, 		 Worm.getShowInfo());
		setVisible(SHOW_INFO_OFF, 		!Worm.getShowInfo());
		setVisible(SHOW_HINTS_ON, 		 Worm.getShowHints());
		setVisible(SHOW_HINTS_OFF, 		!Worm.getShowHints());
		setVisible(AUTO_DIFFICULTY_ON, 	 Worm.getAutoDifficulty());
		setVisible(AUTO_DIFFICULTY_OFF, !Worm.getAutoDifficulty());

		getArea(SHOW_TOOLTIPS_ON).init();
		getArea(SHOW_TOOLTIPS_OFF).init();
		getArea(SHOW_INFO_ON).init();
		getArea(SHOW_INFO_OFF).init();
		getArea(SHOW_HINTS_ON).init();
		getArea(SHOW_HINTS_OFF).init();
		getArea(AUTO_DIFFICULTY_ON).init();
		getArea(AUTO_DIFFICULTY_OFF).init();

	}

	@Override
	protected void onOpen() {
		super.onOpen();
		mouseSpeedPowerDisplayInstance = mouseSpeedPowerDisplay.spawn(this);
		mouseSpeedPowerDisplayInstance.setUsed(Worm.getMouseSpeed());
		onResized();
		setEnabled("exit", false);
	}

	@Override
	protected void doCleanup() {
		super.doCleanup();
		mouseSpeedPowerDisplayInstance.cleanup();
		mouseSpeedPowerDisplayInstance = null;
	}

	@Override
	protected void onResized() {
		super.onResized();
		if (mouseSpeedPowerDisplayInstance != null) {
			mouseSpeedPowerDisplayInstance.onResized();
		}
		enableButtons();
	}

}
