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
package com.shavenpuppy.jglib.resources;

import org.lwjgl.util.Color;

import com.shavenpuppy.jglib.interpolators.ColorInterpolator;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;

/**
 * $Id: ColorPulseResource.java,v 1.6 2011/04/18 23:28:06 cix_foo Exp $
 * A color pulse has a base colour, a pulse colour, and a frequency.
 * The pulse is 1-cos(time)
 * @author $Author: cix_foo $
 * @version $Revision: 1.6 $
 */
public class ColorPulseResource extends Feature implements ColorSequenceWrapper {

	private static final long serialVersionUID = 1L;

	/*
	 * Feature data
	 */

	/** Base hue */
	private float baseHue;

	/** Pulse hue */
	private float pulseHue;

	/** Saturation */
	private float saturation;

	/** Brightness */
	private float brightness;

	/** Frequency (in Hz) */
	private float frequency;

	/*
	 * Transient data
	 */

	/** Base colour & pulse color */
	private transient Color baseColor, pulseColor;

	/**
	 * C'tor
	 * @param baseHue
	 * @param pulseHue
	 * @param saturation
	 * @param brightness
	 * @param frequency
	 */
	public ColorPulseResource(String name, float baseHue, float pulseHue, float saturation,
			float brightness, float frequency) {
		super(name);
		this.baseHue = baseHue;
		this.pulseHue = pulseHue;
		this.saturation = saturation;
		this.brightness = brightness;
		this.frequency = frequency;
	}

	/**
	 * Create!
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		baseColor = new Color();
		pulseColor = new Color();

		baseColor.fromHSB(baseHue, saturation, brightness);
		pulseColor.fromHSB(pulseHue, saturation, brightness);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.ColorSequenceWrapper#getColor(int, org.lwjgl.util.Color)
	 */
	@Override
	public Color getColor(int tick, Color dest) {
		if (dest == null) {
			dest = new Color();
		}
		float time = frequency * tick;
		float ratio = (((float) Math.cos(time)) + 1.0f) / 2.0f;
		ColorInterpolator.interpolate(baseColor, pulseColor, ratio, LinearInterpolator.instance, dest);
		return dest;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.ColorSequenceWrapper#isFinished(int)
	 */
	@Override
	public boolean isFinished(int tick) {
		// Color pulses never finish
		return false;
	}
}
