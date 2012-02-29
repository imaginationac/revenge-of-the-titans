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
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Decrement the loop counter. If it reaches <= 0, then, simply continue. Otherwise
 * go to the specified location.
 */
public class RepeatCommand extends Command {

	private static final long serialVersionUID = 1L;

	/** The sequence number we go to */
	private int newSequenceNumber;

	/** .. or label */
	private String id;

	/**
	 * Constructor for GotoCommand.
	 */
	public RepeatCommand() {
		super();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {
		target.addLoop(-1);
		if (target.getLoop() > 0) {
			if (id != null) {
				int s = target.getAnimation().getLabel(id);
				if (s != -1) {
					target.setSequence(s);
				} else {
					System.out.println("Label "+id+" not found for repeat in "+target.getAnimation());
				}
			} else {
				target.setSequence(newSequenceNumber);
			}
		} else {
			target.setSequence(target.getSequence() + 1);
		}
		return true; // Always go immediately to the next command
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		if (XMLUtil.hasAttribute(element, "id")) {
			id = XMLUtil.getString(element, "id");
		} else {
			newSequenceNumber = Integer.parseInt(element.getAttribute("seq"));
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (id != null) {
			writer.writeAttribute("id", id, true);
		} else {
			writer.writeAttribute("seq", newSequenceNumber, true);
		}
	}

}
