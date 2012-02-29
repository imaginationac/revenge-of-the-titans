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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;

/**
 * A Medal.
 */
public class MedalFeature extends Feature {

	private static final long serialVersionUID = 1L;

	private static final Map<String, MedalFeature> MEDALS = new HashMap<String, MedalFeature>();

	@Data
	private String title;
	@Data
	private String description;
	private int points;
	private int money;
	private boolean repeatable;
	private LayersFeature appearance;
	private boolean suppressHint;
	private boolean steam;
	private boolean xmas;

	private transient HintFeature hintFeature;

	public MedalFeature(String name) {
		super(name);
		setAutoCreated();
	}

	public boolean getSuppressHint() {
		return suppressHint;
	}

	public HintFeature getHint() {
		return hintFeature;
	}

	@Override
	protected void doRegister() {
		MEDALS.put(getName(), this);
	}

	@Override
	protected void doDeregister() {
		MEDALS.remove(getName());
	}

	public String getDescription() {
		return description;
	}

	public String getTitle() {
		return title;
	}

	public int getPoints() {
		return points;
	}

	public int getMoney() {
		return money;
	}

	public LayersFeature getAppearance() {
		return appearance;
	}

	public boolean isRepeatable() {
		return repeatable;
	}

	public boolean isSteam() {
	    return steam;
    }

	public boolean isXmas() {
	    return xmas;
    }

	@Override
	protected void doCreate() {
		super.doCreate();

		// Create a hint
		if (!suppressHint) {
			hintFeature = new HintFeature();
			hintFeature.setDetails(appearance, "{color:gui-mid font:tinyfont.glfont}NEW MEDAL: {color:gui-bright font:tinyfont.glfont}"+title+"\n{color:gui-mid}"+description);
			hintFeature.create();
		}

	}

	public static Map<String, MedalFeature> getMedals() {
		return Collections.unmodifiableMap(MEDALS);
	}
}
