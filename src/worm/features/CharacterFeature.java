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
package worm.features;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Background;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.MappedColor;
import com.shavenpuppy.jglib.resources.ResourceArray;
import com.shavenpuppy.jglib.sprites.Appearance;

/**
 * Defines a character for the story
 * @author Cas
 */
public class CharacterFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Feature data
	 */

	/** Background for the speech bubble */
	private String bubble;

	/** Sound effects */
	private ResourceArray sounds;

	/** Colour of the text */
	private MappedColor color;

	/** Colour of uppercase text */
	private MappedColor boldColor;

	/** Whether the image changes for vowels/consonants etc */
	private boolean animated;

	/** Whether it's speech */
	private boolean speech;

	/** Appearances */
	private Appearance defaultAppearance, vowelAppearance, consonantAppearance;

	/** Mouth layer */
	private int mouthLayer;

	/** Bubble layer */
	private int bubbleLayer;

	/** Text speed */
	private int textSpeed;

	/** Speech speed */
	private int speechSpeed;

	/** Background layers */
	private String idleLayers, talkLayers;

	/** Stop mouth layer from using childOffsets*/
	private boolean suppressChildOffsetMouth;


	/*
	 * Transient data
	 */

	private transient Background bubbleResource;
	private transient LayersFeature idleLayersFeature, talkLayersFeature;

	/**
	 * C'tor
	 * @param name
	 */
	public CharacterFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return Returns the backgroundResource.
	 */
	public Background getBubble() {
		return bubbleResource;
	}

	/**
	 * @return Returns the color.
	 */
	public MappedColor getColor() {
		return color;
	}

	public MappedColor getBoldColor() {
		return boldColor;
	}

	/**
	 * @return true if this is an animated character
	 */
	public boolean isAnimated() {
		return animated;
	}

	/**
	 * Get the sound for a character
	 */
	public ALBuffer getSound(char c) {
		if (sounds == null) {
			return null;
		}
		c %= sounds.getNumResources();
		return (ALBuffer) sounds.getResource(c);
	}

	public Appearance getVowelAppearance() {
		return vowelAppearance;
	}

	public Appearance getConsonantAppearance() {
		return consonantAppearance;
	}

	public Appearance getDefaultAppearance() {
		return defaultAppearance;
	}

	public LayersFeature getIdleLayers() {
		return idleLayersFeature;
	}

	public LayersFeature getTalkLayers() {
		return talkLayersFeature;
	}

	/**
	 * @return the mouth layer
	 */
	public int getMouthLayer() {
		return mouthLayer;
	}

	/**
	 * @return the bubble layer
	 */
	public int getBubbleLayer() {
		return bubbleLayer;
	}

	/**
	 * @return the text speed
	 */
	public int getTextSpeed() {
		return textSpeed;
	}

	/**
	 * @return the speech speed
	 */
	public int getSpeechSpeed() {
		return speechSpeed;
	}

	/**
	 * @return the suppressChildOffsetMouth
	 */
	public boolean getSuppressChildOffsetMouth() {
		return suppressChildOffsetMouth;
	}

	/**
	 * @return the speech
	 */
	public boolean isSpeech() {
		return speech;
	}

}