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

import java.util.*;

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.sprites.SpriteAllocator;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: CompoundEffectFeature.java,v 1.3 2010/02/06 01:26:34 foo Exp $
 * Describes a whole bunch of effects that all go off at once
 * @author $Author: foo $
 * @version $Revision: 1.3 $
 */
public class CompoundEffectFeature extends EffectFeature {

	private static final long serialVersionUID = 1L;

	/*
	 * Sub effects
	 */

	private List<EffectFeature> subEffects;

	/**
	 * C'tor
	 */
	public CompoundEffectFeature() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#load(org.w3c.dom.Element, com.shavenpuppy.jglib.Resource.Loader)
	 */
	@Override
	public void load(Element element, Loader loader) throws Exception {
		super.load(element, loader);

		List<Element> children = XMLUtil.getChildren(element);
		subEffects = new LinkedList<EffectFeature>();
		for (Iterator<Element> i = children.iterator(); i.hasNext(); ) {
			Element child = i.next();
			EffectFeature subEffect = (EffectFeature) loader.load(child);
			subEffects.add(subEffect);
		}

	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doCreate()
	 */
	@Override
	protected void doCreate() {
		super.doCreate();

		for (Iterator<EffectFeature> i = subEffects.iterator(); i.hasNext(); ) {
			(i.next()).create();
		}
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.resources.Feature#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		super.doDestroy();

		for (Iterator<EffectFeature> i = subEffects.iterator(); i.hasNext(); ) {
			(i.next()).destroy();
		}
	}

	/**
	 * Named c'tor
	 * @param name
	 */
	public CompoundEffectFeature(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.EffectFeature#doSpawn(net.puppygames.applet.Screen)
	 */
	@Override
	protected Effect doSpawn(SpriteAllocator screen) {
		return new CompoundEffect(subEffects);
	}

}
