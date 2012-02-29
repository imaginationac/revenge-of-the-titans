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

import java.util.ArrayList;
import java.util.List;

import net.puppygames.applet.Anchor;
import net.puppygames.applet.Bounded;

import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableRectangle;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * $Id: SimpleBaseEffectFeature.java,v 1.1 2009/07/01 15:38:53 foo Exp $
 * Base class for simple effect features
 * @author $Author: foo $
 * @version $Revision: 1.1 $
 */
public abstract class SimpleBaseEffectFeature extends EffectFeature {

	/*
	 * Resource data
	 */

	protected Color startColor;
	protected Color endColor;
	protected int duration;
	protected int fadeDuration;
	private int x, y, w, h;
	private List<Anchor> anchors;

	private class BoundedThing implements Bounded {
		Rectangle r = new Rectangle(x, y, 0, 0);

		@Override
		public ReadableRectangle getBounds() {
		    return r;
		}
		@Override
		public void setBounds(int x, int y, int w, int h) {
			r.setBounds(x, y, w, h);
		}
	}

	/**
	 * C'tor
	 */
	public SimpleBaseEffectFeature() {
		super();
	}

	/**
	 * @param name
	 */
	public SimpleBaseEffectFeature(String name) {
		super(name);
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
	    super.load(element, loader);

		List<Element> anchorElements = XMLUtil.getChildren(element, "anchor");
		if (anchorElements.size() > 0) {
			anchors = new ArrayList<Anchor>(anchorElements.size());
			for (Element anchorChild : anchorElements) {
				Anchor anchor = (Anchor) loader.load(anchorChild);
				anchors.add(anchor);
			}
		}
	}

	public final int getX() {
		if (anchors == null) {
			return x;
		}
		BoundedThing bt = new BoundedThing();
		for (Anchor a : anchors) {
			a.apply(bt);
		}
	    return bt.getBounds().getX();
    }

	public final int getY() {
		if (anchors == null) {
			return y;
		}
		BoundedThing bt = new BoundedThing();
		for (Anchor a : anchors) {
			a.apply(bt);
		}
	    return bt.getBounds().getY();
    }

	public final int getWidth() {
		if (anchors == null) {
			return w;
		}
		BoundedThing bt = new BoundedThing();
		for (Anchor a : anchors) {
			a.apply(bt);
		}
	    return bt.getBounds().getWidth();
    }

	public final int getHeight() {
		if (anchors == null) {
			return h;
		}
		BoundedThing bt = new BoundedThing();
		for (Anchor a : anchors) {
			a.apply(bt);
		}
	    return bt.getBounds().getHeight();
    }

}
