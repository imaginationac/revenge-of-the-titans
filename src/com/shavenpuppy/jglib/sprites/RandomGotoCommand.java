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
import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.Util;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * An animation command that simply changes the sequence number to
 * a different, random one, picked from a list. The list is specified
 * by a list of <dest seq="n"|id="label" /> tags.
 */
public class RandomGotoCommand extends Command {

	private static final long serialVersionUID = 1L;

	/** The sequence numbers or labels we go to */
	private Object[] destinations;

	/**
	 * Constructor for GotoCommand.
	 */
	public RandomGotoCommand() {
		super();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {
		Object s = destinations[Util.random(0, destinations.length - 1)];
		if (s instanceof String) {
			int d = target.getAnimation().getLabel((String) s);
			if (d != -1) {
				target.setSequence(d);
			} else {
				System.err.println("Animation "+target.getAnimation()+": missing label "+s);
			}
		} else {
			target.setSequence(((Integer) s).intValue());
		}
		return true; // Always go immediately to the next command
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {

		NodeList nl = element.getElementsByTagName("dest");
		LinkedList<Object> seqs = new LinkedList<Object>();
		for (int i = 0; i < nl.getLength(); i ++) {
			Element destElement = (Element) nl.item(i);
			Object d;
			if (XMLUtil.hasAttribute(destElement, "id")) {
				seqs.add(d = XMLUtil.getString(destElement, "id"));
			} else {
				seqs.add(d = new Integer(XMLUtil.getInt(destElement, "seq")));
			}
			if (XMLUtil.hasAttribute(destElement, "n")) {
				int n = XMLUtil.getInt(destElement, "n");
				for (int j = 0; j <  n; j ++) {
					seqs.add(d);
				}
			}
		}
		destinations = new Object[seqs.size()];
		seqs.toArray(destinations);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		for (int i = 0; i < destinations.length; i ++) {
			writer.writeTag("dest");
			if (destinations[i] instanceof String) {
				writer.writeAttribute("id", destinations[i], true);
			} else {
				writer.writeAttribute("seq", ((Integer) destinations[i]).intValue(), true);
			}
			writer.closeTag();
		}
	}

}
