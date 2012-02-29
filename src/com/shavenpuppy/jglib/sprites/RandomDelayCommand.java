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

import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.Util;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * An animation command that delays for a random amount of time.
 */
public class RandomDelayCommand extends Command {

	private static final long serialVersionUID = 1L;

	/** The minimum time delay, in ticks */
	private int minDelay;

	/** The maximum time delay, in ticks */
	private int maxDelay;

	/**
	 * Constructor for RandomDelayCommand.
	 */
	public RandomDelayCommand() {
		super();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {

		int tick = target.getTick();
		// If the tick is currently 0 it means this is the first execution of
		// the command, so we should pick a random value.
		if (tick == 0) {
			tick = Util.random(minDelay, maxDelay);
		}
		// Otherwise decrement the tick, and if it reaches zero,
		// signal that we want to carry on with the next command by
		// by returning true:
		target.setTick(--tick);

		if (tick == 0) {
			target.setSequence(target.getSequence() + 1);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		if (XMLUtil.hasAttribute(element, "d")) {
			minDelay = XMLUtil.getInt(element, "d");
			maxDelay = minDelay;
		} else {
			minDelay = XMLUtil.getInt(element, "min");
			maxDelay = XMLUtil.getInt(element, "max");
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (minDelay == maxDelay) {
			writer.writeAttribute("d", minDelay, true);
		} else {
			writer.writeAttribute("min", minDelay, true);
			writer.writeAttribute("max", maxDelay, true);
		}
	}

}
