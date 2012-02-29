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

import worm.Res;
import worm.Worm;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.resources.ColorMapFeature;

/**
 * Special title screen that returns mouse pointer to normal
 */
public class TitleScreen extends net.puppygames.applet.screens.TitleScreen {

	private static final long serialVersionUID = 1L;

	/**
	 * C'tor
	 *
	 * @param name
	 */
	public TitleScreen(String name) {
		super(name);
	}

	@Override
	protected void onOpen() {
		super.onOpen();
		setEnabled("exit", true);

		setGroupVisible("not-xmas", !Worm.isXmas());
		setGroupVisible("xmas", Worm.isXmas());

		Worm.setMouseAppearance(Res.getMousePointer());

		ColorMapFeature.getDefaultColorMap().copy((ColorMapFeature) Resources.get("titles.colormap"));
	}

	@Override
	public String getRegistrationID() {
		return ID_REGISTRATION + (Worm.isXmas() ? "-xmas" : "-not-xmas");
	}

	@Override
	public String getVersionID() {
		return ID_VERSION + (Worm.isXmas() ? "-xmas" : "-not-xmas");
	}

}
