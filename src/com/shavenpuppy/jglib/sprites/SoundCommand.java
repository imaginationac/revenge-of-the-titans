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
package com.shavenpuppy.jglib.sprites;

import java.io.IOException;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Attenuator;
import com.shavenpuppy.jglib.resources.AttenuatorFeature;
import com.shavenpuppy.jglib.sound.SoundEffect;
import com.shavenpuppy.jglib.sound.SoundPlayer;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Play a sound
 */
public class SoundCommand extends Command {

	private static final long serialVersionUID = 1L;

	private static SoundPlayer soundPlayer;
	private static Attenuator defaultAttenuator;

	private String player;
	private String sound;
	private String attenuator;

	private transient ALBuffer soundResource;
	private transient SoundPlayer playerFeature;
	private transient AttenuatorFeature attenuatorFeature;

	/**
	 * Constructor for GotoCommand.
	 */
	public SoundCommand() {
		super();
	}

	/**
	 * Sets the default sound player.
	 * @param soundPlayer
	 */
	public static void setDefaultSoundPlayer(SoundPlayer soundPlayer) {
		SoundCommand.soundPlayer = soundPlayer;
	}

	/**
	 * Sets the default attenuator
	 * @param defaultAttenuator
	 */
	public static void setDefaultAttenuator(Attenuator defaultAttenuator) {
		SoundCommand.defaultAttenuator = defaultAttenuator;
	}

	/**
	 * @return the default SoundPlayer; may be null
	 */
	public static SoundPlayer getDefaultSoundPlayer() {
		return soundPlayer;
	}

	/**
	 * @return the default attenuator; may be null
	 */
	public static Attenuator getDefaultAttenuator() {
		return defaultAttenuator;
	}

	@Override
	public boolean execute(Sprite target) {
		if (target.isVisible() && AL.isCreated()) {
			SoundPlayer p = playerFeature == null ? soundPlayer : playerFeature;
			if (soundResource != null && p != null) {
				SoundEffect fx = p.allocate(soundResource, target);
				if (fx != null) {
					Attenuator att;
					if (attenuatorFeature != null) {
						att = attenuatorFeature;
					} else {
						att = defaultAttenuator;
					}

					if (att != null) {
						Object otarget = target.getOwner();
						if (soundResource.getFormat() == AL10.AL_FORMAT_MONO16 && target instanceof ReadablePosition) {
							ReadablePosition ptarget = (ReadablePosition) otarget;
							float gain = att.getVolume(ptarget.getX(), ptarget.getY());
							fx.setGain(gain * soundResource.getGain(), target);
						} else {
							fx.setGain(att.getVolume(0.0f, 0.0f) * soundResource.getGain(), target);
						}
					}
					fx.play(target);
				}
			}
		}
		target.setSequence(target.getSequence() + 1);
		return true; // Carry on
	}

	@Override
	protected void doCreate() {
		soundResource = (ALBuffer) Resources.get(sound);
		if (player != null) {
			playerFeature = (SoundPlayer) Resources.get(player);
		}
		if (attenuator != null) {
			attenuatorFeature = (AttenuatorFeature) Resources.get(attenuator);
		}
	}

	@Override
	public void archive() {
		player = null;
		attenuator = null;
	}

	@Override
	protected void doDestroy() {
		soundResource = null;
		playerFeature = null;
		attenuatorFeature = null;
	}

	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (player != null) {
			writer.writeAttribute("player", player);
		}
		if (attenuator != null) {
			writer.writeAttribute("attenuator", attenuator);
		}
		writer.writeAttribute("sound", sound);
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		player = XMLUtil.getString(element, "player", null);
		attenuator = XMLUtil.getString(element, "attenuator", null);
		sound = XMLUtil.getString(element, "sound");
	}
}
