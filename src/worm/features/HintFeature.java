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
package worm.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.resources.ResourceArray;

/**
 * Hints
 */
public class HintFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/** Hints map */
	private static final Map<String, HintFeature> ALL_HINTS = new HashMap<String, HintFeature>();
	private static int cycle;

	/** Pause the game while this hint is displayed */
	private boolean pause;

	/** Layers for the icon */
	private String icon;

	/** The actual hint texts (in sequence) */
	private ResourceArray hints;

	/** Random sequence */
	private boolean random;

	/** Hint appears after this many seconds */
	private int seconds;


	private int minLevel, maxLevel;

	private transient LayersFeature iconResource;
	private transient String text;

	public HintFeature() {
		super();
		setAutoCreated();
	}

	public static HintFeature getNext() {
		ArrayList<HintFeature> list = new ArrayList<HintFeature>(ALL_HINTS.values());
		cycle ++;
		if (cycle == list.size()) {
			cycle = 0;
		}
		return list.get(cycle);
	}

	/**
	 * C'tor
	 * @param name
	 */
	public HintFeature(String name) {
		super(name);
		setAutoCreated();
	}

	/**
	 * @return the minLevel
	 */
	public int getMinLevel() {
		return minLevel;
	}

	/**
	 * @return the maxLevel
	 */
	public int getMaxLevel() {
		return maxLevel;
	}

	/**
	 * @return the seconds
	 */
	public int getSeconds() {
		return seconds;
	}

	/**
	 * @return the icon
	 */
	public LayersFeature getIcon() {
		return iconResource;
	}

	/**
	 * @return the text; if null, use {@link #getHints()} instead
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return the hints
	 */
	public ResourceArray getHints() {
		return hints;
	}

	/**
	 * @return the random
	 */
	public boolean isRandom() {
		return random;
	}

	/**
	 * @return the pause
	 */
	public boolean getPause() {
		return pause;
	}

	@Override
	protected void doRegister() {
		ALL_HINTS.put(getName(), this);
	}

	@Override
	protected void doDeregister() {
		ALL_HINTS.remove(getName());
	}

	public void setDetails(LayersFeature iconResource, String text) {
		this.iconResource = iconResource;
		this.text = text;
	}

	public static HintFeature getHint(String hintName) {
		return ALL_HINTS.get(hintName);
	}
}
