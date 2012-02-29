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

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Sets color
 */
public class AnimColorCommand extends Command {

	private static final long serialVersionUID = 1L;

	private int red, green, blue, alpha;
	private boolean deltaRed, deltaGreen, deltaBlue, deltaAlpha;
	private int duration;

	/**
	 * C'tor
	 */
	public AnimColorCommand() {
		super();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		int currentTick = target.getTick() + 1;

		if (currentTick == 1) {
			adjust(target, 0);
			adjust(target, 1);
			adjust(target, 2);
			adjust(target, 3);
		}
		if (currentTick > duration) {
			target.setSequence(++currentSequence);
			target.setTick(0);
			return true; // Execute the next command
		}

		target.setTick(currentTick);
		return false; // Don't execute the next command
	}

	private void adjust(Sprite colored, int index) {
		ReadableColor c = colored.getColor(index);
		Color newC = new Color
			(
				deltaRed ? Math.min(255, Math.max(0, c.getRed() + red)) : red,
				deltaGreen ? Math.min(255, Math.max(0, c.getGreen() + green)) : green,
				deltaBlue ? Math.min(255, Math.max(0, c.getBlue() + blue)) : blue,
				deltaAlpha ? Math.min(255, Math.max(0, c.getAlpha() + alpha)) : alpha
			);
		colored.setColor(index, newC);
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		String redS = XMLUtil.getString(element, "r", "+0");
		if (redS.startsWith("+")) {
			deltaRed = true;
			red = Integer.parseInt(redS.substring(1));
		} else {
			red = Integer.parseInt(redS);
		}
		String greenS = XMLUtil.getString(element, "g", "+0");
		if (greenS.startsWith("+")) {
			deltaGreen = true;
			green = Integer.parseInt(greenS.substring(1));
		} else {
			green = Integer.parseInt(greenS);
		}
		String blueS = XMLUtil.getString(element, "b", "+0");
		if (blueS.startsWith("+")) {
			deltaBlue = true;
			blue = Integer.parseInt(blueS.substring(1));
		} else {
			blue = Integer.parseInt(blueS);
		}
		String alphaS = XMLUtil.getString(element, "a", "+0");
		if (alphaS.startsWith("+")) {
			deltaAlpha = true;
			alpha = Integer.parseInt(alphaS.substring(1));
		} else {
			alpha = Integer.parseInt(alphaS);
		}

		duration = XMLUtil.getInt(element, "d", 0);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (deltaRed) {
			writer.writeAttribute("r", "+"+red);
		} else {
			writer.writeAttribute("r", red, true);
		}
		if (deltaGreen) {
			writer.writeAttribute("g", "+"+green);
		} else {
			writer.writeAttribute("g", green, true);
		}
		if (deltaBlue) {
			writer.writeAttribute("b", "+"+blue);
		} else {
			writer.writeAttribute("b", blue, true);
		}
		if (deltaAlpha) {
			writer.writeAttribute("a", "+"+alpha);
		} else {
			writer.writeAttribute("a", alpha, true);
		}
		writer.writeAttribute("d", duration, true);
	}


}
