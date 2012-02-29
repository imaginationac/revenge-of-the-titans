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
package worm.animation;

import net.puppygames.applet.Area;
import net.puppygames.applet.effects.Emitter;
import net.puppygames.applet.effects.EmitterFeature;

import org.w3c.dom.Element;

import worm.Entity;
import worm.screens.GameScreen;

import com.shavenpuppy.jglib.Resources;
import com.shavenpuppy.jglib.sprites.Command;
import com.shavenpuppy.jglib.sprites.Sprite;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.XMLUtil;

public class EmitCommand extends Command {

	private String id;
	private float ox, oy;

	private transient EmitterFeature idFeature;

	/**
	 * C'tor
	 */
	public EmitCommand() {
	}

	@Override
	public void load(Element element, Loader loader) throws Exception {
		id = XMLUtil.getString(element, "id");
		ox = XMLUtil.getFloat(element, "ox");
		oy = XMLUtil.getFloat(element, "oy");
	}

	@Override
	protected void doCreate() {
		idFeature = Resources.get(id);
	}

	@Override
	public void archive() {
		id = null;
	}

	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		target.setSequence(currentSequence + 1);

		float xx;
		float yy;

		Sprite sprite = (Sprite) target;
		Object owner = sprite.getOwner();
		if (owner instanceof Entity) {
			Entity entity = (Entity) owner;

			// chaz hack - getX() so emitters spawn from centre x, not bottom left so they mirror ok
			xx = entity.getX() + ox * FPMath.floatValue(sprite.getXScale());
			yy = entity.getMapY() + oy * FPMath.floatValue(sprite.getYScale());
			Emitter e = idFeature.spawn(GameScreen.getInstance());
			e.setLocation(xx, yy);
		} else if (owner instanceof Area) {
			Area area = (Area) owner;
			xx = area.getBounds().getX() + ox;
			yy = area.getBounds().getY() + oy;
			Emitter e = idFeature.spawn(area.getScreen());
			e.setLocation(xx, yy);
		} else {
			System.err.println(owner+" is not an area or an entity!");
			return true;
		}
		return true;
	}

}
