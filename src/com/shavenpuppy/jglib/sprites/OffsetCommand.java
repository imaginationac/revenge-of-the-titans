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
package com.shavenpuppy.jglib.sprites;

import java.io.IOException;

import org.lwjgl.util.vector.Vector2f;
import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Adjust the offset of a sprite.
 */
public class OffsetCommand extends Command {

	private static final long serialVersionUID = 1L;

	/** Temp holder */
	private static final Vector2f temp = new Vector2f();

	/** The adjustment */
	private float deltax, deltay;

	/** Relative */
	private boolean relativex, relativey;

	/** Duration */
	private int duration;

	/**
	 * Constructor for OffsetCommand.
	 */
	public OffsetCommand() {
		super();
	}

	/**
	 * @see com.shavenpuppy.jglib.sprites.Command#execute(com.shavenpuppy.jglib.sprites.Animated)
	 */
	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		int currentTick = target.getTick() + 1;

		if (currentTick == 1) {
			if (relativex) {
				target.getOffset(temp);
				if (relativey) {
					temp.setX(temp.getX() + deltax);
					temp.setY(temp.getY() + deltay);
				} else {
					temp.setX(temp.getX() + deltax);
					temp.setY(deltay);
				}
				target.setOffset(temp.getX(), temp.getY());
			} else if (relativey) {
				target.getOffset(temp);
				temp.setX(deltax);
				temp.setY(temp.getY() + deltay);
				target.setOffset(temp.getX(), temp.getY());
			} else {
				target.setOffset(deltax, deltay);
			}
		}
		if (currentTick > duration) {
			target.setSequence(++currentSequence);
			target.setTick(0);
			return true; // Execute the next command
		}

		target.setTick(currentTick);
		return false; // Don't execute the next command
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#load(org.w3c.dom.Element, Loader)
	 */
	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		String sx = XMLUtil.getString(element, "x");
		if (sx.startsWith("+")) {
			relativex = true;
			deltax = Float.parseFloat(sx.substring(1));
		} else {
			deltax = Float.parseFloat(sx);
		}
		String sy = XMLUtil.getString(element, "y");
		if (sy.startsWith("+")) {
			relativey = true;
			deltay = Float.parseFloat(sy.substring(1));
		} else {
			deltay = Float.parseFloat(sy);
		}
		duration = XMLUtil.getInt(element, "d", 0);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (relativex) {
			writer.writeAttribute("x", "+"+deltax);
		} else {
			writer.writeAttribute("x", deltax, true);
		}
		if (relativey) {
			writer.writeAttribute("y", "+"+deltay);
		} else {
			writer.writeAttribute("y", deltay, true);
		}
		writer.writeAttribute("d", duration, true);
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doCreate()
	 */
	@Override
	protected void doCreate() {
	}

	/**
	 * @see com.shavenpuppy.jglib.Resource#doDestroy()
	 */
	@Override
	protected void doDestroy() {
	}

}
