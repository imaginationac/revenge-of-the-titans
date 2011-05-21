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

import org.w3c.dom.*;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.XMLResourceWriter;

/**
 * A simple text resource
 */
public class TextResource extends Resource {

	public static final long serialVersionUID = 1L;

	/**
	 * The text
	 */
	private String text;

	/**
	 * C'tor
	 */
	public TextResource() {
	}

	/**
	 * Resource constructor
	 * @param name
	 */
	public TextResource(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		NodeList children = element.getChildNodes();
		if (children.getLength() != 1) {
			throw new Exception("Text resource needs some text!");
		}
		Text node = (Text) children.item(0);
		text = format(node.getNodeValue().trim());
	}

	/**
	 * Strips out whitespace etc. from XML text blocks
	 * @param raw
	 * @return formatted string
	 */
	private String format(String raw) {
		StringBuilder ret = new StringBuilder(raw.length());
		boolean ignoreWhiteSpace = true;
		boolean addSpace = false;
		for (int i = 0; i < raw.length(); i ++) {
			char c = raw.charAt(i);
			if (Character.isWhitespace(c)) {
				if (ignoreWhiteSpace) {
					continue;
				} else {
					ignoreWhiteSpace = true;
					addSpace = true;
				}
			} else {
				ignoreWhiteSpace = false;
				if (addSpace) {
					ret.append(' ');
					addSpace = false;
				}
				ret.append(c);
			}
		}
		return ret.toString();
	}

	/**
	 * @return the text
	 */
	public final String getText() {
		return text;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		writer.writeText(text);
	}

}
