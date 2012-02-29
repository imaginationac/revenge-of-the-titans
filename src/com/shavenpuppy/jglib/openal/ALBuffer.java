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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.Wave;
import com.shavenpuppy.jglib.resources.WaveWrapper;
import com.shavenpuppy.jglib.util.XMLUtil;

import static org.lwjgl.openal.AL10.*;

/**
 * A sound wave
 */
public class ALBuffer extends Resource implements ALBufferID {

	private static final long serialVersionUID = 1L;

	/** The source wave file URL, which can also be classpath: or resource: */
	protected String url;

	/** Default gain */
	protected float gain;

	/** Default pitch */
	protected float pitch;

	/** Priority */
	protected int priority;

	/** Looped */
	protected boolean looped;

	/*
	 * Transient data
	 */

	/** The buffer ID */
	private transient int buffer;

	/** A source image resource */
	private transient WaveWrapper waveResource;

	/** A source image */
	private transient Wave wave;

	/** AL Format */
	private transient int format;

	/**
	 * Constructor for ALWave.
	 */
	public ALBuffer() {
		super();
	}

	/**
	 * Constructor for ALWave.
	 * @param name
	 */
	public ALBuffer(String name) {
		super(name);
	}

	/**
	 * Constructor
	 * @param wave the sampled wave to use
	 */
	public ALBuffer(String name, Wave wave) {
		super(name);
		this.wave = wave;
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {

		if (!org.lwjgl.openal.AL.isCreated()) {
			return;
		}

		try {
			// 1. Generate a buffer
			buffer = alGenBuffers();

			// First get the wave if necessary
			if (url != null) {
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
			}

			if (waveResource != null) {
				wave = waveResource.getWave();
			}

			if (wave == null) {
				// No wave
				return;
			}

			// 3. Upload wave data to AL
			format = AL.translateFormat(wave.getType());
			alBufferData(buffer, format, wave.getData(), wave.getFrequency());

			// 4. Forget the waveform data as it's been uploaded now...
			wave.dispose();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		if (buffer != 0) {
			ALSource.unattach(this);
			alDeleteBuffers(buffer);
			buffer = 0;
		}

	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		url = element.getAttribute("url");
		String gainS = element.getAttribute("gain");
		if (gainS != null && !"".equals(gainS)) {
			gain = Float.parseFloat(gainS);
		} else {
			gain = 1.0f;
		}
		String pitchS = element.getAttribute("pitch");
		if (pitchS != null && !"".equals(pitchS)) {
			pitch = Float.parseFloat(pitchS);
		} else {
			pitch = 1.0f;
		}
		try {
			priority = Integer.parseInt(element.getAttribute("priority"));
		} catch (NumberFormatException e) {
			throw new Exception(this+" has no priority specified.");
		}
		looped = XMLUtil.getBoolean(element, "looped", false);
	}

	/**
	 * @return the buffer ID
	 */
	@Override
	public final int getBufferID() {
		assert isCreated() : this + " is not created yet.";
		return buffer;
	}

	/**
	 * @return the source Wave
	 */
	public final Wave getWave() {
		return wave;
	}

	@Override
	public String toString() {
		return "ALBuffer["+getName()+"]";
	}

	/**
	 * Returns the gain.
	 * @return float
	 */
	public float getGain() {
		return gain;
	}

	/**
	 * Returns the pitch.
	 * @return float
	 */
	public float getPitch() {
		return pitch;
	}

	/**
	 * Returns the priority
	 * @return int
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * @return the AL format (AL_FORMAT_STEREO16,etc)
	 */
	public int getFormat() {
		return format;
	}

	/**
	 * Returns looped status
	 * @return boolean
	 */
	@Override
	public boolean isLooped() {
		return looped;
	}

	/**
	 * Sets the data directly
	 * @param data
	 */
	public void setData(WaveWrapper wave) throws Exception {
		alBufferData(buffer, AL.translateFormat(wave.getType()), wave.getWave().getData(), wave.getFrequency());
	}
}
