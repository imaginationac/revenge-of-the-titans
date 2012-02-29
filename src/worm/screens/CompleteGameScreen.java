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

import net.puppygames.applet.MiniGame;
import net.puppygames.applet.Screen;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.lwjgl.input.Mouse;

import worm.features.Setting;
import worm.features.SettingFeature;
import worm.features.StoryFeature;

import com.shavenpuppy.jglib.Resources;

/**
 * The Complete Game Screen ends the game when the player finishes all the levels
 */
public class CompleteGameScreen extends Screen {

	private static final long serialVersionUID = 1L;

	private static CompleteGameScreen instance;

	private static final String ID_OK = "ok";

	private StoryFeature story;
	private SettingFeature setting;

	private transient boolean mouseWasDown;
	private transient Setting settingEffect;

	/**
	 * C'tor
	 */
	public CompleteGameScreen(String name) {
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
		settingEffect = setting.spawn(this, story);
		settingEffect.onResized();
	}

	@Override
	protected void doCleanup() {
		if (settingEffect != null) {
			settingEffect.remove();
			settingEffect = null;
		}
	}



	@Override
	protected void doTick() {
		if (Mouse.isButtonDown(0)) {
			if (!mouseWasDown) {
				EmitterFeature ef = (EmitterFeature) Resources.get("victory.firework.0.emitter");
				Emitter em = ef.spawn(this);
				em.setLocation(getMouseX(), getMouseY());
				mouseWasDown = true;
			}
		} else {
			mouseWasDown = false;
		}
	}

	@Override
	protected void onClicked(String id) {
		if (ID_OK.equals(id)) {
			close();
			MiniGame.endGame();
		}
	}

}
