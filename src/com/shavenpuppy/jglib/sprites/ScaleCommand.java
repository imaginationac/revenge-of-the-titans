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

import org.w3c.dom.Element;

import com.shavenpuppy.jglib.Resource;
import com.shavenpuppy.jglib.XMLResourceWriter;
import com.shavenpuppy.jglib.util.FPMath;
import com.shavenpuppy.jglib.util.XMLUtil;

/**
 * Adjust the scale of a sprite.
 */
public class ScaleCommand extends Command {

	private static final long serialVersionUID = 1L;

	/** The adjustment */
	private float xdelta, ydelta;

	/** Relative */
	private boolean relativex, relativey;

	/** Duration */
	private int duration;

	/**
	 * Constructor for FrameCommand.
	 */
	public ScaleCommand() {
		super();
	}

	@Override
	public boolean execute(Sprite target) {
		int currentSequence = target.getSequence();
		int currentTick = target.getTick() + 1;

		if (currentTick == 1) {
			int newScaleX, newScaleY;
			if (relativex) {
				newScaleX = target.getXScale() + FPMath.fpValue(xdelta);
			} else {
				newScaleX = FPMath.fpValue(xdelta);
			}
			if (relativey) {
				newScaleY = target.getYScale() + FPMath.fpValue(ydelta);
			} else {
				newScaleY = FPMath.fpValue(ydelta);
			}
			target.setScale(newScaleX, newScaleY);
		}
		if (currentTick > duration) {
			target.setSequence(++currentSequence);
			target.setTick(0);
			return true; // Execute the next command
		}

		target.setTick(currentTick);
		return false; // Don't execute the next command
	}

	@Override
	public void load(Element element, Resource.Loader loader) throws Exception {
		String sdelta = XMLUtil.getString(element, "scale", "!");
		if (!sdelta.equals("!")) {
			if (sdelta.startsWith("+")) {
				relativex = true;
				relativey = true;
				xdelta = Float.parseFloat(sdelta.substring(1));
			} else {
				xdelta = Float.parseFloat(sdelta);
			}
			ydelta = xdelta;
		} else {
			String sxdelta = XMLUtil.getString(element, "xscale");
			if (sxdelta.startsWith("+")) {
				relativex = true;
				xdelta = Float.parseFloat(sxdelta.substring(1));
			} else {
				xdelta = Float.parseFloat(sxdelta);
			}
			String sydelta = XMLUtil.getString(element, "yscale");
			if (sydelta.startsWith("+")) {
				relativey = true;
				ydelta = Float.parseFloat(sydelta.substring(1));
			} else {
				ydelta = Float.parseFloat(sydelta);
			}
		}
		duration = XMLUtil.getInt(element, "d", 0);
	}

	/* (non-Javadoc)
	 * @see com.shavenpuppy.jglib.Resource#doToXML(com.shavenpuppy.jglib.XMLResourceWriter)
	 */
	@Override
	protected void doToXML(XMLResourceWriter writer) throws IOException {
		if (xdelta == ydelta && relativex == relativey) {
			if (relativex) {
				writer.writeAttribute("scale", "+"+xdelta);
			} else {
				writer.writeAttribute("scale", xdelta, true);
			}
		} else {
			if (relativex) {
				writer.writeAttribute("x", "+"+xdelta);
			} else {
				writer.writeAttribute("x", xdelta, true);
			}
			if (relativey) {
				writer.writeAttribute("y", "+"+ydelta);
			} else {
				writer.writeAttribute("y", ydelta, true);
			}
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
