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
package com.shavenpuppy.jglib.openal;

import java.io.*;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.*;
import com.shavenpuppy.jglib.resources.WaveWrapper;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: SoundBank.java,v 1.9 2011/04/18 23:28:06 cix_foo Exp $
 * A great big buffer of sounds.
 * @author $Author: cix_foo $
 * @version $Revision: 1.9 $
 */
public class SoundBank extends Resource implements WaveWrapper {

	private static final long serialVersionUID = 1L;

	/** Source, which is the URL of a WaveWrapper */
	private String url;

	/*
	 * Transient data
	 */

	/** The wave data. */
	private transient Wave wave;

	/**
	 * C'tor
	 */
	public SoundBank() {
	}

	/**
	 * Named c'tor
	 * @param name
	 */
	public SoundBank(String name) {
		super(name);
	}

	@Override
	public void archive() {
		url = null;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {
		try {
			// Load in the wave
			WaveWrapper waveResource = null;

			if (url.startsWith("classpath:")) {
				// Load directly from a serialised Image in the classpath
				try {
					wave = Wave.read(new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(url.substring(10))));
				} catch (IOException e) {
					System.out.println("Failed to load system resource: "+url.substring(10));
					throw e;
				}
			} else if (url.startsWith("resource:")) {
				// Load directly from Resources
				waveResource = (WaveWrapper) Resources.get(url.substring(9));
			} else {
				// Load from a URL
				wave = Wave.read(new BufferedInputStream(new URL(url).openStream()));
			}

			if (waveResource != null) {
				wave = waveResource.getWave();
			}

			if (wave == null) {
				throw new Exception("No wave specified for "+this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		url = XMLUtil.getString(element, "url");
		List<Element> children = XMLUtil.getChildren(element, "clip");
		for (Element clipElement : children) {
			SoundClip clip = new SoundClip(XMLUtil.getString(clipElement, "name"), this);
			clip.load(clipElement, loader);
			Resources.put(clip);
		}
	}

	/**
	 * @return the Wave
	 */
	@Override
	public Wave getWave() throws Exception {
		assert isCreated();
		if (wave.getData() == null) {
			// Reload the wave
			doCreate();
		}
		return wave;
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getFrequency()
	 */
	@Override
	public int getFrequency() {
		return wave.getFrequency();
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getType()
	 */
	@Override
	public int getType() {
		return wave.getType();
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.WaveWrapper#getStream()
	 */
	@Override
	public InputStream getStream() throws Exception {
		return null;
	}
}
