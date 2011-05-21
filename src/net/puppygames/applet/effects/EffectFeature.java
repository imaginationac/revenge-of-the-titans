/*
 * Copyright (c) 2003 Shaven Puppy Ltd
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
package net.puppygames.applet.effects;

import net.puppygames.applet.Screen;

import com.shavenpuppy.jglib.openal.ALBuffer;
import com.shavenpuppy.jglib.resources.Feature;
import com.shavenpuppy.jglib.sprites.SpriteAllocator;

/**
 * $Id: EffectFeature.java,v 1.3 2010/02/06 01:26:34 foo Exp $
 * An EffectFeature can be used to spawn predefined effects
 * @author $Author: foo $
 * @version $Revision: 1.3 $
 */
public abstract class EffectFeature extends Feature {

	private static final long serialVersionUID = 1L;

	/*
	 * Resource data
	 */

	/** Delay in ticks for the spawned effect */
	private int delay;

	/** A sound effect to play when the effect happens */
	private String sound;

	/*
	 * Transient data
	 */

	private transient ALBuffer soundResource;

	/**
	 * C'tor
	 */
	public EffectFeature() {
		super();
	}

	/**
	 * @param name
	 */
	public EffectFeature(String name) {
		super(name);
	}

	/**
	 * Spawn an effect
	 * @param screen The screen to spawn the effect on
	 * @return the Effect
	 */
	public final Effect spawn(Screen screen) {
		Effect effect = doSpawn(screen);
		effect.setDelay(delay + 1);
		effect.setSound(soundResource);
		effect.spawn(screen);
		return effect;
	}
	protected abstract Effect doSpawn(SpriteAllocator screen);

}
