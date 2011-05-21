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

package net.puppygames.applet;

import org.lwjgl.util.ReadableRectangle;

import com.shavenpuppy.jglib.resources.Data;
import com.shavenpuppy.jglib.resources.Feature;

public class Anchor extends Feature {

	@Data
	private String x, y, w, h;

	private int d;

	public Anchor() {
	}

	public void apply(Bounded target) {
		ReadableRectangle bounds = target.getBounds();

		int newX = bounds.getX();
		int newY = bounds.getY();
		int newW = bounds.getWidth();
		int newH = bounds.getHeight();

		if (x != null) {
			newX = position(x, true);
		}
		if (y != null) {
			newY = position(y, false);
		}
		if (w != null) {
			newW = position(w, true) - newX;
		}
		if (h != null) {
			newH = position(h, false) - newY;
		}
		target.setBounds(newX, newY, newW, newH);
	}

	private int position(String edge, boolean horiz) {
		float ratio = 0.0f;
		if ("top".equals(edge) && !horiz) {
			ratio = 1.0f;
		} else if ("bottom".equals(edge) && !horiz) {
			ratio = 0.0f;
		} else if ("left".equals(edge) && horiz) {
			ratio = 0.0f;
		} else if ("right".equals(edge) && horiz) {
			ratio = 1.0f;
		} else if ("mid".equals(edge)) {
			ratio = 0.5f;
		} else if (edge.endsWith("%")) {
			ratio = Float.parseFloat(edge.substring(0, edge.length() - 2)) / 100.0f;
		} else {
			System.out.println("Illegal anchor specified: "+edge+"/"+d);
			ratio = 0.0f;
		}
		if (horiz) {
			return ((int) (Game.getWidth() * ratio)) + d;
		} else {
			return ((int) (Game.getHeight() * ratio)) + d;
		}
	}
}