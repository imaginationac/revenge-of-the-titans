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
 * Sets flags in an ISprite: flashing, visible, flipped, mirrored.
 */
public class FlagCommand extends Command {

	private static final long serialVersionUID = 1L;

	/** Flashing state */
	private boolean flash, setFlash;

	/** Visible state */
	private boolean visible, setVisible;

	/** Mirror state */
	private boolean mirrored, setMirrored;

	/** Flipped state */
	private boolean flipped, setFlipped;

	/**
	 * C'tor
	 */
	public FlagCommand() {
		super();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {

		target.setSequence(target.getSequence() + 1);
		target.setTick(0);

		if (setFlash) {
			target.setFlash(flash);
		}
		if (setFlipped) {
			target.setFlash(flipped);
		}
		if (setMirrored) {
			target.setMirrored(mirrored);
		}
		if (setVisible) {
			target.setVisible(visible);
		}

		return true; // Execute the next command
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		if (XMLUtil.hasAttribute(element, "flash")) {
			setFlash = true;
			flash = XMLUtil.getBoolean(element, "flash", true);
		}
		if (XMLUtil.hasAttribute(element, "flipped")) {
			setFlipped = true;
			flipped = XMLUtil.getBoolean(element, "flipped", true);
		}
		if (XMLUtil.hasAttribute(element, "mirrored")) {
			setMirrored = true;
			mirrored = XMLUtil.getBoolean(element, "mirrored", true);
		}
		if (XMLUtil.hasAttribute(element, "visible")) {
			setVisible = true;
			visible = XMLUtil.getBoolean(element, "visible", true);
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (setFlash) {
			writer.writeAttribute("flash", flash);
		}
		if (setFlipped) {
			writer.writeAttribute("flipped", flipped);
		}
		if (setMirrored) {
			writer.writeAttribute("mirrored", mirrored);
		}
		if (setVisible) {
			writer.writeAttribute("visible", visible);
		}
	}

}
