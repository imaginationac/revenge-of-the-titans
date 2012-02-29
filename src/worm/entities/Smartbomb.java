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
package worm.entities;

import net.puppygames.applet.effects.StrobeEffect;

import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.Rectangle;

import worm.Entity;
import worm.GameStateInterface;
import worm.SFX;
import worm.effects.SmartbombEffect;
import worm.screens.GameScreen;


/**
 * $Id: Smartbomb.java,v 1.7 2010/03/29 22:39:46 foo Exp $
 *
 * @author $Author: foo $
 * @version $Revision: 1.7 $
 */
public class Smartbomb extends Entity {

	private static final long serialVersionUID = 1L;

	/** Damage caused */
	public static final int DAMAGE = 100;

	/** Effect */
	private transient SmartbombEffect effect;

	/**
	 * C'tor
	 */
	public Smartbomb(float x, float y) {
		setLocation(x, y);
	}

	@Override
	public boolean isShootable() {
		return false;
	}

	@Override
	public boolean isHoverable() {
		return false;
	}

	@Override
	protected void doSpawn() {
		effect = new SmartbombEffect(getMapX(), getMapY());
		effect.setOffset(GameScreen.getSpriteOffset());
		effect.spawn(GameScreen.getInstance());

		StrobeEffect strobe = new StrobeEffect(ReadableColor.WHITE, SmartbombEffect.DURATION * 2);
		strobe.spawn(GameScreen.getInstance());
		GameScreen.shake(32);

		SFX.smartbomb();
	}

	@Override
	protected void doRespawn() {
		// Too much hassle
		remove();
	}

	@Override
	protected void doRemove() {
		if (effect != null) {
			effect.remove();
			effect = null;
		}
	}

	@Override
	protected void doTick() {
		if (effect == null || !effect.isActive()) {
			remove();
		}
	}

	@Override
	public boolean canCollide() {
		return true;
	}

	@Override
	public void onCollision(Entity entity) {
		entity.onCollisionWithSmartbomb(this);
	}

	@Override
	public float getRadius() {
		if (effect == null) {
			return 0.0f;
		} else {
			return effect.getRadius();
		}
	}

	@Override
	public boolean isRound() {
		return true;
	}

	@Override
	public Rectangle getBounds(Rectangle bounds) {
		bounds.setBounds(0, 0, 0, 0);
		return bounds;
	}

	@Override
	public final void addToGameState(GameStateInterface gsi) {
		// Ignore
	}

	@Override
	public final void removeFromGameState(GameStateInterface gsi) {
		// Ignore
	}


}
