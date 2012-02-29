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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.interpolators.LinearInterpolator;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * A sequence of colors. Each color blends into the next one over a number
 * of frames.
 */
public class ColorSequenceResource extends Resource implements ColorSequenceWrapper {

	private static final long serialVersionUID = 1L;

	/** The sequence */
	private ArrayList<SequenceEntry> sequence;

	/** Total duration (calculated) */
	private transient int duration;

	/** Repeat style */
	private int style;

	/** Index of durations */
	private transient int[] index;

	/*
	 * Repeat styles
	 */

	/** The sequence simply stays on the last color at the end */
	public static final int STOP = 0;

	/** The sequence repeats from the beginning */
	public static final int REPEAT = 1;

	/** Each entry in the sequence is a SequenceEntry */
	public static class SequenceEntry implements Serializable {

		private static final long serialVersionUID = 1L;

		private ReadableColor color;
		private int duration; // The number of frames to hold this color for
		private int fade; // The number of frames over which to fade into the next color

		private SequenceEntry() {
		}

		public SequenceEntry(
			ReadableColor color,
			int duration,
			int fade
		) {
			this.color = color;
			this.duration = duration;
			this.fade = fade;
		}
	}

	/**
	 * Constructor for ColorSequence.
	 */
	public ColorSequenceResource(
		SequenceEntry[] entries,
		int style
	) {
		super();

		sequence = new ArrayList<SequenceEntry>(entries.length);
		for (int i = 0; i < entries.length; i ++) {
			sequence.add(entries[i]);
		}
		this.style = style;
		calcDuration();
	}

	/**
	 * Constructor for ColorSequence.
	 */
	public ColorSequenceResource() {
	}

	/**
	 * Constructor for ColorSequence.
	 * @param name
	 */
	public ColorSequenceResource(String name) {
		super(name);
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		NodeList children = element.getElementsByTagName("color");
		int n = children.getLength();
		sequence = new ArrayList<SequenceEntry>(n);
		for (int i = 0; i < n; i ++) {
			Element colorElement = (Element) children.item(i);
			SequenceEntry seq = new SequenceEntry();
			seq.color = new Color(Integer.parseInt(colorElement.getAttribute("r")),
				Integer.parseInt(colorElement.getAttribute("g")),
				Integer.parseInt(colorElement.getAttribute("b")),
				Integer.parseInt(colorElement.getAttribute("a"))
			);
			seq.duration = Integer.parseInt(colorElement.getAttribute("d"));
			seq.fade = Integer.parseInt(colorElement.getAttribute("f"));
			sequence.add(seq);
		}
		style = decode(XMLUtil.getString(element, "style", "stop"));
		calcDuration();
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		writer.writeAttribute("style", style == STOP ? "stop" : "repeat");
		for (SequenceEntry se : sequence) {
			writer.writeTag("color");
			writer.writeAttribute("r", se.color.getRed());
			writer.writeAttribute("g", se.color.getGreen());
			writer.writeAttribute("b", se.color.getBlue());
			writer.writeAttribute("a", se.color.getAlpha());
			writer.writeAttribute("d", se.duration);
			writer.writeAttribute("f", se.fade);
			writer.closeTag();
		}
	}

	/**
	 * Decode a style
	 * @param style The style name
	 * @return a style constant
	 * @throws Exception if the style is not recognised
	 */
	public static int decode(String styleS) throws Exception {
		if (styleS.equalsIgnoreCase("repeat")) {
			return REPEAT;
		} else if (styleS.equalsIgnoreCase("stop")) {
			return STOP;
		} else {
			// TODO: Add random style
			throw new Exception("Illegal style '"+styleS+"'");
		}
	}

	/**
	 * @return true if the color sequence is looped
	 */
	public boolean isLooped() {
		return style == REPEAT;
	}

	/**
	 * Determine the color at a particular point in time.
	 * @param time The time (in frames)
	 * @param color A destination color, or null if a new one is to be created
	 * @return color, or a new color, containing the interpolated color.
	 */
	@Override
	public Color getColor(int time, Color color) {
		if (index == null) {
			calcDuration();
		}

		if (color == null) {
			color = new Color();
		}


		if (time < 0) {
			time = 0;
		}

		if (style == REPEAT) {
			time %= duration;
		}

		// Determine where we are using the index and a binary search
		int idx = Arrays.binarySearch(index, time);

		if (idx < 0) {
			idx = -2 - idx;
		}

		if (idx == index.length) {
			idx --;
		}


		SequenceEntry seq = sequence.get(idx);
		time -= index[idx];
		int fade = seq.duration - seq.fade;
		if (time <= fade || (idx == index.length - 1 && style == STOP)) {
			color.setColor(seq.color);
		} else {
			float fadeRatio = ((float)(time - fade)) / (float)seq.fade;
			ReadableColor a = seq.color;
			if (idx == index.length - 1) {
				idx = 0;
			} else {
				idx ++;
			}
			ReadableColor b = (sequence.get(idx)).color;
			color.set(
				(int)LinearInterpolator.instance.interpolate(a.getRed(), b.getRed(), fadeRatio),
				(int)LinearInterpolator.instance.interpolate(a.getGreen(), b.getGreen(), fadeRatio),
				(int)LinearInterpolator.instance.interpolate(a.getBlue(), b.getBlue(), fadeRatio),
				(int)LinearInterpolator.instance.interpolate(a.getAlpha(), b.getAlpha(), fadeRatio)
			);
		}


		return color;
	}

	private void calcDuration() {
		duration = 0;
		index = new int[sequence.size()];
		int count = 0;
		for (SequenceEntry seq : sequence) {
			index[count ++] = duration;
			duration += seq.duration;
		}
	}

	/**
	 * @see com.shavenpuppy.jglib.GenericResource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		index = null;
	}

	/**
	 * @return the duration of the color sequence
	 */
	public final int getDuration() {
		assert isCreated();
		return duration;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.ColorSequenceWrapper#isFinished(int)
	 */
	@Override
	public boolean isFinished(int tick) {
		if (isLooped()) {
			return false;
		} else {
			return tick >= duration;
		}
	}

}
