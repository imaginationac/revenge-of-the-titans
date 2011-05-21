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

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadablePoint;

import com.shavenpuppy.jglib.sprites.AnimatedAppearance;
import com.shavenpuppy.jglib.sprites.Sprite;

/**
 * $Id: SpriteEffect.java,v 1.2 2010/08/03 23:43:39 foo Exp $
 * An effect which is a sprite animation. As a handy special feature, a sprite
 * animation which sets event 1 will cause the effect to finish.
 * @author $Author: foo $
 * @version $Revision: 1.2 $
 */
public class SpriteEffect extends SimpleBaseEffect {

	private static final long serialVersionUID = 1L;

	/** Sprite */
	private Sprite sprite;

	/** Layer */
	private int layer;

	/** Appearance */
	private AnimatedAppearance appearance;

	/**
	 * @param startColor
	 * @param endColor
	 * @param duration
	 * @param fadeDuration
	 */
	public SpriteEffect(AnimatedAppearance appearance, int layer, ReadableColor startColor,
		ReadableColor endColor, int duration, int fadeDuration) {
		super(startColor, endColor, duration, fadeDuration);

		this.appearance = appearance;
		this.layer = layer;
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.Effect#doSpawn()
	 */
	@Override
	protected void doSpawn() {
		sprite = getScreen().allocateSprite(getScreen()); // A bit wierd but we need something serializable as an owner
		if (sprite == null) {
			remove();
		}
		sprite.setAppearance(appearance);
		sprite.setLayer(layer);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.SimpleBaseEffect#doSetLocation()
	 */
	@Override
	protected void doSetLocation() {
		if (sprite == null) {
			return;
		}
		ReadablePoint offset = getOffset();
		float x = getX();
		float y = getY();
		if (offset != null) {
			x += offset.getX();
			y += offset.getY();
		}
		sprite.setLocation(x, y, 0);
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.SimpleBaseEffect#doEffectRender()
	 */
	@Override
	protected void doEffectRender() {
		sprite.setVisible(isVisible());
		sprite.setColors(getCachedColor());
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.SimpleBaseEffect#doSimpleTick()
	 */
	@Override
	protected void doSimpleTick() {
		if (sprite.getEvent() == 1) {
			finish();
		}
	}

	/* (non-Javadoc)
	 * @see net.puppygames.applet.effects.Effect#doRemove()
	 */
	@Override
	protected void doRemove() {
		if (sprite != null) {
			sprite.deallocate();
			sprite = null;
		}
	}

}
