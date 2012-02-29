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

import worm.Worm;

/**
 * Options screen with additional mouse speed diddler
 */
public class OptionsScreen extends net.puppygames.applet.screens.OptionsScreen {

	private static final long serialVersionUID = 1L;

	private static final String SHOW_TOOLTIPS_ON = "show_tooltips_on";
	private static final String SHOW_TOOLTIPS_OFF = "show_tooltips_off";
	private static final String SHOW_INFO_ON = "show_info_on";
	private static final String SHOW_INFO_OFF = "show_info_off";
	private static final String SHOW_HINTS_ON = "show_hints_on";
	private static final String SHOW_HINTS_OFF = "show_hints_off";
	private static final String AUTO_DIFFICULTY_ON = "auto_difficulty_on";
	private static final String AUTO_DIFFICULTY_OFF = "auto_difficulty_off";


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
		if (SHOW_TOOLTIPS_ON.equals(id)) {
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
		onResized();
		setEnabled("exit", false);
		
		setGroupVisible("not-xmas", !Worm.isXmas());
		setGroupVisible("xmas", Worm.isXmas());
	}

	@Override
	protected void onResized() {
		super.onResized();
		enableButtons();
	}

}
