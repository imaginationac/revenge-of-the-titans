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

import java.io.*;
import java.net.URL;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.*;
/**
 * A WaveResource is a pointer to a Wave held remotely, either at the other
 * end of a URL, or in the classpath.
 *
 * @author foo
 */
public class WaveResource extends Resource implements WaveWrapper {

	private static final long serialVersionUID = 1L;

	/*
	 * Resource data
	 */

	/** The wave's location, which is a URL. For classpath resources, use classpath: as the URL */
	private String url;

	/*
	 * Transient data
	 */

	/** The loaded Wave */
	private transient Wave wave;

	/** Frequency */
	private transient int frequency;

	/** Type */
	private transient int type;

	/**
	 * Resource constructor
	 * @param name
	 */
	public WaveResource(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		wave = null;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		url = element.getAttribute("url");
	}

	/**
	 * @return the wave
	 * @throws Exception
	 */
	@Override
	public final Wave getWave() throws Exception {
		assert isCreated();
		if (wave == null || wave.getData() == null) {
			wave = Wave.read(getStream());
		}
		return wave;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getStream()
	 */
	@Override
	public InputStream getStream() throws Exception {
		BufferedInputStream bis;
		if (url.startsWith("classpath:")) {
			bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(url.substring(10)), 128000);
		} else {
			bis = new BufferedInputStream(new URL(url).openStream(), 128000);
		}
		return bis;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getFrequency()
	 */
	@Override
	public int getFrequency() {
		return frequency;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getType()
	 */
	@Override
	public int getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		writer.writeAttribute("url", url);
	}
}
