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
package worm;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

import worm.features.LevelColorsFeature;
import worm.features.LevelFeature;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.interpolators.LinearInterpolator;

/**
 * Attenuated color - blends two ReadableColors together by a factor
 */
public class AttenuatedColor implements ReadableColor, Serializable {

	private static final long serialVersionUID = 1L;

	/** Attenuation tiles colour name */
	public static final String ATTENUATE = "attenuate";

	/** Attenuation alpha colour name */
	public static final String MAX_ATTENUATION = "maxAttenuation";

	/** Attenuation colour */
	private static ReadableColor attenuation = new Color();

	private ReadableColor color;
	private float ratio;
	private int tick, update;
	private int red, green, blue, alpha;
	private int fadeLevel;
	private int maxFadeLevel;
	private boolean isShadow;

	public AttenuatedColor(ReadableColor color) {
		this(color, 0, 1, 0.0f, 128, false); // Update attenuation only every 128 RGBA reads or so
	}

	public AttenuatedColor(ReadableColor color, boolean isShadow) {
		this(color, 0, 1, 0.0f, 128, true); // Update attenuation only every 128 RGBA reads or so
	}

	public AttenuatedColor(ReadableColor color, int fadeLevel, int maxFadeLevel, float ratio, int updateFrequency, boolean isShadow) {
		this.color = color;
		this.fadeLevel = fadeLevel;
		this.maxFadeLevel = maxFadeLevel;
		this.isShadow = isShadow;
		if (Worm.getGameState() == null || Worm.getGameState().getLevelFeature() == null) {
			this.ratio = ratio; // bodge
		} else {
			this.ratio = (float) (ratio * Worm.getGameState().getLevelFeature().getColors().getColor(MAX_ATTENUATION).getAlpha()/255.0);
		}
		if (isShadow) {
			ratio = Math.min(0.5f, ratio);
		}
		this.update = updateFrequency;
		this.tick = update;
		if (update == 0) {
			doCalc();
		}
	}

	public void setRatio(float ratio) {
		LevelFeature levelFeature = Worm.getGameState().getLevelFeature();
		if (levelFeature == null) {
			// Not ready yet - probably a result of resizing the game window during a Create Easier Level maneouvre...
			return;
		}
		LevelColorsFeature colors = levelFeature.getColors();
		ReadableColor color = colors.getColor(MAX_ATTENUATION);
		this.ratio = (float) (ratio * color.getAlpha()/255.0);
		if (ratio < 0.0f) {
			ratio = 0.0f;
		} else if (ratio > 1.0f) {
			ratio = 1.0f;
		}
		if (isShadow) {
			ratio = Math.min(0.5f, ratio);
		}
		tick = update;
	}

	public void setFade(int fade) {
		this.fadeLevel = fade;
		maxFadeLevel = 5;
		tick = update;
	}

	private void maybeCalc() {
		if (update == 0 && !GameScreen.isDiddlerOpen()) {
			return;
		}
		tick ++;
		if (tick >= update) {
			tick = 0;
			doCalc();
		}
	}

	private void doCalc() {
		double ft = ratio * Math.PI * 0.5;
		float mf = (float) Math.cos(ft);
		float f = 1.0f - mf;
		float fade = LinearInterpolator.instance.interpolate(1.0f, 0.0f, (float) fadeLevel / maxFadeLevel);
		red = (int) (fade * (color.getRed() * mf + attenuation.getRed() * f));
		green = (int) (fade * (color.getGreen() * mf + attenuation.getGreen() * f));
		blue = (int) (fade * (color.getBlue() * mf + attenuation.getBlue() * f));
		alpha = (int) (color.getAlpha() * mf + attenuation.getAlpha() * f);
	}

	@Override
	public int getAlpha() {
		return alpha;
	}

	@Override
	public byte getAlphaByte() {
		return (byte) getAlpha();
	}

	@Override
	public int getBlue() {
		return blue;
	}

	@Override
	public byte getBlueByte() {
		return (byte) getBlue();
	}

	@Override
	public int getGreen() {
		return green;
	}

	@Override
	public byte getGreenByte() {
		return (byte) getGreen();
	}

	@Override
	public int getRed() {
		maybeCalc();
		return red;
	}

	@Override
	public byte getRedByte() {
		return (byte) getRed();
	}

	@Override
	public void writeABGR(ByteBuffer dest) {
		dest.put(getAlphaByte());
		dest.put(getBlueByte());
		dest.put(getGreenByte());
		dest.put(getRedByte());
	}

	@Override
	public void writeARGB(ByteBuffer dest) {
		dest.put(getAlphaByte());
		dest.put(getRedByte());
		dest.put(getGreenByte());
		dest.put(getBlueByte());
	}

	@Override
	public void writeBGR(ByteBuffer dest) {
		dest.put(getBlueByte());
		dest.put(getGreenByte());
		dest.put(getRedByte());
	}

	@Override
	public void writeBGRA(ByteBuffer dest) {
		dest.put(getBlueByte());
		dest.put(getGreenByte());
		dest.put(getRedByte());
		dest.put(getAlphaByte());
	}

	@Override
	public void writeRGB(ByteBuffer dest) {
		dest.put(getRedByte());
		dest.put(getGreenByte());
		dest.put(getBlueByte());
	}

	@Override
	public void writeRGBA(ByteBuffer dest) {
		dest.put(getRedByte());
		dest.put(getGreenByte());
		dest.put(getBlueByte());
		dest.put(getAlphaByte());
	}

	public static void setAttenuation(ReadableColor attenuation) {
		AttenuatedColor.attenuation = attenuation;
	}

}
