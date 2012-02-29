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

import java.io.InputStream;
import java.util.ArrayList;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * A sound wave that is streamed from an input stream. We use three actual AL
 * buffers to stream; one that's playing, the other that's queued, and a third
 * that we're filling with new data.
 */
public class ALStream extends Resource {

	private static final long serialVersionUID = 1L;

	/** Pool of stream instances */
	private static final ArrayList<ALStreamInstance> POOL = new ArrayList<ALStreamInstance>();

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

	/** Format */
	private transient int format;

	/** An InputStream that will supply data (as opposed to using a URL) - this is used by video playback */
	private transient InputStream inputStream;

	/**
	 * Constructor for ALWave.
	 */
	public ALStream() {
		super();
	}

	/**
	 * Constructor for ALWave.
	 * @param name
	 */
	public ALStream(String name) {
		super(name);
	}

	/**
	 * C'tor used by video playback
	 * @param inputStream
	 */
	public ALStream(InputStream inputStream) {
	    super();

	    this.inputStream = inputStream;
	    gain = 1.0f;
	    pitch = 1.0f;
	    priority = Integer.MAX_VALUE;
	}

	/**
	 * @return the channels
	 */
	public int getFormat() {
		return format;
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {

		if (!org.lwjgl.openal.AL.isCreated()) {
			return;
		}

		ALStreamInstance ret = new ALStreamInstance(this);
		ret.create();
		format = ret.getFormat();
		ret.destroy();
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
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

	@Override
	public String toString() {
		return "ALStream["+url+"]";
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
	 * Returns looped status
	 * @return boolean
	 */
	public boolean isLooped() {
		return looped;
	}

	/**
	 * @return Returns the url.
	 */
	public String getURL() {
		return url;
	}

	/**
     * Accessor for inputStream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

	/**
	 * Get or create a stream instance from our pool
	 */
	public ALStreamInstance getInstance(ALSource source) throws Exception {
		for (int i = 0; i < POOL.size(); i ++) {
			ALStreamInstance ret = POOL.get(i);
			if (!ret.isPlaying()) {
				ret.setOwner(source);
				ret.init(this);
				ret.reset();
				return ret;
			}
		}
		// Create a new one
		ALStreamInstance ret = new ALStreamInstance(this);
		ret.create();
		ret.setOwner(source);
		POOL.add(ret);
		//System.out.println("Created a new one");
		return ret;
	}
}
