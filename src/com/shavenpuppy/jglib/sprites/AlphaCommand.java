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

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Sets aloha of a transparent thing.
 */
public class AlphaCommand extends Command {

	private static final long serialVersionUID = 1L;

	private int alpha, duration;
	private boolean delta;

	/**
	 * C'tor
	 */
	public AlphaCommand() {
		super();
	}

	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		int currentTick = target.getTick() + 1;

		if (currentTick == 1) {
			if (delta) {
				target.adjustAlpha(alpha);
			} else {
				target.setAlpha(alpha);
			}
		}
		if (currentTick > duration) {
			target.setSequence(++currentSequence);
			target.setTick(0);
			return true; // Execute the next command
		}

		target.setTick(currentTick);
		return false; // Don't execute the next command
	}

	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		String s = XMLUtil.getString(element, "alpha");
		if (s.startsWith("+")) {
			delta = true;
			alpha = Integer.parseInt(s.substring(1));
		} else {
			alpha = Integer.parseInt(s);
		}

		duration = XMLUtil.getInt(element, "d", 0);
	}

	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (delta) {
			writer.writeAttribute("alpha", "+"+alpha);
		} else {
			writer.writeAttribute("alpha", alpha, true);
		}
		writer.writeAttribute("d", duration, true);
	}


}
